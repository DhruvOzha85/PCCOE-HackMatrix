package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class SubmittedDocumentDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "document_type") val documentType: String? = null,
    @Json(name = "file_name") val fileName: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "verification_status") val verificationStatus: String? = null,
    @Json(name = "verified") val verified: Boolean = false,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "uploaded_at") val uploadedAt: String? = null
) {
    val displayStatus: String
        get() = (verificationStatus ?: if (verified) "VERIFIED" else "PENDING").uppercase()
}

data class ApplicationDto(
    @Json(name = "id") val id: String,
    @Json(name = "applicant_id") val applicantId: String? = null,
    @Json(name = "applicant_name") val applicantName: String? = null,
    @Json(name = "scheme_id") val schemeId: String? = null,
    @Json(name = "scheme_name") val schemeName: String? = null,
    @Json(name = "scheme_title") val schemeTitle: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "estimated_benefit") val estimatedBenefit: Double? = null,
    @Json(name = "benefit_display") val benefitDisplay: String? = null,
    @Json(name = "benefit_subtitle") val benefitSubtitle: String? = null,
    @Json(name = "benefit_numeric") val benefitNumeric: Double? = null,
    @Json(name = "benefit_type") val benefitType: String? = null,
    @Json(name = "dbt_scheme") val dbtScheme: Boolean? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "submitted_documents") val submittedDocuments: List<SubmittedDocumentDto>? = null,
    @Json(name = "required_documents") val requiredDocuments: List<String>? = null,
    @Json(name = "remarks") val remarks: String? = null,
    @Json(name = "rejection_reason") val rejectionReason: String? = null,
    @Json(name = "decision_notes") val decisionNotes: String? = null,
    @Json(name = "llm_explanation") val llmExplanation: String? = null,
    @Json(name = "application_number") val applicationNumber: String? = null,
    @Json(name = "applied_at") val appliedAt: String? = null,
    @Json(name = "submitted_at") val submittedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "last_updated") val lastUpdated: String? = null
) {
    val displayTitle: String
        get() = schemeName ?: schemeTitle ?: decisionNotes ?: "Scheme Application"

    val normalizedStatus: String
        get() = (status ?: "under_review").lowercase()

    val displayStatus: String
        get() = when (normalizedStatus) {
            "approved", "sanctioned" -> "Approved"
            "action_required" -> "Action Required"
            "rejected" -> "Rejected"
            "draft" -> "Draft"
            else -> "Under Review"
        }

    val referenceId: String
        get() = "FIN" + id.replace("-", "").take(8).uppercase()

    val displayDate: String
        get() = submittedAt ?: createdAt ?: lastUpdated ?: ""

    val displayBenefit: String
        get() = when {
            !benefitDisplay.isNullOrBlank() && benefitDisplay != "Not specified" -> benefitDisplay
            estimatedBenefit != null && estimatedBenefit > 0 -> "₹%,.0f".format(estimatedBenefit)
            benefitNumeric != null && benefitNumeric > 0 -> "₹%,.0f".format(benefitNumeric)
            else -> "Benefit Under Assessment"
        }
}

data class CreateApplicationRequestDto(
    @Json(name = "schemeId") val schemeId: String,
    @Json(name = "schemeName") val schemeName: String? = null,
    @Json(name = "status") val status: String? = "under_review"
)

data class ApplicationListResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<ApplicationDto> = emptyList()
)

data class ApplicationDetailResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: ApplicationDto? = null
)

data class AnalyzeApplicationRequestDto(
    @Json(name = "schemeId") val schemeId: String,
    @Json(name = "profile") val profile: Map<String, Any?>? = null
)

data class ReadinessEligibilityDto(
    @Json(name = "verdict") val verdict: String? = null,
    @Json(name = "reason") val reason: String? = null
)

data class ReadinessDocDetailDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "uploaded") val uploaded: Boolean = false,
    @Json(name = "status") val status: String? = null
)

data class ReadinessDocumentsDto(
    @Json(name = "required") val required: Int = 0,
    @Json(name = "uploaded") val uploaded: Int = 0,
    @Json(name = "missing") val missing: Int = 0,
    @Json(name = "details") val details: List<ReadinessDocDetailDto>? = null
)

data class ReadinessProfileDto(
    @Json(name = "completionPercent") val completionPercent: Int = 0
)

data class ReadinessNextActionDto(
    @Json(name = "priority") val priority: String? = null,
    @Json(name = "action") val action: String? = null,
    @Json(name = "detail") val detail: String? = null
)

data class ApplicationReadinessDto(
    @Json(name = "source") val source: String? = null,
    @Json(name = "degraded") val degraded: Boolean = false,
    @Json(name = "schemeId") val schemeId: String? = null,
    @Json(name = "schemeName") val schemeName: String? = null,
    @Json(name = "readinessScore") val readinessScore: Int = 0,
    @Json(name = "readinessLabel") val readinessLabel: String? = null,
    @Json(name = "eligibility") val eligibility: ReadinessEligibilityDto? = null,
    @Json(name = "documents") val documents: ReadinessDocumentsDto? = null,
    @Json(name = "profile") val profile: ReadinessProfileDto? = null,
    @Json(name = "nextActions") val nextActions: List<ReadinessNextActionDto>? = null,
    @Json(name = "analyzedAt") val analyzedAt: String? = null
)

data class AnalyzeApplicationResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: ApplicationReadinessDto? = null
)
