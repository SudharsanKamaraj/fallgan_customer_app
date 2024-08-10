package com.fallgan.customerapp.model

import java.io.Serializable

class CartItems : Serializable {
    var is_item_deliverable: Boolean = false;
    lateinit var measurement: String
    lateinit var price: String
    lateinit var discounted_price: String
    lateinit var serve_for: String
    lateinit var stock: String
    lateinit var name: String
    lateinit var cod_allowed: String
    lateinit var image: String
    lateinit var tax_percentage: String
    lateinit var tax_title: String
    lateinit var unit: String
    lateinit var total_allowed_quantity: String
}