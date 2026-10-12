package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class DashboardResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: DashboardDataDto? = null
)

data class DashboardDataDto(
    @Json(name = "user") val user: DashboardUserDto? = null,
    @Json(name = "metrics") val metrics: List<DashboardMetricDto> = emptyList(),
    @Json(name = "topOpportunities") val topOpportunities: List<TopOpportunityDto> = emptyList()
)

data class DashboardUserDto(
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "profileCompleted") val profileCompleted: Int? = null
)

data class DashboardMetricDto(
    @Json(name = "key") val key: String,
    @Json(name = "label") val label: String,
    @Json(name = "value") val value: String,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "isWarning") val isWarning: Boolean = false
)

data class TopOpportunityEligibilityDto(
    @Json(name = "isEligible") val isEligible: Boolean? = null,
    @Json(name = "status") val status: String = "UNKNOWN",
    @Json(name = "reasons") val reasons: List<String> = emptyList()
)

data class TopOpportunityDto(
    @Json(name = "schemeId") val schemeId: String,
    @Json(name = "schemeName") val schemeName: String? = null,
    @Json(name = "ministry") val ministry: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "eligibilityStatus") val eligibilityStatus: String = "UNKNOWN",
    @Json(name = "isEligible") val isEligible: Boolean? = null,
    @Json(name = "benefitAmount") val benefitAmount: Double? = null,
    @Json(name = "benefitDisplay") val benefitDisplay: String? = null,
    @Json(name = "isLoan") val isLoan: Boolean = false,
    @Json(name = "eligibility") val eligibility: TopOpportunityEligibilityDto? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "deadline") val deadline: String? = null,
    @Json(name = "scheme_open_date") val schemeOpenDate: String? = null,
    @Json(name = "scheme_close_date") val schemeCloseDate: String? = null,
    @Json(name = "applicationLink") val applicationLink: String? = null
)
