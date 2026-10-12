package com.fin.mobile.data.remote

import com.fin.mobile.data.local.SessionTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that attaches the Bearer session token to outgoing HTTP requests
 * if an active session is available via [SessionTokenProvider].
 *
 * Does not mock credentials, forge logins, or embed hardcoded tokens.
 */
class AuthInterceptor(
    private val tokenProvider: SessionTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "FIN-Android/1.0")

        val token = tokenProvider.getAccessToken()
        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
