package wrteam.multivendor.customer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import wrteam.multivendor.customer.helper.ApiConfig
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.fragment.CartFragment
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.helper.DatabaseHelper
import wrteam.multivendor.customer.helper.Session
import wrteam.multivendor.customer.model.OfflineCart

@SuppressLint("NotifyDataSetChanged")
class OfflineSaveForLaterAdapter(activity: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val session: Session
    fun add(position: Int, item: OfflineCart) {
        if (position > 0) {
            CartFragment.offlineSaveForLaterItems.add(position, item)
        } else {
            CartFragment.offlineSaveForLaterItems.add(item)
        }
        CartFragment.offlineSaveForLaterAdapter.notifyDataSetChanged()
        CartFragment.offlineCartAdapter.notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        val cart: OfflineCart = CartFragment.offlineSaveForLaterItems.get(position)
        databaseHelper.RemoveFromSaveForLater(cart.product_variant_id, cart.product_id)
        CartFragment.offlineSaveForLaterItems.remove(cart)
        CartFragment.offlineSaveForLaterAdapter.notifyDataSetChanged()
        CartFragment.offlineCartAdapter.notifyDataSetChanged()
        if (itemCount == 0) CartFragment.lytSaveForLater.setVisibility(View.GONE)
    }

    @SuppressLint("SetTextI18n")
    fun moveItem(position: Int) {
        try {
            val cart: OfflineCart = CartFragment.offlineSaveForLaterItems.get(position)
            CartFragment.isSoldOut = false
            CartFragment.isDeliverable = false
            var taxPercentage = "0"
            try {
                taxPercentage =
                    if (cart.item[0].tax_percentage.toDouble() > 0) cart.item[0].tax_percentage else "0"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val price: Double
            price =
                if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                    (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                } else {
                    (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100).toDouble()
                }
            Constant.FLOAT_TOTAL_AMOUNT += price * databaseHelper.CheckSaveForLaterItemExist(
                cart.product_variant_id,
                cart.product_id
            ).toInt()
            CartFragment.setData(activity)
            CartFragment.offlineSaveForLaterItems.remove(cart)
            CartFragment.offlineCartAdapter.add(0, cart)
            if (CartFragment.offlineSaveForLaterItems.size === 0) CartFragment.lytSaveForLater.setVisibility(
                View.GONE
            )
            CartFragment.tvSaveForLaterTitle.setText(activity.resources.getString(R.string.save_for_later) + " (" + CartFragment.offlineSaveForLaterItems.size + ")")
            CartFragment.saveForLaterValues.remove(cart.product_variant_id)
            Constant.TOTAL_CART_ITEM = CartFragment.offlineCarts.size
            databaseHelper.MoveToCartOrSaveForLater(
                cart.product_variant_id,
                cart.product_id,
                "save_for_later",
                activity
            )
            CartFragment.offlineCartAdapter.notifyDataSetChanged()
            CartFragment.offlineSaveForLaterAdapter.notifyDataSetChanged()
            CartFragment.setData(activity)
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
        if (holderParent is HolderItems) {
            val cart: OfflineCart = CartFragment.offlineSaveForLaterItems.get(position)
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
            if (!cart.item[0].serve_for.equals("available", ignoreCase = true)) {
                holderParent.tvStatus.visibility = View.VISIBLE
            }
            holderParent.tvPrice.text =
                Session(activity).getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + price)
            holderParent.tvDelete.setOnClickListener { v: View? -> removeItem(position) }
            holderParent.tvAction.setOnClickListener { v: View? -> moveItem(position) }
            holderParent.tvProductName.text = cart.item[0].name
            holderParent.tvMeasurement.text =
                cart.item[0].measurement + "\u0020" + cart.item[0].unit
            if (CartFragment.offlineSaveForLaterItems.size > 0) {
                CartFragment.lytSaveForLater.setVisibility(View.VISIBLE)
            } else {
                CartFragment.lytSaveForLater.setVisibility(View.GONE)
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return CartFragment.offlineSaveForLaterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (CartFragment.offlineSaveForLaterItems.get(position) == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        val cart: OfflineCart = CartFragment.offlineSaveForLaterItems.get(position)
        return cart.product_variant_id.toInt().toLong()
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

    companion object {
        lateinit var databaseHelper: DatabaseHelper
    }

    init {
        this.activity = activity
        databaseHelper = DatabaseHelper(activity)
        session = Session(activity)
    }
}