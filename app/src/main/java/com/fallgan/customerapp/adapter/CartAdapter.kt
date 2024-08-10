package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.fallgan.customerapp.R
import com.fallgan.customerapp.fragment.CartFragment
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.model.Cart
import com.fallgan.customerapp.model.CartItems

@SuppressLint("NotifyDataSetChanged")
class CartAdapter(val activity: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val session = Session(activity)
    fun add(position: Int, item: Cart) {
        CartFragment.isSoldOut = false
        CartFragment.isDeliverable = false
        CartFragment.carts.add(position, item)
        CartFragment.cartAdapter.notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        val cart: Cart = CartFragment.carts.get(position)
        totalCalculate(cart, false, false)
        if (Constant.CartValues.containsKey(cart.product_variant_id)) {
            Constant.CartValues.replace(cart.product_variant_id, "0")
        } else {
            Constant.CartValues.put(cart.product_variant_id, "0")
        }
        CartFragment.carts.remove(cart)
        CartFragment.isSoldOut = false
        CartFragment.isDeliverable = false
        notifyDataSetChanged()
        Constant.TOTAL_CART_ITEM = itemCount
        CartFragment.setData(activity)
        activity.invalidateOptionsMenu()
        if (itemCount == 0 && CartFragment.saveForLater.size === 0) {
            CartFragment.lytEmpty.setVisibility(View.VISIBLE)
            CartFragment.lytTotal.setVisibility(View.GONE)
        } else {
            CartFragment.lytEmpty.setVisibility(View.GONE)
            CartFragment.lytTotal.setVisibility(View.VISIBLE)
        }
        showUndoSnackBar(cart, position)
    }

    fun totalCalculate(cart: Cart, isAdd: Boolean, isSingleQty: Boolean) {
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
            Constant.FLOAT_TOTAL_AMOUNT += if (isSingleQty) price else price * cart.qty.toInt()
        } else {
            Constant.FLOAT_TOTAL_AMOUNT -= if (isSingleQty) price else price * cart.qty.toInt()
        }
        CartFragment.setData(activity)
    }

    @SuppressLint("SetTextI18n")
    fun moveItem(position: Int) {
        try {
            val cart: Cart = CartFragment.carts.get(position)
            totalCalculate(cart, false, false)
            CartFragment.isSoldOut = false
            CartFragment.isDeliverable = false
            CartFragment.carts.remove(cart)
            CartFragment.cartAdapter.notifyDataSetChanged()
            CartFragment.saveForLater.add(cart)
            CartFragment.saveForLaterAdapter.notifyDataSetChanged()
            if (CartFragment.lytSaveForLater.getVisibility() === View.GONE) CartFragment.lytSaveForLater.setVisibility(
                View.VISIBLE
            )
            CartFragment.tvSaveForLaterTitle.setText(activity.resources.getString(R.string.save_for_later) + " (" + CartFragment.saveForLater.size + ")")
            CartFragment.saveForLaterValues.put(cart.product_variant_id, cart.qty)
            Constant.TOTAL_CART_ITEM = itemCount
            CartFragment.setData(activity)
            if (itemCount == 0) CartFragment.lytTotal.setVisibility(View.GONE)
            ApiConfig.addMultipleProductInSaveForLater(
                session,
                activity,
                CartFragment.saveForLaterValues
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
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


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
        try {
            if (holderParent is HolderItems) {
                val cart: Cart = CartFragment.carts[position]
                val price: Double
                val oPrice: Double
                var taxPercentage = "0"
                try {
                    taxPercentage =
                        if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                    price =
                        (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                } else {
                    price =
                        (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                    oPrice =
                        (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                    holderParent.tvOriginalPrice.paintFlags =
                        holderParent.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holderParent.tvOriginalPrice.text =
                        session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
                }
                Glide.with(activity).load(cart.item[0].image)
                    .centerInside()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holderParent.imgProduct)
                holderParent.tvProductName.text = cart.item[0].name
                holderParent.tvDelete.setOnClickListener { v: View? -> removeItem(position) }
                holderParent.tvAction.setOnClickListener { v: View? -> moveItem(position) }
                holderParent.tvMeasurement.text =
                    cart.item[0].measurement + "\u0020" + cart.item[0].unit
                if (cart.item[0].serve_for == Constant.SOLD_OUT_TEXT) {
                    holderParent.tvStatus.visibility = View.VISIBLE
                    holderParent.lytQuantity.visibility = View.GONE
                    CartFragment.isSoldOut = true
                } else if (cart.qty.toFloat() > cart.item[0].stock.toFloat()) {
                    holderParent.tvStatus.visibility = View.VISIBLE
                    holderParent.tvStatus.text =
                        activity.getString(R.string.low_stock_warning1) + cart.item[0].stock + activity.getString(
                            R.string.low_stock_warning2
                        )
                    CartFragment.isSoldOut = true
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
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
                holderParent.tvQuantity.text = cart.qty
                holderParent.tvTotalPrice.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price * cart.qty.toInt())
                val maxCartCont: String? = if (cart.item[0].total_allowed_quantity == null || cart.item[0].total_allowed_quantity == "" || cart.item[0].total_allowed_quantity == "0") {
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
                    CartFragment.lytEmpty.visibility = View.VISIBLE
                    CartFragment.lytTotal.visibility = View.GONE
                } else {
                    CartFragment.lytEmpty.visibility = View.GONE
                    CartFragment.lytTotal.visibility = View.VISIBLE
                }
            } else if (holderParent is ViewHolderLoading) {
                holderParent.progressBar.isIndeterminate = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    fun addQuantity(
        cart: Cart,
        cartItem: CartItems,
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
                            cart.qty = "" + count
                            holder.tvQuantity.text = "" + count
                            totalCalculate(cart, true, true)
                            holder.tvTotalPrice.text =
                                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price * cart.qty.toInt())
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
                        cart.qty = "" + count
                        holder.tvQuantity.text = "" + count
                        totalCalculate(cart, false, true)
                        holder.tvTotalPrice.text =
                            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price * cart.qty.toInt())
                        if (Constant.CartValues.containsKey(cart.product_variant_id)) {
                            Constant.CartValues.replace(cart.product_variant_id, "" + count)
                        } else {
                            Constant.CartValues[cart.product_variant_id] = "" + count
                        }
                        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                    }
                }
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
        return CartFragment.carts.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (CartFragment.carts.get(position) == null) viewTypeLoading else viewTypeItem
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
        var lytQuantity: LinearLayout

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
            tvQuantity = itemView.findViewById(R.id.tvQuantity)
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice)
            tvStatus = itemView.findViewById(R.id.tvStatus)
            txtDeliveryStatus = itemView.findViewById(R.id.txtDeliveryStatus)
            lytQuantity = itemView.findViewById(R.id.lytQuantity)
        }
    }

    private fun showUndoSnackBar(cart: Cart, position: Int) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.getString(R.string.undo_message),
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(activity.getString(R.string.undo)) {
            snackbar.dismiss()
            Constant.CartValues[cart.product_variant_id] = cart.qty
            add(position, cart)
            notifyDataSetChanged()
            CartFragment.isSoldOut = false
            Constant.TOTAL_CART_ITEM = itemCount
            ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
            activity.invalidateOptionsMenu()
            totalCalculate(cart, true, false)
            CartFragment.setData(activity)
        }
        snackbar.setActionTextColor(Color.WHITE)
        val snackBarView = snackbar.view
        val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
        textView.maxLines = 5
        snackbar.show()
    }

}