package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessProfileEntity

@Composable
fun BusinessSetupDialog(
    business: BusinessProfileEntity? = null,
    onDismiss: () -> Unit,
    onSave: (BusinessProfileEntity) -> Unit
) {
    var businessName by remember { mutableStateOf(business?.businessName ?: "") }
    var tradeName by remember { mutableStateOf(business?.tradeName ?: "") }
    var gstin by remember { mutableStateOf(business?.gstin ?: "") }
    var businessType by remember { mutableStateOf(business?.businessType ?: "Retailer (Shop / Store)") }
    var turnover by remember { mutableStateOf(business?.turnover ?: "Below ₹20 Lakhs (GST Exempt)") }
    var phone by remember { mutableStateOf(business?.phone ?: "") }
    var email by remember { mutableStateOf(business?.email ?: "") }
    var address by remember { mutableStateOf(business?.address ?: "") }
    var state by remember { mutableStateOf(business?.state ?: "Delhi") }
    var bankName by remember { mutableStateOf(business?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(business?.accountNumber ?: "") }
    var ifscCode by remember { mutableStateOf(business?.ifscCode ?: "") }
    var upiId by remember { mutableStateOf(business?.upiId ?: "") }
    var invoicePrefix by remember { mutableStateOf(business?.invoicePrefix ?: "INV-26-") }
    var termsConditions by remember { mutableStateOf(business?.termsConditions ?: "1. Goods once sold will not be taken back.\n2. Subject to local jurisdiction.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (business == null) "Add New Business" else "Business Profile", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Legal Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = tradeName,
                        onValueChange = { tradeName = it },
                        label = { Text("Trade Name (Shop / Brand)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = gstin,
                            onValueChange = { gstin = it.uppercase() },
                            label = { Text("GSTIN") },
                            placeholder = { Text("e.g. 27AAAAA0000A1Z5") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Business Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Bank & UPI Details (For QR Code)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (e.g. 9876543210@upi)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("A/C Number") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase() },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = termsConditions,
                        onValueChange = { termsConditions = it },
                        label = { Text("Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (businessName.isNotBlank()) {
                        val profile = (business ?: BusinessProfileEntity(businessName = businessName)).copy(
                            businessName = businessName,
                            tradeName = tradeName.ifBlank { businessName },
                            gstin = gstin,
                            businessType = businessType,
                            turnover = turnover,
                            phone = phone,
                            email = email,
                            address = address,
                            state = state,
                            bankName = bankName,
                            accountNumber = accountNumber,
                            ifscCode = ifscCode,
                            upiId = upiId,
                            invoicePrefix = invoicePrefix,
                            termsConditions = termsConditions
                        )
                        onSave(profile)
                    }
                }
            ) {
                Text("Save Business")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
