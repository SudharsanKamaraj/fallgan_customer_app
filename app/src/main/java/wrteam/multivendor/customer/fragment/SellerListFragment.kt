package com.gpn.customerapp.fragment


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.adapter.SellerAdapter
import com.gpn.customerapp.databinding.FragmentSellerListBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Seller

class SellerListFragment : Fragment() {
    lateinit var binding: FragmentSellerListBinding
    lateinit var root: View
    lateinit var activity: Activity
    lateinit var session: Session
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_seller_list, container, false)
        binding = FragmentSellerListBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)
        setHasOptionsMenu(true)

        binding.sellerRecyclerView.layoutManager = GridLayoutManager(activity, Constant.GRID_COLUMN)
        binding.swipeLayout.setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
        binding.swipeLayout.setOnRefreshListener {
            if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
                ApiConfig.getWalletBalance(activity, Session(activity))
            }
            getSellerList()
            binding.swipeLayout.isRefreshing = false
        }
        getSellerList()
        return binding.root
    }

    private fun getSellerList() {
        binding.sellerRecyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_SELLER_DATA] = Constant.GetVal
        if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0") {
            params[Constant.PINCODE] =
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        sellerArrayList = ArrayList()
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            val gson = Gson()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val seller =
                                    gson.fromJson(jsonObject.toString(), Seller::class.java)
                                sellerArrayList.add(seller)
                            }
                            binding.sellerRecyclerView.adapter = SellerAdapter(
                                activity,
                                sellerArrayList,
                                R.layout.lyt_seller,
                                "category",
                                0
                            )
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.sellerRecyclerView.visibility = View.VISIBLE
                        } else {
                            binding.tvAlert.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.sellerRecyclerView.visibility = View.GONE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.sellerRecyclerView.visibility = View.GONE
                    }
                }
            }
        }, activity, Constant.SELLER_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.seller)
        requireActivity().invalidateOptionsMenu()
        hideKeyboard()
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
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
    }

    companion object {
        lateinit var sellerArrayList: ArrayList<Seller>
    }
}