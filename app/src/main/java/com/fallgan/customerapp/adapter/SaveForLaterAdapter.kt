package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fallgan.customerapp.R
import com.fallgan.customerapp.fragment.CartFragment
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.model.Cart

@SuppressLint("NotifyDataSetChanged")
class SaveForLaterAdapter(val activity: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val session: Session = Session(activity)
    var taxPercentage: String
    fun add(position: Int, item: Cart) {
        CartFragment.saveForLater.add(position, item)
        notifyItemInserted(position)
    }

    @SuppressLint("SetTextI18n")
    fun removeItem(position: Int) {
        val cart: Cart = CartFragment.saveForLater.get(position)
        if (Constant.CartValues.containsKey(cart.product_variant_id)) {
            Constant.CartValues.replace(cart.product_variant_id, "0")
        } else {
            Constant.CartValues.put(cart.product_variant_id, "0")
        }
        CartFragment.saveForLater.remove(cart)
        notifyDataSetChanged()
        activity.invalidateOptionsMenu()
        CartFragment.tvSaveForLaterTitle.setText(activity.resources.getString(R.string.save_for_later) + " (" + itemCount + ")")
        CartFragment.lytEmpty.setVisibility(if (itemCount == 0 && CartFragment.carts.size === 0) View.VISIBLE else View.GONE)
        if (itemCount == 0) CartFragment.lytSaveForLater.setVisibility(View.GONE)
    }

    fun totalCalculate(cart: Cart) {
        var taxPercentage1 = "0"
        try {
            taxPercentage1 =
                if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val price: Double
        price = if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
            (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage1.toFloat() / 100).toDouble()
        } else {
            (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage1.toFloat() / 100).toDouble()
        }
        Constant.FLOAT_TOTAL_AMOUNT += price * cart.qty.toInt()
        Constant.TOTAL_CART_ITEM = CartFragment.carts.size
        CartFragment.setData(activity)
        activity.invalidateOptionsMenu()
    }

    @SuppressLint("SetTextI18n")
    fun moveItem(position: Int) {
        try {
            val cart: Cart = CartFragment.saveForLater.get(position)
            CartFragment.isSoldOut = false
            CartFragment.isDeliverable = false
            CartFragment.carts.add(cart)
            CartFragment.cartAdapter.notifyDataSetChanged()
            CartFragment.saveForLater.remove(cart)
            CartFragment.saveForLaterAdapter.notifyDataSetChanged()
            totalCalculate(cart)
            if (itemCount == 0) CartFragment.lytSaveForLater.setVisibility(View.GONE)
            if (CartFragment.carts.size !== 0) CartFragment.lytTotal.setVisibility(View.VISIBLE)
            CartFragment.tvSaveForLaterTitle.setText(activity.resources.getString(R.string.save_for_later) + " (" + itemCount + ")")
            Constant.CartValues.put(cart.product_variant_id, cart.qty)
            ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity)
                    .inflate(R.layout.lyt_save_for_later, parent, false)
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
        try {
            val viewType = getItemViewType(position)
            when (viewType) {
                viewTypeItem -> {
                    val holder = holderParent as HolderItems
                    val cart: Cart = CartFragment.saveForLater.get(position)
                    val price: Double
                    val oPrice: Double
                    try {
                        taxPercentage =
                            if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Glide.with(activity).load(cart.item[0].image)
                        .centerInside()
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(holder.imgProduct)
                    holder.tvDelete.setOnClickListener { v: View? -> removeItem(position) }
                    holder.tvAction.setOnClickListener { v: View? -> moveItem(position) }
                    holder.tvProductName.text = cart.item[0].name
                    holder.tvMeasurement.text =
                        cart.item[0].measurement + "\u0020" + cart.item[0].unit
                    if (cart.item[0].serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
                        holder.tvStatus.visibility = View.VISIBLE
                    }
                    if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                        price =
                            (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                    } else {
                        price =
                            (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                        oPrice =
                            (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                        holder.tvOriginalPrice.paintFlags =
                            holder.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        holder.tvOriginalPrice.setText(
                            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                                "" + oPrice
                            )
                        )
                    }
                    holder.tvPrice.setText(
                        session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                            "" + price
                        )
                    )
                }
                viewTypeLoading -> {
                    val loadingViewHolder = holderParent as ViewHolderLoading
                    loadingViewHolder.progressBar.isIndeterminate = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return CartFragment.saveForLater.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (CartFragment.saveForLater.get(position) == null) viewTypeLoading else viewTypeItem
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView
        val tvProductName: TextView
        val tvMeasurement: TextView
        val tvPrice: TextView
        val tvOriginalPrice: TextView
        val tvDelete: TextView
        val tvAction: TextView
        val tvStatus: TextView
        val lytMain: RelativeLayout

        init {
            lytMain = itemView.findViewById(R.id.lytMain)
            imgProduct = itemView.findViewById(R.id.imgProduct)
            tvDelete = itemView.findViewById(R.id.tvDelete)
            tvAction = itemView.findViewById(R.id.tvAction)
            tvStatus = itemView.findViewById(R.id.tvStatus)
            tvProductName = itemView.findViewById(R.id.tvProductName)
            tvMeasurement = itemView.findViewById(R.id.tvMeasurement)
            tvPrice = itemView.findViewById(R.id.tvPrice)
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice)
        }
    }

    init {
        taxPercentage = "0"
    }
}