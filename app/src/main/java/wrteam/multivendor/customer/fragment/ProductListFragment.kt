package wrteam.multivendor.customer.fragment


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONException
import org.json.JSONObject
import wrteam.multivendor.customer.R
import wrteam.multivendor.customer.adapter.ProductLoadMoreAdapter
import wrteam.multivendor.customer.databinding.FragmentProductListBinding
import wrteam.multivendor.customer.helper.ApiConfig
import wrteam.multivendor.customer.helper.Constant
import wrteam.multivendor.customer.helper.Session
import wrteam.multivendor.customer.helper.VolleyCallback
import wrteam.multivendor.customer.model.Product
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("NotifyDataSetChanged")
class ProductListFragment : Fragment() {
    lateinit var binding: FragmentProductListBinding
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    lateinit var id: String
    private lateinit var filterBy: String
    lateinit var from: String
    private lateinit var productsName: ArrayList<String>
    lateinit var arrayAdapter: ArrayAdapter<String>

    var total = 0
    var offset = 0
    private var filterIndex = 0
    private var isSort = false
    var isLoadMore = false
    private var isGrid = false
    var resource = 0
    private var listPosition = 0
    private var query: String? = ""
    var url = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        assert(arguments != null)
        root = inflater.inflate(R.layout.fragment_product_list, container, false)
        binding= FragmentProductListBinding.inflate(inflater,container,false)
        setHasOptionsMenu(true)
        offset = 0
        activity = requireActivity()
        Constant.CartValues = HashMap()
        session = Session(activity)
        from = requireArguments().getString(Constant.FROM).toString()
        id = requireArguments().getString(Constant.ID).toString()
        listPosition = requireArguments().getInt(Constant.LIST_POSITION)


        productArrayList = ArrayList()
        productsName = ArrayList()

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

        mAdapter = ProductLoadMoreAdapter(activity, productArrayList, resource, from)

