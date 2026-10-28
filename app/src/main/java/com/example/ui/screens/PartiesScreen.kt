package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PartyEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel
import kotlin.math.roundToInt

@Composable
fun PartiesScreen(
    viewModel: BillBookViewModel
) {
    val context = LocalContext.current
    val allParties by viewModel.parties.collectAsStateWithLifecycle()
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Customers, 1: Suppliers
    var searchQuery by remember { mutableStateOf("") }
    var partyToEdit by remember { mutableStateOf<PartyEntity?>(null) }
    var isAddingParty by remember { mutableStateOf(false) }
    var selectedPartyForLedger by remember { mutableStateOf<PartyEntity?>(null) }
    var partyToDelete by remember { mutableStateOf<PartyEntity?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }

    val partyType = if (selectedTab == 0) "CUSTOMER" else "SUPPLIER"

    val filteredParties = remember(allParties, selectedTab, searchQuery) {
        allParties.filter { p ->
            p.type == partyType &&
                    (p.name.contains(searchQuery, ignoreCase = true) ||
                     p.phone.contains(searchQuery, ignoreCase = true) ||
                     p.gstin.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalBalance = filteredParties.sumOf { it.currentBalance }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingParty = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(if (selectedTab == 0) "Add Customer" else "Add Supplier") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_party_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("parties_screen")
        ) {
            // Customer / Supplier Tab Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Customers (${allParties.count { it.type == "CUSTOMER" }})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Suppliers (${allParties.count { it.type == "SUPPLIER" }})", fontWeight = FontWeight.Bold) }
                )
            }

            // Summary Banner
            Surface(
                color = if (selectedTab == 0) TealSuccessContainer.copy(alpha = 0.5f) else RoseErrorContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "Total You'll Receive" else "Total You'll Pay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedTab == 0) TealSuccessText else RoseErrorText
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatCurrency(kotlin.math.abs(totalBalance)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) TealSuccessText else RoseErrorText
                        )
                        if (allParties.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showDeleteAllConfirmDialog = true },
                                modifier = Modifier.size(28.dp).testTag("delete_all_parties_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all parties",
                                    tint = RoseErrorText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, phone, GSTIN...") },
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

            // Parties List
            if (filteredParties.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No ${partyType.lowercase()}s found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap '+ Add ${partyType.lowercase().replaceFirstChar { it.uppercase() }}' to add one", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredParties, key = { it.id }) { party ->
                        PartyCard(
                            party = party,
                            onClick = { selectedPartyForLedger = party },
                            onEdit = { partyToEdit = party },
                            onDelete = { partyToDelete = party },
                            onSendReminder = {
                                val busName = currentBusiness?.businessName ?: "Our Store"
                                val upi = currentBusiness?.upiId ?: ""
                                val msg = "Dear ${party.name},\nThis is a gentle payment reminder from $busName for outstanding dues of ${formatCurrency(party.currentBalance)}.\nPlease transfer via UPI to $upi.\nThank you!"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder"))
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Party Dialog
    if (isAddingParty || partyToEdit != null) {
        PartyEditorDialog(
            party = partyToEdit,
            defaultType = partyType,
            onDismiss = {
                isAddingParty = false
                partyToEdit = null
            },
            onSave = { updated ->
                viewModel.addOrUpdateParty(updated)
                isAddingParty = false
                partyToEdit = null
            }
        )
    }

    // Party Ledger BottomSheet/Dialog
    selectedPartyForLedger?.let { party ->
        val partyInvoices = invoices.filter { it.invoice.partyId == party.id }
        val partyPayments = payments.filter { it.partyId == party.id }

        AlertDialog(
            onDismissRequest = { selectedPartyForLedger = null },
            title = {
                Column {
                    Text(party.name, fontWeight = FontWeight.Bold)
                    Text("Ledger & Statement", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    // Balance Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (party.currentBalance > 0) RoseErrorContainer else TealSuccessContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Outstanding", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                formatCurrency(kotlin.math.abs(party.currentBalance)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Transaction History", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (partyInvoices.isEmpty() && partyPayments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No billing or payment history yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(partyInvoices) { inv ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(inv.invoice.invoiceNumber, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(formatDate(inv.invoice.date), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatCurrency(inv.invoice.grandTotal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Bill (${inv.invoice.status})", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                            items(partyPayments) { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Payment Received (${p.paymentMode})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealSuccess)
                                        Text(formatDate(p.date), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("-${formatCurrency(p.amount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TealSuccess)
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPartyForLedger = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Single Party Dialog
    partyToDelete?.let { party ->
        AlertDialog(
            onDismissRequest = { partyToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RoseError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete ${party.name}?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to remove this ${party.type.lowercase()}? All balance history associated with this party will be removed.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteParty(party.id)
                        partyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { partyToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete All Parties Dialog
    if (showDeleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RoseError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove All Users / Parties?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "This will delete all customers and suppliers from the system. You will start with a completely clean ledger.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllParties()
                        showDeleteAllConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PartyCard(
    party: PartyEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendReminder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = party.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = party.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${party.phone} • ${party.state}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(kotlin.math.abs(party.currentBalance)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (party.currentBalance > 0) RoseError else TealSuccess
                    )
                    Text(
                        text = if (party.currentBalance > 0) "Receivable" else if (party.currentBalance < 0) "Payable" else "Settled",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (party.gstin.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GSTIN: ${party.gstin}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Ledger", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (party.currentBalance > 0) {
                        FilledTonalButton(
                            onClick = onSendReminder,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TealSuccessContainer,
                                contentColor = TealSuccessText
                            )
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remind", fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Party", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_party_${party.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Party",
                            modifier = Modifier.size(16.dp),
                            tint = RoseErrorText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartyEditorDialog(
    party: PartyEntity?,
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (PartyEntity) -> Unit
) {
    var name by remember { mutableStateOf(party?.name ?: "") }
    var phone by remember { mutableStateOf(party?.phone ?: "") }
    var email by remember { mutableStateOf(party?.email ?: "") }
    var gstin by remember { mutableStateOf(party?.gstin ?: "") }
    var address by remember { mutableStateOf(party?.address ?: "") }
    var state by remember { mutableStateOf(party?.state ?: "Delhi") }
    var openingBal by remember { mutableStateOf(party?.openingBalance?.toString() ?: "0.0") }
    var creditLimit by remember { mutableStateOf(party?.creditLimit?.toString() ?: "50000.0") }
    var type by remember { mutableStateOf(party?.type ?: defaultType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (party == null) "Add Party" else "Edit Party", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = type == "CUSTOMER",
                            onClick = { type = "CUSTOMER" },
                            label = { Text("Customer") }
                        )
                        FilterChip(
                            selected = type == "SUPPLIER",
                            onClick = { type = "SUPPLIER" },
                            label = { Text("Supplier") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Party / Business Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it.uppercase() },
                        label = { Text("GSTIN (Optional)") },
                        placeholder = { Text("e.g. 07AAAAA0000A1Z5") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State (for GST calculation)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = openingBal,
                        onValueChange = { openingBal = it },
                        label = { Text("Opening Balance (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val ob = openingBal.toDoubleOrNull() ?: 0.0
                        val cl = creditLimit.toDoubleOrNull() ?: 50000.0
                        val updated = (party ?: PartyEntity(name = name)).copy(
                            name = name,
                            phone = phone,
                            email = email,
                            gstin = gstin,
                            address = address,
                            state = state,
                            openingBalance = ob,
                            currentBalance = if (party == null) ob else party.currentBalance,
                            creditLimit = cl,
                            type = type
                        )
                        onSave(updated)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
