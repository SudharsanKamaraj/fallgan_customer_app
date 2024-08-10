package com.fallgan.customerapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.Attachment

@SuppressLint("NotifyDataSetChanged")
class ProductImagesAdapter(
    val activity: Activity,
    val images: ArrayList<Attachment>,
    var from: String,
    var orderId: String
) : RecyclerView.Adapter<ProductImagesAdapter.ImageHolder>() {
    val session: Session
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        return ImageHolder(
            LayoutInflater.from(
                activity
            ).inflate(R.layout.lyt_image_list, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ImageHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val image = images[position]
        if (from == "api") {
            Glide.with(activity).load(image.image)
                .centerInside()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(holder.imgProductImage)
        } else {
            holder.imgProductImage.setImageBitmap(BitmapFactory.decodeFile(image.image))
        }
        holder.imgProductImageDelete.setOnClickListener { v: View? ->
            if (orderId == "0") {
                images.remove(image)
                notifyDataSetChanged()
            } else {
                removeImage(activity, image.id, image)
            }
        }
    }

    fun removeImage(activity: Activity, id: String, image: Attachment) {
        val alertDialog = AlertDialog.Builder(activity)
        // Setting Dialog Message
        alertDialog.setTitle(R.string.remove_image)
        alertDialog.setMessage(R.string.remove_image_msg)
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()

        // Setting OK Button
        alertDialog.setPositiveButton(R.string.yes) { dialog: DialogInterface?, which: Int ->
            deleteImage(
                orderId,
                id,
                dialog!!,
                attachment = image
            )
        }
        alertDialog.setNegativeButton(R.string.no) { dialog: DialogInterface?, which: Int -> alertDialog1.dismiss() }
        // Showing Alert Message
        alertDialog.show()
    }

    private fun deleteImage(
        orderId: String,
        id: String,
        dialogInterface: DialogInterface,
        attachment: Attachment
    ) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.DELETE_BANK_TRANSFER_ATTACHMENT] = Constant.GetVal
        params[Constant.ORDER_ID] = orderId
        params[Constant.ID] = id


        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            images.remove(attachment)
                            notifyDataSetChanged()
                        } else {
                            dialogInterface.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, params, true)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProductImage: ImageView
        val imgProductImageDelete: ImageView

        init {
            imgProductImage = itemView.findViewById(R.id.imgProductImage)
            imgProductImageDelete = itemView.findViewById(R.id.imgProductImageDelete)
        }
    }

    init {
        session = Session(activity)
    }
}