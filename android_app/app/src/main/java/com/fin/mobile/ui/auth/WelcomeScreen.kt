package com.fin.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mobile.R
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Official Logo
            Image(
                painter = painterResource(id = R.drawable.fin_logo),
                contentDescription = "FIN Crest",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "FIN",
                style = FinTheme.typography.displaySmall,
                color = FinTheme.colors.primaryGreen,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Financial Inclusion Network",
                style = FinTheme.typography.titleMedium,
                color = FinTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Direct Benefits. Zero Intermediaries.",
                style = FinTheme.typography.headlineSmall,
                color = FinTheme.colors.accentSaffron,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "India's unified portal for instant scheme eligibility, direct benefit transfers, and official document verification.",
                style = FinTheme.typography.bodyMedium,
                color = FinTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Value pillars
            FeaturePillarCard(
                icon = Icons.Default.AccountBalance,
                title = "100+ Government Schemes",
                description = "Central & State welfare schemes verified from authoritative gazettes."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeaturePillarCard(
                icon = Icons.Default.AssignmentTurnedIn,
                title = "Automated Eligibility Check",
                description = "Deterministic criteria evaluation matching your verified profile."
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeaturePillarCard(
                icon = Icons.Default.Security,
                title = "Direct & Tamper-Proof",
                description = "End-to-end encrypted session and official government reviewer audit."
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinTheme.colors.primaryGreen,
                    contentColor = FinTheme.colors.textInverse
                ),
                shape = FinTheme.shapes.md
            ) {
                Text(
                    text = "Sign In",
                    style = FinTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToSignup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = FinTheme.shapes.md,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FinTheme.colors.primaryGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.primaryGreen)
            ) {
                Text(
                    text = "Create Citizen Account",
                    style = FinTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Official National e-Governance Infrastructure",
                style = FinTheme.typography.labelSmall,
                color = FinTheme.colors.textMuted
            )
        }
    }
}

@Composable
private fun FeaturePillarCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FinTheme.shapes.md)
            .background(FinTheme.colors.surfaceWhite)
            .border(1.dp, FinTheme.colors.borderSubtle, FinTheme.shapes.md)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FinTheme.colors.primaryGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FinTheme.colors.primaryGreen,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FinTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
