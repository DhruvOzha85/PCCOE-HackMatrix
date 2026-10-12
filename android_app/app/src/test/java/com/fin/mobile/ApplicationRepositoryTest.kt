package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.ApplicationRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApplicationRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ApplicationRepositoryImpl

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

        repository = ApplicationRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testListApplicationsSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "count": 2,
                "data": [
                    {
                        "id": "app-001",
                        "applicant_id": "user-123",
                        "applicant_name": "Ramesh Patel",
                        "scheme_id": "scheme-pm-kisan",
                        "scheme_name": "PM Kisan Samman Nidhi",
                        "status": "under_review",
                        "estimated_benefit": 6000.0,
                        "benefit_display": "₹6,000 / year",
                        "category": "Agriculture",
                        "submitted_at": "2026-03-01T10:00:00Z",
                        "submitted_documents": [
                            {
                                "id": "doc-01",
                                "name": "Aadhaar Card",
                                "document_type": "aadhaar",
                                "verification_status": "VERIFIED",
                                "verified": true
                            }
                        ]
                    },
                    {
                        "id": "app-002",
                        "applicant_id": "user-123",
                        "scheme_id": "scheme-mudra",
                        "scheme_name": "PM MUDRA Yojana",
                        "status": "approved",
                        "benefit_display": "Up to ₹10,00,000 (Loan)",
                        "submitted_at": "2026-02-15T09:00:00Z"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.listApplications()
        assertTrue(result is NetworkResult.Success)

        val apps = (result as NetworkResult.Success).data
        assertEquals(2, apps.size)
        assertEquals("app-001", apps[0].id)
        assertEquals("PM Kisan Samman Nidhi", apps[0].displayTitle)
        assertEquals("Under Review", apps[0].displayStatus)
        assertEquals("₹6,000 / year", apps[0].displayBenefit)
        assertEquals(1, apps[0].submittedDocuments?.size)
        assertEquals("VERIFIED", apps[0].submittedDocuments?.get(0)?.displayStatus)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/applications", recorded.path)
        assertEquals("GET", recorded.method)
    }

    @Test
    fun testGetApplicationByIdSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "id": "app-detail-99",
                    "applicant_id": "user-123",
                    "applicant_name": "Sunita Sharma",
                    "scheme_id": "scheme-standup",
                    "scheme_name": "Stand-Up India",
                    "status": "action_required",
                    "remarks": "Please upload updated ITR return for FY 2025-26",
                    "estimated_benefit": 2500000.0,
                    "submitted_at": "2026-03-05T14:30:00Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getApplicationById("app-detail-99")
        assertTrue(result is NetworkResult.Success)

        val app = (result as NetworkResult.Success).data
        assertEquals("app-detail-99", app.id)
        assertEquals("Action Required", app.displayStatus)
        assertEquals("Please upload updated ITR return for FY 2025-26", app.remarks)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/applications/app-detail-99", recorded.path)
        assertEquals("GET", recorded.method)
    }

    @Test
    fun testGetApplicationByIdNotFound() = runBlocking {
        val json = """
            {
                "success": false,
                "message": "Application not found"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getApplicationById("non-existent-app")
        assertTrue(result is NetworkResult.Error)
        assertEquals(404, (result as NetworkResult.Error).code)
    }

    @Test
    fun testSubmitApplicationSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Application submitted successfully",
                "data": {
                    "id": "app-new-123",
                    "scheme_id": "scheme-kisan",
                    "scheme_name": "PM Kisan Samman Nidhi",
                    "status": "under_review",
                    "submitted_at": "2026-03-10T12:00:00Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.submitApplication("scheme-kisan", "PM Kisan Samman Nidhi")
        assertTrue(result is NetworkResult.Success)

        val app = (result as NetworkResult.Success).data
        assertEquals("app-new-123", app.id)
        assertEquals("Under Review", app.displayStatus)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/applications", recorded.path)
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"schemeId\":\"scheme-kisan\""))
    }

    @Test
    fun testSubmitApplicationDuplicateRejection() = runBlocking {
        val json = """
            {
                "success": false,
                "message": "An application for this scheme has already been submitted for your profile."
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.submitApplication("scheme-kisan", "PM Kisan Samman Nidhi")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(409, error.code)
        assertTrue(error.message.contains("already been submitted"))
    }

    @Test
    fun testAnalyzeApplicationSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "source": "intelligence",
                    "degraded": false,
                    "schemeId": "scheme-kisan",
                    "schemeName": "PM Kisan Samman Nidhi",
                    "readinessScore": 95,
                    "readinessLabel": "Ready",
                    "eligibility": {
                        "verdict": "ELIGIBLE",
                        "reason": "Meets land holding and income criteria"
                    },
                    "documents": {
                        "required": 2,
                        "uploaded": 2,
                        "missing": 0,
                        "details": [
                            {
                                "type": "aadhaar",
                                "label": "Aadhaar Card",
                                "uploaded": true,
                                "status": "uploaded"
                            },
                            {
                                "type": "land",
                                "label": "Land Ownership Records",
                                "uploaded": true,
                                "status": "uploaded"
                            }
                        ]
                    },
                    "profile": {
                        "completionPercent": 90
                    },
                    "nextActions": [
                        {
                            "priority": "low",
                            "action": "Ready to Apply",
                            "detail": "Application validated. Ready for official submission."
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

        val result = repository.analyzeApplication("scheme-kisan")
        assertTrue(result is NetworkResult.Success)

        val readiness = (result as NetworkResult.Success).data
        assertEquals("scheme-kisan", readiness.schemeId)
        assertEquals(95, readiness.readinessScore)
        assertEquals("ELIGIBLE", readiness.eligibility?.verdict)
        assertEquals(2, readiness.documents?.uploaded)
        assertEquals(0, readiness.documents?.missing)
        assertEquals(1, readiness.nextActions?.size)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/applications/analyze", recorded.path)
        assertEquals("POST", recorded.method)
    }
}
