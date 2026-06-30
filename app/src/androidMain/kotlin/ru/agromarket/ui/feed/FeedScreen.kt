package ru.agromarket.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ru.agromarket.data.model.AdListResponse
import ru.agromarket.data.model.LAND_CATEGORY_ID
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AdCard
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.EmptyState
import ru.agromarket.ui.components.ErrorBanner
import ru.agromarket.ui.components.ErrorState
import ru.agromarket.ui.components.FeedLoadingState
import ru.agromarket.ui.components.categoryIconFor

// Quick categories with subcategories; icons come from categoryIconFor(name)
data class QuickCategory(val name: String, val subs: List<String>)

val QUICK_CATEGORIES = listOf(
    QuickCategory("Техника", listOf("Тракторы", "Комбайны", "Сеялки", "Опрыскиватели", "Культиваторы")),
    QuickCategory("Запчасти", listOf("К тракторам", "К комбайнам", "Фильтры", "Ремни")),
    QuickCategory("Семена", listOf("Пшеница", "Подсолнечник", "Кукуруза", "Соя")),
    QuickCategory("Удобрения", listOf("Азотные", "Фосфорные", "Калийные", "Комплексные")),
    QuickCategory("Корма", listOf("Комбикорм", "Сено", "Силос", "Жмых")),
    QuickCategory("Животные", listOf("КРС", "Свиньи", "Птица", "Овцы")),
    QuickCategory("Земля", listOf("Пашня", "Пастбища", "Аренда")),
    QuickCategory("Оборудование", listOf("Доильное", "Зерносушилки", "Весовое")),
    QuickCategory("Прочее", listOf("Инструменты", "Тара", "Стройматериалы")),
)

