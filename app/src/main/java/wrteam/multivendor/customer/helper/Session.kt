package com.gpn.customerapp.helper

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import com.gpn.customerapp.R
import com.gpn.customerapp.activity.MainActivity

class Session(activity: Context) {
    private val privateMode = 0
    lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    fun getCount(id: String?, activity: Context?): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return sharedPreferences.getInt(id, 0)
    }

    fun getData(id: String?): String? {
        return pref.getString(id, "")
    }

    fun getCoordinates(id: String?): String? {
        return pref.getString(id, "0")
    }

    fun setData(id: String?, `val`: String?) {
        editor.putString(id, `val`)
        editor.commit()
    }

    fun setBoolean(id: String?, `val`: Boolean) {
        editor.putBoolean(id, `val`)
        editor.commit()
    }

    fun getBoolean(id: String?): Boolean {
        return pref.getBoolean(id, false)
    }

    fun createuserLoginSession(
        profile: String?,
        fcmId: String?,
        id: String?,
        name: String?,
        email: String?,
        mobile: String?,
        password: String?,
        referCode: String?
    ) {
        editor.putBoolean(Constant.IS_USER_LOGIN, true)
        editor.putString(Constant.FCM_ID, fcmId)
        editor.putString(Constant.ID, id)
        editor.putString(Constant.NAME, name)
        editor.putString(Constant.EMAIL, email)
        editor.putString(Constant.MOBILE, mobile)
        editor.putString(Constant.PASSWORD, password)
        editor.putString(Constant.REFERRAL_CODE, referCode)
        editor.putString(Constant.PROFILE, profile)
        editor.commit()
    }

    fun setUserData(
        user_id: String?,
        name: String?,
        email: String?,
        country_code: String?,
        profile: String?,
        mobile: String?,
        balance: String?,
        referral_code: String?,
        friends_code: String?,
        fcm_id: String?,
        status: String?
    ) {
        editor.putString(Constant.USER_ID, user_id)
        editor.putString(Constant.NAME, name)
        editor.putString(Constant.EMAIL, email)
        editor.putString(Constant.COUNTRY_CODE, country_code)
        editor.putString(Constant.PROFILE, profile)
        editor.putString(Constant.MOBILE, mobile)
        editor.putString(Constant.BALANCE, balance)
        editor.putString(Constant.REFERRAL_CODE, referral_code)
        editor.putString(Constant.FRIEND_CODE, friends_code)
        editor.putString(Constant.FCM_ID, fcm_id)
        editor.putString(Constant.STATUS, status)
        editor.commit()
    }

    fun logoutUser(activity: Activity) {
        editor.clear()
        editor.commit()
        Session(context).setBoolean("is_first_time", true)
        val i = Intent(activity, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.putExtra(Constant.FROM, "")
        activity.startActivity(i)
        activity.finish()
    }

    fun logoutUserConfirmation(activity: Activity) {
        val alertDialog = AlertDialog.Builder(
            context
        )
        alertDialog.setTitle(R.string.logout)
        alertDialog.setMessage(R.string.logout_msg)
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()

        // Setting OK Button
        alertDialog.setPositiveButton(R.string.yes) { dialog: DialogInterface?, which: Int ->
            editor.clear()
            editor.commit()
            Session(context).setBoolean("is_first_time", true)
            val i = Intent(activity, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra(Constant.FROM, "")
            activity.startActivity(i)
            activity.finish()
        }
        alertDialog.setNegativeButton(R.string.no) { dialog: DialogInterface?, which: Int -> alertDialog1.dismiss() }
        // Showing Alert Message
        alertDialog.show()
    }

    companion object {
        const val PREFER_NAME = "eKart"
        @JvmStatic
        fun setCount(id: String?, value: Int, activity: Context?) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
            val editor = sharedPreferences.edit()
            editor.putInt(id, value)
            editor.apply()
        }
    }

    init {
        try {
            context = activity
            pref = context.getSharedPreferences(PREFER_NAME, privateMode)
            editor = pref.edit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}