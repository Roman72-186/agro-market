package ru.agromarket.ui.ad

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun AdDetailScreen(adId: String, onBack: () -> Unit, onEditAd: (String) -> Unit = {}, viewModel: AdDetailViewModel = hiltViewModel()) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale("ru")) }
    val context = LocalContext.current
    var fullscreenPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var fullscreenFeedbackPhotoIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(adId) { viewModel.loadAd(adId) }

    // ACTION_DIAL opens the dialer with the number prefilled and needs no runtime permission
    fun dial(phone: String) {
        context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")))
    }

    Scaffold(
        topBar = {
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
        },
        bottomBar = {
            // Sticky CTA bar: the two target actions are always reachable without scrolling
            viewModel.ad?.let { ad ->
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { dial(ad.phonePrimary) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Позвонить")
                        }
                        OutlinedButton(
                            onClick = { viewModel.requestContacts(adId) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Default.ContactPhone, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Контакты")
                        }
                    }
                }
            }
        },
    ) { padding ->
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
                                    contentDescription = "Фото ${page + 1} из ${ad.photos.size}, открыть на весь экран",
                                    modifier = Modifier.fillMaxSize().clickable { fullscreenPhotoIndex = page },
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
                        if (ad.status == "rejected" || ad.status == "needs_revision") {
                            val feedback = ad.moderationFeedback
                            // Ads moderated before this feature shipped have no feedback row — fall back to the legacy comment field.
                            val comment = feedback?.comment ?: ad.moderationComment
                            val feedbackPhotos = feedback?.photos ?: emptyList()
                            if (!comment.isNullOrBlank() || feedbackPhotos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (ad.status == "needs_revision") "Замечания модератора" else "Причина отклонения",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                        if (!comment.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = comment,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                        if (feedbackPhotos.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                feedbackPhotos.forEachIndexed { index, photo ->
                                                    AsyncImage(
                                                        model = photo.url,
                                                        contentDescription = "Фото-пример от модератора ${index + 1}",
                                                        modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small).clickable { fullscreenFeedbackPhotoIndex = index },
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                }
                                            }
                                        }
                                        if (ad.status == "needs_revision") {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = { onEditAd(ad.id) }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Исправить и отправить снова")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Контакты", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Tappable phone rows: a visible number must actually dial (audit п.3.2)
                                PhoneRow(phone = ad.phonePrimary, onClick = { dial(ad.phonePrimary) })
                                ad.phoneSecondary?.let { phone ->
                                    PhoneRow(phone = phone, onClick = { dial(phone) })
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
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                fullscreenPhotoIndex?.let { startIndex ->
                    FullscreenGallery(
                        photos = ad.photos.map { it.url },
                        startIndex = startIndex,
                        onDismiss = { fullscreenPhotoIndex = null },
                    )
                }

                fullscreenFeedbackPhotoIndex?.let { startIndex ->
                    FullscreenGallery(
                        photos = (ad.moderationFeedback?.photos ?: emptyList()).map { it.url },
                        startIndex = startIndex,
                        onDismiss = { fullscreenFeedbackPhotoIndex = null },
                    )
                }
            }
        }
    }
}

/** Tappable phone row inside the contacts card; numbers are set in JetBrains Mono like all numeric data. */
@Composable
private fun PhoneRow(phone: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = phone,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Позвонить",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Fullscreen photo viewer: black backdrop, swipeable pager, pinch/double-tap zoom, "N/M" counter. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullscreenGallery(photos: List<String>, startIndex: Int, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { photos.size })
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableAsyncImage(model = photos[page], modifier = Modifier.fillMaxSize())
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }
            if (photos.size > 1) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${photos.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/**
 * Image with pinch-to-zoom (1x..4x), pan while zoomed and double-tap to toggle zoom.
 * Touch events are consumed only while pinching or already zoomed in, so single-finger
 * swipes at 1x still reach the surrounding [HorizontalPager].
 */
@Composable
private fun ZoomableAsyncImage(model: Any?, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    offset = Offset.Zero
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (zoomChange != 1f || scale > 1f) {
                            scale = (scale * zoomChange).coerceIn(1f, 4f)
                            offset = if (scale > 1f) offset + panChange else Offset.Zero
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
    )
}
