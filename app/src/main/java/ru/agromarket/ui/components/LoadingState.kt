package ru.agromarket.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.agromarket.ui.theme.AgroMarketTheme

/** Animated shimmer brush for skeleton placeholders. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1100, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "shimmerTranslate",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.6f), base.copy(alpha = 0.2f), base.copy(alpha = 0.6f)),
        start = Offset(translate - 400f, 0f),
        end = Offset(translate, 0f),
    )
}

/** Skeleton placeholder shaped like [AdCard]: image block + two text lines. */
@Composable
fun AdCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(brush))
            Column(modifier = Modifier.padding(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.85f).height(18.dp).background(brush, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).background(brush, RoundedCornerShape(4.dp)))
            }
        }
    }
}

/** List of [AdCardSkeleton] shown while the feed is loading. */
@Composable
fun FeedLoadingState(modifier: Modifier = Modifier, count: Int = 4) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(count) { AdCardSkeleton() }
    }
}

/** Skeleton placeholder shaped like [LandCard]: a tall photo block with rounded corners. */
@Composable
fun LandCardSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Box(modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f).background(brush, RoundedCornerShape(20.dp)))
}

/** List of [LandCardSkeleton] shown while land listings are loading. */
@Composable
fun LandsLoadingState(modifier: Modifier = Modifier, count: Int = 3) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(count) { LandCardSkeleton() }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedLoadingStatePreview() {
    AgroMarketTheme {
        FeedLoadingState(count = 2)
    }
}
