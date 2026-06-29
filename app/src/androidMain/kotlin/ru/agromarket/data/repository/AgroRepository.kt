package ru.agromarket.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.api.TokenManager
import ru.agromarket.data.model.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

@Singleton
class AgroRepository @Inject constructor(
    private val api: AgroMarketApi,
    private val tokenManager: TokenManager
) {
    suspend fun register(email: String): ApiResult<MessageResponse> = safeCall {
        api.register(RegisterRequest(email))
    }

    suspend fun verify(email: String, code: String): ApiResult<MessageResponse> = safeCall {
        api.verify(VerifyCodeRequest(email, code))
    }

    suspend fun setPassword(email: String, code: String, password: String): ApiResult<LoginResponse> {
        val result = safeCall { api.setPassword(SetPasswordRequest(email, code, password)) }
        if (result is ApiResult.Success) {
            tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
            currentPushToken()?.let { registerPushToken(it) }
        }
        return result
    }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        val result = safeCall { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success) {
            tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
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
        tokenManager.clear()
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

    suspend fun getMyAds(): ApiResult<List<ru.agromarket.data.api.AdMyListResponse>> = safeCall { api.getMyAds() }

    suspend fun createAd(request: AdCreateRequest): ApiResult<AdDetailResponse> = safeCall { api.createAd(request) }
    suspend fun updateAd(adId: String, request: AdCreateRequest): ApiResult<AdDetailResponse> = safeCall { api.updateAd(adId, request) }

    suspend fun uploadPhotos(adId: String, files: List<File>): ApiResult<List<AdPhotoResponse>> = safeCall {
        val parts = files.map { file ->
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, requestBody)
        }
        api.uploadPhotos(adId, parts)
    }

    suspend fun deletePhoto(adId: String, photoId: Int): ApiResult<MessageResponse> = safeCall { api.deletePhoto(adId, photoId) }

    suspend fun submitAd(adId: String): ApiResult<AdDetailResponse> = safeCall { api.submitAd(adId) }
    suspend fun deleteAd(adId: String): ApiResult<MessageResponse> = safeCall { api.deleteAd(adId) }

    // ---- Monetization: Платежи (boost через ЮKassa) ----
    /** Создать платёж boost. Возвращает PaymentResponse с confirmation_url для оплаты. */
    suspend fun createPayment(adId: String, type: String): ApiResult<PaymentResponse> =
        safeCall { api.createPayment(CreatePaymentRequest(adId, type)) }

    // ---- Monetization: Pro-подписка (Фаза 2, отложена — мок) ----
    suspend fun getSubscriptionPlans(): ApiResult<SubscriptionPlansResponse> {
        val result = safeCall { api.getSubscriptionPlans() }
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

    suspend fun uploadAvatar(file: File): ApiResult<AvatarResponse> = safeCall {
        val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        api.uploadAvatar(part)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<MessageResponse> = safeCall {
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    suspend fun registerPushToken(token: String): ApiResult<MessageResponse> = safeCall { api.registerPushToken(PushTokenRegisterRequest(token, "android")) }
    suspend fun unregisterPushToken(token: String): ApiResult<MessageResponse> = safeCall { api.unregisterPushToken(token) }

    /**
     * Current FCM registration token, or null if Firebase isn't initialized (e.g. no
     * google-services.json yet) or the call otherwise fails.
     */
    private suspend fun currentPushToken(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (_: Exception) { null }

    private suspend fun <T> safeCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Error("Пустой ответ сервера", response.code())
            } else {
                val errorBody = response.errorBody()?.string()
                val detail = try {
                    val root = ApiJson.parseToJsonElement(errorBody!!).jsonObject["detail"]
                    parseErrorDetail(root)
                } catch (_: Exception) { null }
                ApiResult.Error(detail ?: "Ошибка сервера", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Ошибка соединения")
        }
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
