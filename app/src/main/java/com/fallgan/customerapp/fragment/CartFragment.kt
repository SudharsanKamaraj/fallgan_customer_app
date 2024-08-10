package com.fallgan.customerapp.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.LoginActivity
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.adapter.CartAdapter
import com.fallgan.customerapp.adapter.OfflineCartAdapter
import com.fallgan.customerapp.adapter.OfflineSaveForLaterAdapter
import com.fallgan.customerapp.adapter.SaveForLaterAdapter
import com.fallgan.customerapp.databinding.FragmentCartBinding
import com.fallgan.customerapp.helper.*
import com.fallgan.customerapp.model.Cart
import com.fallgan.customerapp.model.OfflineCart
import com.fallgan.customerapp.model.PromoCode

@SuppressLint("SetTextI18n")
class CartFragment : Fragment() {

    lateinit var activity: Activity
    lateinit var root: View
    lateinit var databaseHelper: DatabaseHelper
    private lateinit var variantIdList: ArrayList<String>
    private lateinit var qtyList: ArrayList<String>
    private lateinit var promoCodeAdapter: PromoCodeAdapter
    private lateinit var promoCodes: ArrayList<PromoCode?>

    var total = 0.0
    var pCodeDiscount = 0.0
    var isApplied = false
    var subTotal = 0.0
    var offset = 0
    var isLoadMore = false
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_cart, container, false)

        binding = FragmentCartBinding.inflate(inflater, container, false)

        lytTotal = binding.lytTotal
        lytEmpty = binding.lytEmpty
        tvSaveForLaterTitle = binding.tvSaveForLaterTitle
        tvLocation = binding.tvLocation
        lytSaveForLater = binding.lytSaveForLater

        activity = requireActivity()
        Constant.CartValues = HashMap()
        saveForLaterValues = HashMap()
        activity = requireActivity()
        session = Session(activity)
        carts = ArrayList()
        saveForLater = ArrayList()
        offlineCarts = ArrayList()
        offlineSaveForLaterItems = ArrayList()
        pCode = ""
        binding.lytPromoCode.setOnClickListener {
            if (session.getBoolean(
                    Constant.IS_USER_LOGIN
                )
            ) {
                openDialog(activity)
            } else {
                val dialogClickListener =
                    DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                        when (which) {
                            DialogInterface.BUTTON_POSITIVE -> startActivity(
                                Intent(activity, LoginActivity::class.java).putExtra(
                                    Constant.FROM, "checkout"
                                )
                            )
                            DialogInterface.BUTTON_NEGATIVE -> dialog.dismiss()
                        }
                    }
                val builder = AlertDialog.Builder(
                    activity
                )
                builder.setMessage(activity.getString(R.string.promo_code_use_message))
                    .setPositiveButton(activity.getString(R.string.yes), dialogClickListener)
                    .setNegativeButton(activity.getString(R.string.no), dialogClickListener)
                    .show()
            }
        }

        binding.tvLocation.text = session.getData(Constant.GET_SELECTED_PINCODE_NAME)

        binding.btnRemoveOffer.setOnClickListener {
            binding.btnRemoveOffer.visibility = View.GONE
            binding.tvTotalAmount.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + Constant.FLOAT_TOTAL_AMOUNT)
            pCode = ""
            binding.tvPromoCode.text = activity.getString(R.string.use_promo_code)
            isApplied = false
            binding.lytPromoDiscount.visibility = View.GONE
            pCodeDiscount = 0.0
        }
        databaseHelper = DatabaseHelper(activity)
        setHasOptionsMenu(true)
        binding.tvLocation.text = session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        variantIdList = ArrayList()
        qtyList = ArrayList()
        binding.cartRecycleView.layoutManager = LinearLayoutManager(getActivity())
        binding.saveForLaterRecyclerView.layoutManager = LinearLayoutManager(getActivity())
        getSettings(activity)
        refreshListener = OnRefreshListener {
            ApiConfig.getProductNames(activity, session)
            getSettings(activity)
        }
        if (session.getData(Constant.SHIPPING_TYPE) == "local") {
            binding.lytPinCode.visibility = View.VISIBLE
            if (session.getData(Constant.GET_SELECTED_PINCODE_ID) == "0" || session.getData(
                    Constant.GET_SELECTED_PINCODE_ID
                ) == ""
            ) {
                openPinCodeView()
            }
            binding.tvTitleLocation.setOnClickListener {
                openPinCodeView()
            }

            binding.tvLocation.setOnClickListener {
                openPinCodeView()
            }

            binding.tvTitleLocation.setOnClickListener {
                openPinCodeView()
            }

            binding.lytPinCode.setOnClickListener {
                openPinCodeView()
            }
        }

        binding.tvConfirmOrder.setOnClickListener {
            if (!isSoldOut && !isDeliverable) {
                if (session.getData(Constant.min_order_amount).toString()
                        .toFloat() <= Constant.FLOAT_TOTAL_AMOUNT
                ) {
                    if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                        if (Constant.CartValues.isNotEmpty()) {
                            ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                        }
                        val fragment: Fragment = AddressListFragment()
                        val bundle = Bundle()
                        bundle.putString(Constant.FROM, "process")
                        bundle.putString(Constant.PROMO_CODE, pCode)
                        bundle.putDouble(Constant.PROMO_DISCOUNT, pCodeDiscount)
                        bundle.putDouble(Constant.TOTAL, Constant.FLOAT_TOTAL_AMOUNT)
                        fragment.arguments = bundle
                        MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                            .addToBackStack(null).commit()
                    } else {
                        startActivity(
                            Intent(
                                activity,
                                LoginActivity::class.java
                            ).putExtra(Constant.FROM, "checkout")
                        )
                    }
                } else {
                    Toast.makeText(
                        activity, getString(R.string.msg_minimum_order_amount) + session.getData(
                            Constant.CURRENCY
                        ) + ApiConfig.stringFormat(
                            session.getData(Constant.min_order_amount).toString()
                        ), Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (isDeliverable) {
                Toast.makeText(
                    activity,
                    getString(R.string.msg_non_deliverable),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(activity, getString(R.string.msg_sold_out), Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.btnShowNow.setOnClickListener { MainActivity.fm.popBackStack() }
        return binding.root
    }

    private fun openPinCodeView() {
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            if (Constant.CartValues.isNotEmpty()) {
                ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
            }
        }
        MainActivity.pinCodeFragment = PinCodeFragment()
        val bundle = Bundle()
        bundle.putString(Constant.FROM, "cart")
        MainActivity.pinCodeFragment.arguments = bundle
        MainActivity.pinCodeFragment.show(MainActivity.fm, null)
    }

    private fun getOfflineCart() {
        isDeliverable = false
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_VARIANTS_OFFLINE] = Constant.GetVal
        params[Constant.VARIANT_IDs] =
            databaseHelper.cartList().toString().replace("[", "").replace("]", "").replace("\"", "")
        if (session.getData(Constant.SHIPPING_TYPE)
                .equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                Constant.GET_SELECTED_PINCODE_ID
            ) != "0"
        ) {
            params[Constant.PINCODE] =
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            session.setData(Constant.TOTAL, jsonObject.getString(Constant.TOTAL))
                            val jsonArray = jsonObject.getJSONArray(Constant.DATA)

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                val cart =
                                    Gson().fromJson(jsonObject1.toString(), OfflineCart::class.java)
                                variantIdList.add(cart.product_variant_id)
                                qtyList.add(
                                    databaseHelper.CheckCartItemExist(
                                        cart.product_variant_id,
                                        cart.product_id
                                    )
                                )
                                var price: Double
                                var taxPercentage = "0"
                                try {
                                    taxPercentage =
                                        if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                price =
                                    if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                                        (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                                    } else {
                                        (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                                    }
                                Constant.FLOAT_TOTAL_AMOUNT += price * databaseHelper.CheckCartItemExist(
                                    cart.product_variant_id,
                                    cart.product_id
                                ).toInt()
                                offlineCarts.add(cart)
                            }
                            offlineCartAdapter = OfflineCartAdapter(activity)
                            binding.cartRecycleView.adapter = offlineCartAdapter
                            setData(activity)
                            binding.lytTotal.visibility = View.VISIBLE
                        }
                        getOfflineSaveForLater()
                    } catch (e: JSONException) {
                        getOfflineSaveForLater()
                    }
                } else {
                    getOfflineSaveForLater()
                }
            }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    private fun getOfflineSaveForLater() {
        offlineSaveForLaterItems = ArrayList()
        tvSaveForLaterTitle.text =
            activity.resources.getString(R.string.save_for_later) + " (" + offlineSaveForLaterItems.size + ")"
        offlineSaveForLaterAdapter = OfflineSaveForLaterAdapter(activity)
        binding.saveForLaterRecyclerView.adapter = offlineSaveForLaterAdapter


        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_VARIANTS_OFFLINE] = Constant.GetVal
        params[Constant.VARIANT_IDs] =
            databaseHelper.saveForLaterList().toString().replace("[", "").replace("]", "")
                .replace("\"", "")
        if (session.getData(Constant.SHIPPING_TYPE)
                .equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                Constant.GET_SELECTED_PINCODE_ID
            ) != "0"
        ) {
            params[Constant.PINCODE] =
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val jsonArray = jsonObject.getJSONArray(Constant.DATA)
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                val cart =
                                    Gson().fromJson(jsonObject1.toString(), OfflineCart::class.java)
                                offlineSaveForLaterItems.add(cart)
                            }
                            binding.lytSaveForLater.visibility = View.VISIBLE
                            offlineSaveForLaterAdapter = OfflineSaveForLaterAdapter(activity)
                            binding.saveForLaterRecyclerView.adapter = offlineSaveForLaterAdapter
                            tvSaveForLaterTitle.text =
                                activity.resources.getString(R.string.save_for_later) + " (" + offlineSaveForLaterItems.size + ")"
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                        } else {
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.lytSaveForLater.visibility = View.GONE
                        }
                    } catch (e: JSONException) {
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.lytSaveForLater.visibility = View.GONE
                    }
                } else {
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.lytSaveForLater.visibility = View.GONE
                }
            }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    private fun getSettings(activity: Activity) {
        Constant.FLOAT_TOTAL_AMOUNT = 0.00
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val session = Session(activity)
        val params: MutableMap<String, String> = HashMap()
        params[Constant.SETTINGS] = Constant.GetVal
        params[Constant.GET_TIMEZONE] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {


                            binding.tvLocation.text =
                                session.getData(Constant.GET_SELECTED_PINCODE_NAME)

                            val `object` = jsonObject.getJSONObject(Constant.SETTINGS)
                            session.setData(
                                Constant.minimum_version_required,
                                `object`.getString(Constant.minimum_version_required)
                            )
                            session.setData(
                                Constant.is_version_system_on,
                                `object`.getString(Constant.is_version_system_on)
                            )
                            session.setData(
                                Constant.CURRENCY,
                                `object`.getString(Constant.CURRENCY)
                            )
                            session.setData(
                                Constant.min_order_amount,
                                `object`.getString(Constant.min_order_amount)
                            )
                            session.setData(
                                Constant.max_cart_items_count,
                                `object`.getString(Constant.max_cart_items_count)
                            )
                            session.setData(
                                Constant.area_wise_delivery_charge,
                                `object`.getString(Constant.area_wise_delivery_charge)
                            )
                            if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                                cartData()
                            } else {
                                offlineCarts = ArrayList()
                                offlineCartAdapter = OfflineCartAdapter(activity)
                                binding.cartRecycleView.adapter = offlineCartAdapter
                                offlineSaveForLaterItems = ArrayList()
                                offlineSaveForLaterAdapter = OfflineSaveForLaterAdapter(activity)
                                binding.saveForLaterRecyclerView.adapter =
                                    offlineSaveForLaterAdapter
                                getOfflineCart()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, false)
    }

    private fun cartData() {
        isDeliverable = false
        carts = ArrayList()
        cartAdapter = CartAdapter(activity)
        binding.cartRecycleView.adapter = cartAdapter
        saveForLater = ArrayList()
        saveForLaterAdapter = SaveForLaterAdapter(activity)
        binding.saveForLaterRecyclerView.adapter = saveForLaterAdapter
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_USER_CART] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        if (session.getData(Constant.SHIPPING_TYPE)
                .equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                Constant.GET_SELECTED_PINCODE_ID
            ) != "0"
        ) {
            params[Constant.PINCODE_ID] =
                session.getData(Constant.GET_SELECTED_PINCODE_ID).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val cart =
                                        Gson().fromJson(jsonObject1.toString(), Cart::class.java)
                                    variantIdList.add(cart.product_variant_id)
                                    qtyList.add(cart.qty)
                                    var price: Double
                                    var taxPercentage = "0"
                                    try {
                                        taxPercentage =
                                            if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    price =
                                        if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                                            (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                                        } else {
                                            (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                                        }
                                    Constant.FLOAT_TOTAL_AMOUNT += price * cart.qty.toDouble()
                                    carts.add(cart)
                                } else {
                                    break
                                }
                            }

                            val jsonArraySaveForLater =
                                `object`.getJSONArray(Constant.SAVE_FOR_LATER)
                            for (i in 0 until jsonArraySaveForLater.length()) {
                                val jsonObject1 = jsonArraySaveForLater.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val cart =
                                        Gson().fromJson(jsonObject1.toString(), Cart::class.java)
                                    saveForLater.add(cart)
                                } else {
                                    break
                                }
                            }

                            cartAdapter = CartAdapter(activity)
                            binding.cartRecycleView.adapter = cartAdapter
                            tvSaveForLaterTitle.text =
                                activity.resources.getString(R.string.save_for_later) + " (" + saveForLater.size + ")"
                            if (jsonArraySaveForLater.length() == 0) {
                                binding.lytSaveForLater.visibility = View.GONE
                            } else {
                                binding.lytSaveForLater.visibility = View.VISIBLE
                                saveForLaterAdapter = SaveForLaterAdapter(activity)
                                binding.saveForLaterRecyclerView.adapter = saveForLaterAdapter
                            }
                            binding.lytTotal.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            Constant.TOTAL_CART_ITEM = jsonObject.getString(Constant.TOTAL).toInt()
                        } else {
                            binding.lytEmpty.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            lytEmpty.visibility = View.VISIBLE
                            binding.lytTotal.visibility = View.GONE
                        }
                        setData(activity)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.lytEmpty.visibility = View.VISIBLE
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                    }
                }
            }
        }, activity, Constant.CART_URL, params, false)
    }

    /*   Promo Code Part Start   */
    @SuppressLint("ClickableViewAccessibility")
    fun openDialog(activity: Activity) {
        offset = 0
        val alertDialog = AlertDialog.Builder(
            activity
        )
        val inflater1 = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater1.inflate(R.layout.dialog_promo_code_selection, null)
        alertDialog.setView(dialogView)
        alertDialog.setCancelable(true)
        val dialog = alertDialog.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val scrollView: NestedScrollView = dialogView.findViewById(R.id.scrollView)
        val tvAlert: TextView = dialogView.findViewById(R.id.tvAlert)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCancel)
        val recyclerViewTimeSlot: RecyclerView = dialogView.findViewById(R.id.recyclerView)
        val shimmerFrameLayout: ShimmerFrameLayout =
            dialogView.findViewById(R.id.shimmerFrameLayout)
        val linearLayoutManager = LinearLayoutManager(activity)
        recyclerViewTimeSlot.layoutManager = linearLayoutManager
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()
        tvAlert.text = getString(R.string.no_promo_code_found)
        btnCancel.setOnClickListener { dialog.dismiss() }
        getPromoCodes(
            recyclerViewTimeSlot,
            tvAlert,
            linearLayoutManager,
            scrollView,
            dialog,
            shimmerFrameLayout
        )
        dialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getPromoCodes(
        recyclerViewTimeSlot: RecyclerView,
        tvAlert: TextView,
        linearLayoutManager: LinearLayoutManager,
        scrollView: NestedScrollView,
        dialog: AlertDialog,
        shimmerFrameLayout: ShimmerFrameLayout
    ) {
        promoCodes = ArrayList()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_PROMO_CODES] = Constant.GetVal
        params[Constant.USER_ID] = "" + session.getData(Constant.ID).toString()
        params[Constant.AMOUNT] = Constant.FLOAT_TOTAL_AMOUNT.toString()
        params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
        params[Constant.OFFSET] = "" + offset
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            try {
                                total = jsonObject.getString(Constant.TOTAL).toInt().toDouble()
                                val `object` = JSONObject(response)
                                val jsonArray = `object`.getJSONArray(Constant.DATA)
                                for (i in 0 until jsonArray.length()) {
                                    val jsonObject1 = jsonArray.getJSONObject(i)
                                    val promoCode =
                                        Gson().fromJson(
                                            jsonObject1.toString(),
                                            PromoCode::class.java
                                        )
                                    promoCodes.add(promoCode)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (offset == 0) {
                                recyclerViewTimeSlot.visibility = View.VISIBLE
                                tvAlert.visibility = View.GONE
                                promoCodeAdapter = PromoCodeAdapter(activity, promoCodes, dialog)
                                promoCodeAdapter.setHasStableIds(true)
                                recyclerViewTimeSlot.adapter = promoCodeAdapter
                                shimmerFrameLayout.visibility = View.GONE
                                shimmerFrameLayout.stopShimmer()
                                scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                        if (promoCodes.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == promoCodes.size - 1) {
                                                    //bottom of list!

                                                    offset += Constant.LOAD_ITEM_LIMIT
                                                    val params1: MutableMap<String, String> =
                                                        HashMap()
                                                    params1[Constant.GET_PROMO_CODES] =
                                                        Constant.GetVal
                                                    params1[Constant.USER_ID] =
                                                        "" + session.getData(Constant.ID).toString()
                                                    params1[Constant.AMOUNT] =
                                                        Constant.FLOAT_TOTAL_AMOUNT.toString()
                                                    params1[Constant.LIMIT] =
                                                        "" + Constant.LOAD_ITEM_LIMIT
                                                    params1[Constant.OFFSET] = "" + offset
                                                    ApiConfig.requestToVolley(
                                                        object :
                                                            VolleyCallback {
                                                            override fun onSuccess(
                                                                result: Boolean,
                                                                response: String
                                                            ) {
                                                                if (result) {
                                                                    try {
                                                                        val jsonObject1 =
                                                                            JSONObject(
                                                                                response
                                                                            )
                                                                        if (!jsonObject1.getBoolean(
                                                                                Constant.ERROR
                                                                            )
                                                                        ) {
                                                                            val `object` =
                                                                                JSONObject(
                                                                                    response
                                                                                )
                                                                            val jsonArray =
                                                                                `object`.getJSONArray(
                                                                                    Constant.DATA
                                                                                )

                                                                            for (i in 0 until jsonArray.length()) {
                                                                                val jsonObject2 =
                                                                                    jsonArray.getJSONObject(
                                                                                        i
                                                                                    )
                                                                                val promoCode =
                                                                                    Gson().fromJson(
                                                                                        jsonObject2.toString(),
                                                                                        PromoCode::class.java
                                                                                    )
                                                                                promoCodes.add(
                                                                                    promoCode
                                                                                )
                                                                            }
                                                                            promoCodeAdapter.notifyDataSetChanged()
                                                                            promoCodeAdapter.setLoaded()
                                                                            isLoadMore = false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        activity,
                                                        Constant.PROMO_CODE_CHECK_URL,
                                                        params1,
                                                        false
                                                    )
                                                }
                                                isLoadMore = true
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            shimmerFrameLayout.visibility = View.GONE
                            shimmerFrameLayout.stopShimmer()
                            recyclerViewTimeSlot.visibility = View.GONE
                            tvAlert.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        shimmerFrameLayout.visibility = View.GONE
                        shimmerFrameLayout.stopShimmer()
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.PROMO_CODE_CHECK_URL, params, false)
    }

    inner class PromoCodeAdapter(
        val activity: Activity,
        private val promoCodes: ArrayList<PromoCode?>,
        val dialog: AlertDialog
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // for load more
        val viewTypeItem = 0
        val viewTypeLoading = 1
        private var isLoading = false
        val session: Session = Session(activity)
        fun add(position: Int, promoCode: PromoCode?) {
            promoCodes.add(position, promoCode)
            notifyItemInserted(position)
        }

        fun setLoaded() {
            isLoading = false
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val view: View
            return when (viewType) {
                viewTypeItem -> {
                    view =
                        LayoutInflater.from(activity)
                            .inflate(R.layout.lyt_promo_code_list, parent, false)
                    HolderItems(view)
                }
                viewTypeLoading -> {
                    view = LayoutInflater.from(activity)
                        .inflate(R.layout.item_progressbar, parent, false)
                    ViewHolderLoading(view)
                }
                else -> throw IllegalArgumentException("unexpected viewType: $viewType")
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(
            holderParent: RecyclerView.ViewHolder,
            position: Int
        ) {
            if (holderParent is HolderItems) {
                try {
                    val promoCode = promoCodes[position]
                    holderParent.tvMessage.text = promoCode!!.message
                    holderParent.tvPromoCode.text = promoCode.promo_code
                    if (java.lang.Boolean.parseBoolean(promoCode.is_validate[0].error)) {
                        holderParent.tvMessageAlert.setTextColor(
                            ContextCompat.getColor(
                                activity, R.color.tx_promo_code_fail
                            )
                        )
                        holderParent.tvMessageAlert.text = promoCode.is_validate[0].message
                        holderParent.tvApply.setTextColor(
                            ContextCompat.getColor(
                                activity, R.color.gray
                            )
                        )
                    } else {
                        holderParent.tvMessageAlert.setTextColor(
                            ContextCompat.getColor(
                                activity, R.color.colorPrimary
                            )
                        )
                        holderParent.tvMessageAlert.text =
                            activity.getString(R.string.you_will_save) + session.getData(
                                Constant.CURRENCY
                            ) + promoCode.is_validate[0].discount + activity.getString(R.string.with_this_code)
                        holderParent.tvApply.setTextColor(
                            ContextCompat.getColor(
                                activity, R.color.colorPrimary
                            )
                        )
                    }
                    if (pCode == promoCode.promo_code) {
                        holderParent.tvApply.isEnabled = false
                        holderParent.tvApply.text = activity.getString(R.string.applied)
                        holderParent.tvApply.setTextColor(
                            ContextCompat.getColor(
                                activity, R.color.green
                            )
                        )
                    } else {
                        holderParent.tvApply.isEnabled = true
                    }
                    holderParent.tvApply.setOnClickListener {
                        try {
                            if (!java.lang.Boolean.parseBoolean(promoCode.is_validate[0].error)) {
                                pCode = promoCode.promo_code
                                binding.btnRemoveOffer.visibility = View.VISIBLE
                                binding.btnRemoveOffer.text =
                                    activity.getString(R.string.remove_offer)
                                binding.btnRemoveOffer.tag = "applied"
                                isApplied = true
                                pCode = promoCode.promo_code
                                binding.tvPromoCode.text =
                                    activity.getString(R.string.applied) + " " + pCode
                                pCodeDiscount = promoCode.is_validate[0].discount.toDouble()
                                subTotal = promoCode.is_validate[0].discounted_amount.toDouble()
                                binding.tvTotalAmount.text =
                                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                                        "" + subTotal
                                    )
                                binding.lytPromoDiscount.visibility = View.VISIBLE
                                binding.tvPromoDiscount.text =
                                    "-" + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                                        "" + promoCode.is_validate[0].discount
                                    )
                                dialog.dismiss()
                                openPartyDialog(session, activity)
                            } else {
                                ObjectAnimator.ofFloat(
                                    holderParent.tvMessageAlert,
                                    "translationX",
                                    0f,
                                    25f,
                                    -25f,
                                    25f,
                                    -25f,
                                    15f,
                                    -15f,
                                    6f,
                                    -6f,
                                    0f
                                ).setDuration(300).start()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (holderParent is ViewHolderLoading) {
                holderParent.progressBar.isIndeterminate = true
            }
        }

        private fun openPartyDialog(session: Session, activity: Activity) {
            try {
                binding.lytAppliedPromoCode.visibility = View.VISIBLE
                binding.lottieAnimationViewParty.setAnimation("celebration.json")
                binding.lottieAnimationViewParty.playAnimation()
                binding.lottieAnimationViewSmile.setAnimation("promo_applied.json")
                binding.lottieAnimationViewSmile.playAnimation()
                binding.tvAppliedPromoCodeAmount.text =
                    activity.getString(R.string.you_saved) + session.getData(
                        Constant.CURRENCY
                    ) + pCodeDiscount
                binding.tvAppliedPromoCode.text =
                    activity.getString(R.string.with) + "\"" + pCode + "\"" + activity.getString(
                        R.string.code
                    )
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.lytAppliedPromoCode.visibility = View.GONE
                    binding.lottieAnimationViewParty.clearAnimation()
                    binding.lottieAnimationViewSmile.clearAnimation()
                }, 4000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun getItemCount(): Int {
            return promoCodes.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (promoCodes[position] == null) viewTypeLoading else viewTypeItem
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        internal inner class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
            val progressBar: ProgressBar = view.findViewById(R.id.itemProgressbar)

        }

        internal inner class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
            val tvPromoCode: TextView = itemView.findViewById(R.id.tvPromoCode)
            val tvMessageAlert: TextView = itemView.findViewById(R.id.tvMessageAlert)
            val tvApply: TextView = itemView.findViewById(R.id.tvApply)

        }

    }

    /*   Promo Code Part End   */
    override fun onPause() {
        super.onPause()
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            if (Constant.CartValues.isNotEmpty()) {
                ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.cart)
        activity.invalidateOptionsMenu()
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
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }

    companion object {

        lateinit var carts: ArrayList<Cart>
        lateinit var saveForLater: ArrayList<Cart>
        lateinit var offlineCarts: ArrayList<OfflineCart>
        lateinit var offlineSaveForLaterItems: ArrayList<OfflineCart>
        lateinit var jsonObject: JSONObject
        lateinit var saveForLaterValues: MutableMap<String, String>
        lateinit var refreshListener: OnRefreshListener

        var isDeliverable = false
        var isSoldOut = false
        var pCode = ""

        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentCartBinding

        @SuppressLint("StaticFieldLeak")
        lateinit var cartAdapter: CartAdapter

        @SuppressLint("StaticFieldLeak")
        lateinit var saveForLaterAdapter: SaveForLaterAdapter

        @SuppressLint("StaticFieldLeak")
        lateinit var offlineCartAdapter: OfflineCartAdapter

        @SuppressLint("StaticFieldLeak")
        lateinit var offlineSaveForLaterAdapter: OfflineSaveForLaterAdapter

        @SuppressLint("StaticFieldLeak")
        lateinit var session: Session

        @SuppressLint("StaticFieldLeak")
        lateinit var lytTotal: RelativeLayout

        @SuppressLint("StaticFieldLeak")
        lateinit var lytEmpty: LinearLayout

        @SuppressLint("StaticFieldLeak")
        lateinit var tvSaveForLaterTitle: TextView

        @SuppressLint("StaticFieldLeak")
        lateinit var tvLocation: TextView

        @SuppressLint("StaticFieldLeak")
        lateinit var lytSaveForLater: LinearLayout

        fun setData(activity: Activity) {
            binding.tvTotalAmount.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    Constant.FLOAT_TOTAL_AMOUNT.toString()
                )
            val count: Int = if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                carts.size
            } else {
                offlineCarts.size
            }
            binding.tvTotalItems.text =
                count.toString() + if (count > 1) activity.getString(R.string.items) else activity.getString(
                    R.string.item
                )
            binding.btnRemoveOffer.performClick()
        }
    }
}
