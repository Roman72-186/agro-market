package ru.agromarket.data.repository

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.api.AdMyListResponse
import ru.agromarket.data.api.PhotoUpload
import ru.agromarket.data.api.PushTokenProvider
import ru.agromarket.data.api.TokenProvider
import ru.agromarket.data.model.*

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

class AgroRepository(
    private val api: AgroMarketApi,
    private val tokenProvider: TokenProvider,
    private val pushTokenProvider: PushTokenProvider,
) {
    suspend fun register(email: String): ApiResult<MessageResponse> = safeCall {
        api.register(RegisterRequest(email))
    }

    suspend fun verify(email: String, code: String): ApiResult<MessageResponse> = safeCall {
        api.verify(VerifyCodeRequest(email, code))
    }

    suspend fun setPassword(email: String, code: String, password: String): ApiResult<LoginResponse> {
        val result = safeCall<LoginResponse> { api.setPassword(SetPasswordRequest(email, code, password)) }
        if (result is ApiResult.Success) {
            saveSession(result.data.accessToken, result.data.refreshToken)
            currentPushToken()?.let { registerPushToken(it) }
        }
        return result
    }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        val result = safeCall<LoginResponse> { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success) {
            saveSession(result.data.accessToken, result.data.refreshToken)
            currentPushToken()?.let { registerPushToken(it) }
        }
        return result
    }

    suspend fun forgotPassword(email: String): ApiResult<MessageResponse> = safeCall {
        api.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<MessageResponse> = safeCall {
        api.resetPassword(ResetPasswordRequest(email, code, newPassword))
    }

    suspend fun logout() {
        currentPushToken()?.let { unregisterPushToken(it) }
        tokenProvider.clear()
        // Сбросить кэш Auth-плагина, иначе он продолжит слать старый токен до первого 401.
        api.clearTokenCache()
    }

    suspend fun getCategories(): ApiResult<List<CategoryTreeResponse>> = safeCall { api.getCategories() }
    suspend fun getRegions(): ApiResult<List<RegionResponse>> = safeCall { api.getRegions() }
    suspend fun getDistricts(regionId: Int): ApiResult<List<DistrictResponse>> = safeCall { api.getDistricts(regionId) }
    suspend fun getLocalities(districtId: Int): ApiResult<List<LocalityResponse>> = safeCall { api.getLocalities(districtId) }

    suspend fun getFeed(
        page: Int = 1, type: String? = null, categoryId: Int? = null, regionId: Int? = null, search: String? = null, sort: String? = null
    ): ApiResult<AdFeedResponse> = safeCall {
        api.getFeed(page, 20, type, categoryId, regionId, search, sort)
    }

    suspend fun getAdDetail(adId: String): ApiResult<AdDetailResponse> = safeCall { api.getAdDetail(adId) }

    suspend fun getMyAds(): ApiResult<List<AdMyListResponse>> = safeCall { api.getMyAds() }

    suspend fun createAd(request: AdCreateRequest): ApiResult<AdDetailResponse> = safeCall { api.createAd(request) }
    suspend fun updateAd(adId: String, request: AdCreateRequest): ApiResult<AdDetailResponse> = safeCall { api.updateAd(adId, request) }

    suspend fun uploadPhotos(adId: String, photos: List<PhotoUpload>): ApiResult<List<AdPhotoResponse>> =
        safeCall { api.uploadPhotos(adId, photos) }

    suspend fun deletePhoto(adId: String, photoId: Int): ApiResult<MessageResponse> = safeCall { api.deletePhoto(adId, photoId) }

    suspend fun submitAd(adId: String): ApiResult<AdDetailResponse> = safeCall { api.submitAd(adId) }
    suspend fun deleteAd(adId: String): ApiResult<MessageResponse> = safeCall { api.deleteAd(adId) }

    // ---- Monetization: Платежи (boost через ЮKassa) ----
    /** Создать платёж boost. Возвращает PaymentResponse с confirmation_url для оплаты. */
    suspend fun createPayment(adId: String, type: String): ApiResult<PaymentResponse> =
        safeCall { api.createPayment(CreatePaymentRequest(adId, type)) }

    // ---- Monetization: Pro-подписка (Фаза 2, отложена — мок) ----
    suspend fun getSubscriptionPlans(): ApiResult<SubscriptionPlansResponse> {
        val result = safeCall<SubscriptionPlansResponse> { api.getSubscriptionPlans() }
        return if (result is ApiResult.Success && result.data.plans.isNotEmpty()) result
        else ApiResult.Success(MonetizationCatalog.DEFAULT_PLANS)
    }

    suspend fun getMySubscription(): ApiResult<SubscriptionResponse> = safeCall { api.getMySubscription() }

    /** Оформить подписку (grant). Сейчас вызывается напрямую из имитации оплаты. */
    suspend fun subscribe(plan: String, months: Int): ApiResult<SubscriptionResponse> =
        safeCall { api.subscribe(SubscribeRequest(plan, months)) }

    suspend fun getFavorites(): ApiResult<List<FavoriteResponse>> = safeCall { api.getFavorites() }
    suspend fun addFavorite(adId: String): ApiResult<MessageResponse> = safeCall { api.addFavorite(adId) }
    suspend fun removeFavorite(adId: String): ApiResult<MessageResponse> = safeCall { api.removeFavorite(adId) }

    suspend fun createContactRequest(adId: String, message: String? = null): ApiResult<ContactRequestResponse> = safeCall {
        api.createContactRequest(adId, ContactRequestCreate(message))
    }

    suspend fun getMyContactRequests(): ApiResult<List<ContactRequestResponse>> = safeCall { api.getMyContactRequests() }

    suspend fun getProfile(): ApiResult<ProfileResponse> = safeCall { api.getProfile() }
    suspend fun updateProfile(request: ProfileUpdateRequest): ApiResult<ProfileResponse> = safeCall { api.updateProfile(request) }

    suspend fun uploadAvatar(bytes: ByteArray, filename: String): ApiResult<AvatarResponse> =
        safeCall { api.uploadAvatar(bytes, filename) }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<MessageResponse> = safeCall {
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    suspend fun registerPushToken(token: String): ApiResult<MessageResponse> = safeCall { api.registerPushToken(PushTokenRegisterRequest(token, "android")) }
    suspend fun unregisterPushToken(token: String): ApiResult<MessageResponse> = safeCall { api.unregisterPushToken(token) }

    /** Сохранить токены и сбросить кэш Auth-плагина, чтобы первый же запрос ушёл уже с токеном. */
    private suspend fun saveSession(access: String, refresh: String) {
        tokenProvider.saveTokens(access, refresh)
        api.clearTokenCache()
    }

    /**
     * Current push registration token, or null if push isn't available (e.g. no
     * google-services.json yet) or the call otherwise fails.
     */
    private suspend fun currentPushToken(): String? = pushTokenProvider.currentToken()

    /**
     * Оборачивает сетевой вызов в [ApiResult]. При expectSuccess=true не-2xx бросает
     * [ResponseException] — из неё берём код и тело для [parseErrorDetail]. Успех с пустым телом
     * (десериализация не удалась) — [ApiResult.Error] «Пустой ответ сервера». Прочее → ошибка соединения.
     */
    suspend inline fun <reified T> safeCall(call: () -> HttpResponse): ApiResult<T> {
        return try {
            val response = call()
            try {
                ApiResult.Success(response.body<T>())
            } catch (_: Exception) {
                emptyBodyError(response)
            }
        } catch (e: ResponseException) {
            responseError(e)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Ошибка соединения")
        }
    }

    @PublishedApi
    internal fun emptyBodyError(response: HttpResponse): ApiResult<Nothing> =
        ApiResult.Error("Пустой ответ сервера", response.status.value)

    @PublishedApi
    internal suspend fun responseError(e: ResponseException): ApiResult<Nothing> {
        val code = e.response.status.value
        val detail = try {
            val root = ApiJson.parseToJsonElement(e.response.bodyAsText()).jsonObject["detail"]
            parseErrorDetail(root)
        } catch (_: Exception) { null }
        return ApiResult.Error(detail ?: "Ошибка сервера", code)
    }

    /**
     * FastAPI/Pydantic validation errors (422) return `detail` as a list of
     * `{loc, msg, type, input}` objects rather than a string, e.g.
     * `[{"type": "enum", "loc": ["body", "type"], "msg": "Input should be 'sale'..."}]`.
     * Turn that into a readable "field: message" string instead of a raw map dump.
     */
    private fun parseErrorDetail(detail: JsonElement?): String? = when (detail) {
        null -> null
        is JsonPrimitive -> if (detail.isString) detail.content else detail.toString()
        is JsonArray -> detail.mapNotNull { item ->
            val obj = item as? JsonObject
                ?: return@mapNotNull (item as? JsonPrimitive)?.content ?: item.toString()
            val field = (obj["loc"] as? JsonArray)?.lastOrNull()
                ?.let { (it as? JsonPrimitive)?.content }
            val msg = (obj["msg"] as? JsonPrimitive)?.content
            when {
                field != null && msg != null -> "$field: $msg"
                msg != null -> msg
                else -> obj.toString()
            }
        }.joinToString("; ").ifBlank { null }
        else -> detail.toString()
    }
}
