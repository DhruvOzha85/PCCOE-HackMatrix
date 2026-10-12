package com.fin.mobile.ui.applications

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.SubmittedDocumentDto
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailScreen(
    viewModel: ApplicationDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = uiState.application?.referenceId ?: "Application Details",
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
                if (uiState.receiptText != null) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FIN Official Acknowledgement - ${uiState.application?.referenceId}")
                                    putExtra(Intent.EXTRA_TEXT, uiState.receiptText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Acknowledgement Receipt"))
                            } catch (_: Exception) {
                                // Prevent crash if no application handles the share action
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Receipt",
                            tint = FinTheme.colors.primaryGreen
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FinTheme.colors.surfaceWhite
            )
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
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
                                text = "Application Not Found",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "Could not retrieve application record.",
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadApplication() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            uiState.application != null -> {
                val app = uiState.application!!
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { StatusHeroCard(app = app) }
                    item { TimelineStepperCard(app = app) }
                    item { SchemeInfoCard(app = app) }
                    if (!app.remarks.isNullOrBlank() || !app.rejectionReason.isNullOrBlank()) {
                        item { RemarksCard(app = app) }
                    }
                    item { LinkedDocumentsCard(documents = app.submittedDocuments ?: emptyList()) }
                    item {
                        OutlinedButton(
                            onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FIN Official Acknowledgement - ${app.referenceId}")
                                    putExtra(Intent.EXTRA_TEXT, uiState.receiptText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Download / Share Official Acknowledgement"))
                            } catch (_: Exception) {
                                // Prevent crash if no application handles the share action
                            }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FinTheme.shapes.md
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Official Acknowledgement")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeroCard(app: ApplicationDto) {
    val status = app.normalizedStatus
    val isApproved = status in setOf("approved", "sanctioned")
    val isActionReq = status == "action_required"
    val isRejected = status == "rejected"

    val config = when {
        isApproved -> HeroTintConfig("Approved & Sanctioned", Color(0xFFDCFCE7), Color(0xFF86EFAC), Color(0xFF15803D))
        isActionReq -> HeroTintConfig("Action Required by Applicant", Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFFB45309))
        isRejected -> HeroTintConfig("Application Rejected", Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFFB91C1C))
        else -> HeroTintConfig("Under Review", Color(0xFFEFF6FF), Color(0xFF93C5FD), Color(0xFF1D4ED8))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = config.bgTint),
        border = androidx.compose.foundation.BorderStroke(1.dp, config.borderTint)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.badgeText,
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = config.textTint
                )
                Text(
                    text = app.referenceId,
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = config.textTint
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    isApproved -> "Your scheme application has been approved by the competent authority."
                    isActionReq -> "The verification officer has requested clarification or updated documents."
                    isRejected -> "Your application does not satisfy statutory scheme criteria."
                    else -> "Your application is currently being scrutinized by the statutory nodal authority."
                },
                style = FinTheme.typography.bodyMedium,
                color = config.textTint.copy(alpha = 0.9f)
            )
        }
    }
}

private data class HeroTintConfig(
    val badgeText: String,
    val bgTint: Color,
    val borderTint: Color,
    val textTint: Color
)

@Composable
private fun TimelineStepperCard(app: ApplicationDto) {
    val status = app.normalizedStatus
    val isApproved = status in setOf("approved", "sanctioned")
    val isRejected = status == "rejected"
    val isActionReq = status == "action_required"
    val isDraft = status == "draft"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tracking Timeline",
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Submission
            TimelineStepItem(
                title = "Application Submitted",
                subtitle = app.displayDate.take(10).ifBlank { "Recorded on Portal" },
                isCompleted = !isDraft,
                isCurrent = isDraft,
                isError = false,
                isLast = false
            )

            // Step 2: Verification
            TimelineStepItem(
                title = "Document Verification",
                subtitle = if (isActionReq) "Action Required by Applicant"
                    else if (isApproved || isRejected) "Scrutiny Completed"
                    else "Under Review by Verification Officer",
                isCompleted = isApproved || isRejected,
                isCurrent = !isApproved && !isRejected && !isDraft && !isActionReq,
                isError = isActionReq,
                isLast = false
            )

            // Step 3: Approval
            TimelineStepItem(
                title = "Sanction & Approval",
                subtitle = if (isApproved) "Sanctioned by Nodal Authority"
                    else if (isRejected) "Rejected by Authority"
                    else "Pending Officer Decision",
                isCompleted = isApproved,
                isCurrent = false,
                isError = isRejected,
                isLast = false
            )

            // Step 4: Disbursement
            TimelineStepItem(
                title = "Benefit Disbursement",
                subtitle = if (isApproved) "Queued for Direct Benefit Transfer (DBT)"
                    else if (isRejected) "Not Applicable"
                    else "Subsequent to Approval",
                isCompleted = false,
                isCurrent = isApproved,
                isError = false,
                isLast = true
            )
        }
    }
}

