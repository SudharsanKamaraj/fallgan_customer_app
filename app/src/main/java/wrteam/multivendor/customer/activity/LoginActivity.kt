package com.gpn.customerapp.activity

import `in`.aabhasjindal.otptextview.OtpTextView
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.databinding.ActivityLoginBinding
import com.gpn.customerapp.helper.*
import com.gpn.customerapp.R
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    lateinit var pinViewOTP: OtpTextView
    
    lateinit var session: Session
    lateinit var animShow: Animation

    private lateinit var animHide: Animation

    ////Firebase
    private lateinit var phoneNumber: String
    var firebaseOtp = ""
    private var otpFor = ""
    var resendOTP = false
    private lateinit var auth: FirebaseAuth
    private lateinit var mCallback: OnVerificationStateChangedCallbacks
    lateinit var databaseHelper: DatabaseHelper
    lateinit var activity: Activity
    var timerOn = false
    lateinit var from: String
    lateinit var mobile: String
    private lateinit var countryCode: String
    lateinit var dialog: ProgressDialog

    private var forMultipleCountryUse = true

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)

        setContentView(binding.root)

        activity = this@LoginActivity
        pinViewOTP = findViewById(R.id.pinViewOTP)
        session = Session(activity)
        databaseHelper = DatabaseHelper(activity)
        binding.toolbar.setBackgroundColor(
            ContextCompat.getColor(
                activity as LoginActivity,
                R.color.colorPrimary
            )
        )
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        animShow = AnimationUtils.loadAnimation(this, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(this, R.anim.view_hide)
        from = intent.getStringExtra(Constant.FROM).toString()

        binding.tvForgotPass.text = underlineSpannable(getString(R.string.forgot_text))
        binding.edtLoginMobile.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_phone, 0, 0, 0)
        binding.imgLoginPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pass,
            0,
            R.drawable.ic_show,
            0
        )
        binding.edtPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pass,
            0,
            R.drawable.ic_show,
            0
        )
        binding.edtConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pass,
            0,
            R.drawable.ic_show,
            0
        )
        binding.edtResetPass.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pass,
            0,
            R.drawable.ic_show,
            0
        )
        binding.edtResetCPass.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_pass,
            0,
            R.drawable.ic_show,
            0
        )
        Utils.setHideShowPassword(binding.edtPassword)
        Utils.setHideShowPassword(binding.edtConfirmPassword)
        Utils.setHideShowPassword(binding.imgLoginPassword)
        Utils.setHideShowPassword(binding.edtResetPass)
        Utils.setHideShowPassword(binding.edtResetCPass)
        binding.lytResetPass.visibility = View.GONE
        binding.lytLogin.visibility = View.VISIBLE
        binding.lytVerify.visibility = View.GONE
        binding.lytSignUp.visibility = View.GONE
        binding.lytOTP.visibility = View.GONE
        binding.lytWebView.visibility = View.GONE
        binding.tvWelcome.text = getString(R.string.welcome) + getString(R.string.app_name)

