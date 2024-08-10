package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.fallgan.customerapp.R
import com.fallgan.customerapp.helper.Constant

class CustomAdapter(internal var activity: Activity, private var variantNames: Array<String?>, private var variantsStockStatus: Array<String?>) : BaseAdapter() {
    private var inflater: LayoutInflater = LayoutInflater.from(activity)

    override fun getCount(): Int {
        return variantNames.size
    }

    override fun getItem(i: Int): String? {
        return variantNames[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, view1: View?, viewGroup: ViewGroup): View {
        val view = inflater.inflate(R.layout.lyt_spinner_item, null)
        val tvMeasurement = view.findViewById<View>(R.id.tvMeasurement) as TextView
        if (variantsStockStatus[position] == Constant.SOLD_OUT_TEXT) {
            tvMeasurement.setTextColor(ContextCompat.getColor(activity, R.color.red))
        } else {
            tvMeasurement.setTextColor(ContextCompat.getColor(activity, R.color.txt_color))
        }
        tvMeasurement.text = variantNames[position]
        return view
    }
}