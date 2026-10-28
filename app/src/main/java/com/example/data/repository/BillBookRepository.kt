package com.example.data.repository

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItemEntity
import com.example.data.model.InvoiceWithItems
import com.example.data.model.ItemEntity
import com.example.data.model.PartyEntity
import com.example.data.model.PaymentEntity
import com.example.data.network.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class SyncSummary(
    val itemsCount: Int = 0,
    val partiesCount: Int = 0,
    val invoicesCount: Int = 0,
    val expensesCount: Int = 0,
    val paymentsCount: Int = 0,
    val businessSynced: Boolean = false,
    val errors: List<String> = emptyList()
) {
    val totalSynced: Int
        get() = itemsCount + partiesCount + invoicesCount + expensesCount + paymentsCount

    fun toDisplayString(): String {
        return "Synced $totalSynced records to Supabase ($itemsCount items, $partiesCount parties, $invoicesCount invoices, $expensesCount expenses)"
    }
}

class BillBookRepository(private val db: AppDatabase) {
    private val TAG = "BillBookRepository"

    private val businessDao = db.businessDao()
    private val itemDao = db.itemDao()
    private val partyDao = db.partyDao()
    private val invoiceDao = db.invoiceDao()
    private val paymentDao = db.paymentDao()
    private val expenseDao = db.expenseDao()

    // Business Profile
    val allBusinesses: Flow<List<BusinessProfileEntity>> = businessDao.getAllBusinesses()
    val currentBusiness: Flow<BusinessProfileEntity?> = businessDao.getCurrentBusiness()

    suspend fun insertBusiness(business: BusinessProfileEntity): Long {
        val id = businessDao.insert(business)
        syncBusinessProfileToSupabase(business.copy(id = id))
        return id
    }

    suspend fun updateBusiness(business: BusinessProfileEntity) {
        businessDao.update(business)
        syncBusinessProfileToSupabase(business)
    }

    suspend fun switchBusiness(id: Long) {
        businessDao.clearCurrentBusiness()
        businessDao.setCurrentBusiness(id)
    }

    // Items
    fun getItems(businessId: Long): Flow<List<ItemEntity>> = itemDao.getItemsByBusiness(businessId)
    fun getLowStockItems(businessId: Long): Flow<List<ItemEntity>> = itemDao.getLowStockItems(businessId)

    suspend fun insertItem(item: ItemEntity): Long {
        val id = itemDao.insert(item)
        val fullItem = item.copy(id = id)
        try {
            syncItemToSupabase(fullItem)
        } catch (e: Exception) {
            Log.w(TAG, "Item saved locally, Supabase sync deferred: ${e.message}")
        }
        return id
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.update(item)
        try {
            syncItemToSupabase(item)
        } catch (e: Exception) {
            Log.w(TAG, "Item updated locally, Supabase sync deferred: ${e.message}")
        }
    }

    suspend fun adjustStock(itemId: Long, delta: Double) {
        itemDao.adjustStock(itemId, delta)
        val item = itemDao.getItemById(itemId)
        if (item != null) {
            try {
                syncItemToSupabase(item)
            } catch (e: Exception) {
                Log.w(TAG, "Stock adjusted locally, Supabase sync deferred: ${e.message}")
            }
        }
    }

    suspend fun deleteItem(itemId: Long) {
        val item = itemDao.getItemById(itemId)
        itemDao.deleteById(itemId)
        if (item != null) {
            try {
                deleteItemFromSupabase(item.name)
            } catch (e: Exception) {
                Log.w(TAG, "Item deleted locally, Supabase sync deferred: ${e.message}")
            }
        }
    }

    // Parties
    fun getParties(businessId: Long): Flow<List<PartyEntity>> = partyDao.getPartiesByBusiness(businessId)
    fun getPartiesByType(businessId: Long, type: String): Flow<List<PartyEntity>> = partyDao.getPartiesByType(businessId, type)

