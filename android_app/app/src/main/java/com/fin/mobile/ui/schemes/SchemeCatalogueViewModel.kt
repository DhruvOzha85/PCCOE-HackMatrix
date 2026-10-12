package com.fin.mobile.ui.schemes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.repository.SchemeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SchemeCategory(
    val key: String,
    val label: String
)

val CANONICAL_SCHEME_CATEGORIES = listOf(
    SchemeCategory("all", "All Schemes"),
    SchemeCategory("agriculture", "Agriculture"),
    SchemeCategory("business", "Business & MSME"),
    SchemeCategory("education", "Education"),
    SchemeCategory("women", "Women & Child"),
    SchemeCategory("youth", "Youth & Skills"),
    SchemeCategory("health", "Health"),
    SchemeCategory("social-welfare", "Social Welfare")
)

data class SchemeCatalogueUiState(
    val isLoading: Boolean = true,
    val schemes: List<SchemeDto> = emptyList(),
    val total: Int = 0,
    val query: String = "",
    val selectedCategory: String = "all",
    val categoryCounts: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)

class SchemeCatalogueViewModel(
    private val schemeRepository: SchemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchemeCatalogueUiState())
    val uiState: StateFlow<SchemeCatalogueUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadStats()
        loadSchemes()
    }

    private fun loadStats() {
        viewModelScope.launch {
            when (val result = schemeRepository.getSchemeStats()) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(categoryCounts = result.data) }
                }
                else -> { /* Non-critical, fallback to empty counts */ }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            loadSchemes(isUserTriggered = true)
        }
    }

    fun onCategorySelected(categoryKey: String) {
        if (_uiState.value.selectedCategory == categoryKey) return
        _uiState.update { it.copy(selectedCategory = categoryKey) }
        loadSchemes(isUserTriggered = true)
    }

    fun retry() {
        loadSchemes(isUserTriggered = true)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadStats()
            loadSchemesInternal()
        }
    }

    private fun loadSchemes(isUserTriggered: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }
            loadSchemesInternal()
        }
    }

    private suspend fun loadSchemesInternal() {
        val current = _uiState.value
        val result = schemeRepository.searchSchemes(
            query = current.query,
            category = if (current.selectedCategory == "all") null else current.selectedCategory,
            limit = 50,
            offset = 0
        )

        when (result) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        schemes = result.data.schemes,
                        total = result.data.total,
                        errorMessage = null
                    )
                }
            }
            is NetworkResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.message.ifBlank { "Failed to load schemes (${result.code})" }
                    )
                }
            }
            is NetworkResult.Exception -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = result.throwable.localizedMessage ?: "Network connection error"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(schemeRepository: SchemeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SchemeCatalogueViewModel(schemeRepository) as T
                }
            }
    }
}
