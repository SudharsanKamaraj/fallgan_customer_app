package com.gpn.customerapp.adapter

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gpn.customerapp.R
import com.gpn.customerapp.fragment.SubCategoryFragment
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.model.Category

class CategoryAdapter(
    activity: Activity,
    categoryList: ArrayList<Category>,
    layout: Int,
    from: String,
    visibleNumber: Int
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
    val categoryList: ArrayList<Category>
    val layout: Int
    val from: String
    val visibleNumber: Int
    var activity: Activity
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = categoryList[position]
        holder.tvTitle.text = model.name
        Glide.with(activity).load(model.image)
            .centerInside()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imgCategory)
        holder.lytMain.setOnClickListener { v: View? ->
            val fragment: Fragment = SubCategoryFragment()
            val bundle = Bundle()
            bundle.putString(Constant.ID, model.id)
            bundle.putString(Constant.NAME, model.name)
            bundle.putString(Constant.FROM, "category")
            fragment.arguments = bundle
            (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment).addToBackStack(null).commit()
        }
    }

    override fun getItemCount(): Int {
        val categories: Int
        categories = if (categoryList.size > visibleNumber && from == "home") {
            visibleNumber
        } else {
            categoryList.size
        }
        return categories
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView
        val imgCategory: ImageView
        val lytMain: LinearLayout

        init {
            lytMain = itemView.findViewById(R.id.lytMain)
            imgCategory = itemView.findViewById(R.id.imgCategory)
            tvTitle = itemView.findViewById(R.id.tvTitle)
        }
    }

    init {
        this.activity = activity
        this.categoryList = categoryList
        this.layout = layout
        this.from = from
        this.visibleNumber = visibleNumber
    }
}