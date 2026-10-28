package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessId: Long = 1,
    val invoiceNumber: String,
    val type: String = "GST_INVOICE", // GST_INVOICE, BILL_OF_SUPPLY, ESTIMATE_QUOTE, PROFORMA, CREDIT_NOTE
    val partyId: Long? = null,
    val partyName: String = "Cash Customer",
    val partyGstin: String = "",
    val partyPhone: String = "",
    val partyAddress: String = "",
    val partyState: String = "Delhi",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 15 * 86400000L,
    val status: String = "PAID", // PAID, UNPAID, PARTIAL, DRAFT
    val subtotal: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalTax: Double = 0.0,
    val shippingCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentMode: String = "CASH",
    val notes: String = "Thank you for your business!",
    val templateStyle: String = "MODERN"
)
