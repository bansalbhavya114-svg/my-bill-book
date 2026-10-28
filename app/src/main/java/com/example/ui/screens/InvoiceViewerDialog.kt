package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.InvoiceWithItems
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceViewerDialog(
    invoiceWithItems: InvoiceWithItems,
    currentBusiness: BusinessProfileEntity?,
    onDismiss: () -> Unit,
    onRecordPayment: (Double, String) -> Unit,
    onConvertEstimate: () -> Unit
) {
    val context = LocalContext.current
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    var selectedTemplate by remember { mutableStateOf("MODERN") } // MODERN, CLASSIC_GST, THERMAL
    var showPaymentDialog by remember { mutableStateOf(false) }

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
                        title = { Text(inv.invoiceNumber, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                shareInvoiceWhatsApp(context, invoiceWithItems, currentBusiness)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share WhatsApp", tint = TealSuccess)
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    shareInvoiceWhatsApp(context, invoiceWithItems, currentBusiness)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = TealSuccessContainer,
                                    contentColor = TealSuccessText
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold)
                            }

                            if (inv.status != "PAID") {
                                Button(
                                    onClick = { showPaymentDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Record Pay")
                                }
                            } else if (inv.type == "ESTIMATE_QUOTE") {
                                Button(
                                    onClick = onConvertEstimate,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Convert to Invoice")
                                }
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Template Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        FilterChip(
                            selected = selectedTemplate == "MODERN",
                            onClick = { selectedTemplate = "MODERN" },
                            label = { Text("Modern Clean", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTemplate == "CLASSIC_GST",
                            onClick = { selectedTemplate = "CLASSIC_GST" },
                            label = { Text("Classic GST", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTemplate == "THERMAL",
                            onClick = { selectedTemplate = "THERMAL" },
                            label = { Text("Thermal Slip", fontSize = 11.sp) }
                        )
                    }

                    // Invoice Paper Document
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        when (selectedTemplate) {
                            "THERMAL" -> ThermalTemplateView(invoiceWithItems, currentBusiness)
                            "CLASSIC_GST" -> ClassicGstTemplateView(invoiceWithItems, currentBusiness)
                            else -> ModernTemplateView(invoiceWithItems, currentBusiness)
                        }
                    }
                }
            }
        }

        if (showPaymentDialog) {
            val remaining = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)
            var payAmountText by remember { mutableStateOf(remaining.roundToInt().toString()) }
            var selectedMode by remember { mutableStateOf("UPI") }

            val enteredAmt = payAmountText.toDoubleOrNull() ?: 0.0
            val projectedTotalPaid = inv.paidAmount + enteredAmt
            val projectedStatus = when {
                projectedTotalPaid >= inv.grandTotal && inv.grandTotal > 0.0 -> "PAID"
                projectedTotalPaid > 0.0 -> "PARTIAL"
                else -> "UNPAID"
            }

            AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Payment", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Invoice: ${inv.invoiceNumber}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Grand Total:", fontSize = 12.sp)
                                    Text(formatCurrency(inv.grandTotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Already Paid:", fontSize = 12.sp)
                                    Text(formatCurrency(inv.paidAmount), fontSize = 12.sp, color = TealSuccessText)
                                }
                                HorizontalDivider()
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Remaining Due:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(formatCurrency(remaining), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseErrorText)
                                }
                            }
                        }

                        // Live resulting status preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            StatusBadge(status = projectedStatus)
                        }

                        // Quick Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = enteredAmt >= remaining && remaining > 0,
                                onClick = { payAmountText = remaining.roundToInt().toString() },
                                label = { Text("Full Due (₹${remaining.roundToInt()})", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = enteredAmt > 0 && enteredAmt < remaining,
                                onClick = { payAmountText = (remaining / 2.0).roundToInt().toString() },
                                label = { Text("Partial (50%)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = payAmountText,
                            onValueChange = { payAmountText = it },
                            label = { Text("Payment Received (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                                FilterChip(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    label = { Text(mode.replace("_", " ")) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = payAmountText.toDoubleOrNull() ?: remaining
                            if (amt > 0.0) {
                                onRecordPayment(amt, selectedMode)
                                showPaymentDialog = false
                            }
                        }
                    ) {
                        Text("Confirm Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// 1. Modern Corporate Invoice Template
@Composable
fun ModernTemplateView(
    invoiceWithItems: InvoiceWithItems,
    currentBusiness: BusinessProfileEntity?
) {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentBusiness?.businessName ?: "My Business",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryBlueDark
                    )
                    Text(currentBusiness?.address ?: "", fontSize = 11.sp, color = Color.Gray)
                    Text("GSTIN: ${currentBusiness?.gstin ?: ""}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Phone: ${currentBusiness?.phone ?: ""}", fontSize = 11.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (inv.type == "GST_INVOICE") "TAX INVOICE" else inv.type.replace("_", " "),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Date: ${formatDate(inv.date)}", fontSize = 11.sp, color = Color.Gray)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFE2E8F0))
        }

        // Bill To
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("BILL TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Text(inv.partyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (inv.partyAddress.isNotBlank()) {
                        Text(inv.partyAddress, fontSize = 11.sp, color = Color.Gray)
                    }
                    if (inv.partyGstin.isNotBlank()) {
                        Text("GSTIN: ${inv.partyGstin}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("State: ${inv.partyState} • Place of Supply: ${inv.partyState}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Table Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Item & HSN", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }

        // Table Rows
        itemsIndexed(items) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(2f)) {
                    Text(item.itemName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    val hsnLabel = if (item.hsnCode.isNotBlank()) "HSN/SAC: ${item.hsnCode} • " else ""
                    val gstLabel = if (item.taxRate % 1.0 == 0.0) "${item.taxRate.toInt()}%" else "${item.taxRate}%"
                    Text("${hsnLabel}GST $gstLabel", fontSize = 10.sp, color = Color.Gray)
                }
                Text("${item.quantity.toInt()} ${item.unit}", fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                Text("₹${item.rate.roundToInt()}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("₹${item.totalAmount.roundToInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            HorizontalDivider(color = Color(0xFFF8FAFC))
        }

        // Totals & Taxes Breakdown
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taxable Subtotal", fontSize = 12.sp)
                    Text(formatCurrency(inv.subtotal), fontSize = 12.sp)
                }
                if (inv.cgstAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST", fontSize = 12.sp)
                        Text(formatCurrency(inv.cgstAmount), fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST", fontSize = 12.sp)
                        Text(formatCurrency(inv.sgstAmount), fontSize = 12.sp)
                    }
                }
                if (inv.igstAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGST (Inter-State)", fontSize = 12.sp)
                        Text(formatCurrency(inv.igstAmount), fontSize = 12.sp)
                    }
                }
                if (inv.shippingCharges > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Charges", fontSize = 12.sp)
                        Text(formatCurrency(inv.shippingCharges), fontSize = 12.sp)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(formatCurrency(inv.grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                }
            }
        }

        // Bank & Payment QR Code
        item {
            val upiId = currentBusiness?.upiId?.trim().takeIf { !it.isNullOrBlank() }
                ?: currentBusiness?.phone?.trim()?.takeIf { it.isNotBlank() }?.let { "$it@upi" }
                ?: "merchant@upi"
            val bName = Uri.encode(currentBusiness?.businessName ?: "Store")
            val amt = String.format(Locale.US, "%.2f", inv.grandTotal)
            val note = Uri.encode("Inv " + inv.invoiceNumber)
            val upiPayload = "upi://pay?pa=$upiId&pn=$bName&am=$amt&cu=INR&tn=$note"

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PAYMENT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealSuccessText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("UPI ID: $upiId", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (!currentBusiness?.bankName.isNullOrBlank()) {
                            Text("Bank: ${currentBusiness?.bankName}", fontSize = 11.sp)
                        }
                        if (!currentBusiness?.accountNumber.isNullOrBlank()) {
                            Text("A/C: ${currentBusiness?.accountNumber}", fontSize = 11.sp)
                        }
                        if (!currentBusiness?.ifscCode.isNullOrBlank()) {
                            Text("IFSC: ${currentBusiness?.ifscCode}", fontSize = 11.sp)
                        }
                        Text("Scan with GPay • PhonePe • Paytm", fontSize = 10.sp, color = Color.Gray)
                    }
                    UpiQrCodeCanvas(
                        upiPayload = upiPayload,
                        sizeDp = 100
                    )
                }
            }
        }

        // Terms & Signatory
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Terms & Conditions:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        currentBusiness?.termsConditions ?: "Goods once sold cannot be returned.",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("For ${currentBusiness?.businessName ?: "Authorized Signatory"}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(28.dp))
                    Text("Authorized Signatory", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

// 2. Classic GST Government Tax Invoice Template
@Composable
fun ClassicGstTemplateView(
    invoiceWithItems: InvoiceWithItems,
    currentBusiness: BusinessProfileEntity?
) {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TAX INVOICE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("(Issued under Section 31 of CGST Act)", fontSize = 9.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(currentBusiness?.businessName ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(currentBusiness?.address ?: "", fontSize = 10.sp)
                    Text("GSTIN: ${currentBusiness?.gstin ?: ""} • State Code: 07", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
            ) {
                Column(modifier = Modifier.weight(1f).padding(6.dp)) {
                    Text("Invoice No: ${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Invoice Date: ${formatDate(inv.date)}", fontSize = 10.sp)
                    Text("State of Supply: ${inv.partyState}", fontSize = 10.sp)
                }
                Column(modifier = Modifier.weight(1f).border(1.dp, Color.Black).padding(6.dp)) {
                    Text("Billed To: ${inv.partyName}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("GSTIN: ${inv.partyGstin.ifBlank { "Unregistered" }}", fontSize = 10.sp)
                    Text("Address: ${inv.partyAddress}", fontSize = 9.sp)
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE2E8F0)).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(2f))
                    Text("HSN", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                    Text("Qty", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.6f))
                    Text("Rate", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                    Text("Tax", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                    Text("Amount", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Color.Black)
                items.forEach { itm ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(itm.itemName, fontSize = 10.sp, modifier = Modifier.weight(2f))
                        Text(itm.hsnCode.ifBlank { "-" }, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                        Text("${itm.quantity.toInt()}", fontSize = 10.sp, modifier = Modifier.weight(0.6f))
                        Text("₹${itm.rate.roundToInt()}", fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                        val gstRateText = if (itm.taxRate % 1.0 == 0.0) "${itm.taxRate.toInt()}%" else "${itm.taxRate}%"
                        Text(gstRateText, fontSize = 10.sp, modifier = Modifier.weight(0.8f))
                        Text("₹${itm.totalAmount.roundToInt()}", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taxable Value", fontSize = 11.sp)
                    Text(formatCurrency(inv.subtotal), fontSize = 11.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Tax (CGST+SGST/IGST)", fontSize = 11.sp)
                    Text(formatCurrency(inv.totalTax), fontSize = 11.sp)
                }
                HorizontalDivider(color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Invoice Value", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(formatCurrency(inv.grandTotal), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // UPI QR Code and Bank Details for Classic GST
        item {
            val upiId = currentBusiness?.upiId?.trim().takeIf { !it.isNullOrBlank() }
                ?: currentBusiness?.phone?.trim()?.takeIf { it.isNotBlank() }?.let { "$it@upi" }
                ?: "merchant@upi"
            val bName = Uri.encode(currentBusiness?.businessName ?: "Store")
            val amt = String.format(Locale.US, "%.2f", inv.grandTotal)
            val note = Uri.encode("Inv " + inv.invoiceNumber)
            val upiPayload = "upi://pay?pa=$upiId&pn=$bName&am=$amt&cu=INR&tn=$note"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PAYMENT MODE: UPI / BANK", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Text("UPI ID: $upiId", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    if (!currentBusiness?.bankName.isNullOrBlank()) {
                        Text("Bank: ${currentBusiness?.bankName} (A/C: ${currentBusiness?.accountNumber ?: ""})", fontSize = 9.sp)
                    }
                    Text("Scan with GPay / PhonePe / Paytm", fontSize = 9.sp, color = Color.Gray)
                }
                UpiQrCodeCanvas(
                    upiPayload = upiPayload,
                    sizeDp = 80
                )
            }
        }
    }
}

// 3. Compact 3-inch Thermal Receipt Template
@Composable
fun ThermalTemplateView(
    invoiceWithItems: InvoiceWithItems,
    currentBusiness: BusinessProfileEntity?
) {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Text(
                currentBusiness?.businessName?.uppercase() ?: "STORE RECEIPT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(currentBusiness?.phone ?: "", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text("GSTIN: ${currentBusiness?.gstin ?: ""}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text("Bill No: ${inv.invoiceNumber} • ${formatDate(inv.date)}", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("Customer: ${inv.partyName}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }

        itemsIndexed(items) { index, itm ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${itm.quantity.toInt()}x ${itm.itemName.take(16)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
                Text(
                    "₹${itm.totalAmount.roundToInt()}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        item {
            Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal:", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text(formatCurrency(inv.subtotal), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("GST Tax:", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text(formatCurrency(inv.totalTax), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            Text("================================", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL:", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(formatCurrency(inv.grandTotal), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            val upiId = currentBusiness?.upiId?.trim().takeIf { !it.isNullOrBlank() }
                ?: currentBusiness?.phone?.trim()?.takeIf { it.isNotBlank() }?.let { "$it@upi" }
                ?: "merchant@upi"
            val bName = Uri.encode(currentBusiness?.businessName ?: "Store")
            val amt = String.format(Locale.US, "%.2f", inv.grandTotal)
            val note = Uri.encode("Inv " + inv.invoiceNumber)
            UpiQrCodeCanvas(
                upiPayload = "upi://pay?pa=$upiId&pn=$bName&am=$amt&cu=INR&tn=$note",
                sizeDp = 90
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("** THANK YOU VISIT AGAIN **", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

