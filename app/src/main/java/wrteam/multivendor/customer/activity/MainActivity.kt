package com.gpn.customerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.razorpay.PaymentResultListener
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.adapter.OfferAdapter
import com.gpn.customerapp.databinding.ActivityMainBinding
import com.gpn.customerapp.fragment.*
import com.gpn.customerapp.helper.*

class MainActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityMainBinding

    lateinit var session: Session
    lateinit var databaseHelper: DatabaseHelper
    lateinit var from: String
    private var doubleBackToExitPressedOnce = false

    @SuppressLint("AppBundleLocaleChanges")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE)
        activity = this@MainActivity
        session = Session(activity)
        ApiConfig.getShippingType(activity, session)
        from = intent.getStringExtra(Constant.FROM).toString()
        databaseHelper = DatabaseHelper(activity)
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            ApiConfig.getCartItemCount(activity, session)
        } else {
            session.setData(Constant.STATUS, "1")
            databaseHelper.getTotalItemOfCart(activity)
        }

        fm = supportFragmentManager
        homeFragment = HomeFragment()
        categoryFragment = CategoryFragment()
        favoriteFragment = FavoriteFragment()
        drawerFragment = DrawerFragment()
        var bundle = Bundle()
        bottomNavigationView.selectedItemId = R.id.navMain
        active = homeFragment
        Constant.homeClicked = true
        Constant.drawerClicked = false
        Constant.favoriteClicked = false
        Constant.categoryClicked = false
        try {
            if (intent.getStringExtra("json")!!.isNotEmpty()) {
                bundle.putString("json", intent.getStringExtra("json"))
            }
            homeFragment.arguments = bundle
            fm.beginTransaction().add(R.id.container, homeFragment).commit()
        } catch (e: Exception) {
            fm.beginTransaction().add(R.id.container, homeFragment).commit()
        }
        @SuppressLint("ResourceType") val iconColorStates = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ), intArrayOf(
                Color.parseColor(resources.getString(R.color.text_unselected)),
                Color.parseColor(resources.getString(R.color.colorSecondary))
            )
        )
        binding.bottomNavigationView.itemIconTintList = iconColorStates
        binding.bottomNavigationView.itemTextColor = iconColorStates
        binding.bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
            run {
                if (item.itemId == R.id.navMain) {
                    if (active !== homeFragment) {
                        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                            Constant.TOOLBAR_TITLE =
                                getString(R.string.hi) + session.getData(Constant.NAME) + "!"
                        } else {
                            Constant.TOOLBAR_TITLE = getString(R.string.hi_user)
                        }
                        invalidateOptionsMenu()
                        binding.bottomNavigationView.menu.findItem(item.itemId).isChecked = true
                        if (!Constant.homeClicked) {
                            fm.beginTransaction().add(R.id.container, homeFragment)
                                .show(homeFragment).hide(
                                    active
                                ).commit()
                            Constant.homeClicked = true
                        } else {
                            fm.beginTransaction().show(homeFragment).hide(
                                active
                            ).commit()
                        }
                        active = homeFragment
                    }
                } else if (item.itemId == R.id.navCategory) {
                    Constant.TOOLBAR_TITLE = getString(R.string.title_category)
                    invalidateOptionsMenu()
                    if (active !== categoryFragment) {
                        binding.bottomNavigationView.menu.findItem(item.itemId).isChecked = true
                        if (!Constant.categoryClicked) {
                            fm.beginTransaction().add(R.id.container, categoryFragment).show(
                                categoryFragment
                            ).hide(
                                active
                            ).commit()
                            Constant.categoryClicked = true
                        } else {
                            fm.beginTransaction().show(categoryFragment).hide(
                                active
                            ).commit()
                        }
                        active = categoryFragment
                    }
                } else if (item.itemId == R.id.navWishList) {
                    Constant.TOOLBAR_TITLE = getString(R.string.title_fav)
                    invalidateOptionsMenu()
                    if (active !== favoriteFragment) {
                        binding.bottomNavigationView.menu.findItem(item.itemId).isChecked = true
                        if (!Constant.favoriteClicked) {
                            fm.beginTransaction().add(R.id.container, favoriteFragment).show(
                                favoriteFragment
                            ).hide(
                                active
                            ).commit()
                            Constant.favoriteClicked = true
                        } else {
                            fm.beginTransaction().show(favoriteFragment).hide(
                                active
                            ).commit()
                        }
                        active = favoriteFragment
                    }
                } else if (item.itemId == R.id.navProfile) {
                    Constant.TOOLBAR_TITLE = getString(R.string.title_profile)
                    invalidateOptionsMenu()
                    if (active !== drawerFragment) {
                        binding.bottomNavigationView.menu.findItem(item.itemId).isChecked = true
                        if (!Constant.drawerClicked) {
                            fm.beginTransaction().add(R.id.container, drawerFragment)
                                .show(drawerFragment).hide(
                                    active
                                ).commit()
                            Constant.drawerClicked = true
                        } else {
                            fm.beginTransaction().show(drawerFragment).hide(
                                active
                            ).commit()
                        }
                        active = drawerFragment
                    }
                }
            }
            false
        }
        when (from) {
            "checkout" -> {
                binding.bottomNavigationView.visibility = View.GONE
                ApiConfig.getCartItemCount(activity, session)
                val fragment: Fragment = CartFragment()
                bundle = Bundle()
                bundle.putString(Constant.FROM, "mainActivity")
                fragment.arguments = bundle
                fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit()
            }
            "share" -> {
                val fragment0: Fragment = ProductDetailFragment()
                val bundle0 = Bundle()
                bundle0.putInt(
                    Constant.VARIANT_POSITION,
                    intent.getIntExtra(Constant.VARIANT_POSITION, 0)
                )
                bundle0.putString(Constant.ID, intent.getStringExtra(Constant.ID))
                bundle0.putString(Constant.FROM, "share")
                fragment0.arguments = bundle0
                fm.beginTransaction().add(R.id.container, fragment0).addToBackStack(null).commit()
            }
            "product" -> {
                val fragment1: Fragment = ProductDetailFragment()
                val bundle1 = Bundle()
                bundle1.putInt(
                    Constant.VARIANT_POSITION,
                    intent.getIntExtra(Constant.VARIANT_POSITION, 0)
                )
                bundle1.putString(Constant.ID, intent.getStringExtra(Constant.ID))
                bundle1.putString(Constant.FROM, "product")
                fragment1.arguments = bundle1
                fm.beginTransaction().add(R.id.container, fragment1).addToBackStack(null).commit()
            }
            "category" -> {
                val fragment2: Fragment = SubCategoryFragment()
                val bundle2 = Bundle()
                bundle2.putString(Constant.ID, intent.getStringExtra(Constant.ID))
                bundle2.putString("name", intent.getStringExtra("name"))
                bundle2.putString(Constant.FROM, "category")
                fragment2.arguments = bundle2
                fm.beginTransaction().add(R.id.container, fragment2).addToBackStack(null).commit()
            }
            "order" -> {
                val fragment3: Fragment = OrderDetailFragment()
                val bundle3 = Bundle()
                bundle3.putSerializable("model", "")
                bundle3.putString(Constant.ID, intent.getStringExtra(Constant.ID))
                fragment3.arguments = bundle3
                fm.beginTransaction().add(R.id.container, fragment3).addToBackStack(null).commit()
            }
            "payment_success" -> fm.beginTransaction().add(R.id.container, OrderPlacedFragment())
                .addToBackStack(null).commit()
            "tracker" -> fm.beginTransaction().add(R.id.container, OrderListFragment())
                .addToBackStack(null).commit()
            "wallet" -> fm.beginTransaction().add(R.id.container, WalletTransactionFragment())
                .addToBackStack(null).commit()
        }
        fm.addOnBackStackChangedListener {
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
            binding.toolbar.visibility = View.VISIBLE
            val currentFragment = fm.findFragmentById(R.id.container)
            currentFragment?.onResume()
        }

        ApiConfig.getProductNames(activity, session)
        getUserData(activity, session)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        doubleBackToExitPressedOnce = true
        if (fm.backStackEntryCount == 0) {
            Toast.makeText(this, getString(R.string.exit_msg), Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
    }

    @SuppressLint("NonConstantResourceId")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_cart -> fm.beginTransaction().add(R.id.container, CartFragment())
                .addToBackStack(null).commit()
            R.id.toolbar_search -> {
                val fragment: Fragment = ProductListFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "search")
                bundle.putString(Constant.NAME, activity.getString(R.string.search))
                bundle.putString(Constant.ID, "")
                fragment.arguments = bundle
                fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit()
            }
            R.id.toolbar_logout -> session.logoutUserConfirmation(activity)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_search).isVisible = true
        menu.findItem(R.id.toolbar_cart).icon =
            ApiConfig.buildCounterDrawable(Constant.TOTAL_CART_ITEM, activity)
        if (fm.backStackEntryCount > 0) {
            binding.toolbarTitle.text = Constant.TOOLBAR_TITLE
            binding.bottomNavigationView.visibility = View.GONE
            binding.cardViewHamburger.setCardBackgroundColor(getColor(R.color.colorPrimaryLight))
            binding.imageMenu.setOnClickListener { fm.popBackStack() }
            binding.imageMenu.visibility = View.VISIBLE
            binding.imageHome.visibility = View.GONE
        } else {
            if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                binding.toolbarTitle.text =
                    getString(R.string.hi) + session.getData(Constant.NAME) + "!"
            } else {
                binding.toolbarTitle.text = getString(R.string.hi_user)
            }
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.cardViewHamburger.setCardBackgroundColor(getColor(R.color.colorPrimaryLight))
            binding.imageMenu.visibility = View.GONE
            binding.imageHome.visibility = View.VISIBLE
        }
        invalidateOptionsMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val fragment = supportFragmentManager.findFragmentById(R.id.container)
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        invalidateOptionsMenu()
        super.onPause()
    }

    override fun onResume() {
        ApiConfig.getWalletBalance(activity, session)
        super.onResume()
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        try {
            WalletTransactionFragment().addWalletBalance(
                activity,
                Session(activity),
                WalletTransactionFragment.amount,
                WalletTransactionFragment.msg
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentError(code: Int, response: String) {
        try {
            Toast.makeText(activity, getString(R.string.order_cancel), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getUserData(activity: Activity, session: Session) {
        try {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_USER_DATA] = Constant.GetVal
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject1 = JSONObject(response)
                            if (!jsonObject1.getBoolean(Constant.ERROR)) {
                                val jsonObject =
                                    jsonObject1.getJSONArray(Constant.DATA).getJSONObject(0)
                                session.setUserData(
                                    jsonObject.getString(Constant.USER_ID),
                                    jsonObject.getString(
                                        Constant.NAME
                                    ),
                                    jsonObject.getString(Constant.EMAIL),
                                    jsonObject.getString(
                                        Constant.COUNTRY_CODE
                                    ),
                                    jsonObject.getString(Constant.PROFILE),
                                    jsonObject.getString(
                                        Constant.MOBILE
                                    ),
                                    jsonObject.getString(Constant.BALANCE),
                                    jsonObject.getString(
                                        Constant.REFERRAL_CODE
                                    ),
                                    jsonObject.getString(Constant.FRIEND_CODE),
                                    jsonObject.getString(
                                        Constant.FCM_ID
                                    ),
                                    jsonObject.getString(Constant.STATUS)
                                )
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.USER_DATA_URL, params, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var toolbar: Toolbar

        @SuppressLint("StaticFieldLeak")
        lateinit var bottomNavigationView: BottomNavigationView
        lateinit var active: Fragment
        lateinit var fm: FragmentManager
        lateinit var homeFragment: Fragment
        lateinit var categoryFragment: Fragment
        lateinit var favoriteFragment: Fragment
        lateinit var drawerFragment: Fragment
        lateinit var activity: Activity

        @SuppressLint("StaticFieldLeak")
        lateinit var pinCodeFragment: PinCodeFragment
    }
}