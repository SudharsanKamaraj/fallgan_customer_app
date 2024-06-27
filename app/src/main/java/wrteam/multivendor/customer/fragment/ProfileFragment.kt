package wrteam.multivendor.customer.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.StrictMode
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.com.coursion.freakycoder.mediapicker.galleries.Gallery
import wrteam.multivendor.customer.databinding.FragmentProfileBinding
import wrteam.multivendor.customer.helper.*
import wrteam.multivendor.customer.helper.ApiConfig.Companion.createJWT
import java.io.File

class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding
    private val openMediaPicker = 1  // Request code
    private val permissionReadExternalStorage = 100       // Request code for read external storage
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_profile, container, false)
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        activity = requireActivity()

        setHasOptionsMenu(true)
        session = Session(activity)

        Glide.with(activity).load(session.getData(Constant.PROFILE))
                .centerInside()
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.imgProfile)
        binding.fabProfile.setOnClickListener {
            if (!permissionIfNeeded()) {
                val intent = Intent(activity, Gallery::class.java)
                // Set the title
                intent.putExtra("title", activity.getString(R.string.select_images))
                // Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
                intent.putExtra("mode", 2)
                intent.putExtra("maxSelection", 1) // Optional
                intent.putExtra("tabBarHidden", true) //Optional - default value is false
                startActivityForResult(intent, openMediaPicker)
            }
        }
        binding.tvChangePassword.setOnClickListener {
            openBottomDialog(
                    activity
            )
        }
        binding.btnSubmit.setOnClickListener {
            val name = binding.edtName.text.toString()
            val email = binding.edtEmail.text.toString()
            val mobile = binding.edtMobile.text.toString()
            when {
                ApiConfig.checkValidation(name,
                    isMailValidation = false,
                    isMobileValidation = false
                ) -> {
                    binding.edtName.requestFocus()
                    binding.edtName.error = getString(R.string.enter_name)
                }
                ApiConfig.checkValidation(email,
                    isMailValidation = false,
                    isMobileValidation = false
                ) -> {
                    binding.edtEmail.requestFocus()
                    binding.edtEmail.error = getString(R.string.enter_email)
                }
                ApiConfig.checkValidation(email,
                    isMailValidation = true,
                    isMobileValidation = false
                ) -> {
                    binding.edtEmail.requestFocus()
                    binding.edtEmail.error = getString(R.string.enter_valid_email)
                }
                else -> {
                    val params: MutableMap<String, String> = HashMap()
                    params[Constant.TYPE] = Constant.EDIT_PROFILE
                    params[Constant.USER_ID] = session.getData(Constant.ID).toString()
                    params[Constant.NAME] = name
                    params[Constant.EMAIL] = email
                    params[Constant.MOBILE] = mobile
                    params[Constant.FCM_ID] = session.getData(Constant.FCM_ID).toString()
                    ApiConfig.requestToVolley(object : VolleyCallback {
                        override fun onSuccess(result: Boolean, response: String) {
                            if (result) {
                                try {
                                    val jsonObject = JSONObject(response)
                                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                                        session.setData(Constant.NAME, name)
                                        session.setData(Constant.EMAIL, email)
                                        session.setData(Constant.MOBILE, mobile)
                                    }
                                    Toast.makeText(
                                            activity,
                                            jsonObject.getString("message"),
                                            Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }, activity, Constant.REGISTER_URL, params, true)
                }
            }
        }
        binding.edtName.setText(session.getData(Constant.NAME))
        binding.edtEmail.setText(session.getData(Constant.EMAIL))
        binding.edtMobile.setText(session.getData(Constant.MOBILE))
        return binding.root
    }

    private fun permissionIfNeeded(): Boolean {
        if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    permissionReadExternalStorage
            )
            return true
        }
        return false
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
        val positiveButton =
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
        alertDialog.setNegativeButton(getString(R.string.no)) { _: DialogInterface?, _: Int -> alertDialog1.dismiss() }
        // Showing Alert Message
        alertDialog.show()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.profile)
        activity.invalidateOptionsMenu()
        hideKeyboard()
    }

    fun hideKeyboard() {
        try {
            val inputMethodManager =
                    (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            inputMethodManager.hideSoftInputFromWindow(root.applicationWindowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_logout).isVisible = true
        menu.findItem(R.id.toolbar_search).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == openMediaPicker) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK && data != null) {
                val selectionResult = data.getStringArrayListExtra("result")!!
                var imagePath = ""
                for (path in selectionResult) {
                    imagePath = path
                }
                updateProfile(activity, imagePath)
            }
        }
    }

    private fun updateProfile(activity: Activity, filePath: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val client = OkHttpClient().newBuilder().build()
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        builder.addFormDataPart(Constant.AccessKey, Constant.AccessKeyVal)
        builder.addFormDataPart(Constant.TYPE, Constant.UPLOAD_PROFILE)
        builder.addFormDataPart(Constant.USER_ID, session.getData(Constant.ID).toString())

        val file = File(filePath)
        builder.addFormDataPart(
                Constant.PROFILE,
                file.name,
                RequestBody.create(MediaType.get("application/octet-stream"), file)
        )

        val body: RequestBody = builder.build()

        val request = Request.Builder()
                .url(Constant.REGISTER_URL)
                .method("POST", body)
                .addHeader(
                        Constant.AUTHORIZATION,
                        "Bearer " + createJWT("eKart", "eKart Authentication")
                )
                .build()

        val response = client.newCall(request).execute()
        val jsonObject = JSONObject(response.peekBody(Long.MAX_VALUE).string())

        try {

            if (!jsonObject.getBoolean(Constant.ERROR)) {
                session.setData(
                        Constant.PROFILE,
                        jsonObject.getString(Constant.PROFILE)
                )
                Glide.with(activity)
                        .load(session.getData(Constant.PROFILE))
                        .centerInside()
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(binding.imgProfile)
                Glide.with(activity)
                        .load(session.getData(Constant.PROFILE))
                        .centerInside()
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(DrawerFragment.imgProfile)
            }
            Toast.makeText(
                    activity,
                    jsonObject.getString(Constant.MESSAGE),
                    Toast.LENGTH_SHORT
            ).show()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Toast.makeText(activity, jsonObject.getString(Constant.MESSAGE), Toast.LENGTH_SHORT).show()


    }
}