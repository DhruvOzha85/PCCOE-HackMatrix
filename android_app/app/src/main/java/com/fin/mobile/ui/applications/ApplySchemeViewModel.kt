package com.fin.mobile.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ApplicationReadinessDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.repository.ApplicationRepository
import com.fin.mobile.data.repository.DocumentRepository
import com.fin.mobile.data.repository.SchemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplySchemeUiState(
    val isLoading: Boolean = true,
    val scheme: SchemeDto? = null,
    val readiness: ApplicationReadinessDto? = null,
    val userDocuments: List<DocumentDto> = emptyList(),
    val hasConfirmedDeclaration: Boolean = false,
    val isSubmitting: Boolean = false,
    val isAlreadySubmitted: Boolean = false,
    val submittedApplication: ApplicationDto? = null,
    val errorMessage: String? = null
)

class ApplySchemeViewModel(
    private val schemeId: String,
    private val applicationRepository: ApplicationRepository,
    private val schemeRepository: SchemeRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplySchemeUiState())
    val uiState: StateFlow<ApplySchemeUiState> = _uiState.asStateFlow()

    init {
        loadReadinessAndScheme()
    }

    fun loadReadinessAndScheme() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Load Scheme
            val schemeResult = schemeRepository.getSchemeById(schemeId)
            val loadedScheme = if (schemeResult is NetworkResult.Success) schemeResult.data else null

            // 2. Load Readiness Analysis
            val readinessResult = applicationRepository.analyzeApplication(schemeId)
            val loadedReadiness = if (readinessResult is NetworkResult.Success) readinessResult.data else null

            // 3. Load User Documents from Vault
            val docsResult = documentRepository.listDocuments()
            val loadedDocs = if (docsResult is NetworkResult.Success) docsResult.data else emptyList()

            val err = if (readinessResult is NetworkResult.Error && loadedScheme == null) {
                readinessResult.message
            } else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    scheme = loadedScheme,
                    readiness = loadedReadiness,
                    userDocuments = loadedDocs,
                    errorMessage = err
                )
            }
        }
    }

    fun onDeclarationToggled(checked: Boolean) {
        _uiState.update { it.copy(hasConfirmedDeclaration = checked) }
    }

    fun submitApplication() {
        if (_uiState.value.isSubmitting) return
        if (!_uiState.value.hasConfirmedDeclaration) {
            _uiState.update { it.copy(errorMessage = "Please confirm the citizen declaration before submitting.") }
            return
        }

        val schemeName = _uiState.value.scheme?.displayTitle
            ?: _uiState.value.readiness?.schemeName

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            when (val result = applicationRepository.submitApplication(schemeId, schemeName)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submittedApplication = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    val isDuplicate = result.code == 409
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isAlreadySubmitted = isDuplicate,
                            errorMessage = if (isDuplicate) {
                                "An application for this scheme has already been submitted for your profile."
                            } else {
                                result.message.ifBlank { "Application submission failed (${result.code})" }
                            }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.throwable.localizedMessage ?: "Network connection error"
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            schemeId: String,
            applicationRepository: ApplicationRepository,
            schemeRepository: SchemeRepository,
            documentRepository: DocumentRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ApplySchemeViewModel(schemeId, applicationRepository, schemeRepository, documentRepository) as T
            }
        }
    }
}
