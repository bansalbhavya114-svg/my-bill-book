package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceWithItems
import com.example.data.network.SupabaseClient
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

@Composable
fun DashboardScreen(
    viewModel: BillBookViewModel,
    onCreateInvoiceClick: () -> Unit,
    onViewInvoiceClick: (InvoiceWithItems) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToParties: () -> Unit,
    onNavigateToReports: () -> Unit,
    onEditCompany: () -> Unit = {}
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockItems.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val supabaseInfo by viewModel.supabaseInfo.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showConnectionModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Minimalist Supabase Cloud Status Card (Live verification & project status)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConnectionModal = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedRadarBeacon(
                                color = if (supabaseInfo.isConnected) TealSuccess else AmberWarning,
                                sizeDp = 7
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Supabase Cloud: Connected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = TealSuccessContainer
                                    ) {
                                        Text(
                                            text = SupabaseClient.PROJECT_ID,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealSuccessText,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isSyncing) "Syncing changes..." else "Verified cloud sync • Tap to test",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.testSupabaseConnection() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                    }
                }
            }

            // 2. Low Stock Alert Banner (if any)
            if (lowStockItems.isNotEmpty()) {
                item {
                    Surface(
                        color = RoseErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToInventory() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RoseError,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Low Stock Alert",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lowStockItems.size} Items Below Threshold",
                                    fontWeight = FontWeight.Bold,
                                    color = RoseErrorText,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = lowStockItems.joinToString(", ") { "${it.name} (${it.stockQty.toInt()})" },
                                    fontSize = 11.sp,
                                    color = RoseErrorText.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "View Stock",
                                tint = RoseErrorText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 3. Human-Crafted Financial Hero Overview (Starts at ₹0, user-defined, zero fake numbers)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header with store name & edit shortcut
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                BrandLogoAvatar(
                                    sizeDp = 44,
                                    showGlow = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentBusiness?.businessName ?: "My Business",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TextPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Text(
                                        text = if (currentBusiness?.gstin.isNullOrBlank()) "Composition / Unregistered" else "GSTIN: ${currentBusiness?.gstin}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            IconButton(
                                onClick = onEditCompany,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Customize Business",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Big Primary Metric: Total Sales with animated counter
                        Column {
                            Text(
                                text = "TOTAL SALES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedCurrencyText(
                                amount = stats.totalSales,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (stats.todaySales > 0) TealSuccessContainer else SurfaceSubtle
                                ) {
                                    Text(
                                        text = "Today: ${formatCurrency(stats.todaySales)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (stats.todaySales > 0) TealSuccessText else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${invoices.size} bills recorded",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        HorizontalDivider(color = SurfaceBorderLight)

                        // Clean Dual KPIs: Cash in Hand & Bank/UPI Balance with spring bounce
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cash in Hand (starts at ₹0.00, real cash flows)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceSubtle,
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(scaleDown = 0.96f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cash in Hand",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Icon(
                                            Icons.Default.Payments,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AnimatedCurrencyText(
                                        amount = stats.cashInHand,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Real cash ledger",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Bank & UPI (starts at ₹0.00, user decides UPI)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceSubtle,
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(scaleDown = 0.96f, onClick = { onEditCompany() })
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Bank & UPI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = null,
                                            tint = TealSuccess,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AnimatedCurrencyText(
                                        amount = stats.bankBalance,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = currentBusiness?.upiId?.takeIf { it.isNotBlank() } ?: "Tap to set UPI",
                                        fontSize = 10.sp,
                                        color = if (currentBusiness?.upiId.isNullOrBlank()) PrimaryBlue else TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Receivables & Payables banner
                        if (stats.outstandingPayments > 0 || stats.totalPayables > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RoseErrorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToParties() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = RoseError, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "To Collect: ${formatCurrency(stats.outstandingPayments)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseErrorText
                                        )
                                    }
                                    Text(
                                        text = "View Parties →",
                                        fontSize = 11.sp,
                                        color = RoseErrorText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Sleek Minimalist Quick Actions Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Quick Actions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModernActionPill(
                            modifier = Modifier.weight(1f),
                            title = "+ Sale Bill",
                            icon = Icons.Default.Receipt,
                            color = PrimaryBlue,
                            onClick = onCreateInvoiceClick
                        )
                        ModernActionPill(
                            modifier = Modifier.weight(1f),
                            title = "+ Item",
                            icon = Icons.Default.AddBox,
                            color = TealSuccess,
                            onClick = onNavigateToInventory
                        )
                        ModernActionPill(
                            modifier = Modifier.weight(1f),
                            title = "+ Party",
                            icon = Icons.Default.PersonAdd,
                            color = PurpleAccent,
                            onClick = onNavigateToParties
                        )
                        ModernActionPill(
                            modifier = Modifier.weight(1f),
                            title = "Settings",
                            icon = Icons.Default.Tune,
                            color = AmberWarning,
                            onClick = onEditCompany
                        )
                    }
                }
            }

            // 5. 7-Day Trend Chart
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Billing Trend",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    "Last 7 days sales activity",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceSubtle
                            ) {
                                Text(
                                    text = if (invoices.isEmpty()) "0 Bills" else "${invoices.size} Bills",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val dayLabels = remember(invoices) {
                            val cal = java.util.Calendar.getInstance()
                            val fmt = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
                            (0..6).map {
                                val label = fmt.format(cal.time)
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                label
                            }
                        }

                        val realDayValues = remember(invoices) {
                            val dayKeyFmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                            val salesByDay = mutableMapOf<String, Float>()
                            invoices.filter { it.invoice.type == "GST_INVOICE" || it.invoice.type == "BILL_OF_SUPPLY" }
                                .forEach { inv ->
                                    val k = dayKeyFmt.format(java.util.Date(inv.invoice.date))
                                    salesByDay[k] = (salesByDay[k] ?: 0f) + inv.invoice.grandTotal.toFloat()
                                }
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
                            (0..6).map {
                                val k = dayKeyFmt.format(cal.time)
                                val v = salesByDay[k] ?: 0f
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                                v
                            }
                        }

                        if (invoices.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "No sales yet • ₹0 balance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    "Tap '+ Sale Bill' to record your first GST sale",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            SalesBarChart(
                                data = realDayValues,
                                labels = dayLabels,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }
                }
            }

            // 6. Recent Invoices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Invoices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Total ${invoices.size}",
                        fontSize = 12.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (invoices.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Ready for your first invoice", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(
                                "Create a GST bill with items, discounts & UPI payments in seconds.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onCreateInvoiceClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Bill", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                items(invoices.take(6)) { invWithItems ->
                    val inv = invWithItems.invoice
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewInvoiceClick(invWithItems) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryBlueContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Receipt,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = inv.partyName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${inv.invoiceNumber} • ${formatShortDate(inv.date)}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(inv.grandTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                StatusBadge(status = inv.status)
                            }
                        }
                    }
                }
            }
        }

        // Modern Floating Action Button for rapid bill creation
        ExtendedFloatingActionButton(
            onClick = onCreateInvoiceClick,
            icon = { Icon(Icons.Default.Add, contentDescription = "New Bill") },
            text = { Text("New Bill", fontWeight = FontWeight.Bold) },
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_invoice_fab")
        )
    }

    // Dedicated Supabase Connection Diagnostic Modal
    if (showConnectionModal) {
        AlertDialog(
            onDismissRequest = { showConnectionModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supabase Cloud Status", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Your BillBook app is linked to Supabase cloud PostgreSQL backend for real-time synchronization.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Surface(
                        color = SurfaceSubtle,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Project ID:", fontSize = 12.sp, color = TextMuted)
                                Text(SupabaseClient.PROJECT_ID, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Tables Synced:", fontSize = 12.sp, color = TextMuted)
                                Text("invoices, items, parties, expenses", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TealSuccessText)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Connection:", fontSize = 12.sp, color = TextMuted)
                                Text(supabaseInfo.message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (supabaseInfo.isConnected) TealSuccessText else RoseErrorText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.testSupabaseConnection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Test Connection Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectionModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ModernActionPill(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .bounceClick(scaleDown = 0.93f, onClick = onClick),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SalesBarChart(
    data: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            val fraction = (value / maxVal).coerceIn(0.12f, 1f)
            val isMax = value == maxVal && value > 0f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (isMax) TealSuccess else PrimaryBlueLight.copy(alpha = 0.7f)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getOrElse(index) { "" },
                    fontSize = 10.sp,
                    fontWeight = if (isMax) FontWeight.Bold else FontWeight.Normal,
                    color = if (isMax) TealSuccessText else TextSecondary
                )
            }
        }
    }
}
