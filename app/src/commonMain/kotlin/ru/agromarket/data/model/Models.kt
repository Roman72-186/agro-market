package ru.agromarket.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** "Земельные участки" category id in the live backend category tree (GET /categories/). */
const val LAND_CATEGORY_ID = 56

// Денежные суммы приходят с бэкенда JSON-числом. BigDecimal — JVM-only и недоступен
// в commonMain, поэтому в DTO используется Double: над ценой нет арифметики (только
// отображение и round-trip), значения — целые рубли << 2^53, потерь точности нет.

// ==========================================
// Auth
// ==========================================
@Serializable
data class RegisterRequest(val email: String)

@Serializable
data class VerifyCodeRequest(val email: String, val code: String)

@Serializable
data class SetPasswordRequest(val email: String, val code: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    @SerialName("new_password") val newPassword: String
)

@Serializable
data class MessageResponse(val message: String)

// platform без значения по умолчанию: kotlinx с encodeDefaults=false опускает поля,
// равные дефолту, а контракт POST /profile/me/push-tokens требует platform всегда.
@Serializable
data class PushTokenRegisterRequest(val token: String, val platform: String)

// ==========================================
// Category
// ==========================================
@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("parent_id") val parentId: Int? = null
)

@Serializable
data class CategoryTreeResponse(
    val id: Int,
    val name: String,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val children: List<CategoryResponse> = emptyList()
)

// ==========================================
// Geography
// ==========================================
@Serializable
data class RegionResponse(val id: Int, val name: String)

@Serializable
data class DistrictResponse(val id: Int, val name: String, @SerialName("region_id") val regionId: Int)

@Serializable
data class LocalityResponse(val id: Int, val name: String, @SerialName("district_id") val districtId: Int)

// ==========================================
// Ad
// ==========================================
@Serializable
data class AdPhotoResponse(val id: Int, val url: String, @SerialName("sort_order") val sortOrder: Int)

/** Последний непогашенный фидбек модератора (доработка/отклонение) с фото-примерами. */
@Serializable
data class ModerationFeedbackResponse(
    val decision: String,
    val comment: String? = null,
    val photos: List<AdPhotoResponse> = emptyList(),
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AdListResponse(
    val id: String,
    val type: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("region_id") val regionId: Int,
    @SerialName("region_name") val regionName: String? = null,
    val title: String,
    val price: Double? = null,
    @SerialName("boost_level") val boostLevel: String,
    val status: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("published_at") val publishedAt: String? = null,
    /** Продавец с активной Pro-подпиской — для бейджа «Проверенный продавец». */
    @SerialName("seller_is_pro") val sellerIsPro: Boolean = false
)

@Serializable
data class AdDetailResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("parent_category_name") val parentCategoryName: String? = null,
    @SerialName("region_id") val regionId: Int,
    @SerialName("region_name") val regionName: String? = null,
    @SerialName("district_id") val districtId: Int? = null,
    @SerialName("district_name") val districtName: String? = null,
    @SerialName("locality_id") val localityId: Int? = null,
    @SerialName("locality_name") val localityName: String? = null,
    val title: String,
    val description: String? = null,
    val price: Double? = null,
    @SerialName("phone_primary") val phonePrimary: String,
    @SerialName("phone_secondary") val phoneSecondary: String? = null,
    val status: String,
    @SerialName("boost_level") val boostLevel: String,
    @SerialName("moderation_comment") val moderationComment: String? = null,
    @SerialName("moderation_feedback") val moderationFeedback: ModerationFeedbackResponse? = null,
    val photos: List<AdPhotoResponse> = emptyList(),
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("boost_expires_at") val boostExpiresAt: String? = null,
    /** Продавец с активной Pro-подпиской — для бейджа «Проверенный продавец». */
    @SerialName("seller_is_pro") val sellerIsPro: Boolean = false
)

@Serializable
data class AdFeedResponse(
    val items: List<AdListResponse>,
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_pages") val totalPages: Int
)

@Serializable
data class AdCreateRequest(
    val type: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("region_id") val regionId: Int,
    @SerialName("district_id") val districtId: Int? = null,
    @SerialName("locality_id") val localityId: Int? = null,
    val title: String,
    val description: String? = null,
    val price: Double? = null,
    @SerialName("phone_primary") val phonePrimary: String,
    @SerialName("phone_secondary") val phoneSecondary: String? = null
)

// ==========================================
// Profile
// ==========================================
@Serializable
data class ProfileResponse(
    val id: String,
    val email: String,
    val phone: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("region_id") val regionId: Int? = null,
    @SerialName("region_name") val regionName: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ProfileUpdateRequest(
    val phone: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerialName("region_id") val regionId: Int? = null
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String
)

@Serializable
data class AvatarResponse(
    @SerialName("avatar_url") val avatarUrl: String
)

// ==========================================
// Favorites
// ==========================================
@Serializable
data class FavoriteResponse(
    val id: Int,
    @SerialName("ad_id") val adId: String,
    @SerialName("ad_title") val adTitle: String? = null,
    @SerialName("ad_photo_url") val adPhotoUrl: String? = null,
    @SerialName("ad_price") val adPrice: Double? = null,
    @SerialName("ad_status") val adStatus: String? = null,
    @SerialName("created_at") val createdAt: String
)

// ==========================================
// Contact Requests
// ==========================================
@Serializable
data class ContactRequestCreate(val message: String? = null)

@Serializable
data class ContactRequestResponse(
    val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("ad_id") val adId: String,
    @SerialName("ad_title") val adTitle: String? = null,
    val status: String,
    @SerialName("admin_price") val adminPrice: Double? = null,
    @SerialName("admin_comment") val adminComment: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

// ==========================================
// Monetization — Платежи (boost через ЮKassa)
// ==========================================
/**
 * Создание платежа: `type` ∈ "boost_7" | "boost_30" | "ad_placement".
 * Контракт бэкенда: POST /payments/create.
 */
@Serializable
data class CreatePaymentRequest(
    @SerialName("ad_id") val adId: String,
    val type: String
)

/** Ответ /payments/create: confirmationUrl — ссылка на форму оплаты ЮKassa. */
@Serializable
data class PaymentResponse(
    val id: String,
    val amount: Double? = null,
    val type: String,
    val status: String,
    @SerialName("confirmation_url") val confirmationUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/** Типы платежей boost (значения из PaymentType бэкенда). */
object BoostPaymentType {
    const val BOOST_7 = "boost_7"
    const val BOOST_30 = "boost_30"
}

// ==========================================
// Monetization — Pro-подписка (Фаза 2)
// ==========================================
/** Тариф подписки. adLimit = null означает безлимит объявлений. */
@Serializable
data class SubscriptionPlan(
    val code: String,
    val name: String,
    @SerialName("price_kopecks") val priceKopecks: Long,
    @SerialName("ad_limit") val adLimit: Int? = null,
    val perks: List<String> = emptyList()
)

@Serializable
data class SubscriptionPlansResponse(
    val plans: List<SubscriptionPlan> = emptyList()
)

/** Оформление подписки: план на N месяцев. */
@Serializable
data class SubscribeRequest(val plan: String, val months: Int)

@Serializable
data class SubscriptionResponse(
    val plan: String,
    val status: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("auto_renew") val autoRenew: Boolean = false
)
