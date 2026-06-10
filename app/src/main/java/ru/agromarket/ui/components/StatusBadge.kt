package ru.agromarket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.agromarket.ui.theme.AgroDanger
import ru.agromarket.ui.theme.AgroInfo
import ru.agromarket.ui.theme.AgroMarketTheme
import ru.agromarket.ui.theme.AgroOnInfoBg
import ru.agromarket.ui.theme.AgroSuccess
import ru.agromarket.ui.theme.AgroWarning

enum class BadgeTone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL }

/** Small colored pill used for ad type and ad status labels. */
@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val (containerColor, contentColor) = when (tone) {
        BadgeTone.SUCCESS -> AgroSuccess to Color.White
        BadgeTone.WARNING -> AgroWarning to Color.White
        BadgeTone.DANGER -> AgroDanger to Color.White
        BadgeTone.INFO -> AgroInfo to AgroOnInfoBg
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = containerColor, contentColor = contentColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Label + tone for an ad type (`sell`/`service`/`land`). */
fun adTypeBadge(type: String): Pair<String, BadgeTone> = when (type) {
    "sell" -> "Продажа" to BadgeTone.SUCCESS
    "service" -> "Агроуслуги" to BadgeTone.WARNING
    "land" -> "Земля" to BadgeTone.INFO
    else -> type to BadgeTone.NEUTRAL
}

/** Label + tone for an ad moderation status. */
fun adStatusBadge(status: String): Pair<String, BadgeTone> = when (status) {
    "draft" -> "Черновик" to BadgeTone.NEUTRAL
    "pending_moderation" -> "На модерации" to BadgeTone.INFO
    "active" -> "Активно" to BadgeTone.SUCCESS
    "rejected" -> "Отклонено" to BadgeTone.DANGER
    else -> status to BadgeTone.NEUTRAL
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    AgroMarketTheme {
        Surface {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                StatusBadge("Продажа", BadgeTone.SUCCESS)
                StatusBadge("Агроуслуги", BadgeTone.WARNING)
                StatusBadge("Земля", BadgeTone.INFO)
                StatusBadge("Отклонено", BadgeTone.DANGER)
                StatusBadge("Черновик", BadgeTone.NEUTRAL)
            }
        }
    }
}
