package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.ApiConfig.Companion.requestToVolley
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.OrderItems
import com.fallgan.customerapp.model.TrackTimeLine

class OrderItemsAdapter(
    val activity: Activity,
    private val orderTrackerArrayList: ArrayList<OrderItems>,
    var recyclerView: RecyclerView,
    var lytMain: RelativeLayout,
    private var lytTrackerTimeLine: LinearLayout
) : RecyclerView.Adapter<OrderItemsAdapter.CartItemHolder>() {
    val session: Session = Session(activity)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemHolder {
        @SuppressLint("InflateParams") val v =
            LayoutInflater.from(parent.context).inflate(R.layout.lyt_order_items, null)
        return CartItemHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CartItemHolder, position: Int) {
        val orderItems = orderTrackerArrayList[position]
        holder.tvQuantity.text = orderItems.quantity
        val taxPercentage: String = orderItems.tax_percentage
        holder.tvActiveStatus.text = ApiConfig.toTitleCase(orderItems.active_status)
        if (orderItems.cancelable_status == "1") {
            if (orderItems.till_status == Constant.RECEIVED && orderItems.active_status == Constant.RECEIVED) {
                holder.btnCancel.visibility = View.VISIBLE
            } else if (orderItems.till_status == Constant.PROCESSED && (orderItems.active_status == Constant.RECEIVED || orderItems.active_status == Constant.PROCESSED)) {
                holder.btnCancel.visibility = View.VISIBLE
            } else if (orderItems.till_status == Constant.SHIPPED && (orderItems.active_status == Constant.RECEIVED || orderItems.active_status == Constant.PROCESSED || orderItems.active_status == Constant.SHIPPED)) {
                holder.btnCancel.visibility = View.VISIBLE
            } else {
                holder.btnCancel.visibility = View.GONE
            }
        } else {
            holder.btnCancel.visibility = View.GONE
        }
        if (orderItems.return_status == "1") {
            if (orderItems.active_status == Constant.DELIVERED) {
                holder.btnReturn.visibility = View.VISIBLE
            }
        } else {
            holder.btnReturn.visibility = View.GONE
        }
        val discountedPrice: Double =
            if (orderItems.discounted_price == "0" || orderItems.discounted_price == "") {
                ((orderItems.price.toFloat() + orderItems.price
                    .toFloat() * taxPercentage.toFloat() / 100) * orderItems.quantity
                    .toInt()).toDouble()
            } else {
                ((orderItems.discounted_price.toFloat() + orderItems.discounted_price
                    .toFloat() * taxPercentage.toFloat() / 100) * orderItems.quantity
                    .toInt()).toDouble()
            }
        holder.tvPrice.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + discountedPrice)
        holder.tvName.text = orderItems.name + "(" + orderItems.measurement + orderItems.unit + ")"

        Glide.with(activity).load(orderItems.image)
            .centerInside()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imgOrder)


        holder.btnCancel.setOnClickListener {
            updateOrderStatus(
                activity,
                orderItems,
                Constant.CANCELLED,
                holder
            )
        }

        holder.btnReturn.setOnClickListener {
            updateOrderStatus(
                activity,
                orderItems,
                Constant.RETURNED,
                holder
            )
        }

        holder.tvTrackItem.setOnClickListener {
            if (orderItems.shipping_method != "local" && orderItems.shipment_id == "0") {
                Toast.makeText(activity, orderItems.active_status, Toast.LENGTH_SHORT).show()
            } else {
                getTrackerData(orderItems)
            }
        }
    }

    private fun updateOrderStatus(
        activity: Activity,
        orderItems: OrderItems,
        status: String,
        holder: CartItemHolder
    ) {
        val alertDialog = AlertDialog.Builder(activity)
        alertDialog.setTitle(R.string.logout)
        alertDialog.setMessage(R.string.logout_msg)
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()

        // Setting OK Button
        alertDialog.setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
            val params: MutableMap<String, String> = HashMap()
            params[Constant.UPDATE_ORDER_STATUS] = Constant.GetVal
            params[Constant.ORDER_ID] = orderItems.order_id
            params[Constant.ORDER_ITEM_ID] = orderItems.id
            params[Constant.STATUS] = status
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                if (status == Constant.CANCELLED) {
                                    holder.btnCancel.visibility = View.GONE
                                    orderItems.active_status = status
                                    orderTrackerArrayList.size
                                    ApiConfig.getWalletBalance(activity, session)
                                } else {
                                    holder.btnReturn.visibility = View.GONE
                                }
                            }
                            Toast.makeText(
                                activity,
                                jsonObject.getString("message"),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        } catch (e: Exception) {
                            e.printStackTrace()

                        }
                    }
                }
            }, activity, Constant.ORDER_PROCESS_URL, params, true)
        }
        alertDialog.setNegativeButton(R.string.no) { _: DialogInterface?, _: Int -> alertDialog1.dismiss() }
        // Showing Alert Message
        alertDialog.show()
    }

    private fun getTrackerData(orderItems: OrderItems) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.TRACK_ORDER] = Constant.GetVal
        if (orderItems.shipping_method == "local") {
            params[Constant.ORDER_ITEM_ID] = orderItems.id
        } else {
            params[Constant.SHIPMENT_ID] = orderItems.shipment_id
        }

        requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        showDialog(activity, jsonObject.getJSONArray("activities"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, params, true)
    }


    private fun showDialog(activity: Activity, jsonArray: JSONArray) {
        try {
            lytMain.visibility = View.VISIBLE
            lytTrackerTimeLine.visibility = View.VISIBLE
            lytTrackerTimeLine.startAnimation(
                AnimationUtils.loadAnimation(
                    activity,
                    R.anim.view_show
                )
            )
            val trackTimeLines = ArrayList<TrackTimeLine>()
            for (i in 0 until jsonArray.length()) {
                val trackTimeLine = Gson().fromJson(
                    jsonArray.getJSONObject(i).toString(),
                    TrackTimeLine::class.java
                )
                trackTimeLines.add(trackTimeLine)
            }
            val orderTimeLineAdapter = OrderTimeLineAdapter(activity, trackTimeLines)
            recyclerView.adapter = orderTimeLineAdapter
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    override fun getItemCount(): Int {
        return orderTrackerArrayList.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class CartItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvActiveStatus: TextView = itemView.findViewById(R.id.tvActiveStatus)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvTrackItem: TextView = itemView.findViewById(R.id.tvTrackItem)
        val imgOrder: ImageView = itemView.findViewById(R.id.imgOrder)
        val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
        val btnReturn: Button = itemView.findViewById(R.id.btnReturn)

    }

}