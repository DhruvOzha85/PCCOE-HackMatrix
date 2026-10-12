package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ApplicationReadinessDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.ReadinessEligibilityDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.data.remote.dto.SchemeSearchResponseDto
import com.fin.mobile.data.repository.ApplicationRepository
import com.fin.mobile.data.repository.DocumentRepository
import com.fin.mobile.data.repository.SchemeRepository
import com.fin.mobile.ui.applications.ApplySchemeViewModel
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

private class FakeApplyApplicationRepository : ApplicationRepository {
    var submitResult: NetworkResult<ApplicationDto> = NetworkResult.Success(ApplicationDto(id = "app-new"))
    var analyzeResult: NetworkResult<ApplicationReadinessDto> = NetworkResult.Success(
        ApplicationReadinessDto(
            schemeId = "test-scheme",
            readinessScore = 85,
            eligibility = ReadinessEligibilityDto(verdict = "ELIGIBLE")
        )
    )

    override suspend fun listApplications(): NetworkResult<List<ApplicationDto>> = NetworkResult.Success(emptyList())
    override suspend fun getApplicationById(id: String): NetworkResult<ApplicationDto> = NetworkResult.Success(ApplicationDto(id = id))
    override suspend fun submitApplication(schemeId: String, schemeName: String?): NetworkResult<ApplicationDto> = submitResult
    override suspend fun analyzeApplication(schemeId: String, profile: Map<String, Any?>?): NetworkResult<ApplicationReadinessDto> = analyzeResult
}

private class FakeApplySchemeRepository : SchemeRepository {
    var schemeResult: NetworkResult<SchemeDto> = NetworkResult.Success(
        SchemeDto(id = "test-scheme", schemeName = "Test Scheme")
    )

    override suspend fun searchSchemes(
        query: String,
        category: String?,
        state: String?,
        limit: Int,
        offset: Int
    ): NetworkResult<SchemeSearchResponseDto> = NetworkResult.Success(SchemeSearchResponseDto())

    override suspend fun getSchemeById(id: String): NetworkResult<SchemeDto> = schemeResult
    override suspend fun getSchemeFaqs(slug: String): NetworkResult<List<SchemeFaqDto>> = NetworkResult.Success(emptyList())
    override suspend fun getSchemeStats(): NetworkResult<Map<String, Int>> = NetworkResult.Success(emptyMap())
    override suspend fun getRecommendedSchemes(): NetworkResult<List<SchemeDto>> = NetworkResult.Success(emptyList())
    override suspend fun checkEligibility(
        schemeId: String,
        profileOverride: Map<String, Any?>?
    ): NetworkResult<EligibilityEvaluationDto> =
        NetworkResult.Success(EligibilityEvaluationDto(schemeId = schemeId, verdict = "ELIGIBLE"))
}

private class FakeApplyDocumentRepository : DocumentRepository {
    var docsResult: NetworkResult<List<DocumentDto>> = NetworkResult.Success(emptyList())

    override suspend fun listDocuments(): NetworkResult<List<DocumentDto>> = docsResult
    override suspend fun getDocumentById(id: String): NetworkResult<DocumentDto> = NetworkResult.Success(DocumentDto(id = id))
    override suspend fun uploadDocument(fileBytes: ByteArray, fileName: String, mimeType: String, documentType: String, applicationId: String?): NetworkResult<DocumentDto> =
        NetworkResult.Success(DocumentDto(id = "doc-1"))
    override suspend fun extractDocument(documentId: String): NetworkResult<DocumentDto> = NetworkResult.Success(DocumentDto(id = documentId))
    override suspend fun deleteDocument(documentId: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApplySchemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appRepo: FakeApplyApplicationRepository
    private lateinit var schemeRepo: FakeApplySchemeRepository
    private lateinit var docRepo: FakeApplyDocumentRepository
    private lateinit var viewModel: ApplySchemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appRepo = FakeApplyApplicationRepository()
        schemeRepo = FakeApplySchemeRepository()
        docRepo = FakeApplyDocumentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialReadinessLoad() = runTest {
        viewModel = ApplySchemeViewModel("test-scheme", appRepo, schemeRepo, docRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.scheme)
        assertEquals("Test Scheme", state.scheme?.displayTitle)
        assertNotNull(state.readiness)
        assertEquals(85, state.readiness?.readinessScore)
        assertFalse(state.hasConfirmedDeclaration)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun testSubmitWithoutDeclarationShowsError() = runTest {
        viewModel = ApplySchemeViewModel("test-scheme", appRepo, schemeRepo, docRepo)
        advanceUntilIdle()

        viewModel.submitApplication()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.submittedApplication)
        assertTrue(state.errorMessage?.contains("declaration") == true)
    }

    @Test
    fun testSubmitSuccess() = runTest {
        val returnedApp = ApplicationDto(
            id = "app-submitted-100",
            schemeId = "test-scheme",
            status = "under_review"
        )
        appRepo.submitResult = NetworkResult.Success(returnedApp)

        viewModel = ApplySchemeViewModel("test-scheme", appRepo, schemeRepo, docRepo)
        advanceUntilIdle()

        viewModel.onDeclarationToggled(true)
        assertTrue(viewModel.uiState.value.hasConfirmedDeclaration)

        viewModel.submitApplication()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.submittedApplication)
        assertEquals("app-submitted-100", state.submittedApplication?.id)
        assertNull(state.errorMessage)
    }

    @Test
    fun testSubmitDuplicateReturns409() = runTest {
        appRepo.submitResult = NetworkResult.Error(409, "An application for this scheme has already been submitted for your profile.")

        viewModel = ApplySchemeViewModel("test-scheme", appRepo, schemeRepo, docRepo)
        advanceUntilIdle()

        viewModel.onDeclarationToggled(true)
        viewModel.submitApplication()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.isAlreadySubmitted)
        assertTrue(state.errorMessage?.contains("already been submitted") == true)
    }
}
