package com.example.currencywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.currencywidget.MainActivity
import com.example.currencywidget.R
import com.example.currencywidget.data.repository.WeatherStore
import java.util.TimeZone

/**
 * Home time vs local time. The clocks are TextClocks, which tick by themselves inside the
 * launcher: the app is only woken when the phone's time zone changes.
 */
class TravelClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = buildViews(context, interactive = true)
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
        Sky.scheduleTicks(context) // also re-armed after a reboot, when the launcher re-sends updates
    }

    override fun onDisabled(context: Context) = Sky.cancelTicksIfUnused(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Sky.ACTION_TICK ->
                WidgetRefresher.updateWeatherWidgets(context)
        }
    }

    companion object {
        /** Home time zone: from the "home" place picked in the app (Open-Meteo returns it). */
        fun homeZone(context: Context): Pair<String, TimeZone>? {
            val store = WeatherStore.getInstance(context)
            val resolved = store.resolved(WeatherStore.HOME)
            val tzId = resolved?.timezone ?: store.get(WeatherStore.HOME)?.timezone ?: return null
            val label = store.get(WeatherStore.HOME)?.label ?: resolved?.label ?: cityOf(tzId)
            return label to TimeZone.getTimeZone(tzId)
        }

        /** The second place: its label, its zone, and whether it is the phone's own position. */
        data class HerePlace(val label: String, val zone: TimeZone, val isPhone: Boolean)

        /**
         * "Here" = the second place. A fixed place uses its own time zone (Open-Meteo gives it
         * when the place is picked or first refreshed); "where my phone is" uses the phone's.
         */
        fun herePlace(context: Context): HerePlace {
            val query = com.example.currencywidget.data.repository.WidgetSettingsCache.getInstance(context).getWeatherLocation2()
            if (query.isNotBlank() && query != WeatherStore.CURRENT_LOCATION) {
                val store = WeatherStore.getInstance(context)
                val resolved = store.resolved(WeatherStore.AWAY)?.takeIf { it.query == query }
                val weather = store.get(WeatherStore.AWAY)
                val tzId = resolved?.timezone ?: weather?.timezone
                if (tzId != null) {
                    return HerePlace(resolved?.label ?: weather?.label ?: cityOf(tzId), TimeZone.getTimeZone(tzId), isPhone = false)
                }
            }
            val device = TimeZone.getDefault()
            return HerePlace(cityOf(device.id), device, isPhone = true)
        }

        fun cityOf(tzId: String): String = tzId.substringAfterLast('/').replace('_', ' ')

        /** Minutes "here" is ahead of home (negative = behind). */
        fun offsetMinutes(home: TimeZone, here: TimeZone, now: Long = System.currentTimeMillis()): Int =
            (here.getOffset(now) - home.getOffset(now)) / 60_000

        fun buildViews(context: Context, interactive: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_travel_clock)
            val herePlace = herePlace(context)
            val here = herePlace.zone
            val hereLabel = if (herePlace.isPhone) context.getString(R.string.place_role_label, context.getString(R.string.place_here), herePlace.label)
            else herePlace.label
            val home = homeZone(context)
                ?: if (!interactive) "Geneva" to TimeZone.getTimeZone("Europe/Zurich") else null

            val palette = WidgetPaint(context, WidgetPaletteStore.get(context, TravelClockWidgetProvider::class.java, preview = !interactive))
            palette.bg(views, R.id.widget_root, palette.d.cBg)
            fun paint(tile: Int, zone: TimeZone, vararg texts: Int) {
                val (bg, ink) = Sky.tile(palette, zone)
                palette.bg(views, tile, bg)
                palette.texts(views, ink, *texts)
            }
            paint(R.id.clock_here_tile, here, R.id.clock_here_label, R.id.clock_here_time, R.id.clock_here_date)
            paint(R.id.clock_home_tile, home?.second ?: here, R.id.clock_home_label, R.id.clock_home_time, R.id.clock_home_date)

            views.setTextViewText(R.id.clock_here_label, hereLabel)
            views.setString(R.id.clock_here_time, "setTimeZone", here.id)
            views.setString(R.id.clock_here_date, "setTimeZone", here.id)

            if (home != null) {
                views.setTextViewText(R.id.clock_home_label, context.getString(R.string.place_role_label, context.getString(R.string.place_home), home.first))
                views.setString(R.id.clock_home_time, "setTimeZone", home.second.id)
                views.setString(R.id.clock_home_date, "setTimeZone", home.second.id)
            } else {
                views.setTextViewText(R.id.clock_home_label, context.getString(R.string.place_home))
                views.setString(R.id.clock_home_time, "setTimeZone", here.id)
                views.setString(R.id.clock_home_date, "setTimeZone", here.id)
            }

            if (interactive) {
                views.setOnClickPendingIntent(
                    R.id.widget_root, WidgetPaletteStore.openPanelIntent(context, TravelClockWidgetProvider::class.java)
                )
            }
            return views
        }
    }
}