    suspend fun insertParty(party: PartyEntity): Long {
        val id = partyDao.insert(party)
        val fullParty = party.copy(id = id)
        try {
            syncPartyToSupabase(fullParty)
        } catch (e: Exception) {
            Log.w(TAG, "Party saved locally, Supabase sync deferred: ${e.message}")
        }
        return id
    }

    suspend fun updateParty(party: PartyEntity) {
        partyDao.update(party)
        try {
            syncPartyToSupabase(party)
        } catch (e: Exception) {
            Log.w(TAG, "Party updated locally, Supabase sync deferred: ${e.message}")
        }
    }

    suspend fun deleteParty(partyId: Long) {
        val party = partyDao.getPartyById(partyId)
        partyDao.deleteById(partyId)
        if (party != null) {
            try {
                deletePartyFromSupabase(party.name)
            } catch (e: Exception) {
                Log.w(TAG, "Party deleted locally, Supabase sync deferred: ${e.message}")
            }
        }
    }

    suspend fun deleteAllParties(businessId: Long? = null) {
        if (businessId != null) {
            partyDao.deleteAllPartiesByBusiness(businessId)
        } else {
            partyDao.deleteAllParties()
        }
    }

    // Invoices & Checkout
    fun getInvoices(businessId: Long): Flow<List<InvoiceWithItems>> = invoiceDao.getInvoicesWithItems(businessId)
    suspend fun getInvoiceById(id: Long): InvoiceWithItems? = invoiceDao.getInvoiceWithItemsById(id)

    suspend fun deleteInvoice(id: Long) {
        val inv = invoiceDao.getInvoiceWithItemsById(id)
        invoiceDao.deleteInvoiceById(id)
        if (inv != null) {
            try {
                deleteInvoiceFromSupabase(inv.invoice.invoiceNumber)
            } catch (e: Exception) {
                Log.w(TAG, "Invoice deleted locally, Supabase sync deferred: ${e.message}")
            }
        }
    }

