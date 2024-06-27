package wrteam.multivendor.customer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.fragment.ProductDetailFragment
import wrteam.multivendor.customer.fragment.SubCategoryFragment
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.model.*

class NotificationAdapter(activity: Activity, Notifications: ArrayList<Notification?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val Notifications: ArrayList<Notification?>
    var isLoading = false
    var id = "0"
    fun add(position: Int, item: Notification?) {
        Notifications.add(position, item)
        notifyItemInserted(position)
    }

    fun setLoaded() {
        isLoading = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view =
                    LayoutInflater.from(activity)
                        .inflate(R.layout.lyt_notification_list, parent, false)
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
            val notification = Notifications[position]
            id = notification!!.id
            if (!notification.image.isEmpty()) {
                holderParent.image.visibility = View.VISIBLE
                Glide.with(activity).load(notification.image)
                    .centerInside()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holderParent.image)
            } else {
                holderParent.image.visibility = View.GONE
            }
            if (!notification.name.isEmpty()) {
                holderParent.tvTitle.visibility = View.VISIBLE
            } else {
                holderParent.tvTitle.visibility = View.GONE
            }
            if (!notification.subtitle.isEmpty()) {
                holderParent.tvMessage.visibility = View.VISIBLE
            } else {
                holderParent.tvMessage.visibility = View.GONE
            }
            holderParent.tvTitle.text = Html.fromHtml(notification.name, 0)
            holderParent.tvMessage.text = Html.fromHtml(notification.subtitle, 0)
            if (!notification.subtitle.isEmpty()) {
                holderParent.tvMessage.visibility = View.VISIBLE
            } else {
                holderParent.tvMessage.visibility = View.GONE
            }
            holderParent.tvTitle.text = Html.fromHtml(notification.name, 0)
            holderParent.tvMessage.text = Html.fromHtml(notification.subtitle, 0)
            val type = notification.type
            if (type.equals("category", ignoreCase = true)) {
                holderParent.tvRedirect.visibility = View.VISIBLE
                holderParent.tvRedirect.text = activity.getString(R.string.go_to_category)
                holderParent.lytMain.setOnClickListener { v: View? ->
                    val fragment: Fragment = SubCategoryFragment()
                    val bundle = Bundle()
                    bundle.putString(Constant.ID, notification.type_id)
                    bundle.putString(Constant.NAME, notification.name)
                    bundle.putString(Constant.FROM, "category")
                    fragment.arguments = bundle
                    (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        .add(R.id.container, fragment).addToBackStack(null).commit()
                }
            } else if (type.equals("product", ignoreCase = true)) {
                holderParent.tvRedirect.visibility = View.VISIBLE
                holderParent.tvRedirect.text = activity.getString(R.string.go_to_product)
                holderParent.lytMain.setOnClickListener { v: View? ->
                    val activity1 = activity as AppCompatActivity
                    val fragment: Fragment = ProductDetailFragment()
                    val bundle = Bundle()
                    bundle.putInt("variantsPosition", 0)
                    bundle.putString("id", notification.type_id)
                    bundle.putString(Constant.FROM, "notification")
                    bundle.putInt("position", 0)
                    fragment.arguments = bundle
                    activity1.supportFragmentManager.beginTransaction()
                        .add(R.id.container, fragment).addToBackStack(null).commit()
                }
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return Notifications.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (Notifications[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        val notification = Notifications[position]
        return notification?.id?.toInt()?.toLong() ?: position.toLong()
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    internal class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView
        val tvTitle: TextView
        val tvMessage: TextView
        val tvRedirect: TextView
        var lytMain: LinearLayout

        init {
            image = itemView.findViewById(R.id.image)
            tvTitle = itemView.findViewById(R.id.tvTitle)
            tvMessage = itemView.findViewById(R.id.tvMessage)
            tvRedirect = itemView.findViewById(R.id.tvRedirect)
            lytMain = itemView.findViewById(R.id.lytMain)
        }
    }

    init {
        this.activity = activity
        this.Notifications = Notifications
    }
}