//        binding.edtCountryCodePicker.setCountryForNameCode("IN")
//        forMultipleCountryUse = false

        if (from != null) {
            when (from) {
                "drawer", "checkout", "tracker" -> {
                    binding.lytLogin.visibility = View.VISIBLE
                    binding.lytLogin.startAnimation(animShow)
                    Handler().postDelayed({ binding.edtLoginMobile.requestFocus() }, 1500)
                }
                "refer" -> {
                    otpFor = "new_user"
                    binding.lytVerify.visibility = View.VISIBLE
                    binding.lytVerify.startAnimation(animShow)
                    Handler().postDelayed({ binding.edtMobileVerify.requestFocus() }, 1500)
                }
                else -> {
                    binding.lytVerify.visibility = View.GONE
                    binding.lytResetPass.visibility = View.GONE
                    binding.lytVerify.visibility = View.GONE
                    binding.lytLogin.visibility = View.GONE
                    binding.lytSignUp.visibility = View.VISIBLE
                    binding.tvMobile.text = mobile
                    binding.edtRefer.setText(Constant.FRIEND_CODE)
                }
            }
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            binding.lytVerify.visibility = View.GONE
            binding.lytResetPass.visibility = View.GONE
            binding.lytVerify.visibility = View.GONE
            binding.lytLogin.visibility = View.VISIBLE
            binding.lytSignUp.visibility = View.GONE
        }
        startFirebaseLogin()
        privacyPolicy()
    }


    private fun generateOTP() {
        dialog = ProgressDialog.show(activity, "", getString(R.string.please_wait), true)
        session.setData(Constant.COUNTRY_CODE, countryCode)
        val params: MutableMap<String, String> = HashMap()
        params[Constant.TYPE] = Constant.VERIFY_USER
        params[Constant.MOBILE] = mobile
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        phoneNumber = "+" + session.getData(Constant.COUNTRY_CODE) + mobile
                        if (otpFor == "new_user") {
                            if (!`object`.getBoolean(Constant.ERROR)) {
                                dialog.dismiss()
                                setSnackBar(
                                    getString(R.string.alert_register_num1) + getString(R.string.app_name) + getString(
                                        R.string.alert_register_num2
                                    ), getString(R.string.btn_ok), from
                                )
                            } else {
                                sentRequest(phoneNumber)
                            }
                        } else if (otpFor == "exist_user") {
                            if (!`object`.getBoolean(Constant.ERROR)) {
                                Constant.U_ID = `object`.getString(Constant.ID)
                                sentRequest(phoneNumber)
                            } else {
                                dialog.dismiss()
                                setSnackBar(
                                    getString(R.string.alert_not_register_num1) + getString(R.string.app_name) + getString(
                                        R.string.alert_not_register_num2
                                    ), getString(R.string.btn_ok), from
                                )
                            }
                        }
                    } catch (ignored: JSONException) {
                    }
                }
            }
        }, activity, Constant.REGISTER_URL, params, false)
    }

    fun sentRequest(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallback)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startFirebaseLogin() {
        auth = FirebaseAuth.getInstance()
        mCallback = object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

            }

            override fun onVerificationFailed(e: FirebaseException) {
                setSnackBar(e.localizedMessage, getString(R.string.btn_ok), Constant.FAILED)
            }

            override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                super.onCodeSent(s, forceResendingToken)
                dialog.dismiss()
                firebaseOtp = s
                pinViewOTP.requestFocus()
                if (resendOTP) {
                    Toast.makeText(
                        activity,
                        getString(R.string.otp_resend_alert),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    binding.edtMobileVerify.isEnabled = false
                    binding.edtCountryCodePicker.setCcpClickable(false)
                    binding.btnVerify.text = getString(R.string.verify_otp)
                    binding.lytOTP.visibility = View.VISIBLE
                    binding.lytOTP.startAnimation(animShow)
                    object : CountDownTimer(120000, 1000) {
                        @SuppressLint("SetTextI18n")
                        override fun onTick(millisUntilFinished: Long) {
                            timerOn = true
                            // Used for formatting digit to be in 2 digits only
                            val f: NumberFormat = DecimalFormat("00")
                            val min = millisUntilFinished / 60000 % 60
                            val sec = millisUntilFinished / 1000 % 60
                            binding.tvTimer.text = f.format(min) + ":" + f.format(sec)
                        }

                        override fun onFinish() {
                            resendOTP = false
                            timerOn = false
                            binding.tvTimer.visibility = View.GONE
                            binding.img.setColorFilter(
                                ContextCompat.getColor(
                                    activity, R.color.colorPrimary
                                )
                            )
                            binding.tvResend.setTextColor(
                                ContextCompat.getColor(
                                    activity, R.color.colorPrimary
                                )
                            )
                            binding.tvResend.setOnClickListener {
                                resendOTP = true
                                sentRequest("+" + session.getData(Constant.COUNTRY_CODE) + mobile)
                                object : CountDownTimer(120000, 1000) {
                                    @SuppressLint("SetTextI18n")
                                    override fun onTick(millisUntilFinished: Long) {
                                        binding.tvTimer.visibility = View.VISIBLE
                                        binding.img.setColorFilter(
                                            ContextCompat.getColor(
                                                activity, R.color.gray
                                            )
                                        )
                                        binding.tvResend.setTextColor(
                                            ContextCompat.getColor(
                                                activity, R.color.gray
                                            )
                                        )
                                        timerOn = true
                                        // Used for formatting digit to be in 2 digits only
                                        val f: NumberFormat = DecimalFormat("00")
                                        val min = millisUntilFinished / 60000 % 60
                                        val sec = millisUntilFinished / 1000 % 60
                                        binding.tvTimer.text = f.format(min) + ":" + f.format(sec)
                                    }

                                    override fun onFinish() {
                                        resendOTP = false
                                        timerOn = false
                                        binding.tvTimer.visibility = View.GONE
                                        binding.img.setColorFilter(
                                            ContextCompat.getColor(
                                                activity, R.color.colorPrimary
                                            )
                                        )
                                        binding.tvResend.setTextColor(
                                            ContextCompat.getColor(
                                                activity, R.color.colorPrimary
                                            )
                                        )
                                        binding.tvResend.setOnClickListener { v1: View? ->
                                            resendOTP = true
                                            sentRequest("+" + session.getData(Constant.COUNTRY_CODE) + mobile)
                                        }
                                    }
                                }.start()
                            }
                        }
                    }.start()
                }
            }
        }
    }

    private fun forgotPassword() {
        val resetPassword = binding.edtResetPass.text.toString()
        val resetConfirmPassword = binding.edtResetCPass.text.toString()
        when {
            ApiConfig.checkValidation(resetPassword, false, false) -> {
                binding.edtResetPass.requestFocus()
                binding.edtResetPass.error = getString(R.string.enter_new_pass)
            }
            ApiConfig.checkValidation(resetConfirmPassword, false, false) -> {
                binding.edtResetCPass.requestFocus()
                binding.edtResetCPass.error = getString(R.string.enter_confirm_pass)
            }
            resetPassword != resetConfirmPassword -> {
                binding.edtResetCPass.requestFocus()
                binding.edtResetCPass.error = getString(R.string.pass_not_match)
            }
            else -> {
                val params: MutableMap<String, String> = HashMap()
                params[Constant.TYPE] = Constant.FORGOT_PASSWORD_MOBILE
                params[Constant.PASSWORD] = resetConfirmPassword
                //params.put(Constant.USER_ID, session.getData(Constant.ID));
                params[Constant.MOBILE] = mobile
                ApiConfig.requestToVolley(object : VolleyCallback {
                    override fun onSuccess(result: Boolean, response: String) {
                        if (result) {
                            try {
                                val `object` = JSONObject(response)
                                if (!`object`.getBoolean(Constant.ERROR)) {
                                    setSnackBar(
                                        getString(R.string.msg_reset_pass_success),
                                        getString(R.string.btn_ok),
                                        "forgot_password"
                                    )
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, activity, Constant.REGISTER_URL, params, true)
            }
        }
    }

    private fun userLogin(mobile: String, password: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.LOGIN] = Constant.GetVal
        params[Constant.MOBILE] = mobile
        params[Constant.PASSWORD] = password
        params[Constant.FCM_ID] = "" + session.getData(Constant.FCM_ID)
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            startMainActivity(
                                jsonObject.getJSONArray(Constant.DATA).getJSONObject(0),
                                password
                            )
                        }
                        Toast.makeText(
                            activity,
                            jsonObject.getString("message"),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.LOGIN_URL, params, true)
    }

    fun setSnackBar(message: String, action: String, type: String) {
        val snackbar =
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(action) {
            if (type == "forgot_password") {
                try {
                    binding.lytResetPass.visibility = View.GONE
                    binding.lytResetPass.startAnimation(animHide)
                    Thread.sleep(500)
                    binding.lytVerify.visibility = View.GONE
                    binding.lytVerify.startAnimation(animHide)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            snackbar.dismiss()
        }
        snackbar.setActionTextColor(Color.RED)
        val snackBarView = snackbar.view
        val textView = snackBarView.findViewById<TextView>(R.id.snackbar_text)
        textView.maxLines = 5
        snackbar.show()
    }

    @SuppressLint("SetTextI18n")
    fun otpVarification(otptext: String) {
        val credential = PhoneAuthProvider.getCredential(firebaseOtp, otptext)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    if (otpFor == "new_user") {
                        binding.tvMobile.text =
                            "+" + session.getData(Constant.COUNTRY_CODE) + " " + mobile
                        binding.lytSignUp.visibility = View.VISIBLE
                        binding.lytSignUp.startAnimation(animShow)
                    }
                    if (otpFor == "exist_user") {
                        binding.lytResetPass.visibility = View.VISIBLE
                        binding.lytResetPass.startAnimation(animShow)
                    }
                } else {
                    //verification unsuccessful.. display an error message
                    var message = "Something is wrong, we will fix it soon..."
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        message = "Invalid code entered..."
                    }
                    pinViewOTP.requestFocus()
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun userSignUpSubmit(name: String, email: String, password: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.TYPE] = Constant.REGISTER
        params[Constant.NAME] = name
        params[Constant.EMAIL] = email
        params[Constant.MOBILE] = mobile
        params[Constant.PASSWORD] = password
        params[Constant.COUNTRY_CODE] = session.getData(Constant.COUNTRY_CODE).toString()
        params[Constant.FCM_ID] = "" + session.getData(Constant.FCM_ID)
        params[Constant.FRIEND_CODE] = binding.edtRefer.text.toString().trim()
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            startMainActivity(jsonObject, password)
                        }
                        Toast.makeText(
                            activity,
                            jsonObject.getString("message"),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.REGISTER_URL, params, true)
    }

    fun onBtnClick(view: View) {
        val id = view.id
        hideKeyboard(activity, view)
        if (id == R.id.tvSignUp) {
            otpFor = "new_user"
            binding.edtMobileVerify.setText("")
            binding.edtMobileVerify.isEnabled = true
            binding.edtCountryCodePicker.setCcpClickable(forMultipleCountryUse)
            binding.lytOTP.visibility = View.GONE
            binding.lytVerify.visibility = View.VISIBLE
            binding.lytVerify.startAnimation(animShow)
        } else if (id == R.id.tvForgotPass) {
            otpFor = "exist_user"
            binding.edtMobileVerify.setText("")
            binding.edtMobileVerify.isEnabled = true
            binding.edtCountryCodePicker.setCcpClickable(forMultipleCountryUse)
            binding.lytOTP.visibility = View.GONE
            binding.lytVerify.visibility = View.VISIBLE
            binding.lytVerify.startAnimation(animShow)
        } else if (id == R.id.btnResetPass) {
            hideKeyboard(activity, view)
            forgotPassword()
        } else if (id == R.id.btnLogin) {
            mobile = binding.edtLoginMobile.text.toString()
            val password = binding.imgLoginPassword.text.toString()
            if (ApiConfig.checkValidation(mobile, false, false)) {
                binding.edtLoginMobile.requestFocus()
                binding.edtLoginMobile.error = getString(R.string.enter_mobile_no)
            } else if (ApiConfig.checkValidation(mobile, false, true)) {
                binding.edtLoginMobile.requestFocus()
                binding.edtLoginMobile.error = getString(R.string.enter_valid_mobile_no)
            } else if (ApiConfig.checkValidation(password, false, false)) {
                binding.imgLoginPassword.requestFocus()
                binding.imgLoginPassword.error = getString(R.string.enter_pass)
            } else {
                userLogin(mobile, password)
            }
        } else if (id == R.id.btnVerify) {
            if (binding.lytOTP.visibility == View.GONE) {
                hideKeyboard(activity, view)
                mobile = binding.edtMobileVerify.text.toString().trim()
                countryCode = binding.edtCountryCodePicker.selectedCountryCode
                if (ApiConfig.checkValidation(mobile, false, false)) {
                    binding.edtMobileVerify.requestFocus()
                    binding.edtMobileVerify.error = getString(R.string.enter_mobile_no)
                } else if (ApiConfig.checkValidation(mobile, false, true)) {
                    binding.edtMobileVerify.requestFocus()
                    binding.edtMobileVerify.error = getString(R.string.enter_valid_mobile_no)
                } else {
                    generateOTP()
                }
            } else {
                val otptext = Objects.requireNonNull(pinViewOTP.otp).trim()
                if (ApiConfig.checkValidation(otptext, false, false)) {
                    pinViewOTP.requestFocus()
                    Toast.makeText(activity, getString(R.string.enter_otp), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    otpVarification(otptext)
                }
            }
        } else if (id == R.id.btnRegister) {
            val name = binding.edtName.text.toString().trim()
            val email = "" + binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            val confirmPassword = binding.edtConfirmPassword.text.toString().trim()
            if (ApiConfig.checkValidation(name,
                    isMailValidation = false,
                    isMobileValidation = false
                )) {
                binding.edtName.requestFocus()
                binding.edtName.error = getString(R.string.enter_name)
            } else if (ApiConfig.checkValidation(email,
                    isMailValidation = false,
                    isMobileValidation = false
                )) {
                binding.edtEmail.requestFocus()
                binding.edtEmail.error = getString(R.string.enter_email)
            } else if (ApiConfig.checkValidation(email,
                    isMailValidation = true,
                    isMobileValidation = false
                )) {
                binding.edtEmail.requestFocus()
                binding.edtEmail.error = getString(R.string.enter_valid_email)
            } else if (ApiConfig.checkValidation(password,
                    isMailValidation = false,
                    isMobileValidation = false
                )) {
                binding.edtConfirmPassword.requestFocus()
                binding.edtPassword.error = getString(R.string.enter_pass)
            } else if (ApiConfig.checkValidation(confirmPassword,
                    isMailValidation = false,
                    isMobileValidation = false
                )) {
                binding.edtConfirmPassword.requestFocus()
                binding.edtConfirmPassword.error = getString(R.string.enter_confirm_pass)
            } else if (password != confirmPassword) {
                binding.edtConfirmPassword.requestFocus()
                binding.edtConfirmPassword.error = getString(R.string.pass_not_match)
            } else if (!binding.chPrivacy.isChecked) {
                Toast.makeText(activity, getString(R.string.alert_privacy_msg), Toast.LENGTH_LONG)
                    .show()
            } else {
                userSignUpSubmit(name, email, password)
            }
        } else if (id == R.id.tvResend) {
            resendOTP = true
            sentRequest("+" + session.getData(Constant.COUNTRY_CODE) + mobile)
        } else if (id == R.id.imgVerifyClose) {
            binding.lytOTP.visibility = View.GONE
            binding.lytVerify.visibility = View.GONE
            binding.lytVerify.startAnimation(animHide)
            binding.edtMobileVerify.setText("")
            binding.edtMobileVerify.isEnabled = true
            binding.edtCountryCodePicker.setCcpClickable(forMultipleCountryUse)
            pinViewOTP.resetState()
        } else if (id == R.id.imgResetPasswordClose) {
            binding.edtResetPass.setText("")
            binding.edtResetCPass.setText("")
            binding.lytResetPass.visibility = View.GONE
            binding.lytResetPass.startAnimation(animHide)
        } else if (id == R.id.imgSignUpClose) {
            binding.lytSignUp.visibility = View.GONE
            binding.lytSignUp.startAnimation(animHide)
            binding.tvMobile.text = ""
            binding.edtName.setText("")
            binding.edtEmail.setText("")
            binding.edtPassword.setText("")
            binding.edtConfirmPassword.setText("")
            binding.edtRefer.setText("")
        } else if (id == R.id.imgWebViewClose) {
            binding.lytWebView.visibility = View.GONE
            binding.lytWebView.startAnimation(animHide)
        }
    }

    private fun startMainActivity(jsonObject: JSONObject, password: String) {
        try {
            Session(activity).createuserLoginSession(
                jsonObject.getString(Constant.PROFILE), session.getData(Constant.FCM_ID),
                jsonObject.getString(Constant.USER_ID),
                jsonObject.getString(Constant.NAME),
                jsonObject.getString(Constant.EMAIL),
                jsonObject.getString(Constant.MOBILE),
                password,
                jsonObject.getString(Constant.REFERRAL_CODE)
            )
            ApiConfig.addMultipleProductInCart(session, activity, databaseHelper.cartData())
            ApiConfig.addMultipleProductInSaveForLater(
                session,
                activity,
                databaseHelper.saveForLaterData()
            )
            ApiConfig.getCartItemCount(activity, session)
            val favorites = databaseHelper.favorite()
            for (i in favorites.indices) {
                ApiConfig.addOrRemoveFavorite(activity, session, favorites[i], true)
            }
            databaseHelper.DeleteAllFavoriteData()
            databaseHelper.ClearCart()
            databaseHelper.ClearSaveForLater()
            ApiConfig.getWalletBalance(activity, session)
            session.setData(Constant.COUNTRY_CODE, jsonObject.getString(Constant.COUNTRY_CODE))
            Constant.homeClicked = false
            Constant.categoryClicked = false
            Constant.favoriteClicked = false
            Constant.drawerClicked = false
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(Constant.FROM, "")
            if (from != null && from == "checkout") {
                intent.putExtra("total", ApiConfig.stringFormat("" + Constant.FLOAT_TOTAL_AMOUNT))
                intent.putExtra(Constant.FROM, from)
            } else if (from != null && from == "tracker") {
                intent.putExtra(Constant.FROM, "tracker")
            }
            startActivity(intent)
            finish()
        } catch (e: JSONException) {
            e.printStackTrace()
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun underlineSpannable(text: String): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(UnderlineSpan(), 0, text.length, 0)
        return spannableString
    }

    fun getContent(type: String, key: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.SETTINGS] = Constant.GetVal
        params[type] = Constant.GetVal
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val obj = JSONObject(response)
                        if (!obj.getBoolean(Constant.ERROR)) {
                            val privacyStr = obj.getString(key)
                            binding.webView.isVerticalScrollBarEnabled = true
                            binding.webView.loadDataWithBaseURL("", privacyStr, "text/html", "UTF-8", "")
                        } else {
                            Toast.makeText(
                                activity,
                                obj.getString(Constant.MESSAGE),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, false)
    }

    private fun privacyPolicy() {
        binding.tvPrivacyPolicy.isClickable = true
        binding.tvPrivacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        val message = getString(R.string.msg_privacy_terms)
        val s2 = getString(R.string.terms_conditions)
        val s1 = getString(R.string.privacy_policy)
        val wordtoSpan: Spannable = SpannableString(message)
        wordtoSpan.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                getContent(Constant.GET_PRIVACY, "privacy")
                try {
                    Thread.sleep(500)
                    binding.lytWebView.visibility = View.VISIBLE
                    binding.lytWebView.startAnimation(animShow)
                } catch (ignored: Exception) {
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(activity, R.color.colorPrimary)
                ds.isUnderlineText
            }
        }, message.indexOf(s1), message.indexOf(s1) + s1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        wordtoSpan.setSpan(object : ClickableSpan() {
            override fun onClick(view: View) {
                getContent(Constant.GET_TERMS, "terms")
                try {
                    Thread.sleep(500)
                    binding.lytWebView.visibility = View.VISIBLE
                    binding.lytWebView.startAnimation(animShow)
                } catch (ignored: Exception) {
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(activity, R.color.colorPrimary)
                ds.isUnderlineText
            }
        }, message.indexOf(s2), message.indexOf(s2) + s2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvPrivacyPolicy.text = wordtoSpan
    }

    fun hideKeyboard(activity: Activity, root: View) {
        try {
            val inputMethodManager =
                (activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(root.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

}