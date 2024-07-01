package com.gpn.customerapp.helper

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.gpn.customerapp.R
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MyNotificationManager(private val mCtx: Context) {
    //the method will show a big notification with an image
    //parameters are title for message title, message for message text, url of the big image and an intent that will open
    //when you will tap on the notification
    fun showBigNotification(title: String, message: String, url: String, intent: Intent) {
        @SuppressLint("UnspecifiedImmutableFlag") val resultPendingIntent =
            PendingIntent.getActivity(
                mCtx,
                ID_BIG_NOTIFICATION,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        val bigPictureStyle = NotificationCompat.BigPictureStyle()
        bigPictureStyle.setBigContentTitle(title)
        bigPictureStyle.setSummaryText(message)
        bigPictureStyle.bigPicture(getBitmapFromURL(url))
        val mBuilder = NotificationCompat.Builder(
            mCtx, "notification"
        )
        val notification: Notification =
            mBuilder.setSmallIcon(R.drawable.ic_logo).setTicker(title)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setStyle(bigPictureStyle)
                .setSmallIcon(R.drawable.ic_logo)
                .setLargeIcon(BitmapFactory.decodeResource(mCtx.resources, R.drawable.ic_logo))
                .setColor(mCtx.getColor(R.color.colorPrimary))
                .setContentText(message)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        val notificationManager =
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ID_BIG_NOTIFICATION, notification)
        createChannel(notificationManager)
    }

    //the method will show a small notification
    //parameters are title for message title, message for message text and an intent that will open
    //when you will tap on the notification
    fun showSmallNotification(title: String, message: String, intent: Intent) {
        @SuppressLint("UnspecifiedImmutableFlag") val resultPendingIntent =
            PendingIntent.getActivity(
                mCtx,
                ID_SMALL_NOTIFICATION,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        val mBuilder = NotificationCompat.Builder(
            mCtx, "notification"
        )
        val notification: Notification =
            mBuilder.setSmallIcon(R.drawable.ic_logo).setTicker(title)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_logo)
                .setLargeIcon(BitmapFactory.decodeResource(mCtx.resources, R.drawable.ic_logo))
                .setColor(mCtx.getColor(R.color.colorPrimary))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentText(message)
                .build()
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        val notificationManager =
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ID_SMALL_NOTIFICATION, notification)
        createChannel(notificationManager)
    }

    private fun createChannel(notificationManager: NotificationManager) {
        val name = "notification"
        val description = "Notifications for download status"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("notification", name, importance)
        mChannel.description = description
        mChannel.enableLights(true)
        mChannel.lightColor = Color.BLUE
        notificationManager.createNotificationChannel(mChannel)
    }

    //The method will return Bitmap from an image URL
    private fun getBitmapFromURL(strURL: String): Bitmap {
        var input: InputStream? = null
        return try {
            val url = URL(strURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            BitmapFactory.decodeStream(input)
        }
    }

    companion object {
        const val ID_BIG_NOTIFICATION = 234
        const val ID_SMALL_NOTIFICATION = 235
    }
}