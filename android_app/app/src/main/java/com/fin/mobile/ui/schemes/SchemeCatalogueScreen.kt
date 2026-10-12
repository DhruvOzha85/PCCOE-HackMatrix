package com.fin.mobile.ui.schemes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeCatalogueScreen(
    viewModel: SchemeCatalogueViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSchemeDetails: (schemeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Scheme Discovery",
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
                IconButton(onClick = { viewModel.refresh() }) {
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

        // Search Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinTheme.colors.surfaceWhite)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search by scheme name, ministry, or benefits...",
                        style = FinTheme.typography.bodyMedium,
                        color = FinTheme.colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = FinTheme.colors.textMuted
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = FinTheme.colors.textMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = FinTheme.shapes.md,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FinTheme.colors.surfaceSubtle,
                    unfocusedContainerColor = FinTheme.colors.surfaceSubtle,
                    focusedIndicatorColor = FinTheme.colors.primaryGreen,
                    unfocusedIndicatorColor = FinTheme.colors.borderSubtle
                )
            )
        }

        // Horizontal Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinTheme.colors.surfaceWhite)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (category in CANONICAL_SCHEME_CATEGORIES) {
                val isSelected = uiState.selectedCategory == category.key
                val count = uiState.categoryCounts[category.key]

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onCategorySelected(category.key) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.label,
                                style = FinTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (count != null && count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    style = FinTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isSelected) FinTheme.colors.textInverse else FinTheme.colors.textMuted
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinTheme.colors.primaryGreen,
                        selectedLabelColor = FinTheme.colors.textInverse,
                        containerColor = FinTheme.colors.surfaceSubtle,
                        labelColor = FinTheme.colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = FinTheme.colors.borderSubtle,
                        selectedBorderColor = FinTheme.colors.primaryGreen,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        // Content States
        when {
            uiState.isLoading && uiState.schemes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching welfare schemes...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.schemes.isEmpty() -> {
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
                                text = "Unable to load schemes",
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
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            uiState.schemes.isEmpty() -> {
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
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = FinTheme.colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No schemes found",
                                style = FinTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FinTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No schemes match '${uiState.query}'. Try adjusting your keywords or category filters.",
                                style = FinTheme.typography.bodyMedium,
                                color = FinTheme.colors.textMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.onSearchQueryChanged("")
                                    viewModel.onCategorySelected("all")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.schemes.size} of ${uiState.total} schemes",
                                style = FinTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = FinTheme.colors.textMuted
                            )
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = FinTheme.colors.primaryGreen
                                )
                            }
                        }
                    }

                    items(uiState.schemes) { scheme ->
                        CatalogueSchemeCard(
                            scheme = scheme,
                            onClick = { onNavigateToSchemeDetails(scheme.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogueSchemeCard(
    scheme: SchemeDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        shape = FinTheme.shapes.md,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ministry & Jurisdiction
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
                    // Jurisdiction Badge
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

                    // DBT Badge if applicable
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

            Spacer(modifier = Modifier.height(8.dp))

            // Scheme Title
            Text(
                text = scheme.displayTitle,
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )

            // Description
            if (scheme.displayDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = scheme.displayDescription,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Benefits & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val benefitText = scheme.maxBenefit?.let {
                    if (it > 0) "₹ ${it.toLong()}" else null
                } ?: scheme.benefit ?: "Financial Welfare"

                Column {
                    Text(
                        text = "Assistance Benefit",
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.textMuted
                    )
                    Text(
                        text = benefitText,
                        style = FinTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.primaryGreen
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "View Details",
                        style = FinTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.primaryGreen
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

            // Tags
            val tags = scheme.tags ?: emptyList()
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (tag in tags.take(3)) {
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
