package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.EligibilityCheckRequestDto
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.data.remote.dto.SchemeSearchRequestDto
import com.fin.mobile.data.remote.dto.SchemeSearchResponseDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface SchemeRepository {
    suspend fun searchSchemes(
        query: String = "",
        category: String? = null,
        state: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): NetworkResult<SchemeSearchResponseDto>

    suspend fun getSchemeById(id: String): NetworkResult<SchemeDto>

    suspend fun getSchemeFaqs(slug: String): NetworkResult<List<SchemeFaqDto>>

    suspend fun getSchemeStats(): NetworkResult<Map<String, Int>>

    suspend fun getRecommendedSchemes(): NetworkResult<List<SchemeDto>>

    suspend fun checkEligibility(
        schemeId: String,
        profileOverride: Map<String, Any?>? = null
    ): NetworkResult<EligibilityEvaluationDto>
}

class SchemeRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : SchemeRepository {

    override suspend fun searchSchemes(
        query: String,
        category: String?,
        state: String?,
        limit: Int,
        offset: Int
    ): NetworkResult<SchemeSearchResponseDto> {
        val filters = mutableMapOf<String, Any?>()
        if (!category.isNullOrBlank() && !category.equals("all", ignoreCase = true)) {
            filters["category"] = category
        }
        if (!state.isNullOrBlank()) {
            filters["state"] = state
        }

        val request = SchemeSearchRequestDto(
            query = query.trim(),
            filters = filters,
            limit = limit,
            offset = offset
        )
        return safeApiCall(moshi) { apiService.searchSchemes(request) }
    }

    override suspend fun getSchemeById(id: String): NetworkResult<SchemeDto> {
        val result = safeApiCall(moshi) { apiService.getSchemeById(id) }
        return when (result) {
            is NetworkResult.Success -> {
                val scheme = result.data.resolvedScheme
                if (scheme != null) {
                    NetworkResult.Success(scheme)
                } else {
                    NetworkResult.Error(404, "Scheme details not found")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getSchemeFaqs(slug: String): NetworkResult<List<SchemeFaqDto>> {
        val result = safeApiCall(moshi) { apiService.getSchemeFaqs(slug) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.faqs)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getSchemeStats(): NetworkResult<Map<String, Int>> {
        val result = safeApiCall(moshi) { apiService.getSchemeStats() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.categoryCounts)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getRecommendedSchemes(): NetworkResult<List<SchemeDto>> {
        val result = safeApiCall(moshi) { apiService.getRecommendedSchemes() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.recommendations)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun checkEligibility(
        schemeId: String,
        profileOverride: Map<String, Any?>?
    ): NetworkResult<EligibilityEvaluationDto> {
        val request = EligibilityCheckRequestDto(
            schemeId = schemeId,
            profile = profileOverride
        )
        val result = safeApiCall(moshi) { apiService.checkEligibility(request) }
        return when (result) {
            is NetworkResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(500, result.data.message ?: "Eligibility evaluation result missing")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
