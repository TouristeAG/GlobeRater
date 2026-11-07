package com.example.currencywidget.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.currencywidget.R
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencywidget.data.repository.PlaceWeather
import com.example.currencywidget.data.repository.WeatherCodes
import com.example.currencywidget.ui.components.CurrencyBadge
import com.example.currencywidget.ui.components.Hairline
import com.example.currencywidget.ui.components.OutlinedPanel
import com.example.currencywidget.ui.components.Entrance
import com.example.currencywidget.ui.components.ProgressLine
import com.example.currencywidget.ui.components.RollingText
import com.example.currencywidget.ui.components.Rule
import com.example.currencywidget.ui.components.ScreenHeader
import com.example.currencywidget.ui.components.SectionLabel
import com.example.currencywidget.ui.components.pressScale
import com.example.currencywidget.ui.viewmodel.HomeUiState
import com.example.currencywidget.ui.viewmodel.HomeViewModel
import com.example.currencywidget.widget.AmountInput
import com.example.currencywidget.widget.Money
import com.example.currencywidget.widget.TravelClockWidgetProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TodayScreen(
    state: HomeUiState,
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit
) {
    var addingExpense by remember { mutableStateOf(false) }
    val today = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp, end = 28.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        )
    ) {
        item { Entrance(0) { ScreenHeader(state.home?.label ?: "GlobeRater", today) } }
        item { Entrance(1) { AmountHero(state, viewModel, onOpenSettings) } }
        item { Entrance(2) { Conversions(state, viewModel) } }
        state.budget?.let { b ->
            item {
                Entrance(3) {
                    Budget(
                        period = viewModel.periodLabel(state.settings.budgetDisplayTimeSpan),
                        spent = b.spent, total = b.total, currency = b.currency,
                        nextReset = state.budgetNextReset,
                        onAdd = { addingExpense = true }
                    )
                }
            }
        }
        item { Entrance(4) { Travel(state, onOpenSettings) } }
    }

    if (addingExpense) {
        ExpenseDialog(
            currencies = state.sources.ifEmpty { listOf(state.budget?.currency ?: "USD") },
            initialCurrency = state.from,
            onDismiss = { addingExpense = false },
            onAdd = { amount, code -> viewModel.addExpense(amount, code); addingExpense = false }
        )
    }
}

// ---- Hero: the amount ------------------------------------------------------------------------

@Composable
private fun AmountHero(state: HomeUiState, vm: HomeViewModel, onOpenSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(top = 36.dp, bottom = 28.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            BasicTextField(
                value = state.amount,
                onValueChange = vm::setAmount,
                singleLine = true,
                textStyle = MaterialTheme.typography.displayLarge.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                // Stored with ',' as decimal mark, shown with the phone's own separator.
                visualTransformation = {
                    androidx.compose.ui.text.input.TransformedText(
                        androidx.compose.ui.text.AnnotatedString(it.text.replace(',', Money.decimalSeparator())),
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                },
                modifier = Modifier.weight(1f)
            )
            CurrencyMenu(state.from, state.sources, onPick = vm::setFrom)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    state.refreshError != null -> state.refreshError
                    state.sampleRates -> stringResource(R.string.rates_example)
                    else -> updatedText(state.ratesUpdatedAt)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.refreshError != null) cs.error else cs.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            when {
                state.isRefreshing -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 1.5.dp, color = cs.primary)
                state.sampleRates -> TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.rates_add_live), color = cs.primary) }
                else -> IconButton(onClick = vm::refreshRates) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.rates_refresh_cd), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CurrencyMenu(code: String, options: List<String>, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(top = 18.dp)) {
        Row(
            Modifier.clickable { open = true }.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.change_currency), modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { c ->
                DropdownMenuItem(
                    leadingIcon = { CurrencyBadge(c, size = 28.dp) },
                    text = { Text(c) },
                    onClick = { onPick(c); open = false }
                )
            }
        }
    }
}

// ---- Conversions -----------------------------------------------------------------------------

