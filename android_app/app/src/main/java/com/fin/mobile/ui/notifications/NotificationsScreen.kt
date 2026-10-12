package com.fin.mobile.ui.notifications

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.NotificationDto
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToApplicationDetail: (applicationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.dismissSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            NotificationsHeader(
                unreadCount = uiState.unreadCount,
                isPerformingAction = uiState.isPerformingAction,
                onNavigateBack = onNavigateBack,
                onRefresh = { viewModel.loadNotifications(isRefresh = true) },
                onMarkAllRead = { viewModel.markAllAsRead() }
            )

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FinTheme.colors.surfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == NotificationFilter.ALL,
                    onClick = { viewModel.setFilter(NotificationFilter.ALL) },
                    label = {
                        Text(
                            text = "All (${uiState.notifications.size})",
                            style = FinTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinTheme.colors.primaryGreen,
                        selectedLabelColor = FinTheme.colors.textInverse
                    )
                )

                FilterChip(
                    selected = uiState.filter == NotificationFilter.UNREAD,
                    onClick = { viewModel.setFilter(NotificationFilter.UNREAD) },
                    label = {
                        Text(
                            text = "Unread (${uiState.unreadCount})",
                            style = FinTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinTheme.colors.primaryGreen,
                        selectedLabelColor = FinTheme.colors.textInverse
                    )
                )
            }

            // Main Content Area
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = FinTheme.colors.primaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading notifications...",
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                        }
                    }
                }

                uiState.displayedNotifications.isEmpty() -> {
                    EmptyNotificationsView(
                        filter = uiState.filter,
                        onRefresh = { viewModel.loadNotifications(isRefresh = true) }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.displayedNotifications,
                            key = { it.id }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onCardClick = {
                                    if (!notification.isRead) {
                                        viewModel.markAsRead(notification.id)
                                    }
                                    val targetAppId = notification.applicationId
                                    if (!targetAppId.isNullOrBlank()) {
                                        onNavigateToApplicationDetail(targetAppId)
                                    }
                                },
                                onMarkAsRead = {
                                    viewModel.markAsRead(notification.id)
                                },
                                onNavigateToApplication = { appId ->
                                    if (!notification.isRead) {
                                        viewModel.markAsRead(notification.id)
                                    }
                                    onNavigateToApplicationDetail(appId)
                                }
                            )
                        }
                    }
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
}

@Composable
private fun NotificationsHeader(
    unreadCount: Int,
    isPerformingAction: Boolean,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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

                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            style = FinTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )

                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = FinTheme.colors.primaryGreen,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    style = FinTheme.typography.labelSmall,
                                    color = FinTheme.colors.textInverse,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Real-time updates on your applications & documents",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh notifications",
                        tint = FinTheme.colors.textSecondary
                    )
                }

                if (unreadCount > 0) {
                    IconButton(
                        onClick = onMarkAllRead,
                        enabled = !isPerformingAction,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isPerformingAction) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FinTheme.colors.primaryGreen
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all notifications as read",
                                tint = FinTheme.colors.primaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationDto,
    onCardClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onNavigateToApplication: (String) -> Unit
) {
    val isRead = notification.isRead
    val notifType = notification.type?.lowercase() ?: "general"

    val (icon, iconTint, iconBg) = when {
        notifType.contains("decision") || notifType.contains("sanction") ->
            Triple(Icons.Default.VerifiedUser, Color(0xFF15803D), Color(0xFFDCFCE7))
        notifType.contains("application") || notifType.contains("status") ->
            Triple(Icons.AutoMirrored.Filled.Assignment, Color(0xFF1D4ED8), Color(0xFFDBEAFE))
        notifType.contains("document") || notifType.contains("ocr") ->
            Triple(Icons.Default.Info, Color(0xFFB45309), Color(0xFFFEF3C7))
        else ->
            Triple(Icons.Default.Notifications, FinTheme.colors.primaryGreen, FinTheme.colors.primaryGreenLight)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .semantics { contentDescription = "Notification: ${notification.title ?: "Update"}" },
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) FinTheme.colors.surfaceWhite else Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isRead) FinTheme.colors.borderSubtle else FinTheme.colors.primaryGreenBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title ?: "Application Update",
                        style = FinTheme.typography.titleSmall,
                        fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Bold,
                        color = FinTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(FinTheme.colors.primaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = notification.displayCreatedAt,
                            style = FinTheme.typography.labelSmall,
                            color = FinTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (!notification.message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                // Action row if application link is present
                val appId = notification.applicationId
                if (!appId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToApplication(appId) },
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "View Application",
                                style = FinTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = FinTheme.colors.primaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (!isRead) {
                            IconButton(
                                onClick = onMarkAsRead,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Mark as read",
                                    tint = FinTheme.colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else if (!isRead) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(
                            onClick = onMarkAsRead,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark as read",
                                tint = FinTheme.colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotificationsView(
    filter: NotificationFilter,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = FinTheme.colors.textMuted,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (filter == NotificationFilter.UNREAD) "No Unread Notifications" else "No Notifications Yet",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (filter == NotificationFilter.UNREAD)
                    "You're all caught up! Switch to 'All' to see past updates."
                else
                    "Updates regarding your scheme applications, OCR verifications, and reviewer decisions will appear here.",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Check Again", style = FinTheme.typography.labelMedium)
            }
        }
    }
}
