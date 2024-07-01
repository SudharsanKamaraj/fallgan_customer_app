package com.gpn.customerapp.fragment


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.adapter.ProductLoadMoreAdapter
import com.gpn.customerapp.databinding.FragmentSellerProductsBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Category
import com.gpn.customerapp.model.Product
import com.gpn.customerapp.model.Seller

class SellerProductsFragment : Fragment() {
    lateinit var binding: FragmentSellerProductsBinding
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var id: String
    private lateinit var filterBy: String
    lateinit var from: String
    var total = 0
    var offset = 0
    private var filterIndex = 0
    private val isSort = false
    var isLoadMore = false
    private var isGrid = false
    var resource = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_seller_products, container, false)
        binding = FragmentSellerProductsBinding.inflate(inflater,container,false)
        setHasOptionsMenu(true)
        offset = 0
        activity = requireActivity()
        session = Session(activity)

        id = requireArguments().getString(Constant.ID).toString()
        from = requireArguments().getString(Constant.FROM).toString()

        productArrayList = ArrayList()

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
        //        categoryRecycleView.setLayoutManager(new GridLayoutManager(activity, 3));
        ApiConfig.getSettings(activity)
        filterIndex = -1
        getSellerData()
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            if (productArrayList.size > 0) {
                offset = 0
                binding.swipeLayout.isRefreshing = false
                productArrayList.clear()
                getSellerData()
            }
        }
        return binding.root
    }

    private fun getSellerData() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_SELLER_DATA] = Constant.GetVal
        params[Constant.SELLER_ID] = id
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            val seller = Gson().fromJson(
                                `object`.getJSONArray(Constant.DATA).getJSONObject(0).toString(),
                                Seller::class.java
                            )
                            binding.tvTitle.text = seller.store_name
                            Glide.with(activity).load(if (seller.logo == "") "-" else seller.logo)
                                .centerInside()
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.placeholder)
                                .into(binding.imgTitle)
                            binding.tvTitle.setOnClickListener {
                                val message = Constant.WebSiteUrl + "vendor/" + seller.slug
                                val sendIntent = Intent()
                                sendIntent.action = Intent.ACTION_SEND
                                sendIntent.putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    getString(R.string.app_name)
                                )
                                sendIntent.putExtra(Intent.EXTRA_TEXT, message)
                                sendIntent.type = "text/plain"
                                val shareIntent =
                                    Intent.createChooser(sendIntent, getString(R.string.share_via))
                                startActivity(shareIntent)
                            }
                        }
                        getData()
                        //                    getCategory();
                    } catch (e: JSONException) {
                        getCategory()
                        e.printStackTrace()
                    }
                }
            }
        }, activity, Constant.GET_SELLER_DATA_URL, params, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getData() {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_ALL_PRODUCTS] = Constant.GetVal
        params[Constant.SELLER_ID] = id
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(Constant.GET_SELECTED_PINCODE_ID) != "0") {
            params[Constant.PINCODES] =
                session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
        }
        params[Constant.LIMIT] = "" + Constant.LOAD_ITEM_LIMIT
        params[Constant.OFFSET] = "" + offset
        if (filterIndex != -1) {
            params[Constant.SORT] = filterBy
        }
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val jsonObject = JSONObject(response)
                        if (!jsonObject.getBoolean(Constant.ERROR)) {
                            total = jsonObject.getString(Constant.TOTAL).toInt()
                            if (offset == 0) {
                                productArrayList = ArrayList()
                                binding.tvAlert.visibility = View.GONE
                            }
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            try {
                                productArrayList.addAll(ApiConfig.getProductList(jsonArray))
                            } catch (e: Exception) {
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                            }
                            if (offset == 0) {
                                mAdapter =
                                    ProductLoadMoreAdapter(
                                        activity,
                                        productArrayList,
                                        resource,
                                        from
                                    )
                                mAdapter.setHasStableIds(true)
                                binding.recyclerView.adapter = mAdapter
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.nestedScrollView.setOnScrollChangeListener { v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->

                                    // if (diff == 0) {
                                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                        val linearLayoutManager =
                                                binding.recyclerView.layoutManager as LinearLayoutManager?
                                        if (productArrayList.size < total) {
                                            if (!isLoadMore) {
                                                if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == productArrayList.size - 1) {
                                                    //bottom of list!

                                                    offset += ("" + Constant.LOAD_ITEM_LIMIT).toInt()
                                                    val params1: MutableMap<String, String> =
                                                        HashMap()
                                                    params1[Constant.GET_ALL_PRODUCTS] =
                                                        Constant.GetVal
                                                    params1[Constant.SELLER_ID] = id
                                                    if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                            Constant.GET_SELECTED_PINCODE_ID
                                                        ) != "0"
                                                    ) {
                                                        params1[Constant.PINCODE_ID] =
                                                            session.getData(
                                                                Constant.GET_SELECTED_PINCODE_ID
                                                            ).toString()
                                                    }
                                                    params1[Constant.USER_ID] =
                                                        session.getData(Constant.ID).toString()
                                                    params1[Constant.LIMIT] =
                                                        "" + Constant.LOAD_ITEM_LIMIT
                                                    params1[Constant.OFFSET] = "" + offset
                                                    if (filterIndex != -1) {
                                                        params1[Constant.SORT] = filterBy
                                                    }
                                                    ApiConfig.requestToVolley(
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
                                                                            val object1 =
                                                                                JSONObject(response)
                                                                            val jsonArray1 =
                                                                                object1.getJSONArray(
                                                                                    Constant.DATA
                                                                                )

                                                                            try {
                                                                                productArrayList.addAll(
                                                                                    ApiConfig.getProductList(
                                                                                        jsonArray1
                                                                                    )
                                                                                )
                                                                            } catch (e: Exception) {
                                                                                e.printStackTrace()
                                                                            }
                                                                            mAdapter.notifyDataSetChanged()
                                                                            mAdapter.setLoaded()
                                                                            isLoadMore = false
                                                                        }
                                                                    } catch (e: JSONException) {
                                                                        e.printStackTrace()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        activity,
                                                        Constant.GET_PRODUCTS_URL,
                                                        params1,
                                                        false
                                                    )
                                                    isLoadMore = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (offset == 0) {
                                binding.shimmerFrameLayout.stopShimmer()
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                binding.tvAlert.visibility = View.VISIBLE
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    private fun getCategory() {
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        categoryArrayList = ArrayList()
        //        categoryRecycleView.setVisibility(View.GONE);
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.SELLER_ID] = id
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            val gson = Gson()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val category =
                                    gson.fromJson(jsonObject.toString(), Category::class.java)
                                categoryArrayList.add(category)
                            }
                            //                        categoryRecycleView.setAdapter(new CategoryAdapter(activity, categoryArrayList, R.layout.lyt_subcategory, "category", 0));
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            //                        categoryRecycleView.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvAlert.visibility = View.VISIBLE
                            binding.shimmerFrameLayout.stopShimmer()
                            binding.shimmerFrameLayout.visibility = View.GONE
                            //                        categoryRecycleView.setVisibility(View.GONE);
                        }
                        getData()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        getData()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        //                    categoryRecycleView.setVisibility(View.GONE);
                    }
                }
            }
        }, activity, Constant.CATEGORY_URL, params, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_sort) {
            val builder = AlertDialog.Builder(
                activity
            )
            builder.setTitle(activity.resources.getString(R.string.filter_by))
            builder.setSingleChoiceItems(
                Constant.filterValues,
                filterIndex
            ) { dialog: DialogInterface, item1: Int ->
                filterIndex = item1
                when (item1) {
                    0 -> filterBy = Constant.NEW
                    1 -> filterBy = Constant.OLD
                    2 -> filterBy = Constant.HIGH
                    3 -> filterBy = Constant.LOW
                }
                if (item1 != -1) getData()
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        } else if (item.itemId == R.id.toolbar_layout) {
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
            mAdapter = ProductLoadMoreAdapter(activity, productArrayList, resource, from)
            binding.recyclerView.adapter = mAdapter
            mAdapter.notifyDataSetChanged()
            activity.invalidateOptionsMenu()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.toolbar_sort).isVisible = isSort
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

    fun startShimmer() {
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
    }

    fun stopShimmer() {
        binding.shimmerFrameLayout.stopShimmer()
        binding.shimmerFrameLayout.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        assert(arguments != null)
        Constant.TOOLBAR_TITLE = requireArguments().getString(Constant.TITLE).toString()
        activity.invalidateOptionsMenu()
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

    override fun onPause() {
        super.onPause()
        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
    }

    companion object {
        lateinit var productArrayList: ArrayList<Product?>

        @SuppressLint("StaticFieldLeak")
        lateinit var mAdapter: ProductLoadMoreAdapter
        lateinit var categoryArrayList: ArrayList<Category>
    }
}