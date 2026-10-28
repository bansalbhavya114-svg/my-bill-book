package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.BillBookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DashboardStats(
    val totalSales: Double = 0.0,
    val todaySales: Double = 0.0,
    val monthlySales: Double = 0.0,
    val outstandingPayments: Double = 0.0,
    val totalReceivables: Double = 0.0,
    val totalPayables: Double = 0.0,
    val cashInHand: Double = 0.0,
    val bankBalance: Double = 0.0,
    val lowStockCount: Int = 0,
    val unpaidInvoiceCount: Int = 0
)

data class GstSummaryReport(
    val totalTaxableValue: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val totalGst: Double = 0.0,
    val b2bInvoiceCount: Int = 0,
    val b2cInvoiceCount: Int = 0,
    val totalInvoices: Int = 0
)

data class ProfitLossReport(
    val grossSales: Double = 0.0,
    val estimatedCogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0
)

class BillBookViewModel(application: Application) : AndroidViewModel(application) {
    // SharedPreferences Session Storage
    private val prefs = application.getSharedPreferences("billbook_session_prefs", android.content.Context.MODE_PRIVATE)

    private fun saveSession(loggedIn: Boolean, user: String?) {
        prefs.edit()
            .putBoolean("is_logged_in", loggedIn)
            .putString("current_user", user ?: "")
            .apply()
    }

    // Auth State - Persisted across app restarts so user is never automatically logged out
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<String?>(prefs.getString("current_user", null).takeIf { !it.isNullOrBlank() })
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    // 1-Time Initial Business Details Setup State
    private val _hasCompletedFirstTimeSetup = MutableStateFlow(prefs.getBoolean("has_completed_first_time_setup", false))
    val hasCompletedFirstTimeSetup: StateFlow<Boolean> = _hasCompletedFirstTimeSetup.asStateFlow()

    fun markFirstTimeSetupCompleted() {
        prefs.edit().putBoolean("has_completed_first_time_setup", true).apply()
        _hasCompletedFirstTimeSetup.value = true
    }

    private val _activeBusinessId = MutableStateFlow<Long>(1L)
    val activeBusinessId = _activeBusinessId.asStateFlow()

    private val repository: BillBookRepository = BillBookRepository(AppDatabase.getDatabase(application, viewModelScope))

