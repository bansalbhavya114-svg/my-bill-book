package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.InvoiceWithItems
import com.example.ui.theme.*
import androidx.compose.animation.core.*
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.core.content.FileProvider
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sin

// Formatters
fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount).replace("INR", "₹")
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillBookTopAppBar(
    currentBusiness: BusinessProfileEntity?,
    allBusinesses: List<BusinessProfileEntity>,
    onSelectBusiness: (Long) -> Unit,
    onAddBusiness: () -> Unit,
    syncStatus: String,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    selectedFY: String,
    onSelectFY: (String) -> Unit,
    onLockApp: () -> Unit,
    onCompanySetup: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onResetData: (() -> Unit)? = null,
    onRemoveAllUsers: (() -> Unit)? = null
) {
    var showBusinessMenu by remember { mutableStateOf(false) }
    var showFYMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color.White.copy(alpha = 0.96f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0x1F94A3B8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Business Switcher
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBusinessMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrandLogoAvatar(
                            sizeDp = 36,
                            showGlow = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentBusiness?.businessName ?: "My Business",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Business",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "GSTIN: ${currentBusiness?.gstin?.ifBlank { "Unregistered" } ?: ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showBusinessMenu,
                        onDismissRequest = { showBusinessMenu = false }
                    ) {
                        Text(
                            "Select Business",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        allBusinesses.forEach { b ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(b.businessName, fontWeight = if (b.id == currentBusiness?.id) FontWeight.Bold else FontWeight.Normal)
                                        Text("${b.state} • ${b.role}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    onSelectBusiness(b.id)
                                    showBusinessMenu = false
                                },
                                leadingIcon = {
                                    if (b.id == currentBusiness?.id) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = TealSuccess)
                                    } else {
                                        Icon(Icons.Default.Business, contentDescription = null)
                                    }
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Business", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                showBusinessMenu = false
                                onAddBusiness()
                            },
                            leadingIcon = { Icon(Icons.Default.AddBusiness, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }
                }

                // Right controls: Sync status + FY chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSyncing) MaterialTheme.colorScheme.primaryContainer else TealSuccessContainer,
                        modifier = Modifier
                            .clickable { onSyncClick() }
                            .padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                AnimatedRadarBeacon(color = TealSuccess, sizeDp = 7)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "Syncing..." else "Synced",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSyncing) MaterialTheme.colorScheme.primary else TealSuccessText
                            )
                        }
                    }

                    // Financial Year Tag
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { showFYMenu = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedFY,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showFYMenu,
                            onDismissRequest = { showFYMenu = false }
                        ) {
                            listOf("FY 2026-27", "FY 2025-26", "FY 2024-25").forEach { fy ->
                                DropdownMenuItem(
                                    text = { Text(fy) },
                                    onClick = {
                                        onSelectFY(fy)
                                        showFYMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Options menu (Lock, Reset Data, Logout)
                    Box(modifier = Modifier.padding(start = 2.dp)) {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            if (onCompanySetup != null) {
                                DropdownMenuItem(
                                    text = { Text("Company Setup") },
                                    onClick = {
                                        showSettingsMenu = false
                                        onCompanySetup()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryBlue) }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text("Sync to Supabase Now") },
                                onClick = {
                                    showSettingsMenu = false
                                    onSyncClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, tint = PrimaryBlue) }
                            )
                            DropdownMenuItem(
                                text = { Text("Lock BillBook") },
                                onClick = {
                                    showSettingsMenu = false
                                    onLockApp()
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                            if (onRemoveAllUsers != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove All Users", color = RoseErrorText) },
                                    onClick = {
                                        showSettingsMenu = false
                                        onRemoveAllUsers()
                                    },
                                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = RoseError) }
                                )
                            }
                            if (onResetData != null) {
                                DropdownMenuItem(
                                    text = { Text("Reset All Data", color = RoseErrorText) },
                                    onClick = {
                                        showSettingsMenu = false
                                        onResetData()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = RoseError) }
                                )
                            }
                            if (onLogout != null) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Log Out") },
                                    onClick = {
                                        showSettingsMenu = false
                                        onLogout()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }

            if (syncStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (syncStatus.contains("error", ignoreCase = true) || syncStatus.contains("offline", ignoreCase = true)) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (syncStatus.contains("error", ignoreCase = true) || syncStatus.contains("offline", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = syncStatus,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, textColor) = when (status.uppercase()) {
        "PAID" -> Pair(TealSuccessContainer, TealSuccessText)
        "UNPAID" -> Pair(RoseErrorContainer, RoseErrorText)
        "PARTIAL" -> Pair(AmberWarningContainer, AmberWarningText)
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = status.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TypeBadge(type: String) {
    val (label, bg, color) = when (type) {
        "GST_INVOICE" -> Triple("GST INVOICE", Color(0xFFEFF6FF), PrimaryBlue)
        "BILL_OF_SUPPLY" -> Triple("BILL OF SUPPLY", Color(0xFFF0FDF4), Color(0xFF166534))
        "ESTIMATE_QUOTE" -> Triple("ESTIMATE", Color(0xFFFAF5FF), PurpleAccent)
        "PROFORMA" -> Triple("PROFORMA", Color(0xFFFEF3C7), Color(0xFFB45309))
        "CREDIT_NOTE" -> Triple("CREDIT NOTE", Color(0xFFFFE4E6), RoseErrorText)
        else -> Triple(type, Color(0xFFF1F5F9), Color.Black)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// 100% Valid and Scannable QR Code generator using ZXing
fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap? {
    if (payload.isBlank()) return null
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Valid, scannable UPI QR Code Component for Invoices
@Composable
fun UpiQrCodeCanvas(
    upiPayload: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 160
) {
    val qrBitmap = remember(upiPayload) {
        generateQrBitmap(upiPayload, (sizeDp * 3).coerceAtLeast(260))
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Valid UPI Payment QR Code",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size((sizeDp / 2).dp)
                )
                Text("Invalid UPI", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

// Interactive Animation Modifiers
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick?.invoke() }
            )
        }
}

fun Modifier.floatingEffect(
    offsetY: androidx.compose.ui.unit.Dp = 6.dp,
    durationMs: Int = 2400
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )
    this.graphicsLayer {
        translationY = (dy - 0.5f) * offsetY.toPx()
    }
}

// Animated Radar Pulse Beacon Ring for Live Cloud Status
@Composable
fun AnimatedRadarBeacon(
    color: Color = TealSuccess,
    sizeDp: Int = 9
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size((sizeDp * 2.2).dp)
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .background(color, CircleShape)
        )
    }
}

// Animated Currency Counting Display
@Composable
fun AnimatedCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: androidx.compose.ui.unit.TextUnit = (-0.5).sp
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "currencyCount"
    )

    Text(
        text = formatCurrency(animatedAmount.toDouble()),
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        letterSpacing = letterSpacing,
        modifier = modifier
    )
}

