package com.fin.mobile.ui.schemes

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.EligibilityRuleDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeDetailScreen(
    viewModel: SchemeDetailViewModel,
    onNavigateBack: () -> Unit,
    onApply: (schemeId: String) -> Unit = {},
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
                    text = uiState.scheme?.displayTitle ?: "Scheme Details",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                IconButton(onClick = { viewModel.loadScheme() }) {
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
                            text = "Loading scheme details...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.scheme == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                        shape = FinTheme.shapes.md,
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
                                text = "Unable to load scheme",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadScheme() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            uiState.scheme != null -> {
                val scheme = uiState.scheme!!

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Scheme Header Card
                    item {
                        SchemeHeaderCard(
                            scheme = scheme,
                            onCheckEligibility = { viewModel.evaluateEligibility() },
                            isEvaluating = uiState.isEvaluatingEligibility
                        )
                    }

                    // Tab Selector
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = uiState.activeTab.ordinal,
                            containerColor = FinTheme.colors.surfaceWhite,
                            contentColor = FinTheme.colors.primaryGreen,
                            edgePadding = 0.dp
                        ) {
                            SchemeDetailTab.entries.forEach { tab ->
                                Tab(
                                    selected = uiState.activeTab == tab,
                                    onClick = { viewModel.setTab(tab) },
                                    text = {
                                        Text(
                                            text = tab.label,
                                            style = FinTheme.typography.labelSmall,
                                            fontWeight = if (uiState.activeTab == tab) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Tab Contents
                    when (uiState.activeTab) {
                        SchemeDetailTab.OVERVIEW -> {
                            item {
                                OverviewTabContent(
                                    scheme = scheme,
                                    onOpenUrl = { url ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            // Prevent crash if URL cannot be handled or is invalid
                                        }
                                    }
                                )
                            }
                        }

                        SchemeDetailTab.BENEFITS -> {
                            item {
                                BenefitsTabContent(scheme = scheme)
                            }
                        }

                        SchemeDetailTab.ELIGIBILITY -> {
                            item {
                                EligibilityTabContent(
                                    scheme = scheme,
                                    evaluation = uiState.eligibilityResult,
                                    isEvaluating = uiState.isEvaluatingEligibility,
                                    evaluationError = uiState.eligibilityError,
                                    onEvaluate = { viewModel.evaluateEligibility() }
                                )
                            }
                        }

                        SchemeDetailTab.FAQS -> {
                            item {
                                FaqsTabContent(
                                    faqs = uiState.faqs,
                                    isLoading = uiState.isLoadingFaqs
                                )
                            }
                        }
                    }
                }

                // Docked Bottom Action Bar
                androidx.compose.material3.Surface(
                    color = FinTheme.colors.surfaceWhite,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setTab(SchemeDetailTab.ELIGIBILITY)
                                viewModel.evaluateEligibility()
                            },
                            modifier = Modifier.weight(1f),
                            shape = FinTheme.shapes.md
                        ) {
                            Text("Eligibility")
                        }
                        Button(
                            onClick = { onApply(scheme.id) },
                            modifier = Modifier.weight(1f),
                            shape = FinTheme.shapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                        ) {
                            Text("Apply Now")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchemeHeaderCard(
    scheme: SchemeDto,
    onCheckEligibility: () -> Unit,
    isEvaluating: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Ministry & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheme.displayMinistry,
                    style = FinTheme.typography.labelSmall,
                    color = FinTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FinTheme.colors.surfaceSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scheme.displayJurisdiction,
                            style = FinTheme.typography.labelSmall,
                            color = FinTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (scheme.dbtScheme == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FinTheme.colors.successLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DBT",
                                style = FinTheme.typography.labelSmall,
                                color = FinTheme.colors.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = scheme.displayTitle,
                style = FinTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            if (!scheme.department.isNullOrBlank() && scheme.department != scheme.ministry) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dept: ${scheme.department}",
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benefit Summary Box
            val benefitText = scheme.maxBenefit?.let {
                if (it > 0) "₹ ${it.toLong()} Financial Assistance" else null
            } ?: scheme.benefit ?: scheme.benefits ?: "Statutory Welfare Assistance"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FinTheme.shapes.md)
                    .background(FinTheme.colors.primaryGreenLight)
                    .border(1.dp, FinTheme.colors.primaryGreenBorder, FinTheme.shapes.md)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Assistance Overview",
                            style = FinTheme.typography.labelSmall,
                            color = FinTheme.colors.primaryGreenDark,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = benefitText,
                            style = FinTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.primaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Check Eligibility CTA
            Button(
                onClick = onCheckEligibility,
                enabled = !isEvaluating,
                modifier = Modifier.fillMaxWidth(),
                shape = FinTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                if (isEvaluating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FinTheme.colors.textInverse
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evaluating Profile Criteria...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check My Eligibility",
                        style = FinTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    scheme: SchemeDto,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Scheme Description",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scheme.detailedDescription ?: scheme.displayDescription,
                style = FinTheme.typography.bodyMedium,
                color = FinTheme.colors.textSecondary,
                lineHeight = 22.sp
            )

            // Target Beneficiaries
            val beneficiaries = scheme.targetBeneficiaries?.joinToString(", ")
                ?: scheme.targetAudience
            if (!beneficiaries.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = FinTheme.colors.borderSubtle)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Target Beneficiaries",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = beneficiaries,
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary
                )
            }

            // Application Mode
            if (!scheme.applicationMode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Application Mode: ${scheme.applicationMode}",
                    style = FinTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = FinTheme.colors.primaryGreen
                )
            }

            // Official Website Link
            if (!scheme.sourceUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = FinTheme.colors.borderSubtle)
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { onOpenUrl(scheme.sourceUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = FinTheme.shapes.md
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visit Official Government Portal",
                        style = FinTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitsTabContent(scheme: SchemeDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Benefits & Financial Assistance",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val benefitsText = scheme.benefits ?: scheme.benefit ?: "Details specified by implementing department."
            Text(
                text = benefitsText,
                style = FinTheme.typography.bodyMedium,
                color = FinTheme.colors.textSecondary,
                lineHeight = 22.sp
            )

            if (scheme.maxBenefit != null && scheme.maxBenefit > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FinTheme.shapes.sm)
                        .background(FinTheme.colors.surfaceSubtle)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Maximum Financial Grant",
                            style = FinTheme.typography.labelSmall,
                            color = FinTheme.colors.textMuted
                        )
                        Text(
                            text = "₹ ${scheme.maxBenefit.toLong()}",
                            style = FinTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.primaryGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EligibilityTabContent(
    scheme: SchemeDto,
    evaluation: EligibilityEvaluationDto?,
    isEvaluating: Boolean,
    evaluationError: String?,
    onEvaluate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Live Evaluation Result Card (if evaluated or error)
        if (evaluation != null) {
            LiveEvaluationResultCard(evaluation = evaluation)
        } else if (evaluationError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.errorLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.errorBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = FinTheme.colors.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evaluation Notice",
                            style = FinTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.error
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = evaluationError,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onEvaluate,
                        colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.error)
                    ) {
                        Text("Retry Evaluation")
                    }
                }
            }
        } else {
            // Prompt to check
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Automated Eligibility Engine",
                        style = FinTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Evaluate your profile against this scheme's statutory criteria using FIN's intelligence engine.",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onEvaluate,
                        enabled = !isEvaluating,
                        colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                    ) {
                        Text("Evaluate Now")
                    }
                }
            }
        }

        // Statutory Criteria Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FinTheme.shapes.md,
            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Eligibility Criteria Guidelines",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val criteriaText = scheme.eligibility
                    ?: "Subject to verification of applicant details against statutory rules."
                Text(
                    text = criteriaText,
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textSecondary,
                    lineHeight = 22.sp
                )

                // Required Documents
                val docs = scheme.displayDocuments
                if (docs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = FinTheme.colors.borderSubtle)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Required Documents Checklist",
                        style = FinTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (doc in docs) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(FinTheme.colors.surfaceSubtle)
                                    .border(1.dp, FinTheme.colors.borderSubtle, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = FinTheme.colors.primaryGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = doc,
                                        style = FinTheme.typography.labelSmall,
                                        color = FinTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveEvaluationResultCard(evaluation: EligibilityEvaluationDto) {
    val status = evaluation.status.uppercase()
    val isEligible = evaluation.isEligible == true || status == "PASS"

    val (badgeText, badgeColor, badgeBg, verdictTitle) = when {
        isEligible -> Quadruple("Eligible", FinTheme.colors.success, FinTheme.colors.successLight, "All Statutory Conditions Met")
        status == "REVIEW" -> Quadruple("Review Required", FinTheme.colors.warning, FinTheme.colors.warningLight, "Manual Departmental Verification Required")
        status == "FAIL" -> Quadruple("Not Eligible", FinTheme.colors.error, FinTheme.colors.errorLight, "Eligibility Criteria Not Met")
        else -> Quadruple("Evidence Needed", FinTheme.colors.accentBlue, FinTheme.colors.accentBlueLight, "Additional Profile Verification Required")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = badgeBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Evaluation Report",
                    style = FinTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.textInverse,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = verdictTitle,
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            if (!evaluation.verdictReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = evaluation.verdictReason,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textSecondary
                )
            }

            // Rules breakdown
            if (evaluation.rules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Evaluated Statutory Rules (${evaluation.rules.size})",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rule in evaluation.rules) {
                        RuleEvaluationRow(rule = rule)
                    }
                }
            }

            // Missing profile fields
            if (evaluation.missingFieldLabels.isNotEmpty() || evaluation.missingFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                val fields = if (evaluation.missingFieldLabels.isNotEmpty()) evaluation.missingFieldLabels else evaluation.missingFields
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FinTheme.shapes.sm)
                        .background(FinTheme.colors.surfaceWhite)
                        .border(1.dp, FinTheme.colors.warningBorder, FinTheme.shapes.sm)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FinTheme.colors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Action Required: Missing Information",
                                style = FinTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.warning
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        for (field in fields) {
                            Text(
                                text = "• $field proof required",
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleEvaluationRow(rule: EligibilityRuleDto) {
    val status = rule.status.lowercase()
    val (icon, iconColor) = when (status) {
        "pass" -> Pair(Icons.Default.CheckCircle, FinTheme.colors.success)
        "fail" -> Pair(Icons.Default.Close, FinTheme.colors.error)
        else -> Pair(Icons.Default.Warning, FinTheme.colors.warning)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(FinTheme.colors.surfaceWhite)
            .border(1.dp, FinTheme.colors.borderSubtle, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = status,
                tint = iconColor,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = rule.description ?: rule.ruleId,
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FinTheme.colors.textPrimary
                )
                if (!rule.reason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = rule.reason,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }
                if (!rule.source.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Source: ${rule.source}",
                        style = FinTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = FinTheme.colors.primaryGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqsTabContent(
    faqs: List<SchemeFaqDto>,
    isLoading: Boolean
) {
    if (isLoading && faqs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
        }
    } else if (faqs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = FinTheme.shapes.md,
            colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = FinTheme.colors.textMuted,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No FAQs available",
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textMuted
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (faq in faqs) {
                FaqAccordionCard(faq = faq)
            }
        }
    }
}

@Composable
private fun FaqAccordionCard(faq: SchemeFaqDto) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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
                    text = faq.question,
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = FinTheme.colors.textSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = FinTheme.colors.borderSubtle)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        style = FinTheme.typography.bodyMedium,
                        color = FinTheme.colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
