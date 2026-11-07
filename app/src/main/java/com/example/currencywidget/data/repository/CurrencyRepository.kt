package com.example.currencywidget.data.repository

import android.content.Context
import com.example.currencywidget.R
import com.example.currencywidget.data.api.ExchangeRateApi
import com.example.currencywidget.data.database.CurrencyDao
import com.example.currencywidget.data.model.Currency
import com.example.currencywidget.widget.RatesCache
import com.example.currencywidget.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class RatesException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class CurrencyRepository @Inject constructor(
    private val currencyDao: CurrencyDao,
    private val exchangeRateApi: ExchangeRateApi,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(RatesCache.PREFS, Context.MODE_PRIVATE)

    fun getAllCurrencies(): Flow<List<Currency>> = currencyDao.getAllCurrencies()

    /**
     * Downloads the latest USD-based rates, stores them for the widgets and re-renders
     * every placed widget. Network errors are returned as [IOException] so callers can retry.
     */
    suspend fun fetchAndUpdateExchangeRates(apiKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = exchangeRateApi.getLatestRates(apiKey.trim())
            if (!response.isSuccessful) {
                val message = when (response.code()) {
                    401 -> context.getString(R.string.err_invalid_key)
                    403 -> context.getString(R.string.err_key_not_allowed)
                    429 -> context.getString(R.string.err_quota)
                    else -> context.getString(R.string.err_service, response.code())
                }
                return@withContext Result.failure(RatesException(message))
            }
            val body = response.body()
            if (body == null || body.rates.isEmpty()) {
                return@withContext Result.failure(RatesException(context.getString(R.string.err_empty)))
            }
            val editor = prefs.edit()
            body.rates.forEach { (code, rate) -> editor.putFloat("rate_$code", rate.toFloat()) }
            editor.putLong(RatesCache.KEY_LAST_UPDATED, System.currentTimeMillis())
            editor.putLong("rates_timestamp", body.timestamp * 1000)
            editor.apply()
            WidgetRefresher.updateAll(context)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(RatesException(e.message ?: context.getString(R.string.err_unexpected), e))
        }
    }

    /** Currency names list. Public endpoint, no key needed; fetched once and kept in Room. */
    suspend fun fetchAndUpdateCurrencies(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = exchangeRateApi.getCurrencies()
            val map = response.body()
            if (!response.isSuccessful || map.isNullOrEmpty()) {
                return@withContext Result.failure(RatesException(context.getString(R.string.currencies_load_error)))
            }
            currencyDao.insertCurrencies(map.map { (code, name) -> Currency(code, name, symbolFor(code)) })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedLastUpdatedTimestamp(): Long = RatesCache.lastUpdated(context)

    private fun symbolFor(code: String): String = when (code) {
        "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY" -> "¥"; "CNY" -> "¥"
        "CAD" -> "C$"; "AUD" -> "A$"; "NZD" -> "NZ$"; "SGD" -> "S$"; "HKD" -> "HK$"
        "SEK", "NOK", "DKK" -> "kr"; "TRY" -> "₺"; "INR" -> "₹"; "BRL" -> "R$"
        "ZAR" -> "R"; "KRW" -> "₩"; "THB" -> "฿"; "VND" -> "₫"; "PHP" -> "₱"
        else -> code
    }
}
