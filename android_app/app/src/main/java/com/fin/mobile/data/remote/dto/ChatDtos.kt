package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer objects for Citizen AI Assistant matching backend /api/chat contracts.
 */

data class ChatTurnDto(
    @Json(name = "role") val role: String, // "user" or "assistant"
    @Json(name = "content") val content: String
)

data class ChatCitationDto(
    @Json(name = "schemeName") val schemeName: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "documentName") val documentName: String? = null,
    @Json(name = "page") val page: Int? = null
)

data class ChatRequestDto(
    @Json(name = "message") val message: String,
    @Json(name = "history") val history: List<ChatTurnDto>? = null,
    @Json(name = "conversationId") val conversationId: String? = null,
    @Json(name = "language") val language: String? = null
)

data class ChatResponseDataDto(
    @Json(name = "reply") val reply: String? = null,
    @Json(name = "citations") val citations: List<ChatCitationDto>? = null,
    @Json(name = "schemes") val schemes: List<SchemeDto>? = null,
    @Json(name = "suggestions") val suggestions: List<String>? = null,
    @Json(name = "isGrounded") val isGrounded: Boolean? = null,
    @Json(name = "schemeId") val schemeId: String? = null,
    @Json(name = "showViewAll") val showViewAll: Boolean? = null
)

data class ChatResponseEnvelopeDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: ChatResponseDataDto? = null,
    @Json(name = "message") val message: String? = null
)

data class ChatHistorySummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "messageCount") val messageCount: Int? = null,
    @Json(name = "lastMessage") val lastMessage: String? = null
)

data class ChatHistoryListResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: List<ChatHistorySummaryDto>? = null,
    @Json(name = "message") val message: String? = null
)

data class ChatMessageItemDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "sender") val sender: String? = null, // "user" or "assistant"
    @Json(name = "text") val text: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "schemes") val schemes: List<SchemeDto>? = null,
    @Json(name = "citations") val citations: List<ChatCitationDto>? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

data class ChatConversationDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "messages") val messages: List<ChatMessageItemDto> = emptyList()
)

data class ChatConversationDetailResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: ChatConversationDetailDto? = null,
    @Json(name = "message") val message: String? = null
)

data class ChatDeleteResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null
)
