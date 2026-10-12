package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.AnalyzeApplicationRequestDto
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ApplicationReadinessDto
import com.fin.mobile.data.remote.dto.CreateApplicationRequestDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface ApplicationRepository {
    suspend fun listApplications(): NetworkResult<List<ApplicationDto>>
    suspend fun getApplicationById(id: String): NetworkResult<ApplicationDto>
    suspend fun submitApplication(schemeId: String, schemeName: String? = null): NetworkResult<ApplicationDto>
    suspend fun analyzeApplication(schemeId: String, profile: Map<String, Any?>? = null): NetworkResult<ApplicationReadinessDto>
}

class ApplicationRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : ApplicationRepository {

    override suspend fun listApplications(): NetworkResult<List<ApplicationDto>> {
        val result = safeApiCall(moshi) { apiService.getApplications() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.data)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getApplicationById(id: String): NetworkResult<ApplicationDto> {
        val result = safeApiCall(moshi) { apiService.getApplicationById(id) }
        return when (result) {
            is NetworkResult.Success -> {
                val app = result.data.data
                if (app != null) {
                    NetworkResult.Success(app)
                } else {
                    NetworkResult.Error(404, result.data.message ?: "Application record not found")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun submitApplication(schemeId: String, schemeName: String?): NetworkResult<ApplicationDto> {
        val req = CreateApplicationRequestDto(
            schemeId = schemeId,
            schemeName = schemeName,
            status = "under_review"
        )
        val result = safeApiCall(moshi) { apiService.submitApplication(req) }
        return when (result) {
            is NetworkResult.Success -> {
                val app = result.data.data
                if (app != null) {
                    NetworkResult.Success(app)
                } else {
                    NetworkResult.Error(500, result.data.message ?: "Application submitted without returned record")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun analyzeApplication(schemeId: String, profile: Map<String, Any?>?): NetworkResult<ApplicationReadinessDto> {
        val req = AnalyzeApplicationRequestDto(schemeId = schemeId, profile = profile)
        val result = safeApiCall(moshi) { apiService.analyzeApplication(req) }
        return when (result) {
            is NetworkResult.Success -> {
                val readiness = result.data.data
                if (readiness != null) {
                    NetworkResult.Success(readiness)
                } else {
                    NetworkResult.Error(500, "Readiness analysis returned empty result")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
