package wrteam.multivendor.customer.model

import java.io.Serializable

class OrderItems : Serializable {
    lateinit var id: String
    lateinit var user_id: String
    lateinit var order_id: String
    lateinit var product_variant_id: String
    lateinit var quantity: String
    lateinit var price: String
    lateinit var discounted_price: String
    lateinit var tax_percentage: String
    lateinit var discount: String
    lateinit var active_status: String
    lateinit var date_added: String
    lateinit var shipping_method: String
    lateinit var name: String
    lateinit var image: String
    lateinit var return_status: String
    lateinit var cancelable_status: String
    lateinit var till_status: String
    lateinit var measurement: String
    lateinit var unit: String
    lateinit var return_days: String
    lateinit var shipment_id: String
}