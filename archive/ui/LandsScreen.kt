package ru.agromarket.ui.lands

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terrain
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
import ru.agromarket.data.model.AdListResponse
import ru.agromarket.data.model.LAND_CATEGORY_ID
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.EmptyState
import ru.agromarket.ui.components.ErrorBanner
import ru.agromarket.ui.components.LandCard
import ru.agromarket.ui.components.LandsLoadingState
import javax.inject.Inject

@HiltViewModel
class LandsViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var lands by mutableStateOf<List<AdListResponse>>(emptyList())
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)

    init { load() }

    fun load(refresh: Boolean = false) {
        if (refresh) currentPage = 1
        isLoading = true
        viewModelScope.launch {
            error = null
            when (val result = repository.getFeed(page = currentPage, categoryId = LAND_CATEGORY_ID)) {
                is ApiResult.Success -> {
                    lands = if (refresh || currentPage == 1) result.data.items else lands + result.data.items
                    totalPages = result.data.totalPages
                }
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadMore() { if (currentPage < totalPages && !isLoading) { currentPage++; load() } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandsScreen(onAdClick: (String) -> Unit, onBack: (() -> Unit)? = null, viewModel: LandsViewModel = hiltViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Земли СХ назначения", onBack = onBack)

        viewModel.error?.let { message ->
            ErrorBanner(message = message, modifier = Modifier.padding(12.dp))
        }

        when {
            viewModel.isLoading && viewModel.lands.isEmpty() -> LandsLoadingState(modifier = Modifier.fillMaxSize())
            viewModel.lands.isEmpty() -> EmptyState(
                title = "Объявлений о земле пока нет",
                subtitle = "Здесь появятся предложения о продаже\nи аренде земельных участков",
                icon = Icons.Outlined.Terrain,
            )
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(viewModel.lands, key = { it.id }) { ad ->
                    LandCard(ad = ad, onClick = { onAdClick(ad.id) }, modifier = Modifier.animateItemPlacement())
                }
                if (viewModel.currentPage < viewModel.totalPages) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                        }
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                    }
                }
            }
        }
    }
}
