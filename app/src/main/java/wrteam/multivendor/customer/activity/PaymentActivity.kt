package wrteam.multivendor.customer.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flutterwave.raveandroid.RaveConstants
import com.flutterwave.raveandroid.RavePayActivity
import com.flutterwave.raveandroid.RavePayManager
import com.google.gson.Gson
import com.paytm.pgsdk.PaytmOrder
import com.paytm.pgsdk.PaytmPGService
import com.paytm.pgsdk.PaytmPaymentTransactionCallback
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.sslcommerz.library.payment.model.datafield.MandatoryFieldModel
import com.sslcommerz.library.payment.model.dataset.TransactionInfo
import com.sslcommerz.library.payment.model.util.CurrencyType
import com.sslcommerz.library.payment.model.util.ErrorKeys
import com.sslcommerz.library.payment.model.util.SdkCategory
import com.sslcommerz.library.payment.model.util.SdkType
import com.sslcommerz.library.payment.viewmodel.listener.OnPaymentResultListener
import com.sslcommerz.library.payment.viewmodel.management.PayUsingSSLCommerz
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.adapter.AddressAdapter
import wrteam.multivendor.customer.adapter.CheckoutItemListAdapter
import wrteam.multivendor.customer.adapter.DateAdapter
import wrteam.multivendor.customer.adapter.SlotAdapter
import wrteam.multivendor.customer.databinding.ActivityPaymentBinding
import wrteam.multivendor.customer.helper.*
import wrteam.multivendor.customer.model.Address
import wrteam.multivendor.customer.model.BookingDate
import wrteam.multivendor.customer.model.Cart
import wrteam.multivendor.customer.model.Slot
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

