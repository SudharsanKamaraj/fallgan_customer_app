package wrteam.multivendor.customer.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.ParseException
import android.net.Uri
import android.text.Html
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.FirebaseApp
import com.google.gson.Gson
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.adapter.OfferAdapter
import wrteam.multivendor.customer.model.HomeOffer
import wrteam.multivendor.customer.model.Product
import wrteam.multivendor.customer.model.Slider
import java.security.Key
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

class ApiConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        mInstance = this
        mRequestQueue = Volley.newRequestQueue(applicationContext)
        FirebaseApp.initializeApp(this)
    }

    companion object {
        private val tag: String = ApiConfig::class.java.simpleName
        private var isDialogOpen = false
        private lateinit var mInstance: ApiConfig
        private lateinit var mRequestQueue: RequestQueue

        fun volleyErrorMessage(error: VolleyError?): String {
            var message = ""
            try {
                message = when (error) {
                    is NetworkError -> {
                        "Cannot connect to Internet...Please check your connection!"
                    }
                    is ServerError -> {
                        "The server could not be found. Please try again after some time!"
                    }
                    is AuthFailureError -> {
                        "Cannot connect to Internet...Please check your connection!"
                    }
                    is ParseError -> {
                        "Parsing error! Please try again after some time!"
                    }
                    is TimeoutError -> {
                        "Connection TimeOut! Please check your internet connection."
                    }
                    else -> ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return message
        }

        @SuppressLint("InflateParams")
        fun isConnected(activity: Activity): Boolean {
            var check = false
            try {
                val connectionManager =
                    activity.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectionManager.activeNetworkInfo
                if (networkInfo != null && networkInfo.isConnected) {
                    check = true
                } else {
                    try {
                        if (!isDialogOpen) {
                            val sheetView: View =
                                activity.layoutInflater.inflate(R.layout.dialog_no_internet, null)
                            val parentViewGroup = sheetView.parent as ViewGroup
                            parentViewGroup.removeAllViews()
                            val mBottomSheetDialog = Dialog(activity)
                            mBottomSheetDialog.setContentView(sheetView)
                            mBottomSheetDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            mBottomSheetDialog.show()
                            isDialogOpen = true
                            val btnRetry: Button = sheetView.findViewById(R.id.btnRetry)
                            mBottomSheetDialog.setCancelable(false)
                            btnRetry.setOnClickListener {
                                if (isConnected(activity)) {
                                    isDialogOpen = false
                                    mBottomSheetDialog.dismiss()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return check
        }

        fun createJWT(issuer: String?, subject: String?): String? {
            try {
                val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.HS256
                val apiKeySecretBytes = Constant.JWT_KEY.toByteArray()
                val signingKey: Key = SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.jcaName)
                val now: Instant = Instant.now()
                return Jwts.builder()
                    .claim("app", "31977632")
                    .setSubject(subject)
                    .setIssuer(issuer)
                    .setId(UUID.randomUUID().toString())
                    .setIssuedAt(Date.from(now))
                    .signWith(signatureAlgorithm, signingKey)
                    .compact()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun requestToVolley(
            callback: VolleyCallback,
            activity: Activity,
            url: String,
            params: MutableMap<String, String>,
            isProgress: Boolean
        ) {
            if (ProgressDisplay.mProgressBar != null) {
                ProgressDisplay.mProgressBar!!.visibility = View.GONE
            }
            val progressDisplay = ProgressDisplay(activity)
            progressDisplay.hideProgress()
            if (isProgress) progressDisplay.showProgress()
            val stringRequest: StringRequest = object : StringRequest(
                Method.POST, url,
                Response.Listener { response: String ->
                    if (isConnected(activity)) callback.onSuccess(true, response)
                    if (isProgress) progressDisplay.hideProgress()
                },
                Response.ErrorListener { error: VolleyError? ->
                    if (isProgress) progressDisplay.hideProgress()
                    if (isConnected(activity)) callback.onSuccess(false, "")
                    val message: String = volleyErrorMessage(error)
                    if (message != "") Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }) {
                override fun getHeaders(): MutableMap<String, String> {
                    val params1: MutableMap<String, String> = HashMap()
                    params1[Constant.AUTHORIZATION] =
                        "Bearer " + createJWT("eKart", "eKart Authentication")
                    return params1
                }

                override fun getParams(): MutableMap<String, String> {
                    params[Constant.AccessKey] = Constant.AccessKeyVal
                    return params
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(0, 0, 0F)
            getRequestQueue().cache.clear()
            addToRequestQueue(stringRequest)
        }

        fun openUnderMaintenanceDialog(activity: Activity) {
            @SuppressLint("InflateParams") val sheetView: View =
                activity.layoutInflater.inflate(R.layout.dialog_under_maintenance, null)
            val mBottomSheetDialog = Dialog(activity)
            mBottomSheetDialog.setContentView(sheetView)
            mBottomSheetDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mBottomSheetDialog.show()
            val lottieAnimationView: LottieAnimationView =
                sheetView.findViewById(R.id.lottieAnimationView)
            val tvCloseApp = sheetView.findViewById<TextView>(R.id.tvCloseApp)
            lottieAnimationView.setAnimation("under_maintenance.json")
            lottieAnimationView.repeatCount = LottieDrawable.INFINITE
            lottieAnimationView.playAnimation()
            tvCloseApp.setOnClickListener {
                activity.finish()
                exitProcess(0)
            }
            mBottomSheetDialog.setCancelable(false)
        }

        fun getProductList(jsonArray: JSONArray): ArrayList<Product> {
            val productArrayList = ArrayList<Product>()
            try {
                for (i in 0 until jsonArray.length()) {
                    val product: Product =
                        Gson().fromJson(jsonArray.get(i).toString(), Product::class.java)
                    productArrayList.add(product)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return productArrayList
        }

        fun getFavoriteProductList(jsonArray: JSONArray): ArrayList<Product> {
            val productArrayList = ArrayList<Product>()
            try {
                for (i in 0 until jsonArray.length()) {
                    val product: Product =
                        Gson().fromJson(jsonArray.get(i).toString(), Product::class.java)
                    productArrayList.add(product)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return productArrayList
        }


        fun getAddress(lat: Double, lng: Double, activity: Activity): String? {
            val addresses: List<Address>
            val geocoder = Geocoder(activity, Locale.getDefault())
            return try {
                addresses = geocoder.getFromLocation(lat, lng, 1) as List<Address>
                addresses[0].getAddressLine(0)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }

        fun openBottomDialog(activity: Activity) {
            try {
                val sheetView: View =
                    activity.layoutInflater.inflate(R.layout.dialog_update_app, null)
                val mBottomSheetDialog = BottomSheetDialog(activity, R.style.BottomSheetTheme)
                mBottomSheetDialog.setContentView(sheetView)
                mBottomSheetDialog.show()
                mBottomSheetDialog.window!!
                    .setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                val imgClose = sheetView.findViewById<ImageView>(R.id.imgClose)
                val btnNotNow = sheetView.findViewById<Button>(R.id.btnNotNow)
                val btnUpdateNow = sheetView.findViewById<Button>(R.id.btnUpdateNow)
                if (Session(activity).getData(Constant.is_version_system_on) == "0") {
                    run {
                        btnNotNow.visibility = View.VISIBLE
                        imgClose.visibility = View.VISIBLE
                        mBottomSheetDialog.setCancelable(true)
                    }
                } else {
                    mBottomSheetDialog.setCancelable(false)
                }
                imgClose.setOnClickListener {
                    if (mBottomSheetDialog.isShowing) Session(activity)
                        .setBoolean("update_skip", true)
                    mBottomSheetDialog.dismiss()
                }
                btnNotNow.setOnClickListener {
                    Session(activity).setBoolean("update_skip", true)
                    if (mBottomSheetDialog.isShowing) mBottomSheetDialog.dismiss()
                }
                btnUpdateNow.setOnClickListener {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Constant.PLAY_STORE_LINK + activity.packageName)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun addMarkers(
            currentPage: Int,
            imageList: ArrayList<Slider>,
            mMarkersLayout: LinearLayout,
            activity: Activity
        ) {
            if (activity != null) {
                val markers = arrayOfNulls<TextView>(imageList.size)
                mMarkersLayout.removeAllViews()
                for (i in markers.indices) {
                    markers[i] = TextView(activity)
                    markers[i]!!.text = Html.fromHtml("&#8226;")
                    markers[i]!!.textSize = 35f
                    markers[i]!!.setTextColor(ContextCompat.getColor(activity, R.color.gray))
                    mMarkersLayout.addView(markers[i])
                }
                if (markers.isNotEmpty()) markers[currentPage]!!
                    .setTextColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            }
        }

        @SuppressLint("SetTextI18n")
        fun buildCounterDrawable(count: Int, activity: Activity): Drawable {
            val inflater = LayoutInflater.from(activity)
            @SuppressLint("InflateParams") val view: View =
                inflater.inflate(R.layout.counter_menuitem_layout, null)
            val textView = view.findViewById<TextView>(R.id.count)
            val lytCount = view.findViewById<RelativeLayout>(R.id.lytCount)
            if (count == 0) {
                lytCount.visibility = View.GONE
            } else {
                lytCount.visibility = View.VISIBLE
                textView.text = "" + count
            }
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            view.isDrawingCacheEnabled = true
            view.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
            val bitmap = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false
            return BitmapDrawable(activity.resources, bitmap)
        }

        fun getOfferImage(
            activity: Activity,
            jsonArray: JSONArray?,
            lytTopOfferImages: RecyclerView
        ) {
            if (jsonArray != null) {
                val images = ArrayList<HomeOffer>()
                for (i in 0 until jsonArray.length()) {
                    try {
                        images.add(Gson().fromJson(jsonArray.getJSONObject(i).toString(),HomeOffer::class.java))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                if (images.size > 0) {
                    lytTopOfferImages.adapter = OfferAdapter(activity, images)
                }
            }
        }

        @SuppressLint("DefaultLocale")
        fun stringFormat(number: String): String {
            return String.format("%.2f", number.toDouble())
        }

        fun copyToClipboard(activity: Activity, title: String, textToCopy: String?) {
            val clipboard: ClipboardManager =
                activity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(title, textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(
                activity,
                title + activity.getString(R.string.copied),
                Toast.LENGTH_SHORT
            )
                .show()
        }

        fun getMonth(monthNo: Int, activity: Activity): String {
            var month = ""
            val month1 = arrayOf(
                activity.getString(R.string.jan),
                activity.getString(R.string.feb),
                activity.getString(R.string.mar),
                activity.getString(R.string.apr),
                activity.getString(R.string.may),
                activity.getString(R.string.jun),
                activity.getString(R.string.jul),
                activity.getString(R.string.aug),
                activity.getString(R.string.sep),
                activity.getString(R.string.oct),
                activity.getString(R.string.nov),
                activity.getString(R.string.dec)
            )
            if (monthNo != 0) {
                month = month1[monthNo - 1]
            }
            return month
        }

        fun getDayOfWeek(dayNo: Int, activity: Activity): String {
            var day = ""
            val day1 = arrayOf(
                activity.getString(R.string.sun),
                activity.getString(R.string.mon),
                activity.getString(R.string.tue),
                activity.getString(R.string.wed),
                activity.getString(R.string.thu),
                activity.getString(R.string.fri),
                activity.getString(R.string.sat)
            )
            if (dayNo != 0) {
                day = day1[dayNo - 1]
            }
            return day
        }

        fun getDates(startDate: String?, endDate: String?): ArrayList<String> {
            val dates = ArrayList<String>()
            @SuppressLint("SimpleDateFormat") val df1: DateFormat = SimpleDateFormat("dd-MM-yyyy")
            var date1: Date? = null
            var date2: Date? = null
            try {
                date1 = df1.parse(startDate)
                date2 = df1.parse(endDate)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            val cal1 = Calendar.getInstance()
            assert(date1 != null)
            cal1.time = date1
            val cal2 = Calendar.getInstance()
            assert(date2 != null)
            cal2.time = date2
            while (!cal1.after(cal2)) {
                dates.add(cal1[Calendar.DATE].toString() + "-" + (cal1[Calendar.MONTH] + 1) + "-" + cal1[Calendar.YEAR] + "-" + cal1[Calendar.DAY_OF_WEEK])
                cal1.add(Calendar.DATE, 1)
            }
            return dates
        }

        fun toTitleCase(str: String?): String? {
            if (str == null) {
                return null
            }
            var space = true
            val builder = StringBuilder(str)
            val len = builder.length
            for (i in 0 until len) {
                val c = builder[i]
                if (space) {
                    if (!Character.isWhitespace(c)) {
                        // Convert to title case and switch out of whitespace mode.
                        builder.setCharAt(i, Character.toTitleCase(c))
                        space = false
                    }
                } else if (Character.isWhitespace(c)) {
                    space = true
                } else {
                    builder.setCharAt(i, Character.toLowerCase(c))
                }
            }
            return builder.toString()
        }

        fun checkValidation(
            item: String,
            isMailValidation: Boolean,
            isMobileValidation: Boolean
        ): Boolean {
            var result = false
            if (item.isEmpty()) {
                result = true
            } else if (isMailValidation) {
                if (!Patterns.EMAIL_ADDRESS.matcher(item).matches()) {
                    result = true
                }
            } else if (isMobileValidation) {
                if (!Patterns.PHONE.matcher(item).matches()) {
                    result = true
                }
            }
            return result
        }

        @SuppressLint("DefaultLocale")
        fun getDiscount(OriginalPrice: Double, discountedPrice: Double): String {
            return String.format(
                "%.0f",
                ("" + ((OriginalPrice - discountedPrice + OriginalPrice) / OriginalPrice - 1) * 100).toDouble()
            ) + "%"
        }

        @Deprecated("")
        fun displayLocationSettingsRequest(activity: Activity) {
            val googleApiClient =
                GoogleApiClient.Builder(activity).addApi(LocationServices.API).build()
            googleApiClient.connect()
            val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 10000
            locationRequest.fastestInterval = (10000 / 2).toLong()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            builder.setAlwaysShow(true)
            val result: PendingResult<LocationSettingsResult> =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())
            result.setResultCallback { result ->
                val status: Status = result.status
                when (status.statusCode) {
                    LocationSettingsStatusCodes.SUCCESS -> {
                    }
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        status.startResolutionForResult(activity, 110)
                    } catch (e: SendIntentException) {
                        Log.i("TAG", "PendingIntent unable to execute request.")
                    }
                }
            }
        }

        @Synchronized
        fun getInstance(): ApiConfig {
            return mInstance
        }

        fun getRequestQueue(): RequestQueue {
            return mRequestQueue
        }

        fun <T> addToRequestQueue(req: Request<T>) {
            req.tag = tag
            getRequestQueue().add(req)
        }

        fun getShippingType(activity: Activity, session: Session) {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_SHIPPING_TYPE] = Constant.GetVal
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                session.setData(
                                    Constant.SHIPPING_TYPE,
                                    jsonObject.getString(Constant.SHIPPING_TYPE)
                                )
                            } else {
                                Toast.makeText(
                                    activity,
                                    jsonObject.getString(Constant.MESSAGE),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: JSONException) {
                            Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.SETTING_URL, params, false)
        }

        @SuppressLint("SetTextI18n")
        fun getWalletBalance(activity: Activity, session: Session) {
            try {
                val params: MutableMap<String, String> = HashMap()
                params[Constant.GET_USER_DATA] = Constant.GetVal
                params[Constant.USER_ID] = session.getData(Constant.ID).toString()

                requestToVolley(object : VolleyCallback {
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
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, activity, Constant.USER_DATA_URL, params, false)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("SetTextI18n")
        fun getCartItemCount(activity: Activity, session: Session) {
            try {
                val params: MutableMap<String, String> = HashMap()
                params[Constant.GET_USER_CART] = Constant.GetVal
                params[Constant.USER_ID] = session.getData(Constant.ID).toString()
                requestToVolley(object : VolleyCallback {
                    override fun onSuccess(result: Boolean, response: String) {
                        if (result) {
                            try {
                                val jsonObject = JSONObject(response)
                                if (!jsonObject.getBoolean(Constant.ERROR)) {
                                    Constant.TOTAL_CART_ITEM =
                                        jsonObject.getString(Constant.TOTAL).toInt()
                                } else {
                                    Constant.TOTAL_CART_ITEM = 0
                                }
                                Constant.CartValues.clear()
                                activity.invalidateOptionsMenu()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, activity, Constant.CART_URL, params, false)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun addOrRemoveFavorite(
            activity: Activity,
            session: Session,
            productID: String,
            isAdd: Boolean
        ) {
            val params: MutableMap<String, String> = HashMap()
            if (isAdd) {
                params[Constant.ADD_TO_FAVORITES] = Constant.GetVal
            } else {
                params[Constant.REMOVE_FROM_FAVORITES] = Constant.GetVal
            }
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
            params[Constant.PRODUCT_ID] = productID
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {}
            }, activity, Constant.GET_FAVORITES_URL, params, false)
        }

        fun getSettings(activity: Activity) {
            val session = Session(activity)
            val params: MutableMap<String, String> = HashMap()
            params[Constant.SETTINGS] = Constant.GetVal
            params[Constant.GET_TIMEZONE] = Constant.GetVal

            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            val jsonObject1 = jsonObject.getJSONObject(Constant.SETTINGS)
                            session.setData(
                                Constant.minimum_version_required,
                                jsonObject1.getString(Constant.minimum_version_required)
                            )
                            session.setData(
                                Constant.is_version_system_on,
                                jsonObject1.getString(Constant.is_version_system_on)
                            )
                            session.setData(
                                Constant.CURRENCY,
                                jsonObject1.getString(Constant.CURRENCY)
                            )
                            session.setData(
                                Constant.min_order_amount,
                                jsonObject1.getString(Constant.min_order_amount)
                            )
                            session.setData(
                                Constant.max_cart_items_count,
                                jsonObject1.getString(Constant.max_cart_items_count)
                            )
                            session.setData(
                                Constant.area_wise_delivery_charge,
                                jsonObject1.getString(Constant.area_wise_delivery_charge)
                            )
                            session.setData(
                                Constant.is_refer_earn_on,
                                jsonObject1.getString(Constant.is_refer_earn_on)
                            )
                            session.setData(
                                Constant.refer_earn_bonus,
                                jsonObject1.getString(Constant.refer_earn_bonus)
                            )
                            session.setData(
                                Constant.refer_earn_bonus,
                                jsonObject1.getString(Constant.refer_earn_bonus)
                            )
                            session.setData(
                                Constant.refer_earn_method,
                                jsonObject1.getString(Constant.refer_earn_method)
                            )
                            session.setData(
                                Constant.max_refer_earn_amount,
                                jsonObject1.getString(Constant.max_refer_earn_amount)
                            )
                            session.setData(
                                Constant.max_product_return_days,
                                jsonObject1.getString(Constant.max_product_return_days)
                            )
                            session.setData(
                                Constant.user_wallet_refill_limit,
                                jsonObject1.getString(Constant.user_wallet_refill_limit)
                            )
                            session.setData(
                                Constant.min_refer_earn_order_amount,
                                jsonObject1.getString(Constant.min_refer_earn_order_amount)
                            )
                            if (!session.getBoolean("update_skip")) {
                                var versionName = ""
                                try {
                                    val packageInfo = activity.packageManager.getPackageInfo(
                                        activity.packageName,
                                        0
                                    )
                                    versionName = packageInfo.versionName
                                } catch (ignore: NameNotFoundException) {
                                }
                                if (checkForUpdate(
                                        versionName,
                                        session.getData(Constant.minimum_version_required)!!
                                    )
                                ) {
                                    openBottomDialog(activity)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, activity, Constant.SETTING_URL, params, false)
        }

        fun checkForUpdate(existingVersion: String, newVersion: String): Boolean {
            var existingVersion = existingVersion
            var newVersion = newVersion
            if (existingVersion.isEmpty() || newVersion.isEmpty()) {
                return false
            }
            existingVersion = existingVersion.replace("\\.".toRegex(), "")
            newVersion = newVersion.replace("\\.".toRegex(), "")
            val existingVersionLength = existingVersion.length
            val newVersionLength = newVersion.length
            val versionBuilder = StringBuilder()
            if (newVersionLength > existingVersionLength) {
                versionBuilder.append(existingVersion)
                for (i in existingVersionLength until newVersionLength) {
                    versionBuilder.append("0")
                }
                existingVersion = versionBuilder.toString()
            } else if (existingVersionLength > newVersionLength) {
                versionBuilder.append(newVersion)
                for (i in newVersionLength until existingVersionLength) {
                    versionBuilder.append("0")
                }
                newVersion = versionBuilder.toString()
            }
            return newVersion.toInt() > existingVersion.toInt()
        }

        fun addMultipleProductInSaveForLater(
            session: Session,
            activity: Activity,
            map: MutableMap<String, String>
        ) {
            if (map.isNotEmpty()) {
                val ids = map.keys.toString().replace("[", "").replace("]", "").replace(" ", "")
                val qty = map.values.toString().replace("[", "").replace("]", "").replace(" ", "")
                val params: MutableMap<String, String> = HashMap()
                params[Constant.SAVE_FOR_LATER_ITEMS] = Constant.GetVal
                params[Constant.USER_ID] = session.getData(Constant.ID).toString()
                params[Constant.PRODUCT_VARIANT_ID] = ids
                params[Constant.QTY] = qty
                requestToVolley(object : VolleyCallback {
                    override fun onSuccess(result: Boolean, response: String) {
                        if (result) {
                            getCartItemCount(activity, session)
                        }
                    }
                }, activity, Constant.CART_URL, params, false)
            }
        }

        fun addMultipleProductInCart(
            session: Session,
            activity: Activity,
            map: MutableMap<String, String>
        ) {
            if (map.isNotEmpty()) {
                val ids = map.keys.toString().replace("[", "").replace("]", "").replace(" ", "")
                val qty = map.values.toString().replace("[", "").replace("]", "").replace(" ", "")

                val params: MutableMap<String, String> = HashMap()
                params[Constant.ADD_MULTIPLE_ITEMS] = Constant.GetVal
                params[Constant.USER_ID] = session.getData(Constant.ID).toString()
                params[Constant.PRODUCT_VARIANT_ID] = ids
                params[Constant.QTY] = qty

                requestToVolley(object : VolleyCallback {
                    override fun onSuccess(result: Boolean, response: String) {
                        if (result) {
                            getCartItemCount(activity, session)
                        }
                    }
                }, activity, Constant.CART_URL, params, false)
            }
        }

        fun removeAddress(activity: Activity, addressId: String) {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.DELETE_ADDRESS] = Constant.GetVal
            params[Constant.ID] = addressId
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {}
            }, activity, Constant.GET_ADDRESS_URL, params, false)
        }


        fun getProductNames(activity: Activity, session: Session) {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_ALL_PRODUCTS_NAME] = Constant.GetVal
            if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(
                    Constant.GET_SELECTED_PINCODE
                ) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0"
            ) {
                params[Constant.PINCODE_ID] =
                    session.getData(Constant.GET_SELECTED_PINCODE_ID).toString()
            }
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                        try {
                            val jsonObject = JSONObject(response)
                            if (!jsonObject.getBoolean(Constant.ERROR)) {
                                session.setData(
                                    Constant.GET_ALL_PRODUCTS_NAME,
                                    jsonObject.getString(Constant.DATA)
                                )
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            }, activity, Constant.GET_PRODUCTS_URL, params, false)
        }
    }
}