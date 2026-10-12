package com.fin.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.R
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: (role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val signupState by viewModel.signupState.collectAsStateWithLifecycle()
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
        Spacer(modifier = Modifier.height(12.dp))

        // Branding Crest
        Image(
            painter = painterResource(id = R.drawable.fin_logo),
            contentDescription = "FIN Crest",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Create Citizen Account",
            style = FinTheme.typography.headlineLarge,
            color = FinTheme.colors.primaryGreen,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Register to access direct DBT benefits and automated eligibility verification",
            style = FinTheme.typography.bodySmall,
            color = FinTheme.colors.textMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error Banner
        if (signupState.generalError != null) {
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
                    text = signupState.generalError ?: "",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.error,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Full Name Field
        OutlinedTextField(
            value = signupState.fullName,
            onValueChange = { viewModel.onSignupFullNameChanged(it) },
            label = { Text("Full Name *") },
            placeholder = { Text("Rajesh Kumar Sharma") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (signupState.fullNameError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            isError = signupState.fullNameError != null,
            supportingText = {
                if (signupState.fullNameError != null) {
                    Text(text = signupState.fullNameError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Email Field
        OutlinedTextField(
            value = signupState.email,
            onValueChange = { viewModel.onSignupEmailChanged(it) },
            label = { Text("Email Address *") },
            placeholder = { Text("rajesh.sharma@example.gov.in") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = if (signupState.emailError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            isError = signupState.emailError != null,
            supportingText = {
                if (signupState.emailError != null) {
                    Text(text = signupState.emailError ?: "", color = FinTheme.colors.error)
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

        Spacer(modifier = Modifier.height(10.dp))

        // Phone Field (Optional)
        OutlinedTextField(
            value = signupState.phone,
            onValueChange = { viewModel.onSignupPhoneChanged(it) },
            label = { Text("Mobile Number (Optional)") },
            placeholder = { Text("+91 9876543210") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = if (signupState.phoneError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            isError = signupState.phoneError != null,
            supportingText = {
                if (signupState.phoneError != null) {
                    Text(text = signupState.phoneError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Password Field
        OutlinedTextField(
            value = signupState.password,
            onValueChange = { viewModel.onSignupPasswordChanged(it) },
            label = { Text("Password * (min 6 characters)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (signupState.passwordError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.toggleSignupPasswordVisibility() }) {
                    Icon(
                        imageVector = if (signupState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (signupState.passwordVisible) "Hide password" else "Show password",
                        tint = FinTheme.colors.textMuted
                    )
                }
            },
            visualTransformation = if (signupState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = signupState.passwordError != null,
            supportingText = {
                if (signupState.passwordError != null) {
                    Text(text = signupState.passwordError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = signupState.confirmPassword,
            onValueChange = { viewModel.onSignupConfirmPasswordChanged(it) },
            label = { Text("Confirm Password *") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (signupState.confirmPasswordError != null) FinTheme.colors.error else FinTheme.colors.primaryGreen
                )
            },
            visualTransformation = if (signupState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = signupState.confirmPasswordError != null,
            supportingText = {
                if (signupState.confirmPasswordError != null) {
                    Text(text = signupState.confirmPasswordError ?: "", color = FinTheme.colors.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.submitSignup(onSignupSuccess)
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

        // Register Button
        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.submitSignup(onSignupSuccess)
            },
            enabled = !signupState.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = FinTheme.shapes.md,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinTheme.colors.primaryGreen,
                contentColor = FinTheme.colors.textInverse
            )
        ) {
            if (signupState.isSubmitting) {
                CircularProgressIndicator(
                    color = FinTheme.colors.textInverse,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = "Create Account",
                    style = FinTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already registered?",
                style = FinTheme.typography.bodyMedium,
                color = FinTheme.colors.textSecondary
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Sign In",
                    style = FinTheme.typography.labelLarge,
                    color = FinTheme.colors.primaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
