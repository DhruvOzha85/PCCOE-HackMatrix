package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.DocumentRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocumentRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: DocumentRepositoryImpl

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

        repository = DocumentRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testListDocumentsSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "count": 2,
                "data": [
                    {
                        "id": "doc-01",
                        "documentType": "aadhaar",
                        "fileName": "aadhaar.pdf",
                        "fileUrl": "https://storage.supabase.co/aadhaar.pdf",
                        "verificationStatus": "VERIFIED",
                        "docNumber": "XXXX-XXXX-1234",
                        "issuer": "UIDAI",
                        "extractedData": {
                            "name": "Ramesh Patel",
                            "dob": "1985-06-15",
                            "gender": "Male"
                        },
                        "ocrMetadata": {
                            "method": "tesseract",
                            "pages": 1,
                            "confidence": 0.96
                        },
                        "uploadedAt": "2026-03-01T10:00:00Z"
                    },
                    {
                        "id": "doc-02",
                        "documentType": "income_cert",
                        "fileName": "income.png",
                        "verificationStatus": "PENDING",
                        "uploadedAt": "2026-03-02T11:00:00Z"
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

        val result = repository.listDocuments()
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val docs = (result as NetworkResult.Success).data
        assertEquals(2, docs.size)
        assertEquals("doc-01", docs[0].id)
        assertEquals("aadhaar", docs[0].documentType)
        assertEquals("VERIFIED", docs[0].verificationStatus)
        assertEquals("XXXX-XXXX-1234", docs[0].docNumber)
        assertEquals("UIDAI", docs[0].issuer)
        assertEquals("Ramesh Patel", docs[0].extractedData?.get("name"))
        assertEquals(0.96, docs[0].ocrMetadata?.confidence ?: 0.0, 0.001)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/documents", recorded.path)
        assertEquals("GET", recorded.method)
    }

    @Test
    fun testGetDocumentByIdSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "id": "doc-pan-99",
                    "documentType": "pan",
                    "fileName": "pan_card.jpg",
                    "fileUrl": "https://storage.supabase.co/signed/pan_card.jpg?token=abc",
                    "verificationStatus": "PENDING",
                    "docNumber": "ABCDE1234F",
                    "issuer": "Income Tax Department",
                    "extractedData": {
                        "name": "Ramesh Patel"
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.getDocumentById("doc-pan-99")
        assertTrue(result is NetworkResult.Success)

        val doc = (result as NetworkResult.Success).data
        assertEquals("doc-pan-99", doc.id)
        assertEquals("pan", doc.documentType)
        assertEquals("ABCDE1234F", doc.docNumber)
        assertEquals("Ramesh Patel", doc.extractedData?.get("name"))
        assertNotNull(doc.fileUrl)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/documents/doc-pan-99", recorded.path)
        assertEquals("GET", recorded.method)
    }

    @Test
    fun testUploadDocumentClientValidationRejectsExcessiveSize() = runBlocking {
        // Exceed 10MB (10 * 1024 * 1024 + 1 bytes)
        val dummy11Mb = ByteArray(10 * 1024 * 1024 + 1)
        val result = repository.uploadDocument(
            fileBytes = dummy11Mb,
            fileName = "large.pdf",
            mimeType = "application/pdf",
            documentType = "income_cert"
        )

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(400, error.code)
        assertTrue(error.message.contains("exceeds permissible limit of 10 MB"))
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun testUploadDocumentSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Document uploaded and queued for processing",
                "data": {
                    "id": "doc-new-1",
                    "documentType": "caste_cert",
                    "fileName": "caste.pdf",
                    "verificationStatus": "PENDING"
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val smallPdf = "%PDF-1.4 test bytes".toByteArray()
        val result = repository.uploadDocument(
            fileBytes = smallPdf,
            fileName = "caste.pdf",
            mimeType = "application/pdf",
            documentType = "caste_cert"
        )

        assertTrue(result is NetworkResult.Success)
        val doc = (result as NetworkResult.Success).data
        assertEquals("doc-new-1", doc.id)
        assertEquals("caste_cert", doc.documentType)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/documents/process", recorded.path)
        assertEquals("POST", recorded.method)
        assertTrue(recorded.getHeader("Content-Type")?.startsWith("multipart/form-data") == true)
    }

    @Test
    fun testExtractDocumentSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Document OCR processed successfully",
                "data": {
                    "id": "doc-extract-1",
                    "documentType": "aadhaar",
                    "fileName": "aadhaar.pdf",
                    "verificationStatus": "PENDING",
                    "docNumber": "9876-5432-1098",
                    "extractedData": {
                        "name": "Sunita Sharma"
                    },
                    "ocrMetadata": {
                        "confidence": 0.98
                    }
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.extractDocument("doc-extract-1")
        assertTrue(result is NetworkResult.Success)

        val doc = (result as NetworkResult.Success).data
        assertEquals("doc-extract-1", doc.id)
        assertEquals("Sunita Sharma", doc.extractedData?.get("name"))
        assertEquals("9876-5432-1098", doc.docNumber)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/documents/extract", recorded.path)
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"documentId\":\"doc-extract-1\""))
    }

    @Test
    fun testDeleteDocumentSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Document deleted successfully"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.deleteDocument("doc-del-1")
        assertTrue(result is NetworkResult.Success)

        val recorded = mockWebServer.takeRequest()
        assertEquals("/api/documents/doc-del-1", recorded.path)
        assertEquals("DELETE", recorded.method)
    }

    @Test
    fun testDeleteDocumentNotFound() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Document not found or unauthorized"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(json)
        )

        val result = repository.deleteDocument("non-existent-doc")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(404, error.code)
    }
}
