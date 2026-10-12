package com.fin.mobile.ui.chat

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fin.mobile.data.remote.dto.ChatCitationDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.ui.theme.FinTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CitizenChatScreen(
    viewModel: CitizenChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSchemeDetails: (schemeId: String) -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToApplications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FinTheme.colors.bgPrimary)
    ) {
        // App Header
        ChatHeader(
            onBack = onNavigateBack,
            onNewChat = { viewModel.startNewConversation() },
            onOpenHistory = { viewModel.toggleHistory(true) }
        )

        // Statutory Scope & Guardrail Banner
        AssistantScopeBanner()

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageItem(
                    message = message,
                    onSchemeClick = onNavigateToSchemeDetails,
                    onSuggestionClick = { viewModel.sendMessage(it) },
                    onRetry = { viewModel.retryLastMessage() }
                )
            }

            if (uiState.isLoading) {
                item {
                    LoadingBubble()
                }
            }
        }

        // Suggestions row if last message has suggestions or default suggestions
        val suggestions = uiState.messages.lastOrNull()?.suggestions?.takeIf { it.isNotEmpty() }
            ?: uiState.defaultSuggestions
        if (!uiState.isLoading && suggestions.isNotEmpty()) {
            SuggestionChipsRow(
                suggestions = suggestions,
                onSuggestionClick = { viewModel.sendMessage(it) }
            )
        }

        // Contextual Quick Nav Shortcuts
        ContextualQuickNavRow(
            onNavigateToDocuments = onNavigateToDocuments,
            onNavigateToApplications = onNavigateToApplications
        )

        // Text Input Bar
        ChatInputBar(
            inputText = uiState.inputText,
            isLoading = uiState.isLoading,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onSend = { viewModel.sendMessage() }
        )
    }

    // Chat History Bottom Sheet
    if (uiState.isHistoryOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistory(false) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = FinTheme.colors.surfaceWhite
        ) {
            ChatHistorySheet(
                history = uiState.historyList,
                isLoading = uiState.isLoadingHistory,
                activeId = uiState.conversationId,
                onSelect = { viewModel.selectConversation(it) },
                onDelete = { viewModel.deleteConversation(it) },
                onNew = { viewModel.startNewConversation() }
            )
        }
    }
}

