package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

/**
 * Data transfer objects for Reviewer workflows matching backend /api/reviewer endpoints.
 */

data class ReviewerDashboardStatsDto(
    @Json(name = "pending_reviews") val pendingReviews: Int = 0,
    @Json(name = "reviewed_today") val reviewedToday: Int = 0,
    @Json(name = "approved") val approved: Int = 0,
    @Json(name = "rejected") val rejected: Int = 0,
    @Json(name = "action_required") val actionRequired: Int = 0,
    @Json(name = "total_applications") val totalApplications: Int = 0
)

data class ReviewerDashboardResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: ReviewerDashboardStatsDto? = null,
    @Json(name = "message") val message: String? = null
)

data class ReviewerQueueResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: List<ApplicationDto> = emptyList(),
    @Json(name = "total") val total: Int = 0,
    @Json(name = "message") val message: String? = null
)

data class ReviewerDocumentOcrDto(
    @Json(name = "extractedFields") val extractedFields: Map<String, Any?>? = null,
    @Json(name = "extractedText") val extractedText: String? = null,
    @Json(name = "confidence") val confidence: Double? = null,
    @Json(name = "method") val method: String? = null,
    @Json(name = "isAutoVerified") val isAutoVerified: Boolean? = null,
    @Json(name = "disclaimer") val disclaimer: String? = null
)

data class ReviewerDocumentDto(
    @Json(name = "id") val id: String,
    @Json(name = "applicationId") val applicationId: String? = null,
    @Json(name = "documentType") val documentType: String? = null,
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "isVerified") val isVerified: Boolean = false,
    @Json(name = "isRejected") val isRejected: Boolean = false,
    @Json(name = "uploadedAt") val uploadedAt: String? = null,
    @Json(name = "ocr") val ocr: ReviewerDocumentOcrDto? = null
)

data class ReviewHistoryItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "applicationId") val applicationId: String? = null,
    @Json(name = "applicantId") val applicantId: String? = null,
    @Json(name = "reviewerId") val reviewerId: String? = null,
    @Json(name = "reviewerName") val reviewerName: String? = null,
    @Json(name = "decision") val decision: String? = null, // "approve", "request_changes", "reject"
    @Json(name = "status") val status: String? = null, // "approved", "action_required", "rejected"
    @Json(name = "remark") val remark: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)

data class ReviewerApplicantUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "role") val role: String? = null
)

data class ReviewerApplicantInfoDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "profile") val profile: UserProfileDto? = null,
    @Json(name = "user") val user: ReviewerApplicantUserDto? = null
)

data class ReviewerApplicationDetailDto(
    @Json(name = "application") val application: ApplicationDto,
    @Json(name = "applicant") val applicant: ReviewerApplicantInfoDto? = null,
    @Json(name = "scheme") val scheme: SchemeDto? = null,
    @Json(name = "documents") val documents: List<ReviewerDocumentDto> = emptyList(),
    @Json(name = "reviewHistory") val reviewHistory: List<ReviewHistoryItemDto> = emptyList()
)

data class ReviewerApplicationDetailResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: ReviewerApplicationDetailDto? = null,
    @Json(name = "message") val message: String? = null
)

data class ReviewerDecisionRequestDto(
    @Json(name = "decision") val decision: String, // "approve", "request_changes", "reject"
    @Json(name = "remark") val remark: String? = null
)

data class ReviewerDecisionResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "message") val message: String? = null,
    @Json(name = "application") val application: ApplicationDto? = null,
    @Json(name = "review") val review: ReviewHistoryItemDto? = null
)

data class ReviewerVerifyDocumentRequestDto(
    @Json(name = "status") val status: String = "VERIFIED" // "VERIFIED" or "REJECTED"
)

data class ReviewerVerifyDocumentResponseDto(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "data") val data: DocumentDto? = null,
    @Json(name = "message") val message: String? = null
)
