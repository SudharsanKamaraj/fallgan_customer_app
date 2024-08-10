package com.fallgan.customerapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import co.paystack.android.Paystack.TransactionCallback
import co.paystack.android.PaystackSdk
import co.paystack.android.Transaction
import co.paystack.android.model.Card
import co.paystack.android.model.Charge
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.databinding.ActivityPayStackBinding
import com.fallgan.customerapp.fragment.WalletTransactionFragment
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import java.util.*

class PayStackActivity : AppCompatActivity() {

    lateinit var binding: ActivityPayStackBinding

    lateinit var email: String
    private lateinit var cardNumber: String
    private lateinit var cvv: String
    lateinit var session: Session
    lateinit var activity: Activity
    private lateinit var sendParams: MutableMap<String, String>

    lateinit var from: String

    //variables
    private lateinit var card: Card
    private lateinit var charge: Charge

    private var expiryMonth = 0
    private var expiryYear = 0
    private var payableAmount = 0.0


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_stack)

        binding = ActivityPayStackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            PaystackSdk.setPublicKey(Constant.PAYSTACK_KEY)
            activity = this@PayStackActivity
            session = Session(activity)
            PaystackSdk.initialize(activity)
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
            sendParams = (intent.getSerializableExtra("params") as MutableMap<String, String>)
            payableAmount = sendParams[Constant.FINAL_TOTAL]!!.toDouble()
            from = sendParams[Constant.FROM].toString()
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            binding.toolbarTitle.text = getString(R.string.paystack)
            binding.imageMenu.setImageDrawable(
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.ic_arrow_back
                )
            )
            binding.cardViewHamburger.setOnClickListener { onBackPressed() }
            binding.edtEmailField.setText(session.getData(Constant.EMAIL))
            binding.tvPayable.text = session.getData(Constant.CURRENCY) + payableAmount
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Method to perform the charging of the card
     */
    private fun performCharge() {
        try {
            //create a Charge object
            println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ${(payableAmount * 100).toString().split(".")[0]}")
            val amount = (payableAmount * 100).toString().split(".")[0].toInt()
            println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> $amount")
            charge = Charge()
            charge.card = card //set the card to charge
            charge.email = email //dummy email address
            charge.amount = amount //test amount
            PaystackSdk.chargeCard(this@PayStackActivity, charge, object : TransactionCallback {
                override fun onSuccess(transaction: Transaction) {
                    val paymentReference = transaction.reference
                    verifyReference(charge.amount.toString(), paymentReference, charge.email)
                }

                override fun beforeValidate(transaction: Transaction) {
                    // This is called only before requesting OTP.
                    // Save reference so you may send to server. If
                    // error occurs with OTP, you should still verify on server.
                }

                override fun onError(error: Throwable, transaction: Transaction) {
                    Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun validateForm(): Boolean {
        var valid = true
        val email = binding.edtEmailField.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.edtEmailField.error = "Required."
            valid = false
        } else {
            binding.edtEmailField.error = null
        }
        val cardNumber = binding.edtCardNumber.text.toString()
        if (TextUtils.isEmpty(cardNumber)) {
            binding.edtCardNumber.error = "Required."
            valid = false
        } else {
            binding.edtCardNumber.error = null
        }
        val expiryMonth = binding.edtExpiryMonth.text.toString()
        if (TextUtils.isEmpty(expiryMonth)) {
            binding.edtExpiryMonth.error = "Required."
            valid = false
        } else {
            binding.edtExpiryMonth.error = null
        }
        val expiryYear = binding.edtExpiryYear.text.toString()
        if (TextUtils.isEmpty(expiryYear)) {
            binding.edtExpiryYear.error = "Required."
            valid = false
        } else {
            binding.edtExpiryYear.error = null
        }
        val cvv = binding.edtCvv.text.toString()
        if (TextUtils.isEmpty(cvv)) {
            binding.edtCvv.error = "Required."
            valid = false
        } else {
            binding.edtCvv.error = null
        }
        return valid
    }

    fun onBtnClick(view: View) {
        if (validateForm()) {
            try {
                email = binding.edtEmailField.text.toString().trim()
                cardNumber = binding.edtCardNumber.text.toString().trim()
                expiryMonth = binding.edtExpiryMonth.text.toString().trim().toInt()
                expiryYear = binding.edtExpiryYear.text.toString().trim().toInt()
                cvv = binding.edtCvv.text.toString().trim()

                //String cardNumber = "4084 0840 8408 4081";
                //int expiryMonth = 11; //any month in the future
                //int expiryYear = 18; // any year in the future
                //String cvv = "408";
                card = Card(cardNumber, expiryMonth, expiryYear, cvv)
                if (card.isValid) {
                    performCharge()
                } else {
                    Toast.makeText(this@PayStackActivity, "Card is not Valid", Toast.LENGTH_LONG)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun verifyReference(amount: String, reference: String, email: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.VERIFY_PAYSTACK] = Constant.GetVal
        params[Constant.AMOUNT] = amount
        params[Constant.REFERENCE] = reference
        params[Constant.EMAIL] = email
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        val status = jsonObject.getString(Constant.STATUS)
                        if (from == Constant.WALLET) {
                            onBackPressed()
                            WalletTransactionFragment().addWalletBalance(
                                activity,
                                Session(activity),
                                WalletTransactionFragment.amount,
                                WalletTransactionFragment.msg
                            )
                        } else if (from == Constant.PAYMENT) {
                            PaymentActivity().placeOrder(
                                activity,
                                getString(R.string.paystack),
                                reference,
                                status.equals("success", ignoreCase = true),
                                sendParams,
                                status
                            )
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.VERIFY_PAYMENT_REQUEST, params, false)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}