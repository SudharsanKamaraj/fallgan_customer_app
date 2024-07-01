package com.gpn.customerapp.com.coursion.freakycoder.mediapicker.helper

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

/**
 * Created by Lincoln on 05/04/16.
 */
class SquareLayout : RelativeLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Set a square layout.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}
