package ru.agromarket.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.model.FavoriteResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val repository: AgroRepository) : ViewModel() {
    var favorites by mutableStateOf<List<FavoriteResponse>>(emptyList())
    var isLoading by mutableStateOf(true)
    init { load() }
    fun load() {
        viewModelScope.launch {
            isLoading = true
            (repository.getFavorites() as? ApiResult.Success)?.let { favorites = it.data }
            isLoading = false
        }
    }
    fun remove(adId: String) {
        viewModelScope.launch { repository.removeFavorite(adId); favorites = favorites.filter { it.adId != adId } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onAdClick: (String) -> Unit, viewModel: FavoritesViewModel = hiltViewModel()) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Избранное", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroGreen, titleContentColor = MaterialTheme.colorScheme.onPrimary))
        when {
            viewModel.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AgroGreen) }
            viewModel.favorites.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(64.dp), tint = AgroGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Нет избранных", style = MaterialTheme.typography.titleMedium, color = AgroGray)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.favorites, key = { it.id }) { fav ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onAdClick(fav.adId) }, shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = fav.adPhotoUrl, contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.adTitle ?: "Объявление", fontWeight = FontWeight.SemiBold, maxLines = 2)
                                fav.adPrice?.let { Text("${priceFormat.format(it)} \u20BD", color = AgroGreen, fontWeight = FontWeight.Bold) }
                            }
                            IconButton(onClick = { viewModel.remove(fav.adId) }) { Icon(Icons.Default.Favorite, null, tint = AgroRed) }
                        }
                    }
                }
            }
        }
    }
}