class PaymentActivity : AppCompatActivity(), PaytmPaymentTransactionCallback,
    PaymentResultListener {

    lateinit var binding: ActivityPaymentBinding

    private lateinit var startDate: Calendar
    private lateinit var endDate: Calendar
    private lateinit var variantIdList: ArrayList<String>
    private lateinit var qtyList: ArrayList<String>
    private lateinit var dateList: ArrayList<String>
    private lateinit var bookingDates: ArrayList<BookingDate>
    private lateinit var slotList: ArrayList<Slot>
    private lateinit var dateAdapter: DateAdapter
    lateinit var activity: Activity
    lateinit var session: Session
    lateinit var checkoutItemListAdapter: CheckoutItemListAdapter
    lateinit var carts: ArrayList<Cart>
    private lateinit var animShow: Animation
    private lateinit var animHide: Animation
    lateinit var drawable: Drawable
    private lateinit var drawableStart: Drawable
    private lateinit var addressAdapter: AddressAdapter
    private var isPaymentAvailable = false
    var total = 0.0
    private var grandTotal = 0.0
    private var savedAmount = 0.0
    private var subTotal = 0.0
    private var usedBalance = 0.0
    private var pCodeDiscount = 0.0
    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var deliveryCharge = 0.0
    private var deliveryChargeWithCod = 0.0
    private var deliveryChargeWithoutCod = 0.0
    private var paymentMethods = 0
    private var pCode: String = ""


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Constant.orderPlaceable = true

        recyclerViewTimeSlot1 = binding.recyclerViewTimeSlot

        activity = this@PaymentActivity
        Constant.selectedDatePosition = 0
        session = Session(activity)

        paymentMethod = ""

        drawableStart = ContextCompat.getDrawable(activity, R.drawable.ic_down_arrow)!!
        animShow = AnimationUtils.loadAnimation(this, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(this, R.anim.view_hide)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbarTitle.text = getString(R.string.payment)
        binding.imageHome.visibility = View.GONE
        binding.imageMenu.setImageDrawable(
            ContextCompat.getDrawable(
                activity,
                R.drawable.ic_arrow_back
            )
        )
        binding.imageMenu.visibility = View.VISIBLE
        binding.cardViewHamburger.setOnClickListener { onBackPressed() }

        ApiConfig.getWalletBalance(activity, session)

        variantIdList = ArrayList()
        qtyList = ArrayList()

        getAllWidget()
    }

    @SuppressLint("SetTextI18n")
    private fun getAllWidget() {
        recyclerViewTimeSlot1.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewDates.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewSingleAddress.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewTimeSlot.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewCartItems.layoutManager = LinearLayoutManager(activity)
        showShimmer()
        binding.tvPaymentMethod.setOnClickListener { showPaymentOptions() }
        binding.imgPaymentListClose.setOnClickListener { hidePaymentOptions() }
        getAddress()
        binding.tvChangeAddress.setOnClickListener { onBackPressed() }
        binding.tvProceedOrder.setOnClickListener {
            if (Constant.orderPlaceable) {
                placeOrderProcess(
                    activity
                )
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.not_deliverable_message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        carts = ArrayList()
        checkoutItemListAdapter = CheckoutItemListAdapter(activity, carts)

        binding.chWallet.setOnClickListener {
            binding.lytWalletAmount.visibility = View.VISIBLE
            if (binding.chWallet.tag == "false") {
                binding.chWallet.isChecked = true
                binding.lytWallet.visibility = View.VISIBLE
                if (session.getData(Constant.WALLET_BALANCE)!!.toDouble() >= subTotal) {
                    usedBalance = subTotal
                    binding.tvWltBalance.text =
                        getString(R.string.remaining_wallet_balance) + session.getData(
                            Constant.CURRENCY
                        ) + ApiConfig.stringFormat(
                            "" + (session.getData(Constant.WALLET_BALANCE)!!
                                .toDouble() - usedBalance)
                        )
                } else {
                    usedBalance = session.getData(Constant.WALLET_BALANCE)!!.toDouble()
                    binding.tvWltBalance.text =
                        getString(R.string.remaining_wallet_balance) + session.getData(
                            Constant.CURRENCY
                        ) + "0.00"
                }
                binding.tvUsedWalletAmount.text =
                    "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + usedBalance
                    )
                grandTotal = subTotal + deliveryCharge - usedBalance
                if (grandTotal == 0.0) {
                    paymentMethod = Constant.WALLET
                }
                binding.tvGrandTotal.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + grandTotal
                    )
                binding.chWallet.tag = "true"
            } else {
                binding.lytWalletAmount.visibility = View.GONE
                walletUncheck()
            }
        }
        binding.confirmLyt.visibility = View.VISIBLE
        binding.scrollView.visibility = View.VISIBLE


        getCartData()

        if (session.getData(Constant.WALLET_BALANCE)!!.toDouble() == 0.0) {
            binding.lytWallet.visibility = View.GONE
        } else {
            binding.lytWallet.visibility = View.VISIBLE
        }

        binding.chWallet.tag = "false"

        binding.tvWltBalance.text =
            "Total Balance: " + session.getData(Constant.CURRENCY) + session.getData(
                Constant.WALLET_BALANCE
            )

        binding.tvSubTotal.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + subTotal)

//        getCartData(binding.rbCOD.isChecked)
    }

    fun getWalletBalance(activity: Activity, session: Session) {
        try {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_USER_DATA] = Constant.GetVal
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()

            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                val jsonObject1 =
                                    jsonObject.getJSONArray(Constant.DATA).getJSONObject(0)
                                Session(activity).setData(
                                    Constant.WALLET_BALANCE,
                                    jsonObject1.getString(Constant.BALANCE)
                                )


                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.REGISTER_DEVICE_URL, params, false)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun setPaymentDrawable(tag: String) {
//        println(">>>>>>>>>>>>>>>> setPaymentDrawable($tag)")
        when (tag) {
            "cod" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_cod)!!
            "RazorPay" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_razorpay)!!
            "Paystack" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_paystack)!!
            "Flutterwave" -> drawable =
                ContextCompat.getDrawable(activity, R.drawable.ic_flutterwave)!!
            "PayPal" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_paypal)!!
            "Midtrans" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_midtrans)!!
            "Stripe" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_stripe)!!
            "PayTm" -> drawable = ContextCompat.getDrawable(activity, R.drawable.ic_paytm)!!
            "SSLCOMMERZ" -> drawable =
                ContextCompat.getDrawable(activity, R.drawable.ic_sslecommerz)!!
            "bank_transfer" -> drawable =
                ContextCompat.getDrawable(activity, R.drawable.ic_bank)!!
        }
        binding.tvPaymentMethod.setCompoundDrawablesWithIntrinsicBounds(
            drawable,
            null,
            drawableStart,
            null
        )
    }

    private fun paymentConfig(isCODAllow: Boolean) {
//        println(">>>>>>>>>>>>>>>> paymentConfig")
        recyclerViewTimeSlot1.visibility = View.GONE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.SETTINGS] = Constant.GetVal
        params[Constant.GET_PAYMENT_METHOD] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (jsonObject.has(Constant.PAYMENT_METHODS)) {
                                val jsonObject = jsonObject.getJSONObject(Constant.PAYMENT_METHODS)
                                if (jsonObject.has(Constant.cod_payment_method)) {
                                    Constant.COD = jsonObject.getString(Constant.cod_payment_method)
                                    Constant.COD_MODE = jsonObject.getString(Constant.cod_mode)
                                }
                                if (jsonObject.has(Constant.razor_pay_method)) {
                                    Constant.RAZORPAY =
                                        jsonObject.getString(Constant.razor_pay_method)
                                    Constant.RAZOR_PAY_KEY_VALUE =
                                        jsonObject.getString(Constant.RAZOR_PAY_KEY)
                                }
                                if (jsonObject.has(Constant.paypal_method)) {
                                    Constant.PAYPAL = jsonObject.getString(Constant.paypal_method)
                                }
                                if (jsonObject.has(Constant.paystack_method)) {
                                    Constant.PAYSTACK =
                                        jsonObject.getString(Constant.paystack_method)
                                    Constant.PAYSTACK_KEY =
                                        jsonObject.getString(Constant.paystack_public_key)
                                }
                                if (jsonObject.has(Constant.flutterwave_payment_method)) {
                                    Constant.FLUTTERWAVE =
                                        jsonObject.getString(Constant.flutterwave_payment_method)
                                    Constant.FLUTTERWAVE_ENCRYPTION_KEY_VAL =
                                        jsonObject.getString(Constant.flutterwave_encryption_key)
                                    Constant.FLUTTERWAVE_PUBLIC_KEY_VAL =
                                        jsonObject.getString(Constant.flutterwave_public_key)
                                    Constant.FLUTTERWAVE_SECRET_KEY_VAL =
                                        jsonObject.getString(Constant.flutterwave_secret_key)
                                    Constant.FLUTTERWAVE_SECRET_KEY_VAL =
                                        jsonObject.getString(Constant.flutterwave_secret_key)
                                    Constant.FLUTTERWAVE_CURRENCY_CODE_VAL =
                                        jsonObject.getString(Constant.flutterwave_currency_code)
                                }
                                if (jsonObject.has(Constant.midtrans_payment_method)) {
                                    Constant.MIDTRANS =
                                        jsonObject.getString(Constant.midtrans_payment_method)
                                }
                                if (jsonObject.has(Constant.stripe_payment_method)) {
                                    Constant.STRIPE =
                                        jsonObject.getString(Constant.stripe_payment_method)
                                }
                                if (jsonObject.has(Constant.paytm_payment_method)) {
                                    Constant.PAYTM =
                                        jsonObject.getString(Constant.paytm_payment_method)
                                    Constant.PAYTM_MERCHANT_ID =
                                        jsonObject.getString(Constant.paytm_merchant_id)
                                    Constant.PAYTM_MERCHANT_KEY =
                                        jsonObject.getString(Constant.paytm_merchant_key)
                                    Constant.PAYTM_MODE =
                                        jsonObject.getString(Constant.paytm_mode)
                                }
                                if (jsonObject.has(Constant.ssl_commerce_payment_method)) {
                                    Constant.SSLECOMMERZ =
                                        jsonObject.getString(Constant.ssl_commerce_payment_method)
                                    Constant.SSLECOMMERZ_MODE =
                                        jsonObject.getString(Constant.ssl_commerece_mode)
                                    Constant.SSLECOMMERZ_STORE_ID =
                                        jsonObject.getString(Constant.ssl_commerece_store_id)
                                    Constant.SSLECOMMERZ_SECRET_KEY =
                                        jsonObject.getString(Constant.ssl_commerece_secret_key)
                                }
                                if (jsonObject.has(Constant.direct_bank_transfer_method)) {
                                    Constant.DIRECT_BANK_TRANSFER =
                                        jsonObject.getString(Constant.direct_bank_transfer_method)
                                    Constant.ACCOUNT_NAME =
                                        jsonObject.getString(Constant.account_name)
                                    Constant.ACCOUNT_NUMBER =
                                        jsonObject.getString(Constant.account_number)
                                    Constant.BANK_NAME = jsonObject.getString(Constant.bank_name)
                                    Constant.BANK_CODE = jsonObject.getString(Constant.bank_code)
                                    Constant.NOTES = jsonObject.getString(Constant.notes)
                                }
                                setPaymentMethod(isCODAllow)
                            } else {
                                recyclerViewTimeSlot1.visibility = View.VISIBLE
                                Toast.makeText(
                                    activity,
                                    getString(R.string.alert_payment_methods_blank),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        recyclerViewTimeSlot1.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, false)
    }

    private fun setCheckItem(radioButton: RadioButton?) {
        radioButton!!.isChecked = true
        setPaymentDrawable(radioButton.tag.toString())
    }

    private fun setPaymentMethod(isCODAllow: Boolean) {
//        println(">>>>>>>>>>>>>>>> setPaymentMethod")
        if (Constant.DIRECT_BANK_TRANSFER == "0" && Constant.FLUTTERWAVE == "0" && Constant.PAYPAL == "0" &&  Constant.COD == "0" && Constant.RAZORPAY == "0" && Constant.PAYSTACK == "0" && Constant.MIDTRANS == "0" && Constant.STRIPE == "0" && Constant.PAYTM == "0" && Constant.SSLECOMMERZ == "0") {
            isPaymentAvailable = false
        } else {
            isPaymentAvailable = true

            binding.lytPayment.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
                try {
                    val rb = findViewById<RadioButton>(checkedId)
                    carts = ArrayList()
                    updateDeliveryCharge(rb.tag.toString() == "cod" && isCODAllow)
                    paymentMethod = rb.tag.toString()
                    defaultPaymentMethod = rb.tag.toString()
                    hidePaymentOptions()
                    setPaymentDrawable(rb.tag.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            checkoutItemListAdapter = CheckoutItemListAdapter(activity, carts)
            binding.recyclerViewCartItems.adapter = checkoutItemListAdapter
            binding.confirmLyt.visibility = View.VISIBLE
            binding.recyclerViewCartItems.visibility = View.VISIBLE

            if (Constant.COD == "1") {
                if ((Constant.COD_MODE == Constant.product) && !isCODAllow) {
                    binding.rbCOD.visibility = View.GONE
                } else {
                    paymentMethods++
                    if (paymentMethod == "") {
                        setCheckItem(binding.rbCOD)
                    }
                    binding.rbCOD.visibility = View.VISIBLE
                }
            }


            if (Constant.RAZORPAY == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbRazorPay)
                binding.rbRazorPay.visibility = View.VISIBLE
            }
            if (Constant.PAYSTACK == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbPayStack)
                binding.rbPayStack.visibility = View.VISIBLE

            }
            if (Constant.FLUTTERWAVE == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbFlutterWave)
                binding.rbFlutterWave.visibility = View.VISIBLE
            }
            if (Constant.PAYPAL == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbPayPal)
                binding.rbPayPal.visibility = View.VISIBLE
            }
            if (Constant.MIDTRANS == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbMidTrans)
                binding.rbMidTrans.visibility = View.VISIBLE
            }
            if (Constant.STRIPE == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbStripe)
                binding.rbStripe.visibility = View.VISIBLE
            }
            if (Constant.PAYTM == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbPayTm)
                binding.rbPayTm.visibility = View.VISIBLE
            }
            if (Constant.SSLECOMMERZ == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbSslCommerz)
                binding.rbSslCommerz.visibility = View.VISIBLE
            }
            if (Constant.DIRECT_BANK_TRANSFER == "1") {
                paymentMethods++
                if (paymentMethod == "") setCheckItem(binding.rbBankTransfer)
                binding.rbBankTransfer.visibility = View.VISIBLE
            }
            binding.tvPaymentMethod.isClickable = paymentMethods > 1
        }
    }

    @SuppressLint("SetTextI18n")
    fun walletUncheck() {
        paymentMethod = defaultPaymentMethod
        setPaymentDrawable(paymentMethod)
        usedBalance = 0.0
        binding.tvWltBalance.text =
            getString(R.string.total) + session.getData(Constant.CURRENCY) + session.getData(
                Constant.WALLET_BALANCE
            )
        grandTotal = subTotal + deliveryCharge
        binding.tvGrandTotal.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                "" + grandTotal
            )
        binding.chWallet.isChecked = false
        binding.chWallet.tag = "false"
    }

    private fun getTimeSlots(session: Session, activity: Activity) {
        if (session.getData(Constant.SHIPPING_TYPE) == "local") {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.SETTINGS] = Constant.GetVal
            params[Constant.GET_TIME_SLOT_CONFIG] = Constant.GetVal
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject1 = JSONObject(response)
                            if (!jsonObject1.getBoolean(Constant.ERROR)) {
                                val jsonObject = JSONObject(
                                    jsonObject1.getJSONObject(Constant.TIME_SLOT_CONFIG).toString()
                                )
                                session.setData(
                                    Constant.IS_TIME_SLOTS_ENABLE,
                                    jsonObject.getString(Constant.IS_TIME_SLOTS_ENABLE)
                                )
                                session.setData(
                                    Constant.DELIVERY_STARTS_FROM,
                                    jsonObject.getString(Constant.DELIVERY_STARTS_FROM)
                                )
                                session.setData(
                                    Constant.ALLOWED_DAYS,
                                    jsonObject.getString(Constant.ALLOWED_DAYS)
                                )
                                if (session.getData(Constant.IS_TIME_SLOTS_ENABLE) == Constant.GetVal) {
                                    binding.deliveryTimeLyt.visibility = View.VISIBLE
                                    startDate = Calendar.getInstance()
                                    endDate = Calendar.getInstance()
                                    mYear = startDate.get(Calendar.YEAR)
                                    mMonth = startDate.get(Calendar.MONTH)
                                    mDay = startDate.get(Calendar.DAY_OF_MONTH)
                                    val deliveryStartFrom =
                                        session.getData(Constant.DELIVERY_STARTS_FROM)!!
                                            .toInt() - 1
                                    val deliveryAllowFrom =
                                        session.getData(Constant.ALLOWED_DAYS)!!.toInt()
                                    startDate.add(Calendar.DATE, deliveryStartFrom)
                                    endDate.add(
                                        Calendar.DATE,
                                        deliveryStartFrom + deliveryAllowFrom
                                    )
                                    dateList = ApiConfig.getDates(
                                        startDate.get(Calendar.DATE)
                                            .toString() + "-" + (startDate.get(
                                            Calendar.MONTH
                                        ) + 1) + "-" + startDate.get(Calendar.YEAR), endDate.get(
                                            Calendar.DATE
                                        )
                                            .toString() + "-" + (endDate.get(Calendar.MONTH) + 1) + "-" + endDate.get(
                                            Calendar.YEAR
                                        )
                                    )
                                    setDateList(dateList)
                                    getTimeSlots()
                                } else {
                                    binding.deliveryTimeLyt.visibility = View.GONE
                                    deliveryDay = "Date : N/A"
                                    deliveryTime = "Time : N/A"
                                    recyclerViewTimeSlot1.visibility = View.VISIBLE
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            recyclerViewTimeSlot1.visibility = View.VISIBLE
                        }
                    }
                }
            }, activity, Constant.SETTING_URL, params, false)
        } else {
            deliveryDay = "Date : N/A"
            deliveryTime = "Time : N/A"
        }
    }

    private fun getTimeSlots() {
        slotList = ArrayList()
        val params: MutableMap<String, String> = HashMap()
        params["get_time_slots"] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val jsonArray = jsonObject.getJSONArray("time_slots")
                            for (i in 0 until jsonArray.length()) {
                                val object1 = jsonArray.getJSONObject(i)
                                slotList.add(
                                    Slot(
                                        object1.getString(Constant.ID),
                                        object1.getString("title"),
                                        object1.getString("last_order_time")
                                    )
                                )
                            }
                            recyclerViewTimeSlot1.layoutManager = LinearLayoutManager(activity)
                            adapter = SlotAdapter(activity, slotList)
                            recyclerViewTimeSlot1.adapter = adapter
                            recyclerViewTimeSlot1.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        recyclerViewTimeSlot1.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, true)
    }

    @JvmName("setDateList1")
    fun setDateList(datesList: ArrayList<String>) {
        bookingDates = ArrayList()
        for (i in datesList.indices) {
            val date = datesList[i].split("-").toTypedArray()
            val bookingDate1 = BookingDate()
            bookingDate1.date = date[0]
            bookingDate1.month = date[1]
            bookingDate1.year = date[2]
            bookingDate1.day = date[3]
            bookingDates.add(bookingDate1)
        }
        dateAdapter = DateAdapter(activity, bookingDates)
        binding.recyclerViewDates.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewDates.adapter = dateAdapter
    }

    @SuppressLint("SetTextI18n")
    fun placeOrderProcess(activity: Activity) {
        when {
            deliveryDay.isEmpty() -> {
                Toast.makeText(
                    activity,
                    getString(R.string.select_delivery_day),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
            deliveryTime.isEmpty() -> {
                Toast.makeText(
                    activity,
                    getString(R.string.select_delivery_time),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
            paymentMethod.isEmpty() -> {
                Toast.makeText(
                    activity,
                    getString(R.string.select_payment_method),
                    Toast.LENGTH_SHORT
                )
                    .show()
                return
            }
            else -> {
                sendParams = HashMap()
                sendParams[Constant.PLACE_ORDER] = Constant.GetVal
                sendParams[Constant.USER_ID] = session.getData(Constant.ID).toString()
                sendParams[Constant.PRODUCT_VARIANT_ID] = variantIdList.toString()
                sendParams[Constant.QUANTITY] = qtyList.toString()
                sendParams[Constant.TOTAL] = "" + subTotal
                sendParams[Constant.DELIVERY_CHARGE] = "" + deliveryCharge
                sendParams[Constant.WALLET_BALANCE] = usedBalance.toString()
                sendParams[Constant.KEY_WALLET_USED] = binding.chWallet.tag.toString()
                sendParams[Constant.FINAL_TOTAL] = "" + grandTotal
                sendParams[Constant.PAYMENT_METHOD] = paymentMethod
                if (paymentMethod == "bank_transfer") {
                    sendParams[Constant.STATUS] = Constant.AWAITING_PAYMENT
                }
                if (pCode.isNotEmpty()) {
                    sendParams[Constant.PROMO_CODE] = pCode
                    sendParams[Constant.PROMO_DISCOUNT] =
                        ApiConfig.stringFormat("" + pCodeDiscount)
                }
                sendParams[Constant.DELIVERY_TIME] = "$deliveryDay - $deliveryTime"
                sendParams[Constant.ADDRESS_ID] = Constant.selectedAddressId
                val alertDialog = AlertDialog.Builder(
                    activity
                )
                val inflater = activity.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_order_confirm, null)
                alertDialog.setView(dialogView)
                alertDialog.setCancelable(true)
                val dialog = alertDialog.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val lytDialogPromo: LinearLayout = dialogView.findViewById(R.id.lytDialogPromo)
                val lytDialogWallet: LinearLayout = dialogView.findViewById(R.id.lytDialogWallet)
                val tvDialogItemTotal: TextView = dialogView.findViewById(R.id.tvDialogItemTotal)
                val tvDialogDeliveryCharge: TextView =
                    dialogView.findViewById(R.id.tvDialogDeliveryCharge)
                val tvDialogTotal: TextView = dialogView.findViewById(R.id.tvDialogTotal)
                val tvDialogPCAmount: TextView = dialogView.findViewById(R.id.tvDialogPCAmount)
                val tvDialogWallet: TextView = dialogView.findViewById(R.id.tvDialogWallet)
                val tvDialogFinalTotal: TextView = dialogView.findViewById(R.id.tvDialogFinalTotal)
                val tvDialogCancel: TextView = dialogView.findViewById(R.id.tvDialogCancel)
                val tvDialogConfirm: TextView = dialogView.findViewById(R.id.tvDialogConfirm)
                val tvSpecialNote: EditText = dialogView.findViewById(R.id.tvSpecialNote)
                if (pCodeDiscount > 0) {
                    lytDialogPromo.visibility = View.VISIBLE
                    tvDialogPCAmount.text =
                        "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + pCodeDiscount)
                } else {
                    lytDialogPromo.visibility = View.GONE
                }
                if (binding.chWallet.tag.toString() == "true") {
                    lytDialogWallet.visibility = View.VISIBLE
                    tvDialogWallet.text =
                        "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + usedBalance)
                } else {
                    lytDialogWallet.visibility = View.GONE
                }
                tvDialogItemTotal.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + subTotal)
                tvDialogDeliveryCharge.text =
                    if (deliveryCharge > 0) session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                        "" + deliveryCharge
                    ) else getString(
                        R.string.free
                    )
                tvDialogTotal.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + (subTotal + deliveryCharge))
                tvDialogFinalTotal.text =
                    session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + grandTotal)
                tvDialogConfirm.setOnClickListener {
                    setProgressDialog()
                    sendParams[Constant.ORDER_NOTE] = tvSpecialNote.text.toString().trim()
                    if (paymentMethod == "cod" || paymentMethod == "wallet" || paymentMethod == "bank_transfer") {
                        ApiConfig.requestToVolley(object : VolleyCallback {
                            override fun onSuccess(result: Boolean, response: String) {
                                if (result) {
                                    try {
                                        val jsonObject = JSONObject(response)
                                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                                            if (binding.chWallet.tag.toString() == "true") {
                                                ApiConfig.getWalletBalance(activity, session)
                                            }
                                            dialog.dismiss()
                                            val intent = Intent(activity, MainActivity::class.java)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            intent.putExtra(Constant.FROM, "payment_success")
                                            activity.startActivity(intent)
                                        } else {
                                            dialog_.dismiss()
                                            Toast.makeText(
                                                activity,
                                                jsonObject.getString(Constant.MESSAGE),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: JSONException) {
                                        e.printStackTrace()
                                        dialog_.dismiss()
                                    }
                                }
                            }
                        }, activity, Constant.ORDER_PROCESS_URL, sendParams, true)
                        dialog.dismiss()
                    } else {
                        sendParams[Constant.USER_NAME] = session.getData(Constant.NAME).toString()
                        when (paymentMethod) {

                            getString(R.string.paypal) -> {
                                dialog.dismiss()
                                sendParams[Constant.FROM] = Constant.PAYMENT
                                sendParams[Constant.STATUS] = Constant.AWAITING_PAYMENT
                                placeOrder(
                                    activity,
                                    getString(R.string.midtrans),
                                    System.currentTimeMillis()
                                        .toString() + Constant.randomNumeric(3),
                                    true,
                                    sendParams,
                                    "paypal"
                                )
                            }
                            "RazorPay" -> {
                                dialog.dismiss()
                                createOrderId(grandTotal)
                            }
                            "Paystack" -> {
                                dialog.dismiss()
                                sendParams[Constant.FROM] = Constant.PAYMENT
                                val intent = Intent(activity, PayStackActivity::class.java)
                                intent.putExtra(Constant.PARAMS, sendParams as Serializable?)
                                startActivity(intent)
                            }
                            "Midtrans" -> {
                                dialog.dismiss()
                                sendParams[Constant.FROM] = Constant.PAYMENT
                                sendParams[Constant.STATUS] = Constant.AWAITING_PAYMENT
                                placeOrder(
                                    activity,
                                    getString(R.string.midtrans),
                                    System.currentTimeMillis()
                                        .toString() + Constant.randomNumeric(3),
                                    true,
                                    sendParams,
                                    "midtrans"
                                )
                            }
                            "stripe" -> {
                                dialog.dismiss()
                                sendParams[Constant.FROM] = Constant.PAYMENT
                                sendParams[Constant.STATUS] = Constant.AWAITING_PAYMENT
                                placeOrder(
                                    activity,
                                    getString(R.string.stripe),
                                    System.currentTimeMillis()
                                        .toString() + Constant.randomNumeric(3),
                                    true,
                                    sendParams,
                                    "stripe"
                                )
                            }
                            "Flutterwave" -> {
                                dialog.dismiss()
                                startFlutterWavePayment()
                            }
                            "PayTm" -> {
                                dialog.dismiss()
                                startPayTmPayment()
                            }
                            "SSLCOMMERZ" -> {
                                dialog.dismiss()
                                startSslCommerzPayment(
                                    activity,
                                    sendParams[Constant.FINAL_TOTAL].toString(),
                                    System.currentTimeMillis()
                                        .toString() + Constant.randomNumeric(3),
                                    sendParams
                                )
                            }
                        }
                    }
                }
                tvDialogCancel.setOnClickListener { dialog.dismiss() }
                dialog.show()
            }
        }
    }

    private fun startSslCommerzPayment(
        activity: Activity,
        amount: String,
        transId: String,
        sendParams: MutableMap<String, String>
    ) {
        val mode: String = if (Constant.SSLECOMMERZ_MODE == "sandbox") {
            SdkType.TESTBOX
        } else {
            SdkType.LIVE
        }
        val mandatoryFieldModel = MandatoryFieldModel(
            Constant.SSLECOMMERZ_STORE_ID,
            Constant.SSLECOMMERZ_SECRET_KEY,
            amount,
            transId,
            CurrencyType.BDT,
            mode,
            SdkCategory.BANK_LIST
        )

/* Call for the payment */
        PayUsingSSLCommerz.getInstance()
            .setData(activity, mandatoryFieldModel, object : OnPaymentResultListener {
                override fun transactionSuccess(transactionInfo: TransactionInfo) {
// If payment is success and risk label is 0.
                    placeOrder(
                        activity,
                        getString(R.string.sslecommerz),
                        transactionInfo.tranId,
                        true,
                        sendParams,
                        "SSLECOMMERZ"
                    )
                    try {
                        if (dialog_ != null) {
                            dialog_.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun transactionFail(sessionKey: String) {
                    try {
                        if (dialog_ != null) {
                            dialog_.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Toast.makeText(activity, sessionKey, Toast.LENGTH_LONG).show()
                }

                override fun error(errorCode: Int) {
                    try {
                        if (dialog_ != null) {
                            dialog_.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    when (errorCode) {
                        ErrorKeys.USER_INPUT_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.user_input_error),
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.INTERNET_CONNECTION_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.internet_connection_error),
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.DATA_PARSING_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.data_parsing_error),
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.CANCEL_TRANSACTION_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.user_cancel_transaction_error),
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.SERVER_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.server_error),
                            Toast.LENGTH_LONG
                        ).show()
                        ErrorKeys.NETWORK_ERROR -> Toast.makeText(
                            activity,
                            activity.getString(R.string.network_error),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    private fun createOrderId(payable: Double) {
        val params: MutableMap<String, String> = HashMap()
        params["amount"] = "" + payable.roundToLong() + "00"
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            startPayment(
                                jsonObject.getString(Constant.ID),
                                jsonObject.getString("amount")
                            )
                        } else {
                            Toast.makeText(
                                activity,
                                jsonObject.getString(Constant.MESSAGE),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                dialog_.dismiss()
            }
        }, activity, Constant.GET_RAZORPAY_ORDER_URL, params, true)
    }

    private fun startPayment(orderId: String, payAmount: String) {
        val checkout = Checkout()
        checkout.setKeyID(Constant.RAZOR_PAY_KEY_VALUE)
        checkout.setImage(R.mipmap.ic_launcher)
        try {
            val options = JSONObject()
            options.put(Constant.NAME, session.getData(Constant.NAME))
            options.put(Constant.ORDER_ID, orderId)
            options.put(Constant.CURRENCY, "INR")
            options.put(Constant.AMOUNT, payAmount)
            val preFill = JSONObject()
            preFill.put(Constant.EMAIL, session.getData(Constant.EMAIL))
            preFill.put(Constant.CONTACT, session.getData(Constant.MOBILE))
            options.put("prefill", preFill)
            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.d(TAG, "Error in starting Razorpay Checkout", e)
        }
    }

    fun placeOrder(
        activity: Activity,
        paymentType: String,
        transactionId: String,
        isSuccess: Boolean,
        sendParams: MutableMap<String, String>,
        payType: String
    ) {
        if (isSuccess) {
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                sendParams[Constant.ORDER_ID] =
                                    jsonObject.getString(Constant.ORDER_ID)
                                when (payType) {
                                    "stripe" -> createStripePayment(jsonObject.getString(Constant.ORDER_ID))
                                    "midtrans" -> createMidtransPayment(
                                        jsonObject.getString(Constant.ORDER_ID),
                                        ApiConfig.stringFormat("" + grandTotal).toString()
                                    )
                                    "paypal" -> startPaypalPayment(sendParams)
                                    else -> addTransaction(
                                        activity,
                                        paymentType,
                                        transactionId,
                                        payType,
                                        activity.getString(R.string.order_success),
                                        sendParams
                                    )
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.ORDER_PROCESS_URL, sendParams, false)
        } else {
            addTransaction(
                activity,
                "RazorPay",
                transactionId,
                payType,
                getString(R.string.order_failed),
                sendParams
            )
        }
    }

    private fun createMidtransPayment(orderId: String, grossAmount: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.ORDER_ID] = orderId
        if (grossAmount.contains(",")) {
            params[Constant.GROSS_AMOUNT] = grossAmount.split(",").toTypedArray()[0]
        } else if (grossAmount.contains(".")) {
            params[Constant.GROSS_AMOUNT] = grossAmount.split("\\.").toTypedArray()[0]
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val intent = Intent(activity, MidtransActivity::class.java)
                            intent.putExtra(
                                Constant.URL, jsonObject.getJSONObject(Constant.DATA).getString(
                                    Constant.REDIRECT_URL
                                )
                            )
                            intent.putExtra(Constant.ORDER_ID, orderId)
                            intent.putExtra(Constant.FROM, Constant.PAYMENT)
                            intent.putExtra(Constant.PARAMS, sendParams as Serializable?)
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                activity,
                                jsonObject.getString(Constant.MESSAGE),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                if (dialog_ != null) {
                    dialog_.dismiss()
                }
            }
        }, activity, Constant.MIDTRANS_PAYMENT_URL, params, true)
    }

    private fun createStripePayment(orderId: String) {
        val intent = Intent(activity, StripeActivity::class.java)
        intent.putExtra(Constant.ORDER_ID, orderId)
        intent.putExtra(Constant.FROM, Constant.PAYMENT)
        intent.putExtra(Constant.PARAMS, sendParams as Serializable?)
        startActivity(intent)
    }

    private fun addTransaction(
        activity: Activity,
        paymentType: String,
        transactionId: String,
        status: String,
        message: String,
        sendParams: MutableMap<String, String>?
    ) {
        val transactionParams: MutableMap<String, String> = HashMap()
        transactionParams[Constant.ADD_TRANSACTION] = Constant.GetVal
        transactionParams[Constant.USER_ID] = sendParams!![Constant.USER_ID].toString()
        transactionParams[Constant.ORDER_ID] = sendParams[Constant.ORDER_ID].toString()
        transactionParams[Constant.TYPE] = paymentType
        transactionParams[Constant.TAX_PERCENT] = "0"
        transactionParams[Constant.TRANS_ID] = transactionId
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
                            if (status != Constant.FAILED) {
                                try {
                                    if (dialog_ != null) {
                                        dialog_.dismiss()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                val intent = Intent(activity, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.putExtra(Constant.FROM, "payment_success")
                                activity.startActivity(intent)
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, transactionParams, false)
    }

    private fun startPaypalPayment(sendParams: MutableMap<String, String>?) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.FIRST_NAME] = sendParams!![Constant.USER_NAME].toString()
        params[Constant.LAST_NAME] = sendParams[Constant.USER_NAME].toString()
        params[Constant.PAYER_EMAIL] = "" + sendParams[Constant.EMAIL]
        params[Constant.ITEM_NAME] = "Card Order"
        params[Constant.ITEM_NUMBER] =
            System.currentTimeMillis().toString() + Constant.randomNumeric(3)
        params[Constant.AMOUNT] = sendParams[Constant.FINAL_TOTAL].toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                val intent = Intent(activity, PayPalWebActivity::class.java)
                intent.putExtra(Constant.URL, response)
                intent.putExtra(Constant.ORDER_ID, params[Constant.ITEM_NUMBER])
                intent.putExtra(Constant.FROM, Constant.PAYMENT)
                intent.putExtra(Constant.PARAMS, sendParams as Serializable?)
                startActivity(intent)
            }
        }, activity, Constant.PAPAL_URL, params, true)
    }

    private fun startPayTmPayment() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.ORDER_ID_] = Constant.randomAlphaNumeric(20)
        params[Constant.CUST_ID] = Constant.randomAlphaNumeric(10)
        params[Constant.TXN_AMOUNT] = ApiConfig.stringFormat("" + grandTotal).toString()
        if (Constant.PAYTM_MODE == "sandbox") {
            params[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_DEMO_VAL
            params[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_DEMO_VAL
            params[Constant.WEBSITE] = Constant.WEBSITE_DEMO_VAL
        } else if (Constant.PAYTM_MODE == "production") {
            params[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_LIVE_VAL
            params[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_LIVE_VAL
            params[Constant.WEBSITE] = Constant.WEBSITE_LIVE_VAL
        }

//        System.out.println("====" + params.toString());
        ApiConfig.requestToVolley(object : VolleyCallback, PaytmPaymentTransactionCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject1 = JSONObject(response)
                        val jsonObject = jsonObject1.getJSONObject(Constant.DATA)
//                    System.out.println("=======res  " + response.toString());
                        val service = if (Constant.PAYTM_MODE == "sandbox") {
                            PaytmPGService.getStagingService(Constant.PAYTM_ORDER_PROCESS_DEMO_VAL)
                        } else {
                            PaytmPGService.getProductionService()
                        }
                        customerId = jsonObject.getString(Constant.CUST_ID)
//creating a hashmap and adding all the values required
                        val paramMap = HashMap<String, String>()
                        paramMap[Constant.MID] = Constant.PAYTM_MERCHANT_ID
                        paramMap[Constant.ORDER_ID_] = jsonObject.getString(Constant.ORDER_ID_)
                        paramMap[Constant.CUST_ID] = jsonObject.getString(Constant.CUST_ID)
                        paramMap[Constant.TXN_AMOUNT] =
                            ApiConfig.stringFormat("" + grandTotal)
                        if (Constant.PAYTM_MODE == "sandbox") {
                            paramMap[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_DEMO_VAL
                            paramMap[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_DEMO_VAL
                            paramMap[Constant.WEBSITE] = Constant.WEBSITE_DEMO_VAL
                        } else if (Constant.PAYTM_MODE == "production") {
                            paramMap[Constant.INDUSTRY_TYPE_ID] = Constant.INDUSTRY_TYPE_ID_LIVE_VAL
                            paramMap[Constant.CHANNEL_ID] = Constant.MOBILE_APP_CHANNEL_ID_LIVE_VAL
                            paramMap[Constant.WEBSITE] = Constant.WEBSITE_LIVE_VAL
                        }
                        paramMap[Constant.CALLBACK_URL] =
                            jsonObject.getString(Constant.CALLBACK_URL)
                        paramMap[Constant.CHECKSUMHASH] = jsonObject1.getString("signature")

//creating a paytm order object using the hashmap
                        val order = PaytmOrder(paramMap)

//initializing the paytm service
                        service!!.initialize(order, null)

//finally starting the payment transaction
                        service.startPaymentTransaction(activity, true, true, this)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onTransactionResponse(bundle: Bundle) {
                val orderId = bundle.getString(Constant.ORDERID)!!
                val status = bundle.getString(Constant.STATUS_)
                if (status.equals(Constant.TXN_SUCCESS, ignoreCase = true)) {
                    verifyTransaction(orderId)
                } else {
                    dialog_.dismiss()
                }
            }

            override fun networkNotAvailable() {
                dialog_.dismiss()
                Toast.makeText(activity, "Network error", Toast.LENGTH_LONG).show()
            }

            override fun clientAuthenticationFailed(s: String) {
                dialog_.dismiss()
                Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
            }

            override fun someUIErrorOccurred(s: String) {
                dialog_.dismiss()
                Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
            }

            override fun onErrorLoadingWebPage(i: Int, s: String, s1: String) {
                dialog_.dismiss()
                Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
            }

            override fun onBackPressedCancelTransaction() {
                dialog_.dismiss()
                Toast.makeText(activity, "Back Pressed", Toast.LENGTH_LONG).show()
            }

            override fun onTransactionCancel(s: String, bundle: Bundle) {
                dialog_.dismiss()
                Toast.makeText(activity, s + bundle.toString(), Toast.LENGTH_LONG).show()
            }
        }, activity, Constant.GENERATE_PAYTM_CHECKSUM, params, false)
    }

    override fun onTransactionResponse(bundle: Bundle) {
        val orderId = bundle.getString(Constant.ORDERID)!!
        val status = bundle.getString(Constant.STATUS_)
        if (status.equals(Constant.TXN_SUCCESS, ignoreCase = true)) {
            verifyTransaction(orderId)
        } else {
            dialog_.dismiss()
        }
    }

    /**
     * Verifying the transaction status once PayTM transaction is over
     * This makes server(own) -> server(PayTM) call to verify the transaction status
     */
    private fun verifyTransaction(orderId: String) {
        val params: MutableMap<String, String> = HashMap()
        params["orderId"] = orderId
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        val status = jsonObject.getJSONObject("body").getJSONObject("resultInfo")
                            .getString("resultStatus")
                        if (status.equals("TXN_SUCCESS", ignoreCase = true)) {
                            val txnId = jsonObject.getJSONObject("body").getString("txnId")
                            placeOrder(
                                activity,
                                getString(R.string.paytm),
                                txnId,
                                true,
                                sendParams,
                                Constant.SUCCESS
                            )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.VALID_TRANSACTION, params, false)
    }

    override fun networkNotAvailable() {
        dialog_.dismiss()
        Toast.makeText(activity, "Network error", Toast.LENGTH_LONG).show()
    }

    override fun clientAuthenticationFailed(s: String) {
        dialog_.dismiss()
        Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
    }

    override fun someUIErrorOccurred(s: String) {
        dialog_.dismiss()
        Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
    }

    override fun onErrorLoadingWebPage(i: Int, s: String, s1: String) {
        dialog_.dismiss()
        Toast.makeText(activity, s, Toast.LENGTH_LONG).show()
    }

    override fun onBackPressedCancelTransaction() {
        dialog_.dismiss()
        Toast.makeText(activity, "Back Pressed", Toast.LENGTH_LONG).show()
    }

    override fun onTransactionCancel(s: String, bundle: Bundle) {
        dialog_.dismiss()
        Toast.makeText(activity, s + bundle.toString(), Toast.LENGTH_LONG).show()
    }

    private fun startFlutterWavePayment() {
        RavePayManager(this)
            .setAmount(grandTotal)
            .setEmail(session.getData(Constant.EMAIL))
            .setCurrency(Constant.FLUTTERWAVE_CURRENCY_CODE_VAL)
            .setfName(session.getData(Constant.FIRST_NAME))
            .setlName(session.getData(Constant.LAST_NAME))
            .setNarration(getString(R.string.app_name) + getString(R.string.shopping))
            .setPublicKey(Constant.FLUTTERWAVE_PUBLIC_KEY_VAL)
            .setEncryptionKey(Constant.FLUTTERWAVE_ENCRYPTION_KEY_VAL)
            .setTxRef(System.currentTimeMillis().toString() + "Ref")
            .acceptAccountPayments(true)
            .acceptCardPayments(true)
            .acceptAccountPayments(true)
            .acceptAchPayments(true)
            .acceptBankTransferPayments(true)
            .acceptBarterPayments(true)
            .acceptGHMobileMoneyPayments(true)
            .acceptRwfMobileMoneyPayments(true)
            .acceptSaBankPayments(true)
            .acceptFrancMobileMoneyPayments(true)
            .acceptZmMobileMoneyPayments(true)
            .acceptUssdPayments(true)
            .acceptUkPayments(true)
            .acceptMpesaPayments(true)
            .shouldDisplayFee(true)
            .onStagingEnv(false)
            .showStagingLabel(false)
            .initialize()
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null && data.getStringExtra(
                "response"
            ) != null
        ) {
            try {
                val details = JSONObject(data.getStringExtra("response"))
                val jsonObject = details.getJSONObject(Constant.DATA)
                when (resultCode) {
                    RavePayActivity.RESULT_SUCCESS -> {
                        Toast.makeText(
                            activity,
                            getString(R.string.order_placed1),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        placeOrder(
                            activity,
                            getString(R.string.flutterwave),
                            jsonObject.getString("txRef"),
                            true,
                            sendParams,
                            Constant.SUCCESS
                        )
                    }
                    RavePayActivity.RESULT_ERROR -> {
                        placeOrder(activity, "", "", false, sendParams, Constant.PENDING)
                        Toast.makeText(activity, getString(R.string.order_error), Toast.LENGTH_LONG)
                            .show()
                    }
                    RavePayActivity.RESULT_CANCELLED -> {
                        placeOrder(activity, "", "", false, sendParams, Constant.FAILED)
                        Toast.makeText(
                            activity,
                            getString(R.string.order_cancel),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            if (dialog_ != null) {
                dialog_.dismiss()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.payment)
        activity.invalidateOptionsMenu()
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        try {
            razorPayId = razorpayPaymentID
            PaymentActivity().placeOrder(
                this@PaymentActivity,
                paymentMethod,
                razorPayId,
                true,
                sendParams,
                Constant.SUCCESS
            )
        } catch (e: Exception) {
            Log.d(TAG, "onPaymentSuccess  ", e)
        }
    }

    override fun onPaymentError(code: Int, response: String) {
        try {
            try {
                if (dialog_ != null) {
                    dialog_.dismiss()
                }
            } catch (ignore: Exception) {
            }
            Toast.makeText(activity, getString(R.string.order_cancel), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.d(TAG, "onPaymentError  ", e)
        }
    }

    /*   Address Part Start   */
    private fun getAddress() {
        singleAddress = ArrayList()
        val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerViewSingleAddress.layoutManager = linearLayoutManager
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ADDRESSES] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.ADDRESS_ID] = Constant.selectedAddressId
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL).toInt().toDouble()
                            session.setData(Constant.TOTAL, total.toString())
                            val jsonObject1 =
                                jsonObject.getJSONArray(Constant.DATA).getJSONObject(0)
                            val address =
                                Gson().fromJson(jsonObject1.toString(), Address::class.java)
                            address.selected = true
                            singleAddress.add(address)
                            getTimeSlots(session, activity)
                            addressAdapter = AddressAdapter(
                                activity,
                                singleAddress,
                                R.layout.lyt_address_checkout
                            )
                            binding.recyclerViewSingleAddress.adapter = addressAdapter
                        } else {
                            binding.tvChangeAddress.text =
                                activity.getString(R.string.add_address)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_ADDRESS_URL, params, false)
    }

    /*  Cart Items Part Start   */
    @SuppressLint("SetTextI18n")
    fun getCartData() {
        var isCODAllow = true;
        carts = ArrayList()
        ApiConfig.getCartItemCount(activity, session)
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_USER_CART] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.ADDRESS_ID] = Constant.selectedAddressId

        params[Constant.LIMIT] = "" + Constant.TOTAL_CART_ITEM
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (intent.getStringExtra(Constant.PROMO_CODE) != null && intent.getStringExtra(
                                    Constant.PROMO_CODE
                                )!!.isNotEmpty()
                            ) {
                                pCode = intent.getStringExtra(Constant.PROMO_CODE).toString()
                                pCodeDiscount =
                                    intent.getDoubleExtra(Constant.PROMO_DISCOUNT, 0.0)
                                binding.lytPromoDiscount.visibility = View.VISIBLE
                                binding.tvPromoDiscount.text =
                                    "-" + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                                        "" + pCodeDiscount
                                    )
                                binding.tvPromoCode.text =
                                    activity.getString(R.string.promo_discount) + " (" + pCode + ")"
                            } else {
                                binding.lytPromoDiscount.visibility = View.GONE
                            }
                            subTotal =
                                jsonObject.getString(Constant.SUB_TOTAL)
                                    .toDouble()

                            if (jsonObject.has(Constant.DELIVERY_CHARGE_WITH_COD)) {
                                deliveryChargeWithCod =
                                    jsonObject.get(Constant.DELIVERY_CHARGE_WITH_COD).toString()
                                        .toDouble()

                                if (session.getData(Constant.SHIPPING_TYPE) == "local") {
                                    deliveryChargeWithoutCod = deliveryChargeWithCod
                                }
                            }
//
                            if (jsonObject.has(Constant.DELIVERY_CHARGE_WITHOUT_COD) && (session.getData(
                                    Constant.SHIPPING_TYPE
                                ) != "local")
                            ) {
                                deliveryChargeWithoutCod =
                                    jsonObject.get(Constant.DELIVERY_CHARGE_WITHOUT_COD).toString()
                                        .toDouble()
                            }

                            savedAmount = jsonObject.getString(Constant.SAVED_AMOUNT).toDouble()
                            binding.tvSaveAmount.text =
                                session.getData(Constant.CURRENCY) + savedAmount

                            val jsonArray = jsonObject.getJSONArray(Constant.DATA)
                            val gson = Gson()
                            for (i in 0 until jsonArray.length()) {
                                try {
                                    val cart = gson.fromJson(
                                        jsonArray.getJSONObject(i).toString(),
                                        Cart::class.java
                                    )
                                    if (cart.save_for_later == "0") {
                                        variantIdList.add(cart.product_variant_id)
                                        qtyList.add(cart.qty)
                                        carts.add(cart)
                                    }
                                    if (cart.item[0].cod_allowed == "0") {
                                        isCODAllow = false
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            paymentConfig(isCODAllow)
                        }
                        hideShimmer()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        hideShimmer()
                    }
                }
            }
        }, activity, Constant.CART_URL, params, false)
    }

    @SuppressLint("SetTextI18n")
    fun updateDeliveryCharge(isCOD: Boolean) {
        deliveryCharge = if (isCOD) {
            deliveryChargeWithCod
        } else {
            deliveryChargeWithoutCod
        }

        grandTotal = (subTotal + deliveryCharge) - pCodeDiscount
        binding.tvSubTotal.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + subTotal)
        binding.tvDeliveryCharge.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + deliveryCharge)
        binding.tvGrandTotal.text =
            session.getData(Constant.CURRENCY) + ApiConfig.stringFormat("" + grandTotal)
    }

    /*   Cart Items Part End
    =======================================================================
    Change Payment Method Actions Start */
    private fun showPaymentOptions() {
        binding.lytMainPaymentMethods.visibility = View.VISIBLE
        binding.lytPaymentMethods.visibility = View.VISIBLE
        binding.lytPaymentMethods.startAnimation(animShow)
    }

    private fun hidePaymentOptions() {
        binding.lytPaymentMethods.visibility = View.GONE
        binding.lytPaymentMethods.startAnimation(animHide)
        animHide.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                binding.lytMainPaymentMethods.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    /*  Change Payment Method Actions End
    =======================================================================
    Shimmer Layout Actions Start   */
    private fun showShimmer() {
        binding.lytMain.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
    }

    private fun hideShimmer() {
        binding.lytMain.visibility = View.VISIBLE
        binding.shimmerFrameLayout.visibility = View.GONE
        binding.shimmerFrameLayout.stopShimmer()
    }

    /*  Shimmer Layout Actions End

    Progress Dialog Start */
    private fun setProgressDialog() {
        val llPadding = 30
        val ll = LinearLayout(activity)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.setPadding(llPadding, llPadding, llPadding, llPadding)
        ll.gravity = Gravity.CENTER
        var llParam = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam
        val progressBar = ProgressBar(activity)
        progressBar.isIndeterminate = true
        progressBar.setPadding(0, 0, llPadding, 0)
        progressBar.layoutParams = llParam
        llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        val tvText = TextView(activity)
        tvText.text = activity.getString(R.string.please_wait)
        tvText.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary))
        tvText.textSize = 20f
        tvText.layoutParams = llParam
        ll.addView(progressBar)
        ll.addView(tvText)
        val builder = AlertDialog.Builder(
            activity
        )
        builder.setCancelable(true)
        builder.setView(ll)
        dialog_ = builder.create()
        dialog_.show()
        val window = dialog_.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog_.window!!.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog_.window!!.attributes = layoutParams
        }
    }

    companion object {
        val TAG: String? = PaymentActivity::class.java.simpleName

        var payFromWallet = false
        lateinit var paymentMethod: String
        lateinit var customerId: String
        lateinit var razorPayId: String
        lateinit var defaultPaymentMethod: String
        lateinit var deliveryTime: String
        lateinit var deliveryDay: String
        lateinit var sendParams: MutableMap<String, String>

        @SuppressLint("StaticFieldLeak")
        lateinit var adapter: SlotAdapter
        lateinit var singleAddress: ArrayList<Address>
        lateinit var dialog_: AlertDialog
        lateinit var recyclerViewTimeSlot1: RecyclerView
    }
}