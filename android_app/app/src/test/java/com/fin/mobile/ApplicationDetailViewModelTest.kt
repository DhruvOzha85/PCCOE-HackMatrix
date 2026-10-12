package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ApplicationReadinessDto
import com.fin.mobile.data.repository.ApplicationRepository
import com.fin.mobile.ui.applications.ApplicationDetailViewModel
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

private class FakeDetailApplicationRepository : ApplicationRepository {
    var getResult: NetworkResult<ApplicationDto> = NetworkResult.Success(ApplicationDto(id = "app-test"))

    override suspend fun listApplications(): NetworkResult<List<ApplicationDto>> =
        NetworkResult.Success(emptyList())

    override suspend fun getApplicationById(id: String): NetworkResult<ApplicationDto> = getResult

    override suspend fun submitApplication(schemeId: String, schemeName: String?): NetworkResult<ApplicationDto> =
        NetworkResult.Success(ApplicationDto(id = "app-test"))

    override suspend fun analyzeApplication(schemeId: String, profile: Map<String, Any?>?): NetworkResult<ApplicationReadinessDto> =
        NetworkResult.Success(ApplicationReadinessDto())
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDetailApplicationRepository
    private lateinit var viewModel: ApplicationDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDetailApplicationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadApplicationSuccessAndReceiptGeneration() = runTest {
        val app = ApplicationDto(
            id = "app-uuid-99",
            applicantName = "Ramesh Patel",
            schemeName = "PM Kisan",
            status = "approved",
            estimatedBenefit = 6000.0,
            submittedAt = "2026-03-01T10:00:00Z"
        )
        fakeRepository.getResult = NetworkResult.Success(app)

        viewModel = ApplicationDetailViewModel("app-uuid-99", fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.application)
        assertEquals("app-uuid-99", state.application?.id)
        assertEquals("Approved", state.application?.displayStatus)
        assertTrue(state.receiptText!!.contains("OFFICIAL SCHEME APPLICATION ACKNOWLEDGEMENT"))
        assertTrue(state.receiptText!!.contains("Ramesh Patel"))
        assertTrue(state.receiptText!!.contains("PM Kisan"))
    }

    @Test
    fun testLoadApplicationError() = runTest {
        fakeRepository.getResult = NetworkResult.Error(404, "Application record not found")

        viewModel = ApplicationDetailViewModel("missing-app", fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.application)
        assertEquals("Application record not found", state.errorMessage)
    }
}
