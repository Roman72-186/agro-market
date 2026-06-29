package ru.agromarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import ru.agromarket.data.model.AdListResponse
import ru.agromarket.ui.theme.AgroAccentClay
import ru.agromarket.ui.theme.AgroCream
import ru.agromarket.ui.theme.AgroMarketTheme
import ru.agromarket.ui.theme.JetBrainsMono
import java.text.NumberFormat
import java.util.Locale

/**
 * Boost tier 0..3 from the API `boost_level` string. The backend currently sends
 * "none"; numeric and named values are mapped defensively so a new backend value
 * degrades to "no boost" rather than crashing or mislabeling.
 */
fun boostTier(boostLevel: String?): Int = when (boostLevel?.lowercase()) {
    null, "", "none" -> 0
    "1", "top", "raise", "top_7days", "top_30days" -> 1
    "2", "highlight", "premium" -> 2
    "3", "xl" -> 3
    else -> boostLevel.toIntOrNull()?.coerceIn(0, 3) ?: 0
}

/**
 * Feed/list ad card: full-width 16:9 photo with type badge and optional favorite
 * heart overlaid, a price overlay with a gradient scrim, then title and region below.
 *
 * Boosted ads (boost_level): tier 1 gets a quiet "↑ В топе" clay tag on the photo
 * (Agriaffaires top-of-list pattern); tiers 2-3 add a thin clay border and a cream
 * container. The feed is a single full-width column, so the XL tier reuses the
 * tier-2 emphasis instead of a separate layout.
 */
@Composable
fun AdCard(
    ad: AdListResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    showStatusBadge: Boolean = false,
) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    val tier = boostTier(ad.boostLevel)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (tier >= 2) BorderStroke(1.5.dp, AgroAccentClay) else null,
        colors = if (tier >= 2) CardDefaults.cardColors(containerColor = AgroCream) else CardDefaults.cardColors(),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (ad.photoUrl != null) {
                    AsyncImage(
                        model = ad.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // Type / status badges, top-start
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val (typeLabel, typeTone) = adTypeBadge(ad.type)
                    StatusBadge(text = typeLabel, tone = typeTone)
                    if (showStatusBadge) {
                        val (statusLabel, statusTone) = adStatusBadge(ad.status)
                        StatusBadge(text = statusLabel, tone = statusTone)
                    }
                    // Paid-boost tag: honest clay label, never disguised as an organic badge
                    if (tier >= 1) {
                        Surface(shape = MaterialTheme.shapes.small, color = AgroAccentClay, contentColor = Color.White) {
                            Text(
                                text = "↑ В топе",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Favorite heart, top-end
                if (onFavoriteClick != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                    ) {
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                                tint = Color.White,
                            )
                        }
                    }
                }

                // Price overlay with gradient scrim, bottom
                ad.price?.let { price ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                ),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "${priceFormat.format(price)} ₽",
                            color = Color.White,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = ad.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = ad.regionName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (ad.sellerIsPro) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Проверенный продавец",
                            modifier = Modifier.size(14.dp),
                            tint = AgroAccentClay,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Проверенный",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroAccentClay,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdCardPreview() {
    AgroMarketTheme {
        AdCard(
            ad = AdListResponse(
                id = "1",
                type = "sale",
                categoryId = 1,
                categoryName = "Техника",
                regionId = 1,
                regionName = "Краснодарский край",
                title = "Трактор МТЗ-82.1, 2018 г., в отличном состоянии",
                price = 1250000.0,
                boostLevel = "none",
                status = "active",
                photoUrl = null,
                createdAt = "",
            ),
            onClick = {},
            isFavorite = true,
            onFavoriteClick = {},
        )
    }
}
