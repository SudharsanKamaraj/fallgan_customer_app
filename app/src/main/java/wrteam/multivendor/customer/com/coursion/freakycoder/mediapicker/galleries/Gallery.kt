package com.gpn.customerapp.com.coursion.freakycoder.mediapicker.galleries

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.gpn.customerapp.R
import com.gpn.customerapp.com.coursion.freakycoder.mediapicker.fragments.ImageFragment
import com.gpn.customerapp.com.coursion.freakycoder.mediapicker.fragments.VideoFragment
import com.gpn.customerapp.com.coursion.freakycoder.mediapicker.helper.Util

class Gallery : AppCompatActivity() {

    companion object {
        var selectionTitle: Int = 0
        var title: String? = null
        var maxSelection: Int = 0
        var mode: Int = 0
        var tabBarHidden = false
    }

    open lateinit var fab: FloatingActionButton
    open lateinit var toolbar: Toolbar
    open lateinit var viewPager: ViewPager
    open lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.multi_select_activity_gallery)
        fab = findViewById(R.id.fab)
        toolbar = findViewById(R.id.toolbar)
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        // Set the toolbar
        setSupportActionBar(toolbar)
        
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        val util = Util()
        util.setButtonTint(fab, ContextCompat.getColorStateList(applicationContext, R.color.colorPrimary)!!)
        fab.setOnClickListener { returnResult() }

        title = intent.extras!!.getString("title")
        maxSelection = intent.extras!!.getInt("maxSelection")
        if (maxSelection == 0) maxSelection = Integer.MAX_VALUE
        mode = intent.extras!!.getInt("mode")
        tabBarHidden = intent.extras!!.getBoolean("tabBarHidden")
        title = title
        selectionTitle = 0
        // Set the ViewPager and TabLayout
        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)

        OpenGallery.selected.clear()
        OpenGallery.imagesSelected.clear()

    }

    override fun onPostResume() {
        super.onPostResume()
        if (selectionTitle > 0) {
            title = selectionTitle.toString()
        }
    }

    //This method set up the tab view for images and videos
    private fun setupViewPager(viewPager: ViewPager?) {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        if (mode == 1 || mode == 2) {
            adapter.addFragment(ImageFragment(), "Images")
        }
        if (mode == 1 || mode == 3)
            adapter.addFragment(VideoFragment(), "Videos")
        viewPager!!.adapter = adapter

        if (tabBarHidden) {
            tabLayout.visibility = View.GONE
        } else {
            tabLayout.visibility = View.VISIBLE
        }
    }

    internal inner class ViewPagerAdapter(manager: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(manager) {
        private val mFragmentList = ArrayList<androidx.fragment.app.Fragment>()
        private val mFragmentTitleList = ArrayList<String>()

        override fun getItem(position: Int): androidx.fragment.app.Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: androidx.fragment.app.Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence {
            return mFragmentTitleList[position]
        }
    }

    private fun returnResult() {
        val returnIntent = Intent()
        returnIntent.putStringArrayListExtra("result", OpenGallery.imagesSelected)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }
}
