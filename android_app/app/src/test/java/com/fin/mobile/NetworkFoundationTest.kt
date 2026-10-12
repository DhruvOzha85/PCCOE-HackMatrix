package com.fin.mobile

import com.fin.mobile.data.local.SessionTokenProvider
import com.fin.mobile.data.remote.AuthInterceptor
import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.safeApiCall
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NetworkFoundationTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testAuthInterceptorAttachesTokenWhenAvailable() {
        val testToken = "sample_secure_jwt_token_12345"
        val tokenProvider = object : SessionTokenProvider {
            override fun getAccessToken(): String = testToken
            override fun getRefreshToken(): String? = null
        }

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"success\":true}"))

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/users/me"))
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer $testToken", recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun testAuthInterceptorOmitsHeaderWhenNoToken() {
        val tokenProvider = object : SessionTokenProvider {
            override fun getAccessToken(): String? = null
            override fun getRefreshToken(): String? = null
        }

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"success\":true}"))

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        val request = Request.Builder()
            .url(mockWebServer.url("/api/schemes"))
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun testSafeApiCallParsesBackendErrorMessage() = runBlocking {
        val moshi = NetworkModule.createMoshi()
        val errorJson = "{\"success\":false,\"message\":\"Account with this email does not exist\"}"
        val response = Response.error<String>(
            404,
            errorJson.toResponseBody("application/json".toMediaType())
        )

        val result = safeApiCall(moshi) { response }

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(404, error.code)
        assertEquals("Account with this email does not exist", error.message)
    }

    @Test
    fun testNetworkConfigDefaultBaseUrl() {
        val config = NetworkConfig(baseUrl = "http://10.0.2.2:5000")
        val sanitized = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"
        assertEquals("http://10.0.2.2:5000/", sanitized)
    }
}
