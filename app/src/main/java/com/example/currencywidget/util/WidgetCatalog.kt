package com.example.currencywidget.util

import androidx.annotation.StringRes
import com.example.currencywidget.R
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.example.currencywidget.widget.BudgetTrackerWidgetBase
import com.example.currencywidget.widget.BudgetTrackerWidgetProvider
import com.example.currencywidget.widget.ConverterWidgetProvider
import com.example.currencywidget.widget.CurrencyConverterCompactBudgetWidgetProvider
import com.example.currencywidget.widget.CurrencyConverterCompactWidgetProvider
import com.example.currencywidget.widget.CurrencyConverterExpandedBudgetWidgetProvider
import com.example.currencywidget.widget.CurrencyConverterWidgetProvider
import com.example.currencywidget.widget.HorizontalBudgetTrackerWidgetProvider
import com.example.currencywidget.widget.MinimalBudgetTrackerWidgetProvider
import com.example.currencywidget.widget.TravelClockWidgetProvider
import com.example.currencywidget.widget.WeatherTwoDayWidgetProvider

enum class WidgetGroup(@StringRes val titleRes: Int) {
    CONVERTER(R.string.group_converter), BUDGET(R.string.group_budget), TRAVEL(R.string.group_travel)
}

/** One widget the user can place, with its natural size (dp) for the in-app preview. */
data class WidgetSpec(
    val provider: Class<out AppWidgetProvider>,
    val group: WidgetGroup,
    @StringRes val titleRes: Int,
    @StringRes val variantRes: Int,
    @StringRes val descriptionRes: Int,
    val widthDp: Int,
    val heightDp: Int
)

object WidgetCatalog {

    val all = listOf(
        WidgetSpec(CurrencyConverterCompactWidgetProvider::class.java, WidgetGroup.CONVERTER,
            R.string.group_converter, R.string.w_compact, R.string.w_compact_desc, 230, 320),
        WidgetSpec(CurrencyConverterWidgetProvider::class.java, WidgetGroup.CONVERTER,
            R.string.group_converter, R.string.w_tall, R.string.w_tall_desc, 230, 400),
        WidgetSpec(CurrencyConverterCompactBudgetWidgetProvider::class.java, WidgetGroup.CONVERTER,
            R.string.group_converter, R.string.w_compact_budget, R.string.w_compact_budget_desc, 230, 330),
        WidgetSpec(CurrencyConverterExpandedBudgetWidgetProvider::class.java, WidgetGroup.CONVERTER,
            R.string.group_converter, R.string.w_tall_budget, R.string.w_tall_budget_desc, 230, 420),
        WidgetSpec(BudgetTrackerWidgetProvider::class.java, WidgetGroup.BUDGET,
            R.string.group_budget, R.string.w_vertical, R.string.w_vertical_desc, 90, 190),
        WidgetSpec(HorizontalBudgetTrackerWidgetProvider::class.java, WidgetGroup.BUDGET,
            R.string.group_budget, R.string.w_horizontal, R.string.w_horizontal_desc, 200, 90),
        WidgetSpec(MinimalBudgetTrackerWidgetProvider::class.java, WidgetGroup.BUDGET,
            R.string.group_budget, R.string.w_mini, R.string.w_mini_desc, 90, 90),
        WidgetSpec(TravelClockWidgetProvider::class.java, WidgetGroup.TRAVEL,
            R.string.group_travel, R.string.w_clock, R.string.w_clock_desc, 260, 150),
        WidgetSpec(WeatherTwoDayWidgetProvider::class.java, WidgetGroup.TRAVEL,
            R.string.group_travel, R.string.w_weather, R.string.w_weather_desc, 300, 130),
    )

    /** Builds the exact RemoteViews the launcher would show, with sample input and no clicks. */
    fun preview(context: Context, spec: WidgetSpec, paletteId: String? = null): RemoteViews {
        com.example.currencywidget.widget.WidgetPaletteStore.previewOverride = paletteId
        try {
            return build(context, spec)
        } finally {
            com.example.currencywidget.widget.WidgetPaletteStore.previewOverride = null
        }
    }

    fun byProvider(name: String?): WidgetSpec? = all.firstOrNull { it.provider.name == name }

    private fun build(context: Context, spec: WidgetSpec): RemoteViews {
        val provider = spec.provider.getDeclaredConstructor().newInstance()
        return when (provider) {
            is ConverterWidgetProvider -> provider.buildViews(context, ConverterWidgetProvider.PREVIEW_ID)
            is BudgetTrackerWidgetBase -> provider.buildViews(
                context, BudgetTrackerWidgetBase.PREVIEW_ID,
                BudgetTrackerWidgetBase.previewOptions(spec.widthDp, spec.heightDp)
            )
            is WeatherTwoDayWidgetProvider -> WeatherTwoDayWidgetProvider.buildViews(context, interactive = false)
            is TravelClockWidgetProvider -> TravelClockWidgetProvider.buildViews(context, interactive = false)
            else -> error("No preview for ${spec.provider.simpleName}")
        }
    }


    /** Asks the launcher to place the widget. Returns false if the launcher can't do it. */
    fun pin(context: Context, spec: WidgetSpec): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, spec.provider), Bundle(), null)
    }
}
