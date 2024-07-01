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
import com.gpn.customerapp.model.WalletTransaction

class WalletTransactionAdapter(
    activity: Activity,
    walletTransactions: ArrayList<WalletTransaction?>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val walletTransactions: ArrayList<WalletTransaction?>
    var isLoading = false
    var id = "0"
    fun add(position: Int, item: WalletTransaction?) {
        walletTransactions.add(position, item)
        notifyItemInserted(position)
    }

    fun setLoaded() {
        isLoading = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        return when (viewType) {
            viewTypeItem -> {
                view = LayoutInflater.from(activity)
                    .inflate(R.layout.lyt_wallet_transection_list, parent, false)
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
            val walletTransaction = walletTransactions[position]
            id = walletTransaction!!.id
            holderParent.tvTxDateAndTime.text = walletTransaction.date_created
            holderParent.tvTxMessage.text =
                activity.getString(R.string.hash) + walletTransaction.order_id + " " + walletTransaction.message
            holderParent.tvTxAmount.text =
                activity.getString(R.string.amount_) + Session(activity)
                    .getData(Constant.CURRENCY) + " " + walletTransaction.amount.toFloat()
            holderParent.tvTxNo.text = activity.getString(R.string.hash) + walletTransaction.id
            holderParent.tvTxStatus.setText(ApiConfig.toTitleCase(walletTransaction.status))
            if (walletTransaction.status.equals(Constant.CREDIT, ignoreCase = true)) {
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
        return walletTransactions.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (walletTransactions[position] == null) viewTypeLoading else viewTypeItem
    }

    override fun getItemId(position: Int): Long {
        val transaction = walletTransactions[position]
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
        val cardViewTxStatus: CardView

        init {
            tvTxNo = itemView.findViewById(R.id.tvTxNo)
            tvTxDateAndTime = itemView.findViewById(R.id.tvTxDateAndTime)
            tvTxMessage = itemView.findViewById(R.id.tvTxMessage)
            tvTxAmount = itemView.findViewById(R.id.tvTxAmount)
            tvTxStatus = itemView.findViewById(R.id.tvTxStatus)
            cardViewTxStatus = itemView.findViewById(R.id.cardViewTxStatus)
        }
    }

    init {
        this.activity = activity
        this.walletTransactions = walletTransactions
    }
}