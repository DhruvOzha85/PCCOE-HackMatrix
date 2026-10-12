package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class EligibilityCheckRequestDto(
    @Json(name = "schemeId") val schemeId: String,
    @Json(name = "profile") val profile: Map<String, Any?>? = null
)

data class EligibilityRuleDto(
    @Json(name = "ruleId") val ruleId: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "status") val status: String = "review", // "pass", "fail", "review"
    @Json(name = "reason") val reason: String? = null
)

data class EligibilityEvaluationDto(
    @Json(name = "source") val source: String? = null,
    @Json(name = "degraded") val degraded: Boolean? = null,
    @Json(name = "schemeId") val schemeId: String,
    @Json(name = "schemeName") val schemeName: String? = null,
    @Json(name = "status") val status: String = "UNKNOWN", // PASS, FAIL, UNKNOWN, REVIEW
    @Json(name = "isEligible") val isEligible: Boolean? = null,
    @Json(name = "verdict") val verdict: String = "MANUAL_REVIEW", // ELIGIBLE, NOT_ELIGIBLE, MANUAL_REVIEW
    @Json(name = "verdictReason") val verdictReason: String? = null,
    @Json(name = "rules") val rules: List<EligibilityRuleDto> = emptyList(),
    @Json(name = "missingFields") val missingFields: List<String> = emptyList(),
    @Json(name = "missingFieldLabels") val missingFieldLabels: List<String> = emptyList(),
    @Json(name = "matchedRules") val matchedRules: List<String> = emptyList(),
    @Json(name = "failedRules") val failedRules: List<String> = emptyList(),
    @Json(name = "evaluatedAt") val evaluatedAt: String? = null
)

data class EligibilityResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: EligibilityEvaluationDto? = null,
    @Json(name = "message") val message: String? = null
)
