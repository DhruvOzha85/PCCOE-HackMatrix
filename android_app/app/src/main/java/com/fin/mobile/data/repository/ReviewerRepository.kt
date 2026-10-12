package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.ReviewerApplicationDetailDto
import com.fin.mobile.data.remote.dto.ReviewerDashboardStatsDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionRequestDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionResponseDto
import com.fin.mobile.data.remote.dto.ReviewerVerifyDocumentRequestDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface ReviewerRepository {
    suspend fun getDashboardStats(): NetworkResult<ReviewerDashboardStatsDto>

    suspend fun getReviewQueue(
        status: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): NetworkResult<List<ApplicationDto>>

    suspend fun getApplicationDetails(applicationId: String): NetworkResult<ReviewerApplicationDetailDto>

    suspend fun submitDecision(
        applicationId: String,
        decision: String,
        remark: String?
    ): NetworkResult<ReviewerDecisionResponseDto>

    suspend fun verifyDocument(
        documentId: String,
        status: String = "VERIFIED"
    ): NetworkResult<DocumentDto>
}

class ReviewerRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : ReviewerRepository {

    override suspend fun getDashboardStats(): NetworkResult<ReviewerDashboardStatsDto> {
        val result = safeApiCall(moshi) { apiService.getReviewerDashboard() }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                if (envelope.success && data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(500, envelope.message ?: "Failed to retrieve dashboard stats")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getReviewQueue(
        status: String?,
        page: Int?,
        limit: Int?
    ): NetworkResult<List<ApplicationDto>> {
        val result = safeApiCall(moshi) {
            apiService.getReviewerQueue(status = status, page = page, limit = limit)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                if (envelope.success) {
                    NetworkResult.Success(envelope.data)
                } else {
                    NetworkResult.Error(400, envelope.message ?: "Failed to load review queue")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getApplicationDetails(applicationId: String): NetworkResult<ReviewerApplicationDetailDto> {
        val result = safeApiCall(moshi) { apiService.getReviewerApplicationDetails(applicationId) }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val data = envelope.data
                if (envelope.success && data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(404, envelope.message ?: "Application review details not found")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun submitDecision(
        applicationId: String,
        decision: String,
        remark: String?
    ): NetworkResult<ReviewerDecisionResponseDto> {
        val req = ReviewerDecisionRequestDto(decision = decision, remark = remark)
        val result = safeApiCall(moshi) { apiService.submitReviewDecision(applicationId, req) }
        return when (result) {
            is NetworkResult.Success -> {
                val response = result.data
                if (response.success) {
                    NetworkResult.Success(response)
                } else {
                    NetworkResult.Error(400, response.message ?: "Failed to submit review decision")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun verifyDocument(
        documentId: String,
        status: String
    ): NetworkResult<DocumentDto> {
        val req = ReviewerVerifyDocumentRequestDto(status = status)
        val result = safeApiCall(moshi) { apiService.verifyDocumentReviewer(documentId, req) }
        return when (result) {
            is NetworkResult.Success -> {
                val envelope = result.data
                val doc = envelope.data
                if (envelope.success && doc != null) {
                    NetworkResult.Success(doc)
                } else {
                    NetworkResult.Error(400, envelope.message ?: "Failed to verify document")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
