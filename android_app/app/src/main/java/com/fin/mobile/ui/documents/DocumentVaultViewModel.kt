package com.fin.mobile.ui.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentTypeOption(
    val key: String,
    val label: String,
    val category: String
)

val CANONICAL_DOCUMENT_TYPES = listOf(
    DocumentTypeOption("aadhaar", "Aadhaar Card", "Identity Proof"),
    DocumentTypeOption("pan", "PAN Card", "Tax / Financial Proof"),
    DocumentTypeOption("income_cert", "Income Certificate", "Income Verification"),
    DocumentTypeOption("caste_cert", "Caste Certificate", "Social Category"),
    DocumentTypeOption("bank_passbook", "Bank Passbook", "Direct Benefit Transfer"),
    DocumentTypeOption("land", "Land Ownership Records", "Agricultural Proof"),
    DocumentTypeOption("domicile", "Domicile Certificate", "State Residency"),
    DocumentTypeOption("udyam", "Udyam Registration", "Enterprise Proof"),
    DocumentTypeOption("address_proof", "Address Proof", "General Proof")
)

data class DocumentVaultUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentDto> = emptyList(),
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val extractingDocumentId: String? = null,
    val deletingDocumentId: String? = null,
    val selectedDocument: DocumentDto? = null,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)

class DocumentVaultViewModel(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentVaultUiState())
    val uiState: StateFlow<DocumentVaultUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = documentRepository.listDocuments()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            documents = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message.ifBlank { "Failed to load documents (${result.code})" }
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

    fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        applicationId: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploading = true,
                    uploadError = null,
                    actionMessage = null
                )
            }

            when (val result = documentRepository.uploadDocument(fileBytes, fileName, mimeType, documentType, applicationId)) {
                is NetworkResult.Success -> {
                    val uploadedDoc = result.data
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            documents = listOf(uploadedDoc) + it.documents.filter { d -> d.id != uploadedDoc.id },
                            actionMessage = "Document uploaded successfully",
                            uploadError = null
                        )
                    }
                    // Trigger OCR extraction automatically for newly uploaded document
                    extractDocument(uploadedDoc.id)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadError = result.message.ifBlank { "Upload failed (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadError = result.throwable.localizedMessage ?: "Failed to upload document"
                        )
                    }
                }
            }
        }
    }

    fun extractDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(extractingDocumentId = documentId) }

            when (val result = documentRepository.extractDocument(documentId)) {
                is NetworkResult.Success -> {
                    val updatedDoc = result.data
                    _uiState.update { state ->
                        state.copy(
                            extractingDocumentId = null,
                            documents = state.documents.map { if (it.id == updatedDoc.id) updatedDoc else it },
                            selectedDocument = if (state.selectedDocument?.id == updatedDoc.id) updatedDoc else state.selectedDocument,
                            actionMessage = "Facts extracted via Intelligence OCR"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            extractingDocumentId = null,
                            errorMessage = result.message.ifBlank { "Extraction failed (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            extractingDocumentId = null,
                            errorMessage = result.throwable.localizedMessage ?: "AI extraction unavailable"
                        )
                    }
                }
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingDocumentId = documentId) }

            when (val result = documentRepository.deleteDocument(documentId)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingDocumentId = null,
                            documents = state.documents.filter { it.id != documentId },
                            selectedDocument = if (state.selectedDocument?.id == documentId) null else state.selectedDocument,
                            actionMessage = "Document deleted from vault"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            deletingDocumentId = null,
                            errorMessage = result.message.ifBlank { "Failed to delete document (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            deletingDocumentId = null,
                            errorMessage = result.throwable.localizedMessage ?: "Network connection error"
                        )
                    }
                }
            }
        }
    }

    fun selectDocument(document: DocumentDto?) {
        _uiState.update { it.copy(selectedDocument = document) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(actionMessage = null, uploadError = null, errorMessage = null) }
    }

    companion object {
        fun provideFactory(documentRepository: DocumentRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DocumentVaultViewModel(documentRepository) as T
                }
            }
    }
}
