package com.example.currencywidget.ui.viewmodel

import android.content.Context
import com.example.currencywidget.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencywidget.data.model.AppSettings
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.data.repository.CurrencyRepository
import com.example.currencywidget.data.repository.PlaceWeather
import com.example.currencywidget.data.repository.SettingsRepository
import com.example.currencywidget.data.repository.SharedBudgetManager
import com.example.currencywidget.data.repository.WeatherRepository
import com.example.currencywidget.data.repository.WeatherStore
import com.example.currencywidget.service.WorkScheduler
import com.example.currencywidget.util.BudgetRefreshCalculator
import com.example.currencywidget.util.LocationHelper
import com.example.currencywidget.widget.AmountInput
import com.example.currencywidget.widget.BudgetSnapshot
import com.example.currencywidget.widget.RatesCache
import com.example.currencywidget.widget.TravelClockWidgetProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class RateLine(val code: String, val value: Double?)

data class HomeUiState(
    val loaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val ratesUpdatedAt: Long = 0L,
    val isRefreshing: Boolean = false,
    val refreshError: String? = null,
    val baseCurrency: String = "USD",
    val rateLines: List<RateLine> = emptyList(),
    val budget: BudgetSnapshot? = null,
    val budgetNextReset: Long = 0L,
    val home: PlaceWeather? = null,
    val away: PlaceWeather? = null,
    val homeZone: TimeZone? = null,
    /** Second place's zone: a fixed place's own zone, or the phone's. */
    val hereZone: TimeZone = TimeZone.getDefault(),
    val hereLabel: String = "",
    /** Bumped whenever cached data changes so widget previews re-render. */
    val version: Int = 0,
    // In-app converter
    val amount: String = "100",
    val from: String = "USD",
    val rates: Map<String, Double> = emptyMap(),
    val sampleRates: Boolean = true
) {
    val amountValue: Double get() = AmountInput.parse(amount)
    /** Every configured currency except the one typed in. */
    val targets: List<String> get() = (settings.outputCurrencies + settings.inputCurrencies).distinct().filter { it != from }
    val sources: List<String> get() = (settings.inputCurrencies + settings.outputCurrencies).distinct()
    fun convert(to: String): Double? = RatesCache.convert(amountValue, from, to, rates)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Called on every resume: cheap, reads caches only, then catches up stale data. */
    fun onResume() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsOrDefault()
            reload(settings)

            if (settings.apiKey.isNotBlank()) {
                WorkScheduler.scheduleExchangeRateUpdate(context, settings.refreshIntervalDays)
                WorkScheduler.refreshExchangeRatesIfStale(context, settings.refreshIntervalDays)
            }
            if (settings.budgetTrackingEnabled) {
                WorkScheduler.scheduleBudgetReset(
                    context, settings.budgetRefreshStartDate, settings.budgetRefreshHour, settings.budgetDisplayTimeSpan
                )
            }
            refreshWeatherIfStale(settings)
        }
    }

    private suspend fun refreshWeatherIfStale(settings: AppSettings) {
        if (settings.weatherLocation1.isBlank() && settings.weatherLocation2.isBlank()) return
        val usesHere = settings.weatherLocation2 == WeatherStore.CURRENT_LOCATION
        val movedHere = usesHere && LocationHelper.updateCurrentPosition(context)
        val store = WeatherStore.getInstance(context)
        val oldest = listOf(WeatherStore.HOME, WeatherStore.AWAY).minOf { store.get(it)?.fetchedAt ?: 0L }
        if (movedHere || System.currentTimeMillis() - oldest > 60 * 60 * 1000L) {
            weatherRepository.refreshAll()
            reload(settings)
        }
    }

    fun refreshRates() {
        val key = _uiState.value.settings.apiKey
        if (key.isBlank() || _uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = null) }
            val result = currencyRepository.fetchAndUpdateExchangeRates(key)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    refreshError = result.exceptionOrNull()?.let { e ->
                        if (e is java.io.IOException) context.getString(R.string.error_no_connection) else e.message
                    }
                )
            }
            reload(_uiState.value.settings)
        }
    }

    private fun reload(settings: AppSettings) {
        val rates = RatesCache.usdRates(context)
        val hasRates = RatesCache.hasRates(context)
        val base = settings.inputCurrencies.firstOrNull() ?: "USD"
        val lines = settings.outputCurrencies.filter { it != base }.take(4).map {
            RateLine(it, if (hasRates) RatesCache.convert(1.0, base, it, rates) else null)
        }
        val budget = if (settings.budgetTrackingEnabled) BudgetSnapshot.load(context) else null
        val manager = SharedBudgetManager.getInstance(context)
        val nextReset = if (budget != null) BudgetRefreshCalculator.getNextBudgetPeriodStart(
            manager.getBudgetRefreshStartDate(), manager.getBudgetRefreshHour(), settings.budgetDisplayTimeSpan
        ) else 0L
        val store = WeatherStore.getInstance(context)

        val (allRates, sample) = RatesCache.ratesOrSample(context)
        _uiState.update {
            it.copy(
                rates = allRates,
                sampleRates = sample,
                from = if (it.loaded && it.from in (settings.inputCurrencies + settings.outputCurrencies)) it.from else base,
                loaded = true,
                settings = settings,
                ratesUpdatedAt = RatesCache.lastUpdated(context),
                baseCurrency = base,
                rateLines = lines,
                budget = budget,
                budgetNextReset = nextReset,
                home = store.get(WeatherStore.HOME),
                away = store.get(WeatherStore.AWAY),
                homeZone = TravelClockWidgetProvider.homeZone(context)?.second,
                hereZone = TravelClockWidgetProvider.herePlace(context).zone,
                hereLabel = TravelClockWidgetProvider.herePlace(context).label,
                version = it.version + 1
            )
        }
    }

    fun setAmount(text: String) {
        val cleaned = text.replace('.', ',').filter { it.isDigit() || it == ',' }
        val parts = cleaned.split(',')
        val normalized = if (parts.size > 2) parts[0] + "," + parts.drop(1).joinToString("") else cleaned
        _uiState.update { it.copy(amount = normalized.take(14)) }
    }

    fun setFrom(code: String) = _uiState.update { it.copy(from = code) }

    /** Swaps the typed currency with a target, converting the amount so the value is kept. */
    fun swapTo(code: String) {
        val st = _uiState.value
        val converted = st.convert(code) ?: return
        _uiState.update {
            // Internal amount format: plain digits, ',' as decimal mark (display localises it).
            val decimals = com.example.currencywidget.widget.Money.decimalsFor(code)
            val plain = java.math.BigDecimal(converted).setScale(decimals, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString().replace('.', ',')
            it.copy(from = code, amount = plain)
        }
    }

    fun addExpense(amount: Double, currency: String) {
        if (amount <= 0) return
        SharedBudgetManager.getInstance(context).addToBudget(amount, currency)
        reload(_uiState.value.settings)
    }

    fun periodLabel(span: BudgetTimeSpan): Int = when (span) {
        BudgetTimeSpan.DAILY -> R.string.period_today
        BudgetTimeSpan.WEEKLY -> R.string.period_week
        BudgetTimeSpan.MONTHLY -> R.string.period_month
        BudgetTimeSpan.YEARLY -> R.string.period_year
    }
}
