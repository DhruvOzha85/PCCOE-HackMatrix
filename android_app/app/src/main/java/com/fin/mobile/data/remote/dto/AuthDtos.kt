package com.fin.mobile.data.remote.dto

import com.squareup.moshi.Json

data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "mode") val mode: String? = "citizen"
)

data class RegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "fullName") val fullName: String,
    @Json(name = "phone") val phone: String? = null
)

data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String
)

data class AuthResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "user") val user: UserDto? = null
)

data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val displayName: String
        get() = (userMetadata?.get("full_name") as? String)
            ?: (userMetadata?.get("fullName") as? String)
            ?: email
            ?: "User"
}

data class UserResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "user") val user: UserDto? = null
)

data class UserProfileDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "occupation") val occupation: String? = null,
    @Json(name = "annual_income") val annualIncome: Double? = null,
    @Json(name = "caste_category") val casteCategory: String? = null,
    @Json(name = "applicant_type") val applicantType: String? = null
)

data class ChangePasswordRequestDto(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

data class ChangePasswordResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null
)
