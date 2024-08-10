package com.fallgan.customerapp.helper

interface VolleyCallback {
    fun onSuccess(
        result: Boolean,
        response: String
    ) //void onSuccessWithMsg(boolean result, String message);
}