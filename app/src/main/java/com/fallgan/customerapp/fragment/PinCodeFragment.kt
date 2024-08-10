package com.fallgan.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.activity.MainActivity
import com.fallgan.customerapp.adapter.PinCodeAdapter
import com.fallgan.customerapp.databinding.FragmentPincodeBinding
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.PinCode

class PinCodeFragment : DialogFragment() {
    lateinit var binding: FragmentPincodeBinding
    lateinit var root: View
    private lateinit var pinCodes: ArrayList<PinCode?>
    private lateinit var pinCodeAdapter: PinCodeAdapter
    lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var activity: Activity
    lateinit var session: Session
    lateinit var from: String

    var offset = 0
    var isLoadMore = false
    var total = 0
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_pincode, container, false)
        binding = FragmentPincodeBinding.inflate(inflater,container,false)
        activity = requireActivity()
        session = Session(activity)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        assert(arguments != null)
        from = requireArguments().getString(Constant.FROM).toString()

        linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager
        binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.ic_search,
            0,
            R.drawable.ic_close_,
            0
        )
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (binding.searchView.text.toString().trim().isNotEmpty()) {
                    binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close,
                        0
                    )
                } else {
                    binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close_,
                        0
                    )
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        if (from == "home") {
            binding.tvPinCode.visibility = View.VISIBLE
        } else {
            binding.tvPinCode.visibility = View.GONE
        }
        binding.tvPinCode.setOnClickListener {
            session.setBoolean(Constant.GET_SELECTED_PINCODE, true)
            session.setData(Constant.GET_SELECTED_PINCODE_ID, "0")
            session.setData(
                Constant.GET_SELECTED_PINCODE_NAME,
                activity.getString(R.string.all)
            )
            if (from == "home") {
                HomeFragment.tvLocation.text = activity.getString(R.string.all)
                HomeFragment.refreshListener.onRefresh()
            } else {
                CartFragment.tvLocation.text = activity.getString(R.string.all)
                CartFragment.refreshListener.onRefresh()
            }
            MainActivity.pinCodeFragment.dismiss()
        }
        binding.tvSearch.setOnClickListener {
            getData(
                    binding.searchView.text.toString().trim()
            )
        }
        binding.searchView.setOnTouchListener(View.OnTouchListener setOnTouchListener@{ _, event: MotionEvent ->
            val drawableRight = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (binding.searchView.text.toString().trim().isNotEmpty()) {
                    if (event.rawX >= binding.searchView.right - binding.searchView.compoundDrawables[drawableRight].bounds.width()) {
                        binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_search,
                            0,
                            R.drawable.ic_close_,
                            0
                        )
                        binding.searchView.setText("")
                        getData("")
                    }
                    return@setOnTouchListener true
                }
            }
            false
        })
        getData("")
        return binding.root
    }

    fun getData(search: String) {
        pinCodes = ArrayList()
        binding.progressBar.visibility = View.VISIBLE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_PIN_CODES] = Constant.GetVal
        params[Constant.SEARCH] = search
        params[Constant.OFFSET] = "" + offset
        params[Constant.LIMIT] = "" + (Constant.LOAD_ITEM_LIMIT + 20)
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        try {
                            total = jsonObject.getString(Constant.TOTAL).toInt()
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                val pinCode =
                                    Gson().fromJson(jsonObject1.toString(), PinCode::class.java)
                                pinCodes.add(pinCode)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (offset == 0) {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.tvAlert.visibility = View.GONE
                            pinCodeAdapter = PinCodeAdapter(activity, pinCodes, from)
                            pinCodeAdapter.setHasStableIds(true)
                            binding.recyclerView.adapter = pinCodeAdapter
                            binding.scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                // if (diff == 0) {
                                if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                    if (pinCodes.size < total) {
                                        if (!isLoadMore) {
                                            if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == pinCodes.size - 1) {
                                                //bottom of list!

                                                offset += Constant.LOAD_ITEM_LIMIT + 20
                                                val params1: MutableMap<String, String> = HashMap()
                                                params1[Constant.GET_PIN_CODES] = Constant.GetVal
                                                params1[Constant.SEARCH] = search
                                                params1[Constant.OFFSET] = "" + offset
                                                params1[Constant.LIMIT] =
                                                    "" + (Constant.LOAD_ITEM_LIMIT + 20)
                                                ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                                                            try {
                                                                val jsonObject1 =
                                                                    JSONObject(response)
                                                                if (!jsonObject1.getBoolean(Constant.ERROR)) {

                                                                    val `object` =
                                                                        JSONObject(response)
                                                                    val jsonArray =
                                                                        `object`.getJSONArray(
                                                                            Constant.DATA
                                                                        )

                                                                    for (i in 0 until jsonArray.length()) {
                                                                        val jsonObject2 =
                                                                            jsonArray.getJSONObject(
                                                                                i
                                                                            )
                                                                        val pinCode =
                                                                            Gson().fromJson(
                                                                                jsonObject2.toString(),
                                                                                PinCode::class.java
                                                                            )
                                                                        pinCodes.add(pinCode)
                                                                    }
                                                                    pinCodeAdapter.notifyDataSetChanged()
                                                                    pinCodeAdapter.setLoaded()
                                                                    isLoadMore = false
                                                                }
                                                            } catch (e: JSONException) {
                                                                e.printStackTrace()
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }},
                                                    activity,
                                                    Constant.GET_LOCATIONS_URL,
                                                    params1,
                                                    false
                                                )
                                            }
                                            isLoadMore = true
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.recyclerView.visibility = View.GONE
                        binding.tvAlert.visibility = View.VISIBLE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.progressBar.visibility = View.GONE
                    e.printStackTrace()
                }
            }
        }
        }, activity, Constant.GET_LOCATIONS_URL, params, false)
    }
}