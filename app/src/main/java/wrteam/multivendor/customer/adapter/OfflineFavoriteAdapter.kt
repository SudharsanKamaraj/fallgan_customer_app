package com.gpn.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import com.gpn.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

class OfflineFavoriteAdapter(activity: Activity, myDataset: ArrayList<Product?>, resource: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val resource: Int
    val activity: Activity
    val session: Session
    val databaseHelper: DatabaseHelper

    // The minimum amount of items to have below your current scroll position
    // before loading more.
    var isLoading = false
    val mDataset: ArrayList<Product?>
    var taxPercentage: String
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
    override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
        if (holderParent is HolderItems) {
            val holder = holderParent
            holder.setIsRecyclable(false)
            val product = mDataset[position]
            try {
                taxPercentage =
                    if (product!!.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val variants = product!!.variants
            if (variants.size == 1) {
                holder.spinner.visibility = View.INVISIBLE
                holder.lytSpinner.visibility = View.INVISIBLE
            }
            if (product.indicator != "0") {
                holder.imgIndicator.visibility = View.VISIBLE
                if (product.indicator == "1") holder.imgIndicator.setImageResource(R.drawable.ic_veg_icon) else if (product.indicator == "2") holder.imgIndicator.setImageResource(
                    R.drawable.ic_non_veg_icon
                )
            }
            holder.productName.text = Html.fromHtml(product.name)
            Glide.with(activity).load(product.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.imgThumb)
            val customAdapter: CustomAdapter = CustomAdapter(activity, variants, holder, product)
            holder.spinner.adapter = customAdapter
            holder.lytMain.setOnClickListener { v: View? ->
                if (Constant.CartValues != null && Constant.CartValues.size > 0) {
                    ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
                }
                val activity1 = activity as AppCompatActivity
                val fragment: Fragment = ProductDetailFragment()
                val bundle = Bundle()
                bundle.putInt(
                    Constant.VARIANT_POSITION,
                    if (variants.size == 1) 0 else holder.spinner.selectedItemPosition
                )
                bundle.putString(Constant.ID, product.variants[0].product_id)
                bundle.putInt("position", position)
                bundle.putString(Constant.FROM, "fragment")
                fragment.arguments = bundle
                activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            holder.tvQuantity.text = databaseHelper.CheckCartItemExist(
                product.variants[0].id,
                product.variants[0].product_id
            )
            holder.imgFav.setImageResource(R.drawable.ic_is_favorite)
            holder.imgFav.setOnClickListener { v: View? ->
                databaseHelper.addOrRemoveFavorite(product.variants[0].product_id, false)
                mDataset.remove(product)
            }
            setSelectedData(holder, variants[0], product)
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
    fun setSelectedData(holder: HolderItems, extra: PriceVariation, product: Product?) {
        try {
            holder.Measurement.text = extra.measurement + extra.measurement_unit_name
            holder.productPrice.text = session.getData(Constant.CURRENCY) + extra.price
            if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                if (Constant.CartValues != null && Constant.CartValues.containsKey(extra.id)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        holder.tvQuantity.text = "" + Constant.CartValues[extra.id]
                    }
                }
            } else {
                if (session.getData(extra.id) != null) {
                    holder.tvQuantity.text = session.getData(extra.id)
                } else {
                    holder.tvQuantity.text = extra.cart_count
                }
            }
            val discountedPrice: Double
            val OriginalPrice: Double
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (product!!.tax_percentage.toDouble() > 0) product.tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (extra.discounted_price == "0" || extra.discounted_price == "") {
                holder.lytDiscount.visibility = View.INVISIBLE
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
                holder.lytDiscount.visibility = View.VISIBLE
                holder.showDiscount.text =
                    "-" + ApiConfig.getDiscount(OriginalPrice, discountedPrice)
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
            holder.tvQuantity.text = databaseHelper.CheckCartItemExist(extra.id, extra.product_id)
            holder.imgAdd.setOnClickListener { view: View? ->
                var count = holder.tvQuantity.text.toString().toInt()
                if (count < extra.stock.toFloat()) {
                    if (count < session.getData(Constant.max_cart_items_count)!!
                            .toInt()
                    ) {
                        count++
                        holder.tvQuantity.text = "" + count
                        databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                        databaseHelper.getTotalItemOfCart(activity)
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
            }
            holder.imgMinus.setOnClickListener { view: View? ->
                var count = holder.tvQuantity.text.toString().toInt()
                if (count > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        count--
                        holder.tvQuantity.text = "" + count
                        databaseHelper.AddToCart(extra.id, extra.product_id, "" + count)
                        databaseHelper.getTotalItemOfCart(activity)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAdd: ImageButton
        val imgMinus: ImageButton
        val productName: TextView
        val productPrice: TextView
        val tvQuantity: TextView
        val Measurement: TextView
        val showDiscount: TextView
        val originalPrice: TextView
        val tvStatus: TextView
        val imgThumb: ImageView
        val imgFav: ImageView
        val imgIndicator: ImageView
        val lytMain: CardView
        val lytDiscount: RelativeLayout
        val lytSpinner: RelativeLayout
        val spinner: AppCompatSpinner
        val qtyLyt: RelativeLayout

        init {
            productName = itemView.findViewById(R.id.productName)
            productPrice = itemView.findViewById(R.id.tvPrice)
            showDiscount = itemView.findViewById(R.id.showDiscount)
            originalPrice = itemView.findViewById(R.id.tvOriginalPrice)
            Measurement = itemView.findViewById(R.id.tvMeasurement)
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
            lytDiscount = itemView.findViewById(R.id.lytDiscount)
            lytSpinner = itemView.findViewById(R.id.lytSpinner)
        }
    }

    inner class CustomAdapter(
        activity: Activity,
        extraList: ArrayList<PriceVariation>,
        holder: HolderItems,
        product: Product
    ) : BaseAdapter() {
        val activity: Activity
        val extraList: ArrayList<PriceVariation>
        val inflter: LayoutInflater
        val holder: HolderItems
        val product: Product
        override fun getCount(): Int {
            return extraList.size
        }

        override fun getItem(i: Int): Any {
            return extraList[i]
        }

        override fun getItemId(i: Int): Long {
            return 0
        }

        @SuppressLint("SetTextI18n", "ViewHolder", "InflateParams")
        override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
            var view = view
            view = inflter.inflate(R.layout.lyt_spinner_item, null)
            val measurement = view.findViewById<TextView>(R.id.tvMeasurement)
            //            TextView discountedPrice = view.findViewById(R.id.tvPrice);
            val extra = extraList[i]
            measurement.text = extra.measurement + " " + extra.measurement_unit_name
            if (extra.serve_for.equals(Constant.SOLD_OUT_TEXT, ignoreCase = true)) {
                measurement.setTextColor(ContextCompat.getColor(activity, R.color.red))
            } else {
                measurement.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }
            holder.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View,
                    i: Int,
                    l: Long
                ) {
                    val priceVariation = extraList[i]
                    setSelectedData(holder, priceVariation, product)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
            return view
        }

        init {
            this.activity = activity
            this.extraList = extraList
            this.holder = holder
            this.product = product
            inflter = LayoutInflater.from(activity)
        }
    }

    init {
        this.activity = activity
        mDataset = myDataset
        this.resource = resource
        session = Session(activity)
        databaseHelper = DatabaseHelper(activity)
        taxPercentage = "0"
    }
}