class FeedViewModel(
    private val repository: AgroRepository
) : ViewModel() {
    var ads by mutableStateOf<List<AdListResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)
    var selectedType by mutableStateOf<String?>(null)
    var selectedCategoryId by mutableStateOf<Int?>(null)
    var selectedQuickCategory by mutableStateOf<QuickCategory?>(null)
    var favoriteIds by mutableStateOf<Set<String>>(emptySet())

    init { loadFeed() }

    /** Sync favorite ids with the server; soft failure — hearts just stay as they were. */
    fun refreshFavorites() {
        viewModelScope.launch {
            (repository.getFavorites() as? ApiResult.Success)?.let { fav ->
                favoriteIds = fav.data.map { it.adId }.toSet()
            }
        }
    }

    fun toggleFavorite(adId: String) {
        viewModelScope.launch {
            val target = adId !in favoriteIds
            favoriteIds = if (target) favoriteIds + adId else favoriteIds - adId // optimistic update
            val result = if (target) repository.addFavorite(adId) else repository.removeFavorite(adId)
            if (result is ApiResult.Error) {
                favoriteIds = if (target) favoriteIds - adId else favoriteIds + adId // rollback
            }
        }
    }

    fun loadFeed(refresh: Boolean = false) {
        if (refresh) currentPage = 1
        isLoading = true
        viewModelScope.launch {
            error = null
            when (val result = repository.getFeed(page = currentPage, type = selectedType, categoryId = selectedCategoryId, search = searchQuery.ifBlank { null })) {
                is ApiResult.Success -> {
                    ads = if (refresh || currentPage == 1) result.data.items else ads + result.data.items
                    totalPages = result.data.totalPages
                }
                is ApiResult.Error -> error = result.message
            }
            isLoading = false
        }
    }

    fun loadMore() { if (currentPage < totalPages && !isLoading) { currentPage++; loadFeed() } }
    // Only updates the text field; the actual network call is debounced in the screen.
    fun onSearchInput(query: String) { searchQuery = query; selectedQuickCategory = null }
    // Triggered after debounce (or immediately for explicit actions like subcategory taps).
    fun runSearch() { currentPage = 1; loadFeed(refresh = true) }
    fun filterByType(type: String?) { selectedType = type; selectedCategoryId = null; currentPage = 1; loadFeed(refresh = true) }
    fun filterByCategory(categoryId: Int?) { selectedCategoryId = categoryId; selectedType = null; currentPage = 1; loadFeed(refresh = true) }
    fun selectQuickCategory(cat: QuickCategory?) {
        selectedQuickCategory = if (selectedQuickCategory == cat) null else cat
    }
    fun searchSub(sub: String) { searchQuery = sub; currentPage = 1; loadFeed(refresh = true) }
    fun clearSearch() { searchQuery = ""; selectedQuickCategory = null; currentPage = 1; loadFeed(refresh = true) }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(onAdClick: (String) -> Unit, onProfileClick: () -> Unit, viewModel: FeedViewModel = koinViewModel()) {

    // Debounce text input: fire the network search ~400ms after the user stops typing.
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.searchQuery }
            .drop(1) // skip the current value already loaded in init
            .debounce(400)
            .collect { viewModel.runSearch() }
    }

    // Re-sync hearts every time the feed re-enters composition (e.g. back from AdDetail,
    // where the user may have toggled the favorite from the top bar).
    LaunchedEffect(Unit) { viewModel.refreshFavorites() }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "АгроМаркет",
            showLogo = true,
            actions = {
                IconButton(onClick = onProfileClick) {
                    Icon(Icons.Default.AccountCircle, "Профиль", modifier = Modifier.size(28.dp))
                }
            },
        )

        // Type filters: Все | Продажа | Агроуслуги | Земли
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = viewModel.selectedType == null && viewModel.selectedCategoryId == null, onClick = { viewModel.filterByType(null) }, label = { Text("Все", style = MaterialTheme.typography.labelLarge) })
            FilterChip(selected = viewModel.selectedType == "sale", onClick = { viewModel.filterByType(if (viewModel.selectedType == "sale") null else "sale") }, label = { Text("Продажа", style = MaterialTheme.typography.labelLarge) })
            FilterChip(selected = viewModel.selectedType == "service", onClick = { viewModel.filterByType(if (viewModel.selectedType == "service") null else "service") }, label = { Text("Агроуслуги", style = MaterialTheme.typography.labelLarge) })
            FilterChip(selected = viewModel.selectedCategoryId == LAND_CATEGORY_ID, onClick = { viewModel.filterByCategory(if (viewModel.selectedCategoryId == LAND_CATEGORY_ID) null else LAND_CATEGORY_ID) }, label = { Text("Земли", style = MaterialTheme.typography.labelLarge) })
        }

        // Search bar - always visible
        OutlinedTextField(
            value = viewModel.searchQuery, onValueChange = { viewModel.onSearchInput(it) },
            placeholder = { Text("Поиск...", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (viewModel.searchQuery.isNotBlank()) IconButton(onClick = { viewModel.clearSearch() }) { Icon(Icons.Default.Close, "Очистить") } },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick category tiles: icon + label, replacing emoji chips
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUICK_CATEGORIES) { cat ->
                QuickCategoryTile(
                    category = cat,
                    selected = viewModel.selectedQuickCategory == cat,
                    onClick = { viewModel.selectQuickCategory(cat) },
                )
            }
        }

        // Subcategories when a quick category is selected
        viewModel.selectedQuickCategory?.let { cat ->
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cat.subs) { sub ->
                    SuggestionChip(
                        onClick = { viewModel.searchSub(sub) },
                        label = { Text(sub, style = MaterialTheme.typography.labelLarge) },
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        when {
            viewModel.isLoading && viewModel.ads.isEmpty() -> FeedLoadingState(modifier = Modifier.fillMaxSize())
            // Network failure must look like a failure, not like an empty catalog (audit п.3.1)
            viewModel.error != null && viewModel.ads.isEmpty() -> ErrorState(
                message = viewModel.error ?: "Не удалось загрузить объявления",
                onRetry = { viewModel.loadFeed(refresh = true) },
            )
            viewModel.ads.isEmpty() -> EmptyState(
                title = "Объявлений пока нет",
                subtitle = "Попробуйте изменить фильтры или поисковый запрос",
                icon = Icons.Filled.Inventory2,
            )
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.ads, key = { it.id }) { ad ->
                    AdCard(
                        ad = ad,
                        onClick = { onAdClick(ad.id) },
                        isFavorite = ad.id in viewModel.favoriteIds,
                        onFavoriteClick = { viewModel.toggleFavorite(ad.id) },
                        modifier = Modifier.animateItemPlacement(),
                    )
                }
                // Pagination failure on a non-empty list: keep the loaded items, show the error inline
                viewModel.error?.let { err ->
                    item { ErrorBanner(message = err) }
                }
                if (viewModel.currentPage < viewModel.totalPages && viewModel.error == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                    }
                }
            }
        }
    }
}

/** Category tile for the quick-filter row: icon over label, highlighted when selected. */
@Composable
private fun QuickCategoryTile(category: QuickCategory, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier.width(72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
        ) {
            Icon(
                imageVector = categoryIconFor(category.name),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
