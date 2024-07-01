package com.gpn.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.adapter.TransactionAdapter
import com.gpn.customerapp.databinding.FragmentTransectionBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Transaction

class TransactionFragment : Fragment() {
    lateinit var binding:FragmentTransectionBinding
    lateinit var root: View
    private lateinit var transactions: ArrayList<Transaction?>
    private lateinit var transactionAdapter: TransactionAdapter
    lateinit var activity: Activity
    lateinit var session: Session
    var total = 0
    var offset = 0
    var isLoadMore = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_transection, container, false)
        binding=FragmentTransectionBinding.inflate(inflater,container,false)
        offset = 0
        activity = requireActivity()
        session = Session(activity)
        setHasOptionsMenu(true)

        binding.tvAlertTitle.text = getString(R.string.no_transaction_history_found)
        binding.tvAlertSubTitle.text = getString(R.string.you_have_not_any_transactional_history_yet)
        transactionData
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            offset = 0
            transactionData
        }
        return binding.root
    }

    private val transactionData: Unit
        @SuppressLint("NotifyDataSetChanged")
        get() {
            binding.recyclerView.visibility = View.GONE
            binding.shimmerFrameLayout.visibility = View.VISIBLE
            binding.shimmerFrameLayout.startShimmer()
            transactions = ArrayList()
            val linearLayoutManager = LinearLayoutManager(activity)
            binding.recyclerView.layoutManager = linearLayoutManager
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_USER_TRANSACTION] = Constant.GetVal
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
            params[Constant.TYPE] = Constant.TYPE_TRANSACTION
            params[Constant.OFFSET] = "" + offset
            params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
            ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL).toInt()
                            session.setData(Constant.TOTAL, total.toString())
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject1 = jsonArray.getJSONObject(i)
                                if (jsonObject1 != null) {
                                    val transaction =
                                        Gson().fromJson(jsonObject1.toString(), Transaction::class.java)
                                    transactions.add(transaction)
                                } else {
                                    break
                                }
                            }
                            if (offset == 0) {
                                transactionAdapter = TransactionAdapter(activity, transactions)
                                transactionAdapter.setHasStableIds(true)
                                binding.recyclerView.adapter = transactionAdapter
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                        val linearLayoutManager1 =
                                                binding.recyclerView.layoutManager as LinearLayoutManager?
                                        if (transactions.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager1 != null && linearLayoutManager1.findLastCompletelyVisibleItemPosition() == transactions.size - 1) {
                                                    //bottom of list!

                                                    Handler().postDelayed({
                                                        offset += Constant.LOAD_ITEM_LIMIT
                                                        val params1: MutableMap<String, String> =
                                                            HashMap()
                                                        params1[Constant.GET_USER_TRANSACTION] =
                                                            Constant.GetVal
                                                        params1[Constant.USER_ID] =
                                                            session.getData(
                                                                Constant.ID
                                                            ).toString()
                                                        params1[Constant.TYPE] =
                                                            Constant.TYPE_TRANSACTION
                                                        params1[Constant.OFFSET] = "" + offset
                                                        params1[Constant.LIMIT] =
                                                            "" + Constant.LOAD_ITEM_LIMIT

                                                        ApiConfig.requestToVolley(object : VolleyCallback {
                                                            override fun onSuccess(result: Boolean, response: String) {
                                                                if (result) {
                                                                    try {

                                                                        val jsonObject1 =
                                                                            JSONObject(response)
                                                                        if (!jsonObject1.getBoolean(
                                                                                Constant.ERROR
                                                                            )
                                                                        ) {
                                                                            session.setData(
                                                                                Constant.TOTAL,
                                                                                jsonObject1.getString(
                                                                                    Constant.TOTAL
                                                                                )
                                                                            )

                                                                            val object1 =
                                                                                JSONObject(response)
                                                                            val jsonArray1 =
                                                                                object1.getJSONArray(
                                                                                    Constant.DATA
                                                                                )
                                                                            val g1 = Gson()
                                                                            for (i in 0 until jsonArray1.length()) {
                                                                                val jsonObject2 =
                                                                                    jsonArray1.getJSONObject(
                                                                                        i
                                                                                    )
                                                                                if (jsonObject2 != null) {
                                                                                    val transaction =
                                                                                        g1.fromJson(
                                                                                            jsonObject2.toString(),
                                                                                            Transaction::class.java
                                                                                        )
                                                                                    transactions.add(
                                                                                        transaction
                                                                                    )
                                                                                } else {
                                                                                    break
                                                                                }
                                                                            }
                                                                            transactionAdapter.notifyDataSetChanged()
                                                                            transactionAdapter.setLoaded()
                                                                            isLoadMore = false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                        binding.shimmerFrameLayout.stopShimmer()
                                                                        binding.shimmerFrameLayout.visibility =
                                                                            View.GONE
                                                                        binding.recyclerView.visibility =
                                                                            View.VISIBLE
                                                                    }
                                                                }
                                                            }},
                                                            activity,
                                                            Constant.TRANSACTION_URL,
                                                            params1,
                                                            false
                                                        )
                                                    }, 0)
                                                    isLoadMore = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.tvAlert.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.TRANSACTION_URL, params, false)
        }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.transaction_history)
        activity.invalidateOptionsMenu()
        Session.setCount(Constant.UNREAD_TRANSACTION_COUNT, 0, activity)
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
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
    }
}