@Composable
private fun ChatHeader(
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: () -> Unit
) {
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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FinTheme.colors.textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FinTheme.colors.primaryGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = FinTheme.colors.primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FIN AI Assistant",
                        style = FinTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textPrimary
                    )
                    Text(
                        text = "Statutory Guidance & Scheme Discovery",
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNewChat) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Conversation",
                        tint = FinTheme.colors.primaryGreen
                    )
                }
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = FinTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantScopeBanner() {
    Surface(
        color = Color(0xFFF0FDF4),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = FinTheme.colors.primaryGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Assistive AI Guidance only. Final benefit eligibility is subject to nodal department verification.",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.primaryGreenDark,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: UiChatMessage,
    onSchemeClick: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Author Badge for Assistant
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (message.isError) "Assistant Error" else "AI Guidance (Assistive)",
                    style = FinTheme.typography.labelSmall,
                    color = if (message.isError) FinTheme.colors.error else FinTheme.colors.primaryGreenDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }

        // Message Bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                FinTheme.colors.primaryGreen
            } else if (message.isError) {
                Color(0xFFFEF2F2)
            } else {
                FinTheme.colors.surfaceWhite
            },
            shadowElevation = if (isUser) 1.dp else 2.dp,
            border = if (!isUser && !message.isError) {
                androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle)
            } else if (message.isError) {
                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
            } else null,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = FinTheme.typography.bodyMedium,
                    color = if (isUser) {
                        FinTheme.colors.textInverse
                    } else if (message.isError) {
                        Color(0xFF991B1B)
                    } else {
                        FinTheme.colors.textPrimary
                    },
                    lineHeight = 20.sp
                )

                // Retry button for errors
                if (message.isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Retry Query", fontSize = 12.sp)
                    }
                }

                // Citations list
                if (message.citations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SOURCES & CITATIONS",
                        style = FinTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FinTheme.colors.textMuted,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    message.citations.forEach { citation ->
                        CitationItem(citation = citation)
                    }
                }

                // Grounded status tag
                if (!isUser && !message.isError && message.isGrounded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FinTheme.colors.primaryGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Grounded in official scheme rules & records",
                            style = FinTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = FinTheme.colors.primaryGreenDark
                        )
                    }
                }

                // Timestamp
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.time,
                    style = FinTheme.typography.bodySmall,
                    color = if (isUser) FinTheme.colors.textInverse.copy(alpha = 0.7f) else FinTheme.colors.textMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // Scheme Recommendation Cards
        if (message.schemes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recommended Schemes:",
                    style = FinTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                message.schemes.forEach { scheme ->
                    SchemeCardItem(
                        scheme = scheme,
                        onClick = { onSchemeClick(scheme.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CitationItem(citation: ChatCitationDto) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = FinTheme.colors.primaryGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                val title = citation.schemeName ?: citation.documentName ?: "Official Guideline"
                val sourceDetail = buildString {
                    citation.source?.let { append(it) }
                    citation.page?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Page $it")
                    }
                }
                Text(
                    text = title,
                    style = FinTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinTheme.colors.textPrimary,
                    fontSize = 11.sp
                )
                if (sourceDetail.isNotBlank()) {
                    Text(
                        text = sourceDetail,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeCardItem(
    scheme: SchemeDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = FinTheme.shapes.md,
        colors = CardDefaults.cardColors(containerColor = FinTheme.colors.surfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.primaryGreenBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheme.displayTitle,
                    style = FinTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FinTheme.colors.primaryGreenDark
                )
                if (scheme.displayDescription.isNotBlank()) {
                    Text(
                        text = scheme.displayDescription,
                        style = FinTheme.typography.bodySmall,
                        color = FinTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!scheme.tags.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = scheme.tags.joinToString(" • "),
                        style = FinTheme.typography.labelSmall,
                        color = FinTheme.colors.textMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "View", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FinTheme.colors.surfaceWhite,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.borderSubtle),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = FinTheme.colors.primaryGreen
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "FIN Assistant is analyzing records...",
                style = FinTheme.typography.bodySmall,
                color = FinTheme.colors.textMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SuggestionChipsRow(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            Surface(
                color = FinTheme.colors.surfaceWhite,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinTheme.colors.primaryGreenBorder),
                modifier = Modifier.clickable { onSuggestionClick(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    style = FinTheme.typography.bodySmall,
                    color = FinTheme.colors.primaryGreenDark,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ContextualQuickNavRow(
    onNavigateToDocuments: () -> Unit,
    onNavigateToApplications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToDocuments)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Document Vault",
                    style = FinTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = FinTheme.colors.textPrimary
                )
            }
        }

        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onNavigateToApplications)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = FinTheme.colors.primaryGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "My Applications",
                    style = FinTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = FinTheme.colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = FinTheme.colors.surfaceWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Ask about schemes, documents, criteria...",
                        style = FinTheme.typography.bodyMedium,
                        color = FinTheme.colors.textMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinTheme.colors.primaryGreen,
                    unfocusedBorderColor = FinTheme.colors.borderSubtle
                ),
                maxLines = 3,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) FinTheme.colors.primaryGreen
                        else FinTheme.colors.borderSubtle
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = FinTheme.colors.textInverse,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatHistorySheet(
    history: List<com.fin.mobile.data.remote.dto.ChatHistorySummaryDto>,
    isLoading: Boolean,
    activeId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Previous Conversations",
                style = FinTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FinTheme.colors.textPrimary
            )
            Button(
                onClick = onNew,
                colors = ButtonDefaults.buttonColors(containerColor = FinTheme.colors.primaryGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "New Chat", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FinTheme.colors.primaryGreen)
            }
        } else if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No saved chat history on file.",
                    style = FinTheme.typography.bodyMedium,
                    color = FinTheme.colors.textMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val isActive = item.id == activeId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.id) },
                        shape = FinTheme.shapes.sm,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) FinTheme.colors.primaryGreenLight else FinTheme.colors.surfaceWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isActive) FinTheme.colors.primaryGreen else FinTheme.colors.borderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title ?: "Conversation",
                                    style = FinTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FinTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.lastMessage.isNullOrBlank()) {
                                    Text(
                                        text = item.lastMessage,
                                        style = FinTheme.typography.bodySmall,
                                        color = FinTheme.colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Conversation",
                                    tint = FinTheme.colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
