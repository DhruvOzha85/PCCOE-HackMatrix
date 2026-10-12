package com.fin.mobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ChatCitationDto
import com.fin.mobile.data.remote.dto.ChatHistorySummaryDto
import com.fin.mobile.data.remote.dto.ChatTurnDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val time: String,
    val citations: List<ChatCitationDto> = emptyList(),
    val schemes: List<SchemeDto> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isGrounded: Boolean = true,
    val isError: Boolean = false
)

data class CitizenChatUiState(
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val conversationId: String? = null,
    val errorMessage: String? = null,
    val defaultSuggestions: List<String> = listOf(
        "Find schemes for my profile",
        "Check my eligibility for PMEGP",
        "Which schemes am I eligible for as a student in Gujarat?",
        "What documents are required for government schemes?",
        "How to apply for a scheme?"
    ),
    val historyList: List<ChatHistorySummaryDto> = emptyList(),
    val isHistoryOpen: Boolean = false,
    val isLoadingHistory: Boolean = false
)

class CitizenChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenChatUiState())
    val uiState: StateFlow<CitizenChatUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    init {
        // Welcome message
        val welcomeMessage = UiChatMessage(
            sender = "assistant",
            text = "Namaste! I am FIN AI Assistant, your guide to central and state government welfare schemes, document vault checks, and eligibility criteria.\n\nHow may I assist you today?",
            time = timeFormat.format(Date()),
            suggestions = _uiState.value.defaultSuggestions
        )
        _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun sendMessage(customText: String? = null) {
        val messageToSend = (customText ?: _uiState.value.inputText).trim()
        if (messageToSend.isBlank() || _uiState.value.isLoading) return

        val userMessage = UiChatMessage(
            sender = "user",
            text = messageToSend,
            time = timeFormat.format(Date())
        )

        // Build history turns for context
        val currentMessages = _uiState.value.messages
        val historyTurns = currentMessages.map {
            ChatTurnDto(
                role = if (it.sender == "user") "user" else "assistant",
                content = it.text
            )
        }

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                message = messageToSend,
                history = historyTurns,
                conversationId = _uiState.value.conversationId,
                language = "en"
            )

            when (result) {
                is NetworkResult.Success -> {
                    val data = result.data
                    val assistantMessage = UiChatMessage(
                        sender = "assistant",
                        text = data.reply ?: "I received your query.",
                        time = timeFormat.format(Date()),
                        citations = data.citations ?: emptyList(),
                        schemes = data.schemes ?: emptyList(),
                        suggestions = data.suggestions ?: emptyList(),
                        isGrounded = data.isGrounded ?: true
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    val errorMsg = result.message ?: "Failed to get response from assistant."
                    val errorMessage = UiChatMessage(
                        sender = "assistant",
                        text = "Unable to process request: $errorMsg\n\nPlease verify your network connection or retry.",
                        time = timeFormat.format(Date()),
                        isError = true
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    val errorMsg = result.throwable.message ?: "Network error. Please check your internet connection."
                    val errorMessage = UiChatMessage(
                        sender = "assistant",
                        text = "Network connection failed: $errorMsg\n\nPlease check your internet connection and try again.",
                        time = timeFormat.format(Date()),
                        isError = true
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMessage,
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun retryLastMessage() {
        val lastUserMessage = _uiState.value.messages.findLast { it.sender == "user" }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.text)
        }
    }

    fun startNewConversation() {
        val welcomeMessage = UiChatMessage(
            sender = "assistant",
            text = "Started a new conversation. What scheme or document question can I help you with?",
            time = timeFormat.format(Date()),
            suggestions = _uiState.value.defaultSuggestions
        )
        _uiState.update {
            it.copy(
                messages = listOf(welcomeMessage),
                inputText = "",
                isLoading = false,
                conversationId = null,
                errorMessage = null,
                isHistoryOpen = false
            )
        }
    }

    fun toggleHistory(open: Boolean) {
        _uiState.update { it.copy(isHistoryOpen = open) }
        if (open) {
            loadHistory()
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            when (val result = chatRepository.getHistory()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(historyList = result.data, isLoadingHistory = false) }
                }
                is NetworkResult.Error, is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoadingHistory = false) }
                }
            }
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isHistoryOpen = false) }
            when (val result = chatRepository.getConversation(id)) {
                is NetworkResult.Success -> {
                    val detail = result.data
                    val uiMessages = detail.messages.map { msg ->
                        UiChatMessage(
                            id = msg.id ?: UUID.randomUUID().toString(),
                            sender = msg.sender ?: "assistant",
                            text = msg.text ?: "",
                            time = msg.time ?: timeFormat.format(Date()),
                            citations = msg.citations ?: emptyList(),
                            schemes = msg.schemes ?: emptyList(),
                            suggestions = emptyList(),
                            isGrounded = true
                        )
                    }
                    _uiState.update {
                        it.copy(
                            messages = if (uiMessages.isNotEmpty()) uiMessages else it.messages,
                            conversationId = detail.id,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error, is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            when (chatRepository.deleteConversation(id)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        state.copy(historyList = state.historyList.filter { it.id != id })
                    }
                    if (_uiState.value.conversationId == id) {
                        startNewConversation()
                    }
                }
                else -> {}
            }
        }
    }

    companion object {
        fun provideFactory(chatRepository: ChatRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CitizenChatViewModel(chatRepository) as T
                }
            }
    }
}
