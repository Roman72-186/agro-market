package ru.agromarket.ui.boost

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.agromarket.data.model.BoostPaymentType
import ru.agromarket.data.payment.PaymentGateway
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar

class BoostPurchaseViewModel(
    private val payment: PaymentGateway,
) : ViewModel() {
    var selectedType by mutableStateOf(BoostPaymentType.BOOST_30)
    var isPaying by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    /** Не-null → нужно открыть форму оплаты ЮKassa по этому URL. */
    var paymentUrl by mutableStateOf<String?>(null)
    /** true → оплата сымитирована (демо), URL нет — просто закрываем экран. */
    var mockDone by mutableStateOf(false)

    fun pay(adId: String) {
        if (isPaying) return
        viewModelScope.launch {
            isPaying = true
            error = null
            when (val r = payment.startBoostPayment(adId, selectedType)) {
                is ApiResult.Success -> if (r.data != null) paymentUrl = r.data else mockDone = true
                is ApiResult.Error -> error = r.message
            }
            isPaying = false
        }
    }
}

@Composable
fun BoostPurchaseScreen(
    adId: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: BoostPurchaseViewModel = koinViewModel(),
) {
    val context = LocalContext.current

    // Реальная оплата: открыть confirmation_url ЮKassa в браузере, затем вернуться.
    LaunchedEffect(viewModel.paymentUrl) {
        viewModel.paymentUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            viewModel.paymentUrl = null
            onSuccess()
        }
    }
    // Демо-режим: оплата сымитирована, просто закрываем экран.
    LaunchedEffect(viewModel.mockDone) {
        if (viewModel.mockDone) onSuccess()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Продвижение в топ", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Поднимите объявление в топ выдачи и выделите его бейджем «В топе».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Срок", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            DurationCard(
                title = "7 дней",
                selected = viewModel.selectedType == BoostPaymentType.BOOST_7,
                onSelect = { viewModel.selectedType = BoostPaymentType.BOOST_7 },
            )
            DurationCard(
                title = "30 дней",
                subtitle = "выгоднее",
                selected = viewModel.selectedType == BoostPaymentType.BOOST_30,
                onSelect = { viewModel.selectedType = BoostPaymentType.BOOST_30 },
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(
                    "Стоимость и оплата — на защищённой странице ЮKassa. " +
                        "Объявление поднимется после подтверждения оплаты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            viewModel.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { viewModel.pay(adId) },
                enabled = !viewModel.isPaying,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (viewModel.isPaying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Создаём платёж…")
                } else {
                    Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Перейти к оплате")
                }
            }
        }
    }
}

@Composable
private fun DurationCard(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    subtitle: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onSelect),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.cardColors(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
