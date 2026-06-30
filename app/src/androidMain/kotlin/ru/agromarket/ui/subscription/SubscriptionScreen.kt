package ru.agromarket.ui.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.agromarket.data.model.SubscriptionPlan
import ru.agromarket.data.model.SubscriptionResponse
import ru.agromarket.data.model.kopecksToRubLabel
import ru.agromarket.data.payment.PaymentGateway
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import ru.agromarket.ui.components.AppTopBar
import ru.agromarket.ui.theme.AgroAccentClay

class SubscriptionViewModel(
    private val repository: AgroRepository,
    private val payment: PaymentGateway,
) : ViewModel() {
    var plans by mutableStateOf<List<SubscriptionPlan>>(emptyList())
    var current by mutableStateOf<SubscriptionResponse?>(null)
    var isLoading by mutableStateOf(true)
    var selectedPlan by mutableStateOf("PRO")
    var selectedMonths by mutableStateOf(1)
    var isPaying by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var success by mutableStateOf(false)

    val monthOptions = listOf(1, 3, 12)

    init { load() }

    private fun load() {
        viewModelScope.launch {
            isLoading = true
            when (val r = repository.getSubscriptionPlans()) {
                is ApiResult.Success -> plans = r.data.plans
                is ApiResult.Error -> error = r.message
            }
            // Текущая подписка — мягкая ошибка: экран полезен и без неё.
            (repository.getMySubscription() as? ApiResult.Success)?.let { current = it.data }
            isLoading = false
        }
    }

    fun planByCode(code: String): SubscriptionPlan? = plans.firstOrNull { it.code == code }

    /** Цена выбранного платного плана за выбранный срок (копейки). */
    fun totalKopecks(): Long = (planByCode(selectedPlan)?.priceKopecks ?: 0L) * selectedMonths

    fun subscribe() {
        if (isPaying) return
        viewModelScope.launch {
            isPaying = true
            error = null
            when (val r = payment.purchaseSubscription(selectedPlan, selectedMonths)) {
                is ApiResult.Success -> { current = r.data; success = true }
                is ApiResult.Error -> error = r.message
            }
            isPaying = false
        }
    }
}

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onSubscribed: () -> Unit = {},
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    LaunchedEffect(viewModel.success) {
        if (viewModel.success) onSubscribed()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = "Тариф", onBack = onBack)

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            viewModel.current?.takeIf { it.status == "active" }?.let { sub ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = AgroAccentClay)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Активна подписка ${viewModel.planByCode(sub.plan)?.name ?: sub.plan}" +
                                (sub.expiresAt?.let { " · до ${it.take(10)}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Text(
                "Pro — для дилеров и активных продавцов: безлимит объявлений, несколько регионов и бейдж доверия.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            viewModel.plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = viewModel.selectedPlan == plan.code,
                    selectable = plan.priceKopecks > 0, // FREE выбирать/покупать не нужно
                    onSelect = { viewModel.selectedPlan = plan.code },
                )
            }

            val paidSelected = (viewModel.planByCode(viewModel.selectedPlan)?.priceKopecks ?: 0L) > 0
            if (paidSelected) {
                Text("Срок", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.monthOptions.forEach { m ->
                        FilterChip(
                            selected = viewModel.selectedMonths == m,
                            onClick = { viewModel.selectedMonths = m },
                            label = { Text(monthsLabel(m)) },
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("К оплате", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                viewModel.totalKopecks().kopecksToRubLabel(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            "${viewModel.planByCode(viewModel.selectedPlan)?.name} · ${monthsLabel(viewModel.selectedMonths)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(
                        "Оплата имитируется — реальные деньги не списываются.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                viewModel.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = { viewModel.subscribe() },
                    enabled = !viewModel.isPaying,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (viewModel.isPaying) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Оформление…")
                    } else {
                        Text("Оформить Pro (демо)")
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    selected: Boolean,
    selectable: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selectable) Modifier.selectable(selected = selected, onClick = onSelect) else Modifier),
        border = if (selected && selectable) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectable) {
                    RadioButton(selected = selected, onClick = onSelect)
                    Spacer(Modifier.width(8.dp))
                }
                Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    if (plan.priceKopecks > 0) "${plan.priceKopecks.kopecksToRubLabel()}/мес" else "бесплатно",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            plan.perks.forEach { perk ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.Check, null, tint = AgroAccentClay, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(perk, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun monthsLabel(m: Int): String = when (m) {
    1 -> "1 мес"
    3 -> "3 мес"
    12 -> "12 мес"
    else -> "$m мес"
}
