package com.fin.mobile.ui.schemes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.EligibilityEvaluationDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqDto
import com.fin.mobile.data.repository.SchemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SchemeDetailTab(val label: String) {
    OVERVIEW("Overview"),
    BENEFITS("Benefits"),
    ELIGIBILITY("Eligibility & Criteria"),
    FAQS("FAQs")
}

data class SchemeDetailUiState(
    val isLoading: Boolean = true,
    val scheme: SchemeDto? = null,
    val faqs: List<SchemeFaqDto> = emptyList(),
    val isLoadingFaqs: Boolean = false,
    val activeTab: SchemeDetailTab = SchemeDetailTab.OVERVIEW,
    val errorMessage: String? = null,
    val isEvaluatingEligibility: Boolean = false,
    val eligibilityResult: EligibilityEvaluationDto? = null,
    val eligibilityError: String? = null
)

class SchemeDetailViewModel(
    private val schemeRepository: SchemeRepository,
    private val schemeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchemeDetailUiState())
    val uiState: StateFlow<SchemeDetailUiState> = _uiState.asStateFlow()

    init {
        loadScheme()
    }

    fun loadScheme() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = schemeRepository.getSchemeById(schemeId)) {
                is NetworkResult.Success -> {
                    val scheme = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scheme = scheme,
                            errorMessage = null
                        )
                    }
                    val slugToFetch = scheme.slug ?: scheme.id
                    loadFaqs(slugToFetch)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message.ifBlank { "Scheme details not found (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.localizedMessage ?: "Network connection error"
                        )
                    }
                }
            }
        }
    }

    fun setTab(tab: SchemeDetailTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    private fun loadFaqs(slug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFaqs = true) }
            when (val result = schemeRepository.getSchemeFaqs(slug)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingFaqs = false,
                            faqs = result.data
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoadingFaqs = false) }
                }
            }
        }
    }

    fun evaluateEligibility() {
        val currentScheme = _uiState.value.scheme ?: return
        val targetSchemeId = currentScheme.id

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEvaluatingEligibility = true,
                    eligibilityError = null
                )
            }
            when (val result = schemeRepository.checkEligibility(targetSchemeId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isEvaluatingEligibility = false,
                            eligibilityResult = result.data,
                            eligibilityError = null,
                            activeTab = SchemeDetailTab.ELIGIBILITY
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEvaluatingEligibility = false,
                            eligibilityError = result.message.ifBlank { "Eligibility evaluation failed (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isEvaluatingEligibility = false,
                            eligibilityError = result.throwable.localizedMessage ?: "Could not evaluate eligibility"
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            schemeRepository: SchemeRepository,
            schemeId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SchemeDetailViewModel(schemeRepository, schemeId) as T
            }
        }
    }
}
