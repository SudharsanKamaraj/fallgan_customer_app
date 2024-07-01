package com.gpn.customerapp.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import com.gpn.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gpn.customerapp.R
import com.gpn.customerapp.activity.PaymentActivity
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.model.BookingDate

/**
 * Created by shree1 on 3/16/2017.
 */
class DateAdapter(val activity: Activity, private val bookingDates: ArrayList<BookingDate>) :
    RecyclerView.Adapter<DateAdapter.HolderItems>() {
    override fun getItemCount(): Int {
        return bookingDates.size
    }

    override fun onBindViewHolder(holder: HolderItems, position: Int) {
        val bookingDate = bookingDates[position]
        if (Constant.selectedDatePosition == position) {
            PaymentActivity.deliveryDay = bookingDate.date + "-" + ApiConfig.getMonth(
                bookingDate.month.toInt(),
                activity
            ) + "-" + bookingDate.year
            holder.relativeLyt.setBackgroundResource(R.drawable.selected_date_shadow)
            holder.tvDay.setTextColor(ContextCompat.getColor(activity, R.color.white))
            holder.tvDate.setTextColor(ContextCompat.getColor(activity, R.color.white))
            holder.tvMonth.setTextColor(ContextCompat.getColor(activity, R.color.white))
        } else {
            holder.tvDay.setTextColor(ContextCompat.getColor(activity, R.color.gray))
            holder.tvDate.setTextColor(ContextCompat.getColor(activity, R.color.gray))
            holder.tvMonth.setTextColor(ContextCompat.getColor(activity, R.color.gray))
            holder.relativeLyt.setBackgroundResource(R.drawable.date_shadow)
        }
        holder.relativeLyt.setPadding(
            activity.resources.getDimension(R.dimen._15sdp).toInt(),
            activity.resources.getDimension(R.dimen._15sdp)
                .toInt(),
            activity.resources.getDimension(R.dimen._15sdp).toInt(),
            activity.resources.getDimension(R.dimen._15sdp)
                .toInt()
        )
        holder.tvDay.setText(ApiConfig.getDayOfWeek(bookingDate.day.toInt(), activity))
        holder.tvDate.text = bookingDate.date
        holder.tvMonth.setText(ApiConfig.getMonth(bookingDate.month.toInt(), activity))
        holder.relativeLyt.setOnClickListener { view: View? ->
            if (PaymentActivity.adapter != null) {
                if (PaymentActivity.deliveryDay.length > 0) {
                    Constant.selectedDatePosition = holder.position
                    notifyDataSetChanged()
                    PaymentActivity.deliveryTime = ""
                    PaymentActivity.adapter.notifyDataSetChanged()
                    PaymentActivity.recyclerViewTimeSlot1.adapter = PaymentActivity.adapter
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderItems {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.lyt_date, parent, false)
        return HolderItems(view)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView
        val tvMonth: TextView
        val tvDay: TextView
        val relativeLyt: CardView

        init {
            tvDate = itemView.findViewById(R.id.tvDate)
            tvMonth = itemView.findViewById(R.id.tvMonth)
            tvDay = itemView.findViewById(R.id.tvDay)
            relativeLyt = itemView.findViewById(R.id.relativeLyt)
        }
    }

}