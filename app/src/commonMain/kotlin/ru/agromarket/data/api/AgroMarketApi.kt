package ru.agromarket.data.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.agromarket.data.model.*

/** Фото для multipart-загрузки в commonMain (вместо java.io.File): байты + имя файла. */
data class PhotoUpload(val bytes: ByteArray, val filename: String)

/**
 * Тонкий клиент REST API поверх Ktor [HttpClient] (был Retrofit-интерфейс).
 *
 * Каждый метод возвращает «сырой» [HttpResponse] — десериализация и обработка ошибок живут в
 * [ru.agromarket.data.repository.AgroRepository.safeCall]. При expectSuccess=true вызов метода сам
 * бросает ResponseException на не-2xx (его и ловит safeCall).
 *
 * Пути относительные — базовый URL задан через defaultRequest в [createHttpClient].
 * JSON-тела сериализуются ContentNegotiation; для них явно ставим Content-Type application/json.
 */
class AgroMarketApi(private val client: HttpClient) {

    // ==========================================
    // Auth
    // ==========================================
    suspend fun register(request: RegisterRequest): HttpResponse =
        client.post("auth/register") { jsonBody(request) }

    suspend fun verify(request: VerifyCodeRequest): HttpResponse =
        client.post("auth/verify") { jsonBody(request) }

    suspend fun setPassword(request: SetPasswordRequest): HttpResponse =
        client.post("auth/set-password") { jsonBody(request) }

    suspend fun login(request: LoginRequest): HttpResponse =
        client.post("auth/login") { jsonBody(request) }

    suspend fun forgotPassword(request: ForgotPasswordRequest): HttpResponse =
        client.post("auth/forgot-password") { jsonBody(request) }

    suspend fun resetPassword(request: ResetPasswordRequest): HttpResponse =
        client.post("auth/reset-password") { jsonBody(request) }

    suspend fun refreshToken(request: RefreshTokenRequest): HttpResponse =
        client.post("auth/refresh") { jsonBody(request) }

    // ==========================================
    // Categories
    // ==========================================
    suspend fun getCategories(): HttpResponse = client.get("categories/")

    suspend fun getSubcategories(categoryId: Int): HttpResponse =
        client.get("categories/$categoryId/subcategories")

    // ==========================================
    // Geography
    // ==========================================
    suspend fun getRegions(): HttpResponse = client.get("geo/regions")

    suspend fun getDistricts(regionId: Int): HttpResponse =
        client.get("geo/regions/$regionId/districts")

    suspend fun getLocalities(districtId: Int): HttpResponse =
        client.get("geo/districts/$districtId/localities")

    suspend fun searchLocality(query: String): HttpResponse =
        client.get("geo/search") { parameter("q", query) }

    // ==========================================
    // Ads
    // ==========================================
    suspend fun getFeed(
        page: Int = 1,
        pageSize: Int = 20,
        type: String? = null,
        categoryId: Int? = null,
        regionId: Int? = null,
        search: String? = null,
        sort: String? = null,
    ): HttpResponse = client.get("ads/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("type", type)
        parameter("category_id", categoryId)
        parameter("region_id", regionId)
        parameter("search", search)
        parameter("sort", sort)
    }

    suspend fun getMyAds(): HttpResponse = client.get("ads/my")

    suspend fun getAdDetail(adId: String): HttpResponse = client.get("ads/$adId")

    suspend fun createAd(request: AdCreateRequest): HttpResponse =
        client.post("ads/") { jsonBody(request) }

    suspend fun updateAd(adId: String, request: AdCreateRequest): HttpResponse =
        client.put("ads/$adId") { jsonBody(request) }

    suspend fun deleteAd(adId: String): HttpResponse = client.delete("ads/$adId")

    suspend fun uploadPhotos(adId: String, photos: List<PhotoUpload>): HttpResponse =
        client.post("ads/$adId/photos") {
            setBody(MultiPartFormDataContent(formData {
                photos.forEach { photo ->
                    append("files", photo.bytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/*")
                        append(HttpHeaders.ContentDisposition, "filename=\"${photo.filename}\"")
                    })
                }
            }))
        }

    suspend fun deletePhoto(adId: String, photoId: Int): HttpResponse =
        client.delete("ads/$adId/photos/$photoId")

    suspend fun submitAd(adId: String): HttpResponse = client.post("ads/$adId/submit")

    // ==========================================
    // Monetization — Платежи (boost через ЮKassa)
    // ==========================================
    suspend fun createPayment(request: CreatePaymentRequest): HttpResponse =
        client.post("payments/create") { jsonBody(request) }

    // ==========================================
    // Monetization — Pro-подписка (Фаза 2, отложена — мок)
    // ==========================================
    suspend fun getSubscriptionPlans(): HttpResponse = client.get("subscriptions/plans")

    suspend fun getMySubscription(): HttpResponse = client.get("profile/me/subscription")

    suspend fun subscribe(request: SubscribeRequest): HttpResponse =
        client.post("profile/me/subscription") { jsonBody(request) }

    // ==========================================
    // Favorites
    // ==========================================
    suspend fun getFavorites(): HttpResponse = client.get("favorites/")

    suspend fun addFavorite(adId: String): HttpResponse = client.post("favorites/$adId")

    suspend fun removeFavorite(adId: String): HttpResponse = client.delete("favorites/$adId")

    // ==========================================
    // Contact Requests
    // ==========================================
    suspend fun createContactRequest(adId: String, request: ContactRequestCreate): HttpResponse =
        client.post("contact-requests/") {
            parameter("ad_id", adId)
            jsonBody(request)
        }

    suspend fun getMyContactRequests(): HttpResponse = client.get("contact-requests/my")

    // ==========================================
    // Profile
    // ==========================================
    suspend fun getProfile(): HttpResponse = client.get("profile/me")

    suspend fun updateProfile(request: ProfileUpdateRequest): HttpResponse =
        client.put("profile/me") { jsonBody(request) }

    suspend fun uploadAvatar(bytes: ByteArray, filename: String): HttpResponse =
        client.post("profile/me/avatar") {
            setBody(MultiPartFormDataContent(formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/*")
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }))
        }

    suspend fun changePassword(request: ChangePasswordRequest): HttpResponse =
        client.post("profile/me/change-password") { jsonBody(request) }

    suspend fun registerPushToken(request: PushTokenRegisterRequest): HttpResponse =
        client.post("profile/me/push-tokens") { jsonBody(request) }

    suspend fun unregisterPushToken(token: String): HttpResponse =
        client.delete("profile/me/push-tokens/$token")

    /**
     * Сбрасывает кэш токенов Ktor `Auth(bearer)` (AuthTokenHolder кэширует результат loadTokens,
     * включая null). Вызывается после логина/логаута, чтобы следующий запрос перечитал актуальные
     * токены из [TokenProvider] — иначе после логина первый запрос ушёл бы без заголовка и словил 401.
     */
    fun clearTokenCache() {
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }
}

/** Ставит JSON-тело и Content-Type application/json (ContentNegotiation сериализует объект). */
private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: Any) {
    contentType(ContentType.Application.Json)
    setBody(body)
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
