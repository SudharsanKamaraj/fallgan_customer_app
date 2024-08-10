package com.fallgan.customerapp.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import com.fallgan.customerapp.helper.ApiConfig
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.fallgan.customerapp.R
import com.fallgan.customerapp.databinding.FragmentReferEarnBinding
import com.fallgan.customerapp.helper.Constant
import com.fallgan.customerapp.helper.Session

class ReferEarnFragment : Fragment() {
    lateinit var binding: FragmentReferEarnBinding
    lateinit var root: View
    lateinit var session: Session
    lateinit var activity: Activity
    var preText = ""
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_refer_earn, container, false)
        binding = FragmentReferEarnBinding.inflate(inflater,container,false)
        activity = requireActivity()
        setHasOptionsMenu(true)
        session = Session(activity)

        preText = if (session.getData(Constant.refer_earn_method) == "rupees") {
            session.getData(Constant.CURRENCY) + session.getData(Constant.refer_earn_bonus)
        } else {
            session.getData(Constant.refer_earn_bonus) + "% "
        }
        binding.tvReferCoin.text = getString(R.string.refer_text_1) + preText + getString(R.string.refer_text_2) + session.getData(
            Constant.CURRENCY
        ) + session.getData(Constant.min_refer_earn_order_amount) + getString(R.string.refer_text_3) + session.getData(
            Constant.CURRENCY
        ) + session.getData(Constant.max_refer_earn_amount) + "."

        binding.tvInvite.setCompoundDrawablesWithIntrinsicBounds(
            AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.ic_share
            ), null, null, null
        )
        binding.tvCode.text = session.getData(Constant.REFERRAL_CODE)
        binding.tvCode.setOnClickListener {
            ApiConfig.copyToClipboard(
                activity,
                activity.getString(R.string.your_refer_code),
                    binding.tvCode.text.toString()
            )
        }
        binding.tvInvite.setOnClickListener {
            if (binding.tvCode.text.toString() != "code") {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        """${getString(R.string.refer_share_msg_1)}${resources.getString(R.string.app_name)}${
                            getString(R.string.refer_share_msg_2)
                        }
 ${Constant.WebSiteUrl}refer/${binding.tvCode.text}"""
                    )
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            getString(R.string.invite_friend_title)
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.refer_code_alert_msg),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Constant.TOOLBAR_TITLE = getString(R.string.refer)
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
        menu.findItem(R.id.toolbar_cart).isVisible = false
        menu.findItem(R.id.toolbar_layout).isVisible = false
        menu.findItem(R.id.toolbar_search).isVisible = false
        menu.findItem(R.id.toolbar_sort).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}