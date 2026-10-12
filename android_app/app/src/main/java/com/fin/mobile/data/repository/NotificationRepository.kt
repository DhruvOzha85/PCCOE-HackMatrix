package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.NotificationListResponseDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi

interface NotificationRepository {
    suspend fun getNotifications(): NetworkResult<NotificationListResponseDto>
    suspend fun markNotificationRead(id: String): NetworkResult<String>
    suspend fun markAllNotificationsRead(): NetworkResult<String>
}

class NotificationRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : NotificationRepository {

    override suspend fun getNotifications(): NetworkResult<NotificationListResponseDto> {
        return safeApiCall(moshi) { apiService.getNotifications() }
    }

    override suspend fun markNotificationRead(id: String): NetworkResult<String> {
        val result = safeApiCall(moshi) { apiService.markNotificationRead(id) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.message ?: "Notification marked as read")
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun markAllNotificationsRead(): NetworkResult<String> {
        val result = safeApiCall(moshi) { apiService.markAllNotificationsRead() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.message ?: "All notifications marked as read")
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
