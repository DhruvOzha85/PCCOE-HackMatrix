package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class SchemeDto(
    @Json(name = "id") val rawId: String? = null,
    @Json(name = "scheme_id") val schemeIdField: String? = null,
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
    @Json(name = "financial_benefit") val financialBenefit: Map<String, Any?>? = null,
    @Json(name = "target_audience") val targetAudience: String? = null,
    @Json(name = "target_beneficiaries") val targetBeneficiaries: List<String>? = null,
    @Json(name = "documents_required") val documentsRequired: List<String>? = null,
    @Json(name = "required_documents") val requiredDocuments: List<String>? = null,
    @Json(name = "eligibility") val eligibility: String? = null,
    @Json(name = "eligibility_criteria") val eligibilityCriteria: Map<String, Any?>? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "source_url") val sourceUrl: String? = null,
    @Json(name = "application_mode") val applicationMode: String? = null,
    @Json(name = "application_process") val applicationProcessRaw: Any? = null,
    @Json(name = "dbt_scheme") val dbtScheme: Boolean? = null,
    @Json(name = "scheme_open_date") val schemeOpenDate: String? = null,
    @Json(name = "scheme_close_date") val schemeCloseDate: String? = null,
    @Json(name = "is_eligible") val isEligible: Boolean? = null,
    @Json(name = "eligibility_status") val eligibilityStatus: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "match_score") val matchScore: Double? = null,
    @Json(name = "match_percentage") val matchPercentage: Double? = null
) {
    val id: String
        get() = if (!rawId.isNullOrBlank()) rawId else (schemeIdField ?: slug ?: "")

    constructor(
        id: String,
        slug: String? = null,
        title: String? = null,
        name: String? = null,
        schemeName: String? = null,
        shortTitle: String? = null,
        briefDescription: String? = null,
        description: String? = null,
        detailedDescription: String? = null,
        category: String? = null,
        categories: List<String>? = null,
        ministry: String? = null,
        department: String? = null,
        state: String? = null,
        level: String? = null,
        benefit: String? = null,
        benefits: String? = null,
        maxBenefit: Double? = null,
        financialBenefit: Map<String, Any?>? = null,
        targetAudience: String? = null,
        targetBeneficiaries: List<String>? = null,
        documentsRequired: List<String>? = null,
        requiredDocuments: List<String>? = null,
        eligibility: String? = null,
        eligibilityCriteria: Map<String, Any?>? = null,
        tags: List<String>? = null,
        sourceUrl: String? = null,
        applicationMode: String? = null,
        applicationProcessRaw: Any? = null,
        dbtScheme: Boolean? = null,
        schemeOpenDate: String? = null,
        schemeCloseDate: String? = null,
        isEligible: Boolean? = null,
        eligibilityStatus: String? = null,
        score: Double? = null,
        matchScore: Double? = null,
        matchPercentage: Double? = null
    ) : this(
        rawId = id,
        schemeIdField = null,
        slug = slug,
        title = title,
        name = name,
        schemeName = schemeName,
        shortTitle = shortTitle,
        briefDescription = briefDescription,
        description = description,
        detailedDescription = detailedDescription,
        category = category,
        categories = categories,
        ministry = ministry,
        department = department,
        state = state,
        level = level,
        benefit = benefit,
        benefits = benefits,
        maxBenefit = maxBenefit,
        financialBenefit = financialBenefit,
        targetAudience = targetAudience,
        targetBeneficiaries = targetBeneficiaries,
        documentsRequired = documentsRequired,
        requiredDocuments = requiredDocuments,
        eligibility = eligibility,
        eligibilityCriteria = eligibilityCriteria,
        tags = tags,
        sourceUrl = sourceUrl,
        applicationMode = applicationMode,
        applicationProcessRaw = applicationProcessRaw,
        dbtScheme = dbtScheme,
        schemeOpenDate = schemeOpenDate,
        schemeCloseDate = schemeCloseDate,
        isEligible = isEligible,
        eligibilityStatus = eligibilityStatus,
        score = score,
        matchScore = matchScore,
        matchPercentage = matchPercentage
    )

    val applicationProcess: List<String>
        get() = when (val raw = applicationProcessRaw) {
            is List<*> -> raw.mapNotNull { it?.toString() }
            is String -> if (raw.isBlank()) emptyList() else listOf(raw)
            else -> emptyList()
        }

    val displayApplicationProcess: String
        get() = when (val raw = applicationProcessRaw) {
            is List<*> -> raw.mapNotNull { it?.toString() }.joinToString("\n")
            is String -> raw
            else -> ""
        }

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
            financialBenefit?.get("amountDisplay") != null -> financialBenefit["amountDisplay"].toString()
            financialBenefit?.get("entitlementText") != null -> financialBenefit["entitlementText"].toString()
            maxBenefit != null && maxBenefit > 0 -> "Up to ₹%,.0f".format(maxBenefit)
            else -> "Financial assistance as per statutory norms"
        }
}