// Brand Logo Avatar with Glowing Rim
@Composable
fun BrandLogoAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 40,
    showGlow: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .floatingEffect(offsetY = 3.dp, durationMs = 2800)
    ) {
        if (showGlow) {
            Surface(
                shape = shape,
                color = Color(0x3338BDF8),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
            ) {}
        }
        Image(
            painter = painterResource(id = R.drawable.billbook_cyber_logo_1793193290394),
            contentDescription = "BillBook Logo",
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFF59E0B))
                    ),
                    shape = shape
                )
        )
    }
}

// Spatial UI Glass Surface Card with Spring Click & Holographic Rim
@Composable
fun SpatialGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = Color.White.copy(alpha = 0.95f),
    borderColor: Color = Color.White.copy(alpha = 0.85f),
    elevationDp: Int = 3,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.bounceClick(scaleDown = 0.97f, onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.5f),
                        Color(0x3338BDF8),
                        Color(0x22818CF8)
                    )
                ),
                shape = shape
            ),
        shape = shape,
        color = containerColor,
        shadowElevation = elevationDp.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// Spatial Ambient Canvas Background
@Composable
fun SpatialAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9),
                        Color(0xFFEEF2F6)
                    )
                )
            )
    ) {
        // Subtle ambient glowing depth circles in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x0C2563EB), // gentle primary blue ambient glow
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.85f, size.height * 0.12f)
            )
            drawCircle(
                color = Color(0x0E0D9488), // gentle teal ambient glow
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.15f, size.height * 0.55f)
            )
            drawCircle(
                color = Color(0x0A7C3AED), // gentle purple ambient glow
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.75f, size.height * 0.85f)
            )
        }
        content()
    }
}

