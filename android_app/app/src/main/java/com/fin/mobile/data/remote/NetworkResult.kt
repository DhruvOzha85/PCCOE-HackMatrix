package com.fin.mobile.data.remote

import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException

/**
 * Typed wrapper representing network execution states and API outcomes.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val message: String, val errorBody: String? = null) : NetworkResult<Nothing>()
    data class Exception(val throwable: Throwable) : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
}

/**
 * Safely executes a suspend Retrofit call, mapping responses and network exceptions
 * into a typed [NetworkResult].
 */
suspend fun <T> safeApiCall(
    moshi: Moshi? = null,
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                @Suppress("UNCHECKED_CAST")
                NetworkResult.Success(Unit as T)
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            val parsedMessage = parseErrorMessage(errorBodyString, moshi) ?: response.message()
            val finalMessage = parsedMessage.ifBlank { "Request failed with status code ${response.code()}" }
            NetworkResult.Error(
                code = response.code(),
                message = finalMessage,
                errorBody = errorBodyString
            )
        }
    } catch (e: IOException) {
        NetworkResult.Exception(e)
    } catch (e: java.lang.Exception) {
        NetworkResult.Exception(e)
    }
}

private fun parseErrorMessage(rawJson: String?, moshi: Moshi?): String? {
    if (rawJson.isNullOrBlank() || moshi == null) return null
    return try {
        val adapter = moshi.adapter(Map::class.java)
        val map = adapter.fromJson(rawJson)
        (map?.get("message") as? String) ?: (map?.get("error") as? String)
    } catch (_: java.lang.Exception) {
        null
    }
}
