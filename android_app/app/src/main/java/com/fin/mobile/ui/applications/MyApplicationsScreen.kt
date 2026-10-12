package com.fin.mobile.ui.applications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    viewModel: MyApplicationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (applicationId: String) -> Unit,
    onExploreSchemes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "My Applications",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FinTheme.colors.textPrimary
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.loadApplications() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = FinTheme.colors.textSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FinTheme.colors.surfaceWhite
            )
        )

        // Metrics Row
        ApplicationsMetricsRow(counts = uiState.counts)

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search by scheme or reference ID...",
                        style = FinTheme.typography.bodyMedium,
                        color = FinTheme.colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = FinTheme.colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = FinTheme.shapes.md,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FinTheme.colors.surfaceWhite,
                    unfocusedContainerColor = FinTheme.colors.surfaceWhite,
                    focusedIndicatorColor = FinTheme.colors.primaryGreen,
                    unfocusedIndicatorColor = FinTheme.colors.borderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status Filter Chips
        StatusFilterChipsRow(
            selectedStatus = uiState.selectedStatus,
            onSelect = { viewModel.onStatusFilterChanged(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Main List Content
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading official applications...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
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
                                text = "Unable to Load Applications",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "Unknown error",
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadApplications() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            uiState.filteredApplications.isEmpty() -> {
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
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = FinTheme.colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.applications.isEmpty()) "No Applications Submitted" else "No matching applications",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.applications.isEmpty()) {
                                    "You haven't submitted any scheme applications yet. Discover eligible schemes and apply."
                                } else {
                                    "No applications found for the selected filter or search keyword."
                                },
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onExploreSchemes,
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Explore Available Schemes")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredApplications, key = { it.id }) { app ->
                        ApplicationCard(
                            application = app,
                            onClick = { onNavigateToDetail(app.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationsMetricsRow(counts: ApplicationCounts) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricBadge(
            label = "Total",
            count = counts.total.toString(),
            color = FinTheme.colors.textPrimary,
            bgColor = FinTheme.colors.surfaceSubtle,
            modifier = Modifier.weight(1f)
        )
        MetricBadge(
            label = "In Review",
            count = counts.underReview.toString(),
            color = Color(0xFF1D4ED8),
            bgColor = Color(0xFFEFF6FF),
            modifier = Modifier.weight(1f)
        )
        MetricBadge(
            label = "Action Req.",
            count = counts.actionRequired.toString(),
            color = Color(0xFFB45309),
            bgColor = Color(0xFFFEF3C7),
            modifier = Modifier.weight(1f)
        )
        MetricBadge(
            label = "Approved",
            count = counts.approved.toString(),
            color = FinTheme.colors.primaryGreen,
            bgColor = FinTheme.colors.primaryGreenLight,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    count: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count,
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = FinTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusFilterChipsRow(
    selectedStatus: String,
    onSelect: (String) -> Unit
) {
    val filters = listOf(
        "all" to "All",
        "under_review" to "Under Review",
        "action_required" to "Action Required",
        "approved" to "Approved",
        "rejected" to "Rejected"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { (key, label) ->
            val isSelected = selectedStatus == key
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(key) },
                label = { Text(text = label, style = FinTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinTheme.colors.primaryGreen,
                    selectedLabelColor = Color.White,
                    containerColor = FinTheme.colors.surfaceWhite,
                    labelColor = FinTheme.colors.textSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) FinTheme.colors.primaryGreen else FinTheme.colors.borderSubtle
                )
            )
        }
    }
}

@Composable
private fun ApplicationCard(
    application: ApplicationDto,
    onClick: () -> Unit
) {
    val (statusLabel, statusColor, statusBgColor, statusIcon) = getStatusPresentation(application.normalizedStatus)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Reference ID + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.referenceId,
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.primaryGreen
                )

                // Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusLabel,
                        style = FinTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scheme Title
            Text(
                text = application.displayTitle,
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!application.category.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = application.category,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Benefit & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Benefit
                Text(
                    text = application.displayBenefit,
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FinTheme.colors.textPrimary
                )

                // Date
                Text(
                    text = application.displayDate.take(10),
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            }

            // Action Required or Remarks Warning Banner
            if (application.normalizedStatus == "action_required" && !application.remarks.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = application.remarks,
                            style = FinTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Tracking link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track Application",
                    style = FinTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinTheme.colors.primaryGreen
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private data class StatusPresentation(
    val label: String,
    val color: Color,
    val bgColor: Color,
    val icon: ImageVector
)

private fun getStatusPresentation(status: String): StatusPresentation {
    return when (status) {
        "approved", "sanctioned" -> StatusPresentation(
            label = "Approved",
            color = Color(0xFF15803D),
            bgColor = Color(0xFFDCFCE7),
            icon = Icons.Default.CheckCircle
        )
        "action_required" -> StatusPresentation(
            label = "Action Required",
            color = Color(0xFFB45309),
            bgColor = Color(0xFFFEF3C7),
            icon = Icons.Default.Warning
        )
        "rejected" -> StatusPresentation(
            label = "Rejected",
            color = Color(0xFFB91C1C),
            bgColor = Color(0xFFFEE2E2),
            icon = Icons.Default.Close
        )
        "draft" -> StatusPresentation(
            label = "Draft",
            color = Color(0xFF475569),
            bgColor = Color(0xFFF1F5F9),
            icon = Icons.Default.Assignment
        )
        else -> StatusPresentation(
            label = "Under Review",
            color = Color(0xFF1D4ED8),
            bgColor = Color(0xFFEFF6FF),
            icon = Icons.Default.HourglassEmpty
        )
    }
}
