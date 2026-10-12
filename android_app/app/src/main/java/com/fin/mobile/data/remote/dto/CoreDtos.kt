package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json


// --- Application DTOs moved to ApplicationDtos.kt ---

// --- Dashboard DTO ---
data class DashboardDto(
    @Json(name = "profile_completion") val profileCompletion: Int? = null,
    @Json(name = "eligible_schemes_count") val eligibleSchemesCount: Int? = null,
    @Json(name = "active_applications_count") val activeApplicationsCount: Int? = null,
    @Json(name = "recent_applications") val recentApplications: List<ApplicationDto>? = null,
    @Json(name = "recommended_schemes") val recommendedSchemes: List<SchemeDto>? = null
)


// --- Notification DTOs ---
data class NotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "unread") val unread: Boolean? = null,
    @Json(name = "created_at") val createdAtSnake: String? = null,
    @Json(name = "createdAt") val createdAtCamel: String? = null,
    @Json(name = "applicationId") val applicationId: String? = null,
    @Json(name = "time") val time: String? = null,
    @Json(name = "link") val link: String? = null
) {
    val isRead: Boolean
        get() = read || (unread == false)

    val displayCreatedAt: String
        get() = time ?: createdAtCamel ?: createdAtSnake ?: "Recent"
}

data class NotificationListResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: List<NotificationDto> = emptyList(),
    @Json(name = "unreadCount") val unreadCount: Int = 0
)

data class NotificationActionResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null
)


