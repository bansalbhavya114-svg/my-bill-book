package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    viewModel: BillBookViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: GST, 1: Profit & Loss, 2: Expenses & AI Scan

    val gstSummary by viewModel.gstSummary.collectAsStateWithLifecycle()
    val profitLoss by viewModel.profitLoss.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAiScanDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("GST Reports", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Profit & Loss", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Expenses & OCR", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        when (selectedTab) {
            0 -> GstReportsView(
                gstSummary = gstSummary,
                currentBusiness = currentBusiness?.businessName ?: "My Store",
                onExport = {
                    val reportText = """
*GST FILING REPORT (GSTR-1 / GSTR-3B READY)*
*Business:* ${currentBusiness?.businessName}
*GSTIN:* ${currentBusiness?.gstin}
----------------------------------------
*Total Outward Invoices:* ${gstSummary.totalInvoices}
*B2B Invoices (With GSTIN):* ${gstSummary.b2bInvoiceCount}
*B2C Invoices (Retail):* ${gstSummary.b2cInvoiceCount}
----------------------------------------
*Total Taxable Value:* ${formatCurrency(gstSummary.totalTaxableValue)}
*CGST (Central Tax):* ${formatCurrency(gstSummary.totalCgst)}
*SGST (State Tax):* ${formatCurrency(gstSummary.totalSgst)}
*IGST (Integrated Tax):* ${formatCurrency(gstSummary.totalIgst)}
*Total GST Liability:* *${formatCurrency(gstSummary.totalGst)}*
----------------------------------------
Generated via BillBook GST Accounting
                    """.trimIndent()
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export GST Report"))
                }
            )
            1 -> ProfitLossView(profitLoss = profitLoss)
            2 -> ExpensesView(
                expenses = expenses,
                onAddExpense = { showAddExpenseDialog = true },
                onAiScan = { showAiScanDialog = true },
                onDelete = { viewModel.deleteExpense(it) }
            )
        }
    }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { exp ->
                viewModel.recordExpense(exp)
                showAddExpenseDialog = false
            }
        )
    }

    // AI Receipt / Bill OCR Scan Dialog
    if (showAiScanDialog) {
        AiBillScanDialog(
            onDismiss = { showAiScanDialog = false },
            onScanned = { rawText ->
                val (expense, message) = viewModel.parseReceiptText(rawText)
                viewModel.recordExpense(expense)
                showAiScanDialog = false
            }
        )
    }
}

@Composable
fun GstReportsView(
    gstSummary: com.example.viewmodel.GstSummaryReport,
    currentBusiness: String,
    onExport: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GSTR-1 Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Ready for GST portal monthly return filing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Invoices", fontSize = 12.sp)
                            Text("${gstSummary.totalInvoices}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column {
                            Text("B2B Invoices", fontSize = 12.sp)
                            Text("${gstSummary.b2bInvoiceCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                        }
                        Column {
                            Text("B2C Small", fontSize = 12.sp)
                            Text("${gstSummary.b2cInvoiceCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealSuccess)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tax Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    TaxRow("Taxable Value", formatCurrency(gstSummary.totalTaxableValue), isBold = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    TaxRow("Central Tax (CGST)", formatCurrency(gstSummary.totalCgst))
                    TaxRow("State Tax (SGST)", formatCurrency(gstSummary.totalSgst))
                    TaxRow("Integrated Tax (IGST)", formatCurrency(gstSummary.totalIgst))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    TaxRow("Total Output GST", formatCurrency(gstSummary.totalGst), isHighlight = true)
                }
            }
        }

        item {
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share GSTR-1 Report")
            }
        }
    }
}

@Composable
fun TaxRow(label: String, value: String, isBold: Boolean = false, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isHighlight) 15.sp else 13.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = if (isHighlight) 16.sp else 13.sp,
            fontWeight = if (isBold || isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProfitLossView(
    profitLoss: com.example.viewmodel.ProfitLossReport
) {
    val marginPercent = if (profitLoss.grossSales > 0) {
        ((profitLoss.netProfit / profitLoss.grossSales) * 100).roundToInt()
    } else 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (profitLoss.netProfit >= 0) TealSuccessContainer else RoseErrorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Net Business Profit",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (profitLoss.netProfit >= 0) TealSuccessText else RoseErrorText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        formatCurrency(profitLoss.netProfit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = if (profitLoss.netProfit >= 0) TealSuccessText else RoseErrorText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Net Profit Margin: $marginPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profitLoss.netProfit >= 0) TealSuccessText else RoseErrorText
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Income Statement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    TaxRow("Gross Sales (Revenue)", formatCurrency(profitLoss.grossSales))
                    TaxRow("Estimated Cost of Goods (COGS)", "-${formatCurrency(profitLoss.estimatedCogs)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    TaxRow("Gross Profit", formatCurrency(profitLoss.grossProfit), isBold = true)
                    TaxRow("Total Operating Expenses", "-${formatCurrency(profitLoss.totalExpenses)}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    TaxRow("Net Profit / (Loss)", formatCurrency(profitLoss.netProfit), isHighlight = true)
                }
            }
        }
    }
}

@Composable
fun ExpensesView(
    expenses: List<ExpenseEntity>,
    onAddExpense: () -> Unit,
    onAiScan: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddExpense,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Expense")
            }

            FilledTonalButton(
                onClick = onAiScan,
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = PurpleAccentContainer,
                    contentColor = PurpleAccent
                )
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Bill OCR", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total Expenses Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Recorded Expenses", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(formatCurrency(totalExpense), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoseError)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No expenses recorded yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(expenses, key = { it.id }) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(exp.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${exp.paidTo.ifBlank { "Cash" }} • ${formatDate(exp.date)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (exp.notes.isNotBlank()) {
                                    Text(exp.notes, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "-${formatCurrency(exp.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = RoseError
                                )
                                IconButton(onClick = { onDelete(exp.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var category by remember { mutableStateOf("Rent") }
    var amount by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("UPI") }

    val categories = listOf("Rent", "Electricity", "Salaries", "Transport", "Packaging", "Tea/Snacks", "Marketing", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Business Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidTo,
                    onValueChange = { paidTo = it },
                    label = { Text("Paid To / Vendor") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(
                            ExpenseEntity(
                                category = category,
                                amount = amt,
                                paidTo = paidTo,
                                notes = notes,
                                paymentMode = mode
                            )
                        )
                    }
                }
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AiBillScanDialog(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit
) {
    var billSampleText by remember {
        mutableStateOf(
            "BSES Rajdhani Power Limited\nElectricity Bill Payment\nConsumer No: 1002348821\nAmount Due: Rs 3450.00\nPayment Status: Successful"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Bill & Receipt Scanner", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Simulate OCR scanning of physical purchase bills, fuel receipts, or utility invoices to auto-fill accounting entries:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = billSampleText.contains("BSES"),
                        onClick = {
                            billSampleText = "BSES Rajdhani Power Limited\nElectricity Bill Payment\nAmount Due: Rs 3450.00\nPaid via UPI"
                        },
                        label = { Text("Electricity Bill", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = billSampleText.contains("Indian Oil"),
                        onClick = {
                            billSampleText = "Indian Oil Petrol Pump\nVehicle Transport Fuel\nTotal Amount: Rs 2800.00\nCash Payment"
                        },
                        label = { Text("Fuel / Transport", fontSize = 11.sp) }
                    )
                }

                OutlinedTextField(
                    value = billSampleText,
                    onValueChange = { billSampleText = it },
                    label = { Text("Scanned Receipt OCR Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onScanned(billSampleText) },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto-Extract & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
