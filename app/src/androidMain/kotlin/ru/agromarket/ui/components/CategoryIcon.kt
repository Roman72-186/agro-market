package ru.agromarket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.agromarket.ui.theme.AgroMarketTheme

/** Maps a quick-category / category name to a Material Symbols Outlined icon. */
fun categoryIconFor(categoryName: String?): ImageVector = when (categoryName) {
    "Техника" -> Icons.Outlined.Agriculture
    "Запчасти" -> Icons.Outlined.Build
    "Семена" -> Icons.Outlined.Grass
    "Удобрения" -> Icons.Outlined.Science
    "Корма" -> Icons.Outlined.Grain
    "Животные" -> Icons.Outlined.Pets
    "Земля" -> Icons.Outlined.Terrain
    "Оборудование" -> Icons.Outlined.Construction
    "Прочее" -> Icons.Outlined.Inventory2
    else -> Icons.Outlined.Category
}

/** Round tinted badge with the category icon, used on category chips/cards. */
@Composable
fun CategoryIcon(categoryName: String?, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIconFor(categoryName),
            contentDescription = categoryName,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryIconPreview() {
    AgroMarketTheme {
        Box(modifier = Modifier.size(48.dp)) {
            CategoryIcon(categoryName = "Техника")
        }
    }
}
