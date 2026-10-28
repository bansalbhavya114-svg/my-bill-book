package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.graphicsLayer
import com.example.data.model.InvoiceWithItems
import com.example.ui.components.BillBookTopAppBar
import com.example.ui.components.SpatialAmbientBackground
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

sealed class NavigationTab(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Dashboard : NavigationTab("dashboard", "Home", Icons.Default.Dashboard)
    data object Invoices : NavigationTab("invoices", "Bills", Icons.AutoMirrored.Filled.ReceiptLong)
    data object Parties : NavigationTab("parties", "Parties", Icons.Default.People)
    data object Inventory : NavigationTab("inventory", "Items", Icons.Default.Inventory2)
    data object Reports : NavigationTab("reports", "Reports", Icons.Default.Assessment)
}

@Composable
fun MainScreen(
    viewModel: BillBookViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val hasCompletedFirstTimeSetup by viewModel.hasCompletedFirstTimeSetup.collectAsStateWithLifecycle()
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val allBusinesses by viewModel.businesses.collectAsStateWithLifecycle()

    var showSignInDirectly by remember { mutableStateOf(false) }

    // Case 1: First time opening app (start directly from business details page ONLY for 1 time, no auto business created)
    if (!hasCompletedFirstTimeSetup && !isLoggedIn && allBusinesses.isEmpty() && !showSignInDirectly) {
        CompanySetupScreen(
            viewModel = viewModel,
            existingBusiness = null,
            isFirstTimeLaunch = true,
            onNavigateToLogin = {
                showSignInDirectly = true
            },
            onSetupCompleted = {
                // saveCompanySetup sets isLoggedIn = true and marks first-time setup completed
                // jumping directly to the Home page
            }
        )
        return
    }

    // Case 2: Not logged in (after logout or user selected Sign In)
    if (!isLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {
                // After login, make user jump directly to home page
            }
        )
        return
    }

    var currentTab by remember { mutableStateOf<NavigationTab>(NavigationTab.Dashboard) }
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val selectedFY by viewModel.selectedFY.collectAsStateWithLifecycle()
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.items.collectAsStateWithLifecycle()

    var showInvoiceEditor by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var selectedInvoiceForView by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var showBusinessSetup by remember { mutableStateOf(false) }
    var showResetDataDialog by remember { mutableStateOf(false) }

    // App Lock State
    var isAppLocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    val correctPin = "1234"

    if (isAppLocked) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("BillBook Protected", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Enter PIN (Default: 1234) to unlock financial data", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4) {
                            enteredPin = it
                            if (it == correctPin) {
                                isAppLocked = false
                                enteredPin = ""
                            }
                        }
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (enteredPin == correctPin) {
                            isAppLocked = false
                            enteredPin = ""
                        }
                    }
                ) {
                    Text("Unlock")
                }
            }
        }
        return
    }

    // Full page Company Setup when requested
    if (showBusinessSetup) {
        CompanySetupScreen(
            viewModel = viewModel,
            existingBusiness = currentBusiness,
            onSetupCompleted = { showBusinessSetup = false },
            onBack = { showBusinessSetup = false }
        )
        return
    }

    Scaffold(
        topBar = {
            BillBookTopAppBar(
                currentBusiness = currentBusiness,
                allBusinesses = allBusinesses,
                onSelectBusiness = { viewModel.switchBusiness(it) },
                onAddBusiness = { showBusinessSetup = true },
                syncStatus = syncStatus,
                isSyncing = isSyncing,
                onSyncClick = { viewModel.triggerSync() },
                selectedFY = selectedFY,
                onSelectFY = { viewModel.setFinancialYear(it) },
                onLockApp = { isAppLocked = true },
                onCompanySetup = { showBusinessSetup = true },
                onLogout = { viewModel.logout() },
                onResetData = { showResetDataDialog = true },
                onRemoveAllUsers = { viewModel.removeAllUsers() }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    tonalElevation = 3.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                Color(0x3394A3B8),
                                Color(0x2238BDF8)
                            )
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            NavigationTab.Dashboard,
                            NavigationTab.Invoices,
                            NavigationTab.Parties,
                            NavigationTab.Inventory,
                            NavigationTab.Reports
                        )
                        tabs.forEach { tab ->
                            val isSelected = currentTab == tab
                            val animatedBgColor by animateColorAsState(
                                targetValue = if (isSelected) PrimaryBlue else Color.Transparent,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "navTabBg_${tab.route}"
                            )
                            val animatedContentColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else TextSecondary,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "navTabContent_${tab.route}"
                            )
                            val tabScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "navTabScale_${tab.route}"
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = animatedBgColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .padding(horizontal = 2.dp)
                                    .bounceClick(scaleDown = 0.92f, onClick = { currentTab = tab })
                                    .testTag("nav_tab_${tab.route}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        tint = animatedContentColor,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer {
                                                scaleX = tabScale
                                                scaleY = tabScale
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = animatedContentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        SpatialAmbientBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(animationSpec = tween(180)))
                },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    NavigationTab.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onCreateInvoiceClick = {
                            invoiceToEdit = null
                            showInvoiceEditor = true
                        },
                        onViewInvoiceClick = { inv ->
                            selectedInvoiceForView = inv
                        },
                        onNavigateToInventory = { currentTab = NavigationTab.Inventory },
                        onNavigateToParties = { currentTab = NavigationTab.Parties },
                        onNavigateToReports = { currentTab = NavigationTab.Reports },
                        onEditCompany = { showBusinessSetup = true }
                    )
                    NavigationTab.Invoices -> InvoicesScreen(
                        viewModel = viewModel,
                        onCreateInvoiceClick = {
                            invoiceToEdit = null
                            showInvoiceEditor = true
                        },
                        onViewInvoiceClick = { inv ->
                            selectedInvoiceForView = inv
                        }
                    )
                    NavigationTab.Parties -> PartiesScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.Inventory -> InventoryScreen(
                        viewModel = viewModel
                    )
                    NavigationTab.Reports -> ReportsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Invoice Editor Dialog
    if (showInvoiceEditor) {
        InvoiceEditorDialog(
            currentBusiness = currentBusiness,
            partiesList = parties,
            inventoryItems = inventoryItems,
            existingInvoice = invoiceToEdit,
            onDismiss = {
                showInvoiceEditor = false
                invoiceToEdit = null
            },
            onSave = { invoice, items ->
                viewModel.createOrUpdateInvoice(invoice, items)
                showInvoiceEditor = false
                invoiceToEdit = null
            }
        )
    }

    // Invoice Viewer Dialog
    selectedInvoiceForView?.let { invWithItems ->
        InvoiceViewerDialog(
            invoiceWithItems = invWithItems,
            currentBusiness = currentBusiness,
            onDismiss = { selectedInvoiceForView = null },
            onRecordPayment = { paidAmt, mode ->
                viewModel.recordInvoicePayment(
                    invoiceId = invWithItems.invoice.id,
                    total = invWithItems.invoice.grandTotal,
                    amount = invWithItems.invoice.paidAmount + paidAmt,
                    mode = mode,
                    partyId = invWithItems.invoice.partyId,
                    partyName = invWithItems.invoice.partyName
                )
                selectedInvoiceForView = null
            },
            onConvertEstimate = {
                viewModel.convertEstimateToInvoice(invWithItems)
                selectedInvoiceForView = null
            }
        )
    }

    // Reset Data Confirmation Dialog
    if (showResetDataDialog) {
        var newShopName by remember { mutableStateOf("My Fresh Store") }
        AlertDialog(
            onDismissRequest = { showResetDataDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = com.example.ui.theme.RoseError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Data?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Are you sure you want to reset all data? All bills, inventory items, and customer balances will be wiped.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newShopName,
                        onValueChange = { newShopName = it },
                        label = { Text("Store / Business Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData(newShopName, "Delhi")
                        showResetDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.RoseError)
                ) {
                    Text("Yes, Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
