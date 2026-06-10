package ru.agromarket.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.model.FavoriteResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.EmptyState
import ru.agromarket.ui.components.FavoriteRow
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(onAdClick: (String) -> Unit, viewModel: FavoritesViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Избранное")
        when {
            viewModel.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            viewModel.favorites.isEmpty() -> EmptyState(
                title = "Нет избранных",
                subtitle = "Объявления, которые вам понравились, появятся здесь",
                icon = Icons.Outlined.FavoriteBorder,
            )
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.favorites, key = { it.id }) { fav ->
                    FavoriteRow(
                        favorite = fav,
                        onClick = { onAdClick(fav.adId) },
                        onRemove = { viewModel.remove(fav.adId) },
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
            }
        }
    }
}
