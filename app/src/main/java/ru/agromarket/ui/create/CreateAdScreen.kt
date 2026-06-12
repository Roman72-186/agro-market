package ru.agromarket.ui.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ru.agromarket.data.draft.AdDraft
import ru.agromarket.data.draft.AdDraftManager
import ru.agromarket.data.model.*
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.theme.AgroAccentClay
import ru.agromarket.utils.FileUtils
import javax.inject.Inject

enum class CreateStep { TYPE, CATEGORY, SUBCATEGORY, FORM }

@HiltViewModel
class CreateAdViewModel @Inject constructor(
    private val repository: AgroRepository,
    private val draftManager: AdDraftManager,
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
    var draftAvailable by mutableStateOf(false)
    private var pendingDraft: AdDraft? = null

    init {
        loadData()
        viewModelScope.launch {
            draftManager.load()?.takeIf { it.isMeaningful }?.let {
                pendingDraft = it
                draftAvailable = true
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            categoriesError = null
            when (val r = repository.getCategories()) {
                is ApiResult.Success -> {
                    categories = r.data
                    categoriesError = null
                    syncCategoryFromId() // a restored draft may have arrived before the tree
                }
                is ApiResult.Error -> categoriesError = r.message
            }
        }
        viewModelScope.launch { (repository.getRegions() as? ApiResult.Success)?.let { regions = it.data } }
    }

    /** Snapshot of the draft-relevant wizard state; observed by the screen for debounced autosave. */
    fun currentDraft() = AdDraft(
        type = type,
        title = title,
        description = description,
        price = price,
        phonePrimary = phonePrimary,
        categoryId = selectedCategoryId,
        regionId = selectedRegionId,
        regionName = selectedRegionName,
        districtId = selectedDistrictId,
        districtName = selectedDistrictName,
        localityId = selectedLocalityId,
        localityName = selectedLocalityName,
    )

    fun persistDraft(draft: AdDraft) {
        if (draft.isMeaningful) viewModelScope.launch { draftManager.save(draft) }
    }

    fun restoreDraft() {
        val d = pendingDraft ?: return
        pendingDraft = null
        draftAvailable = false
        type = d.type
        title = d.title
        description = d.description
        price = d.price
        phonePrimary = d.phonePrimary
        selectedRegionId = d.regionId; selectedRegionName = d.regionName
        selectedDistrictId = d.districtId; selectedDistrictName = d.districtName
        selectedLocalityId = d.localityId; selectedLocalityName = d.localityName
        selectedCategoryId = d.categoryId
        syncCategoryFromId()
        // Refill dependent geo lists so the cascade pickers aren't empty after restore
        d.regionId?.let { id -> viewModelScope.launch { (repository.getDistricts(id) as? ApiResult.Success)?.let { districts = it.data } } }
        d.districtId?.let { id -> viewModelScope.launch { (repository.getLocalities(id) as? ApiResult.Success)?.let { localities = it.data } } }
        step = if (d.categoryId != null) CreateStep.FORM else CreateStep.TYPE
    }

    fun dismissDraft() {
        pendingDraft = null
        draftAvailable = false
        viewModelScope.launch { draftManager.clear() }
    }

    /** Re-link selectedCategory/selectedSubCategory from a bare category id (draft restore). */
    private fun syncCategoryFromId() {
        val id = selectedCategoryId ?: return
        if (selectedCategory != null) return
        categories.firstOrNull { it.id == id }?.let { selectedCategory = it; return }
        for (parent in categories) {
            parent.children.firstOrNull { it.id == id }?.let {
                selectedCategory = parent
                selectedSubCategory = it
                return
            }
        }
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
                        is ApiResult.Success -> {
                            draftManager.clear() // the ad is published; the draft has served its purpose
                            onSuccess()
                        }
                        is ApiResult.Error -> error = submitResult.message
                    }
                }
                is ApiResult.Error -> error = r.message
            }
            isLoading = false
        }
    }
}

/**
 * Segmented wizard progress: passed steps in olive (primary), the current segment in clay,
 * the ones ahead in surfaceVariant, plus a "Шаг X из Y · Название" caption — three visually
 * distinct states raise multi-step form completion (UXPin/Eleken).
 */
@Composable
private fun WizardStepper(stepNumber: Int, totalSteps: Int, stepTitle: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(totalSteps) { index ->
                val color = when {
                    index < stepNumber - 1 -> MaterialTheme.colorScheme.primary
                    index == stepNumber - 1 -> AgroAccentClay
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(color))
            }
        }
        Text(
            text = "Шаг $stepNumber из $totalSteps · $stepTitle",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
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

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun CreateAdScreen(onSuccess: () -> Unit, onBack: () -> Unit, viewModel: CreateAdViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }
    val fallbackLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) viewModel.addPhotos(uris) }
    var showRegionPicker by remember { mutableStateOf(false) }
    var showDistrictPicker by remember { mutableStateOf(false) }
    var showLocalityPicker by remember { mutableStateOf(false) }

    // Autosave: persist the draft ~800ms after the last change, so an interrupted
    // submission (call, crash, accidental back) doesn't lose the typed-in ad.
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.currentDraft() }
            .drop(1) // skip the initial empty/restored value
            .debounce(800)
            .collect { viewModel.persistDraft(it) }
    }

    if (viewModel.draftAvailable) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDraft() },
            title = { Text("Продолжить черновик?") },
            text = { Text("Есть незаконченное объявление. Продолжить заполнение? Фото нужно будет добавить заново.") },
            confirmButton = { TextButton(onClick = { viewModel.restoreDraft() }) { Text("Продолжить") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissDraft() }) { Text("Начать заново") } },
        )
    }

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
            WizardStepper(
                stepNumber = viewModel.stepNumber,
                totalSteps = viewModel.totalSteps,
                stepTitle = when (viewModel.step) {
                    CreateStep.TYPE -> "Тип"
                    CreateStep.CATEGORY -> "Категория"
                    CreateStep.SUBCATEGORY -> "Подкатегория"
                    CreateStep.FORM -> "Детали и фото"
                },
            )
        }
    }) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (viewModel.step) {
            CreateStep.TYPE -> CreateAdTypeStep(viewModel, contentModifier)
            CreateStep.CATEGORY -> CreateAdCategoryStep(viewModel, contentModifier)
            CreateStep.SUBCATEGORY -> CreateAdSubCategoryStep(viewModel, contentModifier)
            CreateStep.FORM -> CreateAdFormStep(
                viewModel = viewModel,
                onSubmit = { viewModel.submitAd(context, onSuccess) },
                onAddPhoto = {
                    try { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } catch (e: Exception) { fallbackLauncher.launch("image/*") }
                },
                onShowRegionPicker = { showRegionPicker = true },
                onShowDistrictPicker = { showDistrictPicker = true },
                onShowLocalityPicker = { showLocalityPicker = true },
                modifier = contentModifier,
            )
        }
    }
}
