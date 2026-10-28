package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 1,
    val invoiceId: Long? = null,
    val partyId: Long? = null,
    val partyName: String,
    val type: String = "PAYMENT_IN", // PAYMENT_IN, PAYMENT_OUT
    val amount: Double,
    val paymentMode: String = "UPI", // CASH, UPI, BANK_TRANSFER, CHEQUE, CARD
    val referenceNo: String = "",
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)
