package com.fallgan.customerapp.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.adapter.OrderItemsAdapter
import com.fallgan.customerapp.adapter.ProductImagesAdapter
import com.fallgan.customerapp.adapter.SelectedImagesAdapter
import com.fallgan.customerapp.com.coursion.freakycoder.mediapicker.galleries.Gallery
import com.fallgan.customerapp.databinding.FragmentOrderDetailBinding
import com.fallgan.customerapp.helper.ApiConfig.Companion.requestToVolley
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.OrderTracker
import java.io.File
import java.util.*

@SuppressLint("NotifyDataSetChanged")
class OrderDetailFragment : Fragment() {
    lateinit var binding: FragmentOrderDetailBinding
    private val openMediaPicker = 1  // Request code
    private val permissionReadExternalStorage = 100       // Request code for read external storage

    lateinit var root: View
    lateinit var order: OrderTracker
    lateinit var activity: Activity
    lateinit var id: String
    lateinit var session: Session
    lateinit var hashMap: MutableMap<String, String>
    private lateinit var receiptImages: ArrayList<String>
    private lateinit var productImagesAdapter: ProductImagesAdapter

    private var totalAfterTax = 0.0
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_order_detail, container, false)

        binding = FragmentOrderDetailBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)

        binding.recyclerViewOrderTracker.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewOrderTracker.isNestedScrollingEnabled = false
        binding.recyclerViewOrderTracker.layoutManager = LinearLayoutManager(activity)


        val animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
        animHide.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                binding.lytMain.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        binding.imgTrackerClose.setOnClickListener {
            binding.lytTrackerTimeLine.visibility = View.GONE
            binding.lytTrackerTimeLine.startAnimation(animHide)
        }
        hashMap = HashMap()
        id = requireArguments().getString(Constant.ID).toString()
        if (id == "") {
            order = (requireArguments().getSerializable("model") as OrderTracker?)!!
            id = order.id
            setData(order)
        } else {
            getOrderDetails(id)
        }
        setHasOptionsMenu(true)
        binding.btnReorder.setOnClickListener {
            AlertDialog.Builder(requireActivity())
                .setTitle(getString(R.string.re_order))
                .setMessage(getString(R.string.reorder_msg))
                .setPositiveButton(getString(R.string.proceed)) { dialog: DialogInterface, which: Int ->
                    if (activity != null) {
                        getReOrderData()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
                .show()
        }
        receiptImages = ArrayList()

        binding.recyclerViewImageGallery.layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.recyclerViewReceiptImages.layoutManager = GridLayoutManager(activity, 3)
        binding.recyclerViewReceiptImages.isNestedScrollingEnabled = false
        binding.tvBankDetail.setOnClickListener {
            if (Constant.ACCOUNT_NAME.isEmpty() || Constant.ACCOUNT_NUMBER.isEmpty() || Constant.BANK_NAME.isEmpty() || Constant.BANK_CODE.isEmpty()) {
                paymentConfig
            } else {
                openBankDetails()
            }
        }
        binding.btnOtherImages.setOnClickListener {
            binding.recyclerView.visibility = View.VISIBLE
            if (!permissionIfNeeded()) {
                val intent = Intent(activity, Gallery::class.java)
                // Set the title
                intent.putExtra("title", getString(R.string.select_media))
                // Mode 1 for both images and videos selection, 2 for images only and 3 for videos!
                intent.putExtra("mode", 2)
                intent.putExtra("maxSelection", true) // Optional
                intent.putExtra("tabBarHidden", false) //Optional - default value is false
                startActivityForResult(intent, 1)
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (receiptImages.size > 0) {
                binding.progressBar.visibility = View.VISIBLE
                submitReceipt()
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.no_receipt_select_message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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

            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), permissionReadExternalStorage)
            return true
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == openMediaPicker) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK && data != null) {
                val selectionResult = data.getStringArrayListExtra("result")!!
                for (path in selectionResult) {
                    receiptImages.add(path)
                }
                binding.recyclerViewReceiptImages.adapter = SelectedImagesAdapter(activity, receiptImages)
            }
        }
    }

    private val paymentConfig: Unit
        get() {
            val params: MutableMap<String, String> = HashMap()
            params[Constant.SETTINGS] = Constant.GetVal
            params[Constant.GET_PAYMENT_METHOD] = Constant.GetVal
            requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (jsonObject.has(Constant.PAYMENT_METHODS)) {
                                val `object` = jsonObject.getJSONObject(Constant.PAYMENT_METHODS)
                                if (`object`.has(Constant.direct_bank_transfer_method)) {
                                    Constant.DIRECT_BANK_TRANSFER =
                                        `object`.getString(Constant.direct_bank_transfer_method)
                                    Constant.ACCOUNT_NAME =
                                        `object`.getString(Constant.account_name)
                                    Constant.ACCOUNT_NUMBER =
                                        `object`.getString(Constant.account_number)
                                    Constant.BANK_NAME = `object`.getString(Constant.bank_name)
                                    Constant.BANK_CODE = `object`.getString(Constant.bank_code)
                                    Constant.NOTES = `object`.getString(Constant.notes)
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.SETTING_URL, params, false)
        }

    private fun openBankDetails() {
        run {
            @SuppressLint("InflateParams") val sheetView =
                activity.layoutInflater.inflate(R.layout.dialog_bank_detail, null)
            val mBottomSheetDialog = Dialog(activity)
            mBottomSheetDialog.setContentView(sheetView)
            mBottomSheetDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mBottomSheetDialog.show()
            val tvAccountName = sheetView.findViewById<TextView>(R.id.tvAccountName)
            val tvAccountNumber = sheetView.findViewById<TextView>(R.id.tvAccountNumber)
            val tvBankName = sheetView.findViewById<TextView>(R.id.tvBankName)
            val tvIFSCCode = sheetView.findViewById<TextView>(R.id.tvIFSCCode)
            val tvExtraNote = sheetView.findViewById<TextView>(R.id.tvExtraNote)
            tvAccountName.text = Constant.ACCOUNT_NAME
            tvAccountNumber.text = Constant.ACCOUNT_NUMBER
            tvBankName.text = Constant.BANK_NAME
            tvIFSCCode.text = Constant.BANK_CODE
            tvExtraNote.text = Constant.NOTES
            tvAccountName.setOnClickListener { 
                ApiConfig.copyToClipboard(
                    activity,
                    activity.getString(R.string.bank_account_name_).replace(":", ""),
                    tvAccountName.text.toString()
                )
            }
            tvAccountNumber.setOnClickListener { 
                ApiConfig.copyToClipboard(
                    activity,
                    activity.getString(R.string.bank_account_number_),
                    tvAccountNumber.text.toString()
                )
            }
            tvBankName.setOnClickListener { 
                ApiConfig.copyToClipboard(
                    activity,
                    activity.getString(R.string.bank_name_),
                    tvBankName.text.toString()
                )
            }
            tvIFSCCode.setOnClickListener { 
                ApiConfig.copyToClipboard(
                    activity,
                    activity.getString(R.string.bank_ifsc_code_),
                    tvIFSCCode.text.toString()
                )
            }
            tvExtraNote.setOnClickListener { 
                ApiConfig.copyToClipboard(
                    activity,
                    activity.getString(R.string.extra_note_),
                    tvExtraNote.text.toString()
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitReceipt() {
        try {
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val client = OkHttpClient().newBuilder().build()
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            builder.addFormDataPart(Constant.AccessKey, Constant.AccessKeyVal)
            builder.addFormDataPart(Constant.UPLOAD_BANK_TRANSFER_ATTACHMENT, Constant.GetVal)
            builder.addFormDataPart(Constant.ORDER_ID, order.id)
            for (i in receiptImages.indices) {
                val file = File(receiptImages[i])
                builder.addFormDataPart(
                    Constant.IMAGES,
                    file.name,
                    RequestBody.create(MediaType.get("application/octet-stream"), file)
                )
            }
            val body: RequestBody = builder.build()
            val request = Request.Builder()
                .url(Constant.ORDER_PROCESS_URL)
                .method("POST", body)
                .addHeader(
                    Constant.AUTHORIZATION,
                    "Bearer " + ApiConfig.createJWT("eKart", "eKart Authentication")
                )
                .build()
            val response = client.newCall(request).execute()
            Toast.makeText(
                activity, JSONObject(
                    response.body()!!.string()
                ).getString(Constant.MESSAGE), Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
            binding.progressBar.visibility = View.GONE
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getReOrderData() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_REORDER_DATA] = Constant.GetVal
        params[Constant.ID] = id
        requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    val jsonArray =
                        jsonObject.getJSONObject(Constant.DATA).getJSONArray(Constant.ITEMS)
                    for (i in 0 until jsonArray.length()) {
                        hashMap[jsonArray.getJSONObject(i)
                            .getString(Constant.PRODUCT_VARIANT_ID)] =
                            jsonArray.getJSONObject(i).getString(
                                Constant.QUANTITY
                            )
                    }
                    ApiConfig.addMultipleProductInCart(session, activity, hashMap)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        }, activity, Constant.ORDER_PROCESS_URL, params, false)
    }

    private fun getOrderDetails(id: String?) {
        binding.scrollView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ORDERS] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.ORDER_ID] = id.toString()

        requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject1 = JSONObject(response)
                    if (!jsonObject1.getBoolean(Constant.ERROR)) {
                        setData(
                            Gson().fromJson(
                                jsonObject1.getJSONArray(Constant.DATA).getJSONObject(0).toString(),
                                OrderTracker::class.java
                            )
                        )
                    } else {
                        binding.scrollView.visibility = View.VISIBLE
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.shimmerFrameLayout.stopShimmer()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.scrollView.visibility = View.VISIBLE
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.shimmerFrameLayout.stopShimmer()
                }
            }
        }
        }, activity, Constant.ORDER_PROCESS_URL, params, false)
    }

    @SuppressLint("SetTextI18n")
    fun setData(order: OrderTracker?) {
        try {
            val date = order!!.date_added.split("\\s+").toTypedArray()
            binding.tvOrderId.text = order.id
            if (order.otp == "0") {
                binding.lytOTP.visibility = View.GONE
            } else {
                binding.tvOrderOTP.text = order.otp
            }
            if (order.order_note.isNotEmpty()) {
                binding.lytOrderNote.visibility = View.VISIBLE
                binding.tvOrderNote.text = order.order_note
            } else {
                binding.lytOrderNote.visibility = View.GONE
            }
            binding.btnInvoice.setOnClickListener {
                val fragment: Fragment = WebViewFragment()
                val bundle = Bundle()
                bundle.putString("type", activity.getString(R.string.order) + "#" + order.id)
                fragment.arguments = bundle
                MainActivity.fm.beginTransaction().add(R.id.container, fragment)
                    .addToBackStack(null).commit()
            }
            binding.tvReceiptStatus.text = when {
                order.bank_transfer_status.equals(
                    "0",
                    ignoreCase = true
                ) -> getString(R.string.pending)
                order.bank_transfer_status.equals(
                    "1",
                    ignoreCase = true
                ) -> getString(R.string.accepted)
                else -> getString(R.string.rejected)
            }
            if (order.bank_transfer_status.equals("2", ignoreCase = true)) {
                binding.tvReceiptStatusReason.visibility = View.VISIBLE
                binding.tvReceiptStatusReason.text = order.bank_transfer_message
            }
            productImagesAdapter = ProductImagesAdapter(activity, order.attachment, "api", order.id)
            binding.recyclerViewImageGallery.adapter = productImagesAdapter
            if (order.payment_method.equals("bank_transfer", ignoreCase = true)) {
                binding.lytReceipt.visibility = View.VISIBLE
                paymentConfig
            }
            binding.tvOrderDate.text = Html.fromHtml(
                activity.getString(R.string.ordered_on) + "<b>" + date[0] + "</b> " + activity.getString(
                    R.string.via
                ) + "<b>" + getPayMethod(
                    order.payment_method
                ) + "</b>", 0
            )
            binding.tvOtherDetail.text =
                getString(R.string.name_1) + order.user_name + getString(R.string.mobile_no_1) + order.mobile + getString(
                    R.string.address_1
                ) + order.address
            totalAfterTax = order.total.toDouble() + order.delivery_charge.toDouble()
            binding.tvItemTotal.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                order.total
            )
            binding.tvDeliveryCharge.text =
                "+ " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    order.delivery_charge
                )
            binding.tvDPercent.text = getString(R.string.discount) + "(" + order.discount + "%) :"
            binding.tvDAmount.text = "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                order.discount_rupees
            )
            binding.tvTotal.text = session.getData(Constant.CURRENCY) + totalAfterTax
            binding.tvPCAmount.text =
                "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                    order.promo_discount
                )
            binding.tvWallet.text = "- " + session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                order.wallet_balance
            )
            binding.tvFinalTotal.text = session.getData(Constant.CURRENCY) + ApiConfig.stringFormat(
                order.final_total
            )
            binding.scrollView.visibility = View.VISIBLE
            binding.shimmerFrameLayout.visibility = View.GONE
            binding.shimmerFrameLayout.stopShimmer()
            binding.recyclerView.adapter = OrderItemsAdapter(
                activity,
                order.items,
                    binding.recyclerViewOrderTracker,
                    binding.lytMain,
                    binding.lytTrackerTimeLine
            )
            binding.relativeLyt.visibility = View.VISIBLE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPayMethod(payMethod: String): String {
        return when (payMethod.lowercase(Locale.getDefault())) {
            "wallet" -> activity.getString(R.string.wallet)
            "cod" -> activity.getString(R.string.cod)
            "stripe" -> activity.getString(R.string.stripe)
            "bank_transfer" -> activity.getString(R.string.bank_transfer)
            "paypal" -> activity.getString(R.string.paypal)
            "razorpay" -> activity.getString(R.string.razor_payment)
            "sslecommerz" -> activity.getString(R.string.sslecommerz)
            "paystack" -> activity.getString(R.string.paystack)
            "midtrans" -> activity.getString(R.string.midtrans)
            "flutterwave" -> activity.getString(R.string.flutterwave)
            "paytm" -> activity.getString(R.string.paytm)
            else -> ""
        }
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.order_track_detail)
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
    }
}