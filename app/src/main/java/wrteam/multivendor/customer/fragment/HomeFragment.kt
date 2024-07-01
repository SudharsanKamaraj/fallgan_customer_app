package com.gpn.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.activity.MainActivity
import com.gpn.customerapp.adapter.*
import com.gpn.customerapp.databinding.FragmentHomeBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.ApiConfig.Companion.requestToVolley
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Category
import com.gpn.customerapp.model.Seller
import com.gpn.customerapp.model.Slider
import java.util.*

class
HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    lateinit var session: Session
    lateinit var sliderArrayList: ArrayList<Slider>
    lateinit var activity: Activity
    lateinit var root: View
    private lateinit var swipeTimer: Timer
    lateinit var handler: Handler
    lateinit var update: Runnable
    lateinit var menu: Menu

    private var timerDelay = 0
    private var timerWaiting = 0
    var size = 0
    private var currentPage = 0
    private var searchVisible = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        activity = requireActivity()

        session = Session(activity)
        timerDelay = 3000
        timerWaiting = 3000

        swipeTimer = Timer()

        tvLocation = root.findViewById(R.id.tvLocation)

        binding.sectionView.layoutManager = LinearLayoutManager(activity)
        binding.sectionView.isNestedScrollingEnabled = false

        binding.lytTopOfferImages.layoutManager = LinearLayoutManager(activity)
        binding.lytTopOfferImages.isNestedScrollingEnabled = false

        binding.lytBelowSliderOfferImages.layoutManager = LinearLayoutManager(activity)
        binding.lytBelowSliderOfferImages.isNestedScrollingEnabled = false

        binding.lytBelowCategoryOfferImages.layoutManager = LinearLayoutManager(activity)
        binding.lytBelowCategoryOfferImages.isNestedScrollingEnabled = false

        binding.lytBelowSellerOfferImages.layoutManager = LinearLayoutManager(activity)
        binding.lytBelowSellerOfferImages.isNestedScrollingEnabled = false

        setHasOptionsMenu(true)

        if (session.getData(Constant.SHIPPING_TYPE) == "local") {
            binding.lytPinCode.visibility = View.VISIBLE
            if (!session.getBoolean(Constant.GET_SELECTED_PINCODE)) {
                MainActivity.pinCodeFragment = PinCodeFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "home")
                MainActivity.pinCodeFragment.arguments = bundle
                MainActivity.pinCodeFragment.show(MainActivity.fm, null)
            } else {
                tvLocation.text = session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
            }
            binding.tvTitleLocation.setOnClickListener {
                MainActivity.pinCodeFragment = PinCodeFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "home")
                MainActivity.pinCodeFragment.arguments = bundle
                MainActivity.pinCodeFragment.show(MainActivity.fm, null)
            }

            binding.tvLocation.setOnClickListener {
                MainActivity.pinCodeFragment = PinCodeFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "home")
                MainActivity.pinCodeFragment.arguments = bundle
                MainActivity.pinCodeFragment.show(MainActivity.fm, null)
            }

            binding.tvTitleLocation.setOnClickListener {
                MainActivity.pinCodeFragment = PinCodeFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "home")
                MainActivity.pinCodeFragment.arguments = bundle
                MainActivity.pinCodeFragment.show(MainActivity.fm, null)
            }

            binding.lytPinCode.setOnClickListener {
                MainActivity.pinCodeFragment = PinCodeFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "home")
                MainActivity.pinCodeFragment.arguments = bundle
                MainActivity.pinCodeFragment.show(MainActivity.fm, null)
            }

        }

        binding.nestedScrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            val scrollBounds = Rect()
            binding.nestedScrollView.getHitRect(scrollBounds)
            if (!binding.lytSearchView.getLocalVisibleRect(scrollBounds) || scrollBounds.height() < binding.lytSearchView.height) {
                searchVisible = true
                menu.findItem(R.id.toolbar_search).isVisible = true
            } else {
                searchVisible = false
                menu.findItem(R.id.toolbar_search).isVisible = false
            }
            activity.invalidateOptionsMenu()
        }

        binding.tvMore.setOnClickListener {
            if (!Constant.categoryClicked) {
                MainActivity.fm.beginTransaction()
                    .add(R.id.container, MainActivity.categoryFragment)
                    .show(MainActivity.categoryFragment).hide(MainActivity.active).commit()
                Constant.categoryClicked = true
            } else {
                MainActivity.fm.beginTransaction().show(MainActivity.categoryFragment)
                    .hide(MainActivity.active).commit()
            }
            MainActivity.bottomNavigationView.selectedItemId = R.id.navCategory
            MainActivity.active = MainActivity.categoryFragment
        }

        binding.tvMoreSeller.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, SellerListFragment())
                .addToBackStack(null).commit()
        }

        binding.searchView.setOnTouchListener { _, _: MotionEvent? ->
            val fragment: Fragment = ProductListFragment()
            val bundle = Bundle()
            bundle.putString(Constant.FROM, "search")
            bundle.putString(Constant.NAME, activity.getString(R.string.search))
            bundle.putString(Constant.ID, "")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
            false
        }

        binding.lytSearchView.setOnClickListener {
            val fragment: Fragment = ProductListFragment()
            val bundle = Bundle()
            bundle.putString(Constant.FROM, "search")
            bundle.putString(Constant.NAME, activity.getString(R.string.search))
            bundle.putString(Constant.ID, "")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }

        binding.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(position: Int) {
                ApiConfig.addMarkers(position, sliderArrayList, binding.markersLayout, activity)
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })

        binding.tvLocation.text = session.getData(Constant.GET_SELECTED_PINCODE_NAME)

        refreshListener = OnRefreshListener {
            swipeTimer.cancel()
            timerDelay = 3000
            timerWaiting = 3000
            ApiConfig.getWalletBalance(activity, session)
            if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
                ApiConfig.getWalletBalance(activity, Session(activity))
            }
            binding.tvLocation.text = session.getData(Constant.GET_SELECTED_PINCODE_NAME)
            getHomeData()
        }

        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            refreshListener.onRefresh()
        }
        getHomeData()
        if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
            ApiConfig.getWalletBalance(activity, Session(activity))
        }


        if (session.getData(Constant.FCM_ID) == null && session.getBoolean(Constant.IS_USER_LOGIN)) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get new FCM registration token
                    val token = task.result
                    session.setData(Constant.FCM_ID, token)
                    registerFcm(token)
                }
            }
        }

        return binding.root
    }

