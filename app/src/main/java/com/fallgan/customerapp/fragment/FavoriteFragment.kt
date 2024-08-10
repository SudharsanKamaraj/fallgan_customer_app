package com.fallgan.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.adapter.ProductLoadMoreAdapter
import com.fallgan.customerapp.databinding.FragmentFavoriteBinding
import com.fallgan.customerapp.helper.*
import com.fallgan.customerapp.model.Product

@SuppressLint("NotifyDataSetChanged")
class FavoriteFragment : Fragment() {
    lateinit var binding: FragmentFavoriteBinding
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var databaseHelper: DatabaseHelper
    var total = 0
    private var isLogin = false
    var offset = 0
    var isLoadMore = false
    private var isGrid = false
    var resource = 0
    var url = ""

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_favorite, container, false)

        binding = FragmentFavoriteBinding.inflate(inflater,container,false)

        tvAlert = root.findViewById(R.id.tvAlert)

        setHasOptionsMenu(true)
        Constant.CartValues = HashMap()
        activity = requireActivity()
        session = Session(activity)
        isLogin = session.getBoolean(Constant.IS_USER_LOGIN)
        databaseHelper = DatabaseHelper(activity)
        url = if (isLogin) {
            Constant.GET_FAVORITES_URL
        } else {
            Constant.GET_PRODUCTS_URL
        }

        if (session.getBoolean("grid")) {
            resource = R.layout.lyt_item_grid
            isGrid = true
            binding.lytGrid.visibility = View.VISIBLE
            binding.lytList.visibility = View.GONE

            binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        } else {
            resource = R.layout.lyt_item_list
            isGrid = false
            binding.lytGrid.visibility = View.GONE
            binding.lytList.visibility = View.VISIBLE

            binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        }
        ApiConfig.getSettings(activity)
        ApiConfig.getWalletBalance(activity, Session(activity))
        getData()
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
                ApiConfig.getWalletBalance(activity, Session(activity))
            }
            if (Constant.CartValues != null && Constant.CartValues.size > 0) {
                ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
            }
            offset = 0
            getData()
            binding.swipeLayout.isRefreshing = false
        }
        return binding.root
    }

    private fun getData() {
        binding.recyclerView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        if (isLogin) {
            params[Constant.GET_FAVORITES] = Constant.GetVal
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
            params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
            params[Constant.OFFSET] = offset.toString() + ""
        } else {
            params[Constant.GET_PRODUCTS_OFFLINE] = Constant.GetVal
            params[Constant.PRODUCT_IDs] =
                    databaseHelper.favorite().toString().replace("[", "").replace("]", "")
        }
        if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0") {
            params[Constant.PINCODE] = session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            if (isLogin) {
                                total = jsonObject.getString(Constant.TOTAL).toInt()
                            }
                            if (offset == 0) {
                                favoriteArrayList = ArrayList()
                                binding.recyclerView.visibility = View.VISIBLE
                                tvAlert.visibility = View.GONE
                            }
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            favoriteArrayList.addAll(ApiConfig.getFavoriteProductList(jsonArray))
                            if (offset == 0) {
                                favoriteLoadMoreAdapter = ProductLoadMoreAdapter(
                                        activity,
                                        favoriteArrayList,
                                        resource,
                                        "favorite"
                                )
                                binding.recyclerView.adapter = favoriteLoadMoreAdapter
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                        val linearLayoutManager =
                                                binding.recyclerView.layoutManager as LinearLayoutManager?
                                        if (favoriteArrayList.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == favoriteArrayList.size - 1) {
                                                    //bottom of list!

                                                    offset += Constant.LOAD_ITEM_LIMIT
                                                    val params1: MutableMap<String, String> = HashMap()
                                                    if (isLogin) {
                                                        params1[Constant.GET_FAVORITES] =
                                                                Constant.GetVal
                                                        params1[Constant.USER_ID] =
                                                                session.getData(Constant.ID).toString()
                                                        params1[Constant.LIMIT] =
                                                                "" + Constant.LOAD_ITEM_LIMIT
                                                        params1[Constant.OFFSET] =
                                                                offset.toString() + ""
                                                    } else {
                                                        params1[Constant.GET_PRODUCTS_OFFLINE] =
                                                                Constant.GetVal
                                                        params1[Constant.PRODUCT_IDs] =
                                                                databaseHelper.favorite().toString()
                                                                        .replace("[", "").replace("]", "")
                                                    }
                                                    if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                                    Constant.GET_SELECTED_PINCODE_ID
                                                            ) != "0"
                                                    ) {
                                                        params[Constant.PINCODE] =
                                                                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
                                                    }
                                                    ApiConfig.requestToVolley(object :
                                                            VolleyCallback {
                                                        override fun onSuccess(
                                                                result: Boolean,
                                                                response: String
                                                        ) {
                                                            if (result) {
                                                                try {
                                                                    val jsonObject1 =
                                                                            JSONObject(response)
                                                                    if (!jsonObject1.getBoolean(Constant.ERROR)) {
                                                                        val object1 =
                                                                                JSONObject(response)
                                                                        val jsonArray1 =
                                                                                object1.getJSONArray(
                                                                                        Constant.DATA
                                                                                )
                                                                        favoriteArrayList.addAll(
                                                                                ApiConfig.getFavoriteProductList(
                                                                                        jsonArray1
                                                                                )
                                                                        )
                                                                        favoriteLoadMoreAdapter.notifyDataSetChanged()
                                                                        isLoadMore = false
                                                                    }
                                                                } catch (e: JSONException) {
                                                                    binding.shimmerFrameLayout.stopShimmer()
                                                                    binding.shimmerFrameLayout.visibility =
                                                                            View.GONE
                                                                    binding.recyclerView.visibility =
                                                                            View.VISIBLE
                                                                }
                                                            } else {
                                                                isLoadMore = false
                                                                favoriteLoadMoreAdapter.notifyDataSetChanged()
                                                                binding.shimmerFrameLayout.stopShimmer()
                                                                binding.shimmerFrameLayout.visibility =
                                                                        View.GONE
                                                                binding.recyclerView.visibility = View.VISIBLE
                                                                binding.recyclerView.visibility = View.GONE
                                                                tvAlert.visibility = View.VISIBLE
                                                            }
                                                        }
                                                    }, activity, url, params1, false)
                                                    isLoadMore = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            tvAlert.visibility = View.VISIBLE
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                        tvAlert.visibility = View.VISIBLE
                    }
                } else {
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    tvAlert.visibility = View.VISIBLE
                }
            }
        }, activity, url, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.title_fav)
        requireActivity().invalidateOptionsMenu()
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

    override fun onHiddenChanged(hidden: Boolean) {
        binding.recyclerView.visibility = View.GONE
        tvAlert.visibility = View.GONE
        if (!hidden) getData()
        super.onHiddenChanged(hidden)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_layout) {
            if (isGrid) {
                binding.lytGrid.visibility = View.GONE
                binding.lytList.visibility = View.VISIBLE
                isGrid = false
                binding.recyclerView.adapter = null
                resource = R.layout.lyt_item_list
                binding.recyclerView.layoutManager = LinearLayoutManager(activity)
            } else {
                binding.lytGrid.visibility = View.VISIBLE
                binding.lytList.visibility = View.GONE
                isGrid = true
                binding.recyclerView.adapter = null
                resource = R.layout.lyt_item_grid
                binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
            }
            session.setBoolean("grid", isGrid)
            favoriteLoadMoreAdapter =
                    ProductLoadMoreAdapter(activity, favoriteArrayList, resource, "favorite")
            binding.recyclerView.adapter = favoriteLoadMoreAdapter
            favoriteLoadMoreAdapter.notifyDataSetChanged()
            activity.invalidateOptionsMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
        menu.findItem(R.id.toolbar_cart).icon = ApiConfig.buildCounterDrawable(
                Constant.TOTAL_CART_ITEM,
                activity
        )
        menu.findItem(R.id.toolbar_layout).isVisible = true
        val myDrawable: Drawable? = if (isGrid) {
            ContextCompat.getDrawable(
                    activity,
                    R.drawable.ic_list_
            ) // The ID of your drawable
        } else {
            ContextCompat.getDrawable(
                    activity,
                    R.drawable.ic_grid_
            ) // The ID of your drawable.
        }
        menu.findItem(R.id.toolbar_layout).icon = myDrawable
        super.onPrepareOptionsMenu(menu)
    }

    companion object {
        lateinit var favoriteArrayList: ArrayList<Product?>

        @SuppressLint("StaticFieldLeak")
        lateinit var favoriteLoadMoreAdapter: ProductLoadMoreAdapter

        @SuppressLint("StaticFieldLeak")
        lateinit var tvAlert: RelativeLayout
    }
}