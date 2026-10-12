package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.ReviewerRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewerRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ReviewerRepositoryImpl

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

        repository = ReviewerRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetDashboardStatsSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "pending_reviews": 7,
                    "reviewed_today": 3,
                    "approved": 12,
                    "rejected": 2,
                    "action_required": 4,
                    "total_applications": 25
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getDashboardStats()
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val stats = (result as NetworkResult.Success).data
        assertEquals(7, stats.pendingReviews)
        assertEquals(3, stats.reviewedToday)
        assertEquals(12, stats.approved)
        assertEquals(2, stats.rejected)
        assertEquals(4, stats.actionRequired)
        assertEquals(25, stats.totalApplications)
    }

    @Test
    fun testGetReviewQueueSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": [
                    {
                        "id": "app-001",
                        "applicant_id": "usr-1",
                        "applicant_name": "Ramesh Patel",
                        "scheme_id": "pmegp",
                        "scheme_name": "PMEGP Scheme",
                        "status": "under_review",
                        "application_number": "APP-PMEGP-001"
                    }
                ],
                "total": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getReviewQueue(status = "under_review")
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val apps = (result as NetworkResult.Success).data
        assertEquals(1, apps.size)
        assertEquals("app-001", apps[0].id)
        assertEquals("Ramesh Patel", apps[0].applicantName)
    }

    @Test
    fun testGetApplicationDetailsSuccessWithOcrAndHistory() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "application": {
                        "id": "app-001",
                        "applicant_name": "Ramesh Patel",
                        "scheme_name": "PMEGP",
                        "status": "under_review"
                    },
                    "applicant": {
                        "fullName": "Ramesh Patel",
                        "email": "ramesh@example.com",
                        "phone": "9876543210"
                    },
                    "documents": [
                        {
                            "id": "doc-001",
                            "documentType": "income_certificate",
                            "fileName": "income_cert.pdf",
                            "verificationStatus": "PENDING",
                            "isVerified": false,
                            "ocr": {
                                "extractedFields": { "annual_income": "120000" },
                                "confidence": 0.95,
                                "method": "OCR_ANALYSIS"
                            }
                        }
                    ],
                    "reviewHistory": [
                        {
                            "id": "rev-1",
                            "applicationId": "app-001",
                            "reviewerName": "Officer Sharma",
                            "decision": "request_changes",
                            "remark": "Please upload valid income certificate."
                        }
                    ]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getApplicationDetails("app-001")
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val details = (result as NetworkResult.Success).data
        assertEquals("app-001", details.application.id)
        assertEquals("Ramesh Patel", details.applicant?.fullName)
        assertEquals(1, details.documents.size)
        assertEquals("PENDING", details.documents[0].verificationStatus)
        assertEquals(0.95, details.documents[0].ocr?.confidence)
        assertEquals(1, details.reviewHistory.size)
    }

    @Test
    fun testSubmitDecisionApproveSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Application successfully marked as approved",
                "application": {
                    "id": "app-001",
                    "status": "approved"
                },
                "review": {
                    "id": "rev-99",
                    "applicationId": "app-001",
                    "decision": "approve",
                    "status": "approved"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.submitDecision("app-001", "approve", "Approved by departmental committee")
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val res = (result as NetworkResult.Success).data
        assertTrue(res.success)
        assertEquals("approved", res.application?.status)
    }

    @Test
    fun testSubmitDecisionSelfReviewPreventionHttp403() = runBlocking {
        val json = """
            {
                "success": false,
                "message": "Conflict of interest: You are not authorized to review or approve your own application."
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(403).setBody(json))

        val result = repository.submitDecision("app-self", "approve", null)
        assertTrue("Expected Error result", result is NetworkResult.Error)
        val err = result as NetworkResult.Error
        assertEquals(403, err.code)
        assertTrue(err.message?.contains("Conflict of interest") == true)
    }

    @Test
    fun testVerifyDocumentSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "id": "doc-001",
                    "document_type": "income_certificate",
                    "file_name": "income.pdf",
                    "verification_status": "VERIFIED"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.verifyDocument("doc-001", "VERIFIED")
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val doc = (result as NetworkResult.Success).data
        assertEquals("doc-001", doc.id)
        assertEquals("VERIFIED", doc.displayStatus)
    }
}
