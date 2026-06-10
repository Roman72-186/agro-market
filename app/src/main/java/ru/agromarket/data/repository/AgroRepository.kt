package ru.agromarket.data.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
        }
        return result
    }

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        val result = safeCall { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success) {
            tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    suspend fun forgotPassword(email: String): ApiResult<MessageResponse> = safeCall {
        api.forgotPassword(ForgotPasswordRequest(email))
    }

    suspend fun logout() { tokenManager.clear() }

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

    suspend fun uploadPhotos(adId: String, files: List<File>): ApiResult<List<AdPhotoResponse>> = safeCall {
        val parts = files.map { file ->
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, requestBody)
        }
        api.uploadPhotos(adId, parts)
    }

    suspend fun submitAd(adId: String): ApiResult<AdDetailResponse> = safeCall { api.submitAd(adId) }
    suspend fun deleteAd(adId: String): ApiResult<MessageResponse> = safeCall { api.deleteAd(adId) }

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

    suspend fun changePassword(oldPassword: String, newPassword: String): ApiResult<MessageResponse> = safeCall {
        api.changePassword(ChangePasswordRequest(oldPassword, newPassword))
    }

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
                    com.google.gson.Gson().fromJson(errorBody, Map::class.java)?.get("detail")?.toString()
                } catch (_: Exception) { null }
                ApiResult.Error(detail ?: "Ошибка сервера", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Ошибка соединения")
        }
    }
}
