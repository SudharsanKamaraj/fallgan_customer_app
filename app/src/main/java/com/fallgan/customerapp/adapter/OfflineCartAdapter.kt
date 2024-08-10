package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.fallgan.customerapp.R
import com.fallgan.customerapp.fragment.CartFragment
import com.fallgan.customerapp.fragment.CartFragment.Companion.offlineCarts
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.DatabaseHelper
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.model.OfflineCart
import com.fallgan.customerapp.model.OfflineItems

@SuppressLint("NotifyDataSetChanged")
class OfflineCartAdapter(activity: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity = activity
    val databaseHelper: DatabaseHelper
    val session: Session
    fun add(position: Int, cart: OfflineCart) {
        CartFragment.isSoldOut = false
        CartFragment.isDeliverable = false
        if (position != 0) {
            offlineCarts.add(position, cart)
        } else {
            offlineCarts.add(cart)
        }
        CartFragment.offlineCartAdapter.notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        val cart: OfflineCart = offlineCarts.get(position)
        totalCalculate(cart, false, false)
        if (Constant.CartValues.containsKey(cart.product_variant_id)) {
            Constant.CartValues.replace(cart.product_variant_id, "0")
        } else {
            Constant.CartValues.put(cart.product_variant_id, "0")
        }
        offlineCarts.remove(cart)
        CartFragment.isSoldOut = false
        CartFragment.isDeliverable = false
        notifyDataSetChanged()
        Constant.TOTAL_CART_ITEM = itemCount
        CartFragment.setData(activity)
        activity.invalidateOptionsMenu()
        if (itemCount == 0 && CartFragment.offlineSaveForLaterItems.size === 0) {
            CartFragment.lytEmpty.setVisibility(View.VISIBLE)
            CartFragment.lytTotal.setVisibility(View.GONE)
        } else {
            CartFragment.lytEmpty.setVisibility(View.GONE)
            CartFragment.lytTotal.setVisibility(View.VISIBLE)
        }
        databaseHelper.RemoveFromCart(cart.product_variant_id, cart.product_id)
        showUndoSnackBar(cart, position)
    }

    @SuppressLint("SetTextI18n")
    fun moveItem(position: Int) {
        try {
            val cart: OfflineCart = offlineCarts.get(position)
            offlineCarts.remove(cart)
            CartFragment.isSoldOut = false
            CartFragment.isDeliverable = false
            totalCalculate(cart, false, false)
            CartFragment.offlineSaveForLaterAdapter.add(0, cart)
            if (CartFragment.lytSaveForLater.getVisibility() === View.GONE) CartFragment.lytSaveForLater.setVisibility(
                View.VISIBLE
            )
            CartFragment.tvSaveForLaterTitle.setText(activity.resources.getString(R.string.save_for_later) + " (" + CartFragment.offlineSaveForLaterItems.size + ")")
            CartFragment.saveForLaterValues.put(
                cart.product_variant_id,
                databaseHelper.CheckCartItemExist(cart.product_variant_id, cart.product_id)
            )
            Constant.TOTAL_CART_ITEM = itemCount
            if (itemCount == 0) CartFragment.lytTotal.setVisibility(View.GONE)
            databaseHelper.MoveToCartOrSaveForLater(
                cart.product_variant_id,
                cart.product_id,
                "cart",
                activity
            )
            CartFragment.offlineCartAdapter.notifyDataSetChanged()
            CartFragment.offlineSaveForLaterAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun totalCalculate(cart: OfflineCart, isAdd: Boolean, isSingleQty: Boolean) {
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
        if (isAdd) {
            Constant.FLOAT_TOTAL_AMOUNT += if (isSingleQty) price else price * cart.item[0].cart_count.toInt()
        } else {
            Constant.FLOAT_TOTAL_AMOUNT -= if (isSingleQty) price else price * cart.item[0].cart_count.toInt()
        }
        CartFragment.setData(activity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity).inflate(R.layout.lyt_cartlist, parent, false)
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
            val cart: OfflineCart = offlineCarts.get(position)
            Glide.with(activity).load(cart.item[0].image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holderParent.imgProduct)
            holderParent.tvProductName.text = cart.item[0].name
            holderParent.tvMeasurement.text =
                cart.item[0].measurement + "\u0020" + cart.item[0].unit
            val price: Double
            val oPrice: Double
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (cart.item[0].serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
                holderParent.tvStatus.visibility = View.VISIBLE
                holderParent.tvQuantity.visibility = View.GONE
                CartFragment.isSoldOut = true
            } else {
                holderParent.tvStatus.visibility = View.GONE
            }
            if (!cart.item[0].is_item_deliverable && session.getData(
                    Constant.SHIPPING_TYPE
                ) == "local"
            ) {
                holderParent.txtDeliveryStatus.visibility = View.VISIBLE
                holderParent.txtDeliveryStatus.text =
                    activity.getString(R.string.msg_non_deliverable_to) + session.getData(
                        Constant.GET_SELECTED_PINCODE_NAME
                    )
                CartFragment.isDeliverable = true
            } else {
                holderParent.txtDeliveryStatus.visibility = View.GONE
            }
            holderParent.tvPrice.text =
                Session(activity).getData(Constant.CURRENCY) + if (cart.discounted_price == "0") cart.price else cart.discounted_price
            holderParent.tvDelete.setOnClickListener { v: View? -> removeItem(position) }
            holderParent.tvAction.setOnClickListener { v: View? -> moveItem(position) }
            if (cart.discounted_price == "0" || cart.discounted_price == "") {
                price =
                    (cart.price.toFloat() + cart.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            } else {
                price =
                    (cart.discounted_price.toFloat() + cart.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                oPrice =
                    (cart.price.toFloat() + cart.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                holderParent.tvOriginalPrice.paintFlags =
                    holderParent.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holderParent.tvOriginalPrice.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
            }
            holderParent.tvPrice.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
            holderParent.tvProductName.text = cart.item[0].name
            holderParent.tvMeasurement.text =
                cart.item[0].measurement + "\u0020" + cart.item[0].unit
            holderParent.tvQuantity.text = databaseHelper.CheckCartItemExist(
                offlineCarts.get(position).id,
                cart.product_id
            )
            cart.item[0].cart_count = databaseHelper.CheckCartItemExist(
                offlineCarts.get(position).id,
                cart.product_id
            )
            holderParent.tvTotalPrice.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    "" + price * databaseHelper.CheckCartItemExist(
                        offlineCarts.get(
                            position
                        ).id,
                        cart.product_id
                    ).toInt()
                )
            val maxCartCont: String?
            maxCartCont =
                if (cart.item[0].total_allowed_quantity == null || cart.item[0].total_allowed_quantity == "" || cart.item[0].total_allowed_quantity == "0") {
                    session.getData(Constant.max_cart_items_count)
                } else {
                    cart.item[0].total_allowed_quantity
                }
            holderParent.btnAddQuantity.setOnClickListener { view: View? ->
                addQuantity(
                    cart,
                    cart.item[0],
                    holderParent,
                    true,
                    maxCartCont,
                    price
                )
            }
            holderParent.btnMinusQuantity.setOnClickListener { view: View? ->
                addQuantity(
                    cart,
                    cart.item[0],
                    holderParent,
                    false,
                    maxCartCont,
                    price
                )
            }
            if (itemCount == 0) {
                CartFragment.lytEmpty.setVisibility(View.VISIBLE)
                CartFragment.lytTotal.setVisibility(View.GONE)
            } else {
                CartFragment.lytEmpty.setVisibility(View.GONE)
                CartFragment.lytTotal.setVisibility(View.VISIBLE)
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    @SuppressLint("SetTextI18n")
    fun addQuantity(
        cart: OfflineCart,
        cartItem: OfflineItems,
        holder: HolderItems,
        isAdd: Boolean,
        maxCartCont: String?,
        price: Double
    ) {
        try {
            if (session.getData(Constant.STATUS) == "1") {
                var count = holder.tvQuantity.text.toString().toInt()
                if (isAdd) {
                    count++
                    if (cartItem.stock.toFloat() >= count) {
                        if (maxCartCont!!.toFloat() >= count) {
                            cartItem.cart_count = "" + count
                            holder.tvQuantity.text = "" + count
                            totalCalculate(cart, true, true)
                            holder.tvTotalPrice.text =
                                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price * cartItem.cart_count.toInt())
                            if (Constant.CartValues.containsKey(cart.product_variant_id)) {
                                Constant.CartValues.replace(cart.product_variant_id, "" + count)
                            } else {
                                Constant.CartValues[cart.product_variant_id] = "" + count
                            }
                            ApiConfig.addMultipleProductInCart(
                                session,
                                activity,
                                Constant.CartValues
                            )
                        } else {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.limit_alert),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.stock_limit),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    count--
                    if (count > 0) {
                        cartItem.cart_count = "" + count
                        holder.tvQuantity.text = "" + count
                        totalCalculate(cart, false, true)
                        holder.tvTotalPrice.text =
                            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price * cartItem.cart_count.toInt())
                        if (Constant.CartValues.containsKey(cart.product_variant_id)) {
                            Constant.CartValues.replace(cart.product_variant_id, "" + count)
                        } else {
                            Constant.CartValues[cart.product_variant_id] = "" + count
                        }
                        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                    }
                }
                CartFragment.setData(activity)
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.user_block_msg),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        return offlineCarts.size
    }

    override fun getItemViewType(position: Int): Int {
        return viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        return offlineCarts.get(position).id.toLong()
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView
        val btnMinusQuantity: ImageView
        val btnAddQuantity: ImageView
        val tvDelete: TextView
        val tvAction: TextView
        val tvProductName: TextView
        val tvMeasurement: TextView
        val tvPrice: TextView
        val tvOriginalPrice: TextView
        val tvQuantity: TextView
        val tvTotalPrice: TextView
        val tvStatus: TextView
        val txtDeliveryStatus: TextView

        init {
            imgProduct = itemView.findViewById(R.id.imgProduct)
            tvDelete = itemView.findViewById(R.id.tvDelete)
            tvAction = itemView.findViewById(R.id.tvAction)
            btnMinusQuantity = itemView.findViewById(R.id.btnMinusQuantity)
            btnAddQuantity = itemView.findViewById(R.id.btnAddQuantity)
            tvProductName = itemView.findViewById(R.id.tvProductName)
            tvMeasurement = itemView.findViewById(R.id.tvMeasurement)
            tvPrice = itemView.findViewById(R.id.tvPrice)
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice)
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice)
            tvStatus = itemView.findViewById(R.id.tvStatus)
            tvQuantity = itemView.findViewById(R.id.tvQuantity)
            txtDeliveryStatus = itemView.findViewById(R.id.txtDeliveryStatus)
        }
    }

    fun showUndoSnackBar(cart: OfflineCart, position: Int) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.getString(R.string.undo_message),
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(ContextCompat.getColor(activity, R.color.gray))
        snackbar.setAction(activity.getString(R.string.undo)) { view: View? ->
            snackbar.dismiss()
            databaseHelper.AddToCart(
                cart.item[0].id,
                cart.item[0].product_id,
                cart.item[0].cart_count
            )
            totalCalculate(cart, true, false)
            add(position, cart)
            CartFragment.isSoldOut = false
            Constant.TOTAL_CART_ITEM = offlineCarts.size
            CartFragment.setData(activity)
            notifyDataSetChanged()
            activity.invalidateOptionsMenu()
        }.setActionTextColor(Color.WHITE)
        val snackBarView = snackbar.view
        val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
        textView.maxLines = 5
        snackbar.show()
    }

    init {
        databaseHelper = DatabaseHelper(activity)
        session = Session(activity)
    }
}