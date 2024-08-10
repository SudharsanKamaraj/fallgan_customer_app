package com.fallgan.customerapp.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import java.util.*

class SplashActivity : Activity() {
    lateinit var session: Session
    lateinit var activity: Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this@SplashActivity

        FirebaseApp.initializeApp(activity)

        val resources = resources
        val dm = resources.displayMetrics
        val configuration = resources.configuration
        configuration.setLocale(Locale(Constant.LANGUAGE_CODE.lowercase(Locale.getDefault())))
        resources.updateConfiguration(configuration, dm)

        session = Session(activity)

        session.setBoolean("update_skip", false)

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor =
            ContextCompat.getColor(activity, R.color.colorPrimary)
        setContentView(R.layout.activity_splash)
        val data = this.intent.data

        getShippingType(activity, session, data)

    }

    private fun getShippingType(activity: Activity, session: Session, data: Uri?) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_SHIPPING_TYPE] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {

                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            session.setData(
                                Constant.maintenance,
                                jsonObject.getString(Constant.maintenance)
                            )
                            session.setData(
                                Constant.SHIPPING_TYPE,
                                jsonObject.getString(Constant.SHIPPING_TYPE)
                            )
                            Log.i("SPLASH", jsonObject.getString(Constant.maintenance))
                            if (jsonObject.getString(Constant.maintenance) != "0") {
                                ApiConfig.openUnderMaintenanceDialog(activity)
                            } else {
                                if (data == null) {
                                    if (!session.getBoolean("is_first_time")) {
                                        startActivity(
                                            Intent(
                                                this@SplashActivity,
                                                WelcomeActivity::class.java
//                                                WelcomeActivity::class.java
                                            ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    } else {
                                        startActivity(
                                            Intent(
                                                this@SplashActivity,
                                                MainActivity::class.java
                                            ).putExtra(
                                                Constant.FROM, ""
                                            ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                } else if (data.isHierarchical) {
                                    when (data.path!!
                                        .split("/").toTypedArray()[data.path!!.split("/")
                                        .toTypedArray().size - 2]) {
                                        "seller", "product" -> {
                                            val intent =
                                                Intent(
                                                    this@SplashActivity,
                                                    MainActivity::class.java
                                                )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            intent.putExtra(
                                                Constant.ID, data.path!!
                                                    .split("/").toTypedArray()[2]
                                            )
                                            intent.putExtra(
                                                Constant.FROM, data.path!!
                                                    .split("/")
                                                    .toTypedArray()[data.path!!.split("/")
                                                    .toTypedArray().size - 2]
                                            )
                                            intent.putExtra(Constant.VARIANT_POSITION, 0)
                                            startActivity(intent)
                                            finish()
                                        }
                                        "refer" -> if (!session.getBoolean(Constant.IS_USER_LOGIN)) {
                                            ApiConfig.copyToClipboard(
                                                activity,
                                                activity.getString(R.string.your_friends_code),
                                                data.path!!
                                                    .split("/").toTypedArray()[2]
                                            )
                                            val referIntent =
                                                Intent(activity, LoginActivity::class.java)
                                            referIntent.putExtra(Constant.FROM, "refer")
                                            startActivity(referIntent)
                                            finish()
                                        } else {
                                            startActivity(
                                                Intent(
                                                    this@SplashActivity,
                                                    MainActivity::class.java
                                                ).putExtra(
                                                    Constant.FROM, ""
                                                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                            Toast.makeText(
                                                activity,
                                                activity.getString(R.string.msg_refer),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        else -> startActivity(
                                            Intent(
                                                this@SplashActivity,
                                                MainActivity::class.java
                                            ).putExtra(
                                                Constant.FROM, ""
                                            ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            }
                        } else {
                            ApiConfig.openUnderMaintenanceDialog(activity)
                        }
                    } catch (e: JSONException) {
                        ApiConfig.openUnderMaintenanceDialog(activity)
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, false)
    }
}