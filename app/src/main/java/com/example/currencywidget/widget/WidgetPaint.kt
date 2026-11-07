package com.example.currencywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.currencywidget.MainActivity

/** Which palette each kind of widget uses (one choice per widget type). */
object WidgetPaletteStore {
    private const val PREFS = "WidgetPalettes"

    /** Set while rendering an in-app preview of a not-yet-saved choice. */
    @Volatile var previewOverride: String? = null

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun idFor(context: Context, provider: Class<*>): String =
        prefs(context).getString(provider.name, WidgetPalettes.DEFAULT) ?: WidgetPalettes.DEFAULT

    fun get(context: Context, provider: Class<*>, preview: Boolean = false): WidgetPalette {
        val override = if (preview) previewOverride else null
        val palette = WidgetPalettes.byId(override ?: idFor(context, provider))
        // Material You colours only exist from Android 12.
        return if (palette.id == WidgetPalettes.SYSTEM && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            WidgetPalettes.byId(WidgetPalettes.FALLBACK)
        } else palette
    }

    fun set(context: Context, provider: Class<*>, paletteId: String) {
        prefs(context).edit().putString(provider.name, paletteId).apply()
        WidgetRefresher.update(context, provider.asSubclass(AppWidgetProvider::class.java))
    }

    fun setAll(context: Context, paletteId: String) {
        val e = prefs(context).edit()
        WidgetRefresher.providers.forEach { e.putString(it.name, paletteId) }
        e.apply()
        WidgetRefresher.updateAll(context)
    }

    /** Tapping the empty parts of a widget opens its colour panel in the app. */
    fun openPanelIntent(context: Context, provider: Class<*>): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_WIDGET_COLORS)
            .putExtra(MainActivity.EXTRA_PROVIDER, provider.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, provider.name.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Applies a palette to RemoteViews. On Android 12+ every colour is sent as a light/dark
 * pair, so the launcher switches it with dark mode without the app being woken.
 */
class WidgetPaint(private val context: Context, val palette: WidgetPalette) {

    private fun ctx(night: Boolean): Context {
        val config = Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        }
        return context.createConfigurationContext(config)
    }

    private val light by lazy { ctx(false) }
    private val dark by lazy { ctx(true) }
    private val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Colour for the current mode (used inside text spans). */
    fun now(@ColorRes res: Int): Int = ContextCompat.getColor(context, res)

    val r get() = palette.colors
    val d get() = palette.drawables

    fun text(views: RemoteViews, viewId: Int, @ColorRes res: Int) {
        if (modern) views.setColorInt(viewId, "setTextColor", ContextCompat.getColor(light, res), ContextCompat.getColor(dark, res))
        else views.setTextColor(viewId, now(res))
    }

    fun texts(views: RemoteViews, @ColorRes res: Int, vararg ids: Int) = ids.forEach { text(views, it, res) }

    fun bg(views: RemoteViews, viewId: Int, @DrawableRes res: Int) = views.setInt(viewId, "setBackgroundResource", res)

    fun tint(views: RemoteViews, viewId: Int, @ColorRes res: Int) {
        if (modern) views.setColorInt(viewId, "setColorFilter", ContextCompat.getColor(light, res), ContextCompat.getColor(dark, res))
        else views.setInt(viewId, "setColorFilter", now(res))
    }

    /** Progress bars can only be re-tinted remotely on Android 12+. */
    fun progress(views: RemoteViews, viewId: Int, @ColorRes fill: Int, @ColorRes track: Int) {
        if (!modern) return
        fun csl(c: Context, res: Int) = ColorStateList.valueOf(ContextCompat.getColor(c, res))
        views.setColorStateList(viewId, "setProgressTintList", csl(light, fill), csl(dark, fill))
        views.setColorStateList(viewId, "setProgressBackgroundTintList", csl(light, track), csl(dark, track))
    }
}
