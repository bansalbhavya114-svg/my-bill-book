package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessName: String,
    val tradeName: String = "",
    val gstin: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val state: String = "Delhi",
    val stateCode: String = "07",
    val businessType: String = "Retail",
    val turnover: String = "Less than ₹20 Lakhs",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val invoicePrefix: String = "INV-26-",
    val nextInvoiceNumber: Int = 101,
    val currencySymbol: String = "₹",
    val termsConditions: String = "1. Goods once sold will not be taken back.\n2. Interest @18% p.a. will be charged if bill is not paid on due date.\n3. Subject to local jurisdiction only.",
    val signatureTitle: String = "Authorized Signatory",
    val role: String = "OWNER",
    val financialYear: String = "FY 2026-27",
    val isCurrent: Boolean = true
)
