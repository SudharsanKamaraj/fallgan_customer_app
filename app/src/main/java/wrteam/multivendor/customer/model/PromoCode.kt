package wrteam.multivendor.customer.model

import java.io.Serializable

class PromoCode : Serializable {
    lateinit var promo_code: String
    lateinit var is_validate: ArrayList<Validate>
    lateinit var message: String
    lateinit var discount: String
}