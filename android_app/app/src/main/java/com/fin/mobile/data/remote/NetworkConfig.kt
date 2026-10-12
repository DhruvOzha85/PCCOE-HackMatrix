package com.fin.mobile.data.remote

import com.fin.mobile.BuildConfig

/**
 * Centralized networking configuration.
 *
 * Defaults to [BuildConfig.BASE_URL] which resolves to:
 * - Local emulator development: http://10.0.2.2:5000/
 * - Release / Production: https://api.fin.gov.in/ (HTTPS enforced)
 */
data class NetworkConfig(
    val baseUrl: String = BuildConfig.BASE_URL,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val isDebug: Boolean = BuildConfig.DEBUG
)
