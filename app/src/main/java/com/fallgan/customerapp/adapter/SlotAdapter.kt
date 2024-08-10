package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.PaymentActivity
import com.fallgan.customerapp.model.Slot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class SlotAdapter(activity: Activity, categoryList: ArrayList<Slot>) :
    RecyclerView.Adapter<SlotAdapter.ViewHolder>() {
    val categoryList: ArrayList<Slot>
    val activity: Activity
    var selectedPosition = 0
    var isToday = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.lyt_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = categoryList[position]
        holder.rdBtn.text = model.title
        holder.rdBtn.tag = position
        holder.rdBtn.isChecked = position == selectedPosition
        val pattern = "HH:mm:ss"
        @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat(pattern)
        val now = sdf.format(Date())
        var currentTime: Date? = null
        var SlotTime: Date? = null
        try {
            currentTime = sdf.parse(now)
            SlotTime = sdf.parse(model.lastOrderTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val calendar = Calendar.getInstance()
        isToday =
            PaymentActivity.deliveryDay == calendar[Calendar.DATE].toString() + "-" + ApiConfig.getMonth(
                calendar[Calendar.MONTH] + 1, activity
            ) + "-" + calendar[Calendar.YEAR]
        assert(currentTime != null)
        if (isToday) {
            if (currentTime!!.compareTo(SlotTime) > 0) {
                holder.rdBtn.isChecked = false
                holder.rdBtn.isClickable = false
                holder.rdBtn.setTextColor(ContextCompat.getColor(activity, R.color.gray))
                holder.rdBtn.buttonDrawable =
                    ContextCompat.getDrawable(activity, R.drawable.ic_uncheck_circle)
            } else {
                holder.rdBtn.isClickable = true
                holder.rdBtn.setTextColor(ContextCompat.getColor(activity, R.color.black))
                holder.rdBtn.buttonDrawable =
                    ContextCompat.getDrawable(activity, R.drawable.ic_active_circle)
            }
        } else {
            holder.rdBtn.isClickable = true
            holder.rdBtn.setTextColor(ContextCompat.getColor(activity, R.color.black))
            holder.rdBtn.buttonDrawable =
                ContextCompat.getDrawable(activity, R.drawable.ic_active_circle)
        }
        val finalCurrentTime = currentTime
        val finalSlotTime = SlotTime
        holder.rdBtn.setOnClickListener { v: View ->
            if (isToday) {
                if (finalCurrentTime!!.compareTo(finalSlotTime) < 0) {
                    PaymentActivity.deliveryTime = model.title
                    selectedPosition = v.tag as Int
                    notifyDataSetChanged()
                }
            } else {
                PaymentActivity.deliveryTime = model.title
                selectedPosition = v.tag as Int
                notifyDataSetChanged()
            }
        }
        if (holder.rdBtn.isChecked) {
            holder.rdBtn.buttonDrawable =
                ContextCompat.getDrawable(activity, R.drawable.ic_radio_button_checked)
            holder.rdBtn.setTextColor(ContextCompat.getColor(activity, R.color.black))
            PaymentActivity.deliveryTime = model.title
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rdBtn: RadioButton

        init {
            rdBtn = itemView.findViewById(R.id.rdBtn)
        }
    }

    init {
        this.activity = activity
        this.categoryList = categoryList
        PaymentActivity.deliveryTime = ""
    }
}