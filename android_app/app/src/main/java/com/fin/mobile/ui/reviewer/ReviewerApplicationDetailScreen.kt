package com.fin.mobile.ui.reviewer

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.ReviewHistoryItemDto
import com.fin.mobile.data.remote.dto.ReviewerApplicantInfoDto
import com.fin.mobile.data.remote.dto.ReviewerDocumentDto
import com.fin.mobile.ui.theme.FinTheme

@Composable
fun ReviewerApplicationDetailScreen(
    viewModel: ReviewerApplicationDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        // Top Header
        Surface(
            color = FinTheme.colors.surfaceWhite,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FinTheme.colors.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = uiState.details?.application?.applicationNumber ?: uiState.details?.application?.referenceId ?: "Application Review",
                            style = FinTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )
                        Text(
                            text = "Departmental Verification Workflow",
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ReviewerStatusBadge(status = uiState.details?.application?.status)
                    IconButton(onClick = { viewModel.loadDetails() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = FinTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Success / Error Banner
        uiState.successMessage?.let { msg ->
            Surface(
                color = Color(0xFFF0FDF4),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = msg, style = FinTheme.typography.bodySmall, color = Color(0xFF166534))
                    IconButton(onClick = { viewModel.clearToast() }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF166534), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        uiState.errorMessage?.let { err ->
            Surface(
                color = Color(0xFFFEF2F2),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = err, style = FinTheme.typography.bodySmall, color = Color(0xFF991B1B))
                    IconButton(onClick = { viewModel.clearToast() }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main Content
        if (uiState.isLoading && uiState.details == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
            }
        } else if (uiState.details != null) {
            val details = uiState.details!!

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Applicant Profile Card
                item {
                    ApplicantOverviewCard(applicant = details.applicant)
                }

                // Scheme Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FinTheme.shapes.md,
                        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SCHEME APPLIED",
                                style = FinTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = details.scheme?.displayTitle ?: details.application.displayTitle,
                                style = FinTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.primaryGreenDark
                            )
                            if (details.scheme?.displayDescription?.isNotBlank() == true) {
                                Text(
                                    text = details.scheme?.displayDescription ?: "",
                                    style = FinTheme.typography.bodySmall,
                                    color = FinTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Documents & Verification Section Header
                item {
                    Text(
                        text = "Document Verification (${details.documents.size})",
                        style = FinTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                }

                if (details.documents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FinTheme.shapes.sm,
                            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                        ) {
                            Text(
                                text = "No documents attached to this application.",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(details.documents, key = { it.id }) { doc ->
                        ReviewerDocumentCard(
                            doc = doc,
                            isVerifying = uiState.verifyingDocumentId == doc.id,
                            onVerify = { viewModel.verifyDocument(doc.id, "VERIFIED") },
                            onReject = { viewModel.verifyDocument(doc.id, "REJECTED") }
                        )
                    }
                }

                // Review History Timeline
                if (details.reviewHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Auditable Review History",
                            style = FinTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )
                    }
                    items(details.reviewHistory, key = { it.id }) { review ->
                        ReviewHistoryCard(review = review)
                    }
                }
            }

            // Fixed Bottom Action Bar for Consequential Decisions
            ReviewDecisionActionBar(
                onApprove = { viewModel.openDecisionDialog("approve") },
                onRequestChanges = { viewModel.openDecisionDialog("request_changes") },
                onReject = { viewModel.openDecisionDialog("reject") }
            )
        }
    }

    // Confirmation & Remark Dialog
    if (uiState.decisionDialog.isOpen) {
        DecisionConfirmationDialog(
            dialogState = uiState.decisionDialog,
            isSubmitting = uiState.isActionSubmitting,
            onRemarkChanged = { viewModel.onRemarkChanged(it) },
            onConfirm = { viewModel.submitDecision() },
            onDismiss = { viewModel.closeDecisionDialog() }
        )
    }
}

@Composable
private fun ApplicantOverviewCard(applicant: ReviewerApplicantInfoDto?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "APPLICANT DOSSIER",
                    style = FinTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = applicant?.fullName ?: "Citizen Applicant",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            applicant?.email?.let {
                Text(text = "Email: $it", style = FinTheme.typography.bodySmall, color = FinTheme.colors.textSecondary)
            }
            applicant?.phone?.let {
                Text(text = "Phone: $it", style = FinTheme.typography.bodySmall, color = FinTheme.colors.textSecondary)
            }

            val prof = applicant?.profile
            if (prof != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        prof.annualIncome?.let {
                            Text(text = "Annual Family Income: ₹$it", style = FinTheme.typography.bodySmall, color = FinTheme.colors.textPrimary)
                        }
                        prof.casteCategory?.let {
                            Text(text = "Social Category: $it", style = FinTheme.typography.bodySmall, color = FinTheme.colors.textPrimary)
                        }
                        prof.state?.let {
                            Text(text = "State: $it", style = FinTheme.typography.bodySmall, color = FinTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewerDocumentCard(
    doc: ReviewerDocumentDto,
    isVerifying: Boolean,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doc.documentType?.replace("_", " ")?.uppercase() ?: "DOCUMENT",
                        style = FinTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                }

                // Document Verification Status Badge
                val statusClean = (doc.verificationStatus ?: "PENDING").uppercase()
                val (badgeBg, badgeColor) = when (statusClean) {
                    "VERIFIED" -> Color(0xFFF0FDF4) to Color(0xFF166534)
                    "REJECTED" -> Color(0xFFFEF2F2) to Color(0xFF991B1B)
                    else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusClean,
                        style = FinTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            doc.fileName?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "File: $it",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted,
                    fontSize = 11.sp
                )
            }

            // OCR Preliminary Extracted Facts Card (with strict disclaimer)
            doc.ocr?.let { ocr ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FinTheme.colors.primaryGreenDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Assistive OCR Extraction",
                                style = FinTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.primaryGreenDark
                            )
                        }

                        ocr.disclaimer?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = it,
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textMuted,
                                fontSize = 10.sp
                            )
                        }

                        ocr.extractedFields?.let { fields ->
                            if (fields.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                fields.forEach { (k, v) ->
                                    Text(
                                        text = "• ${k.replace('_', ' ').capitalizeWords()}: $v",
                                        style = FinTheme.typography.bodySmall,
                                        color = FinTheme.colors.textPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Authoritative Document Verification Action Buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isVerifying) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = FinTheme.colors.primaryGreen
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reject Doc", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onVerify,
                        colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Verify Doc", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryCard(review: ReviewHistoryItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.sm,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Decision: ${review.decision?.uppercase() ?: "REVIEW"}",
                    style = FinTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                review.createdAt?.let {
                    Text(
                        text = it.take(10),
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text = "Officer: ${review.reviewerName ?: "Department Reviewer"}",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted,
                fontSize = 11.sp
            )
            if (!review.remark.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: \"${review.remark}\"",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ReviewDecisionActionBar(
    onApprove: () -> Unit,
    onRequestChanges: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = FinTheme.colors.surfaceWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onReject,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onRequestChanges,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                modifier = Modifier.weight(1.2f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "Request Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text(text = "Approve", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DecisionConfirmationDialog(
    dialogState: DecisionDialogState,
    isSubmitting: Boolean,
    onRemarkChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val decision = dialogState.decisionType?.lowercase() ?: "approve"
    val isMandatoryRemark = decision == "reject" || decision == "request_changes"
    val actionTitle = when (decision) {
        "approve" -> "Approve Application"
        "request_changes" -> "Request Changes from Applicant"
        "reject" -> "Reject Application"
        else -> "Submit Decision"
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = actionTitle,
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = when (decision) {
                        "approve" -> "Are you sure you want to approve this application? All associated documents will be authoritatively marked as VERIFIED."
                        "request_changes" -> "The application status will become ACTION REQUIRED. You must provide clear instructions to the applicant below."
                        "reject" -> "Are you sure you want to reject this application? This is a consequential administrative action. A detailed reason is mandatory."
                        else -> "Confirm your administrative decision."
                    },
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dialogState.remark,
                    onValueChange = onRemarkChanged,
                    label = {
                        Text(
                            text = if (isMandatoryRemark) "Reviewer Remarks (Mandatory) *" else "Decision Notes (Optional)",
                            fontSize = 12.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isMandatoryRemark) "Enter reason or required documents..." else "Optional remarks for applicant...",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    isError = dialogState.validationError != null,
                    enabled = !isSubmitting,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinTheme.colors.primaryGreen,
                        unfocusedBorderColor = FinTheme.colors.borderSubtle
                    )
                )

                dialogState.validationError?.let { err ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = err,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.error,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (decision) {
                        "approve" -> FinTheme.colors.primaryGreen
                        "request_changes" -> Color(0xFFD97706)
                        else -> Color(0xFFDC2626)
                    }
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(text = "Confirm Decision")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
