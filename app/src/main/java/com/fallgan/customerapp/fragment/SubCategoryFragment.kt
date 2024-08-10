package com.fallgan.customerapp.fragment


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.fallgan.customerapp.R
import com.fallgan.customerapp.adapter.ProductLoadMoreAdapter
import com.fallgan.customerapp.adapter.SubCategoryAdapter
import com.fallgan.customerapp.databinding.FragmentSubCategoryBinding
import com.fallgan.customerapp.helper.ApiConfig
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session
import com.fallgan.customerapp.helper.VolleyCallback
import com.fallgan.customerapp.model.Category
import com.fallgan.customerapp.model.Product

@SuppressLint("UseCompatLoadingForDrawables", "NotifyDataSetChanged")
class SubCategoryFragment : Fragment() {
    lateinit var binding: FragmentSubCategoryBinding
    private lateinit var productLoadMoreAdapter: ProductLoadMoreAdapter
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var id: String
    private lateinit var filterBy: String
    lateinit var from: String

    var total = 0
    var offset = 0
    private var filterIndex = 0
    var isLoadMore = false
    private var isGrid = false
    var resource = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_sub_category, container, false)
        binding = FragmentSubCategoryBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)
        offset = 0
        activity = requireActivity()
        binding.subCategoryRecycleView.layoutManager =
            GridLayoutManager(activity, Constant.GRID_COLUMN)
        session = Session(activity)
        assert(arguments != null)
        from = requireArguments().getString(Constant.FROM).toString()
        id = requireArguments().getString("id").toString()
        if (session.getBoolean("grid")) {
            resource = R.layout.lyt_item_grid
            isGrid = true
            binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        } else {
            resource = R.layout.lyt_item_list
            isGrid = false
            binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        }
        filterIndex = -1
        ApiConfig.getSettings(activity)
        getCategory()
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            getCategory()
        }
        return binding.root
    }

    fun stopShimmer() {
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.shimmerFrameLayout.visibility = View.GONE
        binding.shimmerFrameLayout.stopShimmer()
    }

    fun startShimmer() {
        binding.nestedScrollView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
    }

    private fun getCategory() {
        startShimmer()
        categoryArrayList = ArrayList()
        productArrayList = ArrayList()
        productLoadMoreAdapter = ProductLoadMoreAdapter(activity, productArrayList, resource, from)
        val params: MutableMap<String, String> = HashMap()
        params[Constant.CATEGORY_ID] = id
        ApiConfig.requestToVolley(object : VolleyCallback {
            override fun onSuccess(result: Boolean, response: String) {
                if (result) {
                    try {
                        val `object` = JSONObject(response)
                        if (!`object`.getBoolean(Constant.ERROR)) {
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val category = Category()
                                category.id = jsonObject.getString(Constant.ID)
                                category.category_id = jsonObject.getString(Constant.CATEGORY_ID)
                                category.name = jsonObject.getString(Constant.NAME)
                                category.slug = jsonObject.getString(Constant.SLUG)
                                category.subtitle = jsonObject.getString(Constant.SUBTITLE)
                                category.image = jsonObject.getString(Constant.IMAGE)
                                categoryArrayList.add(category)
                            }
                            binding.subCategoryRecycleView.adapter = SubCategoryAdapter(
                                activity,
                                categoryArrayList,
                                R.layout.lyt_subcategory,
                                "sub_cate"
                            )
                        }
                        getProducts()
                        stopShimmer()
                    } catch (e: JSONException) {
                        getProducts()
                        e.printStackTrace()
                        stopShimmer()
                    }
                }
            }
        }, activity, Constant.GET_SUB_CATEGORY_URL, params, false)
    }

    private fun getProducts() {
        startShimmer()
        productArrayList = ArrayList()
        val params: MutableMap<String, String> = HashMap()
        params[Constant.CATEGORY_ID] = id
        params[Constant.GET_ALL_PRODUCTS] = Constant.GetVal
        if (session.getBoolean(Constant.IS_USER_LOGIN)) {
            params[Constant.USER_ID] = session.getData(Constant.ID).toString()
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

                        val jsonObject1 = JSONObject(response)
                        if (!jsonObject1.getBoolean(Constant.ERROR)) {
                            total = jsonObject1.getString(Constant.TOTAL).toInt()
                            val `object` = JSONObject(response)
                            val jsonArray = `object`.getJSONArray(Constant.DATA)
                            for (i in 0 until jsonArray.length()) {
                                val product = Gson().fromJson(
                                    jsonArray.getJSONObject(i).toString(),
                                    Product::class.java
                                )
                                productArrayList.add(product)
                            }
                            if (offset == 0) {
                                productLoadMoreAdapter =
                                    ProductLoadMoreAdapter(
                                        activity,
                                        productArrayList,
                                        resource,
                                        from
                                    )
                                binding.recyclerView.adapter = productLoadMoreAdapter
                                binding.nestedScrollView.visibility = View.VISIBLE
                                binding.shimmerFrameLayout.visibility = View.GONE
                                binding.shimmerFrameLayout.stopShimmer()
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
                                                    params1[Constant.CATEGORY_ID] = id
                                                    params1[Constant.USER_ID] =
                                                        session.getData(Constant.ID).toString()
                                                    params1[Constant.LIMIT] =
                                                        "" + Constant.LOAD_ITEM_LIMIT
                                                    params1[Constant.OFFSET] =
                                                        offset.toString() + ""
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

                                                                        val jsonObject11 =
                                                                            JSONObject(response)
                                                                        if (!jsonObject11.getBoolean(
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
                                                                                for (i in 0 until jsonArray1.length()) {
                                                                                    val product =
                                                                                        Gson().fromJson(
                                                                                            jsonArray1.getJSONObject(
                                                                                                i
                                                                                            )
                                                                                                .toString(),
                                                                                            Product::class.java
                                                                                        )
                                                                                    productArrayList.add(
                                                                                        product
                                                                                    )
                                                                                }
                                                                            } catch (e: Exception) {
                                                                                binding.nestedScrollView.visibility =
                                                                                    View.VISIBLE
                                                                                binding.shimmerFrameLayout.visibility =
                                                                                    View.GONE
                                                                                binding.shimmerFrameLayout.stopShimmer()
                                                                            }
                                                                            productLoadMoreAdapter.notifyDataSetChanged()
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
                            stopShimmer()
                            if (categoryArrayList.size == 0 && productArrayList.size == 0) {
                                binding.tvAlert.visibility = View.VISIBLE
                            } else {
                                binding.tvAlert.visibility = View.GONE
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        stopShimmer()
                        if (categoryArrayList.size == 0 && productArrayList.size == 0) {
                            binding.tvAlert.visibility = View.VISIBLE
                        } else {
                            binding.tvAlert.visibility = View.GONE
                        }
                    }
                }
            }
        }, activity, Constant.GET_PRODUCTS_URL, params, false)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_layout) {
            if (isGrid) {
                isGrid = false
                binding.recyclerView.adapter = null
                resource = R.layout.lyt_item_list
                binding.recyclerView.layoutManager = LinearLayoutManager(activity)
            } else {
                isGrid = true
                binding.recyclerView.adapter = null
                resource = R.layout.lyt_item_grid
                binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
            }
            session.setBoolean("grid", isGrid)
            productLoadMoreAdapter =
                ProductLoadMoreAdapter(activity, productArrayList, resource, from)
            binding.recyclerView.adapter = productLoadMoreAdapter
            productLoadMoreAdapter.notifyDataSetChanged()
            activity.invalidateOptionsMenu()
        }
        return false
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onPrepareOptionsMenu(menu: Menu) {
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

    override fun onResume() {
        super.onResume()
        assert(arguments != null)
        Constant.TOOLBAR_TITLE = requireArguments().getString(Constant.NAME).toString()
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

    override fun onPause() {
        super.onPause()
        ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
    }

    companion object {
        lateinit var productArrayList: ArrayList<Product?>
        lateinit var categoryArrayList: ArrayList<Category>
    }
}