package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.DashboardRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: DashboardRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val config = NetworkConfig(
            baseUrl = mockWebServer.url("/").toString(),
            isDebug = false
        )
        val apiService = NetworkModule.createApiService(null, config)
        val moshi = NetworkModule.createMoshi()

        repository = DashboardRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetDashboardSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "user": {
                        "fullName": "Ramesh Patel",
                        "profileCompleted": 75
                    },
                    "metrics": [
                        {
                            "key": "schemes",
                            "label": "Relevant Schemes",
                            "value": "24",
                            "subtitle": "Available in Gujarat jurisdiction",
                            "icon": "scheme",
                            "isWarning": false
                        },
                        {
                            "key": "benefits",
                            "label": "Estimated Benefits",
                            "value": "₹ 6,000",
                            "subtitle": "Confirmed welfare grants",
                            "icon": "rupee",
                            "isWarning": false
                        },
                        {
                            "key": "documents",
                            "label": "Documents Verified",
                            "value": "2 / 3",
                            "subtitle": "1 pending review in vault",
                            "icon": "document",
                            "isWarning": true
                        },
                        {
                            "key": "applications",
                            "label": "Applications",
                            "value": "1",
                            "subtitle": "1 pending review",
                            "icon": "application",
                            "isWarning": false
                        }
                    ],
                    "topOpportunities": [
                        {
                            "schemeId": "pm-kisan",
                            "schemeName": "PM-KISAN Samman Nidhi",
                            "ministry": "Ministry of Agriculture",
                            "description": "Income support of ₹6,000 per year for farmer families.",
                            "eligibilityStatus": "PASS",
                            "isEligible": true,
                            "benefitAmount": 6000.0,
                            "benefitDisplay": "₹ 6,000",
                            "isLoan": false,
                            "tags": ["Agriculture", "Farmer", "Central"],
                            "deadline": "Ongoing",
                            "applicationLink": "https://pmkisan.gov.in"
                        },
                        {
                            "schemeId": "mudra-shishu",
                            "schemeName": "Pradhan Mantri MUDRA Yojana (Shishu)",
                            "ministry": "Ministry of Finance",
                            "description": "Loans up to ₹50,000 for micro enterprises.",
                            "eligibilityStatus": "REVIEW",
                            "isEligible": null,
                            "benefitAmount": 50000.0,
                            "benefitDisplay": "Up to ₹ 50,000",
                            "isLoan": true,
                            "tags": ["MSME", "Business"],
                            "deadline": null,
                            "applicationLink": null
                        }
                    ]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getDashboardData()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("Ramesh Patel", data.user?.fullName)
        assertEquals(75, data.user?.profileCompleted)

        assertEquals(4, data.metrics.size)
        assertEquals("schemes", data.metrics[0].key)
        assertEquals("24", data.metrics[0].value)
        assertFalse(data.metrics[0].isWarning)
        assertTrue(data.metrics[2].isWarning)

        assertEquals(2, data.topOpportunities.size)
        val opp1 = data.topOpportunities[0]
        assertEquals("pm-kisan", opp1.schemeId)
        assertEquals("PASS", opp1.eligibilityStatus)
        assertEquals(true, opp1.isEligible)
        assertEquals("₹ 6,000", opp1.benefitDisplay)
        assertFalse(opp1.isLoan)

        val opp2 = data.topOpportunities[1]
        assertEquals("mudra-shishu", opp2.schemeId)
        assertEquals("REVIEW", opp2.eligibilityStatus)
        assertTrue(opp2.isLoan)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/api/dashboard", recordedRequest.path)
    }

    @Test
    fun testGetDashboardHttpError() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"message":"Database connection error"}""")
        )

        val result = repository.getDashboardData()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(500, error.code)
        assertEquals("Database connection error", error.message)
    }
}
