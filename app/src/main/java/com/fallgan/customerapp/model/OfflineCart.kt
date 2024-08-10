package com.fallgan.customerapp.model

class OfflineCart {
    lateinit var id: String
    lateinit var product_id: String
    lateinit var type: String
    lateinit var measurement: String
    lateinit var price: String
    lateinit var discounted_price: String
    lateinit var product_variant_id: String
    lateinit var item: ArrayList<OfflineItems>
}