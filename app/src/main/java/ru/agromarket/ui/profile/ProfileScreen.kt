package ru.agromarket.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.api.AdMyListResponse
import ru.agromarket.data.model.ProfileResponse
import ru.agromarket.data.model.ProfileUpdateRequest
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.theme.*
import ru.agromarket.utils.FileUtils
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: AgroRepository) : ViewModel() {
    var profile by mutableStateOf<ProfileResponse?>(null)
    var myAds by mutableStateOf<List<AdMyListResponse>>(emptyList())
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var isEditing by mutableStateOf(false)
    var editFirstName by mutableStateOf("")
    var editLastName by mutableStateOf("")
    var editPhone by mutableStateOf("")
    var saveMessage by mutableStateOf<String?>(null)

    init { load() }

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
            when (val result = repository.uploadAvatar(file)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit, onMyAds: (String) -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Профиль", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AgroGreen, titleContentColor = MaterialTheme.colorScheme.onPrimary),
            actions = { IconButton(onClick = { viewModel.logout(onLogout) }) { Icon(Icons.AutoMirrored.Filled.Logout, "Выйти", tint = MaterialTheme.colorScheme.onPrimary) } }
        )
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AgroGreen) }
        } else if (viewModel.profile == null && viewModel.error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = AgroGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(viewModel.error ?: "Не удалось загрузить профиль", color = AgroGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }) { Text("Повторить") }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    viewModel.profile?.let { p ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AgroGreenBg)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(72.dp).clip(CircleShape).clickable { avatarLauncher.launch("image/*") }) {
                                        AsyncImage(model = p.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        Surface(modifier = Modifier.align(Alignment.BottomEnd).size(24.dp), shape = CircleShape, color = AgroGreen) {
                                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(4.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (!viewModel.isEditing) {
                                            Text(listOfNotNull(p.lastName, p.firstName).joinToString(" ").ifBlank { "Пользователь" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(p.email, style = MaterialTheme.typography.bodySmall, color = AgroGray)
                                            p.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AgroGray) }
                                            p.regionName?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = AgroGray); Text(it, style = MaterialTheme.typography.bodySmall, color = AgroGray) } }
                                        }
                                    }
                                    if (!viewModel.isEditing) {
                                        IconButton(onClick = { viewModel.startEditing() }) { Icon(Icons.Default.Edit, "Редактировать", tint = AgroGreen) }
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
                                viewModel.saveMessage?.let { Spacer(modifier = Modifier.height(8.dp)); Text(it, color = AgroGreen, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                item { Text("Мои объявления (${viewModel.myAds.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (viewModel.myAds.isEmpty()) {
                    item { Card(modifier = Modifier.fillMaxWidth()) { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("У вас пока нет объявлений", color = AgroGray) } } }
                } else {
                    items(viewModel.myAds, key = { it.id }) { ad ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onMyAds(ad.id) }, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = ad.photoUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ad.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    val statusLabel = when (ad.status) { "draft" -> "Черновик"; "pending_moderation" -> "На модерации"; "active" -> "Активно"; "rejected" -> "Отклонено"; else -> ad.status }
                                    val statusColor = when (ad.status) { "active" -> AgroGreen; "rejected" -> AgroRed; "pending_moderation" -> AgroOrange; else -> AgroGray }
                                    Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = statusColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
