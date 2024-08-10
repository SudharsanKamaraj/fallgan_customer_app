package com.fallgan.customerapp.model

class Cart {
    lateinit var product_id: String
    lateinit var product_variant_id: String
    lateinit var qty: String
    lateinit var save_for_later: String
    lateinit var item: ArrayList<CartItems>
}