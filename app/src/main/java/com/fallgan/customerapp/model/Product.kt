package com.fallgan.customerapp.model

import java.io.Serializable
import kotlin.properties.Delegates

class Product : Serializable {
    lateinit var id: String
    lateinit var seller_id: String
    lateinit var name: String
    lateinit var slug: String
    lateinit var category_id: String
    lateinit var indicator: String
    lateinit var manufacturer: String
    lateinit var made_in: String
    lateinit var return_status: String
    lateinit var cancelable_status: String
    lateinit var till_status: String
    lateinit var image: String
    lateinit var description: String
    lateinit var status: String
    lateinit var return_days: String
    lateinit var type: String
    lateinit var seller_name: String
    lateinit var tax_percentage: String
    lateinit var total_allowed_quantity: String
    lateinit var variants: ArrayList<PriceVariation>
    lateinit var other_images: ArrayList<String>
    var is_favorite: Boolean = false
}