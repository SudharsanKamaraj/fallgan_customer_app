package com.gpn.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gpn.customerapp.R
import com.gpn.customerapp.activity.MainActivity
import com.gpn.customerapp.fragment.CartFragment
import com.gpn.customerapp.fragment.HomeFragment
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.model.PinCode

class PinCodeAdapter(activity: Activity, pinCodes: ArrayList<PinCode?>, from: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // for load more
    val viewTypeItem = 0
    val viewTypeLoading = 1
    val activity: Activity
    val pinCodes: ArrayList<PinCode?>
    var isLoading = false
    val session: Session
    val from: String
    fun add(position: Int, pinCode: PinCode?) {
        pinCodes.add(position, pinCode)
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
                    LayoutInflater.from(activity).inflate(R.layout.lyt_pin_code_list, parent, false)
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
        if (holderParent is HolderItems) {
            try {
                val pinCode = pinCodes[position]
                holderParent.tvPinCode.text = pinCode!!.pincode
                holderParent.tvPinCode.setOnClickListener {
                    session.setBoolean(Constant.GET_SELECTED_PINCODE, true)
                    session.setData(Constant.GET_SELECTED_PINCODE_ID, pinCode.id)
                    session.setData(Constant.GET_SELECTED_PINCODE_NAME, pinCode.pincode)

                    if (from == "home") {
                        HomeFragment.tvLocation.text = pinCode.pincode
                        HomeFragment.refreshListener.onRefresh()
                    } else {
                        CartFragment.tvLocation.text = pinCode.pincode
                        CartFragment.refreshListener.onRefresh()
                    }
                    MainActivity.pinCodeFragment.dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (holderParent is ViewHolderLoading) {
            holderParent.progressBar.isIndeterminate = true
        }
    }

    override fun getItemCount(): Int {
        return pinCodes.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (pinCodes[position] == null) viewTypeLoading else viewTypeItem
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
        val tvPinCode: TextView

        init {
            tvPinCode = itemView.findViewById(R.id.tvPinCode)
        }
    }

    init {
        this.activity = activity
        session = Session(activity)
        this.pinCodes = pinCodes
        this.from = from
    }
}