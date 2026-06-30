package ru.agromarket.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.agromarket.data.model.FavoriteResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.EmptyState
import ru.agromarket.ui.components.ErrorState
import ru.agromarket.ui.components.FavoriteRow

/** A lot is "alive" while it is active (or its status is unknown); dead lots are dimmed and grouped last. */
private fun isAlive(fav: FavoriteResponse) = fav.adStatus == null || fav.adStatus == "active"

class FavoritesViewModel(private val repository: AgroRepository) : ViewModel() {
    var favorites by mutableStateOf<List<FavoriteResponse>>(emptyList())
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    private var lastRemoved: FavoriteResponse? = null
    private var lastRemovedIndex = 0

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val r = repository.getFavorites()) {
                is ApiResult.Success -> favorites = r.data.sortedBy { !isAlive(it) }
                is ApiResult.Error -> error = r.message
            }
            isLoading = false
        }
    }

    /** Optimistic remove that keeps the item around for an undo from the snackbar. */
    fun remove(fav: FavoriteResponse) {
        lastRemoved = fav
        lastRemovedIndex = favorites.indexOf(fav).coerceAtLeast(0)
        favorites = favorites - fav
        viewModelScope.launch { repository.removeFavorite(fav.adId) }
    }

    fun undoRemove() {
        val fav = lastRemoved ?: return
        lastRemoved = null
        favorites = favorites.toMutableList().apply { add(lastRemovedIndex.coerceAtMost(size), fav) }
        viewModelScope.launch { repository.addFavorite(fav.adId) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onAdClick: (String) -> Unit,
    onGoToFeed: () -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun removeWithUndo(fav: FavoriteResponse) {
        viewModel.remove(fav)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Удалено из избранного",
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppTopBar(title = "Избранное")
            when {
                viewModel.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                viewModel.error != null -> ErrorState(
                    message = viewModel.error ?: "Не удалось загрузить избранное",
                    onRetry = { viewModel.load() },
                )
                viewModel.favorites.isEmpty() -> EmptyState(
                    title = "Нет избранных",
                    subtitle = "Нажимайте ♥ в ленте, чтобы сохранять объявления",
                    icon = Icons.Outlined.FavoriteBorder,
                    action = { Button(onClick = onGoToFeed) { Text("К объявлениям") } },
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.favorites, key = { it.id }) { fav ->
                        DismissibleFavoriteRow(
                            favorite = fav,
                            onClick = { onAdClick(fav.adId) },
                            onRemove = { removeWithUndo(fav) },
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }
}

/** [FavoriteRow] wrapped in swipe-to-dismiss (both directions) with a delete-tinted background. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DismissibleFavoriteRow(
    favorite: FavoriteResponse,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onRemove()
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        FavoriteRow(favorite = favorite, onClick = onClick, onRemove = onRemove)
    }
}
