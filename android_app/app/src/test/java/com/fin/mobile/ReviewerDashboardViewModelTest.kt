package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.ReviewerApplicationDetailDto
import com.fin.mobile.data.remote.dto.ReviewerDashboardStatsDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionResponseDto
import com.fin.mobile.data.repository.ReviewerRepository
import com.fin.mobile.ui.reviewer.ReviewerDashboardViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeReviewerRepository : ReviewerRepository {
    var statsResult: NetworkResult<ReviewerDashboardStatsDto> = NetworkResult.Success(
        ReviewerDashboardStatsDto(
            pendingReviews = 5,
            reviewedToday = 2,
            approved = 10,
            rejected = 1,
            actionRequired = 3,
            totalApplications = 19
        )
    )
    var queueResult: NetworkResult<List<ApplicationDto>> = NetworkResult.Success(
        listOf(
            ApplicationDto(
                id = "app-1",
                applicantName = "Aarav Patel",
                schemeName = "PMEGP",
                status = "under_review",
                applicationNumber = "APP-PMEGP-1"
            ),
            ApplicationDto(
                id = "app-2",
                applicantName = "Meera Shah",
                schemeName = "Digital India Internship",
                status = "action_required",
                applicationNumber = "APP-DI-2"
            )
        )
    )

    override suspend fun getDashboardStats(): NetworkResult<ReviewerDashboardStatsDto> = statsResult

    override suspend fun getReviewQueue(status: String?, page: Int?, limit: Int?): NetworkResult<List<ApplicationDto>> = queueResult

    override suspend fun getApplicationDetails(applicationId: String): NetworkResult<ReviewerApplicationDetailDto> =
        NetworkResult.Error(404, "Not implemented in dashboard fake")

    override suspend fun submitDecision(applicationId: String, decision: String, remark: String?): NetworkResult<ReviewerDecisionResponseDto> =
        NetworkResult.Error(400, "Not implemented in dashboard fake")

    override suspend fun verifyDocument(documentId: String, status: String): NetworkResult<DocumentDto> =
        NetworkResult.Error(400, "Not implemented in dashboard fake")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewerDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReviewerRepository
    private lateinit var viewModel: ReviewerDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReviewerRepository()
        viewModel = ReviewerDashboardViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadPopulatesStatsAndQueue() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.stats.pendingReviews)
        assertEquals(10, state.stats.approved)
        assertEquals(2, state.applications.size)
        assertEquals("all", state.selectedStatus)
        assertFalse(state.isLoading)
    }

    @Test
    fun testSearchQueryFiltersApplications() = runTest {
        advanceUntilIdle()

        viewModel.setSearchQuery("Aarav")
        val filtered = viewModel.uiState.value.filteredApplications
        assertEquals(1, filtered.size)
        assertEquals("Aarav Patel", filtered[0].applicantName)

        viewModel.setSearchQuery("APP-DI-2")
        val filteredById = viewModel.uiState.value.filteredApplications
        assertEquals(1, filteredById.size)
        assertEquals("Meera Shah", filteredById[0].applicantName)

        viewModel.setSearchQuery("NonExistent")
        val emptyList = viewModel.uiState.value.filteredApplications
        assertEquals(0, emptyList.size)
    }

    @Test
    fun testStatusFilterTriggersQueueReload() = runTest {
        advanceUntilIdle()

        fakeRepository.queueResult = NetworkResult.Success(
            listOf(
                ApplicationDto(
                    id = "app-2",
                    applicantName = "Meera Shah",
                    status = "action_required"
                )
            )
        )

        viewModel.setStatusFilter("action_required")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("action_required", state.selectedStatus)
        assertEquals(1, state.applications.size)
        assertEquals("Meera Shah", state.applications[0].applicantName)
    }
}
