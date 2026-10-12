package com.fin.mobile.ui.applications

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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplySchemeScreen(
    viewModel: ApplySchemeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToApplicationDetail: (applicationId: String) -> Unit,
    onNavigateToMyApplications: () -> Unit,
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
                    text = "Apply for Scheme",
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FinTheme.colors.surfaceWhite
            )
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing Application Readiness...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            uiState.submittedApplication != null -> {
                val app = uiState.submittedApplication!!
                SuccessSubmissionCard(
                    applicationId = app.id,
                    referenceId = app.referenceId,
                    schemeName = app.displayTitle,
                    onTrackApplication = { onNavigateToApplicationDetail(app.id) },
                    onViewAllApplications = onNavigateToMyApplications
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scheme Header Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FinTheme.shapes.md,
                            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = uiState.scheme?.displayTitle ?: uiState.readiness?.schemeName ?: "Scheme Application",
                                    style = FinTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FinTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (!uiState.scheme?.displayBenefit.isNullOrBlank()) {
                                    Text(
                                        text = "Benefit: ${uiState.scheme?.displayBenefit}",
                                        style = FinTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FinTheme.colors.primaryGreen
                                    )
                                }
                            }
                        }
                    }

                    // Readiness Score Card
                    uiState.readiness?.let { readiness ->
                        item {
                            ReadinessScoreCard(
                                score = readiness.readinessScore,
                                label = readiness.readinessLabel ?: "Under Assessment",
                                eligibilityVerdict = readiness.eligibility?.verdict,
                                eligibilityReason = readiness.eligibility?.reason
                            )
                        }

                        // Required Documents Check
                        readiness.documents?.let { docs ->
                            item {
                                DocumentsReadinessCard(
                                    required = docs.required,
                                    uploaded = docs.uploaded,
                                    missing = docs.missing,
                                    details = docs.details ?: emptyList(),
                                    onNavigateToVault = onNavigateToVault
                                )
                            }
                        }

                        // Next Actions Recommendations
                        if (!readiness.nextActions.isNullOrEmpty()) {
                            item {
                                NextActionsCard(nextActions = readiness.nextActions)
                            }
                        }
                    }

                    // Duplicate Application Notice
                    if (uiState.isAlreadySubmitted) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FinTheme.shapes.md,
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Already Submitted",
                                            style = FinTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "An application for this scheme has already been recorded for your citizen profile. Track its progress in My Applications.",
                                        style = FinTheme.typography.bodyMedium,
                                        color = Color(0xFF78350F)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onNavigateToMyApplications,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309))
                                    ) {
                                        Text("View in My Applications")
                                    }
                                }
                            }
                        }
                    }

                    // Error message
                    if (uiState.errorMessage != null && !uiState.isAlreadySubmitted) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = FinTheme.shapes.md,
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFB91C1C))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = uiState.errorMessage ?: "",
                                        style = FinTheme.typography.bodyMedium,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                        }
                    }

                    // Declaration Checkbox
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = FinTheme.shapes.md,
                            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.hasConfirmedDeclaration,
                                    onCheckedChange = { viewModel.onDeclarationToggled(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = FinTheme.colors.primaryGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I declare that the information in my profile and vault documents is authentic. I authorize official scrutiny for scheme sanction.",
                                    style = FinTheme.typography.bodySmall,
                                    color = FinTheme.colors.textPrimary
                                )
                            }
                        }
                    }

                    // Submit Button
                    item {
                        Button(
                            onClick = { viewModel.submitApplication() },
                            enabled = uiState.hasConfirmedDeclaration && !uiState.isSubmitting && !uiState.isAlreadySubmitted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = FinTheme.shapes.md,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinTheme.colors.primaryGreen,
                                disabledContainerColor = FinTheme.colors.borderSubtle
                            )
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Submitting to Nodal Authority...")
                            } else {
                                Text(
                                    text = "Submit Official Application",
                                    style = FinTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessScoreCard(
    score: Int,
    label: String,
    eligibilityVerdict: String?,
    eligibilityReason: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Application Readiness",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                Text(
                    text = "$score%",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (score >= 75) FinTheme.colors.primaryGreen else Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (score >= 75) FinTheme.colors.primaryGreen else Color(0xFFD97706),
                trackColor = FinTheme.colors.surfaceSubtle
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (score >= 75) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (score >= 75) FinTheme.colors.primaryGreen else Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = FinTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (score >= 75) FinTheme.colors.primaryGreen else Color(0xFFD97706)
                )
            }

            if (!eligibilityReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = eligibilityReason,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun DocumentsReadinessCard(
    required: Int,
    uploaded: Int,
    missing: Int,
    details: List<com.fin.mobile.data.remote.dto.ReadinessDocDetailDto>,
    onNavigateToVault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Required Documents ($uploaded/$required)",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )

                if (missing > 0) {
                    OutlinedButton(
                        onClick = onNavigateToVault,
                        shape = FinTheme.shapes.sm
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (doc in details) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = doc.label ?: doc.type ?: "Document",
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textPrimary
                        )

                        Text(
                            text = if (doc.uploaded) "Ready ✓" else "Missing",
                            style = FinTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (doc.uploaded) FinTheme.colors.primaryGreen else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextActionsCard(
    nextActions: List<com.fin.mobile.data.remote.dto.ReadinessNextActionDto>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recommended Next Steps",
                style = FinTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (action in nextActions) {
                    val isHigh = action.priority == "high"
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = if (isHigh) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isHigh) Color(0xFFD97706) else FinTheme.colors.primaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = action.action ?: "Step",
                                style = FinTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FinTheme.colors.textPrimary
                            )
                            if (!action.detail.isNullOrBlank()) {
                                Text(
                                    text = action.detail,
                                    style = FinTheme.typography.bodySmall,
                                    color = FinTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessSubmissionCard(
    applicationId: String,
    referenceId: String,
    schemeName: String,
    onTrackApplication: () -> Unit,
    onViewAllApplications: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FinTheme.shapes.lg,
            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(FinTheme.colors.primaryGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Application Submitted!",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your application for $schemeName has been officially recorded with the Nodal Department.",
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(FinTheme.colors.surfaceSubtle)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Ref: $referenceId",
                        style = FinTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.primaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onTrackApplication,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen),
                    shape = FinTheme.shapes.md
                ) {
                    Text("Track Application Status")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onViewAllApplications,
                    modifier = Modifier.fillMaxWidth(),
                    shape = FinTheme.shapes.md
                ) {
                    Text("View All Applications")
                }
            }
        }
    }
}
