package ru.agromarket.ui.create

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.agromarket.data.model.*
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.theme.*
import ru.agromarket.utils.FileUtils
import javax.inject.Inject

enum class CreateStep { TYPE, CATEGORY, SUBCATEGORY, FORM }

@HiltViewModel
class CreateAdViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var step by mutableStateOf(CreateStep.TYPE)
    var type by mutableStateOf("sell")
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var price by mutableStateOf("")
    var phonePrimary by mutableStateOf("")
    var selectedCategory by mutableStateOf<CategoryTreeResponse?>(null)
    var selectedSubCategory by mutableStateOf<CategoryResponse?>(null)
    var selectedRegionId by mutableStateOf<Int?>(null)
    var selectedRegionName by mutableStateOf("")
    var selectedDistrictId by mutableStateOf<Int?>(null)
    var selectedDistrictName by mutableStateOf("")
    var selectedLocalityId by mutableStateOf<Int?>(null)
    var selectedLocalityName by mutableStateOf("")
    var regions by mutableStateOf<List<RegionResponse>>(emptyList())
    var districts by mutableStateOf<List<DistrictResponse>>(emptyList())
    var localities by mutableStateOf<List<LocalityResponse>>(emptyList())
    var categories by mutableStateOf<List<CategoryTreeResponse>>(emptyList())
    var categoriesError by mutableStateOf<String?>(null)
    var selectedCategoryId by mutableStateOf<Int?>(null)
    var photoUris by mutableStateOf<List<Uri>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            categoriesError = null
            when (val r = repository.getCategories()) {
                is ApiResult.Success -> { categories = r.data; categoriesError = null }
                is ApiResult.Error -> categoriesError = r.message
            }
        }
        viewModelScope.launch { (repository.getRegions() as? ApiResult.Success)?.let { regions = it.data } }
    }

    fun selectType(t: String) { type = t; step = CreateStep.CATEGORY }
    fun selectCategory(cat: CategoryTreeResponse) {
        selectedCategory = cat
        selectedSubCategory = null
        if (cat.children.isEmpty()) {
            // Top-level category has no subcategories on the server → it is the target itself.
            selectedCategoryId = cat.id
            step = CreateStep.FORM
        } else {
            // Force an explicit subcategory choice; don't leak a parent id when children exist.
            selectedCategoryId = null
            step = CreateStep.SUBCATEGORY
        }
    }
    fun selectSubCategory(sub: CategoryResponse) {
        selectedSubCategory = sub
        selectedCategoryId = sub.id // real server category id, no substring guessing
        step = CreateStep.FORM
    }
    fun goBack() {
        step = when (step) {
            CreateStep.FORM -> if (selectedCategory?.children.isNullOrEmpty()) CreateStep.CATEGORY else CreateStep.SUBCATEGORY
            CreateStep.SUBCATEGORY -> CreateStep.CATEGORY
            CreateStep.CATEGORY -> CreateStep.TYPE
            else -> CreateStep.TYPE
        }
    }

    fun selectRegion(id: Int, name: String) {
        selectedRegionId = id; selectedRegionName = name
        // Reset dependent geo levels so a stale district/locality can't be submitted.
        selectedDistrictId = null; selectedDistrictName = ""; districts = emptyList()
        selectedLocalityId = null; selectedLocalityName = ""; localities = emptyList()
        viewModelScope.launch { (repository.getDistricts(id) as? ApiResult.Success)?.let { districts = it.data } }
    }
    fun selectDistrict(id: Int, name: String) {
        selectedDistrictId = id; selectedDistrictName = name
        selectedLocalityId = null; selectedLocalityName = ""; localities = emptyList()
        viewModelScope.launch { (repository.getLocalities(id) as? ApiResult.Success)?.let { localities = it.data } }
    }
    fun selectLocality(id: Int, name: String) { selectedLocalityId = id; selectedLocalityName = name }

    fun addPhotos(uris: List<Uri>) { photoUris = (photoUris + uris).take(10) }
    fun removePhoto(uri: Uri) { photoUris = photoUris - uri }

    fun submitAd(context: android.content.Context, onSuccess: () -> Unit) {
        if (title.length < 5) { error = "Название минимум 5 символов"; return }
        if (selectedCategoryId == null) { error = "Не удалось определить категорию"; return }
        if (selectedRegionId == null) { error = "Выберите регион"; return }
        if (phonePrimary.isBlank()) { error = "Укажите телефон"; return }
        if (photoUris.size < 2) { error = "Загрузите минимум 2 фото"; return }

        viewModelScope.launch {
            isLoading = true; error = null
            val request = AdCreateRequest(type = type, categoryId = selectedCategoryId!!, regionId = selectedRegionId!!, districtId = selectedDistrictId, localityId = selectedLocalityId, title = title, description = description.ifBlank { null }, price = price.toBigDecimalOrNull(), phonePrimary = phonePrimary)
            when (val r = repository.createAd(request)) {
                is ApiResult.Success -> {
                    val adId = r.data.id
                    val files = photoUris.mapNotNull { FileUtils.uriToFile(context, it) }
                    if (files.isNotEmpty()) repository.uploadPhotos(adId, files)
                    when (val submitResult = repository.submitAd(adId)) {
                        is ApiResult.Success -> onSuccess()
                        is ApiResult.Error -> error = submitResult.message
                    }
                }
                is ApiResult.Error -> error = r.message
            }
            isLoading = false
        }
    }
}

