package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ChatConversationDetailDto
import com.fin.mobile.data.remote.dto.ChatHistorySummaryDto
import com.fin.mobile.data.remote.dto.ChatRequestDto
import com.fin.mobile.data.remote.dto.ChatResponseDataDto
import com.fin.mobile.data.remote.dto.ChatTurnDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface ChatRepository {
    suspend fun sendMessage(
        message: String,
        history: List<ChatTurnDto> = emptyList(),
        conversationId: String? = null,
        language: String? = null
    ): NetworkResult<ChatResponseDataDto>

    suspend fun getHistory(): NetworkResult<List<ChatHistorySummaryDto>>

    suspend fun getConversation(id: String): NetworkResult<ChatConversationDetailDto>

    suspend fun deleteConversation(id: String): NetworkResult<Unit>
}

class ChatRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : ChatRepository {

    override suspend fun sendMessage(
        message: String,
        history: List<ChatTurnDto>,
        conversationId: String?,
        language: String?
    ): NetworkResult<ChatResponseDataDto> {
        val request = ChatRequestDto(
            message = message,
            history = if (history.isNotEmpty()) history else null,
            conversationId = conversationId,
            language = language
        )
        val result = safeApiCall(moshi) { apiService.sendChatMessage(request) }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                if (envelope.success && data != null) {
                    NetworkResult.Success(data)
                } else if (!envelope.success) {
                    NetworkResult.Error(400, envelope.message ?: "Chat response error")
                } else {
                    NetworkResult.Error(500, "Empty chat response from assistant")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getHistory(): NetworkResult<List<ChatHistorySummaryDto>> {
        val result = safeApiCall(moshi) { apiService.getChatHistory() }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (envelope.success) {
                    NetworkResult.Success(envelope.data ?: emptyList())
                } else {
                    NetworkResult.Error(400, envelope.message ?: "Failed to load chat history")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getConversation(id: String): NetworkResult<ChatConversationDetailDto> {
        val result = safeApiCall(moshi) { apiService.getChatConversation(id) }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                if (envelope.success && data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(404, envelope.message ?: "Conversation not found")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun deleteConversation(id: String): NetworkResult<Unit> {
        val result = safeApiCall(moshi) { apiService.deleteChatConversation(id) }
        return when (result) {
            is NetworkResult.Success -> {
                if (result.data.success) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(400, result.data.message ?: "Failed to delete conversation")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
