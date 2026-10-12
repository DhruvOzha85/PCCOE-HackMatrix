package com.fin.mobile.ui.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.repository.DocumentFileValidator
import com.fin.mobile.ui.theme.FinTheme
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    viewModel: DocumentVaultViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showUploadModal by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<DocumentDto?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Document Vault",
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
                IconButton(onClick = { viewModel.loadDocuments() }) {
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

        // Notification banner if action message exists
        AnimatedVisibility(visible = uiState.actionMessage != null) {
            uiState.actionMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FinTheme.colors.successLight)
                        .border(1.dp, FinTheme.colors.successBorder)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FinTheme.colors.success,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = FinTheme.typography.bodySmall,
                                color = FinTheme.colors.success,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = FinTheme.colors.success,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        when {
            uiState.isLoading && uiState.documents.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading verified documents...",
                            style = FinTheme.typography.bodyMedium,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            uiState.errorMessage != null && uiState.documents.isEmpty() -> {
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
                                text = "Failed to load documents",
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
                                onClick = { viewModel.loadDocuments() },
                                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Vault Stats & Security Banner
                    item {
                        VaultHeaderCard(
                            total = uiState.documents.size,
                            verified = uiState.documents.count { it.displayStatus == "VERIFIED" },
                            pending = uiState.documents.count { it.displayStatus == "PENDING" },
                            onUploadClick = { showUploadModal = true }
                        )
                    }

                    // Upload Document Button
                    item {
                        Button(
                            onClick = { showUploadModal = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FinTheme.shapes.md,
                            colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upload Official Document",
                                style = FinTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Documents List
                    if (uiState.documents.isEmpty()) {
                        item {
                            EmptyVaultCard(onUploadClick = { showUploadModal = true })
                        }
                    } else {
                        items(uiState.documents, key = { it.id }) { document ->
                            DocumentItemCard(
                                document = document,
                                isExtracting = uiState.extractingDocumentId == document.id,
                                isDeleting = uiState.deletingDocumentId == document.id,
                                onCardClick = { viewModel.selectDocument(document) },
                                onExtractClick = { viewModel.extractDocument(document.id) },
                                onDeleteClick = { documentToDelete = document },
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
                }
            }
        }
    }

    // Upload Bottom Sheet Modal
    if (showUploadModal) {
        UploadDocumentModal(
            isUploading = uiState.isUploading,
            uploadError = uiState.uploadError,
            onDismiss = {
                showUploadModal = false
                viewModel.clearMessages()
            },
            onUpload = { fileBytes, fileName, mimeType, docType ->
                viewModel.uploadDocument(fileBytes, fileName, mimeType, docType)
                showUploadModal = false
            }
        )
    }

    // Document Details Bottom Sheet
    uiState.selectedDocument?.let { selectedDoc ->
        DocumentDetailBottomSheet(
            document = selectedDoc,
            isExtracting = uiState.extractingDocumentId == selectedDoc.id,
            onDismiss = { viewModel.selectDocument(null) },
            onExtract = { viewModel.extractDocument(selectedDoc.id) },
            onDelete = {
                documentToDelete = selectedDoc
                viewModel.selectDocument(null)
            },
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

    // Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = {
                Text(
                    text = "Delete Document",
                    style = FinTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${doc.displayFileName}'? This will remove it from your document vault and any linked applications.",
                    style = FinTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = doc.id
                        documentToDelete = null
                        viewModel.deleteDocument(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VaultHeaderCard(
    total: Int,
    verified: Int,
    pending: Int,
    onUploadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FinTheme.shapes.lg,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FinTheme.colors.primaryGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Citizen Document Vault",
                        style = FinTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = "Statutory proofs & AI OCR verification",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = FinTheme.colors.borderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            // 3 Stat Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCounter(label = "Total Vault", count = total.toString(), color = FinTheme.colors.textPrimary)
                StatCounter(label = "Verified", count = verified.toString(), color = FinTheme.colors.success)
                StatCounter(label = "Pending Review", count = pending.toString(), color = FinTheme.colors.warning)
            }
        }
    }
}

@Composable
private fun StatCounter(label: String, count: String, color: Color) {
    Column {
        Text(
            text = count,
            style = FinTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = FinTheme.typography.labelSmall,
            color = FinTheme.colors.textMuted
        )
    }
}

@Composable
private fun EmptyVaultCard(onUploadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = FinTheme.colors.textMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No documents uploaded yet",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upload Aadhaar, Income Certificate, Caste Proof or Land records to automate your scheme eligibility evaluation.",
                style = FinTheme.typography.bodyMedium,
                color = FinTheme.colors.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                Text("Upload First Document")
            }
        }
    }
}

@Composable
private fun DocumentItemCard(
    document: DocumentDto,
    isExtracting: Boolean,
    isDeleting: Boolean,
    onCardClick: () -> Unit,
    onExtractClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val status = document.displayStatus
    val (statusLabel, statusColor, statusBg) = when (status) {
        "VERIFIED" -> Triple("Verified", FinTheme.colors.success, FinTheme.colors.successLight)
        "REJECTED" -> Triple("Rejected", FinTheme.colors.error, FinTheme.colors.errorLight)
        "REVIEW_REQUIRED" -> Triple("Action Required", FinTheme.colors.warning, FinTheme.colors.warningLight)
        else -> Triple("Pending Review", FinTheme.colors.warning, FinTheme.colors.warningLight)
    }

    val icon = when (document.displayType.lowercase()) {
        "aadhaar", "pan" -> Icons.Default.Badge
        "income_cert" -> Icons.Default.CurrencyRupee
        "bank_passbook" -> Icons.Default.AccountBalance
        "land" -> Icons.Default.Landscape
        else -> Icons.Default.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        shape = FinTheme.shapes.md,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type & Verification Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FinTheme.colors.primaryGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = FinTheme.colors.primaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = getDocTypeLabel(document.displayType),
                            style = FinTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )
                        Text(
                            text = document.displayFileName,
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = FinTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Doc Number / Extracted Preview
            if (!document.docNumber.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ID: ${document.docNumber}",
                    style = FinTheme.typography.labelSmall,
                    color = FinTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = FinTheme.colors.borderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Preview/Download Button
                    val downloadUrl = document.downloadUrl
                    if (!downloadUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onOpenUrl(downloadUrl) },
                            shape = FinTheme.shapes.sm,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "View", style = FinTheme.typography.labelSmall)
                        }
                    }

                    // Extract Button
                    OutlinedButton(
                        onClick = onExtractClick,
                        enabled = !isExtracting,
                        shape = FinTheme.shapes.sm,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = FinTheme.colors.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Extracting...", style = FinTheme.typography.labelSmall)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = FinTheme.colors.accentSaffron
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "OCR Facts", style = FinTheme.typography.labelSmall)
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    modifier = Modifier.size(28.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = FinTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadDocumentModal(
    isUploading: Boolean,
    uploadError: String?,
    onDismiss: () -> Unit,
    onUpload: (ByteArray, String, String, String) -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(CANONICAL_DOCUMENT_TYPES[0].key) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var selectedMimeType by remember { mutableStateOf("application/pdf") }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val meta = queryFileMetadata(context, uri)
            selectedFileName = meta.first
            selectedFileSize = meta.second
            selectedMimeType = context.contentResolver.getType(uri) ?: "application/pdf"

            val validation = DocumentFileValidator.validateFile(
                fileName = selectedFileName,
                fileSizeBytes = selectedFileSize,
                mimeType = selectedMimeType
            )
            localValidationError = if (validation is DocumentFileValidator.ValidationResult.Invalid) {
                validation.reason
            } else {
                null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Upload Verified Document",
                style = FinTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Text(
                text = "Supported: PDF, JPEG, PNG, WEBP (Maximum 10 MB)",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Document Type Dropdown
            Text(
                text = "Select Document Type",
                style = FinTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = getDocTypeLabel(selectedType),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = FinTheme.shapes.md
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    for (option in CANONICAL_DOCUMENT_TYPES) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = option.label, fontWeight = FontWeight.SemiBold)
                                    Text(text = option.category, style = FinTheme.typography.labelSmall, color = FinTheme.colors.textMuted)
                                }
                            },
                            onClick = {
                                selectedType = option.key
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // File Selector Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FinTheme.shapes.md)
                    .background(FinTheme.colors.surfaceSubtle)
                    .border(1.dp, FinTheme.colors.borderSubtle, FinTheme.shapes.md)
                    .clickable {
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp"))
                    }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (selectedFileUri != null) {
                        Text(
                            text = selectedFileName,
                            style = FinTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = FinTheme.colors.textPrimary
                        )
                        Text(
                            text = "${selectedFileSize / 1024} KB",
                            style = FinTheme.typography.bodySmall,
                            color = FinTheme.colors.textMuted
                        )
                    } else {
                        Text(
                            text = "Tap to choose PDF or Image",
                            style = FinTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = FinTheme.colors.primaryGreen
                        )
                        Text(
                            text = "Aadhaar, ITR, Income or Caste Certificate",
                            style = FinTheme.typography.labelSmall,
                            color = FinTheme.colors.textMuted
                        )
                    }
                }
            }

            // Error display
            val activeError = localValidationError ?: uploadError
            if (activeError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = activeError,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    val uri = selectedFileUri
                    if (uri != null && localValidationError == null) {
                        try {
                            val stream: InputStream? = context.contentResolver.openInputStream(uri)
                            val bytes = stream?.readBytes() ?: ByteArray(0)
                            stream?.close()
                            if (bytes.isNotEmpty()) {
                                onUpload(bytes, selectedFileName, selectedMimeType, selectedType)
                            }
                        } catch (e: Exception) {
                            localValidationError = "Unable to read file: ${e.localizedMessage}"
                        }
                    }
                },
                enabled = selectedFileUri != null && localValidationError == null && !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = FinTheme.shapes.md,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FinTheme.colors.textInverse
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading to Vault...")
                } else {
                    Text(
                        text = "Upload & Store in Vault",
                        style = FinTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DocumentDetailBottomSheet(
    document: DocumentDto,
    isExtracting: Boolean,
    onDismiss: () -> Unit,
    onExtract: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getDocTypeLabel(document.displayType),
                        style = FinTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = document.displayFileName,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted
                    )
                }

                val (badgeLabel, badgeColor, badgeBg) = when (document.displayStatus) {
                    "VERIFIED" -> Triple("Verified", FinTheme.colors.success, FinTheme.colors.successLight)
                    "REJECTED" -> Triple("Rejected", FinTheme.colors.error, FinTheme.colors.errorLight)
                    else -> Triple("Pending Review", FinTheme.colors.warning, FinTheme.colors.warningLight)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = badgeLabel, color = badgeColor, fontWeight = FontWeight.Bold, style = FinTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = FinTheme.colors.borderSubtle)
            Spacer(modifier = Modifier.height(14.dp))

            // Metadata: Issuer & Document Number
            if (!document.docNumber.isNullOrBlank()) {
                MetadataRow(label = "Document ID", value = document.docNumber)
            }
            if (!document.issuer.isNullOrBlank()) {
                MetadataRow(label = "Issuing Authority", value = document.issuer)
            }
            if (!document.displayDate.isNullOrBlank()) {
                MetadataRow(label = "Uploaded Date", value = document.displayDate.take(10))
            }

            // Extracted facts if available
            val facts = document.extractedData
            if (!facts.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Extracted Facts (AI OCR)",
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.primaryGreen
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(FinTheme.shapes.sm)
                        .background(FinTheme.colors.surfaceSubtle)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for ((key, value) in facts) {
                        if (value != null && value.toString().isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatFactKey(key),
                                    style = FinTheme.typography.bodySmall,
                                    color = FinTheme.colors.textMuted
                                )
                                Text(
                                    text = value.toString(),
                                    style = FinTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // OCR Provenance Note
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Intelligence OCR extracts structured data for automated eligibility checks. Statutory sanction requires official departmental verification.",
                style = FinTheme.typography.labelSmall,
                color = FinTheme.colors.textMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val downloadUrl = document.downloadUrl
                if (!downloadUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onOpenUrl(downloadUrl) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View / Download")
                    }
                }

                OutlinedButton(
                    onClick = onExtract,
                    enabled = !isExtracting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Re-extract OCR")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FinTheme.colors.error)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete From Vault")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = FinTheme.typography.bodySmall, color = FinTheme.colors.textMuted)
        Text(text = value, style = FinTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = FinTheme.colors.textPrimary)
    }
}

private fun getDocTypeLabel(key: String): String {
    return CANONICAL_DOCUMENT_TYPES.find { it.key.equals(key, ignoreCase = true) }?.label
        ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun formatFactKey(key: String): String {
    return key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun queryFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "document.pdf"
    var size = 0L
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx != -1) name = it.getString(nameIdx) ?: name
                if (sizeIdx != -1) size = it.getLong(sizeIdx)
            }
        }
    } catch (_: Exception) { }
    return Pair(name, size)
}
