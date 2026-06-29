package ru.agromarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.agromarket.data.model.FavoriteResponse
import ru.agromarket.ui.theme.AgroMarketTheme
import ru.agromarket.ui.theme.JetBrainsMono
import java.text.NumberFormat
import java.util.Locale

/**
 * Compact favorites-list row: thumbnail, title/price/status and a remove-favorite heart,
 * on [AdCard] tokens. Lots that are no longer active (sold/removed/rejected) are dimmed
 * so the list doesn't look fresher than it is.
 */
@Composable
fun FavoriteRow(
    favorite: FavoriteResponse,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    val inactive = favorite.adStatus != null && favorite.adStatus != "active"

    Card(
        modifier = modifier.fillMaxWidth().alpha(if (inactive) 0.6f else 1f).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium)) {
                if (favorite.adPhotoUrl != null) {
                    AsyncImage(
                        model = favorite.adPhotoUrl,
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
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.adTitle ?: "Объявление",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                favorite.adPrice?.let { price ->
                    Text(
                        text = "${priceFormat.format(price)} ₽",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                favorite.adStatus?.let { status ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val (label, tone) = adStatusBadge(status)
                    StatusBadge(text = label, tone = tone)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Убрать из избранного",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoriteRowPreview() {
    AgroMarketTheme {
        FavoriteRow(
            favorite = FavoriteResponse(
                id = 1,
                adId = "1",
                adTitle = "Трактор МТЗ-82.1, 2018 г., в отличном состоянии",
                adPhotoUrl = null,
                adPrice = 1250000.0,
                adStatus = "active",
                createdAt = "",
            ),
            onClick = {},
            onRemove = {},
        )
    }
}
