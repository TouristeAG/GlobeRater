package com.example.currencywidget.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.currencywidget.R

/** One forecast day. */
data class DayForecast(
    val code: Int?,
    val min: Double?,
    val max: Double?,
    val popPct: Int?
)

/** Everything a widget or the home screen needs to show one place. */
data class PlaceWeather(
    val label: String,
    val timezone: String?,
    val currentTemp: Double?,
    val currentCode: Int?,
    val isDay: Boolean,
    val days: List<DayForecast>,
    val fetchedAt: Long,
    val fahrenheit: Boolean
)

data class ResolvedPlace(val query: String, val label: String, val lat: Double, val lon: Double, val timezone: String?)

/**
 * Small SharedPreferences store so widgets can render instantly without network access.
 * Slot 0 = "home", slot 1 = "away" (a fixed place or the phone's location).
 */
class WeatherStore private constructor(context: Context) {

    companion object {
        const val HOME = 0
        const val AWAY = 1
        /** Special value of weatherLocation2: use the phone's last known position. */
        const val CURRENT_LOCATION = "@current"

        private const val PREFS = "WeatherStore"

        @Volatile private var instance: WeatherStore? = null
        fun getInstance(context: Context): WeatherStore =
            instance ?: synchronized(this) {
                instance ?: WeatherStore(context.applicationContext).also { instance = it }
            }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Resolved coordinates (avoid geocoding on every refresh) ------------------------

    fun resolved(slot: Int): ResolvedPlace? {
        val query = prefs.getString("p${slot}_query", null) ?: return null
        val lat = prefs.getString("p${slot}_lat", null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString("p${slot}_lon", null)?.toDoubleOrNull() ?: return null
        return ResolvedPlace(
            query = query,
            label = prefs.getString("p${slot}_label", query) ?: query,
            lat = lat,
            lon = lon,
            timezone = prefs.getString("p${slot}_tz", null)
        )
    }

    fun saveResolved(slot: Int, place: ResolvedPlace) {
        prefs.edit()
            .putString("p${slot}_query", place.query)
            .putString("p${slot}_label", place.label)
            .putString("p${slot}_lat", place.lat.toString())
            .putString("p${slot}_lon", place.lon.toString())
            .putString("p${slot}_tz", place.timezone)
            .apply()
    }

    // ---- Forecasts ----------------------------------------------------------------------

    fun save(slot: Int, w: PlaceWeather) {
        val e = prefs.edit()
            .putString("w${slot}_label", w.label)
            .putString("w${slot}_tz", w.timezone)
            .putString("w${slot}_temp", w.currentTemp?.toString())
            .putInt("w${slot}_code", w.currentCode ?: -1)
            .putBoolean("w${slot}_day", w.isDay)
            .putLong("w${slot}_at", w.fetchedAt)
            .putBoolean("w${slot}_f", w.fahrenheit)
            .putInt("w${slot}_n", w.days.size)
        w.days.forEachIndexed { i, d ->
            e.putInt("w${slot}_d${i}_code", d.code ?: -1)
                .putString("w${slot}_d${i}_min", d.min?.toString())
                .putString("w${slot}_d${i}_max", d.max?.toString())
                .putInt("w${slot}_d${i}_pop", d.popPct ?: -1)
        }
        e.apply()
    }

    fun get(slot: Int): PlaceWeather? {
        val label = prefs.getString("w${slot}_label", null) ?: return null
        val n = prefs.getInt("w${slot}_n", 0)
        fun int(key: String) = prefs.getInt(key, -1).takeIf { it >= 0 }
        fun dbl(key: String) = prefs.getString(key, null)?.toDoubleOrNull()
        return PlaceWeather(
            label = label,
            timezone = prefs.getString("w${slot}_tz", null),
            currentTemp = dbl("w${slot}_temp"),
            currentCode = int("w${slot}_code"),
            isDay = prefs.getBoolean("w${slot}_day", true),
            days = (0 until n).map { i ->
                DayForecast(int("w${slot}_d${i}_code"), dbl("w${slot}_d${i}_min"), dbl("w${slot}_d${i}_max"), int("w${slot}_d${i}_pop"))
            },
            fetchedAt = prefs.getLong("w${slot}_at", 0L),
            fahrenheit = prefs.getBoolean("w${slot}_f", false)
        )
    }

    fun clear(slot: Int) {
        val e = prefs.edit()
        prefs.all.keys.filter { it.startsWith("w${slot}_") || it.startsWith("p${slot}_") }.forEach { e.remove(it) }
        e.apply()
    }

    // ---- Phone position (only refreshed while the app is in the foreground) --------------

    fun saveCurrentPosition(lat: Double, lon: Double) {
        prefs.edit()
            .putString("cur_lat", lat.toString())
            .putString("cur_lon", lon.toString())
            .putLong("cur_at", System.currentTimeMillis())
            .apply()
    }

    fun currentPosition(): Pair<Double, Double>? {
        val lat = prefs.getString("cur_lat", null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString("cur_lon", null)?.toDoubleOrNull() ?: return null
        return lat to lon
    }
}

/** WMO weather codes → emoji + short text. Emoji render in widgets with zero assets. */
object WeatherCodes {
    fun emoji(code: Int?, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "·"
    }

    /** Thin line icon (drawable) for a WMO code. */
    fun icon(code: Int?, isDay: Boolean = true): Int = when (code) {
        0 -> if (isDay) R.drawable.wx_sun else R.drawable.wx_moon
        1, 2 -> if (isDay) R.drawable.wx_partly else R.drawable.wx_moon
        3 -> R.drawable.wx_cloud
        45, 48 -> R.drawable.wx_fog
        51, 53, 55, 56, 57 -> R.drawable.wx_drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.wx_rain
        71, 73, 75, 77, 85, 86 -> R.drawable.wx_snow
        95, 96, 99 -> R.drawable.wx_storm
        else -> R.drawable.wx_cloud
    }

    /** Short description as a string resource (translated). */
    fun describeRes(code: Int?): Int = when (code) {
        0 -> R.string.wx_clear
        1 -> R.string.wx_mostly_clear
        2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_overcast
        45, 48 -> R.string.wx_fog
        51, 53, 55 -> R.string.wx_drizzle
        56, 57 -> R.string.wx_freezing_drizzle
        61, 63, 65 -> R.string.wx_rain
        66, 67 -> R.string.wx_freezing_rain
        71, 73, 75, 77 -> R.string.wx_snow
        80, 81, 82 -> R.string.wx_showers
        85, 86 -> R.string.wx_snow_showers
        95, 96, 99 -> R.string.wx_thunderstorm
        else -> R.string.wx_unknown
    }

    fun temp(value: Double?): String = value?.let { "${Math.round(it)}°" } ?: "--°"
}
