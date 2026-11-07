package com.example.currencywidget.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import com.example.currencywidget.ui.components.ScreenHeader
import com.example.currencywidget.ui.components.Entrance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.currencywidget.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.data.model.WeatherUnits
import com.example.currencywidget.data.repository.WeatherStore
import com.example.currencywidget.ui.components.CurrencyChips
import com.example.currencywidget.ui.components.CurrencyPickerDialog
import com.example.currencywidget.ui.components.Segmented
import com.example.currencywidget.ui.components.SettingDivider
import com.example.currencywidget.ui.components.SettingRow
import com.example.currencywidget.ui.components.SettingsSection
import com.example.currencywidget.ui.viewmodel.SettingsUiState
import com.example.currencywidget.ui.viewmodel.SettingsViewModel
import com.example.currencywidget.util.LocationHelper
import com.example.currencywidget.widget.AmountInput
import com.example.currencywidget.widget.Money
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@Composable
private fun spanOptions() = listOf(
    BudgetTimeSpan.DAILY to stringResource(R.string.span_day), BudgetTimeSpan.WEEKLY to stringResource(R.string.span_week),
    BudgetTimeSpan.MONTHLY to stringResource(R.string.span_month), BudgetTimeSpan.YEARLY to stringResource(R.string.span_year)
)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.loaded) return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp, end = 28.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        ),
    ) {
        item {
            Entrance(0) {
                ScreenHeader(stringResource(R.string.tab_settings), stringResource(R.string.settings_header_right))
            }
        }
        item { Entrance(1) { RatesSection(state, viewModel) } }
        item { Entrance(2) { BudgetSection(state, viewModel) } }
        item { Entrance(3) { TravelSection(state, viewModel) } }
        item { Entrance(4) { DataSection(viewModel) } }
    }
}

// ---- Exchange rates ------------------------------------------------------------------------

@Composable
private fun RatesSection(state: SettingsUiState, vm: SettingsViewModel) {
    val s = state.settings
    val uri = LocalUriHandler.current
    var key by remember(s.apiKey) { mutableStateOf(s.apiKey) }
    var reveal by remember { mutableStateOf(false) }

    SettingsSection(stringResource(R.string.rates_title), stringResource(R.string.rates_subtitle), Icons.Filled.CurrencyExchange) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text(stringResource(R.string.api_key_label)) },
                singleLine = true,
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.saveApiKey(key) }),
                trailingIcon = { TextButton(onClick = { reveal = !reveal }) { Text(if (reveal) stringResource(R.string.action_hide) else stringResource(R.string.action_show)) } },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { uri.openUri("https://openexchangerates.org/signup/free") }) { Text(stringResource(R.string.api_key_get)) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { vm.saveApiKey(key) }, enabled = key != s.apiKey,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
        if (s.apiKey.isNotBlank()) {
            SettingDivider()
            SettingRow(
                title = stringResource(R.string.last_update),
                summary = state.refreshMessage?.takeIf { state.refreshFailed } ?: lastUpdate(state.lastRefreshedAt).ifEmpty { stringResource(R.string.never) },
                trailing = {
                    if (state.isRefreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = { vm.refreshNow() }) { Text(stringResource(R.string.action_refresh)) }
                }
            )
            Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.update_every), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Segmented(
                    options = listOf(1 to stringResource(R.string.span_day), 3 to stringResource(R.string.span_3_days), 7 to stringResource(R.string.span_week), 30 to stringResource(R.string.span_month)),
                    selected = s.refreshIntervalDays.let { d -> listOf(1, 3, 7, 30).minBy { kotlin.math.abs(it - d) } },
                    onSelect = vm::setRefreshInterval
                )
            }
        }
        SettingDivider()
        if (state.currencies.isEmpty()) {
            SettingRow(
                title = stringResource(R.string.currency_list),
                summary = state.currenciesError ?: stringResource(R.string.loading),
                trailing = { if (state.currenciesError != null) TextButton(onClick = vm::loadCurrencies) { Text(stringResource(R.string.action_retry)) } }
            )
        } else {
            CurrencyChips(
                title = stringResource(R.string.keypad_currencies),
                summary = stringResource(R.string.keypad_currencies_hint),
                selected = s.inputCurrencies,
                max = 3,
                currencies = state.currencies,
                onChange = vm::setInputCurrencies
            )
            SettingDivider()
            CurrencyChips(
                title = stringResource(R.string.converted_to),
                summary = stringResource(R.string.converted_to_hint),
                selected = s.outputCurrencies,
                max = 5,
                currencies = state.currencies,
                onChange = vm::setOutputCurrencies
            )
        }
    }
}