// Render complete crisp photo bitmap of the invoice bill
fun renderInvoiceBitmap(context: Context, invoiceWithItems: InvoiceWithItems, currentBusiness: BusinessProfileEntity?): Bitmap {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items
    val businessName = currentBusiness?.businessName ?: "My Business"
    val businessGstin = currentBusiness?.gstin ?: ""
    val address = currentBusiness?.address ?: ""
    val cityState = currentBusiness?.state ?: ""
    val phone = currentBusiness?.phone ?: ""
    val upiId = currentBusiness?.upiId ?: ""

    val width = 960
    val baseHeight = 940
    val itemRowHeight = 54
    val totalHeight = (baseHeight + items.size * itemRowHeight).coerceAtLeast(1080)

    val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Background Pure White
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

    // Outer crisp border
    val borderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E2E8F0")
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRect(8f, 8f, (width - 8).toFloat(), (totalHeight - 8).toFloat(), borderPaint)

    // Top Header Banner (Spatial Gradient Deep Slate)
    val headerHeight = 190f
    val headerPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), headerHeight,
            android.graphics.Color.parseColor("#0F172A"),
            android.graphics.Color.parseColor("#1E293B"),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(8f, 8f, (width - 8).toFloat(), headerHeight, headerPaint)

    // Business Name
    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 34f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(businessName, 40f, 60f, textPaint)

    // Business subtitle / address
    val subPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 20f
    }
    var currentHeaderY = 94f
    val contactLine = listOfNotNull(
        address.takeIf { it.isNotBlank() },
        cityState.takeIf { it.isNotBlank() },
        phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" }
    ).joinToString(" • ")
    if (contactLine.isNotBlank()) {
        canvas.drawText(contactLine, 40f, currentHeaderY, subPaint)
        currentHeaderY += 28f
    }

    if (businessGstin.isNotBlank()) {
        val gstinPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FBBF24") // gold accent
            textSize = 20f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText("GSTIN: $businessGstin", 40f, currentHeaderY, gstinPaint)
    }

    // Top Right Document Type Badge
    val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0D9488")
        style = android.graphics.Paint.Style.FILL
    }
    val badgeRect = android.graphics.RectF((width - 250).toFloat(), 35f, (width - 40).toFloat(), 85f)
    canvas.drawRoundRect(badgeRect, 16f, 16f, badgePaint)
    val badgeTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 21f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val docTitle = when (inv.type) {
        "ESTIMATE_QUOTE" -> "ESTIMATE / QUOTE"
        "BILL_OF_SUPPLY" -> "BILL OF SUPPLY"
        "PROFORMA" -> "PROFORMA INVOICE"
        else -> "TAX INVOICE"
    }
    canvas.drawText(docTitle, badgeRect.centerX(), badgeRect.centerY() + 8f, badgeTextPaint)

    // Meta Section: Bill To & Invoice Info
    var y = headerHeight + 35f

    // Billed to
    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#64748B")
        textSize = 17f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val darkPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 24f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("BILLED TO:", 40f, y, labelPaint)
    canvas.drawText(inv.partyName.ifBlank { "Cash / Counter Customer" }, 40f, y + 30f, darkPaint)

    val partySubPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#475569")
        textSize = 19f
    }
    var partyDetailY = y + 58f
    if (inv.partyPhone.isNotBlank()) {
        canvas.drawText("Phone: ${inv.partyPhone}", 40f, partyDetailY, partySubPaint)
        partyDetailY += 24f
    }
    if (inv.partyGstin.isNotBlank()) {
        canvas.drawText("GSTIN: ${inv.partyGstin}", 40f, partyDetailY, partySubPaint)
        partyDetailY += 24f
    }
    if (inv.partyAddress.isNotBlank()) {
        canvas.drawText("Address: ${inv.partyAddress.take(45)}", 40f, partyDetailY, partySubPaint)
    }

    // Right side: Invoice Details
    val rightAlignPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 21f
        textAlign = android.graphics.Paint.Align.RIGHT
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val rightLabelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#64748B")
        textSize = 18f
        textAlign = android.graphics.Paint.Align.RIGHT
    }
    val rx = (width - 40).toFloat()
    canvas.drawText("Invoice No: ${inv.invoiceNumber}", rx, y + 5f, rightAlignPaint)
    canvas.drawText("Date: ${formatDate(inv.date)}", rx, y + 32f, rightLabelPaint)
    if (inv.dueDate > 0) {
        canvas.drawText("Due Date: ${formatDate(inv.dueDate)}", rx, y + 56f, rightLabelPaint)
    }
    val statusColor = if (inv.status == "PAID") "#0D9488" else "#E11D48"
    val statusPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(statusColor)
        textSize = 20f
        textAlign = android.graphics.Paint.Align.RIGHT
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("Status: ${inv.status}", rx, y + 84f, statusPaint)

    // Items Table Header
    y = y + 130f
    val tableHeaderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#1E293B")
        style = android.graphics.Paint.Style.FILL
    }
    val tableHeaderRect = android.graphics.RectF(40f, y, (width - 40).toFloat(), y + 42f)
    canvas.drawRoundRect(tableHeaderRect, 8f, 8f, tableHeaderPaint)

    val thTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 17f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("#", 55f, y + 27f, thTextPaint)
    canvas.drawText("ITEM DESCRIPTION", 100f, y + 27f, thTextPaint)
    canvas.drawText("QTY", 520f, y + 27f, thTextPaint)
    canvas.drawText("RATE", 620f, y + 27f, thTextPaint)
    canvas.drawText("GST", 730f, y + 27f, thTextPaint)
    val thRightPaint = android.graphics.Paint(thTextPaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
    canvas.drawText("AMOUNT", (width - 55).toFloat(), y + 27f, thRightPaint)

    y += 44f

    // Rows
    val rowTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E293B")
        textSize = 19f
    }
    val rowBoldPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 19f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val rowRightBold = android.graphics.Paint(rowBoldPaint).apply { textAlign = android.graphics.Paint.Align.RIGHT }
    val altRowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#F8FAFC")
        style = android.graphics.Paint.Style.FILL
    }

    items.forEachIndexed { idx, item ->
        val rowTop = y
        val rowBottom = y + itemRowHeight
        if (idx % 2 == 1) {
            canvas.drawRect(40f, rowTop, (width - 40).toFloat(), rowBottom, altRowPaint)
        }
        val textY = rowTop + 33f

        canvas.drawText("${idx + 1}", 55f, textY, rowTextPaint)
        val nameToDraw = if (item.itemName.length > 28) item.itemName.take(26) + "..." else item.itemName
        canvas.drawText(nameToDraw, 100f, textY, rowBoldPaint)
        canvas.drawText("${item.quantity.toInt()} ${item.unit}", 520f, textY, rowTextPaint)
        canvas.drawText("₹${item.rate.roundToInt()}", 620f, textY, rowTextPaint)
        canvas.drawText("${item.taxRate.roundToInt()}%", 730f, textY, rowTextPaint)
        canvas.drawText("₹${item.totalAmount.roundToInt()}", (width - 55).toFloat(), textY, rowRightBold)

        y += itemRowHeight
    }

    // Divider line below table
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E2E8F0")
        strokeWidth = 2f
    }
    canvas.drawLine(40f, y, (width - 40).toFloat(), y, linePaint)
    y += 25f

    // Bottom Section: Left (UPI & QR) | Right (Totals Box)
    val upiBoxRect = android.graphics.RectF(40f, y, 420f, y + 210f)
    val upiBoxPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#F1F5F9")
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRoundRect(upiBoxRect, 14f, 14f, upiBoxPaint)

    // Draw real valid, scannable UPI QR code inside upiBox
    val cleanUpi = if (upiId.isNotBlank()) upiId.trim() else if (phone.isNotBlank()) "$phone@upi" else "merchant@upi"
    val upiQrPayload = "upi://pay?pa=$cleanUpi&pn=${Uri.encode(businessName)}&am=${String.format(Locale.US, "%.2f", inv.grandTotal)}&cu=INR&tn=${Uri.encode("Bill " + inv.invoiceNumber)}"
    val realQrBitmap = generateQrBitmap(upiQrPayload, 280)
    if (realQrBitmap != null) {
        val qrDst = android.graphics.RectF(52f, y + 16f, 52f + 145f, y + 16f + 145f)
        canvas.drawBitmap(realQrBitmap, null, qrDst, null)
    }

    // QR instructions
    val upiTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 17f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("Scan to Pay with UPI", 210f, y + 55f, upiTextPaint)
    val upiSubText = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#475569")
        textSize = 15f
    }
    val dispUpi = cleanUpi
    canvas.drawText(dispUpi, 210f, y + 85f, upiSubText)
    canvas.drawText("GPay • PhonePe • Paytm", 210f, y + 115f, upiSubText)

    // Right: Calculation & Grand Total Box
    val totalBoxLeft = 460f
    val totalBoxRight = (width - 40).toFloat()
    var ty = y + 25f

    fun drawSummaryLine(label: String, valStr: String, isBold: Boolean = false) {
        val lp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 20f
        }
        val vp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 20f
            textAlign = android.graphics.Paint.Align.RIGHT
            if (isBold) typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(label, totalBoxLeft, ty, lp)
        canvas.drawText(valStr, totalBoxRight, ty, vp)
        ty += 32f
    }

    drawSummaryLine("Subtotal:", "₹${inv.subtotal.roundToInt()}")
    drawSummaryLine("GST Total Tax:", "₹${inv.totalTax.roundToInt()}")
    if (inv.totalDiscount > 0) {
        drawSummaryLine("Discount:", "-₹${inv.totalDiscount.roundToInt()}")
    }

    // Grand Total Highlight Pill
    ty += 5f
    val gTotalRect = android.graphics.RectF(totalBoxLeft - 10f, ty, totalBoxRight + 10f, ty + 50f)
    val gTotalPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#0F172A")
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRoundRect(gTotalRect, 10f, 10f, gTotalPaint)

    val gTotalLabel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 22f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val gTotalVal = android.graphics.Paint(gTotalLabel).apply {
        color = android.graphics.Color.parseColor("#FBBF24") // gold
        textAlign = android.graphics.Paint.Align.RIGHT
    }
    canvas.drawText("GRAND TOTAL:", totalBoxLeft + 10f, ty + 33f, gTotalLabel)
    canvas.drawText("₹${inv.grandTotal.roundToInt()}", totalBoxRight - 10f, ty + 33f, gTotalVal)

    ty += 75f
    drawSummaryLine("Amount Paid:", "₹${inv.paidAmount.roundToInt()}", isBold = true)
    val balDue = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0).roundToInt()
    val balColor = if (balDue > 0) "#E11D48" else "#0D9488"
    val balPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(balColor)
        textSize = 21f
        textAlign = android.graphics.Paint.Align.RIGHT
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val balLabel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 21f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("Balance Due:", totalBoxLeft, ty, balLabel)
    canvas.drawText("₹$balDue", totalBoxRight, ty, balPaint)

    // Footer & Signature
    val footerY = (totalHeight - 40).toFloat()
    val footerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 16f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("Thank you for doing business with $businessName • Generated via BillBook", width / 2f, footerY, footerPaint)

    return bitmap
}

