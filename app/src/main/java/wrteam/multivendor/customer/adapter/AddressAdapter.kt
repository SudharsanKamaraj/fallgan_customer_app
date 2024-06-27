package wrteam.multivendor.customer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.activity.MainActivity
import wrteam.multivendor.customer.fragment.AddressAddUpdateFragment
import wrteam.multivendor.customer.fragment.AddressListFragment
import wrteam.multivendor.customer.helper.ApiConfig
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.helper.Session
import wrteam.multivendor.customer.model.Address

class AddressAdapter(
    val activity: Activity,
    private val addresses: ArrayList<Address>,
    var layout: Int
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var id = "0"
    var session: Session = Session(activity)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(activity).inflate(layout, parent, false)
        return AddressItemHolder(view)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holderParent: RecyclerView.ViewHolder, position: Int) {
        try {
            val holder = holderParent as AddressItemHolder
            val address = addresses[position]
            id = address.id
            holder.setIsRecyclable(false)
            if (Constant.selectedAddressId == id || layout == R.layout.lyt_address_checkout) {
                Constant.selectedAddressId = address.id
                holder.tvName.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary))
                holder.tvAddressType.background =
                    ResourcesCompat.getDrawable(activity.resources, R.drawable.right_btn_bg, null)
                holder.tvDefaultAddress.background =
                    ResourcesCompat.getDrawable(activity.resources, R.drawable.right_btn_bg, null)
                holder.imgSelect.setImageResource(R.drawable.ic_check_circle)
                holder.lytMain.setBackgroundResource(R.drawable.selected_shadow)
                Constant.DefaultPinCode = address.pincode
                Constant.DefaultCity = address.city
                setData(address)
            } else {
                holder.tvName.setTextColor(ContextCompat.getColor(activity, R.color.gray))
                holder.tvAddressType.background =
                    ResourcesCompat.getDrawable(activity.resources, R.drawable.left_btn_bg, null)
                holder.tvDefaultAddress.background =
                    ResourcesCompat.getDrawable(activity.resources, R.drawable.left_btn_bg, null)
                holder.imgSelect.setImageResource(R.drawable.ic_uncheck_circle)
                holder.lytMain.setBackgroundResource(R.drawable.address_card_shadow)
            }
            holder.tvAddress.text =
                address.address + ", " + address.landmark + ", " + address.city + ", " + address.area + ", " + address.state + ", " + address.country + ", " + activity.getString(
                    R.string.pincode_
                ) + address.pincode
            if (address.is_default == "1" && layout != R.layout.lyt_address_checkout) {
                holder.tvDefaultAddress.visibility = View.VISIBLE
            }
            holder.lytMain.setPadding(
                activity.resources.getDimension(R.dimen._15sdp).toInt(),
                activity.resources.getDimension(R.dimen._15sdp)
                    .toInt(),
                activity.resources.getDimension(R.dimen._15sdp).toInt(),
                activity.resources.getDimension(R.dimen._15sdp)
                    .toInt()
            )
            holder.tvName.text = address.name
            if (!address.type.equals("", ignoreCase = true)) {
                holder.tvAddressType.text = address.type
            }
            holder.tvMobile.text = address.mobile
            holder.imgDelete.setOnClickListener {
                val builder = AlertDialog.Builder(activity)
                builder.setTitle(activity.resources.getString(R.string.delete_address))
                builder.setIcon(R.drawable.ic_delete)
                builder.setMessage(activity.resources.getString(R.string.delete_address_msg))
                builder.setCancelable(false)
                builder.setPositiveButton(activity.resources.getString(R.string.remove)) { _: DialogInterface?, _: Int ->
                    if (ApiConfig.isConnected(activity)) {
                        removeAddress(address)
                        ApiConfig.removeAddress(activity, address.id)
                    }
                    if (addresses.size == 0) {
                        Constant.selectedAddress = ""
                    }
                }
                builder.setNegativeButton(activity.resources.getString(R.string.cancel)) { dialog: DialogInterface, _: Int -> dialog.cancel() }
                val alert = builder.create()
                alert.show()
            }
            holder.lytMain.setOnClickListener {
                if (session.getData(Constant.SHIPPING_TYPE) == "local" && (address.city_id == "0" || address.city_id == "") && (address.area_id == "0" || address.area_id == "") && (address.pincode_id == "0" || address.pincode_id == "") || session.getData(Constant.SHIPPING_TYPE) == "standard" && address.city_id != "0" && address.area_id != "0" && address.pincode_id != "0"
                ) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.address_is_not_selectable) + activity.getString(
                            R.string.because
                        ) + if (session.getData(
                                Constant.SHIPPING_TYPE
                            ) == "local"
                        ) activity.getString(R.string.standard) else activity.getString(R.string.local),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    setData(address)
                    notifyDataSetChanged()
                }
            }
            if (layout == R.layout.lyt_address_list) {
                if (session.getData(Constant.SHIPPING_TYPE) == "local" && address.city_id == "0" && address.area_id == "0" && address.pincode_id == "0" || session.getData(
                        Constant.SHIPPING_TYPE
                    ) == "standard" && address.city_id != "0" && address.area_id != "0" && address.pincode_id != "0"
                ) {
                    holder.tvAlert.visibility = View.VISIBLE
                    holder.imgEdit.visibility = View.GONE
                } else {
                    holder.tvAlert.visibility = View.GONE
                    holder.imgEdit.visibility = View.VISIBLE
                }
            }

            holder.imgEdit.setOnClickListener {
                if (ApiConfig.isConnected(activity)) {
                    val fragment: Fragment = AddressAddUpdateFragment()
                    val bundle = Bundle()
                    bundle.putSerializable("model", address)
                    bundle.putString("for", "update")
                    bundle.putInt("position", position)
                    fragment.arguments = bundle
                    MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                        .addToBackStack(null).commit()
                }
            }

            if(itemCount > 0){
                AddressListFragment.tvAlert.visibility = View.GONE
            }else{
                AddressListFragment.tvAlert.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setData(address: Address) {
        Constant.selectedAddress =
            address.address + ", " + address.landmark + ", " + address.city + ", " + address.area + ", " + address.state + ", " + address.country + ", " + activity.getString(
                R.string.pincode_
            ) + address.pincode
        Constant.selectedAddressId = address.id
        session.setData(Constant.LONGITUDE, address.longitude)
        session.setData(Constant.LATITUDE, address.latitude)
    }

    fun addAddress(address: Address){
        addresses.add(address)
        notifyDataSetChanged()
    }

    fun updateAddress(position:Int,address: Address){
        addresses[position] = address
        notifyDataSetChanged()
    }

    private fun removeAddress(address: Address){
        addresses.remove(address)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return addresses.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    internal class AddressItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        val tvAddressType: TextView = itemView.findViewById(R.id.tvAddressType)
        val tvMobile: TextView = itemView.findViewById(R.id.tvMobile)
        val tvDefaultAddress: TextView = itemView.findViewById(R.id.tvDefaultAddress)
        val tvAlert: TextView = itemView.findViewById(R.id.tvAlert)
        val imgEdit: ImageView = itemView.findViewById(R.id.imgEdit)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
        val imgSelect: ImageView = itemView.findViewById(R.id.imgSelect)
        val lytMain: LinearLayout = itemView.findViewById(R.id.lytMain)

    }

}