package com.example.farmdatapod

data class PaymentFarmer(
    var id: Int = 0,
    var buying_center: String? = null,
    var cig: String? = null,
    var producer: String? = null,
    var grn: String? = null,
    var net_balance: Double = 0.0,
    var payment_type: String? = null,
    var outstanding_loan_amount: Double = 0.0,
    var payment_due: Double = 0.0,
    var set_loan_deduction: Double = 0.0,
    var net_balance_before: Double = 0.0,
    var net_balance_after_loan_deduction: Double = 0.0,
    var comment: String? = null,
    var user_id: String? = null
)
