package com.fin.mobile

import com.fin.mobile.data.local.SessionStorage
import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeSessionStorage: FakeSessionStorage
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        fakeSessionStorage = FakeSessionStorage()
        val config = NetworkConfig(
            baseUrl = mockWebServer.url("/").toString(),
            isDebug = false
        )
        val apiService = NetworkModule.createApiService(fakeSessionStorage, config)
        val moshi = NetworkModule.createMoshi()

        repository = AuthRepositoryImpl(
            apiService = apiService,
            sessionStorage = fakeSessionStorage,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testLoginSuccessSavesSessionData() = runBlocking {
        val responseBody = """
            {
                "success": true,
                "message": "Login successful",
                "access_token": "jwt_token_abc123",
                "refresh_token": "refresh_token_xyz789",
                "expires_in": 3600,
                "user": {
                    "id": "user-uuid-1",
                    "email": "citizen@fin.gov.in",
                    "role": "user"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val result = repository.login("citizen@fin.gov.in", "password123", "citizen")

        assertTrue(result is NetworkResult.Success)
        assertEquals("jwt_token_abc123", fakeSessionStorage.getAccessToken())
        assertEquals("refresh_token_xyz789", fakeSessionStorage.getRefreshToken())
        assertEquals("user-uuid-1", fakeSessionStorage.getUserId())
        assertEquals("user", fakeSessionStorage.getUserRole())
        assertTrue(fakeSessionStorage.hasActiveSession())
    }

    @Test
    fun testLoginFailureReturnsBackendErrorMessage() = runBlocking {
        val errorBody = """
            {
                "success": false,
                "message": "Invalid credentials"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody(errorBody))

        val result = repository.login("unknown@fin.gov.in", "wrongpass", "citizen")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(401, error.code)
        assertEquals("Invalid credentials", error.message)
        assertFalse(fakeSessionStorage.hasActiveSession())
    }

    @Test
    fun testRegisterSuccessSavesSessionData() = runBlocking {
        val responseBody = """
            {
                "success": true,
                "message": "Account created successfully",
                "access_token": "jwt_reg_token",
                "refresh_token": "refresh_reg_token",
                "expires_in": 3600,
                "user": {
                    "id": "new-user-123",
                    "email": "rajesh@fin.gov.in",
                    "role": "user"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(responseBody))

        val result = repository.register("rajesh@fin.gov.in", "pass123", "Rajesh Sharma", "9876543210")

        assertTrue(result is NetworkResult.Success)
        assertEquals("jwt_reg_token", fakeSessionStorage.getAccessToken())
        assertEquals("new-user-123", fakeSessionStorage.getUserId())
    }

    @Test
    fun testVerifyOrRestoreSessionSucceedsWithValidToken() = runBlocking {
        fakeSessionStorage.saveSession("active_token", "active_refresh", 3600, "user-1", "user")

        val meResponse = """
            {
                "success": true,
                "user": {
                    "id": "user-1",
                    "email": "citizen@fin.gov.in",
                    "role": "user"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(meResponse))

        val result = repository.verifyOrRestoreSession()

        assertTrue(result is NetworkResult.Success)
        val user = (result as NetworkResult.Success).data
        assertEquals("citizen@fin.gov.in", user.email)
    }

    @Test
    fun testVerifySessionExpiredTokenRefreshesSuccessfully() = runBlocking {
        fakeSessionStorage.saveSession("expired_token", "valid_refresh_token", 3600, "user-1", "user")

        // First call to /api/users/me returns 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"success\":false,\"message\":\"Invalid or expired token\"}"))

        // Second call to /api/users/refresh returns 200 with new tokens
        val refreshResponse = """
            {
                "success": true,
                "message": "Token refreshed successfully",
                "access_token": "new_refreshed_access_token",
                "refresh_token": "new_refreshed_refresh_token",
                "user": {
                    "id": "user-1",
                    "email": "citizen@fin.gov.in",
                    "role": "user"
                }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(refreshResponse))

        val result = repository.verifyOrRestoreSession()

        assertTrue(result is NetworkResult.Success)
        assertEquals("new_refreshed_access_token", fakeSessionStorage.getAccessToken())
        assertEquals("new_refreshed_refresh_token", fakeSessionStorage.getRefreshToken())
    }

    @Test
    fun testLogoutClearsSessionLocally() = runBlocking {
        fakeSessionStorage.saveSession("jwt_token", "refresh_token", 3600, "user-1", "user")

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"success\":true,\"message\":\"Logged out successfully\"}"))

        val result = repository.logout()

        assertTrue(result is NetworkResult.Success)
        assertFalse(fakeSessionStorage.hasActiveSession())
        assertNull(fakeSessionStorage.getAccessToken())
        assertNull(fakeSessionStorage.getRefreshToken())
    }
}

class FakeSessionStorage : SessionStorage {
    private var token: String? = null
    private var refreshTokenValue: String? = null
    private var expiresAtValue: Long? = null
    private var userIdValue: String? = null
    private var userRoleValue: String? = null

    override fun saveSession(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long?,
        userId: String?,
        userRole: String?
    ) {
        this.token = accessToken
        this.refreshTokenValue = refreshToken
        this.expiresAtValue = expiresInSeconds
        this.userIdValue = userId
        this.userRoleValue = userRole
    }

    override fun getAccessToken(): String? = token
    override fun getRefreshToken(): String? = refreshTokenValue
    override fun getUserId(): String? = userIdValue
    override fun getUserRole(): String? = userRoleValue
    override fun hasActiveSession(): Boolean = !token.isNullOrBlank()
    override fun clearSession() {
        token = null
        refreshTokenValue = null
        expiresAtValue = null
        userIdValue = null
        userRoleValue = null
    }
}
