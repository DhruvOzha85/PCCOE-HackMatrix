package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DashboardDataDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface DashboardRepository {
    suspend fun getDashboardData(): NetworkResult<DashboardDataDto>
}

class DashboardRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : DashboardRepository {

    override suspend fun getDashboardData(): NetworkResult<DashboardDataDto> {
        val result = safeApiCall(moshi) { apiService.getDashboard() }
        return when (result) {
            is NetworkResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.Error(500, "Dashboard data unavailable")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
