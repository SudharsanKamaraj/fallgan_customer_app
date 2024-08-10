package com.fallgan.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.adapter.AdapterStyle1
import com.fallgan.customerapp.adapter.CustomAdapter
import com.fallgan.customerapp.adapter.SliderAdapter
import com.fallgan.customerapp.databinding.FragmentProductDetailBinding
import com.fallgan.customerapp.helper.*
import com.fallgan.customerapp.model.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailFragment : Fragment() {
    lateinit var binding: FragmentProductDetailBinding
    lateinit var session: Session
    lateinit var root: View
    lateinit var from: String
    lateinit var id: String
    lateinit var product: Product
    lateinit var databaseHelper: DatabaseHelper
    lateinit var activity: Activity
    lateinit var taxPercentage: String

    private var favorite = false
    private var isLogin = false
    var variantPosition = 0

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentProductDetailBinding.inflate(inflater,container,false)
        root = binding.root
        setHasOptionsMenu(true)
        activity = requireActivity()
        Constant.CartValues = HashMap()
        sliderArrayList = ArrayList()
        session = Session(activity)
        isLogin = session.getBoolean(Constant.IS_USER_LOGIN)
        databaseHelper = DatabaseHelper(activity)
        from = requireArguments().getString(Constant.FROM).toString()
        taxPercentage = "0"
        assert(arguments != null)
        variantPosition = requireArguments().getInt(Constant.VARIANT_POSITION, 0)
        id = requireArguments().getString(Constant.ID).toString()

        binding.tvSeller.paintFlags = binding.tvSeller.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        binding.lottieAnimationView.setAnimation("add_to_wish_list.json")

        getProductDetail(id)
        ApiConfig.getSettings(activity)
        binding.tvMore.setOnClickListener { showSimilar() }
        binding.lytSimilar.setOnClickListener { showSimilar() }
        binding.btnCart.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, CartFragment())
                .addToBackStack(null).commit()
        }
        binding.lytShare.setOnClickListener {
            val message = Constant.WebSiteUrl + "product/" + product.slug
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            sendIntent.putExtra(Intent.EXTRA_TEXT, message)
            sendIntent.type = "text/plain"
            val shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_via))
            startActivity(shareIntent)
        }
        binding.btnChangePinCode.setOnClickListener {
            if (binding.btnChangePinCode.tag == "checkLoc") {
                val pinCode = binding.edtPinCode.text.toString().trim()
                session.setData(Constant.GET_SELECTED_PINCODE_NAME, pinCode)
                if (pinCode.isNotEmpty()) {
                    showKeyboardWithFocus(binding.edtPinCode, activity, false)
                    binding.btnChangePinCode.tag = "changeLoc"
                    binding.btnChangePinCode.text = activity.getString(R.string.change)
                    binding.edtPinCode.visibility = View.GONE
                    binding.tvPinCode.visibility = View.VISIBLE
                    checkDelivery(product.variants[variantPosition], pinCode, true)
                } else {
                    binding.edtPinCode.error = activity.getString(R.string.enter_valid_pin_code)
                    showKeyboardWithFocus(binding.edtPinCode, activity, true)
                }
            } else {
                binding.edtPinCode.visibility = View.VISIBLE
                binding.tvPinCode.visibility = View.GONE
                showKeyboardWithFocus(binding.edtPinCode, activity, true)
                binding.btnChangePinCode.tag = "checkLoc"
                binding.btnChangePinCode.text = activity.getString(R.string.check)
            }
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    fun checkDelivery(priceVariation: PriceVariation, pinCode: String, isLoading: Boolean) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.CHECK_DELIEVABILITY] = Constant.GetVal
        params[Constant.PRODUCT_VARIANT_ID] = priceVariation.id
        params[Constant.PINCODE] = pinCode
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        binding.tvPinCode.setTextColor(
                            ContextCompat.getColor(
                                activity,
                                R.color.colorPrimary
                            )
                        )
                        binding.tvPinCode.text = activity.getString(R.string.available_at) + pinCode
                        if (jsonObject.has(Constant.DELIVERY_CHARGE_WITHOUT_COD) && jsonObject.has(
                                Constant.DELIVERY_CHARGE_WITH_COD
                            )
                        ) {
                            binding.lytDeliveryCharge.visibility = View.VISIBLE
                            binding.tvCodDeliveryCharges.text =
                                activity.getString(R.string.cod_payment) + session.getData(
                                    Constant.CURRENCY
                                ) + jsonObject.getString(Constant.DELIVERY_CHARGE_WITH_COD)
                            binding.tvOnlineDeliveryCharges.text =
                                activity.getString(R.string.online_payment) + session.getData(
                                    Constant.CURRENCY
                                ) + jsonObject.getString(Constant.DELIVERY_CHARGE_WITHOUT_COD)
                            binding.tvOnlineDeliveryChargesDate.text =
                                activity.getString(R.string.delivery_by) + jsonObject.getString(
                                    Constant.ESTIMATED_DATE
                                ) + " - " + getDay(jsonObject.getString(Constant.ESTIMATED_DATE))
                        }
                    } else {
                        binding.tvPinCode.setTextColor(ContextCompat.getColor(activity, R.color.red))
                        binding.tvPinCode.text = activity.getString(R.string.not_available_at) + pinCode
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
        }, activity, Constant.GET_PRODUCTS_URL, params, isLoading)
    }

    private fun showKeyboardWithFocus(v: View?, a: Activity?, isVisible: Boolean) {
        try {
            val imm = a!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (isVisible) {
                v!!.requestFocus()
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
                a.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            } else {
                v!!.clearFocus()
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                a.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSimilar() {
        val fragment: Fragment = ProductListFragment()
        val bundle = Bundle()
        bundle.putString(Constant.ID, product.variants[0].product_id)
        bundle.putString("cat_id", product.category_id)
        bundle.putString(Constant.FROM, "similar")
        bundle.putString("name", "Similar Products")
        fragment.arguments = bundle
        MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit()
    }

    private fun getSimilarData(product: Product?) {
        val productArrayList = ArrayList<Product>()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_SIMILAR_PRODUCT] = Constant.GetVal
        params[Constant.PRODUCT_ID] = product!!.variants[0].product_id
        params[Constant.CATEGORY_ID] = product.category_id
        if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0") {
            params[Constant.PINCODE] = session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        binding.recyclerView.layoutManager =
                            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                        val jsonArray = jsonObject.getJSONArray(Constant.DATA)
                        try {
                            productArrayList.addAll(ApiConfig.getProductList(jsonArray))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val adapter =
                            AdapterStyle1(activity, productArrayList, R.layout.offer_layout)
                        binding.recyclerView.adapter = adapter
                        binding.relativeLayout.visibility = View.VISIBLE
                    } else {
                        binding.relativeLayout.visibility = View.GONE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    private fun getProductDetail(productId: String) {
        binding.scrollView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ALL_PRODUCTS] = Constant.GetVal
        if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0") {
            params[Constant.PINCODE] = session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        if (from == "share") {
            params[Constant.SLUG] = productId
        } else {
            params[Constant.PRODUCT_ID] = productId
        }
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject1 = JSONObject(response)
                    if (!jsonObject1.getBoolean(Constant.ERROR)) {
                        val `object` = JSONObject(response)
                        val jsonArray = `object`.getJSONArray(Constant.DATA)
                        try {
                            for (i in 0 until jsonArray.length()) {
                                product = Gson().fromJson(
                                    jsonArray.getJSONObject(i).toString(),
                                    Product::class.java
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        setProductDetail(product)
                        getSimilarData(product)
                    }
                    binding.scrollView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.scrollView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                }
            }
        }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    fun setProductDetail(product: Product?) {
        try {
            binding.tvProductName.text = product!!.name
            try {
                taxPercentage =
                    if (product.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (product.made_in.isNotEmpty()) {
                binding.lytMadeIn.visibility = View.VISIBLE
                binding.tvMadeIn.text = product.made_in
            }
            if (product.manufacturer.isNotEmpty()) {
                binding.lytMfg.visibility = View.VISIBLE
                binding.tvMfg.text = product.manufacturer
            }
            binding.tvSeller.text = product.seller_name
            if (session.getBoolean(Constant.GET_SELECTED_PINCODE)) {
                if (session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString() != activity.getString(R.string.all)) {
                    binding.tvPinCode.text =
                        activity.getString(R.string.available_at) + session.getData(
                            Constant.GET_SELECTED_PINCODE_NAME
                        )
                }
            }
            binding.tvSeller.setOnClickListener {
                val fragment: Fragment = SellerProductsFragment()
                val bundle = Bundle()
                bundle.putString(Constant.ID, product.seller_id)
                bundle.putString(Constant.TITLE, product.seller_name)
                bundle.putString(Constant.FROM, from)
                fragment.arguments = bundle
                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            if (isLogin) {
                if (product.is_favorite) {
                    favorite = true
                    binding.imgFav.setImageResource(R.drawable.ic_is_favorite)
                } else {
                    favorite = false
                    binding.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                }
            } else {
                if (databaseHelper.getFavoriteById(product.variants[0].product_id)) {
                    binding.imgFav.setImageResource(R.drawable.ic_is_favorite)
                } else {
                    binding.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                }
            }
            if (isLogin) {
                if (Constant.CartValues.containsKey(product.variants[0].id)) {
                    binding.tvQuantity.text = "" + Constant.CartValues[product.variants[0].id]
                } else {
                    binding.tvQuantity.text = product.variants[0].cart_count
                }
            } else {
                binding.tvQuantity.text = databaseHelper.CheckCartItemExist(
                    product.variants[0].id,
                    product.variants[0].product_id
                )
            }
            if (product.return_status.equals("1", ignoreCase = true)) {
                binding.imgReturnable.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity, R.drawable.ic_returnable
                    )
                )
                binding.tvReturnable.text =
                    product.return_days + getString(R.string.days) + getString(R.string.returnable)
            } else {
                binding.imgReturnable.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity, R.drawable.ic_not_returnable
                    )
                )
                binding.tvReturnable.text = getString(R.string.not_returnable)
            }
            if (product.cancelable_status.equals("1", ignoreCase = true)) {
                binding.imgCancellable.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity, R.drawable.ic_cancellable
                    )
                )
                binding.tvCancellable.text = getString(R.string.cancellable_till) + ApiConfig.toTitleCase(
                    product.till_status
                )
            } else {
                binding.imgCancellable.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity, R.drawable.ic_not_cancellable
                    )
                )
                binding.tvCancellable.text = getString(R.string.not_cancellable)
            }
            if (product.variants.size == 1) {
                binding.spinner.visibility = View.INVISIBLE
                binding.lytSpinner.visibility = View.INVISIBLE
                binding.lytMainPrice.isEnabled = false
                session.setData(Constant.PRODUCT_VARIANT_ID, "" + 0)
                setSelectedData(product.variants[0])
            }

            if (product.indicator != "0") {
                binding.imgIndicator.visibility = View.VISIBLE
                if (product.indicator == "1") binding.imgIndicator.setImageResource(R.drawable.ic_veg_icon) else if (product.indicator == "2") binding.imgIndicator.setImageResource(
                    R.drawable.ic_non_veg_icon
                )
            }



            binding.webViewDescription.isVerticalScrollBarEnabled = true
            binding.webViewDescription.loadDataWithBaseURL("", product.description, "text/html", "UTF-8", "")
            binding.webViewDescription.setBackgroundColor(
                ContextCompat.getColor(
                    activity, R.color.white
                )
            )
            binding.tvProductName.text = product.name
            binding.spinner.setSelection(variantPosition)
            binding.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
                override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
                override fun onPageSelected(variantPosition: Int) {
                    ApiConfig.addMarkers(variantPosition, sliderArrayList, binding.markersLayout, activity)
                }

                override fun onPageScrollStateChanged(i: Int) {}
            })

            val variantsName = arrayOfNulls<String>(product.variants.size)
            val variantsStockStatus = arrayOfNulls<String>(product.variants.size)

            for ((i, name) in product.variants.withIndex()) {
                variantsName[i] = name.measurement + " " + name.measurement_unit_name
                variantsStockStatus[i] = name.serve_for
            }

            binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View,
                    i: Int,
                    l: Long
                ) {
                    variantPosition = i
                    session.setData(Constant.PRODUCT_VARIANT_ID, "" + i)
                    setSelectedData(product.variants[i])
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }


            val customAdapter = CustomAdapter(activity, variantsName, variantsStockStatus)
            binding.spinner.adapter = customAdapter

            binding.scrollView.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.app_name)
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

    private fun getDay(date: String?): String {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
            cal.time = Objects.requireNonNull(sdf.parse(date.toString())) // all done
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return ApiConfig.getDayOfWeek(cal[Calendar.DAY_OF_WEEK], activity).toString()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    fun setSelectedData(priceVariation: PriceVariation) {
        binding.tvMeasurement.text =
            " ( " + priceVariation.measurement + priceVariation.measurement_unit_name + " ) "
        sliderArrayList = ArrayList()
        sliderArrayList.add(
            Slider(
                product.image
            )
        )
        if (priceVariation.images.size != 0) {
            val arrayList = priceVariation.images
            for (i in arrayList.indices) {
                sliderArrayList.add(
                    Slider(
                        arrayList[i]
                    )
                )
            }
        } else {
            val arrayList = product.other_images
            for (i in arrayList.indices) {
                sliderArrayList.add(
                    Slider(
                        arrayList[i]
                    )
                )
            }
        }
        if (session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString() != activity.getString(R.string.all) && session.getData(
                Constant.GET_SELECTED_PINCODE_NAME
            ) != "" && session.getData(Constant.SHIPPING_TYPE) != "local"
        ) {
            checkDelivery(
                product.variants[variantPosition],
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString(),
                false
            )
        }
        binding.viewPager.adapter = SliderAdapter(sliderArrayList, activity, R.layout.lyt_detail_slider, "detail")
        ApiConfig.addMarkers(0, sliderArrayList, binding.markersLayout, activity)
        val discountedPrice: Double
        val originalPrice: Double
        var taxPercentage = "0"
        try {
            taxPercentage =
                if (product.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (priceVariation.discounted_price == "0" || priceVariation.discounted_price == "") {
            binding.showDiscount.visibility = View.GONE
            binding.tvOriginalPrice.visibility = View.GONE
            discountedPrice =
                (priceVariation.price.toFloat() + priceVariation.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
        } else {
            binding.tvOriginalPrice.visibility = View.VISIBLE
            discountedPrice =
                (priceVariation.discounted_price.toFloat() + priceVariation.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            originalPrice =
                (priceVariation.price.toFloat() + priceVariation.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            binding.tvOriginalPrice.paintFlags =
                    binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.tvOriginalPrice.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + originalPrice)
            binding.showDiscount.visibility = View.VISIBLE
            binding.showDiscount.text = "-" + ApiConfig.getDiscount(originalPrice, discountedPrice)
        }
        binding.tvPrice.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + discountedPrice)
        if (isLogin) {
            if (Constant.CartValues.containsKey(priceVariation.id)) {
                binding.tvQuantity.text = Constant.CartValues[priceVariation.id]
            } else {
                binding.tvQuantity.text = priceVariation.cart_count
            }
        } else {
            binding.tvQuantity.text = databaseHelper.CheckCartItemExist(priceVariation.id, priceVariation.product_id)
        }
        val maxCartCont: String? = if (product.total_allowed_quantity == null || product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
            session.getData(Constant.max_cart_items_count)
        } else {
            product.total_allowed_quantity
        }

        binding.btnAddQuantity.setOnClickListener {
            addQuantity(
                priceVariation,
                true,
                maxCartCont
            )
        }

        binding.btnMinusQuantity.setOnClickListener {
            addQuantity(
                priceVariation,
                false,
                maxCartCont
            )
        }

        binding.btnAddToCart.setOnClickListener {  addQuantity(priceVariation, true, maxCartCont) }

        if (isLogin) {
            if (priceVariation.cart_count == "0") {
                binding.btnAddToCart.visibility = View.VISIBLE
            } else {
                binding.btnAddToCart.visibility = View.GONE
            }
        } else {
            if (databaseHelper.CheckCartItemExist(
                    priceVariation.id,
                    priceVariation.product_id
                ) != "0" || databaseHelper.CheckCartItemExist(
                    priceVariation.id,
                    priceVariation.product_id
                ) == null
            ) {
                binding.btnAddToCart.visibility = View.GONE
            } else {
                binding.btnAddToCart.visibility = View.VISIBLE
            }
        }
        binding.lytSave.setOnClickListener {
            if (isLogin) {
                favorite = product.is_favorite
                if (favorite) {
                    favorite = false
                    binding.lottieAnimationView.visibility = View.GONE
                    product.is_favorite = false
                    binding.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                } else {
                    favorite = true
                    product.is_favorite = true
                    binding.lottieAnimationView.visibility = View.VISIBLE
                    binding.lottieAnimationView.playAnimation()
                }
                ApiConfig.addOrRemoveFavorite(
                    activity,
                    session,
                    priceVariation.product_id,
                    favorite
                )
            } else {
                favorite = databaseHelper.getFavoriteById(product.variants[0].product_id)
                if (favorite) {
                    favorite = false
                    binding.lottieAnimationView.visibility = View.GONE
                    binding.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                } else {
                    favorite = true
                    binding.lottieAnimationView.visibility = View.VISIBLE
                    binding.lottieAnimationView.playAnimation()
                }
                databaseHelper.addOrRemoveFavorite(product.variants[0].product_id, favorite)
            }
            when (from) {
                "fragment", "sub_cate", "search" -> {
                    ProductListFragment.productArrayList[variantPosition]!!.is_favorite =
                        favorite
                    ProductListFragment.mAdapter.notifyDataSetChanged()
                }
                "favorite" -> {
                    if (favorite) {
                        FavoriteFragment.favoriteArrayList.add(product)
                    } else {
                        FavoriteFragment.favoriteArrayList.remove(product)
                    }
                    FavoriteFragment.favoriteLoadMoreAdapter.notifyDataSetChanged()
                }
                "seller" -> {
                    SellerProductsFragment.productArrayList[variantPosition]!!.is_favorite =
                        favorite
                    SellerProductsFragment.mAdapter.notifyDataSetChanged()
                }
            }
        }
        if (priceVariation.serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
            binding.tvStatus.visibility = View.VISIBLE
            binding.lytQuantity.visibility = View.GONE
        } else {
            binding.tvStatus.visibility = View.GONE
            binding.lytQuantity.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    fun addQuantity(
        extra: PriceVariation,
        isAdd: Boolean,
        maxCartCont: String?
    ) {
        try {
            if (session.getData(Constant.STATUS) == "1") {
                var count = binding.tvQuantity.text.toString().toInt()
                if (isAdd) {
                    count++
                    if (extra.stock.toFloat() >= count) {
                        if (maxCartCont!!.toFloat() >= count) {
                            extra.cart_count = "" + count
                            binding.tvQuantity.text = "" + count
                            if (isLogin) {
                                if (Constant.CartValues.containsKey(extra.id)) {
                                    Constant.CartValues.replace(extra.id, "" + count)
                                } else {
                                    Constant.CartValues[extra.id] = "" + count
                                }
                                ApiConfig.addMultipleProductInCart(
                                    session,
                                    activity,
                                    Constant.CartValues
                                )
                            } else {
                                databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                                databaseHelper.getTotalItemOfCart(activity)
                                activity.invalidateOptionsMenu()
                            }
                        } else {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.limit_alert),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.stock_limit),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    count--
                    extra.cart_count = "" + count
                    binding.tvQuantity.text = "" + count
                    if (isLogin) {
                        if (Constant.CartValues.containsKey(extra.id)) {
                            Constant.CartValues.replace(extra.id, "" + count)
                        } else {
                            Constant.CartValues[extra.id] = "" + count
                        }
                        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                    } else {
                        databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                        databaseHelper.getTotalItemOfCart(activity)
                        activity.invalidateOptionsMenu()
                    }
                }
                if (count == 0) {
                    binding.btnAddToCart.visibility = View.VISIBLE
                } else {
                    binding.btnAddToCart.visibility = View.GONE
                }
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.user_block_msg),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_cart).icon = ApiConfig.buildCounterDrawable(
            Constant.TOTAL_CART_ITEM,
            activity
        )
        activity.invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
    }

    companion object {
        lateinit var sliderArrayList: ArrayList<Slider>
    }
}