    suspend fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): Long {
        // 1. Local Room DB Persistence (Offline-First)
        val invoiceId = invoiceDao.saveFullInvoice(invoice, items)
        val fullInvoice = invoice.copy(id = invoiceId)

        // If it is a real sale invoice, reduce inventory stock
        if (invoice.type == "GST_INVOICE" || invoice.type == "BILL_OF_SUPPLY") {
            items.forEach { item ->
                item.itemId?.let { id ->
                    itemDao.adjustStock(id, -item.quantity)
                }
            }
            // Update party balance if credit sale
            invoice.partyId?.let { pId ->
                val unpaid = invoice.grandTotal - invoice.paidAmount
                if (unpaid > 0) {
                    partyDao.updateBalance(pId, unpaid)
                }
            }
            // Record payment if paid
            if (invoice.paidAmount > 0) {
                paymentDao.insert(
                    PaymentEntity(
                        businessId = invoice.businessId,
                        invoiceId = invoiceId,
                        partyId = invoice.partyId,
                        partyName = invoice.partyName,
                        type = "PAYMENT_IN",
                        amount = invoice.paidAmount,
                        paymentMode = invoice.paymentMode,
                        notes = "Auto payment recorded for ${invoice.invoiceNumber}",
                        date = invoice.date
                    )
                )
            }
        }

        // 2. Direct Cloud Sync of Checkout Data to Supabase Database
        try {
            syncCheckoutToSupabase(fullInvoice, items)
        } catch (e: Exception) {
            Log.w(TAG, "Offline checkout saved locally, Supabase sync deferred: ${e.message}")
        }

        return invoiceId
    }

    suspend fun recordInvoicePayment(
        invoiceId: Long,
        currentGrandTotal: Double,
        paymentAmount: Double,
        mode: String,
        partyId: Long?,
        partyName: String,
        businessId: Long
    ) {
        val existingInvoice = invoiceDao.getInvoiceById(invoiceId)
        val prevPaid = existingInvoice?.paidAmount ?: 0.0
        val deltaPayment = (paymentAmount - prevPaid).coerceAtLeast(0.0)
        val transactionAmount = if (deltaPayment > 0.0) deltaPayment else paymentAmount

        val newStatus = when {
            paymentAmount >= currentGrandTotal -> "PAID"
            paymentAmount > 0.0 -> "PARTIAL"
            else -> "UNPAID"
        }
        invoiceDao.updateInvoicePaymentStatus(invoiceId, newStatus, paymentAmount)
        val payment = PaymentEntity(
            businessId = businessId,
            invoiceId = invoiceId,
            partyId = partyId,
            partyName = partyName,
            type = "PAYMENT_IN",
            amount = transactionAmount,
            paymentMode = mode,
            notes = "Received against ${existingInvoice?.invoiceNumber ?: "invoice"}",
            date = System.currentTimeMillis()
        )
        paymentDao.insert(payment)
        if (partyId != null && transactionAmount > 0.0) {
            partyDao.updateBalance(partyId, -transactionAmount)
        }

        // Sync payment and invoice update to Supabase
        withContext(Dispatchers.IO) {
            try {
                syncPaymentToSupabase(payment)

                // Update invoice status in Supabase
                val updateObj = JSONObject().apply {
                    put("status", newStatus)
                    put("paid_amount", paymentAmount)
                }
                SupabaseClient.updateRecord("invoices", "invoice_number=eq.INV-$invoiceId", updateObj.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Supabase payment sync deferred: ${e.message}")
            }
        }
    }

    // Payments
    fun getPayments(businessId: Long): Flow<List<PaymentEntity>> = paymentDao.getPaymentsByBusiness(businessId)

    suspend fun insertPayment(payment: PaymentEntity): Long {
        val id = paymentDao.insert(payment)
        payment.partyId?.let { pId ->
            val delta = if (payment.type == "PAYMENT_IN") -payment.amount else payment.amount
            partyDao.updateBalance(pId, delta)
        }
        val fullPayment = payment.copy(id = id)
        try {
            syncPaymentToSupabase(fullPayment)
        } catch (e: Exception) {
            Log.w(TAG, "Payment saved locally, Supabase sync deferred: ${e.message}")
        }
        return id
    }

    // Expenses
    fun getExpenses(businessId: Long): Flow<List<ExpenseEntity>> = expenseDao.getExpensesByBusiness(businessId)

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        val id = expenseDao.insert(expense)
        val fullExpense = expense.copy(id = id)
        try {
            syncExpenseToSupabase(fullExpense)
        } catch (e: Exception) {
            Log.w(TAG, "Expense saved locally, Supabase sync deferred: ${e.message}")
        }
        return id
    }

    suspend fun deleteExpense(id: Long) = expenseDao.deleteById(id)

    // =========================================================================
    // Supabase Cloud Synchronization (PostgREST HTTPS API)
    // =========================================================================

    /**
     * Stores checkout invoice, line items, and payment directly in the Supabase PostgreSQL database.
     * Safely handles foreign keys so that invoice creation succeeds even if references are new.
     */
    suspend fun syncCheckoutToSupabase(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Resolve or create Supabase party_id to satisfy foreign key constraint
            var supabasePartyId: Long? = null
            if (!invoice.partyName.isNullOrBlank() && invoice.partyName != "Cash Customer") {
                try {
                    val encodedName = URLEncoder.encode(invoice.partyName, "UTF-8")
                    val existingPartyRes = SupabaseClient.queryTable("parties", "id", "name=eq.$encodedName")
                    val existingPartyText = existingPartyRes.getOrNull().orEmpty()
                    val pArray = JSONArray(existingPartyText)
                    if (pArray.length() > 0) {
                        supabasePartyId = pArray.getJSONObject(0).optLong("id")
                    } else {
                        // Create customer in Supabase so party_id exists
                        val partyObj = JSONObject().apply {
                            put("name", invoice.partyName)
                            put("phone", invoice.partyPhone)
                            put("gstin", invoice.partyGstin)
                            put("address", invoice.partyAddress)
                            put("state", invoice.partyState)
                            put("type", "CUSTOMER")
                            put("business_id", invoice.businessId)
                        }
                        val createdPartyRes = SupabaseClient.upsertRecord("parties", onConflict = "name", jsonPayload = partyObj.toString())
                        val createdPartyArr = JSONArray(createdPartyRes.getOrNull().orEmpty())
                        if (createdPartyArr.length() > 0) {
                            supabasePartyId = createdPartyArr.getJSONObject(0).optLong("id")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Party lookup/upsert for Supabase invoice: ${e.message}")
                }
            }

            val validDueDate = if (invoice.dueDate > 0L) invoice.dueDate else (invoice.date + 15 * 86400000L)
            val validDate = if (invoice.date > 0L) invoice.date else System.currentTimeMillis()

            // 2. Build Invoices table record
            val invoiceJson = JSONObject().apply {
                put("business_id", invoice.businessId)
                if (supabasePartyId != null && supabasePartyId > 0) {
                    put("party_id", supabasePartyId)
                }
                put("party_name", invoice.partyName)
                put("party_gstin", invoice.partyGstin)
                put("party_phone", invoice.partyPhone)
                put("party_address", invoice.partyAddress)
                put("party_state", invoice.partyState)
                put("invoice_number", invoice.invoiceNumber)
                put("type", invoice.type)
                put("date", validDate)
                put("due_date", validDueDate)
                put("subtotal", invoice.subtotal)
                put("total_discount", invoice.totalDiscount)
                put("cgst", invoice.cgstAmount)
                put("cgst_amount", invoice.cgstAmount)
                put("sgst", invoice.sgstAmount)
                put("sgst_amount", invoice.sgstAmount)
                put("igst", invoice.igstAmount)
                put("igst_amount", invoice.igstAmount)
                put("total_tax", invoice.totalTax)
                put("shipping_charges", invoice.shippingCharges)
                put("round_off", invoice.roundOff)
                put("grand_total", invoice.grandTotal)
                put("status", invoice.status)
                put("payment_mode", invoice.paymentMode)
                put("paid_amount", invoice.paidAmount)
                put("notes", invoice.notes)
                put("template_style", invoice.templateStyle)
            }.toString()

            val insertResult = SupabaseClient.insertRecord("invoices", invoiceJson)
            if (insertResult.isFailure) {
                val err = insertResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e(TAG, "Failed to insert invoice into Supabase: $err")
                return@withContext Result.failure(IOException(err))
            }

            val responseText = insertResult.getOrNull().orEmpty()
            var supabaseInvoiceId = invoice.id
            try {
                val array = JSONArray(responseText)
                if (array.length() > 0) {
                    val createdObj = array.getJSONObject(0)
                    supabaseInvoiceId = createdObj.optLong("id", invoice.id)
                }
            } catch (_: Exception) {}

            // 3. Build Invoice line items array
            if (items.isNotEmpty()) {
                val itemsArray = JSONArray()
                for (line in items) {
                    var supabaseItemId: Long? = null
                    try {
                        val encItemName = URLEncoder.encode(line.itemName, "UTF-8")
                        val itemRes = SupabaseClient.queryTable("items", "id", "name=eq.$encItemName")
                        val arr = JSONArray(itemRes.getOrNull().orEmpty())
                        if (arr.length() > 0) {
                            supabaseItemId = arr.getJSONObject(0).optLong("id")
                        }
                    } catch (_: Exception) {}

                    val lineObj = JSONObject().apply {
                        put("invoice_id", supabaseInvoiceId)
                        if (supabaseItemId != null && supabaseItemId > 0) {
                            put("item_id", supabaseItemId)
                        }
                        put("item_name", line.itemName)
                        put("hsn_code", line.hsnCode)
                        put("unit", line.unit)
                        put("quantity", line.quantity)
                        put("rate", line.rate)
                        put("discount_percent", line.discountPercent)
                        put("tax_rate", line.taxRate)
                        put("gst_rate", line.taxRate)
                        put("amount", line.taxableAmount)
                        put("taxable_amount", line.taxableAmount)
                        put("cgst", line.cgst)
                        put("sgst", line.sgst)
                        put("igst", line.igst)
                        put("total_amount", line.totalAmount)
                    }
                    itemsArray.put(lineObj)
                }
                SupabaseClient.insertRecord("invoice_items", itemsArray.toString())
            }

            // 4. Payment record
            if (invoice.paidAmount > 0) {
                val paymentObj = JSONObject().apply {
                    put("business_id", invoice.businessId)
                    put("invoice_id", supabaseInvoiceId)
                    if (supabasePartyId != null && supabasePartyId > 0) {
                        put("party_id", supabasePartyId)
                    }
                    put("party_name", invoice.partyName)
                    put("amount", invoice.paidAmount)
                    put("date", validDate)
                    put("payment_mode", invoice.paymentMode)
                    put("mode", invoice.paymentMode)
                    put("reference_no", invoice.invoiceNumber)
                    put("reference_number", invoice.invoiceNumber)
                    put("notes", "Auto payment for ${invoice.invoiceNumber}")
                }
                SupabaseClient.insertRecord("payments", paymentObj.toString())
            }

            Log.d(TAG, "Successfully synced invoice ${invoice.invoiceNumber} to Supabase.")
            Result.success("Synced to Supabase: ${invoice.invoiceNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Supabase checkout sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates an inventory item in Supabase 'items' table.
     */
    suspend fun syncItemToSupabase(item: ItemEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", item.name)
                put("sku", item.sku)
                put("hsn_code", item.hsnCode)
                put("category", item.category)
                put("unit", item.unit)
                put("mrp", item.mrp)
                put("sale_price", item.salePrice)
                put("purchase_price", item.purchasePrice)
                put("tax_rate", item.taxRate)
                put("stock_qty", item.stockQty)
                put("min_stock_alert", item.minStockAlert)
                put("business_id", item.businessId)
            }.toString()

            val res = SupabaseClient.upsertRecord("items", onConflict = "name", jsonPayload = json)
            if (res.isSuccess) {
                Log.d(TAG, "Upserted item '${item.name}' in Supabase items table")
                Result.success(true)
            } else {
                Log.w(TAG, "Failed upserting item '${item.name}': ${res.exceptionOrNull()?.message}")
                Result.failure(res.exceptionOrNull() ?: IOException("Sync failed"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Item sync exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteItemFromSupabase(name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            SupabaseClient.deleteRecord("items", "name=eq.$encodedName")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates a customer/supplier party in Supabase 'parties' table.
     */
    suspend fun syncPartyToSupabase(party: PartyEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", party.name)
                put("type", party.type)
                put("phone", party.phone)
                put("email", party.email)
                put("gstin", party.gstin)
                put("address", party.address)
                put("state", party.state)
                put("state_code", party.stateCode)
                put("opening_balance", party.openingBalance)
                put("current_balance", party.currentBalance)
                put("credit_limit", party.creditLimit)
                put("payment_terms_days", party.paymentTermsDays)
                put("business_id", party.businessId)
            }.toString()

            val res = SupabaseClient.upsertRecord("parties", onConflict = "name", jsonPayload = json)
            if (res.isSuccess) {
                Log.d(TAG, "Upserted party '${party.name}' in Supabase parties table")
                Result.success(true)
            } else {
                Log.w(TAG, "Failed upserting party '${party.name}': ${res.exceptionOrNull()?.message}")
                Result.failure(res.exceptionOrNull() ?: IOException("Sync failed"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Party sync exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deletePartyFromSupabase(name: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            SupabaseClient.deleteRecord("parties", "name=eq.$encodedName")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads an expense to Supabase 'expenses' table.
     */
    suspend fun syncExpenseToSupabase(expense: ExpenseEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("category", expense.category)
                put("amount", expense.amount)
                put("payment_mode", expense.paymentMode)
                put("paid_to", expense.paidTo)
                put("vendor_name", expense.paidTo)
                put("notes", expense.notes)
                put("date", expense.date)
                put("business_id", expense.businessId)
            }.toString()

            val res = SupabaseClient.insertRecord("expenses", json)
            if (res.isSuccess) {
                Log.d(TAG, "Synced expense '${expense.category}' (₹${expense.amount}) to Supabase")
                Result.success(true)
            } else {
                Log.w(TAG, "Failed syncing expense: ${res.exceptionOrNull()?.message}")
                Result.failure(res.exceptionOrNull() ?: IOException("Sync failed"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Expense sync exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads a payment to Supabase 'payments' table.
     */
    suspend fun syncPaymentToSupabase(payment: PaymentEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("amount", payment.amount)
                put("type", payment.type)
                put("payment_mode", payment.paymentMode)
                put("mode", payment.paymentMode)
                put("notes", payment.notes)
                put("date", payment.date)
                put("reference_no", payment.invoiceId?.let { "INV-$it" } ?: "")
                put("reference_number", payment.invoiceId?.let { "INV-$it" } ?: "")
                put("party_name", payment.partyName)
                put("business_id", payment.businessId)
            }.toString()

            val res = SupabaseClient.insertRecord("payments", json)
            if (res.isSuccess) {
                Log.d(TAG, "Synced payment of ₹${payment.amount} to Supabase")
                Result.success(true)
            } else {
                Log.w(TAG, "Failed syncing payment: ${res.exceptionOrNull()?.message}")
                Result.failure(res.exceptionOrNull() ?: IOException("Sync failed"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Payment sync exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteInvoiceFromSupabase(invoiceNumber: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encodedNum = URLEncoder.encode(invoiceNumber, "UTF-8")
            SupabaseClient.deleteRecord("invoices", "invoice_number=eq.$encodedNum")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads the business profile into Supabase 'businesses' table.
     */
    suspend fun syncBusinessProfileToSupabase(business: BusinessProfileEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val busObj = JSONObject().apply {
                put("business_name", business.businessName)
                put("trade_name", business.tradeName)
                put("email", business.email)
                put("phone", business.phone)
                put("gstin", business.gstin)
                put("business_type", business.businessType)
                put("turnover", business.turnover)
                put("address", business.address)
                put("state", business.state)
                put("state_code", business.stateCode)
                put("upi_id", business.upiId)
                put("bank_name", business.bankName)
                put("account_number", business.accountNumber)
                put("ifsc_code", business.ifscCode)
                put("invoice_prefix", business.invoicePrefix)
                put("currency_symbol", business.currencySymbol)
            }
            val res = SupabaseClient.upsertRecord("businesses", onConflict = "business_name", jsonPayload = busObj.toString())
            if (res.isFailure) {
                val res2 = SupabaseClient.upsertRecord("buisness", onConflict = "business_name", jsonPayload = busObj.toString())
                if (res2.isFailure) {
                    Log.w(TAG, "Business profile sync note: ${res.exceptionOrNull()?.message}")
                    return@withContext Result.failure(res.exceptionOrNull() ?: IOException("Upsert failed"))
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Business profile sync exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Performs a complete synchronization of all local data (Business, Items, Parties, Invoices, Expenses, Payments) to Supabase.
     */
    suspend fun syncAllDataToSupabase(businessId: Long): Result<SyncSummary> = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var itemsCount = 0
        var partiesCount = 0
        var invoicesCount = 0
        var expensesCount = 0
        var paymentsCount = 0
        var businessSynced = false

        // 1. Business Profile
        try {
            val business = currentBusiness.firstOrNull()
            if (business != null) {
                val bRes = syncBusinessProfileToSupabase(business)
                businessSynced = bRes.isSuccess
                if (!bRes.isSuccess) {
                    errors.add("Business profile note: ${bRes.exceptionOrNull()?.message}")
                }
            }
        } catch (e: Exception) {
            errors.add("Business: ${e.message}")
        }

        // 2. Items
        try {
            val allItems = itemDao.getItemsByBusiness(businessId).firstOrNull() ?: emptyList()
            for (item in allItems) {
                val res = syncItemToSupabase(item)
                if (res.isSuccess) itemsCount++
            }
        } catch (e: Exception) {
            errors.add("Items: ${e.message}")
        }

        // 3. Parties
        try {
            val allParties = partyDao.getPartiesByBusiness(businessId).firstOrNull() ?: emptyList()
            for (party in allParties) {
                val res = syncPartyToSupabase(party)
                if (res.isSuccess) partiesCount++
            }
        } catch (e: Exception) {
            errors.add("Parties: ${e.message}")
        }

        // 4. Invoices & Line Items
        try {
            val allInvoices = invoiceDao.getInvoicesWithItems(businessId).firstOrNull() ?: emptyList()
            for (invWithItems in allInvoices) {
                val res = syncCheckoutToSupabase(invWithItems.invoice, invWithItems.items)
                if (res.isSuccess) invoicesCount++
            }
        } catch (e: Exception) {
            errors.add("Invoices: ${e.message}")
        }

        // 5. Expenses
        try {
            val allExpenses = expenseDao.getExpensesByBusiness(businessId).firstOrNull() ?: emptyList()
            for (expense in allExpenses) {
                val res = syncExpenseToSupabase(expense)
                if (res.isSuccess) expensesCount++
            }
        } catch (e: Exception) {
            errors.add("Expenses: ${e.message}")
        }

        // 6. Payments
        try {
            val allPayments = paymentDao.getPaymentsByBusiness(businessId).firstOrNull() ?: emptyList()
            for (payment in allPayments) {
                val res = syncPaymentToSupabase(payment)
                if (res.isSuccess) paymentsCount++
            }
        } catch (e: Exception) {
            errors.add("Payments: ${e.message}")
        }

        val summary = SyncSummary(
            itemsCount = itemsCount,
            partiesCount = partiesCount,
            invoicesCount = invoicesCount,
            expensesCount = expensesCount,
            paymentsCount = paymentsCount,
            businessSynced = businessSynced,
            errors = errors
        )
        Result.success(summary)
    }

    suspend fun syncAllInvoicesToSupabase(businessId: Long): Result<Int> = withContext(Dispatchers.IO) {
        val summaryRes = syncAllDataToSupabase(businessId)
        if (summaryRes.isSuccess) {
            Result.success(summaryRes.getOrNull()?.totalSynced ?: 0)
        } else {
            Result.failure(summaryRes.exceptionOrNull() ?: IOException("Sync failed"))
        }
    }

    // Reset & Database wipe
    suspend fun purgeAllFakeUsersAndData() {
        partyDao.deleteAllParties()
        businessDao.deleteFakeBusinesses()
        invoiceDao.deleteAllInvoices()
        invoiceDao.deleteAllInvoiceItems()
        paymentDao.deleteAllPayments()
        expenseDao.deleteAllExpenses()
        itemDao.deleteAllItems()
    }

    suspend fun removeAllUsers(): Long {
        db.clearAllTables()
        return 0L
    }

    suspend fun clearAndResetAllData(newBusiness: BusinessProfileEntity? = null): Long {
        db.clearAllTables()
        if (newBusiness != null) {
            return businessDao.insert(newBusiness)
        }
        return 0L
    }
}