// Save invoice bitmap to application cache and get FileProvider content Uri
fun saveInvoiceBitmapToFile(context: Context, bitmap: Bitmap, invoiceNumber: String): Uri? {
    return try {
        val invoicesDir = File(context.cacheDir, "invoices")
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs()
        }
        val safeName = invoiceNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val imageFile = File(invoicesDir, "bill_${safeName}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// WhatsApp Share Formatter - shares the generated photo of the bill to WhatsApp
fun shareInvoiceWhatsApp(context: Context, invoiceWithItems: InvoiceWithItems, currentBusiness: BusinessProfileEntity?) {
    val inv = invoiceWithItems.invoice
    val businessName = currentBusiness?.businessName ?: "Our Store"
    val summaryCaption = "Invoice #${inv.invoiceNumber} from $businessName\nTotal: ₹${inv.grandTotal.roundToInt()}\nStatus: ${inv.status}\n\nGenerated with BillBook"

    try {
        val bitmap = renderInvoiceBitmap(context, invoiceWithItems, currentBusiness)
        val imageUri = saveInvoiceBitmapToFile(context, bitmap, inv.invoiceNumber)

        if (imageUri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, summaryCaption)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                Toast.makeText(context, "Opening WhatsApp with Bill Photo...", Toast.LENGTH_SHORT).show()
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // If WhatsApp is not directly installed, open chooser to share the photo via any app
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        putExtra(Intent.EXTRA_TEXT, summaryCaption)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share Bill Photo via"
                )
                context.startActivity(chooser)
            }
        } else {
            fallbackTextShare(context, invoiceWithItems, currentBusiness)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        fallbackTextShare(context, invoiceWithItems, currentBusiness)
    }
}

