package com.fin.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.R
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onLoginSuccess: (role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Branding Crest
        Image(
            painter = painterResource(id = R.drawable.fin_logo),
            contentDescription = "FIN Crest",
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sign in to FIN",
            style = FinTheme.typography.headlineLarge,
            color = FinTheme.colors.primaryGreen,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Unified Direct Benefit Transfer & Scheme Evaluation",
            style = FinTheme.typography.bodySmall,
            color = FinTheme.colors.textMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mode Switcher: Citizen vs Government Reviewer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FinTheme.shapes.pill)
                .background(FinTheme.colors.surfaceSubtle)
                .border(1.dp, FinTheme.colors.borderSubtle, FinTheme.shapes.pill)
                .padding(4.dp)
        ) {
            val isCitizen = loginState.mode == "citizen"
            val isReviewer = loginState.mode == "reviewer"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(FinTheme.shapes.pill)
                    .background(if (isCitizen) FinTheme.colors.primaryGreen else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { viewModel.onLoginModeChanged("citizen") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Citizen",
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCitizen) FinTheme.colors.textInverse else FinTheme.colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(FinTheme.shapes.pill)
                    .background(if (isReviewer) FinTheme.colors.accentSaffron else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { viewModel.onLoginModeChanged("reviewer") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reviewer",
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isReviewer) FinTheme.colors.textInverse else FinTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error Banner
        if (loginState.generalError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FinTheme.shapes.md)
                    .background(FinTheme.colors.errorLight)
                    .border(1.dp, FinTheme.colors.errorBorder, FinTheme.shapes.md)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = FinTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = loginState.generalError ?: "",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.error,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Email Field
        OutlinedTextField(
            value = loginState.email,
            onValueChange = { viewModel.onLoginEmailChanged(it) },
            label = { Text("Email Address") },
            placeholder = { Text("citizen@example.gov.in") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = if (loginState.emailError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            isError = loginState.emailError != null,
            supportingText = {
                if (loginState.emailError != null) {
                    Text(text = loginState.emailError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = FinTheme.shapes.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FinTheme.colors.primaryGreen,
                focusedLabelColor = FinTheme.colors.primaryGreen,
                unfocusedBorderColor = FinTheme.colors.borderMedium
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Field
        OutlinedTextField(
            value = loginState.password,
            onValueChange = { viewModel.onLoginPasswordChanged(it) },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (loginState.passwordError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.toggleLoginPasswordVisibility() }) {
                    Icon(
                        imageVector = if (loginState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (loginState.passwordVisible) "Hide password" else "Show password",
                        tint = FinTheme.colors.textMuted
                    )
                }
            },
            visualTransformation = if (loginState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = loginState.passwordError != null,
            supportingText = {
                if (loginState.passwordError != null) {
                    Text(text = loginState.passwordError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.submitLogin(onLoginSuccess)
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = FinTheme.shapes.md,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FinTheme.colors.primaryGreen,
                focusedLabelColor = FinTheme.colors.primaryGreen,
                unfocusedBorderColor = FinTheme.colors.borderMedium
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In Button
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.submitLogin(onLoginSuccess)
            },
            enabled = !loginState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = FinTheme.shapes.md,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (loginState.mode == "reviewer") FinTheme.colors.accentSaffron else FinTheme.colors.primaryGreen,
                contentColor = FinTheme.colors.textInverse
            )
        ) {
            if (loginState.isSubmitting) {
                CircularProgressIndicator(
                    color = FinTheme.colors.textInverse,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = if (loginState.mode == "reviewer") "Sign In as Reviewer" else "Sign In as Citizen",
                    style = FinTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Registration link (only available for citizen mode)
        if (loginState.mode == "citizen") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account?",
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary
                )
                TextButton(onClick = onNavigateToSignup) {
                    Text(
                        text = "Register Now",
                        style = FinTheme.typography.labelLarge,
                        color = FinTheme.colors.primaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Text(
                text = "Official Government Reviewer credentials required.",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
