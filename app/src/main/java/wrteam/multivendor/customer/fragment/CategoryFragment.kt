package com.gpn.customerapp.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import com.gpn.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.adapter.CategoryAdapter
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback
import com.gpn.customerapp.model.Category
import com.gpn.customerapp.R
import com.gpn.customerapp.databinding.FragmentCategoryBinding

class CategoryFragment : Fragment() {
    lateinit var binding: FragmentCategoryBinding

    lateinit var root: View
    lateinit var activity: Activity
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_category, container, false)

        binding = FragmentCategoryBinding.inflate(inflater,container,false)

        activity = requireActivity()
        setHasOptionsMenu(true)

        binding.categoryRecycleView.layoutManager = GridLayoutManager(activity, Constant.GRID_COLUMN)
        binding.swipeLayout.setColorSchemeColors(ContextCompat.getColor(activity, R.color.colorPrimary))
        binding.swipeLayout.setOnRefreshListener {
            binding.swipeLayout.isRefreshing = false
            if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
                ApiConfig.getWalletBalance(activity, Session(activity))
            }
            getCategory()
        }
        if (Session(activity).getBoolean(Constant.IS_USER_LOGIN)) {
            ApiConfig.getWalletBalance(activity, Session(activity))
        }
        getCategory()
        return binding.root
    }

    private fun getCategory() {
        categoryArrayList = ArrayList()
        binding.categoryRecycleView.visibility = View.GONE
        binding.shimmerFrameLayout.visibility = View.VISIBLE
        binding.shimmerFrameLayout.startShimmer()
        val params: MutableMap<String, String> = HashMap()
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
                        binding.categoryRecycleView.adapter = CategoryAdapter(
                            activity,
                            categoryArrayList,
                            R.layout.lyt_subcategory,
                            "category",
                            0
                        )
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.categoryRecycleView.visibility = View.VISIBLE
                    } else {
                        binding.tvAlert.visibility = View.VISIBLE
                        binding.shimmerFrameLayout.stopShimmer()
                        binding.shimmerFrameLayout.visibility = View.GONE
                        binding.categoryRecycleView.visibility = View.GONE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.shimmerFrameLayout.stopShimmer()
                    binding.shimmerFrameLayout.visibility = View.GONE
                    binding.categoryRecycleView.visibility = View.GONE
                }
            }
        }
        }, activity, Constant.CATEGORY_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.title_category)
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.toolbar_cart).isVisible = true
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = true
    }

    companion object {
        lateinit var categoryArrayList: ArrayList<Category>
    }
}