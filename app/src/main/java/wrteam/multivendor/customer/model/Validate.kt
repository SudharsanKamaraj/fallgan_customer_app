package com.gpn.customerapp.model

import java.io.Serializable

class Validate : Serializable {
    lateinit var error: String
    lateinit var message: String
    lateinit var discounted_amount: String
    lateinit var discount: String

}