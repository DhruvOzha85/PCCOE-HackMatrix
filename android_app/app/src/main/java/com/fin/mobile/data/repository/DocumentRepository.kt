package com.fin.mobile.data.repository

import com.fin.mobile.data.remote.FinApiService
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.ExtractDocumentRequestDto
import com.fin.mobile.data.remote.safeApiCall
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object DocumentFileValidator {
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
    val ALLOWED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "webp")
    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun validateFile(fileName: String, fileSizeBytes: Long, mimeType: String?): ValidationResult {
        if (fileSizeBytes <= 0) {
            return ValidationResult.Invalid("Selected file is empty (0 bytes).")
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            val sizeMb = fileSizeBytes / (1024 * 1024.0)
            return ValidationResult.Invalid(
                "File size (${String.format("%.1f", sizeMb)} MB) exceeds permissible limit of 10 MB."
            )
        }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isBlank() || !ALLOWED_EXTENSIONS.contains(extension)) {
            return ValidationResult.Invalid(
                "Unsupported file format '.$extension'. Allowed formats: PDF, JPEG, PNG, WEBP."
            )
        }

        if (!mimeType.isNullOrBlank() && mimeType != "application/octet-stream") {
            val normalizedMime = mimeType.lowercase().trim()
            if (!ALLOWED_MIME_TYPES.contains(normalizedMime)) {
                return ValidationResult.Invalid(
                    "Unsupported MIME type '$mimeType'. Allowed: PDF or images (JPG, PNG, WEBP)."
                )
            }
        }

        return ValidationResult.Valid
    }
}

interface DocumentRepository {
    suspend fun listDocuments(): NetworkResult<List<DocumentDto>>
    suspend fun getDocumentById(id: String): NetworkResult<DocumentDto>
    suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        applicationId: String? = null
    ): NetworkResult<DocumentDto>
    suspend fun extractDocument(documentId: String): NetworkResult<DocumentDto>
    suspend fun deleteDocument(documentId: String): NetworkResult<Unit>
}

class DocumentRepositoryImpl(
    private val apiService: FinApiService,
    private val moshi: Moshi
) : DocumentRepository {

    override suspend fun listDocuments(): NetworkResult<List<DocumentDto>> {
        val result = safeApiCall(moshi) { apiService.getDocuments() }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.data)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun getDocumentById(id: String): NetworkResult<DocumentDto> {
        val result = safeApiCall(moshi) { apiService.getDocumentById(id) }
        return when (result) {
            is NetworkResult.Success -> {
                val doc = result.data.data
                if (doc != null) {
                    NetworkResult.Success(doc)
                } else {
                    NetworkResult.Error(404, result.data.message ?: "Document not found")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun uploadDocument(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        applicationId: String?
    ): NetworkResult<DocumentDto> {
        val validation = DocumentFileValidator.validateFile(fileName, fileBytes.size.toLong(), mimeType)
        if (validation is DocumentFileValidator.ValidationResult.Invalid) {
            return NetworkResult.Error(400, validation.reason)
        }

        val mediaType = mimeType.toMediaTypeOrNull() ?: "application/pdf".toMediaTypeOrNull()
        val fileRequestBody = fileBytes.toRequestBody(mediaType)
        val filePart = MultipartBody.Part.createFormData("file", fileName, fileRequestBody)

        val docTypeRequestBody = documentType.toRequestBody("text/plain".toMediaTypeOrNull())
        val appRequestBody = applicationId?.toRequestBody("text/plain".toMediaTypeOrNull())

        val result = safeApiCall(moshi) {
            apiService.uploadDocument(filePart, docTypeRequestBody, appRequestBody)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val doc = result.data.data
                if (doc != null) {
                    NetworkResult.Success(doc)
                } else {
                    NetworkResult.Error(500, result.data.message ?: "Upload succeeded without returned document")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun extractDocument(documentId: String): NetworkResult<DocumentDto> {
        val request = ExtractDocumentRequestDto(documentId = documentId)
        val result = safeApiCall(moshi) { apiService.extractDocument(request) }
        return when (result) {
            is NetworkResult.Success -> {
                val doc = result.data.data
                if (doc != null) {
                    NetworkResult.Success(doc)
                } else {
                    NetworkResult.Error(500, result.data.message ?: "Extraction failed")
                }
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }

    override suspend fun deleteDocument(documentId: String): NetworkResult<Unit> {
        val result = safeApiCall(moshi) { apiService.deleteDocument(documentId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message, result.errorBody)
            is NetworkResult.Exception -> NetworkResult.Exception(result.throwable)
        }
    }
}
