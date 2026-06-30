package ru.agromarket.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.agromarket.data.api.AdMyListResponse
import ru.agromarket.data.model.ProfileResponse
import ru.agromarket.data.model.ProfileUpdateRequest
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.ErrorBanner
import ru.agromarket.ui.components.ErrorState
import ru.agromarket.ui.components.StatusBadge
import ru.agromarket.ui.components.adStatusBadge
import ru.agromarket.ui.components.boostTier
import ru.agromarket.ui.theme.AgroAccentClay
import ru.agromarket.utils.FileUtils
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ProfileViewModel(private val repository: AgroRepository) : ViewModel() {
    var profile by mutableStateOf<ProfileResponse?>(null)
    var myAds by mutableStateOf<List<AdMyListResponse>>(emptyList())
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var isEditing by mutableStateOf(false)
    var editFirstName by mutableStateOf("")
    var editLastName by mutableStateOf("")
    var editPhone by mutableStateOf("")
    var saveMessage by mutableStateOf<String?>(null)
    var myAdsError by mutableStateOf<String?>(null)

    init { load() }

    /** Перезагрузить только список объявлений — например, после покупки boost. */
    fun reloadMyAds() {
        viewModelScope.launch {
            (repository.getMyAds() as? ApiResult.Success)?.let { myAds = it.data }
        }
    }

    /** Owner action from "Мои объявления": remove the ad and drop it from the list. */
    fun deleteAd(adId: String) {
        viewModelScope.launch {
            myAdsError = null
            when (val r = repository.deleteAd(adId)) {
                is ApiResult.Success -> myAds = myAds.filter { it.id != adId }
                is ApiResult.Error -> myAdsError = r.message
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            when (val r = repository.getProfile()) {
                is ApiResult.Success -> {
                    profile = r.data
                    editFirstName = r.data.firstName ?: ""
                    editLastName = r.data.lastName ?: ""
                    editPhone = r.data.phone ?: ""
                }
                is ApiResult.Error -> error = r.message // hard error: profile is the screen's core data
            }
            // Soft failure: keep the profile usable even if the ads list can't load.
            (repository.getMyAds() as? ApiResult.Success)?.let { myAds = it.data }
            isLoading = false
        }
    }

    fun startEditing() { isEditing = true; saveMessage = null }

    fun saveProfile() {
        viewModelScope.launch {
            val request = ProfileUpdateRequest(
                firstName = editFirstName.ifBlank { null },
                lastName = editLastName.ifBlank { null },
                phone = editPhone.ifBlank { null }
            )
            when (val result = repository.updateProfile(request)) {
                is ApiResult.Success -> { profile = result.data; isEditing = false; saveMessage = "Сохранено!" }
                is ApiResult.Error -> saveMessage = result.message
            }
        }
    }

    fun uploadAvatar(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            val file = FileUtils.uriToFile(context, uri) ?: return@launch
            // File→bytes на call-site (androidMain): commonMain-репозиторий принимает байты + имя.
            when (val result = repository.uploadAvatar(file.readBytes(), file.name)) {
                is ApiResult.Success -> {
                    profile = profile?.copy(avatarUrl = result.data.avatarUrl)
                    saveMessage = "Фото обновлено!"
                }
                is ApiResult.Error -> saveMessage = result.message
            }
        }
    }

    fun logout(onDone: () -> Unit) { viewModelScope.launch { repository.logout(); onDone() } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onMyAds: (String) -> Unit,
    onBack: () -> Unit,
    onChangePassword: () -> Unit,
    onBoost: (String) -> Unit = {},
    onSubscription: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }
    var adToDelete by remember { mutableStateOf<AdMyListResponse?>(null) }

    adToDelete?.let { ad ->
        AlertDialog(
            onDismissRequest = { adToDelete = null },
            title = { Text("Удалить объявление?") },
            text = { Text("«${ad.title}» будет удалено безвозвратно.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAd(ad.id); adToDelete = null }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { adToDelete = null }) { Text("Отмена") } },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "Профиль",
            onBack = onBack,
            actions = { IconButton(onClick = { viewModel.logout(onLogout) }) { Icon(Icons.AutoMirrored.Filled.Logout, "Выйти") } }
        )
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else if (viewModel.profile == null && viewModel.error != null) {
            ErrorState(
                message = viewModel.error ?: "Не удалось загрузить профиль",
                onRetry = { viewModel.load() },
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    viewModel.profile?.let { p ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).clickable { avatarLauncher.launch("image/*") }) {
                                        AsyncImage(model = p.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        Surface(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (!viewModel.isEditing) {
                                            Text(listOfNotNull(p.lastName, p.firstName).joinToString(" ").ifBlank { "Пользователь" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text(p.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            p.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                                            p.regionName?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) } }
                                        }
                                    }
                                    if (!viewModel.isEditing) {
                                        IconButton(onClick = { viewModel.startEditing() }) { Icon(Icons.Default.Edit, "Редактировать", tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                                    }
                                }
                                if (viewModel.isEditing) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(value = viewModel.editLastName, onValueChange = { viewModel.editLastName = it }, label = { Text("Фамилия") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(value = viewModel.editFirstName, onValueChange = { viewModel.editFirstName = it }, label = { Text("Имя") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(value = viewModel.editPhone, onValueChange = { viewModel.editPhone = it }, label = { Text("Телефон") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { viewModel.isEditing = false }) { Text("Отмена") }
                                        Button(onClick = { viewModel.saveProfile() }) { Text("Сохранить") }
                                    }
                                }
                                viewModel.saveMessage?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().clickable { onSubscription() }, shape = MaterialTheme.shapes.medium) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, null, tint = AgroAccentClay)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Тариф · Pro для дилеров", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth().clickable { onChangePassword() }, shape = MaterialTheme.shapes.medium) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Сменить пароль", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Text("Мои объявления (${viewModel.myAds.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                viewModel.myAdsError?.let { err -> item { ErrorBanner(message = err) } }
                if (viewModel.myAds.isEmpty()) {
                    item { Card(modifier = Modifier.fillMaxWidth()) { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("У вас пока нет объявлений", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                } else {
                    items(viewModel.myAds, key = { it.id }) { ad ->
                        MyAdRow(
                            ad = ad,
                            onClick = { onMyAds(ad.id) },
                            onDelete = { adToDelete = ad },
                            onBoost = { onBoost(ad.id) },
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Мои объявления" row: thumbnail, title, moderation status and an owner kebab menu.
 * The status badge is mandatory here — the seller must see "на модерации"/"отклонено"
 * without contacting support. "Редактировать" will join the menu once an edit screen
 * exists (`updateAd` is already in the API).
 */
@Composable
private fun MyAdRow(
    ad: AdMyListResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onBoost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val boosted = boostTier(ad.boostLevel) >= 1

    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ad.photoUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ad.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (statusLabel, statusTone) = adStatusBadge(ad.status)
                    StatusBadge(text = statusLabel, tone = statusTone)
                    if (boosted) {
                        Surface(shape = MaterialTheme.shapes.small, color = AgroAccentClay, contentColor = androidx.compose.ui.graphics.Color.White) {
                            Text(
                                text = boostExpiryLabel(ad.boostExpiresAt),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (ad.status == "rejected" || ad.status == "needs_revision") {
                    val comment = ad.moderationFeedback?.comment ?: ad.moderationComment
                    if (!comment.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comment.lineSequence().first(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Действия с объявлением")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    // Продвигать имеет смысл только активное (опубликованное) объявление.
                    if (ad.status == "active" || ad.status == "published") {
                        DropdownMenuItem(
                            text = { Text(if (boosted) "Продлить в топе" else "Продвинуть в топ") },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, null, tint = AgroAccentClay) },
                            onClick = { menuOpen = false; onBoost() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

/** «↑ В топе» или «↑ В топе до DD.MM», если бэкенд прислал срок boost_expires_at. */
private fun boostExpiryLabel(boostExpiresAt: String?): String {
    val until = boostExpiresAt?.let {
        try {
            OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("dd.MM"))
        } catch (_: Exception) {
            null
        }
    }
    return if (until != null) "↑ В топе до $until" else "↑ В топе"
}