data class SchemeDetailResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "source") val source: String? = null,
    @Json(name = "scheme") val scheme: SchemeDto? = null,

    // Fallback in case the scheme properties are provided at the root level (e.g. flat response or unit tests)
    @Json(name = "id") val rootId: String? = null,
    @Json(name = "scheme_id") val rootSchemeId: String? = null,
    @Json(name = "slug") val rootSlug: String? = null,
    @Json(name = "title") val rootTitle: String? = null,
    @Json(name = "name") val rootName: String? = null,
    @Json(name = "scheme_name") val rootSchemeName: String? = null,
    @Json(name = "short_title") val rootShortTitle: String? = null,
    @Json(name = "brief_description") val rootBriefDescription: String? = null,
    @Json(name = "description") val rootDescription: String? = null,
    @Json(name = "detailed_description") val rootDetailedDescription: String? = null,
    @Json(name = "category") val rootCategory: String? = null,
    @Json(name = "categories") val rootCategories: List<String>? = null,
    @Json(name = "ministry") val rootMinistry: String? = null,
    @Json(name = "department") val rootDepartment: String? = null,
    @Json(name = "state") val rootState: String? = null,
    @Json(name = "level") val rootLevel: String? = null,
    @Json(name = "benefit") val rootBenefit: String? = null,
    @Json(name = "benefits") val rootBenefits: String? = null,
    @Json(name = "max_benefit") val rootMaxBenefit: Double? = null,
    @Json(name = "financial_benefit") val rootFinancialBenefit: Map<String, Any?>? = null,
    @Json(name = "target_audience") val rootTargetAudience: String? = null,
    @Json(name = "target_beneficiaries") val rootTargetBeneficiaries: List<String>? = null,
    @Json(name = "documents_required") val rootDocumentsRequired: List<String>? = null,
    @Json(name = "required_documents") val rootRequiredDocuments: List<String>? = null,
    @Json(name = "eligibility") val rootEligibility: String? = null,
    @Json(name = "eligibility_criteria") val rootEligibilityCriteria: Map<String, Any?>? = null,
    @Json(name = "tags") val rootTags: List<String>? = null,
    @Json(name = "source_url") val rootSourceUrl: String? = null,
    @Json(name = "application_mode") val rootApplicationMode: String? = null,
    @Json(name = "application_process") val rootApplicationProcessRaw: Any? = null,
    @Json(name = "dbt_scheme") val rootDbtScheme: Boolean? = null,
    @Json(name = "scheme_open_date") val rootSchemeOpenDate: String? = null,
    @Json(name = "scheme_close_date") val rootSchemeCloseDate: String? = null,
    @Json(name = "is_eligible") val rootIsEligible: Boolean? = null,
    @Json(name = "eligibility_status") val rootEligibilityStatus: String? = null,
    @Json(name = "score") val rootScore: Double? = null,
    @Json(name = "match_score") val rootMatchScore: Double? = null,
    @Json(name = "match_percentage") val rootMatchPercentage: Double? = null
) {
    val resolvedScheme: SchemeDto?
        get() = scheme ?: if (!rootId.isNullOrBlank() || !rootSchemeId.isNullOrBlank() || !rootSlug.isNullOrBlank() || !rootTitle.isNullOrBlank() || !rootSchemeName.isNullOrBlank()) {
            SchemeDto(
                id = rootId ?: rootSchemeId ?: rootSlug ?: "",
                slug = rootSlug,
                title = rootTitle,
                name = rootName,
                schemeName = rootSchemeName,
                shortTitle = rootShortTitle,
                briefDescription = rootBriefDescription,
                description = rootDescription,
                detailedDescription = rootDetailedDescription,
                category = rootCategory,
                categories = rootCategories,
                ministry = rootMinistry,
                department = rootDepartment,
                state = rootState,
                level = rootLevel,
                benefit = rootBenefit,
                benefits = rootBenefits,
                maxBenefit = rootMaxBenefit,
                financialBenefit = rootFinancialBenefit,
                targetAudience = rootTargetAudience,
                targetBeneficiaries = rootTargetBeneficiaries,
                documentsRequired = rootDocumentsRequired,
                requiredDocuments = rootRequiredDocuments,
                eligibility = rootEligibility,
                eligibilityCriteria = rootEligibilityCriteria,
                tags = rootTags,
                sourceUrl = rootSourceUrl,
                applicationMode = rootApplicationMode,
                applicationProcessRaw = rootApplicationProcessRaw,
                dbtScheme = rootDbtScheme,
                schemeOpenDate = rootSchemeOpenDate,
                schemeCloseDate = rootSchemeCloseDate,
                isEligible = rootIsEligible,
                eligibilityStatus = rootEligibilityStatus,
                score = rootScore,
                matchScore = rootMatchScore,
                matchPercentage = rootMatchPercentage
            )
        } else null
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
