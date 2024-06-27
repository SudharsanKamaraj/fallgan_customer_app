package wrteam.multivendor.customer.fragment


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
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
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.adapter.OrderListAdapter
import wrteam.multivendor.customer.databinding.FragmentTrackOrderBinding
import wrteam.multivendor.customer.helper.ApiConfig.Companion.requestToVolley
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.helper.Session
import wrteam.multivendor.customer.helper.VolleyCallback
import wrteam.multivendor.customer.model.OrderTracker

class OrderListFragment : Fragment() {
    lateinit var binding: FragmentTrackOrderBinding
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var root: View
    lateinit var orderTrackerArrayList: ArrayList<OrderTracker?>
    lateinit var orderListAdapter: OrderListAdapter
    private var offset = 0
    private var total = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_track_order, container, false)

        binding = FragmentTrackOrderBinding.inflate(inflater,container,false)

        activity = requireActivity()
        session = Session(activity)

        setHasOptionsMenu(true)

        binding.swipeLayout.setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
        binding.swipeLayout.setOnRefreshListener {
            offset = 0
            binding.swipeLayout.isRefreshing = false
            getAllOrders()
        }
        getAllOrders()
        return binding.root
    }
    private fun getAllOrders() {
        binding.recyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        orderTrackerArrayList = ArrayList()
        val linearLayoutManager = LinearLayoutManager(activity)
        binding.recyclerView.layoutManager = linearLayoutManager
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ORDERS] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        params[Constant.OFFSET] = "" + offset
        params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
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
                                val orderTracker = Gson().fromJson(
                                    jsonArray.getJSONObject(i).toString(),
                                    OrderTracker::class.java
                                )
                                orderTrackerArrayList.add(orderTracker)
                            }
                            if (offset == 0) {
                                orderListAdapter = OrderListAdapter(activity, orderTrackerArrayList)
                                binding.recyclerView.adapter = orderListAdapter
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.scrollView.setOnScrollChangeListener(object :
                                    NestedScrollView.OnScrollChangeListener {
                                    private var isLoadMore = false
                                    override fun onScrollChange(
                                        v: NestedScrollView,
                                        scrollX: Int,
                                        scrollY: Int,
                                        oldScrollX: Int,
                                        oldScrollY: Int
                                    ) {

                                        // if (diff == 0) {
                                        if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                            val linearLayoutManager1 =
                                                    binding.recyclerView.layoutManager as LinearLayoutManager?
                                            if (orderTrackerArrayList.size < total) {
                                                if (!isLoadMore) {
                                                    if (linearLayoutManager1 != null && linearLayoutManager1.findLastCompletelyVisibleItemPosition() == orderTrackerArrayList.size - 1) {
                                                        //bottom of list!


                                                        offset += Constant.LOAD_ITEM_LIMIT
                                                        val params1: MutableMap<String, String> = HashMap()
                                                        params1[Constant.GET_ORDERS] = Constant.GetVal
                                                        params1[Constant.USER_ID] = session.getData(Constant.ID).toString()
                                                        params1[Constant.OFFSET] = "" + offset
                                                        params1[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
                                                        requestToVolley(
                                                            object :
                                                                VolleyCallback {
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
                                                                                for (i in 0 until jsonArray1.length()) {
                                                                                    val orderTracker =
                                                                                        Gson().fromJson(
                                                                                            jsonArray.getJSONObject(
                                                                                                i
                                                                                            )
                                                                                                .toString(),
                                                                                            OrderTracker::class.java
                                                                                        )
                                                                                    orderTrackerArrayList.add(
                                                                                        orderTracker
                                                                                    )
                                                                                }
                                                                                orderListAdapter.notifyDataSetChanged()
                                                                                orderListAdapter.setLoaded()
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
                                                                }
                                                            },
                                                            activity,
                                                            Constant.ORDER_PROCESS_URL,
                                                            params1,
                                                            false
                                                        )
                                                    }
                                                    isLoadMore = true
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                        } else {
                            binding.recyclerView.visibility = View.GONE
                            binding.tvAlert.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.ORDER_PROCESS_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.your_order)
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
}