package com.gpn.customerapp.model

import java.io.Serializable

class OrderTracker : Serializable {
    lateinit var id: String
    lateinit var user_id: String
    lateinit var otp: String
    lateinit var mobile: String
    lateinit var order_note: String
    lateinit var total: String
    lateinit var delivery_charge: String
    lateinit var tax_percentage: String
    lateinit var wallet_balance: String
    lateinit var discount: String
    lateinit var promo_discount: String
    lateinit var final_total: String
    lateinit var payment_method: String
    lateinit var address: String
    lateinit var date_added: String
    lateinit var bank_transfer_message: String
    lateinit var bank_transfer_status: String
    lateinit var user_name: String
    lateinit var discount_rupees: String
    lateinit var attachment: ArrayList<Attachment>
    lateinit var items: ArrayList<OrderItems>
}