@Composable
fun SearchablePickerDialog(title: String, items: List<Pair<Int, String>>, onSelect: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var searchText by remember { mutableStateOf("") }
    val filtered = items.filter { it.second.contains(searchText, ignoreCase = true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontSize = 20.sp) }, text = {
        Column {
            OutlinedTextField(value = searchText, onValueChange = { searchText = it }, placeholder = { Text("Поиск...", fontSize = 16.sp) }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                items(filtered) { (id, name) ->
                    Text(text = name, modifier = Modifier.fillMaxWidth().clickable { onSelect(id, name) }.padding(vertical = 14.dp, horizontal = 4.dp), fontSize = 17.sp)
                    HorizontalDivider()
                }
                if (filtered.isEmpty()) { item { Text("Ничего не найдено", color = AgroGray, modifier = Modifier.padding(16.dp), fontSize = 16.sp) } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть", fontSize = 16.sp) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdScreen(onSuccess: () -> Unit, onBack: () -> Unit, viewModel: CreateAdViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }
    val fallbackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showDistrictPicker by remember { mutableStateOf(false) }
    var showLocalityPicker by remember { mutableStateOf(false) }

    if (showRegionPicker) {
        SearchablePickerDialog(title = "Выберите регион", items = viewModel.regions.map { Pair(it.id, it.name) }, onSelect = { id, name -> viewModel.selectRegion(id, name); showRegionPicker = false }, onDismiss = { showRegionPicker = false })
    }
    if (showDistrictPicker) {
        SearchablePickerDialog(title = "Выберите район", items = viewModel.districts.map { Pair(it.id, it.name) }, onSelect = { id, name -> viewModel.selectDistrict(id, name); showDistrictPicker = false }, onDismiss = { showDistrictPicker = false })
    }
    if (showLocalityPicker) {
        SearchablePickerDialog(title = "Выберите населённый пункт", items = viewModel.localities.map { Pair(it.id, it.name) }, onSelect = { id, name -> viewModel.selectLocality(id, name); showLocalityPicker = false }, onDismiss = { showLocalityPicker = false })
    }

    Scaffold(topBar = {
        if (viewModel.step != CreateStep.TYPE) TopAppBar(title = { Text(when (viewModel.step) { CreateStep.CATEGORY -> "Выберите категорию"; CreateStep.SUBCATEGORY -> viewModel.selectedCategory?.name ?: ""; else -> "Новое объявление" }, fontSize = 20.sp) }, navigationIcon = { IconButton(onClick = { viewModel.goBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } })
    }) { padding ->
        when (viewModel.step) {
            // Step 1: Type
            CreateStep.TYPE -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Что размещаем?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    listOf(
                        Triple("sell", "\uD83D\uDE9C Продать", "Зерно, технику, скот и др."),
                        Triple("service", "\uD83D\uDD27 Агроуслуги", "Обработка, перевозка, ремонт"),
                        Triple("land", "\uD83C\uDFDE\uFE0F Земли СХ назначения", "Продажа и аренда участков"),
                    ).forEach { (t, label, desc) ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { viewModel.selectType(t) }, shape = RoundedCornerShape(16.dp)) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = AgroGray)
                            }
                            Text(desc, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp), color = AgroGray, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Step 2: Category grid (driven by the server category tree, not a hardcoded list)
            CreateStep.CATEGORY -> {
                if (viewModel.categories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                        if (viewModel.categoriesError != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = AgroGray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(viewModel.categoriesError ?: "Не удалось загрузить категории", color = AgroGray, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadData() }) { Text("Повторить") }
                            }
                        } else {
                            CircularProgressIndicator(color = AgroGreen)
                        }
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(viewModel.categories) { cat ->
                            Card(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.selectCategory(cat) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AgroGreenBg)) {
                                Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(cat.icon ?: "📦", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Subcategory list
            CreateStep.SUBCATEGORY -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Выберите подкатегорию:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    viewModel.selectedCategory?.children?.forEach { sub ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectSubCategory(sub) }, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(sub.name, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = AgroGray)
                            }
                        }
                    }
                }
            }

            // Step 4: Form
            CreateStep.FORM -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).imePadding()) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Show selected path
                        item {
                            Surface(color = AgroGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                val typeLabel = when (viewModel.type) { "sell" -> "Продажа"; "service" -> "Агроуслуги"; "land" -> "Земля"; else -> viewModel.type }
                                val path = listOfNotNull(typeLabel, viewModel.selectedCategory?.name, viewModel.selectedSubCategory?.name).joinToString(" \u203A ")
                                Text(path, modifier = Modifier.padding(12.dp), color = AgroGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        item { OutlinedTextField(value = viewModel.title, onValueChange = { viewModel.title = it }, label = { Text("Название *", fontSize = 16.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.description, onValueChange = { viewModel.description = it }, label = { Text("Описание", fontSize = 16.sp) }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.price, onValueChange = { viewModel.price = it }, label = { Text("Цена (\u20BD)", fontSize = 16.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.phonePrimary, onValueChange = { viewModel.phonePrimary = it }, label = { Text("Телефон *", fontSize = 16.sp) }, placeholder = { Text("+79001234567") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }

                        // Region picker
                        item {
                            Surface(modifier = Modifier.fillMaxWidth().clickable { showRegionPicker = true }, shape = RoundedCornerShape(4.dp), border = ButtonDefaults.outlinedButtonBorder) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Регион *", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = viewModel.selectedRegionName.ifBlank { "Нажмите для выбора" }, fontSize = 17.sp, color = if (viewModel.selectedRegionName.isBlank()) AgroGray else MaterialTheme.colorScheme.onSurface)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        }

                        // District picker (optional) — shown once a region is chosen
                        if (viewModel.selectedRegionId != null) {
                            item {
                                Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.districts.isNotEmpty()) { showDistrictPicker = true }, shape = RoundedCornerShape(4.dp), border = ButtonDefaults.outlinedButtonBorder) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Район", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val districtHint = if (viewModel.districts.isEmpty()) "Нет районов" else "Не выбран"
                                            Text(text = viewModel.selectedDistrictName.ifBlank { districtHint }, fontSize = 17.sp, color = if (viewModel.selectedDistrictName.isBlank()) AgroGray else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        // Locality picker (optional) — shown once a district is chosen
                        if (viewModel.selectedDistrictId != null) {
                            item {
                                Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.localities.isNotEmpty()) { showLocalityPicker = true }, shape = RoundedCornerShape(4.dp), border = ButtonDefaults.outlinedButtonBorder) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Населённый пункт", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val localityHint = if (viewModel.localities.isEmpty()) "Нет населённых пунктов" else "Не выбран"
                                            Text(text = viewModel.selectedLocalityName.ifBlank { localityHint }, fontSize = 17.sp, color = if (viewModel.selectedLocalityName.isBlank()) AgroGray else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        // Photos
                        item {
                            Text("Фото (мин. 2, макс. 10)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(viewModel.photoUris) { uri ->
                                    Box(modifier = Modifier.size(90.dp)) {
                                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                        IconButton(onClick = { viewModel.removePhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) { Icon(Icons.Default.Close, null, tint = AgroRed) }
                                    }
                                }
                                item {
                                    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, AgroGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable {
                                        try { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } catch (e: Exception) { fallbackLauncher.launch("image/*") }
                                    }, contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, tint = AgroGreen, modifier = Modifier.size(32.dp)); Text("Добавить", fontSize = 13.sp, color = AgroGreen) }
                                    }
                                }
                            }
                            Text("${viewModel.photoUris.size} из 10 фото", fontSize = 14.sp, color = AgroGray)
                        }
                        viewModel.error?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error, fontSize = 15.sp) } }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.submitAd(context, onSuccess) }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !viewModel.isLoading, shape = RoundedCornerShape(12.dp)) {
                        if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Отправить на модерацию", fontSize = 17.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
