package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.data.remote.dto.SchemeSearchResponseDto
import com.fin.mobile.data.repository.SchemeRepository
import com.fin.mobile.ui.schemes.SchemeCatalogueViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

private class FakeSchemeRepository : SchemeRepository {
    var lastSearchQuery: String? = null
    var lastCategory: String? = null

    var searchResult: NetworkResult<SchemeSearchResponseDto> = NetworkResult.Success(
        SchemeSearchResponseDto(
            success = true,
            total = 1,
            schemes = listOf(
                SchemeDto(id = "s-1", title = "Scheme One", category = "agriculture")
            )
        )
    )

    var statsResult: NetworkResult<Map<String, Int>> = NetworkResult.Success(
        mapOf("agriculture" to 5, "business" to 3)
    )

    var schemeDetailResult: NetworkResult<SchemeDto> = NetworkResult.Success(
        SchemeDto(id = "s-1", title = "Scheme One", category = "agriculture")
    )

    var faqsResult: NetworkResult<List<SchemeFaqDto>> = NetworkResult.Success(
        listOf(SchemeFaqDto(question = "Q1", answer = "A1"))
    )

    var eligibilityResult: NetworkResult<EligibilityEvaluationDto> = NetworkResult.Success(
        EligibilityEvaluationDto(
            schemeId = "s-1",
            status = "PASS",
            isEligible = true,
            verdict = "ELIGIBLE"
        )
    )

    override suspend fun searchSchemes(
        query: String,
        category: String?,
        state: String?,
        limit: Int,
        offset: Int
    ): NetworkResult<SchemeSearchResponseDto> {
        lastSearchQuery = query
        lastCategory = category
        return searchResult
    }

    override suspend fun getSchemeById(id: String): NetworkResult<SchemeDto> = schemeDetailResult
    override suspend fun getSchemeFaqs(slug: String): NetworkResult<List<SchemeFaqDto>> = faqsResult
    override suspend fun getSchemeStats(): NetworkResult<Map<String, Int>> = statsResult
    override suspend fun getRecommendedSchemes(): NetworkResult<List<SchemeDto>> = NetworkResult.Success(emptyList())
    override suspend fun checkEligibility(
        schemeId: String,
        profileOverride: Map<String, Any?>?
    ): NetworkResult<EligibilityEvaluationDto> = eligibilityResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class SchemeCatalogueViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeSchemeRepository
    private lateinit var viewModel: SchemeCatalogueViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeSchemeRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadLoadsStatsAndSchemes() = runTest {
        viewModel = SchemeCatalogueViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.schemes.size)
        assertEquals("s-1", state.schemes[0].id)
        assertEquals(5, state.categoryCounts["agriculture"])
    }

    @Test
    fun testCategoryFilterTriggersNewSearch() = runTest {
        viewModel = SchemeCatalogueViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onCategorySelected("business")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("business", state.selectedCategory)
        assertEquals("business", fakeRepository.lastCategory)
    }

    @Test
    fun testSearchQueryDebounce() = runTest {
        viewModel = SchemeCatalogueViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("pm-kisan")
        // Before debounce
        advanceTimeBy(100)
        // Advance past debounce 300ms
        advanceTimeBy(250)
        advanceUntilIdle()

        assertEquals("pm-kisan", fakeRepository.lastSearchQuery)
        assertEquals("pm-kisan", viewModel.uiState.value.query)
    }

    @Test
    fun testErrorAndRetry() = runTest {
        fakeRepository.searchResult = NetworkResult.Error(code = 404, message = "Not found")
        viewModel = SchemeCatalogueViewModel(fakeRepository)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertNotNull(errorState.errorMessage)
        assertEquals(0, errorState.schemes.size)

        // Fix and retry
        fakeRepository.searchResult = NetworkResult.Success(
            SchemeSearchResponseDto(
                success = true,
                total = 2,
                schemes = listOf(
                    SchemeDto(id = "s-1", title = "Scheme One"),
                    SchemeDto(id = "s-2", title = "Scheme Two")
                )
            )
        )

        viewModel.retry()
        advanceUntilIdle()

        val recoveredState = viewModel.uiState.value
        assertEquals(null, recoveredState.errorMessage)
        assertEquals(2, recoveredState.schemes.size)
    }
}
