package ru.agromarket.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ru.agromarket.data.model.AdListResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

// Quick categories with subcategories
data class QuickCategory(val name: String, val icon: String, val subs: List<String>)

val QUICK_CATEGORIES = listOf(
    QuickCategory("Техника", "\uD83D\uDE9C", listOf("Тракторы", "Комбайны", "Сеялки", "Опрыскиватели", "Культиваторы")),
    QuickCategory("Запчасти", "\uD83D\uDD27", listOf("К тракторам", "К комбайнам", "Фильтры", "Ремни")),
    QuickCategory("Семена", "\uD83C\uDF31", listOf("Пшеница", "Подсолнечник", "Кукуруза", "Соя")),
    QuickCategory("Удобрения", "\uD83E\uDDEA", listOf("Азотные", "Фосфорные", "Калийные", "Комплексные")),
    QuickCategory("Корма", "\uD83C\uDF3E", listOf("Комбикорм", "Сено", "Силос", "Жмых")),
    QuickCategory("Животные", "\uD83D\uDC04", listOf("КРС", "Свиньи", "Птица", "Овцы")),
    QuickCategory("Земля", "\uD83C\uDFDE\uFE0F", listOf("Пашня", "Пастбища", "Аренда")),
    QuickCategory("Оборудование", "\u2699\uFE0F", listOf("Доильное", "Зерносушилки", "Весовое")),
    QuickCategory("Прочее", "\uD83D\uDCE6", listOf("Инструменты", "Тара", "Стройматериалы")),
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var ads by mutableStateOf<List<AdListResponse>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)
    var selectedType by mutableStateOf<String?>(null)
    var selectedQuickCategory by mutableStateOf<QuickCategory?>(null)

    init { loadFeed() }

    fun loadFeed(refresh: Boolean = false) {
        if (refresh) currentPage = 1
        isLoading = true
        viewModelScope.launch {
            error = null
            when (val result = repository.getFeed(page = currentPage, type = selectedType, search = searchQuery.ifBlank { null })) {
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
    fun filterByType(type: String?) { selectedType = type; currentPage = 1; loadFeed(refresh = true) }
    fun selectQuickCategory(cat: QuickCategory?) {
        selectedQuickCategory = if (selectedQuickCategory == cat) null else cat
    }
    fun searchSub(sub: String) { searchQuery = sub; currentPage = 1; loadFeed(refresh = true) }
    fun clearSearch() { searchQuery = ""; selectedQuickCategory = null; currentPage = 1; loadFeed(refresh = true) }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun FeedScreen(onAdClick: (String) -> Unit, onProfileClick: () -> Unit, viewModel: FeedViewModel = hiltViewModel()) {

    // Debounce text input: fire the network search ~400ms after the user stops typing.
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.searchQuery }
            .drop(1) // skip the current value already loaded in init
            .debounce(400)
            .collect { viewModel.runSearch() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar - only logo + profile
        TopAppBar(
            title = { Text("\uD83C\uDF3E АгроМаркет", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            actions = { IconButton(onClick = onProfileClick) { Icon(Icons.Default.AccountCircle, "Профиль", modifier = Modifier.size(28.dp)) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroGreen, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary)
        )

        // Type filters: Все | Продажа | Агроуслуги | Земли
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = viewModel.selectedType == null, onClick = { viewModel.filterByType(null) }, label = { Text("Все", fontSize = 15.sp) })
            FilterChip(selected = viewModel.selectedType == "sell", onClick = { viewModel.filterByType(if (viewModel.selectedType == "sell") null else "sell") }, label = { Text("Продажа", fontSize = 15.sp) })
            FilterChip(selected = viewModel.selectedType == "service", onClick = { viewModel.filterByType(if (viewModel.selectedType == "service") null else "service") }, label = { Text("Агроуслуги", fontSize = 15.sp) })
            FilterChip(selected = viewModel.selectedType == "land", onClick = { viewModel.filterByType(if (viewModel.selectedType == "land") null else "land") }, label = { Text("Земли", fontSize = 15.sp) })
        }

        // Search bar - always visible
        OutlinedTextField(
            value = viewModel.searchQuery, onValueChange = { viewModel.onSearchInput(it) },
            placeholder = { Text("Поиск...", fontSize = 16.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (viewModel.searchQuery.isNotBlank()) IconButton(onClick = { viewModel.clearSearch() }) { Icon(Icons.Default.Close, "Очистить") } },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick category chips with emoji
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(QUICK_CATEGORIES) { cat ->
                FilterChip(
                    selected = viewModel.selectedQuickCategory == cat,
                    onClick = { viewModel.selectQuickCategory(cat) },
                    label = { Text("${cat.icon} ${cat.name}", fontSize = 14.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Subcategories when a quick category is selected
        viewModel.selectedQuickCategory?.let { cat ->
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cat.subs) { sub ->
                    SuggestionChip(
                        onClick = { viewModel.searchSub(sub) },
                        label = { Text(sub, fontSize = 14.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Content
        when {
            viewModel.isLoading && viewModel.ads.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AgroGreen) }
            viewModel.ads.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(64.dp), tint = AgroGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Объявлений пока нет", style = MaterialTheme.typography.titleMedium, color = AgroGray)
                }
            }
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.ads, key = { it.id }) { ad -> AdCard(ad = ad, onClick = { onAdClick(ad.id) }) }
                if (viewModel.currentPage < viewModel.totalPages) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                    }
                }
            }
        }
    }
}

@Composable
fun AdCard(ad: AdListResponse, onClick: () -> Unit) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(model = ad.photoUrl, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val typeLabel = when (ad.type) { "sell" -> "Продажа"; "service" -> "Агроуслуги"; "land" -> "Земля"; else -> ad.type }
                val typeColor = when (ad.type) { "sell" -> AgroGreen; "service" -> AgroOrange; "land" -> AgroGreenDark; else -> AgroGray }
                Surface(color = typeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(typeLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = typeColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(ad.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                ad.price?.let { Text("${priceFormat.format(it)} \u20BD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AgroGreen) }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = AgroGray)
                    Text(ad.regionName ?: "", style = MaterialTheme.typography.bodySmall, color = AgroGray, modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
    }
}
