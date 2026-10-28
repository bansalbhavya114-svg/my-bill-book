package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.SupabaseClient
import com.example.ui.components.AnimatedRadarBeacon
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodel.BillBookViewModel

@Composable
fun AuthScreen(
    viewModel: BillBookViewModel,
    onAuthSuccess: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Create Account
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp)
        ) {
            // Modern Spatial Brand Hero Header
            item {
                BrandLogoAvatar(
                    sizeDp = 76,
                    showGlow = true,
                    shape = RoundedCornerShape(22.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "BillBook",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Clean, human-crafted GST billing & accounting",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Supabase Cloud Verified Indicator
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TealSuccessContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealSuccess.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedRadarBeacon(color = TealSuccess, sizeDp = 7)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Supabase Cloud: Connected (${SupabaseClient.PROJECT_ID})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealSuccessText
                        )
                    }
                }
            }

            // Sleek Tab Switcher (No OTP, pure direct access)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Sign In Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 0 }
                                .testTag("tab_login")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) PrimaryBlue else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sign In",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) TextPrimary else TextSecondary
                                )
                            }
                        }

                        // Create Account Tab
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("tab_signup")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) PrimaryBlue else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Create Account",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Authentication Form
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (selectedTab == 0) {
                            DirectLoginForm(
                                onLogin = { identifier, password ->
                                    if (identifier.isNotBlank()) {
                                        viewModel.login(identifier, password)
                                        onAuthSuccess()
                                    }
                                },
                                onQuickDemo = {
                                    viewModel.login("bansalbhavya114@gmail.com", "pass123")
                                    onAuthSuccess()
                                }
                            )
                        } else {
                            DirectSignUpForm(
                                onSignUp = { name, bName, phone, email, state, gstin, bType, turnover ->
                                    viewModel.signUp(name, bName, phone, email, state, gstin, bType, turnover)
                                    onAuthSuccess()
                                }
                            )
                        }
                    }
                }
            }

            // Quick Data Clean Slate Action (Optional)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Fresh Setup / Reset",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Start with a brand new clean database (0 bills, 0 money)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        TextButton(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = RoseError)
                        ) {
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reset Data Confirmation Dialog
    if (showResetConfirmDialog) {
        var newShopName by remember { mutableStateOf("My Fresh Store") }
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Fresh Ledger?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will wipe all existing data and create a clean business profile starting at ₹0.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newShopName,
                        onValueChange = { newShopName = it },
                        label = { Text("Initial Store Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData(newShopName, "Delhi")
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Confirm Fresh Slate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Direct Login Form - ZERO OTP friction.
 * Simple, human-crafted login with Email or Phone and Password.
 */
@Composable
fun DirectLoginForm(
    onLogin: (String, String) -> Unit,
    onQuickDemo: () -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Welcome back",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Text(
                text = "Sign in directly with your email or phone number. No OTP needed.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email or Phone Number") },
            placeholder = { Text("e.g. user@gmail.com or 9876543210") },
            leadingIcon = {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryBlue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_identifier_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (or Passcode)") },
            placeholder = { Text("Enter your account password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextMuted
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onLogin(identifier, password) },
            enabled = identifier.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_login_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        // Quick Instant Access chip for effortless testing
        OutlinedButton(
            onClick = onQuickDemo,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = TealSuccess, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Quick 1-Tap Access (Demo / Verified User)", fontSize = 13.sp, color = TextPrimary)
        }
    }
}

/**
 * Direct Sign Up Form - ZERO OTP friction.
 * Direct registration with business details, saved immediately to Supabase and Room.
 */
@Composable
fun DirectSignUpForm(
    onSignUp: (String, String, String, String, String, String, String, String) -> Unit
) {
    var ownerName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Delhi") }
    var gstin by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("Retailer (Shop / Store)") }
    var turnover by remember { mutableStateOf("Below ₹20 Lakhs (GST Exempt)") }

    val businessTypes = listOf(
        "Retailer (Shop / Store)",
        "Wholesale & Distribution",
        "Manufacturing",
        "Services & Consulting",
        "Trader",
        "Freelancer / Agency",
        "E-Commerce",
        "Other"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Create Business Account",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Text(
                text = "Instant registration without OTP. Start fresh at ₹0 balance.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Store / Company Name *") },
            placeholder = { Text("e.g. My Business Store") },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = PrimaryBlue) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_business_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Owner / Contact Person *") },
            placeholder = { Text("e.g. Rahul Sharma") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_owner_name_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (Optional/Gmail)") },
            placeholder = { Text("e.g. mycompany@gmail.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("9876543210") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .weight(1f)
                    .testTag("signup_phone_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("State") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("signup_state_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = gstin,
            onValueChange = { gstin = it.uppercase() },
            label = { Text("GSTIN (Optional)") },
            placeholder = { Text("Leave blank if unregistered") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_gstin_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Business Type Selector
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Business Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            var expandedType by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = businessType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedType = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_business_type_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    businessTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                businessType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                onSignUp(ownerName, businessName, phone, email, state, gstin, businessType, turnover)
            },
            enabled = businessName.isNotBlank() && (ownerName.isNotBlank() || email.isNotBlank() || phone.isNotBlank()),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_signup_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Account & Start", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
