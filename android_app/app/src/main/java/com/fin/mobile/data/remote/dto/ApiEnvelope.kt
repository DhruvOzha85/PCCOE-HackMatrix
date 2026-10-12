package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

/**
 * Standard backend API envelope wrapper matching the Node.js/Express responses.
 */
data class ApiEnvelope<T>(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "data") val data: T? = null
)
