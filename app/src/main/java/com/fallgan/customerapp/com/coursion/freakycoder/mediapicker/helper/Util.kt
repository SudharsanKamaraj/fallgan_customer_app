package com.fallgan.customerapp.com.coursion.freakycoder.mediapicker.helper

import android.content.res.ColorStateList
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Created by WrathChaos on 5.03.2018.
 */
class Util{
    fun setButtonTint(button: FloatingActionButton, tint: ColorStateList) {
        button.backgroundTintList = tint
    }
}