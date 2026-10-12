package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.OcrMetadataDto
import com.fin.mobile.data.repository.DocumentRepository
import com.fin.mobile.ui.documents.DocumentVaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeDocumentRepository : DocumentRepository {
    var listResult: NetworkResult<List<DocumentDto>> = NetworkResult.Success(emptyList())
    var uploadResult: NetworkResult<DocumentDto> = NetworkResult.Success(
        DocumentDto(
            id = "doc-default-upload",
            documentType = "pan",
            fileName = "pan.jpg"
        )
    )
    var extractResult: NetworkResult<DocumentDto> = NetworkResult.Success(
        DocumentDto(
            id = "doc-default-extract",
            documentType = "pan",
            fileName = "pan.jpg",
            docNumber = "ABCDE1234F"
        )
    )
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    var lastUploadedFileName: String? = null
    var lastExtractedId: String? = null
    var lastDeletedId: String? = null

    override suspend fun listDocuments(): NetworkResult<List<DocumentDto>> = listResult

    override suspend fun getDocumentById(id: String): NetworkResult<DocumentDto> =
        NetworkResult.Success(DocumentDto(id = id, documentType = "aadhaar"))

    override suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        applicationId: String?
    ): NetworkResult<DocumentDto> {
        lastUploadedFileName = fileName
        return uploadResult
    }

    override suspend fun extractDocument(documentId: String): NetworkResult<DocumentDto> {
        lastExtractedId = documentId
        return extractResult
    }

    override suspend fun deleteDocument(documentId: String): NetworkResult<Unit> {
        lastDeletedId = documentId
        return deleteResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentVaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDocumentRepository
    private lateinit var viewModel: DocumentVaultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDocumentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadDocumentsSuccess() = runTest {
        val sampleDocs = listOf(
            DocumentDto(
                id = "doc-1",
                documentType = "aadhaar",
                fileName = "aadhaar.pdf",
                verificationStatus = "VERIFIED",
                docNumber = "1234-5678-9012",
                extractedData = mapOf("name" to "Citizen Tester")
            ),
            DocumentDto(
                id = "doc-2",
                documentType = "pan",
                fileName = "pan.jpg",
                verificationStatus = "PENDING"
            )
        )
        fakeRepository.listResult = NetworkResult.Success(sampleDocs)

        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.documents.size)
        assertEquals("doc-1", state.documents[0].id)
        assertNull(state.errorMessage)
    }

    @Test
    fun testInitialLoadDocumentsError() = runTest {
        fakeRepository.listResult = NetworkResult.Error(500, "Database connection failure")

        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.documents.isEmpty())
        assertEquals("Database connection failure", state.errorMessage)
    }

    @Test
    fun testUploadDocumentSuccessAndAutoExtract() = runTest {
        fakeRepository.listResult = NetworkResult.Success(emptyList())
        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        val uploadedDoc = DocumentDto(
            id = "doc-uploaded-1",
            documentType = "income_cert",
            fileName = "income.pdf",
            verificationStatus = "PENDING"
        )
        val extractedDoc = uploadedDoc.copy(
            ocrMetadata = OcrMetadataDto(confidence = 0.95),
            extractedData = mapOf("annualIncome" to 180000.0)
        )

        fakeRepository.uploadResult = NetworkResult.Success(uploadedDoc)
        fakeRepository.extractResult = NetworkResult.Success(extractedDoc)

        viewModel.uploadDocument(
            fileBytes = "%PDF-test".toByteArray(),
            fileName = "income.pdf",
            mimeType = "application/pdf",
            documentType = "income_cert"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUploading)
        assertEquals("income.pdf", fakeRepository.lastUploadedFileName)
        assertEquals("doc-uploaded-1", fakeRepository.lastExtractedId)
        assertEquals(1, state.documents.size)
        assertEquals(180000.0, state.documents[0].extractedData?.get("annualIncome"))
        assertEquals("Facts extracted via Intelligence OCR", state.actionMessage)
    }

    @Test
    fun testUploadDocumentFailure() = runTest {
        fakeRepository.listResult = NetworkResult.Success(emptyList())
        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.uploadResult = NetworkResult.Error(400, "File corrupted or invalid magic bytes")

        viewModel.uploadDocument(
            fileBytes = "invalid".toByteArray(),
            fileName = "corrupt.pdf",
            mimeType = "application/pdf",
            documentType = "caste_cert"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUploading)
        assertEquals("File corrupted or invalid magic bytes", state.uploadError)
    }

    @Test
    fun testExtractDocumentSuccess() = runTest {
        val initialDoc = DocumentDto(
            id = "doc-ext-test",
            documentType = "pan",
            fileName = "pan.jpg"
        )
        fakeRepository.listResult = NetworkResult.Success(listOf(initialDoc))
        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        val extracted = initialDoc.copy(
            docNumber = "ABCDE1234F",
            extractedData = mapOf("name" to "Test Citizen")
        )
        fakeRepository.extractResult = NetworkResult.Success(extracted)

        viewModel.extractDocument("doc-ext-test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.extractingDocumentId)
        assertEquals("ABCDE1234F", state.documents.first { it.id == "doc-ext-test" }.docNumber)
        assertEquals("Facts extracted via Intelligence OCR", state.actionMessage)
    }

    @Test
    fun testDeleteDocumentSuccess() = runTest {
        val doc1 = DocumentDto(id = "doc-del-1", documentType = "aadhaar")
        val doc2 = DocumentDto(id = "doc-del-2", documentType = "pan")
        fakeRepository.listResult = NetworkResult.Success(listOf(doc1, doc2))
        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.selectDocument(doc1)
        assertEquals("doc-del-1", viewModel.uiState.value.selectedDocument?.id)

        fakeRepository.deleteResult = NetworkResult.Success(Unit)
        viewModel.deleteDocument("doc-del-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.deletingDocumentId)
        assertEquals(1, state.documents.size)
        assertEquals("doc-del-2", state.documents[0].id)
        assertNull(state.selectedDocument)
        assertEquals("Document deleted from vault", state.actionMessage)
    }

    @Test
    fun testClearMessages() = runTest {
        fakeRepository.listResult = NetworkResult.Success(emptyList())
        viewModel = DocumentVaultViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.uploadResult = NetworkResult.Error(400, "Error")
        viewModel.uploadDocument("".toByteArray(), "a.pdf", "application/pdf", "aadhaar")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.uploadError)
        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.uploadError)
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.actionMessage)
    }
}
