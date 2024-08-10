package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.fallgan.customerapp.R
import com.fallgan.customerapp.fragment.FavoriteFragment
import com.fallgan.customerapp.fragment.ProductDetailFragment
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.DatabaseHelper
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.model.PriceVariation
import com.fallgan.customerapp.model.Product

@SuppressLint("NotifyDataSetChanged")
class ProductLoadMoreAdapter(
    var activity: Activity,
    var mDataset: ArrayList<Product?>,
    var resource: Int,
    var from: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    var session: Session = Session(activity)
    var isLogin: Boolean = session.getBoolean(Constant.IS_USER_LOGIN)
    var databaseHelper: DatabaseHelper
    var isLoading = false
    var isFavorite = false
    fun add(position: Int, item: Product?) {
        mDataset.add(position, item)
        notifyItemInserted(position)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity).inflate(resource, parent, false)
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

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(
        holderParent: RecyclerView.ViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        if (holderParent is HolderItems) {
            holderParent.setIsRecyclable(false)
            val product = mDataset[position]
            val variants = product!!.variants

            if (variants.size == 1) {
                holderParent.lytSpinner.visibility = View.GONE
                holderParent.tvMeasurement.visibility = View.VISIBLE
            }else{
                holderParent.lytSpinner.visibility = View.VISIBLE
                holderParent.tvMeasurement.visibility = View.GONE
            }



            if (product.indicator != "0") {
                holderParent.imgIndicator.visibility = View.VISIBLE
                if (product.indicator == "1") holderParent.imgIndicator.setImageResource(R.drawable.ic_veg_icon) else if (product.indicator == "2") holderParent.imgIndicator.setImageResource(
                    R.drawable.ic_non_veg_icon
                )
            }
            holderParent.productName.text = Html.fromHtml(product.name, 0)
            Glide.with(activity).load(product.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holderParent.imgThumb)


            val variantsName = arrayOfNulls<String>(variants.size)
            val variantsStockStatus = arrayOfNulls<String>(variants.size)

            for ((i, name) in variants.withIndex()) {
                variantsName[i] = name.measurement + " " + name.measurement_unit_name.uppercase()
                variantsStockStatus[i] = name.serve_for
            }

            holderParent.spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        setSelectedData(holderParent, variants[position], product)
                    }
                }

            val customAdapter = CustomAdapter(activity, variantsName, variantsStockStatus)
            holderParent.spinner.adapter = customAdapter


            holderParent.lytMain.setOnClickListener { v: View? ->
                if (Constant.CartValues.size > 0) {
                    ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                }
                val activity1 = activity as AppCompatActivity
                val fragment: Fragment = ProductDetailFragment()
                val bundle = Bundle()
                bundle.putInt(
                    Constant.VARIANT_POSITION,
                    if (variants.size == 1) 0 else holderParent.spinner.selectedItemPosition
                )
                bundle.putString(Constant.ID, variants[0].product_id)
                bundle.putString(Constant.FROM, from)
                bundle.putInt(Constant.LIST_POSITION, position)
                fragment.arguments = bundle
                activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            setSelectedData(holderParent, variants[0], product)
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mDataset[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        val product = mDataset[position]
        return product?.variants?.get(0)?.product_id?.toInt()?.toLong() ?: position.toLong()
    }

    fun setLoaded() {
        isLoading = false
    }

    @SuppressLint("SetTextI18n")
    fun setSelectedData(holder: HolderItems, extra: PriceVariation, product: Product) {

//        GST_Amount (Original Cost x GST %)/100
//        Net_Price Original Cost + GST Amount
        holder.tvMeasurement.text = extra.measurement +" "+ extra.measurement_unit_name.uppercase()
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            if (Constant.CartValues.containsKey(extra.id)) {
                holder.tvQuantity.text = "" + Constant.CartValues[extra.id]
            }
        } else {
            if (session.getData(extra.id) != null) {
                holder.tvQuantity.text = session.getData(extra.id)
            } else {
                holder.tvQuantity.text = extra.cart_count
            }
        }


        if (product.is_favorite) {
            holder.imgFav.setImageResource(R.drawable.ic_is_favorite)
        } else {
            holder.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
        }

        if (isLogin) {
            holder.tvQuantity.text = extra.cart_count
            val session = Session(activity)
            holder.imgFav.setOnClickListener { v: View? ->
                try {
                    isFavorite = product.is_favorite
                    if (from == "favorite") {
                        isFavorite = false
                        mDataset.remove(product)
                        notifyDataSetChanged()
                        if (mDataset.size == 0) {
                            FavoriteFragment.tvAlert.visibility = View.VISIBLE
                        } else {
                            FavoriteFragment.tvAlert.visibility = View.GONE
                        }
                    } else {
                        if (isFavorite) {
                            isFavorite = false
                            holder.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                            holder.lottieAnimationView.visibility = View.GONE
                        } else {
                            isFavorite = true
                            holder.lottieAnimationView.visibility = View.VISIBLE
                            holder.lottieAnimationView.playAnimation()
                        }
                        product.is_favorite = isFavorite
                    }
                    ApiConfig.addOrRemoveFavorite(activity, session, extra.product_id, isFavorite)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            holder.tvQuantity.text = databaseHelper.CheckCartItemExist(
                product!!.variants[0].id,
                product.variants[0].product_id
            )
            if (databaseHelper.getFavoriteById(product.variants[0].product_id)) {
                holder.imgFav.setImageResource(R.drawable.ic_is_favorite)
            } else {
                holder.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
            }
            holder.imgFav.setOnClickListener {
                isFavorite = databaseHelper.getFavoriteById(product.variants[0].product_id)
                if (from == "favorite") {
                    isFavorite = false
                    mDataset.remove(product)
                    notifyDataSetChanged()
                    if (mDataset.size == 0) {
                        FavoriteFragment.tvAlert.visibility = View.VISIBLE
                    } else {
                        FavoriteFragment.tvAlert.visibility = View.GONE
                    }
                } else {
                    if (isFavorite) {
                        isFavorite = false
                        holder.imgFav.setImageResource(R.drawable.ic_is_not_favorite)
                        holder.lottieAnimationView.visibility = View.GONE
                    } else {
                        isFavorite = true
                        holder.lottieAnimationView.visibility = View.VISIBLE
                        holder.lottieAnimationView.playAnimation()
                    }
                }
                databaseHelper.addOrRemoveFavorite(extra.product_id, isFavorite)
            }
        }
        val discountedPrice: Double
        val OriginalPrice: Double
        var taxPercentage = "0"
        try {
            taxPercentage =
                if (product.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (extra.discounted_price == "0" || extra.discounted_price == "") {
            holder.showDiscount.visibility = View.GONE
            discountedPrice =
                (extra.price.toFloat() + extra.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
        } else {
            discountedPrice =
                (extra.discounted_price.toFloat() + extra.discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            OriginalPrice =
                (extra.price.toFloat() + extra.price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
            holder.originalPrice.paintFlags =
                holder.originalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.originalPrice.text =
                session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + OriginalPrice)
            holder.showDiscount.visibility = View.VISIBLE
            holder.showDiscount.text = "-" + ApiConfig.getDiscount(OriginalPrice, discountedPrice)
        }
        holder.productPrice.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + discountedPrice)
        if (extra.serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
            holder.tvStatus.visibility = View.VISIBLE
            holder.qtyLyt.visibility = View.GONE
        } else {
            holder.tvStatus.visibility = View.GONE
            holder.qtyLyt.visibility = View.VISIBLE
        }
        if (isLogin) {
            if (Constant.CartValues.containsKey(extra.id)) {
                holder.tvQuantity.text = "" + Constant.CartValues[extra.id]
            } else {
                holder.tvQuantity.text = extra.cart_count
            }
            if (extra.cart_count == "0") {
                holder.btnAddToCart.visibility = View.VISIBLE
            } else {
                holder.btnAddToCart.visibility = View.GONE
            }
        } else {
            if (databaseHelper.CheckCartItemExist(extra.id, extra.product_id) == "0") {
                holder.btnAddToCart.visibility = View.VISIBLE
            } else {
                holder.btnAddToCart.visibility = View.GONE
            }
            holder.tvQuantity.text = databaseHelper.CheckCartItemExist(extra.id, extra.product_id)
        }
        val maxCartCont: String? =
            if (product.total_allowed_quantity == null || product.total_allowed_quantity == "" || product.total_allowed_quantity == "0") {
                session.getData(Constant.max_cart_items_count)
            } else {
                product.total_allowed_quantity
            }
        holder.imgAdd.setOnClickListener {
            addQuantity(
                extra,
                holder,
                true,
                maxCartCont
            )
        }
        holder.imgMinus.setOnClickListener { v: View? ->
            addQuantity(
                extra,
                holder,
                false,
                maxCartCont
            )
        }
        holder.btnAddToCart.setOnClickListener { v: View? ->
            addQuantity(
                extra,
                holder,
                true,
                maxCartCont
            )
        }
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
                            extra.cart_count = "" + count
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
                    extra.cart_count = "" + count
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

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        var progressBar: ProgressBar = view.findViewById(R.id.itemProgressbar)

    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imgAdd: ImageButton
        var imgMinus: ImageButton
        var productName: TextView
        var productPrice: TextView
        var tvQuantity: TextView
        var tvMeasurement: TextView
        var showDiscount: TextView
        var originalPrice: TextView
        var tvStatus: TextView
        var imgThumb: ImageView
        var imgFav: ImageView
        var imgIndicator: ImageView
        var lytSpinner: RelativeLayout
        var lytMain: CardView
        var spinner: AppCompatSpinner
        var qtyLyt: RelativeLayout
        var lottieAnimationView: LottieAnimationView
        var btnAddToCart: Button

        init {
            productName = itemView.findViewById(R.id.productName)
            productPrice = itemView.findViewById(R.id.tvPrice)
            showDiscount = itemView.findViewById(R.id.showDiscount)
            originalPrice = itemView.findViewById(R.id.tvOriginalPrice)
            tvMeasurement = itemView.findViewById(R.id.tvMeasurement)
            tvStatus = itemView.findViewById(R.id.tvStatus)
            imgThumb = itemView.findViewById(R.id.imgThumb)
            imgIndicator = itemView.findViewById(R.id.imgIndicator)
            imgAdd = itemView.findViewById(R.id.btnAddQuantity)
            imgMinus = itemView.findViewById(R.id.btnMinusQuantity)
            tvQuantity = itemView.findViewById(R.id.tvQuantity)
            qtyLyt = itemView.findViewById(R.id.qtyLyt)
            imgFav = itemView.findViewById(R.id.imgFav)
            lytMain = itemView.findViewById(R.id.lytMain)
            spinner = itemView.findViewById(R.id.spinner)
            lytSpinner = itemView.findViewById(R.id.lytSpinner)
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart)
            lottieAnimationView = itemView.findViewById(R.id.lottieAnimationView)
        }
    }

    init {
        Constant.CartValues = HashMap()
        databaseHelper = DatabaseHelper(activity)
    }
}