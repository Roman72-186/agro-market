package ru.agromarket.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** "Земельные участки" category id in the live backend category tree (GET /categories/). */
const val LAND_CATEGORY_ID = 56

// ==========================================
// Auth
// ==========================================
data class RegisterRequest(val email: String)
data class VerifyCodeRequest(val email: String, val code: String)
data class SetPasswordRequest(val email: String, val code: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)
data class RefreshTokenRequest(@SerializedName("refresh_token") val refreshToken: String)
data class ForgotPasswordRequest(val email: String)
data class ResetPasswordRequest(val email: String, val code: String, val password: String)
data class MessageResponse(val message: String)

// ==========================================
// Category
// ==========================================
data class CategoryResponse(
    val id: Int,
    val name: String,
    val icon: String? = null,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("parent_id") val parentId: Int? = null
)

data class CategoryTreeResponse(
    val id: Int,
    val name: String,
    val icon: String? = null,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    val children: List<CategoryResponse> = emptyList()
)

// ==========================================
// Geography
// ==========================================
data class RegionResponse(val id: Int, val name: String)
data class DistrictResponse(val id: Int, val name: String, @SerializedName("region_id") val regionId: Int)
data class LocalityResponse(val id: Int, val name: String, @SerializedName("district_id") val districtId: Int)

// ==========================================
// Ad
// ==========================================
data class AdPhotoResponse(val id: Int, val url: String, @SerializedName("sort_order") val sortOrder: Int)

data class AdListResponse(
    val id: String,
    val type: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("region_id") val regionId: Int,
    @SerializedName("region_name") val regionName: String? = null,
    val title: String,
    val price: BigDecimal? = null,
    @SerializedName("boost_level") val boostLevel: String,
    val status: String,
    @SerializedName("photo_url") val photoUrl: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("published_at") val publishedAt: String? = null
)

data class AdDetailResponse(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val type: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("parent_category_name") val parentCategoryName: String? = null,
    @SerializedName("region_id") val regionId: Int,
    @SerializedName("region_name") val regionName: String? = null,
    @SerializedName("district_id") val districtId: Int? = null,
    @SerializedName("district_name") val districtName: String? = null,
    @SerializedName("locality_id") val localityId: Int? = null,
    @SerializedName("locality_name") val localityName: String? = null,
    val title: String,
    val description: String? = null,
    val price: BigDecimal? = null,
    @SerializedName("phone_primary") val phonePrimary: String,
    @SerializedName("phone_secondary") val phoneSecondary: String? = null,
    val status: String,
    @SerializedName("boost_level") val boostLevel: String,
    @SerializedName("moderation_comment") val moderationComment: String? = null,
    val photos: List<AdPhotoResponse> = emptyList(),
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class AdFeedResponse(
    val items: List<AdListResponse>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class AdCreateRequest(
    val type: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("region_id") val regionId: Int,
    @SerializedName("district_id") val districtId: Int? = null,
    @SerializedName("locality_id") val localityId: Int? = null,
    val title: String,
    val description: String? = null,
    val price: BigDecimal? = null,
    @SerializedName("phone_primary") val phonePrimary: String,
    @SerializedName("phone_secondary") val phoneSecondary: String? = null
)

// ==========================================
// Profile
// ==========================================
data class ProfileResponse(
    val id: String,
    val email: String,
    val phone: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("region_id") val regionId: Int? = null,
    @SerializedName("region_name") val regionName: String? = null,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class ProfileUpdateRequest(
    val phone: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val patronymic: String? = null,
    @SerializedName("region_id") val regionId: Int? = null
)

data class ChangePasswordRequest(
    @SerializedName("old_password") val oldPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class AvatarResponse(
    @SerializedName("avatar_url") val avatarUrl: String
)

// ==========================================
// Favorites
// ==========================================
data class FavoriteResponse(
    val id: Int,
    @SerializedName("ad_id") val adId: String,
    @SerializedName("ad_title") val adTitle: String? = null,
    @SerializedName("ad_photo_url") val adPhotoUrl: String? = null,
    @SerializedName("ad_price") val adPrice: BigDecimal? = null,
    @SerializedName("ad_status") val adStatus: String? = null,
    @SerializedName("created_at") val createdAt: String
)

// ==========================================
// Contact Requests
// ==========================================
data class ContactRequestCreate(val message: String? = null)

data class ContactRequestResponse(
    val id: String,
    @SerializedName("requester_id") val requesterId: String,
    @SerializedName("ad_id") val adId: String,
    @SerializedName("ad_title") val adTitle: String? = null,
    val status: String,
    @SerializedName("admin_price") val adminPrice: BigDecimal? = null,
    @SerializedName("admin_comment") val adminComment: String? = null,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("created_at") val createdAt: String
)
