package com.gpn.customerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.databinding.ActivityWebViewBinding
import com.gpn.customerapp.helper.*
import java.text.SimpleDateFormat
import java.util.*

class PayPalWebActivity : AppCompatActivity() {

    lateinit var binding: ActivityWebViewBinding

    lateinit var toolbar: Toolbar
    lateinit var webView: WebView
    lateinit var cardViewHamburger: CardView
    lateinit var toolbarTitle: TextView
    lateinit var imageMenu: ImageView
    lateinit var imageHome: ImageView
    
    lateinit var url: String
    private lateinit var orderId: String
    lateinit var session: Session
    private lateinit var sendParams: MutableMap<String, String>
    lateinit var from: String
    lateinit var activity: Activity

    var isTxnInProcess = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = binding.toolbar
        webView = binding.webView
        cardViewHamburger = binding.cardViewHamburger
        toolbarTitle = binding.toolbarTitle
        imageMenu = binding.imageMenu
        imageHome = binding.imageHome

        sendParams = (intent.getSerializableExtra(Constant.PARAMS) as MutableMap<String, String>?)!!
        orderId = intent.getStringExtra(Constant.ORDER_ID).toString()
        from = intent.getStringExtra(Constant.FROM).toString()
        activity = this
        session = Session(activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbarTitle.text = getString(R.string.payment)
        imageHome.visibility = View.GONE
        imageMenu.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_arrow_back))
        imageMenu.visibility = View.VISIBLE
        cardViewHamburger.setOnClickListener { onBackPressed() }

        url = intent.getStringExtra("url").toString()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                isTxnInProcess = if (url.startsWith(Constant.MainBaseUrl)) {
                    getTransactionResponse(url)
                    return true
                } else true
                return false
            }
        }
        webView.loadUrl(url)
    }

    fun getTransactionResponse(url: String) {
        val stringRequest = StringRequest(
            Request.Method.POST, url,
            { response: String ->
                isTxnInProcess = false
                try {
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.getString("status")
                    addTransaction(
                        this@PayPalWebActivity,
                        orderId,
                        getString(R.string.paypal),
                        orderId,
                        status,
                        "",
                        sendParams
                    )
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        ) { }
        stringRequest.retryPolicy = DefaultRetryPolicy(0, 0, 0F)
        ApiConfig.getRequestQueue().cache.clear()
        ApiConfig.addToRequestQueue(stringRequest)

    }

    private fun addTransaction(
        activity: Activity,
        orderId: String,
        paymentType: String,
        txnid: String,
        status: String,
        message: String,
        sendParams: MutableMap<String, String>?
    ) {
        val transactionParams: MutableMap<String, String> = HashMap()
        transactionParams[Constant.ADD_TRANSACTION] = Constant.GetVal
        transactionParams[Constant.USER_ID] = sendParams!![Constant.USER_ID].toString()
        transactionParams[Constant.ORDER_ID] = orderId
        transactionParams[Constant.TYPE] = paymentType
        transactionParams[Constant.TRANS_ID] = txnid
        transactionParams[Constant.AMOUNT] = sendParams[Constant.FINAL_TOTAL].toString()
        transactionParams[Constant.STATUS] = status
        transactionParams[Constant.MESSAGE] = message
        val c = Calendar.getInstance().time
        @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        transactionParams["transaction_date"] = df.format(c)
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (from == Constant.WALLET) {
                                onBackPressed()
                                ApiConfig.getWalletBalance(activity, session)
                                Toast.makeText(
                                    activity,
                                    "Amount will be credited in wallet very soon.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (from == Constant.PAYMENT) {
                                if (status == Constant.SUCCESS || status == Constant.AWAITING_PAYMENT) {
                                    finish()
                                    val intent = Intent(activity, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.putExtra(Constant.FROM, "payment_success")
                                    activity.startActivity(intent)
                                } else {
                                    finish()
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, transactionParams, true)
    }

    override fun onBackPressed() {
        if (isTxnInProcess) {
            processAlertDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun processAlertDialog() {
        val alertDialog = AlertDialog.Builder(this@PayPalWebActivity)
        // Setting Dialog Message
        alertDialog.setMessage(getString(R.string.txn_cancel_msg))
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()
        alertDialog.setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface?, which: Int ->
            deleteTransaction(
                this@PayPalWebActivity, intent.getStringExtra(
                    Constant.ORDER_ID
                ).toString()
            )
            alertDialog1.dismiss()
        }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface?, which: Int -> alertDialog1.dismiss() }
        // Showing Alert Message
        alertDialog.show()
    }

    private fun deleteTransaction(activity: Activity, orderId: String) {
        val transactionParams: MutableMap<String, String> = HashMap()
        transactionParams[Constant.DELETE_ORDER] = Constant.GetVal
        transactionParams[Constant.ORDER_ID] = orderId
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    super@PayPalWebActivity.onBackPressed()
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, transactionParams, false)
    }
}