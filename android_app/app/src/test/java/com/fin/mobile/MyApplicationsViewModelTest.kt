package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ApplicationReadinessDto
import com.fin.mobile.data.repository.ApplicationRepository
import com.fin.mobile.ui.applications.MyApplicationsViewModel
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeApplicationRepository : ApplicationRepository {
    var listResult: NetworkResult<List<ApplicationDto>> = NetworkResult.Success(emptyList())
    var getResult: NetworkResult<ApplicationDto> = NetworkResult.Success(ApplicationDto(id = "app-1"))
    var submitResult: NetworkResult<ApplicationDto> = NetworkResult.Success(ApplicationDto(id = "app-1"))
    var analyzeResult: NetworkResult<ApplicationReadinessDto> = NetworkResult.Success(ApplicationReadinessDto())

    var lastSubmittedSchemeId: String? = null

    override suspend fun listApplications(): NetworkResult<List<ApplicationDto>> = listResult

    override suspend fun getApplicationById(id: String): NetworkResult<ApplicationDto> = getResult

    override suspend fun submitApplication(schemeId: String, schemeName: String?): NetworkResult<ApplicationDto> {
        lastSubmittedSchemeId = schemeId
        return submitResult
    }

    override suspend fun analyzeApplication(schemeId: String, profile: Map<String, Any?>?): NetworkResult<ApplicationReadinessDto> =
        analyzeResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class MyApplicationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeApplicationRepository
    private lateinit var viewModel: MyApplicationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeApplicationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadSuccessAndMetrics() = runTest {
        val sampleApps = listOf(
            ApplicationDto(id = "app-1", schemeName = "PM Kisan", status = "under_review"),
            ApplicationDto(id = "app-2", schemeName = "Stand Up India", status = "action_required"),
            ApplicationDto(id = "app-3", schemeName = "MUDRA", status = "approved"),
            ApplicationDto(id = "app-4", schemeName = "Scholarship", status = "rejected")
        )
        fakeRepository.listResult = NetworkResult.Success(sampleApps)

        viewModel = MyApplicationsViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.applications.size)
        assertEquals(4, state.filteredApplications.size)
        assertEquals(4, state.counts.total)
        assertEquals(1, state.counts.underReview)
        assertEquals(1, state.counts.actionRequired)
        assertEquals(1, state.counts.approved)
        assertEquals(1, state.counts.rejected)
        assertNull(state.errorMessage)
    }

    @Test
    fun testInitialLoadError() = runTest {
        fakeRepository.listResult = NetworkResult.Error(500, "Database offline")

        viewModel = MyApplicationsViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.applications.isEmpty())
        assertEquals("Database offline", state.errorMessage)
    }

    @Test
    fun testStatusFilterActionRequired() = runTest {
        val sampleApps = listOf(
            ApplicationDto(id = "app-1", schemeName = "PM Kisan", status = "under_review"),
            ApplicationDto(id = "app-2", schemeName = "Stand Up India", status = "action_required"),
            ApplicationDto(id = "app-3", schemeName = "MUDRA", status = "approved")
        )
        fakeRepository.listResult = NetworkResult.Success(sampleApps)

        viewModel = MyApplicationsViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onStatusFilterChanged("action_required")
        val state = viewModel.uiState.value
        assertEquals("action_required", state.selectedStatus)
        assertEquals(1, state.filteredApplications.size)
        assertEquals("app-2", state.filteredApplications[0].id)
    }

    @Test
    fun testSearchQueryFilter() = runTest {
        val sampleApps = listOf(
            ApplicationDto(id = "app-1", schemeName = "PM Kisan Samman Nidhi", status = "under_review"),
            ApplicationDto(id = "app-2", schemeName = "Stand-Up India Scheme", status = "approved")
        )
        fakeRepository.listResult = NetworkResult.Success(sampleApps)

        viewModel = MyApplicationsViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Kisan")
        val state = viewModel.uiState.value
        assertEquals(1, state.filteredApplications.size)
        assertEquals("app-1", state.filteredApplications[0].id)

        viewModel.onSearchQueryChanged("NonExistent")
        assertEquals(0, viewModel.uiState.value.filteredApplications.size)
    }
}
