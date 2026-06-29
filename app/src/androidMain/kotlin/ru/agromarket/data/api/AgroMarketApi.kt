package ru.agromarket.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import ru.agromarket.data.model.*

interface AgroMarketApi {

    // ==========================================
    // Auth
    // ==========================================
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("auth/verify")
    suspend fun verify(@Body request: VerifyCodeRequest): Response<MessageResponse>

    @POST("auth/set-password")
    suspend fun setPassword(@Body request: SetPasswordRequest): Response<LoginResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<LoginResponse>

    // ==========================================
    // Categories
    // ==========================================
    @GET("categories/")
    suspend fun getCategories(): Response<List<CategoryTreeResponse>>

    @GET("categories/{categoryId}/subcategories")
    suspend fun getSubcategories(@Path("categoryId") categoryId: Int): Response<List<CategoryResponse>>

    // ==========================================
    // Geography
    // ==========================================
    @GET("geo/regions")
    suspend fun getRegions(): Response<List<RegionResponse>>

    @GET("geo/regions/{regionId}/districts")
    suspend fun getDistricts(@Path("regionId") regionId: Int): Response<List<DistrictResponse>>

    @GET("geo/districts/{districtId}/localities")
    suspend fun getLocalities(@Path("districtId") districtId: Int): Response<List<LocalityResponse>>

    @GET("geo/search")
    suspend fun searchLocality(@Query("q") query: String): Response<List<LocalityResponse>>

    // ==========================================
    // Ads
    // ==========================================
    @GET("ads/")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("type") type: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("region_id") regionId: Int? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null
    ): Response<AdFeedResponse>

    @GET("ads/my")
    suspend fun getMyAds(): Response<List<AdMyListResponse>>

    @GET("ads/{adId}")
    suspend fun getAdDetail(@Path("adId") adId: String): Response<AdDetailResponse>

    @POST("ads/")
    suspend fun createAd(@Body request: AdCreateRequest): Response<AdDetailResponse>

    @PUT("ads/{adId}")
    suspend fun updateAd(@Path("adId") adId: String, @Body request: AdCreateRequest): Response<AdDetailResponse>

    @DELETE("ads/{adId}")
    suspend fun deleteAd(@Path("adId") adId: String): Response<MessageResponse>

    @Multipart
    @POST("ads/{adId}/photos")
    suspend fun uploadPhotos(
        @Path("adId") adId: String,
        @Part files: List<MultipartBody.Part>
    ): Response<List<AdPhotoResponse>>

    @DELETE("ads/{adId}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("adId") adId: String,
        @Path("photoId") photoId: Int
    ): Response<MessageResponse>

    @POST("ads/{adId}/submit")
    suspend fun submitAd(@Path("adId") adId: String): Response<AdDetailResponse>

    // ==========================================
    // Monetization — Платежи (boost через ЮKassa)
    // ==========================================
    @POST("payments/create")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<PaymentResponse>

    // ==========================================
    // Monetization — Pro-подписка (Фаза 2, отложена — мок)
    // ==========================================
    @GET("subscriptions/plans")
    suspend fun getSubscriptionPlans(): Response<SubscriptionPlansResponse>

    @GET("profile/me/subscription")
    suspend fun getMySubscription(): Response<SubscriptionResponse>

    @POST("profile/me/subscription")
    suspend fun subscribe(@Body request: SubscribeRequest): Response<SubscriptionResponse>

    // ==========================================
    // Favorites
    // ==========================================
    @GET("favorites/")
    suspend fun getFavorites(): Response<List<FavoriteResponse>>

    @POST("favorites/{adId}")
    suspend fun addFavorite(@Path("adId") adId: String): Response<MessageResponse>

    @DELETE("favorites/{adId}")
    suspend fun removeFavorite(@Path("adId") adId: String): Response<MessageResponse>

    // ==========================================
    // Contact Requests
    // ==========================================
    @POST("contact-requests/")
    suspend fun createContactRequest(
        @Query("ad_id") adId: String,
        @Body request: ContactRequestCreate
    ): Response<ContactRequestResponse>

    @GET("contact-requests/my")
    suspend fun getMyContactRequests(): Response<List<ContactRequestResponse>>

    // ==========================================
    // Profile
    // ==========================================
    @GET("profile/me")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("profile/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): Response<ProfileResponse>

    @Multipart
    @POST("profile/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<AvatarResponse>

    @POST("profile/me/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    @POST("profile/me/push-tokens")
    suspend fun registerPushToken(@Body request: PushTokenRegisterRequest): Response<MessageResponse>

    @DELETE("profile/me/push-tokens/{token}")
    suspend fun unregisterPushToken(@Path("token") token: String): Response<MessageResponse>
}

// Дополнительная модель для "Мои объявления"
@Serializable
data class AdMyListResponse(
    val id: String,
    val type: String,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("region_name") val regionName: String? = null,
    val title: String,
    val price: Double? = null,
    val status: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("moderation_comment") val moderationComment: String? = null,
    @SerialName("moderation_feedback") val moderationFeedback: ModerationFeedbackResponse? = null,
    @SerialName("boost_level") val boostLevel: String = "none",
    @SerialName("boost_expires_at") val boostExpiresAt: String? = null
)