@Composable
private fun Conversions(state: HomeUiState, vm: HomeViewModel) {
    Column(Modifier.padding(bottom = 20.dp)) {
        SectionLabel(stringResource(R.string.section_converted))
        OutlinedPanel(padding = 0.dp) {
            Column(Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
                state.targets.take(6).forEachIndexed { i, code ->
                    if (i > 0) Hairline(Modifier.padding(horizontal = 14.dp))
                    val value = state.convert(code)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = value != null) { vm.swapTo(code) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CurrencyBadge(code, size = 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(code, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        RollingText(
                            value?.let { Money.number(it, code) } ?: "—",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

// ---- Budget ----------------------------------------------------------------------------------

@Composable
private fun Budget(@androidx.annotation.StringRes period: Int, spent: Double, total: Double, currency: String, nextReset: Long, onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val spentAnimated by animateFloatAsState(
        if (shown) spent.toFloat() else 0f, tween(1100, easing = FastOutSlowInEasing), label = "spent"
    )
    val ratio = if (total > 0) (spent / total).toFloat() else 0f
    val left = total - spent
    Column(Modifier.padding(bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.budget_spent_period, stringResource(period)), Modifier.weight(1f))
            val press = remember { MutableInteractionSource() }
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(10.dp),
                color = cs.primary,
                contentColor = cs.onPrimary,
                interactionSource = press,
                modifier = Modifier.padding(bottom = 6.dp).size(40.dp).pressScale(press)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_expense), modifier = Modifier.size(18.dp))
                }
            }
        }
        OutlinedPanel {
          Column {
            Text(
                Money.format(spentAnimated.toDouble(), currency),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            ProgressLine(if (shown) ratio else 0f, color = if (left < 0) cs.error else cs.primary)
            Row(Modifier.padding(top = 10.dp)) {
            Text(
                if (left >= 0) stringResource(R.string.budget_left, Money.format(left, currency)) else stringResource(R.string.budget_over, Money.format(-left, currency)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (left < 0) cs.error else cs.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (nextReset > 0) {
                Text(
                    stringResource(R.string.budget_resets, SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(nextReset))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
            }
          }
        }
    }
}

@Composable
private fun ExpenseDialog(currencies: List<String>, initialCurrency: String, onDismiss: () -> Unit, onAdd: (Double, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var code by remember { mutableStateOf(if (initialCurrency in currencies) initialCurrency else currencies.first()) }
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        title = { Text(stringResource(R.string.add_expense), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    placeholder = { Text("0") },
                    suffix = { Text(code) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencies.take(4).forEach { c ->
                        FilterChip(
                            selected = c == code,
                            onClick = { code = c },
                            label = { Text(c) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cs.primary, selectedLabelColor = cs.onPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(AmountInput.parse(text), code) }, enabled = AmountInput.parse(text) > 0) {
                Text(stringResource(R.string.action_add), color = cs.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = cs.onSurfaceVariant) } }
    )
}

// ---- Travel ----------------------------------------------------------------------------------

@Composable
private fun Travel(state: HomeUiState, onOpenSettings: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            now = System.currentTimeMillis()
        }
    }
    val here = state.hereZone
    val home = state.homeZone
    Column {
        SectionLabel(stringResource(R.string.section_home_here))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Place(Modifier.weight(1f), stringResource(R.string.place_home), state.home?.label ?: "—", home ?: here, state.home, now)
            Place(Modifier.weight(1f), stringResource(R.string.place_here), state.hereLabel.ifBlank { state.away?.label ?: TravelClockWidgetProvider.cityOf(here.id) }, here, state.away, now)
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (home != null) offsetText(TravelClockWidgetProvider.offsetMinutes(home, here, now)) else stringResource(R.string.travel_set_home_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (home == null) TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.travel_set_home), color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/** Same tile as the clock widget: outlined by day, solid at night. */
@Composable
private fun Place(modifier: Modifier, role: String, label: String, zone: TimeZone, weather: PlaceWeather?, now: Long) {
    val cs = MaterialTheme.colorScheme
    val night = remember(now, zone) {
        java.util.Calendar.getInstance(zone).apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY) !in 6..19
    }
    val ink = if (night) cs.onPrimary else cs.primary
    val time = remember(now, zone) { SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = zone }.format(Date(now)) }
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (night) Modifier.background(cs.primary)
                else Modifier.border(2.5.dp, cs.primary, RoundedCornerShape(12.dp))
            )
            .padding(14.dp)
    ) {
        Text(stringResource(R.string.place_role_label, role, label), style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(time, style = MaterialTheme.typography.displaySmall, color = ink, modifier = Modifier.padding(vertical = 2.dp))
        if (weather != null) {
            val today = weather.days.firstOrNull()
            Row(verticalAlignment = Alignment.CenterVertically) {
                val drift = rememberInfiniteTransition(label = "drift")
                val dx by drift.animateFloat(
                    -1.5f, 1.5f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dx"
                )
                Icon(
                    painterResource(WeatherCodes.icon(weather.currentCode, weather.isDay)),
                    contentDescription = stringResource(WeatherCodes.describeRes(weather.currentCode)),
                    tint = ink,
                    modifier = Modifier.size(22.dp).graphicsLayer { translationX = dx * density }
                )
                Spacer(Modifier.width(8.dp))
                Text(WeatherCodes.temp(weather.currentTemp), style = MaterialTheme.typography.titleLarge, color = ink)
            }
            if (today != null) {
                Text(
                    "${WeatherCodes.temp(today.max)} / ${WeatherCodes.temp(today.min)}",
                    style = MaterialTheme.typography.bodySmall, color = ink
                )
            }
        }
    }
}

@Composable
internal fun updatedText(at: Long): String {
    if (at <= 0L) return stringResource(R.string.updated_never)
    val minutes = ((System.currentTimeMillis() - at) / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.updated_just_now)
        minutes < 60 -> stringResource(R.string.updated_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.updated_hours, minutes / 60)
        else -> stringResource(R.string.updated_days, minutes / (24 * 60))
    }
}

/** "+6 h vs home", "−4 h 30 vs home", "Same time as home". */
@Composable
internal fun offsetText(minutes: Int): String {
    if (minutes == 0) return stringResource(R.string.offset_same)
    val abs = kotlin.math.abs(minutes)
    val amount = if (abs % 60 == 0) stringResource(R.string.offset_hours, abs / 60)
    else stringResource(R.string.offset_hours_minutes, abs / 60, abs % 60)
    return stringResource(if (minutes > 0) R.string.offset_ahead else R.string.offset_behind, amount)
}
