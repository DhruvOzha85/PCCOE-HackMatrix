package com.fin.mobile.data.remote

import com.fin.mobile.data.remote.dto.ApiEnvelope
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.AuthResponseDto
import com.fin.mobile.data.remote.dto.ChatConversationDetailResponseDto
import com.fin.mobile.data.remote.dto.ChatDeleteResponseDto
import com.fin.mobile.data.remote.dto.ChatHistoryListResponseDto
import com.fin.mobile.data.remote.dto.ChatRequestDto
import com.fin.mobile.data.remote.dto.ChatResponseEnvelopeDto
import com.fin.mobile.data.remote.dto.CreateApplicationRequestDto
import com.fin.mobile.data.remote.dto.DashboardResponseDto
import com.fin.mobile.data.remote.dto.DeleteDocumentResponseDto
import com.fin.mobile.data.remote.dto.DocumentDetailResponseDto
import com.fin.mobile.data.remote.dto.DocumentDto
import com.fin.mobile.data.remote.dto.DocumentListResponseDto
import com.fin.mobile.data.remote.dto.ChangePasswordRequestDto
import com.fin.mobile.data.remote.dto.ChangePasswordResponseDto
import com.fin.mobile.data.remote.dto.EligibilityCheckRequestDto
import com.fin.mobile.data.remote.dto.EligibilityResponseDto
import com.fin.mobile.data.remote.dto.ExtractDocumentRequestDto
import com.fin.mobile.data.remote.dto.LoginRequestDto
import com.fin.mobile.data.remote.dto.NotificationActionResponseDto
import com.fin.mobile.data.remote.dto.NotificationDto
import com.fin.mobile.data.remote.dto.NotificationListResponseDto
import com.fin.mobile.data.remote.dto.RefreshTokenRequestDto
import com.fin.mobile.data.remote.dto.RegisterRequestDto
import com.fin.mobile.data.remote.dto.SchemeDetailResponseDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.remote.dto.SchemeFaqsResponseDto
import com.fin.mobile.data.remote.dto.SchemeRecommendationResponseDto
import com.fin.mobile.data.remote.dto.SchemeSearchRequestDto
import com.fin.mobile.data.remote.dto.SchemeSearchResponseDto
import com.fin.mobile.data.remote.dto.SchemeStatsResponseDto
import com.fin.mobile.data.remote.dto.ReviewerApplicationDetailResponseDto
import com.fin.mobile.data.remote.dto.ReviewerDashboardResponseDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionRequestDto
import com.fin.mobile.data.remote.dto.ReviewerDecisionResponseDto
import com.fin.mobile.data.remote.dto.ReviewerQueueResponseDto
import com.fin.mobile.data.remote.dto.ReviewerVerifyDocumentRequestDto
import com.fin.mobile.data.remote.dto.ReviewerVerifyDocumentResponseDto
import com.fin.mobile.data.remote.dto.UserProfileDto
import com.fin.mobile.data.remote.dto.UserResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Typed Retrofit API interface corresponding directly to verified backend route definitions.
 */
interface FinApiService {

