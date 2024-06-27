package wrteam.multivendor.customer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.helper.ApiConfig
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.helper.Session
import wrteam.multivendor.customer.model.Cart
import java.lang.Boolean
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.plus

/**
 * Created by shree1 on 3/16/2017.
 */
class CheckoutItemListAdapter(var activity: Activity, private var carts: ArrayList<Cart>?) :
    RecyclerView.Adapter<CheckoutItemListAdapter.ItemHolder>() {
    var session = Session(activity)

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        try {
            val cart: Cart = carts!![position]
            val discountedPrice: Float = if (cart.item[0].discounted_price == "0") {
                cart.item[0].price.toFloat()
            } else {
                cart.item[0].discounted_price.toFloat()
            }
            val taxPercentage: String = cart.item[0].tax_percentage
            holder.tvItemName.text = cart.item[0].name + " (" + cart.item[0].measurement + " " + ApiConfig.toTitleCase(
                cart.item[0].unit
            ) + ")"
            holder.tvQty.text = activity.getString(R.string.qty_1) + cart.qty
            holder.tvPrice.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + discountedPrice)
            if (cart.item[0].cod_allowed == "0") {
                Constant.isCODAllow = false
            }
            if (!cart.item[0].is_item_deliverable && session.getData(
                    Constant.SHIPPING_TYPE
                ) == "local"
            ) {
                Constant.orderPlaceable = false
                holder.tvDeliverable.visibility = View.VISIBLE
                holder.tvDeliverable.text = activity.getString(R.string.msg_item_not_deliverable)
            } else if (cart.item[0].serve_for.equals(
                    Constant.SOLD_OUT_TEXT,
                    ignoreCase = true
                )
            ) {
                Constant.orderPlaceable = false
                holder.tvDeliverable.visibility = View.VISIBLE
                holder.tvDeliverable.text = activity.getString(R.string.sold_out)
            } else {
                holder.tvDeliverable.visibility = View.GONE
            }
            if (cart.item[0].tax_title == "") {
                holder.lytTax.visibility = View.GONE
            } else {

                holder.lytTax.visibility = View.VISIBLE
                if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                    holder.tvTaxTitle.text = cart.item[0].tax_title
                    holder.tvTaxAmount.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + cart.qty.toInt() * (cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100)
                    )
                } else {
                    holder.tvTaxTitle.text = cart.item[0].tax_title
                    holder.tvTaxAmount.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + cart.qty.toInt() * (cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100)
                    )
                }
                if (cart.item[0].tax_percentage == "0") {
                    holder.tvTaxTitle.text = "TAX"
                }
                holder.tvTaxPercent.text = "(" + cart.item[0].tax_percentage + "%)"
            }

            if (cart.item[0].discounted_price == "0" || cart.item[0].discounted_price == "") {
                holder.tvSubTotal.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    "" + cart.qty.toInt() * (cart.item[0].price.toFloat() + cart.item[0].price.toFloat() * taxPercentage.toFloat() / 100)
                )
            } else {
                holder.tvSubTotal.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    "" + cart.qty.toInt() * (cart.item[0].discounted_price.toFloat() + cart.item[0].discounted_price.toFloat() * taxPercentage.toFloat() / 100)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.lyt_checkout_item_list, parent, false)
        return ItemHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvSubTotal: TextView = itemView.findViewById(R.id.tvSubTotal)
        val tvTaxPercent: TextView = itemView.findViewById(R.id.tvTaxPercent)
        val tvTaxTitle: TextView = itemView.findViewById(R.id.tvTaxTitle)
        val tvTaxAmount: TextView = itemView.findViewById(R.id.tvTaxAmount)
        val tvDeliverable: TextView = itemView.findViewById(R.id.tvDeliverable)
        val lytTax: LinearLayout = itemView.findViewById(R.id.lytTax)

    }

    override fun getItemCount(): Int {
        return carts!!.size
    }
}