package wrteam.multivendor.customer.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import wrteam.multivendor.customer.helper.ApiConfig
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.activity.LoginActivity
import wrteam.multivendor.customer.activity.MainActivity
import wrteam.multivendor.customer.databinding.FragmentDrawerBinding
import wrteam.multivendor.customer.helper.*

class DrawerFragment : Fragment() {
    lateinit var binding: FragmentDrawerBinding

    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var fragment: Fragment
    lateinit var bundle: Bundle
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_drawer, container, false)

        binding = FragmentDrawerBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)

        imgProfile = root.findViewById(R.id.imgProfile)
        tvWallet1 = root.findViewById(R.id.tvWallet)

        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            binding.tvName.text = session.getData(Constant.NAME)
            binding.tvMobile.text = session.getData(Constant.MOBILE)
            binding.tvWallet.visibility = View.VISIBLE
            binding.imgEditProfile.visibility = View.VISIBLE

            Glide.with(activity).load(session.getData(Constant.PROFILE))
                .centerInside()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.imgProfile)

            binding.tvWallet.text = activity.resources.getString(R.string.wallet_balance) + "\t:\t" + session.getData(
                Constant.WALLET_BALANCE
            )
        } else {
            binding.tvWallet.visibility = View.GONE
            binding.imgEditProfile.visibility = View.GONE
            binding.tvName.text = resources.getString(R.string.is_login)
            binding.tvMobile.text = resources.getString(R.string.is_mobile)

            Glide.with(activity).load("-")
                .centerInside()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.imgProfile)
        }
        binding.imgEditProfile.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, ProfileFragment())
                .addToBackStack(null).commit()
        }
        binding.lytProfile.setOnClickListener {
            if (!session.getBoolean(
                    Constant.IS_USER_LOGIN
                )
            ) {
                startActivity(
                    Intent(activity, LoginActivity::class.java).putExtra(
                        Constant.FROM,
                        "drawer"
                    )
                )
            }
        }
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            if (session.getData(Constant.is_refer_earn_on) == "0") {
                binding.tvMenuReferEarn.visibility = View.GONE
            } else {
                binding.tvMenuReferEarn.visibility = View.VISIBLE
            }
            binding.tvMenuLogout.visibility = View.VISIBLE
            binding.lytMenuGroup.visibility = View.VISIBLE
        } else {
            binding.tvMenuLogout.visibility = View.GONE
            binding.lytMenuGroup.visibility = View.GONE
        }
        binding.tvMenuHome.setOnClickListener {
            Constant.homeClicked = false
            Constant.categoryClicked = false
            Constant.favoriteClicked = false
            Constant.drawerClicked = false
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(Constant.FROM, "")
            startActivity(intent)
        }
        binding.tvMenuCart.setOnClickListener {
            fragment = CartFragment()
            bundle = Bundle()
            bundle.putString(Constant.FROM, "mainActivity")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuNotification.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, NotificationFragment())
                .addToBackStack(null).commit()
        }
        binding.tvMenuOrders.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, OrderListFragment())
                .addToBackStack(null).commit()
        }
        binding.tvMenuWalletHistory.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, WalletTransactionFragment())
                .addToBackStack(null).commit()
        }
        binding.tvMenuTransactionHistory.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, TransactionFragment())
                .addToBackStack(null).commit()
        }
        binding.tvMenuChangePassword.setOnClickListener {
            openBottomDialog(
                activity
            )
        }
        binding.tvMenuManageAddresses.setOnClickListener {
            fragment = AddressListFragment()
            bundle = Bundle()
            bundle.putString(Constant.FROM, "MainActivity")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuReferEarn.setOnClickListener {
            if (session.getBoolean(
                    Constant.IS_USER_LOGIN
                )
            ) {
                MainActivity.fm.beginTransaction().add(R.id.container, ReferEarnFragment())
                    .addToBackStack(null).commit()
            } else {
                startActivity(Intent(activity, LoginActivity::class.java))
            }
        }
        binding.tvMenuContactUs.setOnClickListener {
            fragment = WebViewFragment()
            bundle = Bundle()
            bundle.putString("type", "Contact Us")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuAboutUs.setOnClickListener {
            fragment = WebViewFragment()
            bundle = Bundle()
            bundle.putString("type", "About Us")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuRateUs.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(Constant.PLAY_STORE_RATE_US_LINK + activity.packageName)
                    )
                )
            } catch (e: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(Constant.PLAY_STORE_LINK + activity.packageName)
                    )
                )
            }
        }
        binding.tvMenuShareApp.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.take_a_look) + "\"" + getString(R.string.app_name) + "\" - " + Constant.PLAY_STORE_LINK + activity.packageName
            )
            shareIntent.type = "text/plain"
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
        }
        binding.tvMenuFAQ.setOnClickListener {
            MainActivity.fm.beginTransaction().add(R.id.container, FaqFragment())
                .addToBackStack(null).commit()
        }
        binding.tvMenuTermsConditions.setOnClickListener {
            fragment = WebViewFragment()
            bundle = Bundle()
            bundle.putString("type", "Terms & Conditions")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuPrivacyPolicy.setOnClickListener {
            fragment = WebViewFragment()
            bundle = Bundle()
            bundle.putString("type", "Privacy Policy")
            fragment.arguments = bundle
            MainActivity.fm.beginTransaction().add(R.id.container, fragment).addToBackStack(null)
                .commit()
        }
        binding.tvMenuLogout.setOnClickListener {
            session.logoutUserConfirmation(
                activity
            )
        }
        return binding.root
    }

    @SuppressLint("InflateParams")
    private fun openBottomDialog(activity: Activity) {
        try {
            val sheetView =activity.layoutInflater.inflate(R.layout.dialog_change_password, null)
            val mBottomSheetDialog = BottomSheetDialog(activity, R.style.BottomSheetTheme)
            mBottomSheetDialog.setContentView(sheetView)
            mBottomSheetDialog.window!!
                .setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            mBottomSheetDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val edtOldPassword = sheetView.findViewById<EditText>(R.id.edtOldPassword)
            val edtNewPassword = sheetView.findViewById<EditText>(R.id.edtNewPassword)
            val edtConfirmNewPassword = sheetView.findViewById<EditText>(R.id.edtConfirmNewPassword)
            val imgChangePasswordClose =
                sheetView.findViewById<ImageView>(R.id.imgChangePasswordClose)
            val btnChangePassword = sheetView.findViewById<Button>(R.id.btnChangePassword)
            edtOldPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pass,
                0,
                R.drawable.ic_show,
                0
            )
            edtNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pass,
                0,
                R.drawable.ic_show,
                0
            )
            edtConfirmNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_pass,
                0,
                R.drawable.ic_show,
                0
            )
            Utils.setHideShowPassword(edtOldPassword)
            Utils.setHideShowPassword(edtNewPassword)
            Utils.setHideShowPassword(edtConfirmNewPassword)
            mBottomSheetDialog.setCancelable(true)
            imgChangePasswordClose.setOnClickListener {  mBottomSheetDialog.dismiss() }
            btnChangePassword.setOnClickListener { 
                val oldPassword = edtOldPassword.text.toString()
                val password = edtNewPassword.text.toString()
                val confirmPassword = edtConfirmNewPassword.text.toString()
                when {
                    password != confirmPassword -> {
                        edtConfirmNewPassword.requestFocus()
                        edtConfirmNewPassword.error = activity.getString(R.string.pass_not_match)
                    }
                    ApiConfig.checkValidation(oldPassword,
                        isMailValidation = false,
                        isMobileValidation = false
                    ) -> {
                        edtOldPassword.requestFocus()
                        edtOldPassword.error = activity.getString(R.string.enter_old_pass)
                    }
                    ApiConfig.checkValidation(password,
                        isMailValidation = false,
                        isMobileValidation = false
                    ) -> {
                        edtNewPassword.requestFocus()
                        edtNewPassword.error = activity.getString(R.string.enter_new_pass)
                    }
                    oldPassword != Session(activity).getData(Constant.PASSWORD) -> {
                        edtOldPassword.requestFocus()
                        edtOldPassword.error = activity.getString(R.string.no_match_old_pass)
                    }
                    else -> {
                        changePassword(password)
                    }
                }
            }
            mBottomSheetDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun changePassword(password: String) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.TYPE] = Constant.CHANGE_PASSWORD
        params[Constant.PASSWORD] = password
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        val alertDialog = AlertDialog.Builder(activity)
        // Setting Dialog Message
        alertDialog.setTitle(getString(R.string.change_pass))
        alertDialog.setMessage(getString(R.string.reset_alert_msg))
        alertDialog.setCancelable(false)
        val alertDialog1 = alertDialog.create()

        // Setting OK Button
        alertDialog.setPositiveButton(getString(R.string.yes)) { _: DialogInterface?, _: Int ->
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val `object` = JSONObject(response)
                            if (!`object`.getBoolean(Constant.ERROR)) {
                                session.logoutUser(activity)
                            }
                            Toast.makeText(
                                activity,
                                `object`.getString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
        }, activity, Constant.REGISTER_URL, params, true
                )
            }
                    alertDialog . setNegativeButton (getString(R.string.no)) { _: DialogInterface?, _: Int -> alertDialog1.dismiss() }
                    // Showing Alert Message
                    alertDialog . show ()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var tvWallet1: TextView

        @SuppressLint("StaticFieldLeak")
        lateinit var imgProfile: ImageView
    }
}