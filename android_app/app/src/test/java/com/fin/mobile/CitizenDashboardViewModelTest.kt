package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DashboardDataDto
import com.fin.mobile.data.remote.dto.DashboardMetricDto
import com.fin.mobile.data.remote.dto.DashboardUserDto
import com.fin.mobile.data.remote.dto.TopOpportunityDto
import com.fin.mobile.data.repository.DashboardRepository
import com.fin.mobile.ui.dashboard.CitizenDashboardUiState
import com.fin.mobile.ui.dashboard.CitizenDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeDashboardRepository : DashboardRepository {
    var result: NetworkResult<DashboardDataDto> = NetworkResult.Success(
        DashboardDataDto(
            user = DashboardUserDto(fullName = "Citizen Tester", profileCompleted = 90),
            metrics = listOf(
                DashboardMetricDto(
                    key = "schemes",
                    label = "Relevant Schemes",
                    value = "10"
                )
            ),
            topOpportunities = listOf(
                TopOpportunityDto(
                    schemeId = "test-scheme",
                    schemeName = "Test Scheme",
                    eligibilityStatus = "PASS",
                    isEligible = true
                )
            )
        )
    )

    override suspend fun getDashboardData(): NetworkResult<DashboardDataDto> {
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CitizenDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDashboardRepository
    private lateinit var viewModel: CitizenDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDashboardRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadSuccess() = runTest {
        viewModel = CitizenDashboardViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CitizenDashboardUiState.Success)
        val success = state as CitizenDashboardUiState.Success
        assertEquals("Citizen Tester", success.data.user?.fullName)
        assertEquals(1, success.data.metrics.size)
        assertEquals("test-scheme", success.data.topOpportunities[0].schemeId)
    }

    @Test
    fun testInitialLoadError() = runTest {
        fakeRepository.result = NetworkResult.Error(code = 500, message = "Server internal error")
        viewModel = CitizenDashboardViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CitizenDashboardUiState.Error)
        val error = state as CitizenDashboardUiState.Error
        assertEquals("Server internal error", error.message)
    }

    @Test
    fun testRefresh() = runTest {
        viewModel = CitizenDashboardViewModel(fakeRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CitizenDashboardUiState.Success)

        // Change fake repository data and trigger refresh
        fakeRepository.result = NetworkResult.Success(
            DashboardDataDto(
                user = DashboardUserDto(fullName = "Updated Citizen", profileCompleted = 100),
                metrics = emptyList(),
                topOpportunities = emptyList()
            )
        )

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CitizenDashboardUiState.Success)
        val success = state as CitizenDashboardUiState.Success
        assertEquals("Updated Citizen", success.data.user?.fullName)
        assertEquals(100, success.data.user?.profileCompleted)
    }
}
