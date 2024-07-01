package com.gpn.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.gpn.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gpn.customerapp.R
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.model.Transaction

class TransactionAdapter(activity: Activity, transactions: ArrayList<Transaction?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val transactions: ArrayList<Transaction?>
    var isLoading = false
    var id = "0"
    fun add(position: Int, item: Transaction?) {
        transactions.add(position, item)
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
                        .inflate(R.layout.lyt_transection_list, parent, false)
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
            val transaction = transactions[position]
            id = transaction!!.id
            holderParent.tvTxDateAndTime.text = transaction.date_created
            holderParent.tvTxMessage.text =
                activity.getString(R.string.hash) + transaction.order_id + " " + transaction.message
            holderParent.tvTxAmount.text =
                activity.getString(R.string.amount_) + Session(activity)
                    .getData(Constant.CURRENCY) + transaction.amount.toFloat()
            holderParent.tvTxNo.text = activity.getString(R.string.hash) + transaction.txn_id
            holderParent.tvPaymentMethod.text = activity.getString(R.string.via) + transaction.type
            holderParent.tvTxStatus.setText(ApiConfig.toTitleCase(transaction.status))
            if (transaction.status.equals(
                    Constant.CREDIT,
                    ignoreCase = true
                ) || transaction.status.equals(
                    Constant.SUCCESS, ignoreCase = true
                ) || transaction.status.equals(
                    "capture",
                    ignoreCase = true
                ) || transaction.status.equals(
                    "challenge",
                    ignoreCase = true
                ) || transaction.status.equals("pending", ignoreCase = true)
            ) {
                holderParent.cardViewTxStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                        activity,
                        R.color.tx_success_bg
                    )
                )
            } else {
                holderParent.cardViewTxStatus.setCardBackgroundColor(
                    ContextCompat.getColor(
                        activity,
                        R.color.tx_fail_bg
                    )
                )
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (transactions[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        val transaction = transactions[position]
        return transaction?.id?.toInt()?.toLong() ?: position.toLong()
    }

    internal class ViewHolderLoading(view: View) : RecyclerView.ViewHolder(view) {
        val progressBar: ProgressBar

        init {
            progressBar = view.findViewById(R.id.itemProgressbar)
        }
    }

    class HolderItems(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTxNo: TextView
        val tvTxDateAndTime: TextView
        val tvTxMessage: TextView
        val tvTxAmount: TextView
        val tvTxStatus: TextView
        val tvPaymentMethod: TextView
        val cardViewTxStatus: CardView

        init {
            tvTxNo = itemView.findViewById(R.id.tvTxNo)
            tvTxDateAndTime = itemView.findViewById(R.id.tvTxDateAndTime)
            tvTxMessage = itemView.findViewById(R.id.tvTxMessage)
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount)
            tvTxStatus = itemView.findViewById(R.id.tvTxStatus)
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod)
            cardViewTxStatus = itemView.findViewById(R.id.cardViewTxStatus)
        }
    }

    init {
        this.activity = activity
        this.transactions = transactions
    }
}