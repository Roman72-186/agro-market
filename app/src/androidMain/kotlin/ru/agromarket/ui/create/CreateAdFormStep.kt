package ru.agromarket.ui.create

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.agromarket.ui.components.ErrorBanner

/** Step 4: ad details form — title, price, location, photos and submit. */
@Composable
fun CreateAdFormStep(
    viewModel: CreateAdViewModel,
    onSubmit: () -> Unit,
    onAddPhoto: () -> Unit,
    onShowRegionPicker: () -> Unit,
    onShowDistrictPicker: () -> Unit,
    onShowLocalityPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp).imePadding()) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Show selected path
            item {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    val typeLabel = when (viewModel.type) { "sale" -> "Продажа"; "rent" -> "Аренда"; "service" -> "Агроуслуги"; else -> viewModel.type }
                    val path = listOfNotNull(typeLabel, viewModel.selectedCategory?.name, viewModel.selectedSubCategory?.name).joinToString(" › ")
                    Text(path, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            item { OutlinedTextField(value = viewModel.title, onValueChange = { viewModel.title = it }, label = { Text("Название *", fontSize = 16.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
            item { OutlinedTextField(value = viewModel.description, onValueChange = { viewModel.description = it }, label = { Text("Описание", fontSize = 16.sp) }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
            item { OutlinedTextField(value = viewModel.price, onValueChange = { viewModel.price = it }, label = { Text("Цена (₽)", fontSize = 16.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }
            item { OutlinedTextField(value = viewModel.phonePrimary, onValueChange = { viewModel.phonePrimary = it }, label = { Text("Телефон *", fontSize = 16.sp) }, placeholder = { Text("+79001234567") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontSize = 17.sp)) }

            // Region picker
            item {
                Surface(modifier = Modifier.fillMaxWidth().clickable { onShowRegionPicker() }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
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
                    Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.districts.isNotEmpty()) { onShowDistrictPicker() }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
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
                    Surface(modifier = Modifier.fillMaxWidth().clickable(enabled = viewModel.localities.isNotEmpty()) { onShowLocalityPicker() }, shape = MaterialTheme.shapes.medium, border = ButtonDefaults.outlinedButtonBorder) {
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
                Text("Фото (мин. ${CreateAdViewModel.MIN_PHOTOS}, макс. ${CreateAdViewModel.MAX_PHOTOS})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                val totalPhotos = viewModel.existingPhotos.size + viewModel.photoUris.size
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.existingPhotos, key = { it.id }) { photo ->
                        Box(modifier = Modifier.size(90.dp)) {
                            AsyncImage(model = photo.url, contentDescription = null, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                            IconButton(onClick = { viewModel.removeExistingPhoto(photo) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    items(viewModel.photoUris) { uri ->
                        Box(modifier = Modifier.size(90.dp)) {
                            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                            IconButton(onClick = { viewModel.removePhoto(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                    if (totalPhotos < CreateAdViewModel.MAX_PHOTOS) {
                        item {
                            Box(modifier = Modifier.size(90.dp).clip(MaterialTheme.shapes.small).border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.shapes.small).clickable { onAddPhoto() }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)); Text("Добавить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                }
                Text("$totalPhotos из ${CreateAdViewModel.MAX_PHOTOS} фото", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            viewModel.geoError?.let { err -> item { ErrorBanner(message = err) } }
            viewModel.error?.let { err -> item { ErrorBanner(message = err) } }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !viewModel.isLoading, shape = MaterialTheme.shapes.medium) {
            if (viewModel.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (viewModel.isEditMode) "Отправить на повторную проверку" else "Отправить на модерацию", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