@Composable
private fun TimelineStepItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isError: Boolean,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> Color(0xFFDC2626)
                            isCompleted -> FinTheme.colors.primaryGreen
                            isCurrent -> Color(0xFF2563EB)
                            else -> FinTheme.colors.borderSubtle
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else if (isError) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else if (isCurrent) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(if (isCompleted) FinTheme.colors.primaryGreen else FinTheme.colors.borderSubtle)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                style = FinTheme.typography.bodyMedium,
                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                color = if (isError) Color(0xFFDC2626) else FinTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted
            )
        }
    }
}

@Composable
private fun SchemeInfoCard(app: ApplicationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scheme & Implementing Details",
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Scheme Title", value = app.displayTitle)
            if (!app.category.isNullOrBlank()) {
                DetailRow(label = "Classification", value = app.category)
            }
            DetailRow(label = "Benefit Entitlement", value = app.displayBenefit)
            DetailRow(label = "Applicant Name", value = app.applicantName ?: "Registered Citizen")
            DetailRow(label = "Application ID", value = app.referenceId)
            DetailRow(label = "Tracking ID (UUID)", value = app.id)
        }
    }
}

@Composable
private fun RemarksCard(app: ApplicationDto) {
    val text = app.remarks ?: app.rejectionReason ?: ""
    val isActionReq = app.normalizedStatus == "action_required"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(
            containerColor = if (isActionReq) Color(0xFFFEF3C7) else FinTheme.colors.surfaceWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActionReq) Color(0xFFFCD34D) else FinTheme.colors.borderSubtle
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isActionReq) Icons.Default.Warning else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isActionReq) Color(0xFFB45309) else FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActionReq) "Action Required by Applicant" else "Officer Remarks",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActionReq) Color(0xFF92400E) else FinTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = FinTheme.typography.bodyMedium,
                color = if (isActionReq) Color(0xFF78350F) else FinTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun LinkedDocumentsCard(documents: List<SubmittedDocumentDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Submitted Documents (${documents.size})",
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (documents.isEmpty()) {
                Text(
                    text = "No linked documents on this application record.",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (doc in documents) {
                        SubmittedDocRow(doc = doc)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmittedDocRow(doc: SubmittedDocumentDto) {
    val isVerified = doc.displayStatus == "VERIFIED"
    val isRejected = doc.displayStatus == "REJECTED"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(FinTheme.colors.surfaceSubtle)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.name ?: doc.fileName ?: doc.documentType ?: "Document",
                style = FinTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = FinTheme.colors.textPrimary
            )
            if (!doc.fileName.isNullOrBlank()) {
                Text(
                    text = doc.fileName,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        isVerified -> Color(0xFFDCFCE7)
                        isRejected -> Color(0xFFFEE2E2)
                        else -> Color(0xFFFEF3C7)
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = doc.displayStatus,
                style = FinTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    isVerified -> Color(0xFF15803D)
                    isRejected -> Color(0xFFB91C1C)
                    else -> Color(0xFFB45309)
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = FinTheme.typography.labelSmall, color = FinTheme.colors.textMuted)
        Text(text = value, style = FinTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = FinTheme.colors.textPrimary)
    }
}