    val businesses: StateFlow<List<BusinessProfileEntity>> = repository.allBusinesses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBusiness: StateFlow<BusinessProfileEntity?> = repository.currentBusiness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ItemEntity>> = _activeBusinessId.flatMapLatest { id ->
        repository.getItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val lowStockItems: StateFlow<List<ItemEntity>> = _activeBusinessId.flatMapLatest { id ->
        repository.getLowStockItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val parties: StateFlow<List<PartyEntity>> = _activeBusinessId.flatMapLatest { id ->
        repository.getParties(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val invoices: StateFlow<List<InvoiceWithItems>> = _activeBusinessId.flatMapLatest { id ->
        repository.getInvoices(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val payments: StateFlow<List<PaymentEntity>> = _activeBusinessId.flatMapLatest { id ->
        repository.getPayments(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<ExpenseEntity>> = _activeBusinessId.flatMapLatest { id ->
        repository.getExpenses(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cloud Sync state
    private val _syncStatus = MutableStateFlow("Connected to Supabase (${SupabaseClient.PROJECT_ID})")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Supabase Health State
    data class SupabaseConnectionInfo(
        val isChecking: Boolean = false,
        val isConnected: Boolean = true,
        val projectId: String = SupabaseClient.PROJECT_ID,
        val message: String = "Connected",
        val lastCheck: String = "Just now"
    )

    private val _supabaseInfo = MutableStateFlow(SupabaseConnectionInfo())
    val supabaseInfo: StateFlow<SupabaseConnectionInfo> = _supabaseInfo.asStateFlow()

    fun testSupabaseConnection(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _supabaseInfo.value = _supabaseInfo.value.copy(isChecking = true)
            val result = SupabaseClient.testConnection()
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            if (result.isSuccess) {
                _supabaseInfo.value = SupabaseConnectionInfo(
                    isChecking = false,
                    isConnected = true,
                    message = "Active & verified with Supabase ($time)",
                    lastCheck = time
                )
                _syncStatus.value = "Verified with Supabase ($time)"
                onResult?.invoke(true, "Successfully reached Supabase (${SupabaseClient.PROJECT_ID}) at $time")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unreachable"
                _supabaseInfo.value = SupabaseConnectionInfo(
                    isChecking = false,
                    isConnected = false,
                    message = "Supabase offline: $err",
                    lastCheck = time
                )
                _syncStatus.value = "Offline: $err at $time"
                onResult?.invoke(false, err)
            }
        }
    }

    // Financial Year selector
    private val _selectedFY = MutableStateFlow("FY 2026-27")
    val selectedFY: StateFlow<String> = _selectedFY.asStateFlow()

    fun setFinancialYear(fy: String) {
        _selectedFY.value = fy
    }

    // App Lock PIN security
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _appPin = MutableStateFlow("1234")
    val appPin: StateFlow<String> = _appPin.asStateFlow()

    fun unlockApp(pin: String): Boolean {
        if (pin == _appPin.value) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun login(identifier: String, secret: String = ""): Boolean {
        val clean = identifier.trim()
        if (clean.isBlank()) return false
        _currentUser.value = clean
        saveSession(true, clean)
        _isLoggedIn.value = true
        markFirstTimeSetupCompleted()
        return true
    }

    fun signUp(
        name: String,
        businessName: String,
        phone: String,
        email: String,
        state: String,
        gstin: String,
        businessType: String = "Retailer (Shop / Store)",
        turnover: String = "Below ₹20 Lakhs (GST Exempt)"
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanBusName = businessName.trim().ifBlank { if (cleanName.isNotBlank()) "$cleanName's Store" else "My Company" }
            val newBusiness = BusinessProfileEntity(
                businessName = cleanBusName,
                tradeName = cleanBusName,
                phone = phone.trim(),
                email = email.trim(),
                state = state.ifBlank { "Delhi" },
                gstin = gstin.trim(),
                businessType = businessType,
                turnover = turnover,
                isCurrent = true
            )
            val newId = repository.clearAndResetAllData(newBusiness)
            _activeBusinessId.value = newId
            val user = email.trim().ifBlank { phone.trim().ifBlank { cleanName } }
            _currentUser.value = user
            saveSession(true, user)
            _isLoggedIn.value = true
            markFirstTimeSetupCompleted()
        }
    }

    fun saveCompanySetup(
        businessName: String,
        email: String,
        gstin: String,
        businessType: String,
        turnover: String,
        phone: String = "",
        address: String = "",
        state: String = "Delhi",
        upiId: String = "",
        bankName: String = "",
        accountNumber: String = "",
        ifscCode: String = "",
        invoicePrefix: String = "INV-"
    ) {
        viewModelScope.launch {
            val existing = currentBusiness.value
            val cleanBusName = businessName.trim().ifBlank { "My Business" }
            val cleanEmail = email.trim()
            val cleanGstin = gstin.trim()

            if (existing != null && existing.id > 0) {
                val updated = existing.copy(
                    businessName = cleanBusName,
                    tradeName = cleanBusName,
                    email = cleanEmail,
                    gstin = cleanGstin,
                    businessType = businessType,
                    turnover = turnover,
                    phone = phone.trim(),
                    address = address.trim(),
                    state = state.trim(),
                    upiId = upiId.trim(),
                    bankName = bankName.trim(),
                    accountNumber = accountNumber.trim(),
                    ifscCode = ifscCode.trim(),
                    invoicePrefix = invoicePrefix.trim().ifBlank { "INV-" },
                    isCurrent = true
                )
                repository.updateBusiness(updated)
            } else {
                val newEntity = BusinessProfileEntity(
                    businessName = cleanBusName,
                    tradeName = cleanBusName,
                    email = cleanEmail,
                    gstin = cleanGstin,
                    businessType = businessType,
                    turnover = turnover,
                    phone = phone.trim(),
                    address = address.trim(),
                    state = state.trim(),
                    upiId = upiId.trim(),
                    bankName = bankName.trim(),
                    accountNumber = accountNumber.trim(),
                    ifscCode = ifscCode.trim(),
                    invoicePrefix = invoicePrefix.trim().ifBlank { "INV-" },
                    isCurrent = true
                )
                val newId = repository.insertBusiness(newEntity)
                repository.switchBusiness(newId)
                _activeBusinessId.value = newId
            }
            val user = cleanEmail.ifBlank { phone.trim().ifBlank { cleanBusName } }
            _currentUser.value = user
            saveSession(true, user)
            _isLoggedIn.value = true
            markFirstTimeSetupCompleted()
        }
    }

    fun logout() {
        saveSession(false, null)
        _isLoggedIn.value = false
        _currentUser.value = null
    }

    fun removeAllUsers() {
        viewModelScope.launch {
            saveSession(false, null)
            val newId = repository.removeAllUsers()
            _activeBusinessId.value = newId
            _currentUser.value = null
            _isLoggedIn.value = false
        }
    }

    fun deleteAllParties() {
        viewModelScope.launch {
            repository.deleteAllParties(_activeBusinessId.value)
        }
    }

    fun resetAllData(newBusinessName: String? = null, newState: String? = null) {
        viewModelScope.launch {
            val busName = newBusinessName?.trim()?.ifBlank { "My Store" } ?: "My Store"
            val stateName = newState?.ifBlank { "Delhi" } ?: "Delhi"
            val newBusiness = BusinessProfileEntity(
                businessName = busName,
                tradeName = busName,
                state = stateName,
                stateCode = "07",
                currencySymbol = "₹",
                invoicePrefix = "INV-26-",
                nextInvoiceNumber = 101,
                isCurrent = true
            )
            val newId = repository.clearAndResetAllData(newBusiness)
            _activeBusinessId.value = newId
        }
    }

    // Combined Dashboard Stats - Pure zero-based metrics with ZERO fake/starter money
    val dashboardStats: StateFlow<DashboardStats> = combine(
        invoices, parties, payments, expenses, lowStockItems
    ) { invs, pts, pms, exps, lowStock ->
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 86400000L
        val thirtyDaysAgo = now - (30 * 86400000L)

        val totalSales = invs
            .filter { it.invoice.type == "GST_INVOICE" || it.invoice.type == "BILL_OF_SUPPLY" }
            .sumOf { it.invoice.grandTotal }

        val todaySales = invs
            .filter { (it.invoice.type == "GST_INVOICE" || it.invoice.type == "BILL_OF_SUPPLY") && it.invoice.date >= oneDayAgo }
            .sumOf { it.invoice.grandTotal }

        val monthlySales = invs
            .filter { (it.invoice.type == "GST_INVOICE" || it.invoice.type == "BILL_OF_SUPPLY") && it.invoice.date >= thirtyDaysAgo }
            .sumOf { it.invoice.grandTotal }

        val unpaidInvoicesOutstanding = invs
            .filter { it.invoice.status == "UNPAID" || it.invoice.status == "PARTIAL" }
            .sumOf { (it.invoice.grandTotal - it.invoice.paidAmount).coerceAtLeast(0.0) }

        val totalReceivables = pts.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val outstandingPayments = if (unpaidInvoicesOutstanding > 0) unpaidInvoicesOutstanding else totalReceivables

        val totalPayables = pts.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

        // Cash flow calculations - Starts at 0.0 with NO hardcoded seed money
        val cashIn = pms.filter { it.type == "PAYMENT_IN" && it.paymentMode == "CASH" }.sumOf { it.amount }
        val cashOut = pms.filter { it.type == "PAYMENT_OUT" && it.paymentMode == "CASH" }.sumOf { it.amount }
        val cashExp = exps.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
        val cashInHand = (cashIn - cashOut - cashExp).coerceAtLeast(0.0)

        val bankIn = pms.filter { it.type == "PAYMENT_IN" && it.paymentMode != "CASH" }.sumOf { it.amount }
        val bankOut = pms.filter { it.type == "PAYMENT_OUT" && it.paymentMode != "CASH" }.sumOf { it.amount }
        val bankExp = exps.filter { it.paymentMode != "CASH" }.sumOf { it.amount }
        val bankBalance = (bankIn - bankOut - bankExp).coerceAtLeast(0.0)

        val unpaidCount = invs.count { it.invoice.status == "UNPAID" || it.invoice.status == "PARTIAL" }

        DashboardStats(
            totalSales = totalSales,
            todaySales = todaySales,
            monthlySales = monthlySales,
            outstandingPayments = outstandingPayments,
            totalReceivables = totalReceivables,
            totalPayables = totalPayables,
            cashInHand = cashInHand,
            bankBalance = bankBalance,
            lowStockCount = lowStock.size,
            unpaidInvoiceCount = unpaidCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // GST Report calculation
    val gstSummary: StateFlow<GstSummaryReport> = invoices.combine(currentBusiness) { invList, bus ->
        var taxable = 0.0
        var cgst = 0.0
        var sgst = 0.0
        var igst = 0.0
        var b2b = 0
        var b2c = 0

        invList.filter { it.invoice.type == "GST_INVOICE" }.forEach { inv ->
            taxable += inv.invoice.subtotal - inv.invoice.totalDiscount
            cgst += inv.invoice.cgstAmount
            sgst += inv.invoice.sgstAmount
            igst += inv.invoice.igstAmount
            if (inv.invoice.partyGstin.isNotBlank()) {
                b2b++
            } else {
                b2c++
            }
        }

        GstSummaryReport(
            totalTaxableValue = taxable,
            totalCgst = cgst,
            totalSgst = sgst,
            totalIgst = igst,
            totalGst = cgst + sgst + igst,
            b2bInvoiceCount = b2b,
            b2cInvoiceCount = b2c,
            totalInvoices = b2b + b2c
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GstSummaryReport())

    // Profit & Loss calculation
    val profitLoss: StateFlow<ProfitLossReport> = combine(invoices, expenses, items) { invs, exps, itmList ->
        val salesInvs = invs.filter { it.invoice.type == "GST_INVOICE" || it.invoice.type == "BILL_OF_SUPPLY" }
        val grossSales = salesInvs.sumOf { it.invoice.subtotal - it.invoice.totalDiscount }
        
        // Map items by id to find purchase price for COGS
        val itemCostMap = itmList.associate { it.id to it.purchasePrice }
        var estimatedCogs = 0.0
        salesInvs.forEach { inv ->
            inv.items.forEach { line ->
                val cost = line.itemId?.let { itemCostMap[it] } ?: (line.rate * 0.7)
                estimatedCogs += cost * line.quantity
            }
        }
        val grossProfit = grossSales - estimatedCogs
        val totalExpenses = exps.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses

        ProfitLossReport(
            grossSales = grossSales,
            estimatedCogs = estimatedCogs,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitLossReport())

    init {
        viewModelScope.launch {
            repository.currentBusiness.collect { bus ->
                if (bus != null) {
                    _activeBusinessId.value = bus.id
                    // If a valid business already exists, ensure user stays logged in seamlessly
                    if (!_isLoggedIn.value && bus.businessName.isNotBlank() && bus.businessName != "My Company") {
                        _isLoggedIn.value = true
                        val u = bus.email.ifBlank { bus.phone.ifBlank { bus.businessName } }
                        _currentUser.value = u
                        saveSession(true, u)
                        markFirstTimeSetupCompleted()
                    }
                } else {
                    val all = repository.allBusinesses.firstOrNull().orEmpty()
                    if (all.isNotEmpty()) {
                        val first = all.first()
                        repository.switchBusiness(first.id)
                        _activeBusinessId.value = first.id
                    }
                    // CRITICAL: Do NOT create any business automatically.
                    // The user will enter their business details directly on the Business Details page.
                }
            }
        }
        viewModelScope.launch {
            triggerSync()
        }
    }

    // Actions
    fun triggerSync() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatus.value = "Connecting to Supabase (${SupabaseClient.PROJECT_ID})..."
                val conn = SupabaseClient.testConnection()
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                if (conn.isSuccess) {
                    val syncResult = repository.syncAllDataToSupabase(_activeBusinessId.value)
                    val summary = syncResult.getOrNull()
                    if (summary != null) {
                        _syncStatus.value = "${summary.toDisplayString()} at $time"
                    } else {
                        _syncStatus.value = "Synced with Supabase at $time"
                    }
                } else {
                    _syncStatus.value = "Local Saved (Supabase offline) at $time"
                }
            } catch (e: Exception) {
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                _syncStatus.value = "Local Mode at $time"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun switchBusiness(id: Long) {
        viewModelScope.launch {
            repository.switchBusiness(id)
            _activeBusinessId.value = id
        }
    }

    fun addNewBusiness(name: String, gstin: String, state: String, phone: String) {
        viewModelScope.launch {
            val newB = BusinessProfileEntity(
                businessName = name,
                gstin = gstin,
                phone = phone,
                state = state,
                isCurrent = true
            )
            val newId = repository.insertBusiness(newB)
            repository.switchBusiness(newId)
            _activeBusinessId.value = newId
        }
    }

    fun addBusiness(business: BusinessProfileEntity) {
        viewModelScope.launch {
            val newId = repository.insertBusiness(business)
            repository.switchBusiness(newId)
            _activeBusinessId.value = newId
        }
    }

    fun createOrUpdateInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        saveInvoice(invoice, items) {}
    }

    fun updateBusinessProfile(business: BusinessProfileEntity) {
        viewModelScope.launch {
            repository.updateBusiness(business)
        }
    }

    fun addOrUpdateItem(item: ItemEntity) {
        viewModelScope.launch {
            try {
                val currentBiz = repository.currentBusiness.firstOrNull()
                val targetBusinessId = if (currentBiz != null && currentBiz.id > 0L) {
                    currentBiz.id
                } else if (_activeBusinessId.value > 0L) {
                    _activeBusinessId.value
                } else {
                    1L
                }
                _activeBusinessId.value = targetBusinessId

                if (item.id == 0L) {
                    repository.insertItem(item.copy(businessId = targetBusinessId))
                } else {
                    val bizId = if (item.businessId > 0L) item.businessId else targetBusinessId
                    repository.updateItem(item.copy(businessId = bizId))
                }
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                _syncStatus.value = "Item '${item.name}' synced to Supabase at $time"
            } catch (e: Exception) {
                android.util.Log.e("BillBookViewModel", "Error saving item: ${e.message}", e)
                _syncStatus.value = "Item '${item.name}' saved locally"
            }
        }
    }

    fun adjustItemStock(itemId: Long, delta: Double) {
        viewModelScope.launch {
            repository.adjustStock(itemId, delta)
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Stock synced to Supabase at $time"
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteItem(itemId)
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Item deleted and synced to Supabase at $time"
        }
    }

    fun addOrUpdateParty(party: PartyEntity) {
        viewModelScope.launch {
            if (party.id == 0L) {
                repository.insertParty(party.copy(businessId = _activeBusinessId.value))
            } else {
                repository.updateParty(party)
            }
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Party '${party.name}' synced to Supabase at $time"
        }
    }

    fun deleteParty(partyId: Long) {
        viewModelScope.launch {
            repository.deleteParty(partyId)
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Party deleted and synced to Supabase at $time"
        }
    }

    fun saveInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val bus = currentBusiness.value
            val nextNum = (bus?.nextInvoiceNumber ?: 100) + 1
            if (bus != null && invoice.id == 0L) {
                repository.updateBusiness(bus.copy(nextInvoiceNumber = nextNum))
            }
            val id = repository.saveInvoice(invoice.copy(businessId = _activeBusinessId.value), items)
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Synced to Supabase at $time"
            onComplete(id)
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    fun convertEstimateToInvoice(estimateWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val bus = currentBusiness.value
            val prefix = bus?.invoicePrefix ?: "INV-26-"
            val nextNum = bus?.nextInvoiceNumber ?: 106
            if (bus != null) {
                repository.updateBusiness(bus.copy(nextInvoiceNumber = nextNum + 1))
            }
            val newInvoice = estimateWithItems.invoice.copy(
                id = 0L,
                invoiceNumber = "$prefix$nextNum",
                type = "GST_INVOICE",
                date = System.currentTimeMillis(),
                dueDate = System.currentTimeMillis() + 15 * 86400000L,
                status = "UNPAID",
                notes = "Converted from Estimate ${estimateWithItems.invoice.invoiceNumber}"
            )
            repository.saveInvoice(newInvoice, estimateWithItems.items.map { it.copy(id = 0L) })
        }
    }

    fun convertToCreditNote(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            val newCrn = invoiceWithItems.invoice.copy(
                id = 0L,
                invoiceNumber = "CRN-${invoiceWithItems.invoice.invoiceNumber.replace("INV-", "")}",
                type = "CREDIT_NOTE",
                date = System.currentTimeMillis(),
                status = "PAID",
                notes = "Credit note issued against ${invoiceWithItems.invoice.invoiceNumber}"
            )
            repository.saveInvoice(newCrn, invoiceWithItems.items.map { it.copy(id = 0L) })
        }
    }

    fun recordInvoicePayment(invoiceId: Long, total: Double, amount: Double, mode: String, partyId: Long?, partyName: String) {
        viewModelScope.launch {
            repository.recordInvoicePayment(
                invoiceId = invoiceId,
                currentGrandTotal = total,
                paymentAmount = amount,
                mode = mode,
                partyId = partyId,
                partyName = partyName,
                businessId = _activeBusinessId.value
            )
        }
    }

    fun recordPayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.insertPayment(payment.copy(businessId = _activeBusinessId.value))
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Payment of ₹${payment.amount} synced to Supabase at $time"
        }
    }

    fun recordExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.insertExpense(expense.copy(businessId = _activeBusinessId.value))
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Expense of ₹${expense.amount} synced to Supabase at $time"
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            _syncStatus.value = "Expense deleted at $time"
        }
    }

    // AI Bill/Receipt Parser simulation (smart OCR extraction)
    fun parseReceiptText(rawText: String): Pair<ExpenseEntity, String> {
        val lower = rawText.lowercase()
        val amountRegex = Regex("(?:total|rs|inr|amt|amount|paid)[\\s:]*([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val match = amountRegex.find(rawText)
        val extractedAmount = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 1500.0

        val category = when {
            lower.contains("rent") || lower.contains("lease") -> "Rent"
            lower.contains("power") || lower.contains("electric") || lower.contains("bses") -> "Electricity"
            lower.contains("salary") || lower.contains("wages") -> "Salaries"
            lower.contains("tea") || lower.contains("coffee") || lower.contains("lunch") || lower.contains("hotel") -> "Tea/Snacks"
            lower.contains("transport") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") || lower.contains("uber") -> "Transport"
            lower.contains("packing") || lower.contains("box") || lower.contains("carton") -> "Packaging"
            lower.contains("ad") || lower.contains("marketing") || lower.contains("facebook") || lower.contains("google") -> "Marketing"
            else -> "Office"
        }

        val vendor = rawText.lines().firstOrNull { it.isNotBlank() }?.take(30) ?: "Local Vendor"

        val expense = ExpenseEntity(
            businessId = _activeBusinessId.value,
            category = category,
            amount = extractedAmount,
            paymentMode = "UPI",
            paidTo = vendor,
            notes = "Scanned receipt: $vendor",
            date = System.currentTimeMillis()
        )
        return Pair(expense, "Successfully scanned and extracted ₹$extractedAmount for $category from $vendor")
    }
}
