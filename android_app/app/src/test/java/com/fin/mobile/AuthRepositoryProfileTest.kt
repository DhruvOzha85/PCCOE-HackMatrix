package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryProfileTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeSessionStorage: FakeSessionStorage
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        fakeSessionStorage = FakeSessionStorage()
        fakeSessionStorage.saveSession("active_token", "active_refresh", 3600, "user_123", "citizen")

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
    fun testGetProfileSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "id": "user_123",
                    "full_name": "Ramesh Kumar",
                    "phone": "9812345678",
                    "gender": "male",
                    "dob": "1990-05-15",
                    "state": "Maharashtra",
                    "district": "Pune",
                    "occupation": "farmer",
                    "annual_income": 120000.0,
                    "caste_category": "obc",
                    "applicant_type": "Individual"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getProfile()

        assertTrue(result is NetworkResult.Success)
        val profile = (result as NetworkResult.Success).data
        assertEquals("Ramesh Kumar", profile.fullName)
        assertEquals("Pune", profile.district)
        assertEquals("farmer", profile.occupation)
        assertEquals(120000.0, profile.annualIncome)
    }

    @Test
    fun testUpdateProfileSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Profile updated successfully",
                "data": {
                    "id": "user_123",
                    "full_name": "Ramesh Kumar",
                    "phone": "9812345678",
                    "state": "Maharashtra",
                    "district": "Nagpur",
                    "occupation": "msme",
                    "annual_income": 350000.0
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val updates = mapOf<String, Any?>(
            "district" to "Nagpur",
            "occupation" to "msme",
            "annual_income" to 350000.0
        )
        val result = repository.updateProfile(updates)

        assertTrue(result is NetworkResult.Success)
        val updated = (result as NetworkResult.Success).data
        assertEquals("Nagpur", updated.district)
        assertEquals("msme", updated.occupation)
        assertEquals(350000.0, updated.annualIncome)
    }

    @Test
    fun testChangePasswordSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Password changed successfully"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.changePassword("oldPass123", "newPass456")

        assertTrue(result is NetworkResult.Success)
        assertEquals("Password changed successfully", (result as NetworkResult.Success).data)
    }

    @Test
    fun testChangePasswordFailure() = runBlocking {
        val errorJson = """
            {
                "success": false,
                "message": "Current password does not match"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(errorJson))

        val result = repository.changePassword("wrongPass", "newPass456")

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(400, error.code)
        assertEquals("Current password does not match", error.message)
    }
}
