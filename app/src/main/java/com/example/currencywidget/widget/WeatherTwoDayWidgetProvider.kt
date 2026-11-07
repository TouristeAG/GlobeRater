package com.example.currencywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.currencywidget.MainActivity
import com.example.currencywidget.R
import com.example.currencywidget.data.repository.DayForecast
import com.example.currencywidget.data.repository.PlaceWeather
import com.example.currencywidget.data.repository.WeatherCodes
import com.example.currencywidget.data.repository.WeatherStore
import com.example.currencywidget.service.WorkScheduler

/** Weather for "home" and "here" (or a second place). Reads the cache only. */
class WeatherTwoDayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(context, interactive = true)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
        Sky.scheduleTicks(context)
    }

    override fun onEnabled(context: Context) {
        WorkScheduler.scheduleWeatherUpdate(context)
        WorkScheduler.refreshWeatherNow(context)
    }

    override fun onDisabled(context: Context) {
        Sky.cancelTicksIfUnused(context)
        // Keep refreshing only if the travel clock still needs the home time zone? It doesn't:
        // the time zone is stored once. So stop the periodic job when no weather widget is left.
        WorkScheduler.cancelWeatherUpdate(context)
    }

    companion object {
        private class Row(
            val root: Int, val place: Int, val condition: Int,
            val icon: Int, val temp: Int, val today: Int, val tomorrow: Int
        )

        private val rows = listOf(
            Row(R.id.weather_row_1, R.id.w1_place, R.id.w1_condition, R.id.w1_now_icon, R.id.w1_now_temp, R.id.w1_today, R.id.w1_tomorrow),
            Row(R.id.weather_row_2, R.id.w2_place, R.id.w2_condition, R.id.w2_now_icon, R.id.w2_now_temp, R.id.w2_today, R.id.w2_tomorrow)
        )

        fun buildViews(context: Context, interactive: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_two_day)
            val paint = WidgetPaint(context, WidgetPaletteStore.get(context, WeatherTwoDayWidgetProvider::class.java, preview = !interactive))
            paint.bg(views, R.id.widget_root, paint.d.cBg)
            paint.text(views, R.id.weather_empty, paint.r.accent)
            val store = WeatherStore.getInstance(context)
            val places = listOf(WeatherStore.HOME to context.getString(R.string.place_home), WeatherStore.AWAY to context.getString(R.string.place_here))
                .mapNotNull { (slot, badge) -> store.get(slot)?.let { badge to it } }
                .ifEmpty { if (interactive) emptyList() else samplePlaces(context) }

            views.setViewVisibility(R.id.weather_empty, if (places.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.weather_divider, if (places.size == 2) View.VISIBLE else View.GONE)
            rows.forEachIndexed { i, row ->
                val entry = places.getOrNull(i)
                views.setViewVisibility(row.root, if (entry == null) View.GONE else View.VISIBLE)
                if (entry != null) bindRow(context, views, row, entry.first, entry.second, paint)
            }

            if (interactive) {
                views.setOnClickPendingIntent(
                    R.id.widget_root, WidgetPaletteStore.openPanelIntent(context, WeatherTwoDayWidgetProvider::class.java)
                )
            }
            return views
        }

        /** Shown in the in-app preview before any place is configured. */
        private fun samplePlaces(context: Context): List<Pair<String, PlaceWeather>> {
            val now = System.currentTimeMillis()
            return listOf(
                context.getString(R.string.place_home) to PlaceWeather("Geneva", "Europe/Zurich", 9.0, 2, true,
                    listOf(DayForecast(2, 4.0, 11.0, 10), DayForecast(61, 5.0, 9.0, 70)), now, false),
                context.getString(R.string.place_here) to PlaceWeather("Bangkok", "Asia/Bangkok", 31.0, 1, true,
                    listOf(DayForecast(1, 25.0, 33.0, 20), DayForecast(95, 26.0, 32.0, 80)), now, false)
            )
        }

        private fun bindRow(context: Context, views: RemoteViews, row: Row, badge: String, w: PlaceWeather, paint: WidgetPaint) {
            val zone = w.timezone?.let { java.util.TimeZone.getTimeZone(it) } ?: java.util.TimeZone.getDefault()
            val (bg, ink) = Sky.tile(paint, zone)
            paint.bg(views, row.root, bg)
            paint.texts(views, ink, row.place, row.condition, row.temp, row.today, row.tomorrow)
            val today = w.days.getOrNull(0)
            val tomorrow = w.days.getOrNull(1)
            views.setTextViewText(row.place, w.label)
            val rain = today?.popPct?.takeIf { it >= 20 }?.let { context.getString(R.string.widget_rain, it) } ?: ""
            views.setTextViewText(row.condition, "$badge · " + context.getString(WeatherCodes.describeRes(w.currentCode ?: today?.code)) + rain)
            views.setImageViewResource(row.icon, WeatherCodes.icon(w.currentCode ?: today?.code, w.isDay))
            paint.tint(views, row.icon, ink)
            views.setTextViewText(row.temp, WeatherCodes.temp(w.currentTemp))
            views.setTextViewText(
                row.today,
                today?.let { "↑${WeatherCodes.temp(it.max)} ↓${WeatherCodes.temp(it.min)}" } ?: ""
            )
            views.setTextViewText(
                row.tomorrow,
                tomorrow?.let { context.getString(R.string.widget_tomorrow, WeatherCodes.temp(it.max)) } ?: ""
            )
        }
    }
}
