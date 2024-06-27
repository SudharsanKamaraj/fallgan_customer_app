package wrteam.multivendor.customer.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.activity.MainActivity
import wrteam.multivendor.customer.fragment.ProductDetailFragment
import wrteam.multivendor.customer.fragment.SubCategoryFragment
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.model.HomeOffer
import java.util.*

class OfferAdapter(var activity: Activity, private val offerList: ArrayList<HomeOffer>) :
    RecyclerView.Adapter<OfferAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.offer_lyt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try{
            viewHolder = holder
            when (offerList[position].offer_type) {
                "image" -> {
                    Glide.with(activity).load(offerList[position].image).centerInside()
                        .placeholder(R.drawable.offer_placeholder).error(R.drawable.offer_placeholder)
                        .into(holder.offerImage)

                    holder.offerImage.visibility = View.VISIBLE

                    holder.offerImage.setOnClickListener {

                        when (offerList[position].type) {
                            "category" -> {
                                val fragment: Fragment = SubCategoryFragment()
                                val bundle = Bundle()
                                bundle.putString(Constant.ID, offerList[position].type_id)
                                bundle.putString(Constant.NAME, offerList[position].name)
                                bundle.putString(Constant.FROM, "category")
                                fragment.arguments = bundle
                                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                                    .addToBackStack(null).commit()
                            }
                            "products" -> {
                                val fragment: Fragment = ProductDetailFragment()
                                val bundle = Bundle()
                                bundle.putString(Constant.ID, offerList[position].type_id)
                                bundle.putString(Constant.FROM, "slider")
                                bundle.putInt("variantsPosition", 0)
                                fragment.arguments = bundle
                                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                                    .addToBackStack(null).commit()
                            }
                            "offer_image_url" -> {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(offerList[position].offer_image_url)
                                )
                                activity.startActivity(browserIntent)
                            }
                        }

                    }
                    holder.lytOfferImage.visibility = View.VISIBLE
                }
                "video" -> {

                    holder.videoView.setVideoURI(Uri.parse(offerList[position].video))

                    holder.imgPlay.setOnClickListener {
                        if (holder.imgPlay.tag == "play") {
                            holder.imgPlay.setImageDrawable(
                                ContextCompat.getDrawable(
                                    activity, R.drawable.ic_pause
                                )
                            )
                            holder.imgPlay.tag = "pause"
                            holder.lytPlay.visibility = View.GONE
                            Handler(Looper.getMainLooper()).postDelayed({
                                holder.lytPlay.visibility = View.GONE
                            }, 3000)
                            holder.videoView.start()
                        } else {
                            holder.imgPlay.setImageDrawable(
                                ContextCompat.getDrawable(
                                    activity, R.drawable.ic_play
                                )
                            )
                            holder.imgPlay.tag = "play"
                            holder.videoView.pause()
                        }
                    }

                    holder.videoView.setOnPreparedListener { mp: MediaPlayer ->
                        mediaPlayer = mp
                        holder.imgVolume.setOnClickListener {
                            if (holder.imgVolume.tag == "soundOn") {
                                mp.setVolume(0f, 0f)
                                holder.imgVolume.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        activity, R.drawable.ic_volume_off
                                    )
                                )
                                holder.imgVolume.tag = "soundOff"
                            } else {
                                mp.setVolume(1f, 1f)
                                holder.imgVolume.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        activity, R.drawable.ic_volume_on
                                    )
                                )
                                holder.videoView.isSoundEffectsEnabled = true
                                holder.imgVolume.tag = "soundOn"
                            }
                        }
                    }

                    holder.lytVideo.visibility = View.VISIBLE
                    holder.lytOfferImage.visibility = View.VISIBLE
                }
                else -> {
                    holder.lytOfferImage.visibility = View.GONE
                }
            }
        }catch (_: Exception){}
    }

    override fun getItemCount(): Int {
        return offerList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val offerImage: ImageView = itemView.findViewById(R.id.offerImage)
        val imgPlay: ImageView = itemView.findViewById(R.id.imgPlay)
        val imgVolume: ImageView = itemView.findViewById(R.id.imgVolume)
        val videoView: VideoView = itemView.findViewById(R.id.videoView)
        val lytPlay: RelativeLayout = itemView.findViewById(R.id.lytPlay)
        val lytVideo: RelativeLayout = itemView.findViewById(R.id.lytVideo)
        val lytOfferImage: CardView = itemView.findViewById(R.id.lytOfferImage)
    }

    companion object {
        lateinit var mediaPlayer: MediaPlayer

        @SuppressLint("StaticFieldLeak")
        lateinit var viewHolder: ViewHolder
    }
}