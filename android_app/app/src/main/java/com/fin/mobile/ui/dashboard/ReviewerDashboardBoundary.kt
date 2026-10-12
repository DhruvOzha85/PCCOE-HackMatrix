package com.fin.mobile.ui.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fin.mobile.R
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun ReviewerDashboardBoundary(
    user: UserDto?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = (user?.userMetadata?.get("full_name") as? String)
        ?: (user?.userMetadata?.get("fullName") as? String)
        ?: user?.email
        ?: "Officer"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FinTheme.shapes.md)
                    .background(FinTheme.colors.surfaceWhite)
                    .border(1.dp, FinTheme.colors.borderSubtle, FinTheme.shapes.md)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fin_logo),
                    contentDescription = "FIN Crest",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Government Reviewer Portal",
                        style = FinTheme.typography.titleMedium,
                        color = FinTheme.colors.accentSaffron,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Official Scheme Audit & Verification Desk",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Reviewer Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(FinTheme.colors.accentSaffronLight)
                    .border(2.dp, FinTheme.colors.accentSaffronBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Reviewer Verified",
                    tint = FinTheme.colors.accentSaffron,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Reviewer Authority Verified",
                style = FinTheme.typography.headlineMedium,
                color = FinTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Officer: $displayName",
                style = FinTheme.typography.bodyLarge,
                color = FinTheme.colors.accentSaffron,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Credential Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                shape = FinTheme.shapes.md,
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Reviewer Credentials & Authorization",
                        style = FinTheme.typography.labelLarge,
                        color = FinTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SessionDetailRow(label = "Verified Role", value = "reviewer (Official)")
                    SessionDetailRow(label = "Officer Email", value = user?.email ?: "N/A")
                    SessionDetailRow(label = "Officer ID", value = user?.id ?: "N/A")
                    SessionDetailRow(label = "Access Tier", value = "Tier 1 — Application Queue & Verification")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FinTheme.shapes.md)
                    .background(FinTheme.colors.accentSaffronLight)
                    .border(1.dp, FinTheme.colors.accentSaffronBorder, FinTheme.shapes.md)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Reviewer Navigation Boundary: Strict server-side authorization check validated. Reviewer queue, conflict resolution, and decision portal will be rendered in subsequent phases.",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.warning,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Sign Out Button
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = FinTheme.shapes.md,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinTheme.colors.error,
                contentColor = FinTheme.colors.textInverse
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                style = FinTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
