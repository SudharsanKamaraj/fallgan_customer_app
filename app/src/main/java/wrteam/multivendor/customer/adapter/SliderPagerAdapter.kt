package com.gpn.customerapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.gpn.customerapp.fragment.SliderItemFragment

class SliderPagerAdapter(fm: FragmentManager, behavior: Int) :
    FragmentPagerAdapter(fm, behavior) {
    override fun getItem(position: Int): Fragment {
        return SliderItemFragment.newInstance(position)
    }

    // size is hardcoded
    override fun getCount(): Int {
        return 3
    }
}