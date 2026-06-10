package ru.agromarket.ui.ad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.ErrorBanner
import ru.agromarket.ui.components.adTypeBadge
import ru.agromarket.ui.components.StatusBadge
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
    var contactError by mutableStateOf(false)

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
                is ApiResult.Success -> { contactMessage = "Запрос отправлен! Ожидайте ответа."; contactError = false }
                is ApiResult.Error -> { contactMessage = result.message; contactError = true }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AdDetailScreen(adId: String, onBack: () -> Unit, viewModel: AdDetailViewModel = hiltViewModel()) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    LaunchedEffect(adId) { viewModel.loadAd(adId) }

    Scaffold(topBar = {
        AppTopBar(
            title = "Объявление",
            onBack = onBack,
            actions = {
                IconButton(onClick = { viewModel.toggleFavorite(adId) }) {
                    Icon(
                        imageVector = if (viewModel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Избранное",
                        tint = if (viewModel.isFavorite) AgroRed else MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
        )
    }) { padding ->
        when {
            viewModel.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            viewModel.error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(viewModel.error!!, color = MaterialTheme.colorScheme.error) }
            viewModel.ad != null -> {
                val ad = viewModel.ad!!
                val pagerState = rememberPagerState(pageCount = { ad.photos.size.coerceAtLeast(1) })
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                        if (ad.photos.isNotEmpty()) {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                AsyncImage(
                                    model = ad.photos[page].url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            if (ad.photos.size > 1) {
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = Color.Black.copy(alpha = 0.55f),
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${ad.photos.size}",
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(56.dp),
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        val (typeLabel, typeTone) = adTypeBadge(ad.type)
                        StatusBadge(text = typeLabel, tone = typeTone)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(ad.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ad.price?.let {
                            Text(
                                text = "${priceFormat.format(it)} ₽",
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = listOfNotNull(ad.regionName, ad.districtName, ad.localityName).joinToString(", "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = listOfNotNull(ad.parentCategoryName, ad.categoryName).joinToString(" → "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ad.description?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Описание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(it)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Контакты", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(ad.phonePrimary, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                ad.phoneSecondary?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.contactMessage?.let { message ->
                            if (viewModel.contactError) {
                                ErrorBanner(message = message)
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { viewModel.requestContacts(adId) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Default.ContactPhone, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запросить контакты", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
