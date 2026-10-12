package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class SchemeDto(
    @Json(name = "id") val id: String,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "scheme_name") val schemeName: String? = null,
    @Json(name = "short_title") val shortTitle: String? = null,
    @Json(name = "brief_description") val briefDescription: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "detailed_description") val detailedDescription: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "categories") val categories: List<String>? = null,
    @Json(name = "ministry") val ministry: String? = null,
    @Json(name = "department") val department: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "level") val level: String? = null,
    @Json(name = "benefit") val benefit: String? = null,
    @Json(name = "benefits") val benefits: String? = null,
    @Json(name = "max_benefit") val maxBenefit: Double? = null,
    @Json(name = "target_audience") val targetAudience: String? = null,
    @Json(name = "target_beneficiaries") val targetBeneficiaries: List<String>? = null,
    @Json(name = "documents_required") val documentsRequired: List<String>? = null,
    @Json(name = "required_documents") val requiredDocuments: List<String>? = null,
    @Json(name = "eligibility") val eligibility: String? = null,
    @Json(name = "eligibility_criteria") val eligibilityCriteria: Map<String, Any?>? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "source_url") val sourceUrl: String? = null,
    @Json(name = "application_mode") val applicationMode: String? = null,
    @Json(name = "application_process") val applicationProcess: List<String>? = null,
    @Json(name = "dbt_scheme") val dbtScheme: Boolean? = null,
    @Json(name = "scheme_open_date") val schemeOpenDate: String? = null,
    @Json(name = "scheme_close_date") val schemeCloseDate: String? = null,
    @Json(name = "is_eligible") val isEligible: Boolean? = null,
    @Json(name = "eligibility_status") val eligibilityStatus: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "match_score") val matchScore: Double? = null,
    @Json(name = "match_percentage") val matchPercentage: Double? = null
) {
    val displayTitle: String
        get() = title ?: schemeName ?: name ?: "Government Welfare Scheme"

    val displayMinistry: String
        get() = ministry ?: department ?: "Government of India"

    val displayDescription: String
        get() = briefDescription ?: description ?: detailedDescription ?: ""

    val displayDocuments: List<String>
        get() = documentsRequired ?: requiredDocuments ?: emptyList()

    val displayJurisdiction: String
        get() = if (!state.isNullOrBlank() && state != "All India" && state != "Central") {
            "$state Scheme"
        } else {
            "Central Scheme"
        }

    val displayBenefit: String
        get() = when {
            !benefit.isNullOrBlank() -> benefit!!
            !benefits.isNullOrBlank() -> benefits!!
            maxBenefit != null && maxBenefit > 0 -> "Up to ₹%,.0f".format(maxBenefit)
            else -> "Financial assistance as per statutory norms"
        }
}

data class SchemeFaqDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "scheme_id") val schemeId: String? = null,
    @Json(name = "scheme_slug") val schemeSlug: String? = null,
    @Json(name = "faq_number") val faqNumber: Int? = null,
    @Json(name = "question") val question: String,
    @Json(name = "answer") val answer: String
)

data class SchemeSearchRequestDto(
    @Json(name = "query") val query: String? = "",
    @Json(name = "filters") val filters: Map<String, Any?>? = emptyMap(),
    @Json(name = "limit") val limit: Int = 50,
    @Json(name = "offset") val offset: Int = 0
)

data class SchemeSearchResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "schemes") val schemes: List<SchemeDto> = emptyList(),
    @Json(name = "total") val total: Int = 0,
    @Json(name = "offset") val offset: Int = 0,
    @Json(name = "limit") val limit: Int = 50,
    @Json(name = "source") val source: String? = null,
    @Json(name = "degraded") val degraded: Boolean? = null
)

data class SchemeStatsResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "categoryCounts") val categoryCounts: Map<String, Int> = emptyMap()
)

data class SchemeFaqsResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "scheme_slug") val schemeSlug: String? = null,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "faqs") val faqs: List<SchemeFaqDto> = emptyList()
)

data class SchemeRecommendationResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "recommendations") val recommendations: List<SchemeDto> = emptyList()
)

data class SchemeStatsDto(
    @Json(name = "total") val total: Int? = null,
    @Json(name = "categories") val categories: Map<String, Int>? = null,
    @Json(name = "categoryCounts") val categoryCounts: Map<String, Int>? = null,
    @Json(name = "activeCount") val activeCount: Int? = null
)
