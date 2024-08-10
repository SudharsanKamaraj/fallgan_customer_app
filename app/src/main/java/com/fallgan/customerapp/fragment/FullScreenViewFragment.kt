package com.fallgan.customerapp.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.fallgan.customerapp.R
import com.fallgan.customerapp.adapter.SliderAdapter
import com.fallgan.customerapp.databinding.FragmentFullScreenViewBinding
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.model.Slider

class FullScreenViewFragment : Fragment() {
    lateinit var binding:FragmentFullScreenViewBinding
    lateinit var root: View
    private var pos = 0
    lateinit var imgList: ArrayList<Slider>
    lateinit var activity: Activity
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_full_screen_view, container, false)
        binding = FragmentFullScreenViewBinding.inflate(inflater,container,false)

        activity = requireActivity()
        activity = requireActivity()
        setHasOptionsMenu(true)
        imgList = ArrayList()
        imgList = ProductDetailFragment.sliderArrayList
        pos = requireArguments().getInt("pos", 0)
        binding.viewPager.adapter = SliderAdapter(
            imgList,
            activity,
            R.layout.lyt_fullscreenimg,
            "fullscreen"
        )
        binding.viewPager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(position: Int) {
                ApiConfig.addMarkers(position, imgList, binding.layoutMarkers, activity)
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })
        binding.viewPager.currentItem = pos
        ApiConfig.addMarkers(pos, imgList, binding.layoutMarkers, activity)
        return binding.root
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
    }
}