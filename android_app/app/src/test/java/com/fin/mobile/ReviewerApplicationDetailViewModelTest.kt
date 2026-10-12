package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.ReviewHistoryItemDto
import com.fin.mobile.data.remote.dto.ReviewerApplicantInfoDto
import com.fin.mobile.data.remote.dto.ReviewerApplicationDetailDto
import com.fin.mobile.data.remote.dto.ReviewerDashboardStatsDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionResponseDto
import com.fin.mobile.data.remote.dto.ReviewerDocumentDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.repository.ReviewerRepository
import com.fin.mobile.ui.reviewer.ReviewerApplicationDetailViewModel
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

private class FakeReviewerDetailRepository : ReviewerRepository {
    var detailsResult: NetworkResult<ReviewerApplicationDetailDto> = NetworkResult.Success(
        ReviewerApplicationDetailDto(
            application = ApplicationDto(id = "app-100", status = "under_review"),
            applicant = ReviewerApplicantInfoDto(fullName = "Sunita Rao", email = "sunita@example.com"),
            scheme = SchemeDto(id = "pmegp", title = "PMEGP"),
            documents = listOf(
                ReviewerDocumentDto(id = "doc-1", documentType = "income_certificate", verificationStatus = "PENDING")
            ),
            reviewHistory = emptyList()
        )
    )

    var decisionResult: NetworkResult<ReviewerDecisionResponseDto> = NetworkResult.Success(
        ReviewerDecisionResponseDto(
            success = true,
            message = "Application successfully marked as approved",
            application = ApplicationDto(id = "app-100", status = "approved")
        )
    )

    var verifyDocResult: NetworkResult<DocumentDto> = NetworkResult.Success(
        DocumentDto(id = "doc-1", verificationStatus = "VERIFIED")
    )

    var submittedDecision: String? = null
    var submittedRemark: String? = null

    override suspend fun getDashboardStats(): NetworkResult<ReviewerDashboardStatsDto> =
        NetworkResult.Error(400, "Not used")

    override suspend fun getReviewQueue(status: String?, page: Int?, limit: Int?): NetworkResult<List<ApplicationDto>> =
        NetworkResult.Error(400, "Not used")

    override suspend fun getApplicationDetails(applicationId: String): NetworkResult<ReviewerApplicationDetailDto> =
        detailsResult

    override suspend fun submitDecision(applicationId: String, decision: String, remark: String?): NetworkResult<ReviewerDecisionResponseDto> {
        submittedDecision = decision
        submittedRemark = remark
        return decisionResult
    }

    override suspend fun verifyDocument(documentId: String, status: String): NetworkResult<DocumentDto> =
        verifyDocResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewerApplicationDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReviewerDetailRepository
    private lateinit var viewModel: ReviewerApplicationDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReviewerDetailRepository()
        viewModel = ReviewerApplicationDetailViewModel("app-100", fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadDetailsSuccess() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.details)
        assertEquals("app-100", state.details?.application?.id)
        assertEquals("Sunita Rao", state.details?.applicant?.fullName)
        assertEquals(1, state.details?.documents?.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun testRejectRequiresMandatoryRemark() = runTest {
        advanceUntilIdle()

        viewModel.openDecisionDialog("reject")
        assertTrue(viewModel.uiState.value.decisionDialog.isOpen)
        assertEquals("reject", viewModel.uiState.value.decisionDialog.decisionType)

        // Attempt submit with blank remark
        viewModel.onRemarkChanged("")
        viewModel.submitDecision()

        val stateAfterValidationFail = viewModel.uiState.value
        assertNotNull(stateAfterValidationFail.decisionDialog.validationError)
        assertTrue(stateAfterValidationFail.decisionDialog.validationError?.contains("mandatory") == true)
        assertNull(fakeRepository.submittedDecision) // Never called repository
    }

    @Test
    fun testRequestChangesRequiresMandatoryRemark() = runTest {
        advanceUntilIdle()

        viewModel.openDecisionDialog("request_changes")
        viewModel.onRemarkChanged("   ")
        viewModel.submitDecision()

        val state = viewModel.uiState.value
        assertNotNull(state.decisionDialog.validationError)
        assertTrue(state.decisionDialog.validationError?.contains("mandatory") == true)
        assertNull(fakeRepository.submittedDecision)
    }

    @Test
    fun testApproveSubmitsSuccessfullyAndRefreshesAuthoritativeState() = runTest {
        advanceUntilIdle()

        viewModel.openDecisionDialog("approve")
        viewModel.onRemarkChanged("All documents in order")
        viewModel.submitDecision()
        advanceUntilIdle()

        assertEquals("approve", fakeRepository.submittedDecision)
        assertEquals("All documents in order", fakeRepository.submittedRemark)

        val finalState = viewModel.uiState.value
        assertFalse(finalState.decisionDialog.isOpen)
        assertNotNull(finalState.successMessage)
    }

    @Test
    fun testSubmitDecisionSelfReviewForbidden403() = runTest {
        advanceUntilIdle()

        fakeRepository.decisionResult = NetworkResult.Error(
            403,
            "Conflict of interest: You are not authorized to review or approve your own application."
        )

        viewModel.openDecisionDialog("approve")
        viewModel.submitDecision()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("Conflict of interest") == true)
        assertTrue(state.decisionDialog.validationError?.contains("Conflict of interest") == true)
    }

    @Test
    fun testVerifyDocumentActionRefreshesAuthoritativeState() = runTest {
        advanceUntilIdle()

        viewModel.verifyDocument("doc-1", "VERIFIED")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.verifyingDocumentId)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage?.contains("VERIFIED") == true)
    }
}
