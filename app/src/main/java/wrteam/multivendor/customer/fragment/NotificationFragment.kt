package com.gpn.customerapp.fragment


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.adapter.NotificationAdapter
import com.gpn.customerapp.databinding.FragmentNotificationBinding
import com.gpn.customerapp.helper.ApiConfig.Companion.requestToVolley
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Notification

class NotificationFragment : Fragment() {
    lateinit var binding: FragmentNotificationBinding

    lateinit var root: View
    private lateinit var notifications: ArrayList<Notification?>
    lateinit var notificationAdapter: NotificationAdapter
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
        root = inflater.inflate(R.layout.fragment_notification, container, false)

        binding = FragmentNotificationBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)

        setHasOptionsMenu(true)
        notificationData()
        binding.swipeLayout.setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
        binding.swipeLayout.setOnRefreshListener {
            offset = 0
            notificationData()
            binding.swipeLayout.isRefreshing = false
        }
        return binding.root
    }//bottom of list!

    // if (diff == 0) {
    fun notificationData(){
            binding.recyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
            notifications = ArrayList()
            val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager
            val params: MutableMap<String, String> = HashMap()
            params[Constant.GET_NOTIFICATIONS] = Constant.GetVal
            params[Constant.OFFSET] = "" + offset
            params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT + 10
            requestToVolley(object : VolleyCallback {
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
                                        val notification =
                                            Gson().fromJson(
                                                jsonObject1.toString(),
                                                Notification::class.java
                                            )
                                        notifications.add(notification)
                                    } else {
                                        break
                                    }
                                }
                                if (offset == 0) {
                                    notificationAdapter =
                                        NotificationAdapter(activity, notifications)
                                    notificationAdapter.setHasStableIds(true)
                                    binding.recyclerView.adapter = notificationAdapter
                                    binding.shimmerFrameLayout.visibility = View.GONE
                                    binding.shimmerFrameLayout.stopShimmer()
                                    binding.recyclerView.visibility = View.VISIBLE
                                    binding.scrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                        // if (diff == 0) {
                                        if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                            if (notifications.size < total) {
                                                if (!isLoadMore) {
                                                    if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == notifications.size - 1) {
                                                        //bottom of list!

                                                        offset += Constant.LOAD_ITEM_LIMIT + 10
                                                        val params1: MutableMap<String, String> =
                                                            HashMap()
                                                        params1[Constant.GET_NOTIFICATIONS] =
                                                            Constant.GetVal
                                                        params1[Constant.OFFSET] = "" + offset
                                                        params1[Constant.LIMIT] =
                                                            "" + Constant.LOAD_ITEM_LIMIT + 10
                                                        requestToVolley(
                                                            object : VolleyCallback {
                                                                override fun onSuccess(
                                                                    result: Boolean,
                                                                    response: String
                                                                ) {
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
                                                                                    JSONObject(
                                                                                        response
                                                                                    )
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
                                                                                        val notification =
                                                                                            g1.fromJson(
                                                                                                jsonObject2.toString(),
                                                                                                Notification::class.java
                                                                                            )
                                                                                        notifications.add(
                                                                                            notification
                                                                                        )
                                                                                    } else {
                                                                                        break
                                                                                    }
                                                                                }
                                                                                notificationAdapter.notifyDataSetChanged()
                                                                                notificationAdapter.setLoaded()
                                                                                isLoadMore = false
                                                                            }
                                                                        } catch (e: JSONException) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            activity,
                                                            Constant.GET_SECTION_URL,
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
                                binding.tvAlert.visibility = View.VISIBLE
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.shimmerFrameLayout.stopShimmer()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.recyclerView.visibility = View.VISIBLE
                        }
                    }
                }
            }, activity, Constant.GET_SECTION_URL, params, false)
        }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.notifications)
        activity.invalidateOptionsMenu()
        Session.setCount(Constant.UNREAD_NOTIFICATION_COUNT, 0, activity)
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
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}