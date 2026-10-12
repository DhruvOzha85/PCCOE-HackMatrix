package com.fin.mobile.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplicationDetailUiState(
    val isLoading: Boolean = true,
    val application: ApplicationDto? = null,
    val errorMessage: String? = null,
    val receiptText: String? = null
)

class ApplicationDetailViewModel(
    private val applicationId: String,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationDetailUiState())
    val uiState: StateFlow<ApplicationDetailUiState> = _uiState.asStateFlow()

    init {
        loadApplication()
    }

    fun loadApplication() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = applicationRepository.getApplicationById(applicationId)) {
                is NetworkResult.Success -> {
                    val app = result.data
                    val receipt = buildReceiptText(app)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            application = app,
                            receiptText = receipt,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message.ifBlank { "Failed to load application record (${result.code})" }
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

    private fun buildReceiptText(app: ApplicationDto): String {
        return buildString {
            appendLine("================================================================================")
            appendLine("           GOVERNMENT OF INDIA / STATE NODAL ADMINISTRATION")
            appendLine("                    FINANCIAL POLICY INTELLIGENCE (FIN)")
            appendLine("                 OFFICIAL SCHEME APPLICATION ACKNOWLEDGEMENT")
            appendLine("================================================================================")
            appendLine()
            appendLine("APPLICATION REFERENCE")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Application Identifier : ${app.referenceId}")
            appendLine("Record Tracking ID     : ${app.id}")
            appendLine("Applicant Name         : ${app.applicantName ?: "Registered Citizen"}")
            appendLine("Date of Submission     : ${app.displayDate}")
            appendLine("Current Review Status  : ${app.displayStatus}")
            appendLine()
            appendLine("SCHEME & IMPLEMENTING DETAILS")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("Scheme Title           : ${app.displayTitle}")
            appendLine("Classification         : ${app.category ?: "Government Assistance Programme"}")
            appendLine("Benefit Entitlement    : ${app.displayBenefit}")
            appendLine()
            appendLine("STATUS TIMELINE & VERIFICATION")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("• Submission      : [COMPLETED] ${app.displayDate}")
            val verifyStatus = if (app.normalizedStatus in setOf("approved", "sanctioned")) "COMPLETED"
                else if (app.normalizedStatus == "action_required") "ACTION REQUIRED"
                else if (app.normalizedStatus == "rejected") "REJECTED"
                else "IN PROGRESS"
            appendLine("• Verification    : [$verifyStatus]")
            val approvalStatus = if (app.normalizedStatus in setOf("approved", "sanctioned")) "APPROVED"
                else if (app.normalizedStatus == "rejected") "REJECTED"
                else "PENDING"
            appendLine("• Authority Decision : [$approvalStatus]")
            appendLine()
            appendLine("OFFICER REMARKS / DECISION NOTES")
            appendLine("--------------------------------------------------------------------------------")
            appendLine(app.remarks ?: app.rejectionReason ?: "Application is currently pending administrative review.")
            appendLine()
            appendLine("================================================================================")
            appendLine("Generated via FIN Policy Intelligence Native Mobile Portal")
        }
    }

    companion object {
        fun provideFactory(
            applicationId: String,
            applicationRepository: ApplicationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ApplicationDetailViewModel(applicationId, applicationRepository) as T
            }
        }
    }
}
