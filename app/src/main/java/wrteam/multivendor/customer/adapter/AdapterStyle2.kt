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
import androidx.cardview.widget.CardView
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
class AdapterStyle2(activity: Activity, productList: ArrayList<Product>) :
    RecyclerView.Adapter<AdapterStyle2.HolderItems>() {
    val productList: ArrayList<Product>
    val activity: Activity
    var session: Session
    var isLogin: Boolean
    var databaseHelper: DatabaseHelper
    override fun getItemCount(): Int {
        return 1
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HolderItems, position: Int) {
        if (productList.size > 0) {
            val product = productList[0]
            val variant = product.variants[0]
            val maxCartCont: String?
            maxCartCont =
                if (product.total_allowed_quantity == null || product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
                    session.getData(Constant.max_cart_items_count)
                } else {
                    product.total_allowed_quantity
                }
            val price: Double
            val oPrice: Double
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (productList[0].tax_percentage.toDouble() > 0) productList[0].tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (variant.discounted_price == "0" || variant.discounted_price == "") {
                holder.tvSubStyle2_1_.visibility = View.GONE
                price =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            } else {
                price =
                    (variant.discounted_price.toFloat() + variant.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                oPrice =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                holder.tvSubStyle2_1_.paintFlags =
                    holder.tvSubStyle2_1_.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.tvSubStyle2_1_.text =
                    Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
                holder.tvSubStyle2_1_.visibility = View.VISIBLE
            }
            holder.tvSubStyle2_1.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
            holder.tvStyle2_1.text = product.name
            Glide.with(activity).load(product.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.imgStyle2_1)
            holder.layoutStyle2_1.setOnClickListener { view: View? ->
                val activity1 = activity as AppCompatActivity
                val fragment: Fragment = ProductDetailFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "section")
                bundle.putInt(Constant.VARIANT_POSITION, 0)
                bundle.putString(Constant.ID, product.id)
                fragment.arguments = bundle
                activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            if (isLogin) {
                holder.tvQuantity2_1.text = variant.cart_count
            } else {
                holder.tvQuantity2_1.text =
                    databaseHelper.CheckCartItemExist(variant.id, variant.product_id)
            }
            holder.btnAddToCart2_1.visibility =
                if (holder.tvQuantity2_1.text == "0") View.VISIBLE else View.GONE
            holder.imgAdd2_1.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_1,
                    holder.btnAddToCart2_1,
                    true,
                    maxCartCont
                )
            }
            holder.imgMinus2_1.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_1,
                    holder.btnAddToCart2_1,
                    false,
                    maxCartCont
                )
            }
            holder.btnAddToCart2_1.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_1,
                    holder.btnAddToCart2_1,
                    true,
                    maxCartCont
                )
            }
        }
        if (productList.size > 1) {
            val product = productList[1]
            val variant = product.variants[0]
            val maxCartCont: String?
            maxCartCont =
                if (product.total_allowed_quantity == null || product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
                    session.getData(Constant.max_cart_items_count)
                } else {
                    product.total_allowed_quantity
                }
            val price: Double
            val oPrice: Double
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (productList[1].tax_percentage.toDouble() > 0) productList[1].tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (variant.discounted_price == "0" || variant.discounted_price == "") {
                holder.tvSubStyle2_2_.visibility = View.GONE
                price =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            } else {
                price =
                    (variant.discounted_price.toFloat() + variant.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                oPrice =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                holder.tvSubStyle2_2_.paintFlags =
                    holder.tvSubStyle2_2_.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.tvSubStyle2_2_.text =
                    Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
                holder.tvSubStyle2_2_.visibility = View.VISIBLE
            }
            holder.tvStyle2_2.text = product.name
            holder.tvSubStyle2_2.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
            Glide.with(activity).load(product.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.imgStyle2_2)
            holder.layoutStyle2_2.setOnClickListener { view: View? ->
                val activity1 = activity as AppCompatActivity
                val fragment: Fragment = ProductDetailFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "section")
                bundle.putInt(Constant.VARIANT_POSITION, 0)
                bundle.putString(Constant.ID, product.id)
                fragment.arguments = bundle
                activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            if (isLogin) {
                holder.tvQuantity2_2.text = variant.cart_count
            } else {
                holder.tvQuantity2_2.text =
                    databaseHelper.CheckCartItemExist(variant.id, variant.product_id)
            }
            holder.btnAddToCart2_2.visibility =
                if (holder.tvQuantity2_2.text == "0") View.VISIBLE else View.GONE
            holder.imgAdd2_2.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_2,
                    holder.btnAddToCart2_2,
                    true,
                    maxCartCont
                )
            }
            holder.imgMinus2_2.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_2,
                    holder.btnAddToCart2_2,
                    false,
                    maxCartCont
                )
            }
            holder.btnAddToCart2_2.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_2,
                    holder.btnAddToCart2_2,
                    true,
                    maxCartCont
                )
            }
        }
        if (productList.size > 2) {
            val product = productList[2]
            val variant = product.variants[0]
            val maxCartCont: String?
            maxCartCont =
                if (product.total_allowed_quantity == null || product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
                    session.getData(Constant.max_cart_items_count)
                } else {
                    product.total_allowed_quantity
                }
            holder.tvStyle2_3.text = product.name
            val price: Double
            val oPrice: Double
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (productList[2].tax_percentage.toDouble() > 0) productList[2].tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (variant.discounted_price == "0" || variant.discounted_price == "") {
                holder.tvSubStyle2_3_.visibility = View.GONE
                price =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            } else {
                price =
                    (variant.discounted_price.toFloat() + variant.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                oPrice =
                    (variant.price.toFloat() + variant.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                holder.tvSubStyle2_3_.paintFlags =
                    holder.tvSubStyle2_3_.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.tvSubStyle2_3_.text =
                    Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + oPrice)
                holder.tvSubStyle2_3_.visibility = View.VISIBLE
            }
            holder.tvSubStyle2_3.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
            Glide.with(activity).load(product.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.imgStyle2_3)
            holder.layoutStyle2_3.setOnClickListener { view: View? ->
                val activity1 = activity as AppCompatActivity
                val fragment: Fragment = ProductDetailFragment()
                val bundle = Bundle()
                bundle.putString(Constant.FROM, "section")
                bundle.putInt(Constant.VARIANT_POSITION, 0)
                bundle.putString(Constant.ID, product.id)
                fragment.arguments = bundle
                activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            if (isLogin) {
                holder.tvQuantity2_3.text = variant.cart_count
            } else {
                holder.tvQuantity2_3.text =
                    databaseHelper.CheckCartItemExist(variant.id, variant.product_id)
            }
            holder.btnAddToCart2_3.visibility =
                if (holder.tvQuantity2_3.text == "0") View.VISIBLE else View.GONE
            holder.imgAdd2_3.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_3,
                    holder.btnAddToCart2_3,
                    true,
                    maxCartCont
                )
            }
            holder.imgMinus2_3.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_3,
                    holder.btnAddToCart2_3,
                    false,
                    maxCartCont
                )
            }
            holder.btnAddToCart2_3.setOnClickListener { v: View? ->
                addQuantity(
                    variant,
                    holder.tvQuantity2_3,
                    holder.btnAddToCart2_3,
                    true,
                    maxCartCont
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun addQuantity(
        extra: PriceVariation,
        tvQuantity: TextView,
        btnAddToCart: TextView,
        isAdd: Boolean,
        maxCartCont: String?
    ) {
        try {
            if (session.getData(Constant.STATUS) == "1") {
                var count = tvQuantity.text.toString().toInt()
                if (isAdd) {
                    count++
                    if (extra.stock.toFloat() >= count) {
                        if (maxCartCont!!.toFloat() >= count) {
                            tvQuantity.text = "" + count
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
                    tvQuantity.text = "" + count
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
                    btnAddToCart.visibility = View.VISIBLE
                } else {
                    btnAddToCart.visibility = View.GONE
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderItems {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lyt_style_2, parent, false)
        return HolderItems(view)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgStyle2_1: ImageView
        val imgStyle2_2: ImageView
        val imgStyle2_3: ImageView
        val tvStyle2_1: TextView
        val tvStyle2_2: TextView
        val tvStyle2_3: TextView
        val tvSubStyle2_1: TextView
        val tvSubStyle2_1_: TextView
        val tvSubStyle2_2: TextView
        val tvSubStyle2_2_: TextView
        val tvSubStyle2_3: TextView
        val tvSubStyle2_3_: TextView
        val layoutStyle2_1: CardView
        val layoutStyle2_2: CardView
        val layoutStyle2_3: CardView
        val tvStatus2_1: TextView
        val tvStatus2_2: TextView
        val tvStatus2_3: TextView
        val lytQuantity2_1: RelativeLayout
        val lytQuantity2_2: RelativeLayout
        val lytQuantity2_3: RelativeLayout
        val imgMinus2_1: ImageView
        val imgMinus2_2: ImageView
        val imgMinus2_3: ImageView
        val tvQuantity2_1: TextView
        val tvQuantity2_2: TextView
        val tvQuantity2_3: TextView
        val imgAdd2_1: ImageView
        val imgAdd2_2: ImageView
        val imgAdd2_3: ImageView
        val btnAddToCart2_1: TextView
        val btnAddToCart2_2: TextView
        val btnAddToCart2_3: TextView

        init {
            imgStyle2_1 = itemView.findViewById(R.id.imgStyle2_1)
            imgStyle2_2 = itemView.findViewById(R.id.imgStyle2_2)
            imgStyle2_3 = itemView.findViewById(R.id.imgStyle2_3)
            tvStyle2_1 = itemView.findViewById(R.id.tvStyle2_1)
            tvStyle2_2 = itemView.findViewById(R.id.tvStyle2_2)
            tvStyle2_3 = itemView.findViewById(R.id.tvStyle2_3)
            tvSubStyle2_1 = itemView.findViewById(R.id.tvSubStyle2_1)
            tvSubStyle2_1_ = itemView.findViewById(R.id.tvSubStyle2_1_)
            tvSubStyle2_2 = itemView.findViewById(R.id.tvSubStyle2_2)
            tvSubStyle2_2_ = itemView.findViewById(R.id.tvSubStyle2_2_)
            tvSubStyle2_3 = itemView.findViewById(R.id.tvSubStyle2_3)
            tvSubStyle2_3_ = itemView.findViewById(R.id.tvSubStyle2_3_)
            layoutStyle2_1 = itemView.findViewById(R.id.layoutStyle2_1)
            layoutStyle2_2 = itemView.findViewById(R.id.layoutStyle2_2)
            layoutStyle2_3 = itemView.findViewById(R.id.layoutStyle2_3)
            tvStatus2_1 = itemView.findViewById(R.id.tvStatus2_1)
            tvStatus2_2 = itemView.findViewById(R.id.tvStatus2_2)
            tvStatus2_3 = itemView.findViewById(R.id.tvStatus2_3)
            lytQuantity2_1 = itemView.findViewById(R.id.lytQuantity2_1)
            lytQuantity2_2 = itemView.findViewById(R.id.lytQuantity2_2)
            lytQuantity2_3 = itemView.findViewById(R.id.lytQuantity2_3)
            imgMinus2_1 = itemView.findViewById(R.id.imgMinus2_1)
            imgMinus2_2 = itemView.findViewById(R.id.imgMinus2_2)
            imgMinus2_3 = itemView.findViewById(R.id.imgMinus2_3)
            tvQuantity2_1 = itemView.findViewById(R.id.tvQuantity2_1)
            tvQuantity2_2 = itemView.findViewById(R.id.tvQuantity2_2)
            tvQuantity2_3 = itemView.findViewById(R.id.tvQuantity2_3)
            imgAdd2_1 = itemView.findViewById(R.id.imgAdd2_1)
            imgAdd2_2 = itemView.findViewById(R.id.imgAdd2_2)
            imgAdd2_3 = itemView.findViewById(R.id.imgAdd2_3)
            btnAddToCart2_1 = itemView.findViewById(R.id.btnAddToCart2_1)
            btnAddToCart2_2 = itemView.findViewById(R.id.btnAddToCart2_2)
            btnAddToCart2_3 = itemView.findViewById(R.id.btnAddToCart2_3)
        }
    }

    init {
        this.activity = activity
        session = Session(activity)
        databaseHelper = DatabaseHelper(activity)
        isLogin = session.getBoolean(Constant.IS_USER_LOGIN)
        this.productList = productList
    }
}