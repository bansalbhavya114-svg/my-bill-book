package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.InvoiceWithItems
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

@Composable
fun InvoicesScreen(
    viewModel: BillBookViewModel,
    onCreateInvoiceClick: () -> Unit,
    onViewInvoiceClick: (InvoiceWithItems) -> Unit
) {
    val context = LocalContext.current
    val allInvoices by viewModel.invoices.collectAsStateWithLifecycle()
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var invoiceToRecordPayment by remember { mutableStateOf<InvoiceWithItems?>(null) }

    val filterOptions = listOf("All", "GST Invoices", "Estimates", "Credit Notes", "Unpaid", "Paid")

    val filteredInvoices = remember(allInvoices, searchQuery, selectedFilter) {
        allInvoices.filter { item ->
            val inv = item.invoice
            val matchesSearch = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.partyName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "GST Invoices" -> inv.type == "GST_INVOICE" || inv.type == "BILL_OF_SUPPLY"
                "Estimates" -> inv.type == "ESTIMATE_QUOTE" || inv.type == "PROFORMA"
                "Credit Notes" -> inv.type == "CREDIT_NOTE"
                "Unpaid" -> inv.status == "UNPAID" || inv.status == "PARTIAL"
                "Paid" -> inv.status == "PAID"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateInvoiceClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Bill / Invoice") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_invoice_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("invoices_screen")
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Invoice #, Party name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(filterOptions) { opt ->
                    FilterChip(
                        selected = selectedFilter == opt,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            // List of Invoices
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No invoices found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Try clearing filters or create a new invoice",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInvoices, key = { it.invoice.id }) { invWithItems ->
                        InvoiceListItemCard(
                            invoiceWithItems = invWithItems,
                            onViewClick = { onViewInvoiceClick(invWithItems) },
                            onShareWhatsApp = { shareInvoiceWhatsApp(context, invWithItems, currentBusiness) },
                            onConvertEstimate = { viewModel.convertEstimateToInvoice(invWithItems) },
                            onCreateCreditNote = { viewModel.convertToCreditNote(invWithItems) },
                            onRecordPayment = { invoiceToRecordPayment = invWithItems },
                            onDelete = { viewModel.deleteInvoice(invWithItems.invoice.id) }
                        )
                    }
                }
            }
        }
    }

    // Quick Record Payment Dialog
    invoiceToRecordPayment?.let { invWithItems ->
        val inv = invWithItems.invoice
        val remaining = inv.grandTotal - inv.paidAmount
        var payAmountText by remember { mutableStateOf(remaining.toString()) }
        var selectedMode by remember { mutableStateOf("UPI") }

        AlertDialog(
            onDismissRequest = { invoiceToRecordPayment = null },
            title = { Text("Record Payment for ${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Total Invoice: ${formatCurrency(inv.grandTotal)}")
                    Text("Already Paid: ${formatCurrency(inv.paidAmount)}")
                    Text("Remaining Due: ${formatCurrency(remaining)}", fontWeight = FontWeight.Bold, color = RoseError)

                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Payment Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI", "CASH", "BANK").forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmountText.toDoubleOrNull() ?: remaining
                        viewModel.recordInvoicePayment(
                            invoiceId = inv.id,
                            total = inv.grandTotal,
                            amount = inv.paidAmount + amt,
                            mode = selectedMode,
                            partyId = inv.partyId,
                            partyName = inv.partyName
                        )
                        invoiceToRecordPayment = null
                    }
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToRecordPayment = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InvoiceListItemCard(
    invoiceWithItems: InvoiceWithItems,
    onViewClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onConvertEstimate: () -> Unit,
    onCreateCreditNote: () -> Unit,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val inv = invoiceWithItems.invoice
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Number, Type badge, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = inv.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TypeBadge(type = inv.type)
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View & Print") },
                            onClick = {
                                showMenu = false
                                onViewClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share via WhatsApp") },
                            onClick = {
                                showMenu = false
                                onShareWhatsApp()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        if (inv.status != "PAID") {
                            DropdownMenuItem(
                                text = { Text("Record Payment") },
                                onClick = {
                                    showMenu = false
                                    onRecordPayment()
                                },
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
                            )
                        }
                        if (inv.type == "ESTIMATE_QUOTE" || inv.type == "PROFORMA") {
                            DropdownMenuItem(
                                text = { Text("Convert to Tax Invoice") },
                                onClick = {
                                    showMenu = false
                                    onConvertEstimate()
                                },
                                leadingIcon = { Icon(Icons.Default.Transform, contentDescription = null) }
                            )
                        } else if (inv.type == "GST_INVOICE") {
                            DropdownMenuItem(
                                text = { Text("Create Credit Note") },
                                onClick = {
                                    showMenu = false
                                    onCreateCreditNote()
                                },
                                leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = RoseError) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseError) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Party Name and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = inv.partyName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Dated: ${formatDate(inv.date)} • ${invoiceWithItems.items.size} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(inv.grandTotal),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StatusBadge(status = inv.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tax: ${formatCurrency(inv.totalTax)} • Due: ${formatShortDate(inv.dueDate)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onShareWhatsApp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TealSuccessContainer,
                            contentColor = TealSuccessText
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onViewClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Preview", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
