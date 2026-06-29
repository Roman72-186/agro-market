package ru.agromarket.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Step 3: pick a subcategory of the previously selected category. */
@Composable
fun CreateAdSubCategoryStep(viewModel: CreateAdViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Выберите подкатегорию:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        viewModel.selectedCategory?.children?.forEach { sub ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectSubCategory(sub) },
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sub.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
