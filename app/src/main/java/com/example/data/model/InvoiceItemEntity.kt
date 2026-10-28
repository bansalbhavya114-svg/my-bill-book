package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long = 0,
    val itemId: Long? = null,
    val itemName: String,
    val hsnCode: String = "",
    val unit: String = "PCS",
    val quantity: Double = 1.0,
    val rate: Double = 0.0,
    val discountPercent: Double = 0.0,
    val taxRate: Double = 18.0,
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val totalAmount: Double = 0.0
)
