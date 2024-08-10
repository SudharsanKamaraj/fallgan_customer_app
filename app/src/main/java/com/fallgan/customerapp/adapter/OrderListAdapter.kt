package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.fragment.OrderDetailFragment
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.model.OrderTracker
import java.util.*

class OrderListAdapter(activity: Activity, orderTrackerArrayList: ArrayList<OrderTracker?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val orderTrackerArrayList: ArrayList<OrderTracker?>
    var isLoading = false
    fun add(position: Int, item: OrderTracker?) {
        orderTrackerArrayList.add(position, item)
        notifyItemInserted(position)
    }

    fun setLoaded() {
        isLoading = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity).inflate(R.layout.lyt_order_list, parent, false)
                HolderItems(view)
            }
            viewTypeLoading -> {
                view =
                    LayoutInflater.from(activity).inflate(R.layout.item_progressbar, parent, false)
                ViewHolderLoading(view)
            }
            else -> throw IllegalArgumentException("unexpected viewType: $viewType")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
        if (holderParent is HolderItems) {
            val order = orderTrackerArrayList[position]
            holderParent.tvOrderID.text = activity.getString(R.string.order_number) + order!!.id
            val date = order.date_added.split("\\s+").toTypedArray()
            holderParent.tvOrderDate.text = activity.getString(R.string.ordered_on) + date[0]
            holderParent.tvOrderAmount.text =
                activity.getString(R.string.for_amount_on) + Session(activity).getData(
                    Constant.CURRENCY
                ) + ApiConfig.stringFormat(
                    order.final_total
                )
            holderParent.lytMain.setOnClickListener { v: View? ->
                val fragment: Fragment = OrderDetailFragment()
                val bundle = Bundle()
                bundle.putString(Constant.ID, "")
                bundle.putSerializable("model", order)
                fragment.arguments = bundle
                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            val items = ArrayList<String>()
            for (i in order.items.indices) {
                items.add(order.items[i].name)
            }
            holderParent.tvItems.text =
                Arrays.toString(items.toTypedArray()).replace("]", "").replace("[", "")
            holderParent.tvTotalItems.text =
                items.size.toString() + if (items.size > 1) activity.getString(R.string.items) else activity.getString(
                    R.string.item
                )
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return orderTrackerArrayList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (orderTrackerArrayList[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderID: TextView
        val tvOrderDate: TextView
        val tvOrderAmount: TextView
        val tvTotalItems: TextView
        val tvItems: TextView
        val lytMain: RelativeLayout

        init {
            tvOrderID = itemView.findViewById(R.id.tvOrderID)
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate)
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount)
            tvTotalItems = itemView.findViewById(R.id.tvTotalItems)
            tvItems = itemView.findViewById(R.id.tvItems)
            lytMain = itemView.findViewById(R.id.lytMain)
        }
    }

    init {
        this.activity = activity
        this.orderTrackerArrayList = orderTrackerArrayList
    }
}