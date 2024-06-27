package wrteam.multivendor.customer.adapter

import android.app.Activity
import java.util.ArrayList
import wrteam.multivendor.customer.model.TrackTimeLine
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.annotation.SuppressLint
import android.view.LayoutInflater
import wrteam.multivendor.customer.R
import android.text.Html
import android.view.View
import android.widget.TextView
import android.widget.RelativeLayout
import wrteam.multivendor.customer.helper.Session

class OrderTimeLineAdapter(val activity: Activity, val trackTimeLines: ArrayList<TrackTimeLine>) :
    RecyclerView.Adapter<OrderTimeLineAdapter.CartItemHolder>() {
    val session: Session
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemHolder {
        @SuppressLint("InflateParams") val v =
            LayoutInflater.from(parent.context).inflate(R.layout.lyt_order_time_line, null)
        return CartItemHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CartItemHolder, position: Int) {
        val trackTimeLine = trackTimeLines[position]
        var from = ""
        if (trackTimeLine.location != "" && trackTimeLine.location != null) {
            from = activity.getString(R.string.from) + " <b> " + trackTimeLine.location + " </b>"
        }
        holder.tvTrackerDetail.text = Html.fromHtml(
            activity.getString(R.string.order) + " <b> " + trackTimeLine.activity + " </b> " + activity.getString(
                R.string.on
            ) + " <b> " + trackTimeLine.date + "</br>" + from, 0
        )
        if (position + 1 != itemCount) {
            holder.lytTimeLine.visibility = View.VISIBLE
        } else {
            holder.lytTimeLine.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return trackTimeLines.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return trackTimeLines[position].date.toLong()
    }

    class CartItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTrackerDetail: TextView
        val viewTimeLine: View
        val lytTimeLine: RelativeLayout

        init {
            tvTrackerDetail = itemView.findViewById(R.id.tvTrackerDetail)
            viewTimeLine = itemView.findViewById(R.id.viewTimeLine)
            lytTimeLine = itemView.findViewById(R.id.lytTimeLine)
        }
    }

    init {
        session = Session(activity)
    }
}