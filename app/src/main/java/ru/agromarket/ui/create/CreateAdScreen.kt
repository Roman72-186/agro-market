package ru.agromarket.ui.create

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Terrain
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
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.components.CategoryIcon
import ru.agromarket.ui.components.ErrorBanner
import ru.agromarket.ui.theme.*
import ru.agromarket.utils.FileUtils
import javax.inject.Inject

enum class CreateStep { TYPE, CATEGORY, SUBCATEGORY, FORM }

@HiltViewModel
class CreateAdViewModel @Inject constructor(
    private val repository: AgroRepository
) : ViewModel() {
    var step by mutableStateOf(CreateStep.TYPE)
    var type by mutableStateOf("sale")
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
    var geoError by mutableStateOf<String?>(null)

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

    fun selectType(t: String) {
        if (t == "land") {
            // "Земли СХ назначения" is a category (Земельные участки), not a backend AdType —
            // map it to type=sale and jump straight into that category's subcategories.
            type = "sale"
            val landCategory = categories.find { it.id == LAND_CATEGORY_ID }
            if (landCategory != null) {
                selectCategory(landCategory)
            } else {
                selectedCategory = null
                selectedCategoryId = null
                step = CreateStep.CATEGORY
            }
        } else {
            type = t
            step = CreateStep.CATEGORY
        }
    }
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

    /** Total wizard steps: 4 when the chosen category has a subcategory step, otherwise 3. */
    val totalSteps: Int get() = if (selectedCategory?.children.isNullOrEmpty()) 3 else 4

    /** 1-based position of the current step within [totalSteps]. */
    val stepNumber: Int get() = when (step) {
        CreateStep.TYPE -> 1
        CreateStep.CATEGORY -> 2
        CreateStep.SUBCATEGORY -> 3
        CreateStep.FORM -> totalSteps
    }

    fun selectRegion(id: Int, name: String) {
        selectedRegionId = id; selectedRegionName = name
        // Reset dependent geo levels so a stale district/locality can't be submitted.
        selectedDistrictId = null; selectedDistrictName = ""; districts = emptyList()
        selectedLocalityId = null; selectedLocalityName = ""; localities = emptyList()
        geoError = null
        viewModelScope.launch {
            when (val r = repository.getDistricts(id)) {
                is ApiResult.Success -> districts = r.data
                is ApiResult.Error -> geoError = "Не удалось загрузить районы: ${r.message}"
            }
        }
    }
    fun selectDistrict(id: Int, name: String) {
        selectedDistrictId = id; selectedDistrictName = name
        selectedLocalityId = null; selectedLocalityName = ""; localities = emptyList()
        geoError = null
        viewModelScope.launch {
            when (val r = repository.getLocalities(id)) {
                is ApiResult.Success -> localities = r.data
                is ApiResult.Error -> geoError = "Не удалось загрузить населённые пункты: ${r.message}"
            }
        }
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
                if (filtered.isEmpty()) { item { Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp), fontSize = 16.sp) } }
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
        Column {
            AppTopBar(
                title = when (viewModel.step) {
                    CreateStep.TYPE -> "Новое объявление"
                    CreateStep.CATEGORY -> "Выберите категорию"
                    CreateStep.SUBCATEGORY -> viewModel.selectedCategory?.name ?: ""
                    CreateStep.FORM -> "Новое объявление"
                },
                onBack = if (viewModel.step != CreateStep.TYPE) { { viewModel.goBack() } } else null,
            )
            LinearProgressIndicator(
                progress = { viewModel.stepNumber.toFloat() / viewModel.totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Шаг ${viewModel.stepNumber} из ${viewModel.totalSteps}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }) { padding ->
        when (viewModel.step) {
            // Step 1: Type
            CreateStep.TYPE -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Что размещаем?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(32.dp))
                    listOf(
                        Triple("sale", "Продать" to Icons.Outlined.Storefront, "Зерно, технику, скот и др."),
                        Triple("service", "Агроуслуги" to Icons.Outlined.Build, "Обработка, перевозка, ремонт"),
                        Triple("land", "Земли СХ назначения" to Icons.Outlined.Terrain, "Продажа и аренда участков"),
                    ).forEach { (t, labelIcon, desc) ->
                        val (label, icon) = labelIcon
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { viewModel.selectType(t) },
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(viewModel.categoriesError ?: "Не удалось загрузить категории", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadData() }) { Text("Повторить") }
                            }
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(viewModel.categories) { cat ->
                            Card(
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewModel.selectCategory(cat) },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    CategoryIcon(categoryName = cat.name, size = 44.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // Step 3: Subcategory list
            CreateStep.SUBCATEGORY -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Выберите подкатегорию:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    viewModel.selectedCategory?.children?.forEach { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.selectSubCategory(sub) },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(sub.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                val typeLabel = when (viewModel.type) { "sale" -> "Продажа"; "rent" -> "Аренда"; "service" -> "Агроуслуги"; else -> viewModel.type }
                                val path = listOfNotNull(typeLabel, viewModel.selectedCategory?.name, viewModel.selectedSubCategory?.name).joinToString(" \u203A ")
                                Text(path, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        item { OutlinedTextField(value = viewModel.title, onValueChange = { viewModel.title = it }, label = { Text("Название *", fontSize = 16.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.description, onValueChange = { viewModel.description = it }, label = { Text("Описание", fontSize = 16.sp) }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.price, onValueChange = { viewModel.price = it }, label = { Text("Цена (\u20BD)", fontSize = 16.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
                        item { OutlinedTextField(value = viewModel.phonePrimary, onValueChange = { viewModel.phonePrimary = it }, label = { Text("Телефон *", fontSize = 16.sp) }, placeholder = { Text("+79001234567") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }

                        // Region picker
                        item {
                            Surface(modifier = Modifier.fillMaxWidth().clickable { showRegionPicker = true }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Регион *", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = viewModel.selectedRegionName.ifBlank { "Нажмите для выбора" }, fontSize = 17.sp, color = if (viewModel.selectedRegionName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        }

                        // District picker (optional) — shown once a region is chosen
                        if (viewModel.selectedRegionId != null) {
                            item {
                                Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.districts.isNotEmpty()) { showDistrictPicker = true }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Район", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val districtHint = if (viewModel.districts.isEmpty()) "Нет районов" else "Не выбран"
                                            Text(text = viewModel.selectedDistrictName.ifBlank { districtHint }, fontSize = 17.sp, color = if (viewModel.selectedDistrictName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        // Locality picker (optional) — shown once a district is chosen
                        if (viewModel.selectedDistrictId != null) {
                            item {
                                Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.localities.isNotEmpty()) { showLocalityPicker = true }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Населённый пункт", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val localityHint = if (viewModel.localities.isEmpty()) "Нет населённых пунктов" else "Не выбран"
                                            Text(text = viewModel.selectedLocalityName.ifBlank { localityHint }, fontSize = 17.sp, color = if (viewModel.selectedLocalityName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        // Photos
                        item {
                            Text("Фото (мин. 2, макс. 10)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(viewModel.photoUris) { uri ->
                                    Box(modifier = Modifier.size(90.dp)) {
                                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                                        IconButton(onClick = { viewModel.removePhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                                item {
                                    Box(modifier = Modifier.size(90.dp).clip(MaterialTheme.shapes.small).border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.small).clickable {
                                        try { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } catch (e: Exception) { fallbackLauncher.launch("image/*") }
                                    }, contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Text("Добавить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                    }
                                }
                            }
                            Text("${viewModel.photoUris.size} из 10 фото", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        viewModel.geoError?.let { err -> item { ErrorBanner(message = err) } }
                        viewModel.error?.let { err -> item { ErrorBanner(message = err) } }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.submitAd(context, onSuccess) }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !viewModel.isLoading, shape = MaterialTheme.shapes.medium) {
                        if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Отправить на модерацию", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
