package com.fin.mobile.ui.reviewer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun ReviewerDashboardScreen(
    viewModel: ReviewerDashboardViewModel,
    user: UserDto?,
    onNavigateToApplicationDetails: (applicationId: String) -> Unit,
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
        // Reviewer Header
        ReviewerHeader(
            user = user,
            onRefresh = { viewModel.loadData(isRefresh = true) },
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToSettings = onNavigateToSettings,
            onSignOut = onSignOut
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Section
            item {
                ReviewerMetricsGrid(stats = uiState.stats)
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search by applicant, application ID, scheme...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FinTheme.colors.textSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinTheme.colors.primaryGreen,
                        unfocusedBorderColor = FinTheme.colors.borderSubtle
                    ),
                    singleLine = true
                )
            }

            // Status Filter Tabs
            item {
                StatusFilterTabs(
                    selectedStatus = uiState.selectedStatus,
                    onSelect = { viewModel.setStatusFilter(it) }
                )
            }

            // Queue Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Review Queue (${uiState.filteredApplications.size})",
                        style = FinTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = FinTheme.colors.primaryGreen
                        )
                    }
                }
            }

            // Error Message
            if (uiState.errorMessage != null && !uiState.isLoading) {
                item {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = FinTheme.shapes.sm,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = FinTheme.colors.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = FinTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // Applications Queue List
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                    }
                }
            } else if (uiState.filteredApplications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FinTheme.shapes.md,
                        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FinTheme.colors.primaryGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Queue is clear",
                                style = FinTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Text(
                                text = "No applications require review in this status tab.",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted
                            )
                        }
                    }
                }
            } else {
                items(uiState.filteredApplications, key = { it.id }) { app ->
                    ReviewQueueCard(
                        application = app,
                        onClick = { onNavigateToApplicationDetails(app.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewerHeader(
    user: UserDto?,
    onRefresh: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = FinTheme.colors.surfaceWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Reviewer Shield",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FIN Review Portal",
                            style = FinTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OFFICER",
                                style = FinTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = user?.displayName ?: "Verification Officer",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun ReviewerMetricsGrid(stats: com.fin.mobile.data.remote.dto.ReviewerDashboardStatsDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Pending Reviews",
                count = stats.pendingReviews,
                color = Color(0xFFD97706),
                bgColor = Color(0xFFFEF3C7),
                icon = Icons.Default.PendingActions,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Reviewed Today",
                count = stats.reviewedToday,
                color = Color(0xFF2563EB),
                bgColor = Color(0xFFEFF6FF),
                icon = Icons.Default.HourglassTop,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Action Required",
                count = stats.actionRequired,
                color = Color(0xFFEA580C),
                bgColor = Color(0xFFFFF7ED),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Approved",
                count = stats.approved,
                color = FinTheme.colors.primaryGreen,
                bgColor = Color(0xFFF0FDF4),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = FinTheme.shapes.sm,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = count.toString(),
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                Text(
                    text = title,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusFilterTabs(
    selectedStatus: String,
    onSelect: (String) -> Unit
) {
    val tabs = listOf(
        "all" to "All",
        "under_review" to "Pending",
        "action_required" to "Action Req.",
        "approved" to "Approved",
        "rejected" to "Rejected"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (key, label) ->
            val isSelected = selectedStatus == key
            Surface(
                color = if (isSelected) FinTheme.colors.primaryGreen else FinTheme.colors.surfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) FinTheme.colors.primaryGreen else FinTheme.colors.borderSubtle
                ),
                modifier = Modifier.clickable { onSelect(key) }
            ) {
                Text(
                    text = label,
                    style = FinTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) FinTheme.colors.textInverse else FinTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewQueueCard(
    application: ApplicationDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.applicationNumber ?: application.referenceId,
                    style = FinTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.primaryGreenDark
                )
                ReviewerStatusBadge(status = application.status)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = application.displayTitle,
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Applicant: ${application.applicantName ?: "Registered Citizen"}",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textSecondary
                    )
                    val dateStr = application.appliedAt ?: application.displayDate
                    if (dateStr.isNotBlank()) {
                        Text(
                            text = "Submitted: ${dateStr.take(10)}",
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewerStatusBadge(status: String?) {
    val clean = (status ?: "under_review").lowercase()
    val (label, bg, text) = when (clean) {
        "approved", "sanctioned" -> Triple("Approved", Color(0xFFF0FDF4), Color(0xFF166534))
        "rejected" -> Triple("Rejected", Color(0xFFFEF2F2), Color(0xFF991B1B))
        "action_required" -> Triple("Action Required", Color(0xFFFFF7ED), Color(0xFF9A3412))
        else -> Triple("Under Review", Color(0xFFFEF3C7), Color(0xFF92400E))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            style = FinTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = text,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
