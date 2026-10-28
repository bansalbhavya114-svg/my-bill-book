package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.formatCurrency
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlin.math.roundToInt

data class EditableLineItem(
    val itemId: Long? = null,
    var itemName: String = "",
    var hsnCode: String = "",
    var unit: String = "PCS",
    var quantity: Double = 1.0,
    var rate: Double = 0.0,
    var discountPercent: Double = 0.0,
    var taxRate: Double = 18.0
) {
    val subtotal: Double get() = quantity * rate
    val discountAmount: Double get() = subtotal * (discountPercent / 100.0)
    val taxableAmount: Double get() = subtotal - discountAmount
    val taxAmount: Double get() = taxableAmount * (taxRate / 100.0)
    val totalAmount: Double get() = taxableAmount + taxAmount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorDialog(
    currentBusiness: BusinessProfileEntity?,
    partiesList: List<PartyEntity>,
    inventoryItems: List<ItemEntity>,
    existingInvoice: InvoiceWithItems? = null,
    onDismiss: () -> Unit,
    onSave: (InvoiceEntity, List<InvoiceItemEntity>) -> Unit
) {
    var invoiceType by remember { mutableStateOf(existingInvoice?.invoice.let { it?.type } ?: "GST_INVOICE") }
    var invoiceNumber by remember {
        mutableStateOf(
            existingInvoice?.invoice?.invoiceNumber ?: "${currentBusiness?.invoicePrefix ?: "INV-26-"}${currentBusiness?.nextInvoiceNumber ?: 105}"
        )
    }

    // Party details
    var selectedPartyId by remember { mutableStateOf<Long?>(existingInvoice?.invoice?.partyId) }
    var partyName by remember { mutableStateOf(existingInvoice?.invoice?.partyName ?: "Cash Customer") }
    var partyGstin by remember { mutableStateOf(existingInvoice?.invoice?.partyGstin ?: "") }
    var partyPhone by remember { mutableStateOf(existingInvoice?.invoice?.partyPhone ?: "") }
    var partyAddress by remember { mutableStateOf(existingInvoice?.invoice?.partyAddress ?: "") }
    var partyState by remember { mutableStateOf(existingInvoice?.invoice?.partyState ?: currentBusiness?.state ?: "Delhi") }
    var showPartyPicker by remember { mutableStateOf(false) }

    // Line items
    val lineItems = remember {
        mutableStateListOf<EditableLineItem>().apply {
            if (existingInvoice != null && existingInvoice.items.isNotEmpty()) {
                addAll(existingInvoice.items.map {
                    EditableLineItem(
                        itemId = it.itemId,
                        itemName = it.itemName,
                        hsnCode = it.hsnCode,
                        unit = it.unit,
                        quantity = it.quantity,
                        rate = it.rate,
                        discountPercent = it.discountPercent,
                        taxRate = it.taxRate
                    )
                })
            } else {
                if (inventoryItems.isNotEmpty()) {
                    val first = inventoryItems.first()
                    add(EditableLineItem(itemId = first.id, itemName = first.name, rate = first.salePrice, quantity = 1.0, taxRate = first.taxRate, hsnCode = first.hsnCode, unit = first.unit))
                } else {
                    add(EditableLineItem(itemName = "", rate = 0.0, quantity = 1.0, taxRate = 0.0))
                }
            }
        }
    }

    var showAddItemSheet by remember { mutableStateOf(false) }

    // Charges & Payment
    var overallDiscount by remember { mutableStateOf("0.0") }
    var shippingCharges by remember { mutableStateOf("0.0") }
    var paidAmountInput by remember {
        mutableStateOf(
            if (existingInvoice != null && existingInvoice.invoice.paidAmount > 0.0) {
                existingInvoice.invoice.paidAmount.roundToInt().toString()
            } else {
                "0"
            }
        )
    }
    var paymentMode by remember { mutableStateOf(existingInvoice?.invoice?.paymentMode ?: "UPI") }
    var notes by remember { mutableStateOf(existingInvoice?.invoice?.notes ?: "Thank you for your business!") }

    // Computations
    val businessState = currentBusiness?.state ?: "Delhi"
    val isInterState = !partyState.equals(businessState, ignoreCase = true)

    val subtotal = lineItems.sumOf { it.taxableAmount }
    val discountVal = overallDiscount.toDoubleOrNull() ?: 0.0
    val shippingVal = shippingCharges.toDoubleOrNull() ?: 0.0

    val totalTax = lineItems.sumOf { it.taxAmount }
    val cgstAmount = if (isInterState) 0.0 else totalTax / 2.0
    val sgstAmount = if (isInterState) 0.0 else totalTax / 2.0
    val igstAmount = if (isInterState) totalTax else 0.0

    val rawGrandTotal = (subtotal - discountVal + totalTax + shippingVal).coerceAtLeast(0.0)
    val grandTotalRounded = (rawGrandTotal).roundToInt().toDouble()
    val roundOff = grandTotalRounded - rawGrandTotal

    val paidAmountVal = paidAmountInput.toDoubleOrNull() ?: 0.0
    val autoPaymentStatus = when {
        invoiceType == "ESTIMATE_QUOTE" || invoiceType == "PROFORMA" -> "DRAFT"
        paidAmountVal >= grandTotalRounded && grandTotalRounded > 0.0 -> "PAID"
        paidAmountVal > 0.0 -> "PARTIAL"
        else -> "UNPAID"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (existingInvoice == null) "Create Invoice" else "Edit Invoice", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    val finalPaidAmount = if (invoiceType == "ESTIMATE_QUOTE" || invoiceType == "PROFORMA") 0.0
                                    else paidAmountVal.coerceAtMost(grandTotalRounded)

                                    val invoiceEntity = InvoiceEntity(
                                        id = existingInvoice?.invoice?.id ?: 0L,
                                        businessId = currentBusiness?.id ?: 1L,
                                        invoiceNumber = invoiceNumber,
                                        type = invoiceType,
                                        partyId = selectedPartyId,
                                        partyName = partyName.ifBlank { "Cash Customer" },
                                        partyGstin = partyGstin,
                                        partyPhone = partyPhone,
                                        partyAddress = partyAddress,
                                        partyState = partyState,
                                        status = autoPaymentStatus,
                                        subtotal = subtotal,
                                        totalDiscount = discountVal,
                                        cgstAmount = cgstAmount,
                                        sgstAmount = sgstAmount,
                                        igstAmount = igstAmount,
                                        totalTax = totalTax,
                                        shippingCharges = shippingVal,
                                        roundOff = roundOff,
                                        grandTotal = grandTotalRounded,
                                        paidAmount = finalPaidAmount,
                                        paymentMode = paymentMode,
                                        notes = notes
                                    )

                                    val invoiceItems = lineItems.map { line ->
                                        val lineTax = line.taxAmount
                                        InvoiceItemEntity(
                                            invoiceId = existingInvoice?.invoice?.id ?: 0L,
                                            itemId = line.itemId,
                                            itemName = line.itemName,
                                            hsnCode = line.hsnCode,
                                            unit = line.unit,
                                            quantity = line.quantity,
                                            rate = line.rate,
                                            discountPercent = line.discountPercent,
                                            taxRate = line.taxRate,
                                            taxableAmount = line.taxableAmount,
                                            cgst = if (isInterState) 0.0 else lineTax / 2.0,
                                            sgst = if (isInterState) 0.0 else lineTax / 2.0,
                                            igst = if (isInterState) lineTax else 0.0,
                                            totalAmount = line.totalAmount
                                        )
                                    }

                                    onSave(invoiceEntity, invoiceItems)
                                },
                                modifier = Modifier.testTag("save_invoice_button")
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & Sync", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Invoice Type Selector
                    item {
                        Text("Invoice Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "GST_INVOICE" to "Tax Invoice",
                                "BILL_OF_SUPPLY" to "Bill of Supply",
                                "ESTIMATE_QUOTE" to "Estimate / Quote"
                            ).forEach { (type, label) ->
                                FilterChip(
                                    selected = invoiceType == type,
                                    onClick = { invoiceType = type },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Invoice Number
                    item {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("Invoice Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Customer / Party Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Customer / Party Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    TextButton(onClick = { showPartyPicker = true }) {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select Saved Party")
                                    }
                                }

                                OutlinedTextField(
                                    value = partyName,
                                    onValueChange = { partyName = it },
                                    label = { Text("Party Name *") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = partyPhone,
                                        onValueChange = { partyPhone = it },
                                        label = { Text("Phone") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = partyState,
                                        onValueChange = { partyState = it },
                                        label = { Text("State") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = partyGstin,
                                    onValueChange = { partyGstin = it.uppercase() },
                                    label = { Text("Party GSTIN (B2B)") },
                                    placeholder = { Text("e.g. 07AAAAA1234A1Z5") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // GST Tax Context Note
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isInterState) "Inter-State Sale: IGST applies (${currentBusiness?.state} -> $partyState)"
                                    else "Intra-State Sale: CGST + SGST applies ($partyState)",
                                    fontSize = 11.sp,
                                    color = if (isInterState) PurpleAccent else TealSuccessText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Line Items Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Items (${lineItems.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            FilledTonalButton(
                                onClick = { showAddItemSheet = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add from Catalog", fontSize = 12.sp)
                            }
                        }
                    }

                    // Line Items List
                    itemsIndexed(lineItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.itemName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            if (lineItems.size > 1) {
                                                lineItems.removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = RoseError, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quantity Stepper
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                            .padding(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (item.quantity > 1) {
                                                    lineItems[index] = item.copy(quantity = item.quantity - 1)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Dec", modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            "${item.quantity.toInt()} ${item.unit}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                lineItems[index] = item.copy(quantity = item.quantity + 1)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Inc", modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    // Rate
                                    OutlinedTextField(
                                        value = if (item.rate == 0.0) "" else if (item.rate % 1.0 == 0.0) item.rate.toInt().toString() else item.rate.toString(),
                                        onValueChange = {
                                            val r = it.toDoubleOrNull() ?: 0.0
                                            lineItems[index] = item.copy(rate = r)
                                        },
                                        label = { Text("Rate (₹)") },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Tax % editable by user
                                    OutlinedTextField(
                                        value = if (item.taxRate % 1.0 == 0.0) item.taxRate.toInt().toString() else item.taxRate.toString(),
                                        onValueChange = {
                                            val tr = it.toDoubleOrNull() ?: 0.0
                                            lineItems[index] = item.copy(taxRate = tr)
                                        },
                                        label = { Text("GST %") },
                                        trailingIcon = { Text("%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(86.dp),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Taxable: ${formatCurrency(item.taxableAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Total: ${formatCurrency(item.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Extra Charges
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Discounts & Delivery", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = overallDiscount,
                                        onValueChange = { overallDiscount = it },
                                        label = { Text("Discount (₹)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = shippingCharges,
                                        onValueChange = { shippingCharges = it },
                                        label = { Text("Shipping (₹)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Payment Status & Amount
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Payment Status & Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Auto-selected by payment received", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusBadge(status = autoPaymentStatus)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick chips to select Paid, Partial or Unpaid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = paidAmountVal >= grandTotalRounded && grandTotalRounded > 0.0,
                                        onClick = {
                                            paidAmountInput = grandTotalRounded.roundToInt().toString()
                                        },
                                        label = { Text("Full Paid (₹${grandTotalRounded.roundToInt()})", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = paidAmountVal > 0.0 && paidAmountVal < grandTotalRounded,
                                        onClick = {
                                            val half = (grandTotalRounded / 2.0).roundToInt()
                                            paidAmountInput = half.toString()
                                        },
                                        label = { Text("Partial (50%)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = paidAmountVal <= 0.0,
                                        onClick = {
                                            paidAmountInput = "0"
                                        },
                                        label = { Text("Unpaid (₹0)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = paidAmountInput,
                                    onValueChange = { paidAmountInput = it },
                                    label = { Text("Amount Paid (₹)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    trailingIcon = {
                                        val bal = (grandTotalRounded - paidAmountVal).coerceAtLeast(0.0)
                                        if (bal > 0.0 && paidAmountVal > 0.0) {
                                            Text(
                                                "Due: ₹${bal.roundToInt()}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AmberWarningText,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                        }
                                    }
                                )

                                if (paidAmountVal > 0.0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                                            FilterChip(
                                                selected = paymentMode == mode,
                                                onClick = { paymentMode = mode },
                                                label = { Text(mode.replace("_", " ")) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bill Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Taxable Subtotal", fontSize = 13.sp)
                                    Text(formatCurrency(subtotal), fontSize = 13.sp)
                                }
                                if (isInterState) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("IGST", fontSize = 13.sp)
                                        Text(formatCurrency(igstAmount), fontSize = 13.sp)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("CGST", fontSize = 13.sp)
                                        Text(formatCurrency(cgstAmount), fontSize = 13.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("SGST", fontSize = 13.sp)
                                        Text(formatCurrency(sgstAmount), fontSize = 13.sp)
                                    }
                                }
                                if (roundOff != 0.0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Round Off", fontSize = 13.sp)
                                        Text(formatCurrency(roundOff), fontSize = 13.sp)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(
                                        formatCurrency(grandTotalRounded),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealSuccessContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = TealSuccessText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Checkout saves to Supabase (srpuwmqmxvwjdyyjgueh)",
                                            fontSize = 11.sp,
                                            color = TealSuccessText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Party Picker Dialog
    if (showPartyPicker) {
        AlertDialog(
            onDismissRequest = { showPartyPicker = false },
            title = { Text("Select Customer / Party") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(partiesList) { party ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPartyId = party.id
                                    partyName = party.name
                                    partyPhone = party.phone
                                    partyAddress = party.address
                                    partyGstin = party.gstin
                                    partyState = party.state
                                    showPartyPicker = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(party.name, fontWeight = FontWeight.Bold)
                                Text("${party.state} • ${party.phone}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(formatCurrency(party.currentBalance), fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPartyPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Item From Catalog Picker
    if (showAddItemSheet) {
        AlertDialog(
            onDismissRequest = { showAddItemSheet = false },
            title = { Text("Select Product from Inventory") },
            text = {
                if (inventoryItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No products in inventory catalog yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "You can type the product name and price directly on the invoice line item, or add products from the Items tab.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(inventoryItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        lineItems.add(
                                            EditableLineItem(
                                                itemId = item.id,
                                                itemName = item.name,
                                                hsnCode = item.hsnCode,
                                                unit = item.unit,
                                                quantity = 1.0,
                                                rate = item.salePrice,
                                                taxRate = item.taxRate
                                            )
                                        )
                                        showAddItemSheet = false
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    val hsnStr = if (item.hsnCode.isNotBlank()) "HSN/SAC: ${item.hsnCode} • " else ""
                                    val gstStr = if (item.taxRate % 1.0 == 0.0) "${item.taxRate.toInt()}%" else "${item.taxRate}%"
                                    Text("${hsnStr}GST: $gstStr • Stock: ${item.stockQty.toInt()} ${item.unit}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(formatCurrency(item.salePrice), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddItemSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
