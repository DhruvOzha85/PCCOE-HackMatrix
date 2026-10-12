package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.SchemeRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SchemeRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: SchemeRepositoryImpl

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

        repository = SchemeRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testSearchSchemes() = runBlocking {
        val json = """
            {
                "success": true,
                "total": 1,
                "offset": 0,
                "limit": 50,
                "schemes": [
                    {
                        "id": "pm-kisan",
                        "title": "PM-KISAN Samman Nidhi",
                        "ministry": "Ministry of Agriculture",
                        "brief_description": "Income support for landholding farmers",
                        "max_benefit": 6000.0,
                        "dbt_scheme": true,
                        "tags": ["Agriculture", "Farmer"]
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

        val result = repository.searchSchemes(query = "kisan", category = "agriculture")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(1, data.total)
        assertEquals(1, data.schemes.size)
        val scheme = data.schemes[0]
        assertEquals("pm-kisan", scheme.id)
        assertEquals("PM-KISAN Samman Nidhi", scheme.displayTitle)
        assertEquals("Ministry of Agriculture", scheme.displayMinistry)
        assertEquals(6000.0, scheme.maxBenefit)
        assertEquals(true, scheme.dbtScheme)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/schemes/search", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"query\":\"kisan\""))
        assertTrue(body.contains("\"category\":\"agriculture\""))
    }

    @Test
    fun testGetSchemeById() = runBlocking {
        val json = """
            {
                "success": true,
                "id": "pm-kisan",
                "slug": "pm-kisan",
                "title": "PM-KISAN Samman Nidhi",
                "brief_description": "Direct income support for farmers.",
                "detailed_description": "Central sector scheme providing ₹6,000 per year in 3 equal installments.",
                "benefits": "₹6,000 per year directly transferred to bank account.",
                "eligibility": "All landholding farmer families with cultivable land.",
                "documents_required": ["Aadhaar Card", "Land Ownership Documents", "Bank Passbook"],
                "ministry": "Ministry of Agriculture & Farmers Welfare",
                "department": "Department of Agriculture and Farmers Welfare",
                "level": "Central",
                "state": "All India",
                "max_benefit": 6000.0,
                "source_url": "https://pmkisan.gov.in",
                "dbt_scheme": true
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getSchemeById("pm-kisan")

        assertTrue(result is NetworkResult.Success)
        val scheme = (result as NetworkResult.Success).data
        assertEquals("pm-kisan", scheme.id)
        assertEquals("PM-KISAN Samman Nidhi", scheme.displayTitle)
        assertEquals(3, scheme.displayDocuments.size)
        assertEquals("Aadhaar Card", scheme.displayDocuments[0])
        assertEquals("Central Scheme", scheme.displayJurisdiction)
        assertEquals("https://pmkisan.gov.in", scheme.sourceUrl)
    }

    @Test
    fun testGetSchemeFaqs() = runBlocking {
        val json = """
            {
                "success": true,
                "scheme_slug": "pm-kisan",
                "total": 2,
                "faqs": [
                    {
                        "id": "faq-1",
                        "faq_number": 1,
                        "question": "Who is eligible for PM-KISAN?",
                        "answer": "All landholder farmer families with cultivable landholding."
                    },
                    {
                        "id": "faq-2",
                        "faq_number": 2,
                        "question": "How is the benefit paid?",
                        "answer": "Through Direct Benefit Transfer into Aadhaar-seeded bank accounts."
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

        val result = repository.getSchemeFaqs("pm-kisan")

        assertTrue(result is NetworkResult.Success)
        val faqs = (result as NetworkResult.Success).data
        assertEquals(2, faqs.size)
        assertEquals("Who is eligible for PM-KISAN?", faqs[0].question)
        assertEquals("Through Direct Benefit Transfer into Aadhaar-seeded bank accounts.", faqs[1].answer)
    }

    @Test
    fun testCheckEligibilityPreservesStatusesAndRules() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "source": "deterministic_engine",
                    "schemeId": "pm-kisan",
                    "schemeName": "pm-kisan",
                    "status": "PASS",
                    "isEligible": true,
                    "verdict": "ELIGIBLE",
                    "verdictReason": "All statutory criteria satisfied.",
                    "rules": [
                        {
                            "ruleId": "LAND_OWNERSHIP",
                            "description": "Applicant must own cultivable agricultural land.",
                            "source": "PM-KISAN Guidelines, Clause 2.1",
                            "status": "pass",
                            "reason": "Applicant owns cultivable land."
                        },
                        {
                            "ruleId": "INCOME_CRITERIA",
                            "description": "Income threshold evaluation.",
                            "source": "Statutory Protocol 4.1",
                            "status": "pass",
                            "reason": "Verified under threshold."
                        }
                    ],
                    "missingFields": [],
                    "missingFieldLabels": [],
                    "matchedRules": ["LAND_OWNERSHIP", "INCOME_CRITERIA"],
                    "failedRules": [],
                    "evaluatedAt": "2026-10-11T12:00:00Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.checkEligibility("pm-kisan")

        assertTrue(result is NetworkResult.Success)
        val evaluation = (result as NetworkResult.Success).data
        assertEquals("pm-kisan", evaluation.schemeId)
        assertEquals("PASS", evaluation.status)
        assertEquals(true, evaluation.isEligible)
        assertEquals("ELIGIBLE", evaluation.verdict)
        assertEquals(2, evaluation.rules.size)
        assertEquals("LAND_OWNERSHIP", evaluation.rules[0].ruleId)
        assertEquals("pass", evaluation.rules[0].status)
        assertEquals("PM-KISAN Guidelines, Clause 2.1", evaluation.rules[0].source)
    }

    @Test
    fun testCheckEligibilityReviewRequiredState() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "source": "intelligence_service",
                    "schemeId": "mudra-shishu",
                    "status": "REVIEW",
                    "isEligible": null,
                    "verdict": "MANUAL_REVIEW",
                    "verdictReason": "Missing verified income and business proof documents.",
                    "rules": [
                        {
                            "ruleId": "BUSINESS_REGISTRATION",
                            "description": "Proof of enterprise activity.",
                            "source": "MUDRA Operational Guidelines Clause 3",
                            "status": "review",
                            "reason": "Document not uploaded to vault."
                        }
                    ],
                    "missingFields": ["annual_income", "business_proof"],
                    "missingFieldLabels": ["Annual Family Income", "Business Registration Proof"],
                    "matchedRules": [],
                    "failedRules": [],
                    "evaluatedAt": "2026-10-11T12:00:00Z"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.checkEligibility("mudra-shishu")

        assertTrue(result is NetworkResult.Success)
        val evaluation = (result as NetworkResult.Success).data
        assertEquals("REVIEW", evaluation.status)
        assertEquals("MANUAL_REVIEW", evaluation.verdict)
        assertEquals(2, evaluation.missingFieldLabels.size)
        assertEquals("Annual Family Income", evaluation.missingFieldLabels[0])
        assertEquals("review", evaluation.rules[0].status)
    }
}
