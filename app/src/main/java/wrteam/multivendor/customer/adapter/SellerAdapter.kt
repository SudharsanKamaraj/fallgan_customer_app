package wrteam.multivendor.customer.adapter

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.fragment.SellerProductsFragment
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.model.Seller

class SellerAdapter(
    val activity: Activity,
    private val SellerList: ArrayList<Seller>,
    val layout: Int,
    val from: String,
    private val visibleNumber: Int
) : RecyclerView.Adapter<SellerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = SellerList[position]
        holder.tvTitle.text = model.store_name
        Glide.with(activity).load(model.logo)
            .centerInside()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.placeholder)
            .into(holder.imgSeller)
        holder.lytMain.setOnClickListener {
            val fragment: Fragment = SellerProductsFragment()
            val bundle = Bundle()
            bundle.putString(Constant.ID, model.id)
            bundle.putString(Constant.TITLE, model.store_name)
            bundle.putString(Constant.FROM, "Seller")
            fragment.arguments = bundle
            (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment).addToBackStack(null).commit()
        }
    }

    override fun getItemCount(): Int {
        val categories: Int = if (SellerList.size > visibleNumber && from == "home") {
            visibleNumber
        } else {
            SellerList.size
        }
        return categories
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val imgSeller: ImageView = itemView.findViewById(R.id.imgSeller)
        val lytMain: LinearLayout = itemView.findViewById(R.id.lytMain)

    }

}