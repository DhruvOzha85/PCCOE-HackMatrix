package com.fin.mobile

import com.fin.mobile.data.repository.DocumentFileValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentFileValidatorTest {

    @Test
    fun testValidPdfFile() {
        val result = DocumentFileValidator.validateFile(
            fileName = "aadhaar_card.pdf",
            fileSizeBytes = 2 * 1024 * 1024L, // 2MB
            mimeType = "application/pdf"
        )
        assertTrue(result is DocumentFileValidator.ValidationResult.Valid)
    }

    @Test
    fun testValidImageFiles() {
        val jpgResult = DocumentFileValidator.validateFile(
            fileName = "pan_card.jpg",
            fileSizeBytes = 1 * 1024 * 1024L,
            mimeType = "image/jpeg"
        )
        assertTrue(jpgResult is DocumentFileValidator.ValidationResult.Valid)

        val pngResult = DocumentFileValidator.validateFile(
            fileName = "income_proof.png",
            fileSizeBytes = 500 * 1024L,
            mimeType = "image/png"
        )
        assertTrue(pngResult is DocumentFileValidator.ValidationResult.Valid)

        val webpResult = DocumentFileValidator.validateFile(
            fileName = "passbook.webp",
            fileSizeBytes = 750 * 1024L,
            mimeType = "image/webp"
        )
        assertTrue(webpResult is DocumentFileValidator.ValidationResult.Valid)
    }

    @Test
    fun testRejectZeroByteFile() {
        val result = DocumentFileValidator.validateFile(
            fileName = "empty.pdf",
            fileSizeBytes = 0L,
            mimeType = "application/pdf"
        )
        assertTrue(result is DocumentFileValidator.ValidationResult.Invalid)
        val invalid = result as DocumentFileValidator.ValidationResult.Invalid
        assertTrue(invalid.reason.contains("empty"))
    }

    @Test
    fun testRejectOversizedFile() {
        val result = DocumentFileValidator.validateFile(
            fileName = "large_scan.pdf",
            fileSizeBytes = 11 * 1024 * 1024L, // 11MB exceeds 10MB limit
            mimeType = "application/pdf"
        )
        assertTrue(result is DocumentFileValidator.ValidationResult.Invalid)
        val invalid = result as DocumentFileValidator.ValidationResult.Invalid
        assertTrue(invalid.reason.contains("10 MB"))
    }

    @Test
    fun testRejectUnsupportedExtension() {
        val docxResult = DocumentFileValidator.validateFile(
            fileName = "document.docx",
            fileSizeBytes = 1024 * 1024L,
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        assertTrue(docxResult is DocumentFileValidator.ValidationResult.Invalid)

        val exeResult = DocumentFileValidator.validateFile(
            fileName = "malicious.exe",
            fileSizeBytes = 1024 * 1024L,
            mimeType = "application/octet-stream"
        )
        assertTrue(exeResult is DocumentFileValidator.ValidationResult.Invalid)
    }

    @Test
    fun testRejectDisallowedMimeType() {
        val result = DocumentFileValidator.validateFile(
            fileName = "test.pdf",
            fileSizeBytes = 1024 * 1024L,
            mimeType = "text/plain"
        )
        assertTrue(result is DocumentFileValidator.ValidationResult.Invalid)
    }
}
