package ru.agromarket.ui.ad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import ru.agromarket.data.model.AdDetailResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AdDetailViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var ad by mutableStateOf<AdDetailResponse?>(null)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var isFavorite by mutableStateOf(false)
    var contactMessage by mutableStateOf<String?>(null)

    fun loadAd(adId: String) {
        viewModelScope.launch {
            isLoading = true
            when (val result = repository.getAdDetail(adId)) {
                is ApiResult.Success -> ad = result.data
                is ApiResult.Error -> error = result.message
            }
            // AdDetailResponse has no favorite flag, so sync against the real favorites list.
            (repository.getFavorites() as? ApiResult.Success)?.let { fav ->
                isFavorite = fav.data.any { it.adId == adId }
            }
            isLoading = false
        }
    }

    fun toggleFavorite(adId: String) {
        viewModelScope.launch {
            val target = !isFavorite
            isFavorite = target // optimistic update
            val result = if (target) repository.addFavorite(adId) else repository.removeFavorite(adId)
            if (result is ApiResult.Error) isFavorite = !target // rollback on failure
        }
    }

    fun requestContacts(adId: String) {
        viewModelScope.launch {
            when (val result = repository.createContactRequest(adId)) {
                is ApiResult.Success -> contactMessage = "Запрос отправлен! Ожидайте ответа."
                is ApiResult.Error -> contactMessage = result.message
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdDetailScreen(adId: String, onBack: () -> Unit, viewModel: AdDetailViewModel = hiltViewModel()) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    LaunchedEffect(adId) { viewModel.loadAd(adId) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Объявление") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            actions = {
                IconButton(onClick = { viewModel.toggleFavorite(adId) }) {
                    Icon(if (viewModel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Избранное",
                        tint = if (viewModel.isFavorite) AgroRed else MaterialTheme.colorScheme.onSurface)
                }
            })
    }) { padding ->
        when {
            viewModel.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AgroGreen) }
            viewModel.error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(viewModel.error!!, color = MaterialTheme.colorScheme.error) }
            viewModel.ad != null -> {
                val ad = viewModel.ad!!
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    if (ad.photos.isNotEmpty()) {
                        LazyRow(modifier = Modifier.fillMaxWidth().height(250.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ad.photos) { photo -> AsyncImage(model = photo.url, contentDescription = null, modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        val typeLabel = when (ad.type) { "sell" -> "Продажа"; "buy" -> "Покупка"; "service" -> "Услуга"; else -> ad.type }
                        Surface(color = AgroGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) { Text(typeLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = AgroGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(ad.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ad.price?.let { Text("${priceFormat.format(it)} \u20BD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AgroGreen) }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = AgroGray); Spacer(modifier = Modifier.width(4.dp)); Text(listOfNotNull(ad.regionName, ad.districtName, ad.localityName).joinToString(", "), color = AgroGray) }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Category, null, tint = AgroGray); Spacer(modifier = Modifier.width(4.dp)); Text(listOfNotNull(ad.parentCategoryName, ad.categoryName).joinToString(" \u2192 "), color = AgroGray) }
                        ad.description?.let { Spacer(modifier = Modifier.height(16.dp)); Text("Описание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(modifier = Modifier.height(4.dp)); Text(it) }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AgroGreenBg)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Контакты", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Phone, null, tint = AgroGreen); Spacer(modifier = Modifier.width(8.dp)); Text(ad.phonePrimary, fontWeight = FontWeight.Medium) }
                                ad.phoneSecondary?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Phone, null, tint = AgroGreen); Spacer(modifier = Modifier.width(8.dp)); Text(it) } }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.contactMessage?.let { Text(it, color = AgroGreen); Spacer(modifier = Modifier.height(8.dp)) }
                        Button(onClick = { viewModel.requestContacts(adId) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.ContactPhone, null); Spacer(modifier = Modifier.width(8.dp)); Text("Запросить контакты")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