        ApiConfig.getSettings(activity)
        filterIndex = -1
        when (from) {
            "sub_cate", "similar", "section" -> getData()
            "search" -> {
                try {
                    stopShimmer()
                    binding.lytSearchView.visibility = View.VISIBLE
                    Constant.CartValues = HashMap()

                    val string =
                        session.getData(Constant.GET_ALL_PRODUCTS_NAME)!!.replace("[\"", "")
                            .replace("[\"", "").split("\",\"")
                    for (item in string) {
                        productsName.add(item)
                    }

                    binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close_,
                        0
                    )
                    arrayAdapter = ArrayAdapter(
                        activity, R.layout.spinner_search_item, productsName
                    )
                    binding.listView.dividerHeight = 0
                    binding.listView.adapter = arrayAdapter
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
        binding.swipeLayout.setColorSchemeResources(R.color.colorPrimary)
        binding.swipeLayout.setOnRefreshListener {
            if (productArrayList.size > 0) {
                offset = 0
                binding.swipeLayout.isRefreshing = false
                getData()
            }
        }
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                arrayAdapter.filter.filter(
                        binding.searchView.text.toString().trim()
                        .lowercase(Locale.getDefault())
                )
                if (binding.searchView.text.toString().trim()
                        .isNotEmpty() && binding.listView.visibility == View.GONE
                ) {
                    binding.listView.visibility = View.VISIBLE
                    binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_search,
                        0,
                        R.drawable.ic_close,
                        0
                    )
                } else {
                    binding.listView.visibility = View.GONE
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
        binding.searchView.setOnEditorActionListener { v: TextView, _: Int, _: KeyEvent? ->
            query = v.text.toString().trim()
            binding.listView.visibility = View.GONE
            getData()
            true
        }
        binding.listView.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                binding.searchView.setText(
                    arrayAdapter.getItem(position)
                )
                binding.searchView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_search,
                    0,
                    R.drawable.ic_close,
                    0
                )
                query = arrayAdapter.getItem(position)
                binding.listView.visibility = View.GONE
                getData()
            }
        binding.searchView.setOnTouchListener { _, event: MotionEvent ->
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
                    }
                }
            }
            false
        }
        return binding.root
    }

    fun getData() {
        productArrayList = ArrayList()
        mAdapter.notifyDataSetChanged()
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
        when (from) {
            "sub_cate" -> {
                url = Constant.GET_PRODUCTS_URL
                params[Constant.GET_ALL_PRODUCTS] = Constant.GetVal
                params[Constant.SUB_CATEGORY_ID] = id
                if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                        Constant.GET_SELECTED_PINCODE_ID
                    ) != "0"
                ) {
                    params[Constant.PINCODE] =
                        session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
                }
                isSort = true
            }
            "similar" -> {
                url = Constant.GET_PRODUCTS_URL
                params[Constant.GET_SIMILAR_PRODUCT] = Constant.GetVal
                params[Constant.PRODUCT_ID] = id
                params[Constant.CATEGORY_ID] = requireArguments().getString("cat_id").toString()
                if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                        Constant.GET_SELECTED_PINCODE_ID
                    ) != "0"
                ) {
                    params[Constant.PINCODE] =
                        session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
                }
            }
            "section" -> {
                url = Constant.GET_SECTION_URL
                params[Constant.GET_ALL_SECTIONS] = Constant.GetVal
                params[Constant.SECTION_ID] = id
                if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                        Constant.GET_SELECTED_PINCODE_ID
                    ) != "0"
                ) {
                    params[Constant.PINCODE] =
                        session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
                }
            }
            "search" -> {
                url = Constant.GET_PRODUCTS_URL
                params[Constant.GET_ALL_PRODUCTS] = Constant.GetVal
                if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                        Constant.GET_SELECTED_PINCODE_ID
                    ) != "0"
                ) {
                    params[Constant.PINCODE] =
                        session.getData(Constant.GET_SELECTED_PINCODE_NAME).toString()
                }
                params[Constant.SEARCH] = query.toString()
            }
        }
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
                                                    when (from) {
                                                        "sub_cate" -> {
                                                            params1[Constant.GET_ALL_PRODUCTS] =
                                                                Constant.GetVal
                                                            params1[Constant.SUB_CATEGORY_ID] = id
                                                            if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                                    Constant.GET_SELECTED_PINCODE_ID
                                                                ) != "0"
                                                            ) {
                                                                params1[Constant.PINCODE_ID] =
                                                                    session.getData(
                                                                        Constant.GET_SELECTED_PINCODE_ID
                                                                    ).toString()
                                                            }
                                                            isSort = true
                                                        }
                                                        "similar" -> {
                                                            params1[Constant.GET_SIMILAR_PRODUCT] =
                                                                Constant.GetVal
                                                            params1[Constant.PRODUCT_ID] = id
                                                            params1[Constant.CATEGORY_ID] =
                                                                requireArguments().getString("cat_id")
                                                                    .toString()
                                                            if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                                    Constant.GET_SELECTED_PINCODE_ID
                                                                ) != "0"
                                                            ) {
                                                                params1[Constant.PINCODE_ID] =
                                                                    session.getData(
                                                                        Constant.GET_SELECTED_PINCODE_ID
                                                                    ).toString()
                                                            }
                                                        }
                                                        "section" -> {
                                                            params1[Constant.GET_ALL_SECTIONS] =
                                                                Constant.GetVal
                                                            params1[Constant.SECTION_ID] = id
                                                            if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                                    Constant.GET_SELECTED_PINCODE_ID
                                                                ) != "0"
                                                            ) {
                                                                params1[Constant.PINCODE_ID] =
                                                                    session.getData(
                                                                        Constant.GET_SELECTED_PINCODE_ID
                                                                    ).toString()
                                                            }
                                                        }
                                                        "search" -> {
                                                            params1[Constant.GET_ALL_PRODUCTS] =
                                                                Constant.GetVal
                                                            if (session.getData(Constant.SHIPPING_TYPE).equals("local") && session.getBoolean(Constant.GET_SELECTED_PINCODE) && session.getData(
                                                                    Constant.GET_SELECTED_PINCODE_ID
                                                                ) != "0"
                                                            ) {
                                                                params1[Constant.PINCODE_ID] =
                                                                    session.getData(
                                                                        Constant.GET_SELECTED_PINCODE_ID
                                                                    ).toString()
                                                            }
                                                            params1[Constant.SEARCH] =
                                                                query.toString()
                                                        }
                                                    }
                                                    if (session.getBoolean(Constant.IS_USER_LOGIN)) {
                                                        params1[Constant.USER_ID] =
                                                            session.getData(Constant.ID).toString()
                                                    }
                                                    params1[Constant.LIMIT] =
                                                        "" + Constant.LOAD_ITEM_LIMIT
                                                    params1[Constant.OFFSET] = "" + offset
                                                    if (filterIndex != -1) {
                                                        params1[Constant.SORT] = filterBy
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
                                                    }, activity, url, params1, false)
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
                                binding.tvAlert.visibility = View.VISIBLE
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.tvAlert.visibility = View.VISIBLE
                    }
                } else {
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.tvAlert.visibility = View.VISIBLE
                }
            }
        }, activity, url, params, false)
    }

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
                resource = R.layout.lyt_item_list
                binding.recyclerView.layoutManager = LinearLayoutManager(activity)
            } else {
                binding.lytGrid.visibility = View.VISIBLE
                binding.lytList.visibility = View.GONE
                isGrid = true
                resource = R.layout.lyt_item_grid
                binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
            }
            if (mAdapter != null) {
                mAdapter = ProductLoadMoreAdapter(activity, productArrayList, resource, from)
                binding.recyclerView.adapter = mAdapter
                mAdapter.notifyDataSetChanged()
            }
            session.setBoolean("grid", isGrid)
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
        Constant.TOOLBAR_TITLE = requireArguments().getString(Constant.NAME).toString()
        activity.invalidateOptionsMenu()
        if (requireArguments().getString(Constant.FROM) == "search") {
            binding.searchView.requestFocus()
            showSoftKeyboard(binding.searchView)
        } else {
            hideKeyboard()
        }
    }

    private fun showSoftKeyboard(view: View?) {
        if (requireView().requestFocus()) {
            val imm =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
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
        if (Constant.CartValues != null) {
            ApiConfig.addMultipleProductInCart(session, activity, Constant.CartValues)
        }
    }

    companion object {
        lateinit var productArrayList: ArrayList<Product?>

        @SuppressLint("StaticFieldLeak")
        lateinit var mAdapter: ProductLoadMoreAdapter
    }

}