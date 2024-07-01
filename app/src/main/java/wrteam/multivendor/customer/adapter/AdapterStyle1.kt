package com.gpn.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.gpn.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gpn.customerapp.R
import com.gpn.customerapp.fragment.ProductDetailFragment
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.DatabaseHelper
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.model.PriceVariation
import com.gpn.customerapp.model.Product

/**
 * Created by shree1 on 3/16/2017.
 */
class AdapterStyle1(
    val activity: Activity, productList: ArrayList<Product>,
    private val itemResource: Int
) :
    RecyclerView.Adapter<AdapterStyle1.HolderItems>() {

    var productList = productList
    var session: Session = Session(activity)
    private var isLogin = session.getBoolean(Constant.IS_USER_LOGIN)
    var databaseHelper = DatabaseHelper(activity)

    override fun getItemCount(): Int {
        return Math.min(productList.size, 4)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderItems, position: Int) {
        val product = productList[position]
        val variant = product.variants[0]
        val maxCartCont =
            if (product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
                session.getData(Constant.max_cart_items_count)
            } else {
                product.total_allowed_quantity
            }
        if (variant.serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.lytQuantity.visibility = View.GONE
        } else {
            holder.tvStatus.visibility = View.GONE
            holder.lytQuantity.visibility = View.VISIBLE
        }
        Glide.with(activity).load(product.image)
            .centerInside()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.thumbnail)
        holder.tvTitle.text = product.name
        val price: Double
        val oPrice: Double
        var taxPercentage = "0"
        try {
            taxPercentage =
                if (product.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (variant.discounted_price == "0" || variant.discounted_price == "") {
            holder.tvDPrice.visibility = View.GONE
            price =
                (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
        } else {
            price =
                (variant.discounted_price.toFloat() + variant.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            oPrice =
                (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            holder.tvDPrice.paintFlags = holder.tvDPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvDPrice.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
            holder.tvDPrice.visibility = View.VISIBLE
        }
        holder.tvPrice.text =
            Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
        holder.tvTitle.text = product.name
        if (isLogin) {
            holder.tvQuantity.text = variant.cart_count
        } else {
            holder.tvQuantity.text = databaseHelper.CheckCartItemExist(
                product.variants[0].id,
                product.variants[0].product_id
            )
        }
        holder.btnAddToCart.visibility =
            if (holder.tvQuantity.text == "0") View.VISIBLE else View.GONE
        holder.relativeLayout.setOnClickListener { view: View? ->
            val activity1 = activity as AppCompatActivity
            val fragment: Fragment = ProductDetailFragment()
            val bundle = Bundle()
            bundle.putString(Constant.ID, product.variants[0].product_id)
            bundle.putString(Constant.FROM, "section")
            bundle.putInt(Constant.VARIANT_POSITION, 0)
            fragment.arguments = bundle
            activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                .addToBackStack(null).commit()
        }
        holder.imgAdd.setOnClickListener { v: View? ->
            addQuantity(
                variant,
                holder,
                true,
                maxCartCont
            )
        }
        holder.imgMinus.setOnClickListener { v: View? ->
            addQuantity(
                variant,
                holder,
                false,
                maxCartCont
            )
        }
        holder.btnAddToCart.setOnClickListener { v: View? ->
            addQuantity(
                variant,
                holder,
                true,
                maxCartCont
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderItems {
        val view = LayoutInflater.from(parent.context).inflate(itemResource, parent, false)
        return HolderItems(view)
    }

    @SuppressLint("SetTextI18n")
    fun addQuantity(
        extra: PriceVariation,
        holder: HolderItems,
        isAdd: Boolean,
        maxCartCont: String?
    ) {
        try {
            if (session.getData(Constant.STATUS) == "1") {
                var count = holder.tvQuantity.text.toString().toInt()
                if (isAdd) {
                    count++
                    if (extra.stock.toFloat() >= count) {
                        if (maxCartCont!!.toFloat() >= count) {
                            holder.tvQuantity.text = "" + count
                            if (isLogin) {
                                if (Constant.CartValues.containsKey(extra.id)) {
                                    Constant.CartValues.replace(extra.id, "" + count)
                                } else {
                                    Constant.CartValues[extra.id] = "" + count
                                }
                                ApiConfig.addMultipleProductInCart(
                                    session,
                                    activity,
                                    Constant.CartValues
                                )
                            } else {
                                databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                                databaseHelper.getTotalItemOfCart(activity)
                                activity.invalidateOptionsMenu()
                            }
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
                    holder.tvQuantity.text = "" + count
                    if (isLogin) {
                        if (Constant.CartValues.containsKey(extra.id)) {
                            Constant.CartValues.replace(extra.id, "" + count)
                        } else {
                            Constant.CartValues[extra.id] = "" + count
                        }
                        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                    } else {
                        databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                        databaseHelper.getTotalItemOfCart(activity)
                        activity.invalidateOptionsMenu()
                    }
                }
                if (count == 0) {
                    holder.btnAddToCart.visibility = View.VISIBLE
                } else {
                    holder.btnAddToCart.visibility = View.GONE
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

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        val imgAdd: ImageView = itemView.findViewById(R.id.imgAdd)
        val imgMinus: ImageView = itemView.findViewById(R.id.imgMinus)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvDPrice: TextView = itemView.findViewById(R.id.tvDPrice)
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.play_layout)
        val lytQuantity: RelativeLayout = itemView.findViewById(R.id.lytQuantity)
        val btnAddToCart: TextView = itemView.findViewById(R.id.btnAddToCart)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

    }
}