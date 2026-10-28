package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ItemEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

@Composable
fun InventoryScreen(
    viewModel: BillBookViewModel
) {
    val context = LocalContext.current
    val allItems by viewModel.items.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var isAddingItem by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var itemToAdjustStock by remember { mutableStateOf<ItemEntity?>(null) }

    val categories = remember(allItems) {
        listOf("All") + allItems.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredItems = remember(allItems, searchQuery, selectedCategory, showOnlyLowStock) {
        allItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    item.sku.contains(searchQuery, ignoreCase = true) ||
                    item.hsnCode.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesLowStock = !showOnlyLowStock || (item.stockQty <= item.minStockAlert)

            matchesSearch && matchesCategory && matchesLowStock
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddingItem = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add New Item") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_item_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("inventory_screen")
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search items by name, SKU, HSN...") },
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

            // Category & Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = { Text("⚠️ Low Stock", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseErrorContainer,
                            selectedLabelColor = RoseErrorText
                        )
                    )
                }

                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat && !showOnlyLowStock,
                        onClick = {
                            selectedCategory = cat
                            showOnlyLowStock = false
                        },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No inventory items found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap '+ Add New Item' to create products or services", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            onEdit = { itemToEdit = item },
                            onAdjustStock = { itemToAdjustStock = item },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (isAddingItem || itemToEdit != null) {
        ItemEditorDialog(
            item = itemToEdit,
            onDismiss = {
                isAddingItem = false
                itemToEdit = null
            },
            onSave = { updated ->
                viewModel.addOrUpdateItem(updated)
                searchQuery = ""
                selectedCategory = "All"
                showOnlyLowStock = false
                isAddingItem = false
                itemToEdit = null
                Toast.makeText(context, "Item '${updated.name}' saved successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Stock Adjustment Dialog
    itemToAdjustStock?.let { item ->
        var adjustmentType by remember { mutableStateOf("ADD") } // ADD or REDUCE
        var qtyText by remember { mutableStateOf("1") }
        var reason by remember { mutableStateOf("Stock replenishment") }

        AlertDialog(
            onDismissRequest = { itemToAdjustStock = null },
            title = { Text("Adjust Stock: ${item.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Stock: ${item.stockQty.toInt()} ${item.unit}", fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = adjustmentType == "ADD",
                            onClick = {
                                adjustmentType = "ADD"
                                reason = "Stock replenishment"
                            },
                            label = { Text("+ Stock In") }
                        )
                        FilterChip(
                            selected = adjustmentType == "REDUCE",
                            onClick = {
                                adjustmentType = "REDUCE"
                                reason = "Damage / Loss / Correction"
                            },
                            label = { Text("- Stock Out") }
                        )
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity (${item.unit})") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason / Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = qtyText.toDoubleOrNull() ?: 0.0
                        val delta = if (adjustmentType == "ADD") qty else -qty
                        viewModel.adjustItemStock(item.id, delta)
                        itemToAdjustStock = null
                    }
                ) {
                    Text("Update Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToAdjustStock = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InventoryItemCard(
    item: ItemEntity,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.stockQty <= item.minStockAlert

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    val details = buildList {
                        add(item.category)
                        if (item.hsnCode.isNotBlank()) add("HSN/SAC: ${item.hsnCode}")
                        if (item.sku.isNotBlank()) add("SKU: ${item.sku}")
                    }.joinToString(" • ")
                    Text(
                        text = details,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLowStock) RoseErrorContainer else TealSuccessContainer
                ) {
                    Text(
                        text = "${item.stockQty.toInt()} ${item.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isLowStock) RoseErrorText else TealSuccessText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sale Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(item.salePrice),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text("Purchase Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(item.purchasePrice),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Column {
                    Text("GST Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val formattedGst = if (item.taxRate % 1.0 == 0.0) "${item.taxRate.toInt()}%" else "${item.taxRate}%"
                    Text(
                        formattedGst,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
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
                OutlinedButton(
                    onClick = onAdjustStock,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Stock", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = RoseError, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ItemEditorDialog(
    item: ItemEntity?,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var sku by remember { mutableStateOf(item?.sku ?: "") }
    var hsnCode by remember { mutableStateOf(item?.hsnCode ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Electronics") }
    var unit by remember { mutableStateOf(item?.unit ?: "PCS") }
    var salePrice by remember { mutableStateOf(item?.salePrice?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var purchasePrice by remember { mutableStateOf(item?.purchasePrice?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var mrp by remember { mutableStateOf(item?.mrp?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var taxRate by remember {
        mutableStateOf(
            item?.taxRate?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "18"
        )
    }
    var stockQty by remember { mutableStateOf(item?.stockQty?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "10") }
    var minStockAlert by remember { mutableStateOf(item?.minStockAlert?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Product / Item" else "Edit Item", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g. Wireless Mouse, Grocery Item, Service") },
                        isError = nameError,
                        supportingText = {
                            if (nameError) {
                                Text("Item Name is required to save", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            } else {
                                Text("Required product or service name", fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_name_input"),
                        singleLine = true
                    )
                }

                // Optional SKU & HSN / SAC Code
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU / Barcode (Optional)") },
                            placeholder = { Text("Optional") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hsnCode,
                            onValueChange = { hsnCode = it },
                            label = { Text("HSN / SAC Code (Optional)") },
                            placeholder = { Text("Optional") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (PCS, KGS..)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^₹?\s*\d*\.?\d*$"""))) {
                                    salePrice = input.replace("₹", "").trim()
                                }
                            },
                            label = { Text("Sale Price (₹)") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^₹?\s*\d*\.?\d*$"""))) {
                                    purchasePrice = input.replace("₹", "").trim()
                                }
                            },
                            label = { Text("Purchase Price (₹)") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Custom GST percentage writing & quick presets
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "GST Tax Rate",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${taxRate.ifBlank { "0" }}% GST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Direct text field so the user can write ANY GST percentage
                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                                    taxRate = input
                                }
                            },
                            label = { Text("Write GST % on Product") },
                            placeholder = { Text("Enter GST %, e.g. 18, 5, 12, 0.25, 3, 0") },
                            trailingIcon = {
                                Text(
                                    "%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("Type custom % or tap a preset chip below") }
                        )

                        Text(
                            "Quick Presets (tap to apply or edit above):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presetRates = listOf("0", "0.25", "3", "5", "12", "18", "28", "40")
                            items(presetRates) { rate ->
                                FilterChip(
                                    selected = taxRate == rate,
                                    onClick = { taxRate = rate },
                                    label = { Text("$rate%") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stockQty,
                            onValueChange = { stockQty = it },
                            label = { Text("Initial Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockAlert,
                            onValueChange = { minStockAlert = it },
                            label = { Text("Low Alert Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isBlank()) {
                        nameError = true
                        return@Button
                    }

                    val sp = salePrice.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                    val pp = purchasePrice.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                    val m = mrp.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: sp
                    val tr = taxRate.replace("%", "").trim().toDoubleOrNull() ?: 0.0
                    val sq = stockQty.toDoubleOrNull() ?: 0.0
                    val minS = minStockAlert.toDoubleOrNull() ?: 5.0

                    // SKU and HSN/SAC are optional and can be saved empty without issue
                    val updated = (item ?: ItemEntity(name = cleanName)).copy(
                        name = cleanName,
                        sku = sku.trim(),
                        hsnCode = hsnCode.trim(),
                        category = category.trim().ifBlank { "General" },
                        unit = unit.trim().ifBlank { "PCS" },
                        salePrice = sp,
                        purchasePrice = pp,
                        mrp = m,
                        taxRate = tr,
                        stockQty = sq,
                        minStockAlert = minS
                    )
                    onSave(updated)
                },
                modifier = Modifier.testTag("save_item_button")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
