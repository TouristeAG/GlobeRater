package com.example.currencywidget.data.repository

import android.content.Context
import com.example.currencywidget.data.api.GeoCodingResult
import com.example.currencywidget.data.api.OpenMeteoForecastApi
import com.example.currencywidget.data.api.OpenMeteoGeocodingApi
import com.example.currencywidget.data.model.WeatherUnits
import com.example.currencywidget.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val forecastApi: OpenMeteoForecastApi
) {
    private val store get() = WeatherStore.getInstance(context)

    suspend fun searchLocations(query: String, limit: Int = 5): Result<List<GeoCodingResult>> = runCatching {
        val response = geocodingApi.search(name = query.trim(), count = limit)
        if (!response.isSuccessful) error("Search failed (${response.code()})")
        response.body()?.results.orEmpty().mapNotNull {
            if (it.latitude == null || it.longitude == null) null
            else GeoCodingResult(it.name, it.latitude, it.longitude, it.country, it.admin1, it.timezone)
        }
    }

    /** Remembers the exact place picked from the suggestions, so no geocoding is needed later. */
    fun rememberPlace(slot: Int, place: GeoCodingResult) {
        val lat = place.lat ?: return
        val lon = place.lon ?: return
        store.saveResolved(slot, ResolvedPlace(place.label, place.name ?: place.label, lat, lon, place.timezone))
    }

    /**
     * Refreshes both slots from the network and re-renders the weather & clock widgets.
     * Returns true if at least one place was updated.
     */
    suspend fun refreshAll(): Boolean {
        val cache = WidgetSettingsCache.getInstance(context)
        val units = cache.getWeatherUnits()
        var updated = false
        listOf(WeatherStore.HOME to cache.getWeatherLocation1(), WeatherStore.AWAY to cache.getWeatherLocation2())
            .forEach { (slot, query) ->
                if (query.isBlank()) {
                    store.clear(slot)
                } else if (refreshSlot(slot, query.trim(), units)) {
                    updated = true
                }
            }
        WidgetRefresher.updateWeatherWidgets(context)
        return updated
    }

    private suspend fun refreshSlot(slot: Int, query: String, units: WeatherUnits): Boolean {
        val place = resolve(slot, query) ?: return false
        val fahrenheit = units == WeatherUnits.IMPERIAL
        val result = runCatching {
            val response = forecastApi.forecast(
                latitude = place.lat,
                longitude = place.lon,
                temperatureUnit = if (fahrenheit) "fahrenheit" else "celsius"
            )
            if (!response.isSuccessful) error("Forecast failed (${response.code()})")
            response.body() ?: error("Empty forecast")
        }.getOrNull() ?: return false

        val daily = result.daily
        val days = (0 until (daily?.time?.size ?: 0)).map { i ->
            DayForecast(
                code = daily?.weather_code?.getOrNull(i),
                min = daily?.temperature_2m_min?.getOrNull(i),
                max = daily?.temperature_2m_max?.getOrNull(i),
                popPct = daily?.precipitation_probability_max?.getOrNull(i)?.toInt()?.coerceIn(0, 100)
            )
        }
        val timezone = result.timezone ?: place.timezone
        if (timezone != place.timezone) store.saveResolved(slot, place.copy(timezone = timezone))
        store.save(
            slot,
            PlaceWeather(
                label = place.label,
                timezone = timezone,
                currentTemp = result.current?.temperature_2m,
                currentCode = result.current?.weather_code,
                isDay = result.current?.is_day != 0,
                days = days,
                fetchedAt = System.currentTimeMillis(),
                fahrenheit = fahrenheit
            )
        )
        return true
    }

    private suspend fun resolve(slot: Int, query: String): ResolvedPlace? {
        if (query == WeatherStore.CURRENT_LOCATION) {
            val (lat, lon) = store.currentPosition() ?: return null
            return ResolvedPlace(query, "Here", lat, lon, null)
        }
        store.resolved(slot)?.takeIf { it.query == query }?.let { return it }

        // "47.37, 8.54" style coordinates.
        val parts = query.split(",").map { it.trim() }
        if (parts.size == 2) {
            val lat = parts[0].toDoubleOrNull()
            val lon = parts[1].toDoubleOrNull()
            if (lat != null && lon != null) {
                return ResolvedPlace(query, query, lat, lon, null).also { store.saveResolved(slot, it) }
            }
        }
        // Free text: try the whole query, then just the city part ("Geneva, Switzerland" → "Geneva").
        val candidates = listOf(query, parts.firstOrNull().orEmpty()).filter { it.isNotBlank() }.distinct()
        for (candidate in candidates) {
            val hit = searchLocations(candidate, 1).getOrNull()?.firstOrNull() ?: continue
            val place = ResolvedPlace(query, hit.name ?: query, hit.lat!!, hit.lon!!, hit.timezone)
            store.saveResolved(slot, place)
            return place
        }
        return null
    }
}
