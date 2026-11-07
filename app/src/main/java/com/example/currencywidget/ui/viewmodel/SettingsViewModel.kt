package com.example.currencywidget.ui.viewmodel

import android.content.Context
import com.example.currencywidget.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencywidget.data.api.GeoCodingResult
import com.example.currencywidget.data.model.AppSettings
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.example.currencywidget.data.model.Currency
import com.example.currencywidget.data.model.WeatherUnits
import com.example.currencywidget.data.repository.CurrencyRepository
import com.example.currencywidget.data.repository.SettingsRepository
import com.example.currencywidget.data.repository.SharedBudgetManager
import com.example.currencywidget.data.repository.WeatherRepository
import com.example.currencywidget.data.repository.WeatherStore
import com.example.currencywidget.service.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false,
    val currencies: List<Currency> = emptyList(),
    val currenciesError: String? = null,
    val isRefreshing: Boolean = false,
    val lastRefreshedAt: Long = 0L,
    val refreshMessage: String? = null,
    val refreshFailed: Boolean = false,
    val budgetSpent: Double = 0.0,
    /** Which weather field (0 = home, 1 = away) the suggestions belong to. */
    val suggestionsFor: Int = -1,
    val suggestions: List<GeoCodingResult> = emptyList(),
    val searching: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val budget get() = SharedBudgetManager.getInstance(context)
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.getSettingsOrDefault()
            combine(settingsRepository.getSettings(), currencyRepository.getAllCurrencies()) { s, c -> s to c }
                .collect { (settings, currencies) ->
                    _uiState.update {
                        it.copy(
                            settings = settings ?: AppSettings(),
                            loaded = true,
                            currencies = currencies.sortedBy { c -> c.code },
                            lastRefreshedAt = currencyRepository.getCachedLastUpdatedTimestamp()
                        )
                    }
                }
        }
        viewModelScope.launch {
            // The currency list is public (no key needed): load it once, silently.
            if (currencyRepository.getAllCurrencies().first().isEmpty()) loadCurrencies()
        }
        refreshBudgetSpent()
    }

    private fun edit(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = transform(settingsRepository.getSettingsOrDefault())
            _uiState.update { it.copy(settings = updated) }
            settingsRepository.updateSettings(updated)
        }
    }

    // ---- Exchange rates ---------------------------------------------------------------

    fun loadCurrencies() {
        viewModelScope.launch {
            val result = currencyRepository.fetchAndUpdateCurrencies()
            _uiState.update {
                it.copy(currenciesError = result.exceptionOrNull()?.let { context.getString(R.string.currencies_load_error) })
            }
        }
    }

    fun saveApiKey(raw: String) {
        val key = raw.trim()
        edit { it.copy(apiKey = key) }
        if (key.isEmpty()) {
            WorkScheduler.cancelExchangeRateUpdate(context)
            _uiState.update { it.copy(refreshMessage = null, refreshFailed = false) }
        } else {
            WorkScheduler.scheduleExchangeRateUpdate(context, _uiState.value.settings.refreshIntervalDays)
            refreshNow(key)
        }
    }

    fun refreshNow(keyOverride: String? = null) {
        val key = keyOverride ?: _uiState.value.settings.apiKey
        if (key.isBlank() || _uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshMessage = null) }
            val result = currencyRepository.fetchAndUpdateExchangeRates(key)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    lastRefreshedAt = currencyRepository.getCachedLastUpdatedTimestamp(),
                    refreshFailed = result.isFailure,
                    refreshMessage = result.exceptionOrNull()?.let { e ->
                        if (e is java.io.IOException) context.getString(R.string.error_no_connection_retry) else e.message
                    } ?: context.getString(R.string.rates_updated)
                )
            }
        }
    }

    fun setRefreshInterval(days: Int) {
        edit { it.copy(refreshIntervalDays = days) }
        if (_uiState.value.settings.apiKey.isNotBlank()) WorkScheduler.scheduleExchangeRateUpdate(context, days)
    }

    fun setInputCurrencies(codes: List<String>) = edit { it.copy(inputCurrencies = codes) }
    fun setOutputCurrencies(codes: List<String>) = edit { it.copy(outputCurrencies = codes) }

    // ---- Budget -------------------------------------------------------------------------

    fun setBudgetEnabled(enabled: Boolean) {
        edit { it.copy(budgetTrackingEnabled = enabled) }
        if (enabled) scheduleBudgetReset() else WorkScheduler.cancelBudgetReset(context)
    }

    fun setBudgetAmount(amount: Double) {
        edit { it.copy(budgetAmount = amount) }
        budget.setBudgetAmount(amount)
    }

    fun setBudgetCurrency(code: String) {
        edit { it.copy(budgetCurrency = code) }
        budget.setBudgetCurrency(code)
    }

    fun setBudgetPeriod(span: BudgetTimeSpan) {
        edit { it.copy(budgetTimeSpan = span) }
        budget.setBudgetTimeSpan(span.name)
    }

    fun setBudgetDisplayPeriod(span: BudgetTimeSpan) {
        edit { it.copy(budgetDisplayTimeSpan = span) }
        budget.setBudgetDisplayTimeSpan(span)
        scheduleBudgetReset(displaySpan = span)
    }

    fun setBudgetResetStart(startDate: Long, hour: Int) {
        edit { it.copy(budgetRefreshStartDate = startDate, budgetRefreshHour = hour) }
        budget.setBudgetRefreshStartDate(startDate)
        budget.setBudgetRefreshHour(hour)
        scheduleBudgetReset(startDate = startDate, hour = hour)
    }

    private fun scheduleBudgetReset(
        startDate: Long = _uiState.value.settings.budgetRefreshStartDate,
        hour: Int = _uiState.value.settings.budgetRefreshHour,
        displaySpan: BudgetTimeSpan = _uiState.value.settings.budgetDisplayTimeSpan
    ) {
        WorkScheduler.scheduleBudgetReset(context, startDate, hour, displaySpan)
    }

    fun setBudgetSpent(amount: Double) {
        budget.resetBudget()
        if (amount > 0) budget.addToBudget(amount, budget.getBudgetCurrency())
        refreshBudgetSpent()
    }

    fun refreshBudgetSpent() {
        _uiState.update { it.copy(budgetSpent = budget.getTotalSpending()) }
    }

    // ---- Weather / travel -----------------------------------------------------------------

    fun searchPlaces(slot: Int, query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(suggestions = emptyList(), suggestionsFor = slot, searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce typing
            _uiState.update { it.copy(searching = true, suggestionsFor = slot) }
            val results = weatherRepository.searchLocations(query).getOrDefault(emptyList())
            _uiState.update { it.copy(searching = false, suggestions = results, suggestionsFor = slot) }
        }
    }

    fun pickPlace(slot: Int, place: GeoCodingResult) {
        weatherRepository.rememberPlace(slot, place)
        setPlace(slot, place.label)
    }

    /** Free text (or "lat, lon") — resolved on the next refresh. Blank clears the place. */
    fun setPlace(slot: Int, value: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(suggestions = emptyList(), suggestionsFor = -1, searching = false) }
        viewModelScope.launch {
            val current = settingsRepository.getSettingsOrDefault()
            val updated = if (slot == WeatherStore.HOME) current.copy(weatherLocation1 = value.trim())
            else current.copy(weatherLocation2 = value.trim())
            _uiState.update { it.copy(settings = updated) }
            settingsRepository.updateSettings(updated)
            onPlacesChanged(updated)
        }
    }

    fun setWeatherUnits(units: WeatherUnits) {
        viewModelScope.launch {
            val updated = settingsRepository.getSettingsOrDefault().copy(weatherUnits = units)
            _uiState.update { it.copy(settings = updated) }
            settingsRepository.updateSettings(updated)
            onPlacesChanged(updated)
        }
    }

    private suspend fun onPlacesChanged(settings: AppSettings) {
        if (settings.weatherLocation1.isBlank() && settings.weatherLocation2.isBlank()) {
            WorkScheduler.cancelWeatherUpdate(context)
        } else {
            WorkScheduler.scheduleWeatherUpdate(context)
        }
        weatherRepository.refreshAll()
    }

    // ---- Data -----------------------------------------------------------------------------

    fun resetAllData() {
        viewModelScope.launch {
            WorkScheduler.cancelAll(context)
            settingsRepository.resetAllData()
            refreshBudgetSpent()
            _uiState.update { it.copy(lastRefreshedAt = 0L, refreshMessage = null) }
        }
    }
}
