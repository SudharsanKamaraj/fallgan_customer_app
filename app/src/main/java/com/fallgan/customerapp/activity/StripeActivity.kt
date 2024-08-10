package com.fallgan.customerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardInputWidget
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.databinding.ActivityStripePaymentBinding
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class StripeActivity : AppCompatActivity() {

    lateinit var binding: ActivityStripePaymentBinding

    lateinit var payButton: Button
    lateinit var toolbar: Toolbar
    lateinit var tvTitle: TextView
    lateinit var tvPayableAmount:TextView
    lateinit var cardViewHamburger: CardView
    lateinit var toolbarTitle: TextView
    lateinit var imageMenu: ImageView
    lateinit var imageHome: ImageView
    
    lateinit var sendParams: MutableMap<String, String>
    lateinit var session: Session
    lateinit var amount: String
    private lateinit var stripe: Stripe
    private lateinit var paymentIntentClientSecret: String
    private lateinit var stripePublishableKey: String
    private lateinit var orderId: String
    private lateinit var from: String
    lateinit var activity: Activity

    private var isTxnInProcess = true

    @SuppressLint("SetTextI18n")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStripePaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cardViewHamburger = binding.cardViewHamburger
        toolbarTitle = binding.toolbarTitle
        imageMenu = binding.imageMenu
        imageHome = binding.imageHome
        payButton = binding.payButton
        tvTitle = binding.tvTitle
        tvPayableAmount = binding.tvPayableAmount

        activity = this
        session = Session(activity)
        sendParams = (intent.getSerializableExtra(Constant.PARAMS) as MutableMap<String, String>?)!!
        orderId = intent.getStringExtra(Constant.ORDER_ID).toString()
        from = intent.getStringExtra(Constant.FROM).toString()
        amount = sendParams[Constant.FINAL_TOTAL].toString()

        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toolbarTitle.text = getString(R.string.payment)
        imageHome.visibility = View.GONE
        imageMenu.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_arrow_back))
        imageMenu.visibility = View.VISIBLE
        cardViewHamburger.setOnClickListener { onBackPressed() }

        if (from == Constant.PAYMENT) {
            tvTitle.text = getString(R.string.app_name) + getString(R.string.shopping)
        } else {
            tvTitle.text = getString(R.string.app_name) + getString(R.string.wallet_recharge_)
        }
        tvPayableAmount.text = session.getData(Constant.CURRENCY) + sendParams[Constant.FINAL_TOTAL]
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.stripe)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        startCheckout()
    }

    private fun startCheckout() {
        val address = if (from == Constant.PAYMENT) {
            Constant.selectedAddress
        } else {
            Constant.DefaultAddress
        }
        val params: MutableMap<String, String> = HashMap()
        params[Constant.NAME] = session.getData(Constant.NAME).toString()
        params[Constant.ADDRESS_LINE1] = address
        if (Constant.DefaultPinCode.length > 5) {
            params[Constant.POSTAL_CODE] = "" + Constant.DefaultPinCode.toInt() / 10
        } else {
            params[Constant.POSTAL_CODE] = "" + Constant.DefaultPinCode
        }
        params[Constant.CITY] = Constant.DefaultCity
        params[Constant.AMOUNT] = amount
        params[Constant.ORDER_ID] = orderId
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            stripePublishableKey = jsonObject.getString(Constant.publishableKey)
                            paymentIntentClientSecret = jsonObject.getString(Constant.clientSecret)
                            stripe = Stripe(
                                activity,
                                Objects.requireNonNull(stripePublishableKey)
                            )
                            payButton.setOnClickListener {
                                val cardInputWidget =
                                    findViewById<CardInputWidget>(R.id.cardInputWidget)
                                val params1 = cardInputWidget.paymentMethodCreateParams
                                if (params1 != null) {
                                    val confirmParams =
                                        ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                                            params1,
                                            paymentIntentClientSecret
                                        )
                                    stripe.confirmPayment(this@StripeActivity, confirmParams)
                                }
                            }
                        } else {
                            payButton.isEnabled = false
                            Toast.makeText(
                                this@StripeActivity,
                                jsonObject.getString(Constant.MESSAGE),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, this@StripeActivity, Constant.STRIPE_BASE_URL, params, false)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        isTxnInProcess = false
        stripe.onPaymentResult(requestCode, data, PaymentResultCallback(this))
    }

    fun addTransaction(
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
                                    activity.getString(R.string.wallet_message),
                                    Toast.LENGTH_SHORT
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
        if (isTxnInProcess) processAlertDialog() else super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun processAlertDialog() {
        val alertDialog = AlertDialog.Builder(this@StripeActivity)
        // Setting Dialog Message
        alertDialog.setMessage(getString(R.string.txn_cancel_msg))
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()
        alertDialog.setPositiveButton(getString(R.string.yes)) { dialog: DialogInterface?, which: Int ->
            alertDialog1.dismiss()
            deleteTransaction(this@StripeActivity, orderId)
        }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface?, which: Int -> alertDialog1.dismiss() }
        alertDialog.show()
    }

    fun deleteTransaction(activity: Activity, orderId: String) {
        val transactionParams: MutableMap<String, String> = HashMap()
        transactionParams[Constant.DELETE_ORDER] = Constant.GetVal
        transactionParams[Constant.ORDER_ID] = orderId
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    super@StripeActivity.onBackPressed()
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, transactionParams, false)
    }

    private inner class PaymentResultCallback(activity: StripeActivity) :
        ApiResultCallback<PaymentIntentResult> {
        override fun onSuccess(result: PaymentIntentResult) {
            if (result.intent.status == StripeIntent.Status.Succeeded) {
                addTransaction(
                    this@StripeActivity,
                    orderId,
                    getString(R.string.stripe),
                    orderId,
                    Constant.SUCCESS,
                    "",
                    sendParams
                )
            } else if (result.intent.status == StripeIntent.Status.Processing) {
                addTransaction(
                    this@StripeActivity,
                    orderId,
                    getString(R.string.stripe),
                    orderId,
                    Constant.AWAITING_PAYMENT,
                    "",
                    sendParams
                )
            }
        }

        override fun onError(e: Exception) {
            deleteTransaction(this@StripeActivity, orderId)
            Toast.makeText(this@StripeActivity, e.message, Toast.LENGTH_SHORT).show()
        }

        init {
            WeakReference(activity)
        }
    }
}