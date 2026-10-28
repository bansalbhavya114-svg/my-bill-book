package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessProfileEntity
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

val BUSINESS_TYPES = listOf(
    "Retailer (Shop / Store)",
    "Wholesale & Distribution",
    "Manufacturing",
    "Services & Consulting",
    "Trader",
    "Freelancer / Agency",
    "E-Commerce",
    "Other"
)

val TURNOVER_TIERS = listOf(
    "Below ₹20 Lakhs (GST Exempt)",
    "₹20 Lakhs – ₹40 Lakhs (Standard)",
    "₹40 Lakhs – ₹1.5 Crore (Composition)",
    "₹1.5 Crore – ₹5 Crore",
    "Above ₹5 Crore"
)

val INDIAN_STATES = listOf(
    "Delhi", "Maharashtra", "Karnataka", "Tamil Nadu", "Gujarat",
    "Uttar Pradesh", "Haryana", "Rajasthan", "West Bengal", "Punjab",
    "Telangana", "Kerala", "Madhya Pradesh", "Bihar", "Odisha",
    "Andhra Pradesh", "Assam", "Jharkhand", "Chhattisgarh", "Uttarakhand"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompanySetupScreen(
    viewModel: BillBookViewModel,
    existingBusiness: BusinessProfileEntity? = null,
    isFirstTimeLaunch: Boolean = false,
    onNavigateToLogin: (() -> Unit)? = null,
    onSetupCompleted: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var companyName by remember { mutableStateOf(existingBusiness?.businessName ?: "") }
    var email by remember { mutableStateOf(existingBusiness?.email ?: "") }
    var gstin by remember { mutableStateOf(existingBusiness?.gstin ?: "") }
    var selectedBusinessType by remember { mutableStateOf(existingBusiness?.businessType.takeIf { !it.isNullOrBlank() } ?: BUSINESS_TYPES[0]) }
    var selectedTurnover by remember { mutableStateOf(existingBusiness?.turnover.takeIf { !it.isNullOrBlank() } ?: TURNOVER_TIERS[0]) }
    var phone by remember { mutableStateOf(existingBusiness?.phone ?: "") }
    var state by remember { mutableStateOf(existingBusiness?.state ?: "Delhi") }
    var address by remember { mutableStateOf(existingBusiness?.address ?: "") }
    var upiId by remember { mutableStateOf(existingBusiness?.upiId ?: "") }
    var bankName by remember { mutableStateOf(existingBusiness?.bankName ?: "") }
    var accountNumber by remember { mutableStateOf(existingBusiness?.accountNumber ?: "") }
    var ifscCode by remember { mutableStateOf(existingBusiness?.ifscCode ?: "") }
    var invoicePrefix by remember { mutableStateOf(existingBusiness?.invoicePrefix ?: "INV-26-") }

    var expandedStateDropdown by remember { mutableStateOf(false) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                isFirstTimeLaunch -> "Business Details"
                                existingBusiness == null -> "Company Setup"
                                else -> "Edit Company Profile"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isFirstTimeLaunch) "Step 1 of 1: Initial business setup" else "Configure real business identity • Zero fake sales",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isFirstTimeLaunch && onNavigateToLogin != null) {
                        TextButton(onClick = onNavigateToLogin) {
                            Text("Sign In", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("company_setup_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlueContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogoAvatar(
                            sizeDp = 42,
                            showGlow = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Business Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryBlue
                            )
                            Text(
                                text = "These details will appear on your tax invoices, receipts, and UPI QR codes.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. Company Name (Required)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Company / Business Name *",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        placeholder = { Text("e.g. Bright Horizon Trading Co.") },
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = PrimaryBlue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. Gmail / Email (Required)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Gmail / Business Email *",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("e.g. mycompany@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_email_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 3. GST Number (Explicitly Optional)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GST Number",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = TealSuccessContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Optional",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealSuccessText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Leave blank if you are not registered under GST or turnover is below ₹20L.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        placeholder = { Text("e.g. 07AAAAA0000A1Z5 (15-digit GSTIN)") },
                        leadingIcon = {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Gray)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_gstin_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 4. Type of Business (Required)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Type of Business *",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BUSINESS_TYPES.forEach { type ->
                            val isSelected = selectedBusinessType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBusinessType = type },
                                label = { Text(type, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // 5. Annual Turnover (Required)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Estimated Annual Turnover *",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TURNOVER_TIERS.forEach { tier ->
                            val isSelected = selectedTurnover == tier
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryBlueContainer else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTurnover = tier }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedTurnover = tier },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tier,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Contact Phone & State
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Phone Number", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("9876543210") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("company_phone_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("State / UT *", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Box {
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                trailingIcon = {
                                    IconButton(onClick = { expandedStateDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select State")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("company_state_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedStateDropdown,
                                onDismissRequest = { expandedStateDropdown = false }
                            ) {
                                INDIAN_STATES.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            state = st
                                            expandedStateDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Business Address
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Office / Shop Address", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("Plot/Shop number, Street, City, Pincode") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_address_input"),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 8. Optional UPI ID for QR codes
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UPI ID for Instant Payments", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(Optional)", fontSize = 11.sp, color = Color.Gray)
                    }
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        placeholder = { Text("e.g. businessname@okaxis or 9876543210@upi") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("company_upi_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 9. Bank Account Details (Optional)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bank Account (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        placeholder = { Text("e.g. HDFC Bank, State Bank of India") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("Account Number") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase() },
                            label = { Text("IFSC Code") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 10. Invoice Prefix
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Invoice Prefix", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = invoicePrefix,
                        onValueChange = { invoicePrefix = it },
                        placeholder = { Text("e.g. INV-26-") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 11. Save Company Setup Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val finalEmail = email.trim().ifBlank {
                            if (phone.isNotBlank()) "${phone.trim()}@billbook.local" else "owner@billbook.local"
                        }
                        viewModel.saveCompanySetup(
                            businessName = companyName,
                            email = finalEmail,
                            gstin = gstin,
                            businessType = selectedBusinessType,
                            turnover = selectedTurnover,
                            phone = phone,
                            address = address,
                            state = state,
                            upiId = upiId,
                            bankName = bankName,
                            accountNumber = accountNumber,
                            ifscCode = ifscCode,
                            invoicePrefix = invoicePrefix
                        )
                        onSetupCompleted()
                    },
                    enabled = companyName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick(scaleDown = 0.95f)
                        .testTag("save_company_setup_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFirstTimeLaunch) "Save & Continue to Home" else if (existingBusiness == null) "Save & Start Clean Billing" else "Update Company Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isFirstTimeLaunch && onNavigateToLogin != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Already registered?", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = onNavigateToLogin) {
                            Text("Sign In here", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                    }
                }
            }
        }
    }
}