private fun registerFcm(token: String) {
    val params: MutableMap<String, String> = java.util.HashMap()
    params[Constant.USER_ID] = session.getData(Constant.ID).toString()
    params[Constant.FCM_ID] = token
    ApiConfig.requestToVolley(object : VolleyCallback {
        override fun onSuccess(result: Boolean, response: String) {
            if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        session.setData(Constant.FCM_ID, token)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }, MainActivity.activity, Constant.REGISTER_DEVICE_URL, params, false)
}

    private fun getHomeData() {
        binding.nestedScrollView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        if (swipeTimer != null) {
            swipeTimer.cancel()
        }
        val params: MutableMap<String, String> = HashMap()
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        }
        if (session.getData(Constant.SHIPPING_TYPE)
                .equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                Constant.GET_SELECTED_PINCODE_ID
            ) != "0"
        ) {
            params[Constant.PINCODE] =
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (jsonObject.getJSONArray(Constant.OFFER_IMAGES).length() > 0) {
                                ApiConfig.getOfferImage(
                                    activity,
                                    jsonObject.getJSONArray(Constant.OFFER_IMAGES),
                                    binding.lytTopOfferImages
                                )
                            }
                            if (jsonObject.getJSONArray(Constant.SLIDER_OFFER_IMAGES)
                                    .length() > 0
                            ) {
                                ApiConfig.getOfferImage(
                                    activity,
                                    jsonObject.getJSONArray(Constant.SLIDER_OFFER_IMAGES),
                                    binding.lytBelowSliderOfferImages
                                )
                            }
                            if (jsonObject.getJSONArray(Constant.CATEGORY_OFFER_IMAGES)
                                    .length() > 0
                            ) {
                                ApiConfig.getOfferImage(
                                    activity,
                                    jsonObject.getJSONArray(Constant.CATEGORY_OFFER_IMAGES),
                                    binding.lytBelowCategoryOfferImages
                                )
                            }
                            if (jsonObject.getJSONArray(Constant.SELLER_OFFER_IMAGES)
                                    .length() > 0
                            ) {
                                ApiConfig.getOfferImage(
                                    activity,
                                    jsonObject.getJSONArray(Constant.SELLER_OFFER_IMAGES),
                                    binding.lytBelowSellerOfferImages
                                )
                            }

                            getCategory(jsonObject)

                            if (jsonObject.getJSONArray(Constant.SECTIONS).length() > 0) {
                                sectionProductRequest(jsonObject.getJSONArray(Constant.SECTIONS))
                            }
                            if (jsonObject.getJSONArray(Constant.SLIDER_IMAGES).length() > 0) {
                                getSlider(jsonObject.getJSONArray(Constant.SLIDER_IMAGES))
                            }
                            if (Constant.SHOW_SELLERS_IN_HOME_PAGE) {
                                getSeller(jsonObject.getJSONArray(Constant.SELLER))
                            } else {
                                binding.lytSeller.visibility = View.GONE
                            }
                        } else {
                            binding.nestedScrollView.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.shimmerFrameLayout.stopShimmer()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.nestedScrollView.visibility = View.VISIBLE
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.shimmerFrameLayout.stopShimmer()
                    }
                }
            }
        }, activity, Constant.GET_ALL_DATA_URL, params, false)
    }

    private fun getCategory(`object`: JSONObject) {
        categoryArrayList = ArrayList()
        try {
            val visibleCount: Int
            val columnCount: Int
            val jsonArray = `object`.getJSONArray(Constant.CATEGORIES)
            if (jsonArray.length() > 0) {
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val category = Gson().fromJson(jsonObject.toString(), Category::class.java)
                    categoryArrayList.add(category)
                }
                if (`object`.getString("style") != "") {
                    if (`object`.getString("style") == "style_1") {
                        visibleCount = `object`.getString("visible_count").toInt()
                        columnCount = `object`.getString("column_count").toInt()
                        binding.categoryRecyclerView.layoutManager =
                            GridLayoutManager(activity, columnCount)
                        binding.categoryRecyclerView.adapter = CategoryAdapter(
                            activity,
                            categoryArrayList,
                            R.layout.lyt_category_grid,
                            "home",
                            visibleCount
                        )
                    } else if (`object`.getString("style") == "style_2") {
                        visibleCount = `object`.getString("visible_count").toInt()
                        binding.categoryRecyclerView.layoutManager =
                            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                        binding.categoryRecyclerView.adapter = CategoryAdapter(
                            activity,
                            categoryArrayList,
                            R.layout.lyt_category_list,
                            "home",
                            visibleCount
                        )
                    }

                    binding.nestedScrollView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                } else {
                    binding.categoryRecyclerView.layoutManager =
                        LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                    binding.categoryRecyclerView.adapter = CategoryAdapter(
                        activity,
                        categoryArrayList,
                        R.layout.lyt_category_list,
                        "home",
                        6
                    )
                }
            } else {
                binding.categoryRecyclerView.visibility = View.GONE
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun sectionProductRequest(jsonArray: JSONArray) {  //json request for product search
        sectionList = ArrayList()
        try {
            for (j in 0 until jsonArray.length()) {
                val section = Category()
                    val jsonObject = jsonArray.getJSONObject(j)
                    section.name = jsonObject.getString(Constant.TITLE)
                    section.id = jsonObject.getString(Constant.ID)
                    section.style = jsonObject.getString(Constant.SECTION_STYLE)
                    section.subtitle = jsonObject.getString(Constant.SHORT_DESC)
                    val productArray = jsonObject.getJSONArray(Constant.PRODUCTS)
                    section.productList = ApiConfig.getProductList(productArray)
                    if(section.productList.size>0) {
                    sectionList.add(section)
                    val sectionAdapter = SectionAdapter(activity, sectionList, jsonArray)
                    binding.sectionView.adapter = sectionAdapter
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun getSlider(jsonArray: JSONArray) {
        sliderArrayList = ArrayList()
        try {
            size = jsonArray.length()
            if(size>0){
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val slider = Gson().fromJson(jsonObject.toString(), Slider::class.java)
                    sliderArrayList.add(slider)
                }
                binding.viewPager.adapter =
                    SliderAdapter(sliderArrayList, activity, R.layout.lyt_slider, "home")
                ApiConfig.addMarkers(0, sliderArrayList, binding.markersLayout, activity)
                handler = Handler()
                update = Runnable {
                    if (currentPage == size) {
                        currentPage = 0
                    }
                    try {
                        binding.viewPager.setCurrentItem(currentPage++, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                swipeTimer = Timer()
                swipeTimer.schedule(object : TimerTask() {
                    override fun run() {
                        handler.post(update)
                    }
                }, timerDelay.toLong(), timerWaiting.toLong())
                binding.viewPagerLayout.visibility=View.VISIBLE
            }else{
                binding.viewPagerLayout.visibility=View.GONE
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.shimmerFrameLayout.visibility = View.GONE
        binding.shimmerFrameLayout.stopShimmer()
    }

    private fun getSeller(jsonArray: JSONArray) {
        try {
            sellerArrayList = ArrayList()
            if (jsonArray.length() > 0) {
                binding.lytSeller.visibility = View.VISIBLE
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val seller = Gson().fromJson(jsonObject.toString(), Seller::class.java)
                    sellerArrayList.add(seller)
                }
                binding.sellerRecyclerView.layoutManager =
                    GridLayoutManager(activity, Constant.GRID_COLUMN)
                binding.sellerRecyclerView.adapter =
                    SellerAdapter(activity, sellerArrayList, R.layout.lyt_seller, "home", 6)
            } else {
                binding.lytSeller.visibility = View.GONE
            }
        } catch (jsonException: JSONException) {
            jsonException.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        activity.invalidateOptionsMenu()
        ApiConfig.getSettings(activity)
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
        this.menu = menu
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = searchVisible
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        try {
            if (OfferAdapter.viewHolder.imgPlay.tag == "pause") {
                OfferAdapter.viewHolder.imgPlay.setImageDrawable(
                    ContextCompat.getDrawable(
                        activity, R.drawable.ic_play
                    )
                )
                OfferAdapter.viewHolder.imgPlay.tag = "play"
                OfferAdapter.mediaPlayer.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        lateinit var categoryArrayList: ArrayList<Category>
        lateinit var sectionList: ArrayList<Category>
        lateinit var sellerArrayList: ArrayList<Seller>

        @SuppressLint("StaticFieldLeak")
        lateinit var tvLocation: TextView
        lateinit var refreshListener: OnRefreshListener
    }
}