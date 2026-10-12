package com.fin.mobile.ui.reviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ReviewerApplicationDetailDto
import com.fin.mobile.data.repository.ReviewerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecisionDialogState(
    val isOpen: Boolean = false,
    val decisionType: String? = null, // "approve", "request_changes", "reject"
    val remark: String = "",
    val validationError: String? = null
)

data class ReviewerApplicationDetailUiState(
    val applicationId: String,
    val details: ReviewerApplicationDetailDto? = null,
    val isLoading: Boolean = false,
    val isActionSubmitting: Boolean = false,
    val verifyingDocumentId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val decisionDialog: DecisionDialogState = DecisionDialogState()
)

class ReviewerApplicationDetailViewModel(
    private val applicationId: String,
    private val reviewerRepository: ReviewerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReviewerApplicationDetailUiState(applicationId = applicationId)
    )
    val uiState: StateFlow<ReviewerApplicationDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = reviewerRepository.getApplicationDetails(applicationId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            details = result.data,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Failed to retrieve application review details"
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.message ?: "Network error loading details"
                        )
                    }
                }
            }
        }
    }

    fun openDecisionDialog(type: String) {
        _uiState.update {
            it.copy(
                decisionDialog = DecisionDialogState(
                    isOpen = true,
                    decisionType = type,
                    remark = "",
                    validationError = null
                )
            )
        }
    }

    fun closeDecisionDialog() {
        _uiState.update {
            it.copy(decisionDialog = DecisionDialogState())
        }
    }

    fun onRemarkChanged(remark: String) {
        _uiState.update {
            it.copy(
                decisionDialog = it.decisionDialog.copy(
                    remark = remark,
                    validationError = null
                )
            )
        }
    }

    fun submitDecision() {
        val currentDialog = _uiState.value.decisionDialog
        val decision = currentDialog.decisionType?.lowercase() ?: return
        val remark = currentDialog.remark.trim()

        // Validation: Mandatory non-empty remark for 'reject' and 'request_changes'
        if ((decision == "reject" || decision == "request_changes") && remark.isBlank()) {
            val label = if (decision == "reject") "Reject" else "Request Changes"
            _uiState.update {
                it.copy(
                    decisionDialog = it.decisionDialog.copy(
                        validationError = "A detailed reviewer remark is mandatory when choosing '$label'."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionSubmitting = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            val result = reviewerRepository.submitDecision(
                applicationId = applicationId,
                decision = decision,
                remark = if (remark.isNotBlank()) remark else null
            )

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isActionSubmitting = false,
                            decisionDialog = DecisionDialogState(),
                            successMessage = result.data.message ?: "Review decision recorded successfully."
                        )
                    }
                    // Authoritative refresh of server state
                    loadDetails()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isActionSubmitting = false,
                            decisionDialog = it.decisionDialog.copy(
                                validationError = result.message ?: "Failed to submit decision."
                            ),
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    val errorMsg = result.throwable.message ?: "Network error submitting decision."
                    _uiState.update {
                        it.copy(
                            isActionSubmitting = false,
                            decisionDialog = it.decisionDialog.copy(
                                validationError = errorMsg
                            ),
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun verifyDocument(documentId: String, status: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    verifyingDocumentId = documentId,
                    errorMessage = null
                )
            }

            val result = reviewerRepository.verifyDocument(documentId, status)
            when (result) {
                is NetworkResult.Success -> {
                    // Update document state locally then refresh authoritative state
                    _uiState.update {
                        it.copy(
                            verifyingDocumentId = null,
                            successMessage = "Document status updated to $status"
                        )
                    }
                    loadDetails()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            verifyingDocumentId = null,
                            errorMessage = result.message ?: "Failed to verify document"
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            verifyingDocumentId = null,
                            errorMessage = result.throwable.message ?: "Network error verifying document"
                        )
                    }
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            applicationId: String,
            reviewerRepository: ReviewerRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReviewerApplicationDetailViewModel(applicationId, reviewerRepository) as T
                }
            }
    }
}
