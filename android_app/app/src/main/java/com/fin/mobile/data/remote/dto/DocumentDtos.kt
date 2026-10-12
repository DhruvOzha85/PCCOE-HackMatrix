package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class OcrMetadataDto(
    @Json(name = "method") val method: String? = null,
    @Json(name = "pages") val pages: Int? = null,
    @Json(name = "sha256") val sha256: String? = null,
    @Json(name = "confidence") val confidence: Double? = null
)

data class DocumentDto(
    @Json(name = "id") val id: String,
    @Json(name = "applicationId") val applicationId: String? = null,
    @Json(name = "application_id") val applicationIdSnake: String? = null,
    @Json(name = "documentType") val documentType: String? = null,
    @Json(name = "document_type") val documentTypeSnake: String? = null,
    @Json(name = "fileName") val fileName: String? = null,
    @Json(name = "file_name") val fileNameSnake: String? = null,
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "verification_status") val verificationStatusSnake: String? = null,
    @Json(name = "reviewerRemarks") val reviewerRemarks: String? = null,
    @Json(name = "reviewer_remarks") val reviewerRemarksSnake: String? = null,
    @Json(name = "docNumber") val docNumber: String? = null,
    @Json(name = "issuer") val issuer: String? = null,
    @Json(name = "extractedData") val extractedData: Map<String, Any?>? = null,
    @Json(name = "extractedText") val extractedText: String? = null,
    @Json(name = "ocrMetadata") val ocrMetadata: OcrMetadataDto? = null,
    @Json(name = "uploadedAt") val uploadedAt: String? = null,
    @Json(name = "uploaded_at") val uploadedAtSnake: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "updated_at") val updatedAtSnake: String? = null,
    @Json(name = "fileUrl") val fileUrl: String? = null,
    @Json(name = "file_url") val fileUrlSnake: String? = null
) {
    val displayType: String
        get() = documentType ?: documentTypeSnake ?: "address_proof"

    val displayFileName: String
        get() = fileName ?: fileNameSnake ?: "Document"

    val displayStatus: String
        get() = (verificationStatus ?: verificationStatusSnake ?: "PENDING").uppercase()

    val displayDate: String
        get() = uploadedAt ?: uploadedAtSnake ?: ""

    val downloadUrl: String?
        get() = fileUrl ?: fileUrlSnake
}

data class DocumentListResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "data") val data: List<DocumentDto> = emptyList()
)

data class DocumentDetailResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: DocumentDto? = null
)

data class ExtractDocumentRequestDto(
    @Json(name = "documentId") val documentId: String
)

data class DeleteDocumentResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: Map<String, Any?>? = null
)