private fun fallbackTextShare(context: Context, invoiceWithItems: InvoiceWithItems, currentBusiness: BusinessProfileEntity?) {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items
    val businessName = currentBusiness?.businessName ?: "Our Store"
    val upiId = currentBusiness?.upiId ?: "merchant@upi"

    val itemsListText = items.joinToString("\n") { item ->
        "• ${item.itemName} (${item.quantity.toInt()} ${item.unit}) : ₹${item.totalAmount.roundToInt()}"
    }
    val upiPayLink = "upi://pay?pa=$upiId&pn=${Uri.encode(businessName)}&am=${inv.grandTotal}&cu=INR"

    val message = """
*INVOICE FROM $businessName*
----------------------------------
*Invoice No:* ${inv.invoiceNumber}
*Date:* ${formatDate(inv.date)}
*Customer:* ${inv.partyName}
*Status:* ${inv.status}
----------------------------------
*ITEMS:*
$itemsListText
----------------------------------
*Subtotal:* ₹${inv.subtotal.roundToInt()}
*GST Tax:* ₹${inv.totalTax.roundToInt()}
*Grand Total:* *₹${inv.grandTotal.roundToInt()}*
*Amount Paid:* ₹${inv.paidAmount.roundToInt()}
*Balance Due:* *₹${(inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0).roundToInt()}*
----------------------------------
*Instant UPI Payment Link:*
$upiPayLink

Thank you for doing business with us!
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage("com.whatsapp")
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "Share Invoice via"
        )
        context.startActivity(chooser)
    }
}

