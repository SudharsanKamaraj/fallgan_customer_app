package wrteam.multivendor.customer.model

import java.io.Serializable

class PriceVariation : Serializable {
    lateinit var id: String
    lateinit var product_id: String
    lateinit var type: String
    lateinit var measurement: String
    lateinit var price: String
    lateinit var discounted_price: String
    lateinit var serve_for: String
    lateinit var stock: String
    lateinit var measurement_unit_name: String
    lateinit var cart_count: String
    lateinit var images: ArrayList<String>
}