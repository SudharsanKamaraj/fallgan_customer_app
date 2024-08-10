package com.fallgan.customerapp.helper

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.helper.Session.Companion.setCount

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            try {
                val json = JSONObject(remoteMessage.data.toString())
                sendPushNotification(json)
            } catch (e: Exception) {
                Log.e("MyFirebaseMessagingService", "Exception: " + e.message)
            }
        }
    }

    private fun sendPushNotification(json: JSONObject) {
        try {
            val data = json.getJSONObject(Constant.DATA)
            val type = data.getString("type")
            val title = data.getString("title")
            val message = data.getString("message").replace("\\n", "\n").replace("\\r", "\r")

            val imageUrl = data.getString("image")
            val id = data.getString(Constant.ID)
            val intent = Intent(applicationContext, MainActivity::class.java)
            when (type) {
                "category" -> {
                    intent.putExtra(Constant.ID, id)
                    intent.putExtra("name", title)
                    intent.putExtra(Constant.FROM, type)
                }
                "product" -> {
                    intent.putExtra(Constant.ID, id)
                    intent.putExtra(Constant.VARIANT_POSITION, 0)
                    intent.putExtra(Constant.FROM, type)
                }
                "order" -> {
                    intent.putExtra(Constant.FROM, type)
                    intent.putExtra("model", "")
                    intent.putExtra(Constant.ID, id)
                }
                else -> intent.putExtra(Constant.FROM, "")
            }
            when (type) {
                "payment_transaction" -> setCount(
                    Constant.UNREAD_TRANSACTION_COUNT,
                    Session(
                        applicationContext
                    ).getCount(Constant.UNREAD_TRANSACTION_COUNT, applicationContext) + 1,
                    applicationContext
                )
                "wallet_transaction" -> setCount(
                    Constant.UNREAD_WALLET_COUNT,
                    Session(
                        applicationContext
                    ).getCount(Constant.UNREAD_WALLET_COUNT, applicationContext) + 1,
                    applicationContext
                )
                "default", "category", "product" -> setCount(
                    Constant.UNREAD_NOTIFICATION_COUNT,
                    Session(
                        applicationContext
                    ).getCount(Constant.UNREAD_NOTIFICATION_COUNT, applicationContext) + 1,
                    applicationContext
                )
            }
            val mNotificationManager = MyNotificationManager(applicationContext)
            if (imageUrl == "null" || imageUrl == "") {
                mNotificationManager.showSmallNotification(title, message, intent)
            } else {
                mNotificationManager.showBigNotification(title, message, imageUrl, intent)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("MyFirebaseMsgService", "Json Exception: " + e.message)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MyFirebaseMsgService", "Exception: " + e.localizedMessage)
        }
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Session(applicationContext).setData(Constant.FCM_ID,p0)
    }
}