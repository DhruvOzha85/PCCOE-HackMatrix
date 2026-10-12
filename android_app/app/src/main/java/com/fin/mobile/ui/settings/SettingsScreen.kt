package com.fin.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.dismissMessages()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            SettingsHeader(
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.loadData() }
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section
                ProfileOverviewCard(
                    uiState = uiState,
                    onEditClick = { viewModel.openEditProfileDialog() }
                )

                // Security Section
                AccountSecurityCard(
                    onChangePasswordClick = { viewModel.openPasswordDialog() }
                )

                // Preferences & Language Section
                PreferencesCard(
                    selectedLanguage = uiState.language,
                    onLanguageSelect = { viewModel.setLanguage(it) }
                )

                // System & Reliability Compliance Card
                SecurityAssuranceCard()

                // Sign Out Action Button
                SignOutCard(
                    isLoggingOut = uiState.isLoggingOut,
                    onSignOutClick = { showSignOutConfirmDialog = true }
                )

                // App Version info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FIN Android v1.7.0 • National Welfare Engine",
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.textMuted
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Change Password Dialog
    if (uiState.isPasswordDialogOpen) {
        ChangePasswordDialog(
            isChanging = uiState.isChangingPassword,
            errorMessage = uiState.passwordError,
            onDismiss = { viewModel.closePasswordDialog() },
            onSubmit = { cur, new, conf ->
                viewModel.changePassword(cur, new, conf)
            }
        )
    }

    // Edit Profile Dialog
    if (uiState.isEditProfileDialogOpen) {
        EditProfileDialog(
            currentProfile = uiState.profile,
            isUpdating = uiState.isUpdatingProfile,
            onDismiss = { viewModel.closeEditProfileDialog() },
            onSubmit = { phone, state, district, occupation, income ->
                viewModel.updateProfile(phone, state, district, occupation, income)
            }
        )
    }

    // Logout Confirmation Dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out? Your local encrypted session will be cleared.",
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirmDialog = false
                        viewModel.logout(onSignOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancel", color = FinTheme.colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        color = FinTheme.colors.surfaceWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = FinTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Settings & Profile",
                        style = FinTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = "Account, Security & App Preferences",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh profile",
                    tint = FinTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ProfileOverviewCard(
    uiState: SettingsUiState,
    onEditClick: () -> Unit
) {
    val user = uiState.user
    val profile = uiState.profile
    val displayName = profile?.fullName ?: user?.displayName ?: "Citizen"
    val email = user?.email ?: "Not specified"
    val role = user?.role?.uppercase() ?: "USER"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(FinTheme.colors.primaryGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase(),
                            style = FinTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.primaryGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayName,
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (role == "REVIEWER") Color(0xFFFEF3C7) else FinTheme.colors.primaryGreenLight,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = role,
                                    style = FinTheme.typography.labelSmall,
                                    color = if (role == "REVIEWER") Color(0xFFB45309) else FinTheme.colors.primaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = email,
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile details",
                        tint = FinTheme.colors.primaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FinTheme.colors.borderSubtle)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Verified Profile Attributes Grid
            ProfileInfoRow(label = "Phone", value = profile?.phone ?: "Not registered")
            ProfileInfoRow(label = "State", value = profile?.state ?: "Not specified")
            ProfileInfoRow(label = "District", value = profile?.district ?: "Not specified")
            ProfileInfoRow(label = "Occupation", value = profile?.occupation ?: "Not specified")
            ProfileInfoRow(
                label = "Annual Income",
                value = profile?.annualIncome?.let { "₹${String.format("%,.0f", it)}" } ?: "Not specified"
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FinTheme.typography.bodySmall,
            color = FinTheme.colors.textMuted
        )
        Text(
            text = value,
            style = FinTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = FinTheme.colors.textPrimary
        )
    }
}

@Composable
private fun AccountSecurityCard(
    onChangePasswordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security & Credentials",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Keep your FIN portal account secured with a strong password.",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onChangePasswordClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FinTheme.colors.primaryGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change Password",
                    style = FinTheme.typography.labelMedium,
                    color = FinTheme.colors.primaryGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesCard(
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English (Default)",
        "hi" to "हिंदी (Hindi)",
        "mr" to "मराठी (Marathi)",
        "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)"
    )
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = languages.firstOrNull { it.first == selectedLanguage }?.second ?: "English (Default)"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Language & Accessibility",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your preferred language for the FIN Assistant and application interfaces.",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Interface Language") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinTheme.colors.primaryGreen,
                        unfocusedBorderColor = FinTheme.colors.borderSubtle
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label, style = FinTheme.typography.bodyMedium) },
                            onClick = {
                                onLanguageSelect(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityAssuranceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security & Privacy Assurances",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SecurityBullet(text = "AES-256 hardware-backed session encryption on Android Keystore")
            SecurityBullet(text = "Strict zero-logging of Aadhaar, passwords, or OCR document text")
            SecurityBullet(text = "Time-limited signed URLs for citizen documents prevent data leakage")
            SecurityBullet(text = "Role-based verification enforced on all reviewer operations")
        }
    }
}

@Composable
private fun SecurityBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FinTheme.colors.primaryGreen,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = FinTheme.typography.bodySmall,
            color = FinTheme.colors.textSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SignOutCard(
    isLoggingOut: Boolean,
    onSignOutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Button(
                onClick = onSignOutClick,
                enabled = !isLoggingOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = FinTheme.colors.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FinTheme.colors.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out from Account",
                        fontWeight = FontWeight.Bold,
                        style = FinTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    isChanging: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isChanging) onDismiss() },
        title = {
            Text(
                text = "Change Account Password",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = FinTheme.colors.errorLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showCurrentPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (min 6 chars)") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showNewPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(currentPassword, newPassword, confirmPassword) },
                enabled = !isChanging && currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FinTheme.colors.textInverse
                    )
                } else {
                    Text("Update Password")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isChanging
            ) {
                Text("Cancel", color = FinTheme.colors.textSecondary)
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    currentProfile: com.fin.mobile.data.remote.dto.UserProfileDto?,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (phone: String?, state: String?, district: String?, occupation: String?, annualIncome: Double?) -> Unit
) {
    var phone by remember { mutableStateOf(currentProfile?.phone ?: "") }
    var state by remember { mutableStateOf(currentProfile?.state ?: "") }
    var district by remember { mutableStateOf(currentProfile?.district ?: "") }
    var occupation by remember { mutableStateOf(currentProfile?.occupation ?: "") }
    var incomeStr by remember {
        mutableStateOf(currentProfile?.annualIncome?.let { String.format("%.0f", it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = {
            Text(
                text = "Edit Profile Details",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State / UT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Occupation (e.g. student, farmer, msme)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = incomeStr,
                    onValueChange = { incomeStr = it },
                    label = { Text("Annual Income (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val incomeVal = incomeStr.toDoubleOrNull()
                    onSubmit(
                        phone.ifBlank { null },
                        state.ifBlank { null },
                        district.ifBlank { null },
                        occupation.ifBlank { null },
                        incomeVal
                    )
                },
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FinTheme.colors.textInverse
                    )
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancel", color = FinTheme.colors.textSecondary)
            }
        }
    )
}