    // --- Authentication & User Accounts (/api/users) ---
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("api/users/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<AuthResponseDto>

    @GET("api/users/me")
    suspend fun getMe(): Response<UserResponseDto>

    @POST("api/users/logout")
    suspend fun logout(): Response<ApiEnvelope<Unit>>

    @POST("api/users/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<ChangePasswordResponseDto>

    // --- User Profile (/api/users/profile) ---
    @GET("api/users/profile")
    suspend fun getProfile(): Response<ApiEnvelope<UserProfileDto>>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body profile: @JvmSuppressWildcards Map<String, Any?>): Response<ApiEnvelope<UserProfileDto>>

    // --- Schemes (/api/schemes) ---
    @GET("api/schemes")
    suspend fun getSchemes(@QueryMap queries: Map<String, String> = emptyMap()): Response<SchemeSearchResponseDto>

    @POST("api/schemes/search")
    suspend fun searchSchemes(@Body request: SchemeSearchRequestDto): Response<SchemeSearchResponseDto>

    @GET("api/schemes/search")
    suspend fun searchSchemesGet(@Query("q") query: String): Response<SchemeSearchResponseDto>

    @GET("api/schemes/stats")
    suspend fun getSchemeStats(): Response<SchemeStatsResponseDto>

    @GET("api/schemes/applicable")
    suspend fun getApplicableSchemes(@QueryMap queries: Map<String, String> = emptyMap()): Response<SchemeSearchResponseDto>

    @GET("api/schemes/recommend")
    suspend fun getRecommendedSchemes(): Response<SchemeRecommendationResponseDto>

    @POST("api/schemes/recommend")
    suspend fun getRecommendedSchemesPost(@Body body: @JvmSuppressWildcards Map<String, Any?> = emptyMap()): Response<SchemeRecommendationResponseDto>

    @GET("api/schemes/{id}")
    suspend fun getSchemeById(@Path("id") schemeId: String): Response<SchemeDetailResponseDto>

    @GET("api/schemes/{slug}/faqs")
    suspend fun getSchemeFaqs(@Path("slug") slug: String): Response<SchemeFaqsResponseDto>

    // --- Eligibility Evaluation (/api/eligibility) ---
    @POST("api/eligibility/check")
    suspend fun checkEligibility(@Body request: EligibilityCheckRequestDto): Response<EligibilityResponseDto>

    // --- Applications (/api/applications) ---
    @GET("api/applications")
    suspend fun getApplications(): Response<com.fin.mobile.data.remote.dto.ApplicationListResponseDto>

    @POST("api/applications")
    suspend fun submitApplication(@Body request: CreateApplicationRequestDto): Response<com.fin.mobile.data.remote.dto.ApplicationDetailResponseDto>

    @GET("api/applications/{id}")
    suspend fun getApplicationById(@Path("id") id: String): Response<com.fin.mobile.data.remote.dto.ApplicationDetailResponseDto>

    @POST("api/applications/analyze")
    suspend fun analyzeApplication(@Body request: com.fin.mobile.data.remote.dto.AnalyzeApplicationRequestDto): Response<com.fin.mobile.data.remote.dto.AnalyzeApplicationResponseDto>

    // --- Citizen Dashboard (/api/dashboard) ---
    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponseDto>

    // --- Documents (/api/documents) ---
    @GET("api/documents")
    suspend fun getDocuments(): Response<DocumentListResponseDto>

    @GET("api/documents/{id}")
    suspend fun getDocumentById(@Path("id") id: String): Response<DocumentDetailResponseDto>

    @Multipart
    @POST("api/documents/process")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("documentType") documentType: RequestBody?,
        @Part("applicationId") applicationId: RequestBody? = null
    ): Response<DocumentDetailResponseDto>

    @POST("api/documents/extract")
    suspend fun extractDocument(@Body request: ExtractDocumentRequestDto): Response<DocumentDetailResponseDto>

    @DELETE("api/documents/{id}")
    suspend fun deleteDocument(@Path("id") id: String): Response<DeleteDocumentResponseDto>

    // --- Notifications (/api/notifications) ---
    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationListResponseDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<NotificationActionResponseDto>

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<NotificationActionResponseDto>

    // --- AI Assistant / Chat (/api/chat) ---
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequestDto): Response<ChatResponseEnvelopeDto>

    @GET("api/chat/history")
    suspend fun getChatHistory(): Response<ChatHistoryListResponseDto>

    @GET("api/chat/history/{id}")
    suspend fun getChatConversation(@Path("id") id: String): Response<ChatConversationDetailResponseDto>

    @DELETE("api/chat/history/{id}")
    suspend fun deleteChatConversation(@Path("id") id: String): Response<ChatDeleteResponseDto>

    // --- Reviewer Workflows (/api/reviewer) ---
    @GET("api/reviewer/dashboard")
    suspend fun getReviewerDashboard(): Response<ReviewerDashboardResponseDto>

    @GET("api/reviewer/applications")
    suspend fun getReviewerQueue(
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ReviewerQueueResponseDto>

    @GET("api/reviewer/applications/{id}")
    suspend fun getReviewerApplicationDetails(@Path("id") id: String): Response<ReviewerApplicationDetailResponseDto>

    @POST("api/reviewer/applications/{id}/decision")
    suspend fun submitReviewDecision(
        @Path("id") id: String,
        @Body request: ReviewerDecisionRequestDto
    ): Response<ReviewerDecisionResponseDto>

    @POST("api/reviewer/documents/{id}/verify")
    suspend fun verifyDocumentReviewer(
        @Path("id") id: String,
        @Body request: ReviewerVerifyDocumentRequestDto
    ): Response<ReviewerVerifyDocumentResponseDto>
}

