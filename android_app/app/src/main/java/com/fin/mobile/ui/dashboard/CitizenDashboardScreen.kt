package com.fin.mobile.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.R
import com.fin.mobile.data.remote.dto.DashboardDataDto
import com.fin.mobile.data.remote.dto.DashboardMetricDto
import com.fin.mobile.data.remote.dto.TopOpportunityDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun CitizenDashboardScreen(
    viewModel: CitizenDashboardViewModel,
    user: UserDto?,
    onNavigateToCatalogue: () -> Unit,
    onNavigateToSchemeDetails: (schemeId: String) -> Unit,
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToApplications: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        // App Header
        DashboardHeader(
            user = user,
            onRefresh = { viewModel.refresh() },
            onNavigateToAssistant = onNavigateToAssistant,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToSettings = onNavigateToSettings,
            onSignOut = onSignOut
        )

        when (val state = uiState) {
            is CitizenDashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Citizen Dashboard...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            is CitizenDashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                        shape = FinTheme.shapes.md,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = FinTheme.colors.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Failed to load dashboard",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.loadDashboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is CitizenDashboardUiState.Success -> {
                DashboardContent(
                    data = state.data,
                    onNavigateToCatalogue = onNavigateToCatalogue,
                    onNavigateToSchemeDetails = onNavigateToSchemeDetails,
                    onNavigateToDocuments = onNavigateToDocuments,
                    onNavigateToApplications = onNavigateToApplications,
                    onNavigateToAssistant = onNavigateToAssistant
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    user: UserDto?,
    onRefresh: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    val userName = (user?.userMetadata?.get("full_name") as? String)
        ?: (user?.userMetadata?.get("fullName") as? String)
        ?: user?.email
        ?: "Citizen"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FinTheme.colors.surfaceWhite)
            .border(1.dp, FinTheme.colors.borderSubtle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Image(
                painter = painterResource(id = R.drawable.fin_logo),
                contentDescription = "FIN Crest",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "FIN Portal",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.primaryGreen
                )
                Text(
                    text = userName,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateToAssistant,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "FIN AI Assistant",
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onNavigateToNotifications,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = FinTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = FinTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = FinTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = FinTheme.colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardDataDto,
    onNavigateToCatalogue: () -> Unit,
    onNavigateToSchemeDetails: (schemeId: String) -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Greeting Banner
        item {
            HeroGreetingCard(data = data, onExploreSchemes = onNavigateToCatalogue)
        }

        // 4 Core Metrics
        item {
            MetricsGrid(
                metrics = data.metrics,
                onExploreSchemes = onNavigateToCatalogue,
                onNavigateToDocuments = onNavigateToDocuments,
                onNavigateToApplications = onNavigateToApplications
            )
        }

        // Top Opportunities Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Top Opportunities",
                        style = FinTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = "Statutory schemes matched to your profile",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToCatalogue,
                    shape = FinTheme.shapes.sm,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "View All",
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.primaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = FinTheme.colors.primaryGreen
                    )
                }
            }
        }

        // Opportunities List
        if (data.topOpportunities.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No top opportunities available yet",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToCatalogue,
                            colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                        ) {
                            Text("Discover All Schemes")
                        }
                    }
                }
            }
        } else {
            items(data.topOpportunities) { opportunity ->
                OpportunityCard(
                    opportunity = opportunity,
                    onClick = { onNavigateToSchemeDetails(opportunity.schemeId) }
                )
            }
        }

        // Bottom Discover Schemes CTA
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCatalogue() },
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.primaryGreenLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.primaryGreenBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FinTheme.colors.primaryGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = FinTheme.colors.textInverse,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Search & Filter Schemes",
                                style = FinTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.primaryGreenDark
                            )
                            Text(
                                text = "Browse national and state welfare catalog",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen
                    )
                }
            }
        }

        // Quick Action: FIN AI Assistant
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAssistant),
                shape = FinTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.primaryGreenBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFF0FDF4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = FinTheme.colors.primaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "FIN AI Assistant",
                                style = FinTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Text(
                                text = "Ask scheme queries, verify eligibility & documents",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen
                    )
                }
            }
        }

        // Quick Action: My Applications
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToApplications),
                shape = FinTheme.shapes.md,
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFF1D4ED8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Track My Applications",
                                style = FinTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Text(
                                text = "Check review status, actions & sanctions",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroGreetingCard(
    data: DashboardDataDto,
    onExploreSchemes: () -> Unit
) {
    val citizenName = data.user?.fullName ?: "Citizen"
    val profileCompleted = data.user?.profileCompleted ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Namaste, $citizenName",
                        style = FinTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = "Direct Benefit Transfer & Welfare Assistance",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile completion indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile Completion",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$profileCompleted%",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.primaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (profileCompleted.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = FinTheme.colors.primaryGreen,
                trackColor = FinTheme.colors.primaryGreenLight
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onExploreSchemes,
                modifier = Modifier.fillMaxWidth(),
                shape = FinTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Explore Scheme Catalogue",
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MetricsGrid(
    metrics: List<DashboardMetricDto>,
    onExploreSchemes: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val chunked = metrics.chunked(2)
        for (row in chunked) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (metric in row) {
                    MetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (metric.key) {
                                "schemes", "benefits" -> onExploreSchemes()
                                "documents" -> onNavigateToDocuments()
                                "applications" -> onNavigateToApplications()
                                else -> onNavigateToApplications()
                            }
                        }
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: DashboardMetricDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (icon, tintColor, bgColor) = when (metric.icon) {
        "scheme" -> Triple(Icons.Default.AccountBalance, FinTheme.colors.primaryGreen, FinTheme.colors.primaryGreenLight)
        "rupee" -> Triple(Icons.Default.CurrencyRupee, FinTheme.colors.accentBlue, FinTheme.colors.accentBlueLight)
        "document" -> Triple(
            Icons.Default.Description,
            if (metric.isWarning) FinTheme.colors.warning else FinTheme.colors.success,
            if (metric.isWarning) FinTheme.colors.warningLight else FinTheme.colors.successLight
        )
        else -> Triple(Icons.Default.Assignment, Color(0xFF6B46C1), Color(0xFFF3E8FF))
    }

    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        shape = FinTheme.shapes.md,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (metric.isWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = FinTheme.colors.warning,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = metric.value,
                style = FinTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            Text(
                text = metric.label,
                style = FinTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = FinTheme.colors.textSecondary
            )

            if (!metric.subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metric.subtitle,
                    style = FinTheme.typography.labelSmall,
                    color = FinTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpportunityCard(
    opportunity: TopOpportunityDto,
    onClick: () -> Unit
) {
    val status = opportunity.eligibilityStatus.uppercase()
    val isEligible = opportunity.isEligible == true || status == "PASS"

    val (badgeText, badgeColor, badgeBg) = when {
        isEligible -> Triple("Eligible", FinTheme.colors.success, FinTheme.colors.successLight)
        status == "REVIEW" -> Triple("Review Required", FinTheme.colors.warning, FinTheme.colors.warningLight)
        status == "FAIL" -> Triple("Criteria Not Met", FinTheme.colors.error, FinTheme.colors.errorLight)
        else -> Triple("Verification Needed", FinTheme.colors.accentBlue, FinTheme.colors.accentBlueLight)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        shape = FinTheme.shapes.md,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ministry & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = opportunity.ministry ?: "Government of India",
                    style = FinTheme.typography.labelSmall,
                    color = FinTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = FinTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scheme Name
            Text(
                text = opportunity.schemeName ?: opportunity.schemeId,
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            // Description
            if (!opportunity.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = opportunity.description,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Benefit & Deadline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val benefitText = opportunity.benefitDisplay
                    ?: (opportunity.benefitAmount?.let { "₹ ${it.toLong()}" } ?: "Financial Assistance")

                Column {
                    Text(
                        text = "Estimated Benefit",
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.textMuted
                    )
                    Text(
                        text = benefitText,
                        style = FinTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.primaryGreen
                    )
                }

                if (!opportunity.deadline.isNullOrBlank()) {
                    Text(
                        text = opportunity.deadline,
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.accentSaffron,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tags
            if (opportunity.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (tag in opportunity.tags.take(3)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FinTheme.colors.surfaceSubtle)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                style = FinTheme.typography.labelSmall,
                                color = FinTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
