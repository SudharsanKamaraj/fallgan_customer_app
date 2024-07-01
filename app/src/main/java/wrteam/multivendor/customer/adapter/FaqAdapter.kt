package com.gpn.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.gpn.customerapp.R
import com.gpn.customerapp.model.Faq

class FaqAdapter(activity: Activity, faqs: ArrayList<Faq?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val faqs: ArrayList<Faq?>
    var visible: Boolean
    fun setLoaded() {}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity).inflate(R.layout.lyt_faq_list, parent, false)
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
            val faq = faqs[position]
            if (!faq!!.question.trim().isEmpty() && !faq.answer.trim()
                    .isEmpty()) {
                holderParent.tvQue.text = faq.question
                holderParent.tvAns.text = faq.answer
                holderParent.tvAns.visibility = View.GONE
            } else {
                holderParent.mainLyt.visibility = View.GONE
            }
            holderParent.mainLyt.setOnClickListener { v: View? ->
                if (visible) {
                    visible = false
                    holderParent.tvAns.visibility = View.GONE
                } else {
                    visible = true
                    holderParent.tvAns.visibility = View.VISIBLE
                }
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return faqs.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (faqs[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    internal class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQue: TextView
        val tvAns: TextView
        val mainLyt: RelativeLayout

        init {
            tvQue = itemView.findViewById(R.id.tvQue)
            tvAns = itemView.findViewById(R.id.tvAns)
            mainLyt = itemView.findViewById(R.id.mainLyt)
        }
    }

    init {
        this.activity = activity
        this.faqs = faqs
        visible = false
    }
}