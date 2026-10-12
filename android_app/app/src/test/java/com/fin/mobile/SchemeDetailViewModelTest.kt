package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.EligibilityRuleDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.data.remote.dto.SchemeSearchResponseDto
import com.fin.mobile.data.repository.SchemeRepository
import com.fin.mobile.ui.schemes.SchemeDetailTab
import com.fin.mobile.ui.schemes.SchemeDetailViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeDetailSchemeRepository : SchemeRepository {
    var detailResult: NetworkResult<SchemeDto> = NetworkResult.Success(
        SchemeDto(
            id = "pm-kisan",
            slug = "pm-kisan",
            title = "PM-KISAN",
            benefits = "₹6,000 per year",
            documentsRequired = listOf("Aadhaar", "Land Records")
        )
    )

    var faqsResult: NetworkResult<List<SchemeFaqDto>> = NetworkResult.Success(
        listOf(
            SchemeFaqDto(id = "1", question = "Question 1", answer = "Answer 1")
        )
    )

    var checkEligibilityResult: NetworkResult<EligibilityEvaluationDto> = NetworkResult.Success(
        EligibilityEvaluationDto(
            schemeId = "pm-kisan",
            status = "PASS",
            isEligible = true,
            verdict = "ELIGIBLE",
            verdictReason = "All statutory rules passed.",
            rules = listOf(
                EligibilityRuleDto(
                    ruleId = "LAND_RULE",
                    description = "Must own land",
                    status = "pass",
                    source = "Clause 1.1"
                )
            )
        )
    )

    override suspend fun searchSchemes(
        query: String,
        category: String?,
        state: String?,
        limit: Int,
        offset: Int
    ): NetworkResult<SchemeSearchResponseDto> = NetworkResult.Success(SchemeSearchResponseDto())

    override suspend fun getSchemeById(id: String): NetworkResult<SchemeDto> = detailResult
    override suspend fun getSchemeFaqs(slug: String): NetworkResult<List<SchemeFaqDto>> = faqsResult
    override suspend fun getSchemeStats(): NetworkResult<Map<String, Int>> = NetworkResult.Success(emptyMap())
    override suspend fun getRecommendedSchemes(): NetworkResult<List<SchemeDto>> = NetworkResult.Success(emptyList())

    override suspend fun checkEligibility(
        schemeId: String,
        profileOverride: Map<String, Any?>?
    ): NetworkResult<EligibilityEvaluationDto> = checkEligibilityResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class SchemeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDetailSchemeRepository
    private lateinit var viewModel: SchemeDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDetailSchemeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadLoadsSchemeAndFaqs() = runTest {
        viewModel = SchemeDetailViewModel(fakeRepository, "pm-kisan")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.scheme)
        assertEquals("PM-KISAN", state.scheme?.displayTitle)
        assertEquals(1, state.faqs.size)
        assertEquals("Question 1", state.faqs[0].question)
        assertEquals(SchemeDetailTab.OVERVIEW, state.activeTab)
    }

    @Test
    fun testTabSwitching() = runTest {
        viewModel = SchemeDetailViewModel(fakeRepository, "pm-kisan")
        advanceUntilIdle()

        viewModel.setTab(SchemeDetailTab.BENEFITS)
        assertEquals(SchemeDetailTab.BENEFITS, viewModel.uiState.value.activeTab)

        viewModel.setTab(SchemeDetailTab.FAQS)
        assertEquals(SchemeDetailTab.FAQS, viewModel.uiState.value.activeTab)
    }

    @Test
    fun testLiveEligibilityEvaluationSuccess() = runTest {
        viewModel = SchemeDetailViewModel(fakeRepository, "pm-kisan")
        advanceUntilIdle()

        viewModel.evaluateEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEvaluatingEligibility)
        assertNotNull(state.eligibilityResult)
        assertEquals("PASS", state.eligibilityResult?.status)
        assertEquals("ELIGIBLE", state.eligibilityResult?.verdict)
        assertEquals(1, state.eligibilityResult?.rules?.size)
        assertEquals("LAND_RULE", state.eligibilityResult?.rules?.get(0)?.ruleId)
        // Evaluation auto-switches to ELIGIBILITY tab
        assertEquals(SchemeDetailTab.ELIGIBILITY, state.activeTab)
    }

    @Test
    fun testLiveEligibilityEvaluationError() = runTest {
        viewModel = SchemeDetailViewModel(fakeRepository, "pm-kisan")
        advanceUntilIdle()

        fakeRepository.checkEligibilityResult = NetworkResult.Error(code = 400, message = "Profile missing required fields")
        viewModel.evaluateEligibility()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEvaluatingEligibility)
        assertEquals(null, state.eligibilityResult)
        assertEquals("Profile missing required fields", state.eligibilityError)
    }
}
