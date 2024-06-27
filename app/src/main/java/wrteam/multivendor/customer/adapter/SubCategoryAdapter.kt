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
import wrteam.multivendor.customer.fragment.ProductListFragment
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.model.Category

class SubCategoryAdapter(
    activity: Activity,
    val categoryList: ArrayList<Category>,
    val layout: Int,
    from: String
) : RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {
    val activity: Activity
    val from: String
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = categoryList[position]
        holder.tvTitle.text = model.name
        Glide.with(activity).load(model.image)
            .placeholder(R.drawable.placeholder)
            .centerInside()
            .into(holder.imgCategory)
        holder.lytMain.setOnClickListener { v: View? ->
            val activity1 = activity as AppCompatActivity
            val fragment: Fragment = ProductListFragment()
            val bundle = Bundle()
            bundle.putString(Constant.ID, model.id)
            bundle.putString(Constant.NAME, model.name)
            bundle.putString(Constant.FROM, from)
            fragment.arguments = bundle
            activity1.supportFragmentManager.beginTransaction().add(R.id.container, fragment)
                .addToBackStack(null).commit()
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView
        val imgCategory: ImageView
        val lytMain: LinearLayout

        init {
            lytMain = itemView.findViewById(R.id.lytMain)
            imgCategory = itemView.findViewById(R.id.imgCategory)
            tvTitle = itemView.findViewById(R.id.tvTitle)
        }
    }

    init {
        this.activity = activity
        this.from = from
    }
}