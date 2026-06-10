package ru.agromarket.ui.lands

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.agromarket.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandsScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Земли СХ назначения", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroGreen, titleContentColor = MaterialTheme.colorScheme.onPrimary)
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Landscape, null, modifier = Modifier.size(64.dp), tint = AgroGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Раздел в разработке", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AgroGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Здесь будут объявления о продаже\nи аренде земельных участков", style = MaterialTheme.typography.bodyMedium, color = AgroGray)
            }
        }
    }
}