private fun lastUpdate(at: Long): String {
    if (at <= 0) return ""
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at))
}

// ---- Budget ---------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSection(state: SettingsUiState, vm: SettingsViewModel) {
    val s = state.settings
    var pickCurrency by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var editSpent by remember { mutableStateOf(false) }

    SettingsSection(stringResource(R.string.budget_title), stringResource(R.string.budget_subtitle), Icons.Filled.Savings) {
        SettingRow(
            title = stringResource(R.string.budget_track),
            onClick = { vm.setBudgetEnabled(!s.budgetTrackingEnabled) },
            trailing = {
                Switch(
                    checked = s.budgetTrackingEnabled, onCheckedChange = vm::setBudgetEnabled,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        )
        if (!s.budgetTrackingEnabled) return@SettingsSection
        SettingDivider()

        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountField(
                value = s.budgetAmount,
                currency = s.budgetCurrency,
                onCurrencyClick = { pickCurrency = true },
                onCommit = vm::setBudgetAmount
            )
            Text(stringResource(R.string.budget_per), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Segmented(spanOptions(), s.budgetTimeSpan, vm::setBudgetPeriod)
            Text(stringResource(R.string.budget_widgets_per), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Segmented(spanOptions(), s.budgetDisplayTimeSpan, vm::setBudgetDisplayPeriod)
            if (s.budgetTimeSpan != s.budgetDisplayTimeSpan) {
                val factor = days(s.budgetDisplayTimeSpan).toDouble() / days(s.budgetTimeSpan)
                Text(
                    stringResource(
                        when (s.budgetDisplayTimeSpan) {
                            BudgetTimeSpan.DAILY -> R.string.budget_eq_day
                            BudgetTimeSpan.WEEKLY -> R.string.budget_eq_week
                            BudgetTimeSpan.MONTHLY -> R.string.budget_eq_month
                            BudgetTimeSpan.YEARLY -> R.string.budget_eq_year
                        },
                        Money.format(s.budgetAmount * factor, s.budgetCurrency)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        SettingDivider()
        SettingRow(
            title = stringResource(R.string.budget_period_starts),
            summary = stringResource(R.string.budget_period_starts_value, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(s.budgetRefreshStartDate)), "%02d:00".format(s.budgetRefreshHour)),
            onClick = { pickDate = true },
            trailing = { HourMenu(s.budgetRefreshHour) { vm.setBudgetResetStart(s.budgetRefreshStartDate, it) } }
        )
        SettingDivider()
        SettingRow(
            title = stringResource(R.string.budget_spent_this_period),
            summary = Money.format(state.budgetSpent, s.budgetCurrency),
            trailing = {
                Row {
                    TextButton(onClick = { editSpent = true }) { Text(stringResource(R.string.action_edit)) }
                    TextButton(onClick = { vm.setBudgetSpent(0.0) }, enabled = state.budgetSpent > 0) { Text(stringResource(R.string.action_reset)) }
                }
            }
        )
    }

    if (pickCurrency) {
        CurrencyPickerDialog(
            title = stringResource(R.string.budget_currency),
            currencies = state.currencies,
            initial = listOf(s.budgetCurrency),
            max = 1,
            onDismiss = { pickCurrency = false },
            onSave = { it.firstOrNull()?.let(vm::setBudgetCurrency); pickCurrency = false }
        )
    }
    if (pickDate) {
        // DatePicker works in UTC midnight; convert to/from the local day.
        val local = Calendar.getInstance().apply { timeInMillis = s.budgetRefreshStartDate }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = utc.timeInMillis)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        val p = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = picked }
                        val day = Calendar.getInstance().apply {
                            clear(); set(p.get(Calendar.YEAR), p.get(Calendar.MONTH), p.get(Calendar.DAY_OF_MONTH))
                        }
                        vm.setBudgetResetStart(day.timeInMillis, s.budgetRefreshHour)
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = dateState, showModeToggle = false) }
    }
    if (editSpent) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editSpent = false },
            title = { Text(stringResource(R.string.budget_spent_this_period)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    suffix = { Text(s.budgetCurrency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.setBudgetSpent(AmountInput.parse(text)); editSpent = false }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editSpent = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

private fun days(span: BudgetTimeSpan) = when (span) {
    BudgetTimeSpan.DAILY -> 1; BudgetTimeSpan.WEEKLY -> 7; BudgetTimeSpan.MONTHLY -> 30; BudgetTimeSpan.YEARLY -> 365
}

@Composable
private fun AmountField(value: Double, currency: String, onCurrencyClick: () -> Unit, onCommit: (Double) -> Unit) {
    fun fmt(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString().replace('.', ',')
    var text by remember(value) { mutableStateOf(fmt(value)) }
    val commit = {
        val parsed = AmountInput.parse(text)
        if (parsed > 0 && parsed != value) onCommit(parsed) else text = fmt(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
        label = { Text(stringResource(R.string.budget_title)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        trailingIcon = { TextButton(onClick = onCurrencyClick) { Text(currency, fontWeight = FontWeight.SemiBold) } },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (!it.isFocused) commit() }
    )
}

@Composable
private fun HourMenu(hour: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("%02d:00".format(hour)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        (0..23).forEach { h ->
            DropdownMenuItem(text = { Text("%02d:00".format(h)) }, onClick = { onPick(h); open = false })
        }
    }
}

// ---- Travel -------------------------------------------------------------------------------

@Composable
private fun TravelSection(state: SettingsUiState, vm: SettingsViewModel) {
    val s = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usesHere = s.weatherLocation2 == WeatherStore.CURRENT_LOCATION
    var awayMode by remember(usesHere) { mutableStateOf(if (usesHere) 0 else 1) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch {
                LocationHelper.updateCurrentPosition(context)
                vm.setPlace(WeatherStore.AWAY, WeatherStore.CURRENT_LOCATION)
            }
        } else {
            permissionDenied = true
            awayMode = 1
        }
    }

    SettingsSection(stringResource(R.string.travel_title), stringResource(R.string.travel_subtitle), Icons.Filled.Flight) {
        PlaceField(
            label = stringResource(R.string.place_home),
            placeholder = stringResource(R.string.travel_home_hint),
            value = s.weatherLocation1,
            slot = WeatherStore.HOME,
            state = state,
            vm = vm
        )
        SettingDivider()
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(stringResource(R.string.place_here), style = MaterialTheme.typography.bodyLarge)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        awayMode = 0
                        permissionDenied = false
                        // Already granted → the callback fires immediately with true.
                        permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = awayMode == 0, onClick = null)
                Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.travel_where_phone))
                    Text(
                        if (permissionDenied) stringResource(R.string.travel_location_denied) else stringResource(R.string.travel_location_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (permissionDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        awayMode = 1
                        if (usesHere) vm.setPlace(WeatherStore.AWAY, "")
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = awayMode == 1, onClick = null)
                Text(stringResource(R.string.travel_fixed_place), Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
            }
        }
        if (awayMode == 1) {
            PlaceField(
                label = stringResource(R.string.travel_second_place),
                placeholder = stringResource(R.string.travel_second_hint),
                value = if (usesHere) "" else s.weatherLocation2,
                slot = WeatherStore.AWAY,
                state = state,
                vm = vm
            )
        }
        SettingDivider()
        Column(Modifier.padding(vertical = 8.dp)) {
            Segmented(
                options = listOf(WeatherUnits.METRIC to "°C", WeatherUnits.IMPERIAL to "°F"),
                selected = s.weatherUnits,
                onSelect = vm::setWeatherUnits
            )
        }
    }
}

@Composable
private fun PlaceField(
    label: String,
    placeholder: String,
    value: String,
    slot: Int,
    state: SettingsUiState,
    vm: SettingsViewModel
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; vm.searchPlaces(slot, it) },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (text != value) vm.setPlace(slot, text) }),
            trailingIcon = {
                if (state.searching && state.suggestionsFor == slot) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else if (text.isNotEmpty()) TextButton(onClick = { text = ""; vm.setPlace(slot, "") }) { Text(stringResource(R.string.action_clear)) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
        if (focused && state.suggestionsFor == slot && state.suggestions.isNotEmpty()) {
            state.suggestions.forEach { place ->
                Text(
                    place.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { text = place.label; vm.pickPlace(slot, place) }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ---- Data & about ------------------------------------------------------------------------

@Composable
private fun DataSection(vm: SettingsViewModel) {
    var confirm by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    SettingsSection(stringResource(R.string.about_title), null, Icons.Filled.Info) {
        SettingRow(
            title = stringResource(R.string.data_sources),
            summary = stringResource(R.string.data_sources_text),
            onClick = { uri.openUri("https://open-meteo.com/") }
        )
        SettingDivider()
        SettingRow(
            title = stringResource(R.string.reset_everything),
            summary = stringResource(R.string.reset_everything_hint),
            onClick = { confirm = true }
        )
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { vm.resetAllData(); confirm = false }) {
                    Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}
