// ARCHIVED 2026-06-12: cut from app/src/main/java/ru/agromarket/ui/components/LoadingState.kt
// Used only by LandsScreen, which became unreachable after the lands button was removed
// from the feed top bar. Restore into LoadingState.kt together with LandsScreen/LandCard.
package ru.agromarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
