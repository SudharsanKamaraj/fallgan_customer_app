package wrteam.multivendor.customer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import wrteam.multivendor.customer.R

class SliderItemFragment : Fragment() {
    private var position = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            position = requireArguments().getInt(ARG_POSITION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slider_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set page background
        view.background = ContextCompat.getDrawable(
            requireActivity(),
            BG_IMAGE[position]
        )
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        // set page tvTitle
        tvTitle.setText(PAGE_TITLES[position])
        // set page sub tvTitle text
        tvMessage.setText(PAGE_TEXT[position])
        // set page image
        imageView.setImageResource(PAGE_IMAGE[position])
    }

    companion object {
        private const val ARG_POSITION = "slider-position"

        // prepare all tvTitle ids arrays
        @StringRes
        private val PAGE_TITLES = intArrayOf(R.string.discover, R.string.shop, R.string.offers)

        // prepare all subtitle ids arrays
        @StringRes
        private val PAGE_TEXT =
            intArrayOf(R.string.discover_text, R.string.shop_text, R.string.offers_text)

        // prepare all subtitle images arrays
        @StringRes
        private val PAGE_IMAGE =
            intArrayOf(R.drawable.intro_a, R.drawable.intro_b, R.drawable.intro_c)

        // prepare all background images arrays
        @StringRes
        private val BG_IMAGE = intArrayOf(R.color.white, R.color.white, R.color.white)

        /**
         * Use this factory method to create a new instance of
         *
         * @return A new instance of fragment SliderItemFragment.
         */
        fun newInstance(position: Int): SliderItemFragment {
            val fragment = SliderItemFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
}