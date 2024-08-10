package com.fallgan.customerapp.adapter

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.adapter.SectionAdapter.SectionHolder
import com.fallgan.customerapp.fragment.ProductListFragment
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.model.Category

class SectionAdapter(activity: Activity, sectionList: ArrayList<Category>, jsonArray: JSONArray) :
    RecyclerView.Adapter<SectionHolder>() {
    val sectionList: ArrayList<Category>
    val activity: Activity
    var jsonArrayImages: JSONArray
    val jsonArray: JSONArray
    override fun getItemCount(): Int {
        return sectionList.size
    }

    override fun onBindViewHolder(holder: SectionHolder, position: Int) {
        val section: Category
        section = sectionList[position]
        holder.tvTitle.text = section.name
        holder.tvSubTitle.text = section.subtitle
        holder.lytBelowSectionOfferImages.layoutManager = LinearLayoutManager(activity)
        holder.lytBelowSectionOfferImages.isNestedScrollingEnabled = false
        try {
            jsonArrayImages = jsonArray.getJSONObject(position).getJSONArray(Constant.OFFER_IMAGES)
            ApiConfig.getOfferImage(activity, jsonArrayImages, holder.lytBelowSectionOfferImages)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (section.style) {
            "style_1" -> {
                holder.recyclerView.layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                val adapter = AdapterStyle1(activity, section.productList, R.layout.offer_layout)
                holder.recyclerView.adapter = adapter
            }
            "style_2" -> {
                holder.recyclerView.layoutManager = LinearLayoutManager(activity)
                val adapterStyle2 = AdapterStyle2(activity, section.productList)
                holder.recyclerView.adapter = adapterStyle2
            }
            "style_3" -> {
                holder.recyclerView.layoutManager = GridLayoutManager(activity, 2)
                val adapter3 = AdapterStyle1(activity, section.productList, R.layout.lyt_style_3)
                holder.recyclerView.adapter = adapter3
            }
        }
        holder.tvMore.setOnClickListener { view: View? ->
            val fragment: Fragment = ProductListFragment()
            val bundle = Bundle()
            bundle.putString(Constant.FROM, "section")
            bundle.putString(Constant.NAME, section.name)
            bundle.putString(Constant.ID, section.id)
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.section_layout, parent, false)
        return SectionHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class SectionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView
        val tvSubTitle: TextView
        val tvMore: TextView
        val recyclerView: RecyclerView
        val lytBelowSectionOfferImages: RecyclerView
        val relativeLayout: RelativeLayout

        init {
            tvTitle = itemView.findViewById(R.id.tvTitle)
            tvSubTitle = itemView.findViewById(R.id.tvSubTitle)
            tvMore = itemView.findViewById(R.id.tvMore)
            recyclerView = itemView.findViewById(R.id.recyclerView)
            lytBelowSectionOfferImages = itemView.findViewById(R.id.lytBelowSectionOfferImages)
            relativeLayout = itemView.findViewById(R.id.relativeLayout)
        }
    }

    init {
        this.activity = activity
        this.sectionList = sectionList
        this.jsonArray = jsonArray
        jsonArrayImages = JSONArray()
    }
}