package com.gpn.customerapp.fragment


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.json.JSONObject
import com.gpn.customerapp.R
import com.gpn.customerapp.activity.MainActivity
import com.gpn.customerapp.databinding.FragmentOrderPlacedBinding
import com.gpn.customerapp.helper.ApiConfig
import com.gpn.customerapp.helper.Constant
import com.gpn.customerapp.helper.Session
import com.gpn.customerapp.helper.VolleyCallback

class OrderPlacedFragment : Fragment() {
    lateinit var binding: FragmentOrderPlacedBinding
    lateinit var root: View
    lateinit var activity: Activity
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_order_placed, container, false)
        binding = FragmentOrderPlacedBinding.inflate(inflater,container,false)
        activity = requireActivity()
        val session = Session(activity)
        
        setHasOptionsMenu(true)
        removeAllItemFromCart(activity, session)
        return binding.root
    }

    private fun removeAllItemFromCart(activity: Activity, session: Session) {
        binding.progressBar.visibility = View.VISIBLE
        val params: MutableMap<String, String> = HashMap()
        params[Constant.REMOVE_FROM_CART] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        getCartItemCount(activity, session)
                    }
                    binding.progressBar.visibility = View.GONE
                } catch (e: JSONException) {
                    e.printStackTrace()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
        }, activity, Constant.CART_URL, params, false)
    }

    private fun getCartItemCount(activity: Activity, session: Session) {
        val params: MutableMap<String, String> = HashMap()
        params[Constant.GET_USER_CART] = Constant.GetVal
        params[Constant.USER_ID] = session.getData(Constant.ID).toString()
        ApiConfig.requestToVolley(object : VolleyCallback {
                override fun onSuccess(result: Boolean, response: String) {
                    if (result) {
                try {
                    val jsonObject = JSONObject(response)
                    if (!jsonObject.getBoolean(Constant.ERROR)) {
                        Constant.TOTAL_CART_ITEM = jsonObject.getString(Constant.TOTAL).toInt()
                    } else {
                        Constant.TOTAL_CART_ITEM = 0
                    }
                    Constant.CartValues.clear()
                    binding.lottieAnimationView.playAnimation()
                    binding.btnShopping.setOnClickListener {
                        startActivity(
                            Intent(activity, MainActivity::class.java).putExtra(
                                Constant.FROM, ""
                            )
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                    }
                    binding.btnSummary.setOnClickListener {
                        startActivity(
                            Intent(activity, MainActivity::class.java).putExtra(
                                Constant.FROM, "tracker"
                            )
                        )
                    }
                    activity.invalidateOptionsMenu()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        }, activity, Constant.CART_URL, params, false)
    }

    override fun onResume() {
        super.onResume()
        MainActivity.toolbar.visibility = View.GONE
        binding.lottieAnimationView.setAnimation("placed-order.json")
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
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
    }
}