package wrteam.multivendor.customer.model

import java.io.Serializable

class Address : Serializable {
    lateinit var id: String
    lateinit var user_id: String
    lateinit var type: String
    lateinit var name: String
    lateinit var mobile: String
    lateinit var alternate_mobile: String
    lateinit var address: String
    lateinit var landmark: String
    lateinit var state: String
    lateinit var country: String
    lateinit var latitude: String
    lateinit var longitude: String
    lateinit var area_id: String
    lateinit var area: String
    lateinit var city_id: String
    lateinit var city: String
    lateinit var pincode_id: String
    lateinit var pincode: String
    lateinit var is_default: String
    var selected = false
}