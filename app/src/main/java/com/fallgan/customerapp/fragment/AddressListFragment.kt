package com.fallgan.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.activity.PaymentActivity
import com.fallgan.customerapp.adapter.AddressAdapter
import com.fallgan.customerapp.databinding.FragmentAddressListBinding
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.Address

class AddressListFragment : Fragment() {

    lateinit var binding: FragmentAddressListBinding

    var total = 0
    private var subTotal = 0.0
    private var pCodeDiscount = 0.0
    lateinit var root: View
    private lateinit var session: Session
    private lateinit var pCode: String
    lateinit var activity: Activity

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_address_list, container, false)

        binding = FragmentAddressListBinding.inflate(inflater, container, false)

        recyclerView = root.findViewById(R.id.recyclerView)
        tvAlert = root.findViewById(R.id.tvNoAddressAlert)

        activity = requireActivity()
        session = Session(activity)

        Constant.selectedAddressId = ""

        getAddresses()

        if (requireArguments().getString(Constant.FROM).equals("process", ignoreCase = true)) {
            binding.confirmLyt.visibility = View.VISIBLE
            subTotal = requireArguments().getDouble(Constant.TOTAL)
            if (requireArguments().getString(Constant.PROMO_CODE) != null && requireArguments().getString(
                    Constant.PROMO_CODE
                )!!.isNotEmpty()
            ) {
                pCode = requireArguments().getString(Constant.PROMO_CODE).toString()
                pCodeDiscount = requireArguments().getDouble(Constant.PROMO_DISCOUNT, 0.0)
                binding.lytPromoDiscount.visibility = View.VISIBLE
                binding.tvPromoDiscount.text =
                    "-" + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + pCodeDiscount
                    )
                binding.tvPromoCode.text =
                    activity.getString(R.string.promo_discount) + " (" + pCode + ")"
            } else {
                binding.lytPromoDiscount.visibility = View.GONE
            }
            binding.tvSubTotal.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + (subTotal - pCodeDiscount))
            binding.tvTotalItems.text =
                Constant.TOTAL_CART_ITEM.toString() + if (Constant.TOTAL_CART_ITEM > 1) activity.getString(
                    R.string.items
                ) else activity.getString(R.string.item)
            binding.tvConfirmOrder.setOnClickListener {
                when {
                    Constant.selectedAddressId == "" -> {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.please_select_address),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    session.getData(Constant.STATUS) == "1" -> {
                        val intent = Intent(activity, PaymentActivity::class.java)
                        intent.putExtra(Constant.FROM, "process")
                        intent.putExtra(
                            Constant.PROMO_CODE,
                            requireArguments().getString(Constant.PROMO_CODE)
                        )
                        intent.putExtra(
                            Constant.PROMO_DISCOUNT,
                            requireArguments().getDouble(Constant.PROMO_DISCOUNT)
                        )
                        startActivity(intent)
                    }
                    else -> {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.user_block_msg),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            binding.confirmLyt.visibility = View.GONE
        }
        setHasOptionsMenu(true)
        binding.swipeLayout.setColorSchemeColors(
            ContextCompat.getColor(
                activity, R.color.colorPrimary
            )
        )
        binding.swipeLayout.setOnRefreshListener {
            addresses.clear()
            getAddresses()
            binding.swipeLayout.isRefreshing = false
        }
        binding.fabAddAddress.setOnClickListener { addNewAddress() }
        return binding.root
    }

    private fun addNewAddress() {
        val fragment: Fragment = AddressAddUpdateFragment()
        val bundle = Bundle()
        bundle.putSerializable("model", "")
        bundle.putString("for", "add")
        bundle.putInt("position", 0)
        fragment.arguments = bundle
        MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
            .commit()
    }

    private fun getAddresses() {
        addresses = ArrayList()
        addressAdapter = AddressAdapter(
            activity,
            addresses,
            R.layout.lyt_address_list
        )

        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ADDRESSES] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL).toInt()
                            session.setData(Constant.TOTAL, total.toString())
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val address =
                                        Gson().fromJson(jsonObject1.toString(), Address::class.java)
                                    if (address.is_default == "1" || (session.getData(Constant.SHIPPING_TYPE) == "local" && (address.city_id == "0" || address.city_id == "") && (address.area_id == "0" || address.area_id == "") && (address.pincode_id == "0" || address.pincode_id == "") || session.getData(
                                            Constant.SHIPPING_TYPE
                                        ) == "standard" && address.city_id != "0" && address.area_id != "0" && address.pincode_id != "0"
                                                )
                                    ) { Constant.selectedAddressId = address.id

                                    }
                                    addresses.add(address)
                                } else {
                                    break
                                }
                            }
                            addressAdapter = AddressAdapter(
                                activity,
                                addresses,
                                R.layout.lyt_address_list
                            )
                            binding.recyclerView.adapter = addressAdapter
                            binding.tvNoAddressAlert.visibility = View.GONE
                        } else {
                            binding.tvNoAddressAlert.visibility = View.VISIBLE
                        }
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.tvNoAddressAlert.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.GET_ADDRESS_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.addresses)
        activity.invalidateOptionsMenu()
        hideKeyboard()
        try {
            binding.recyclerView.adapter = null
            binding.recyclerView.adapter = addressAdapter
        } catch (e: Exception) {

        }
    }

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(root.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
    }

    companion object {
        lateinit var recyclerView: RecyclerView

        @SuppressLint("StaticFieldLeak")
        lateinit var tvAlert: TextView

        @SuppressLint("StaticFieldLeak")
        lateinit var addressAdapter: AddressAdapter

        lateinit var addresses: ArrayList<Address>
    }
}