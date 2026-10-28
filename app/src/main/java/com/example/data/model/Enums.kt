package com.example.data.model

enum class InvoiceType(val displayName: String, val code: String) {
    GST_INVOICE("Tax Invoice (GST)", "INV"),
    BILL_OF_SUPPLY("Bill of Supply", "BOS"),
    ESTIMATE_QUOTE("Quotation / Estimate", "EST"),
    PROFORMA("Proforma Invoice", "PRO"),
    CREDIT_NOTE("Credit Note", "CRN")
}

enum class InvoiceStatus(val displayName: String) {
    PAID("Paid"),
    UNPAID("Unpaid"),
    PARTIAL("Partial"),
    DRAFT("Draft")
}

enum class PartyType(val displayName: String) {
    CUSTOMER("Customer"),
    SUPPLIER("Supplier / Vendor")
}

enum class PaymentType(val displayName: String) {
    PAYMENT_IN("Payment In (Received)"),
    PAYMENT_OUT("Payment Out (Paid)")
}

enum class PaymentMode(val displayName: String) {
    CASH("Cash"),
    UPI("UPI"),
    BANK_TRANSFER("Bank Transfer / NEFT"),
    CHEQUE("Cheque"),
    CARD("Debit/Credit Card")
}

enum class TemplateStyle(val displayName: String) {
    MODERN("Modern Corporate"),
    CLASSIC_GST("Classic GST Format"),
    THERMAL("Compact 3-inch Thermal")
}

enum class UserRole(val displayName: String) {
    OWNER("Business Owner"),
    ADMIN("Admin"),
    STAFF("Cashier / Billing Staff")
}
