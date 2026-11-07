package com.example.currencywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.currencywidget.R
import com.example.currencywidget.data.repository.SharedBudgetManager
import kotlin.math.min

/** Snapshot of the budget used by every tracker layout. */
data class BudgetSnapshot(val total: Double, val spent: Double, val currency: String) {
    val ratio: Double get() = if (total > 0) (spent / total).coerceAtLeast(0.0) else 0.0
    val percent: Int get() = (ratio * 100).toInt().coerceIn(0, 100)

    companion object {
        fun load(context: Context): BudgetSnapshot {
            val budget = SharedBudgetManager.getInstance(context)
            budget.initializeFromSettingsCache()
            budget.checkAndResetBudgetIfNeeded()
            return BudgetSnapshot(budget.getScaledBudgetAmount(), budget.getTotalSpending(), budget.getBudgetCurrency())
        }
    }
}

/**
 * Budget tracker widgets: a progress shape and a "+" button that adds the amount last
 * typed in any converter widget.
 */
abstract class BudgetTrackerWidgetBase : AppWidgetProvider() {

    protected abstract val layoutRes: Int

    /** Renders the layout. [options] carries the widget size in dp (min/max width/height). */
    protected abstract fun render(context: Context, views: RemoteViews, budget: BudgetSnapshot, options: Bundle, paint: WidgetPaint)

    companion object {
        private const val ACTION_ADD = "com.example.currencywidget.tracker.ADD"
        const val PREVIEW_ID = -43

        fun previewOptions(widthDp: Int, heightDp: Int) = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }

        private val addButtons = intArrayOf(
            R.id.btn_add_to_budget_xlarge, R.id.btn_add_to_budget_large,
            R.id.btn_add_to_budget_medium, R.id.btn_add_to_budget_small
        )

        /** 0 = xlarge … 3 = small, from the smallest widget dimension. */
        fun sizeClass(options: Bundle): Int {
            val minDim = min(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            )
            return when {
                minDim >= 150 -> 0
                minDim >= 120 -> 1
                minDim >= 90 -> 2
                else -> 3
            }
        }
    }

    /** Which "+" button size fits this widget (0 = xlarge … 3 = small). */
    protected open fun buttonSize(options: Bundle): Int = sizeClass(options)

    /** Fill animations collected while building views, played after the update. */
    private val animations = mutableListOf<BudgetAnimator.Job>()

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { redraw(context, manager, it) }
        playAnimations(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        redraw(context, manager, appWidgetId)
        playAnimations(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = BudgetAnimator.forget(context, appWidgetIds)

    private fun playAnimations(context: Context) {
        BudgetAnimator.run(this, context, animations.toList())
        animations.clear()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            SharedBudgetManager.ACTION_BUDGET_UPDATED -> WidgetRefresher.update(context, javaClass)
            ACTION_ADD -> addLastInput(context)
        }
    }

    private fun addLastInput(context: Context) {
        val budget = SharedBudgetManager.getInstance(context)
        budget.initializeFromSettingsCache()
        val amount = budget.getLastInputAmount()
        val currency = budget.getLastInputCurrency()
        if (amount <= 0.0) return
        budget.addToBudget(amount, currency) // broadcasts BUDGET_UPDATED to every budget widget
        budget.setLastInput(0.0, currency)
        // The typed amount has been "spent": clear it in the converter widgets too.
        ConverterWidgetProvider.clearAllInputs(context)
        WidgetRefresher.providers
            .filter { ConverterWidgetProvider::class.java.isAssignableFrom(it) }
            .forEach { WidgetRefresher.update(context, it) }
    }

    private fun redraw(context: Context, manager: AppWidgetManager, id: Int) {
        manager.updateAppWidget(id, buildViews(context, id, manager.getAppWidgetOptions(id)))
    }

    fun buildViews(context: Context, id: Int, options: Bundle): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        val paint = WidgetPaint(context, WidgetPaletteStore.get(context, javaClass, preview = id == PREVIEW_ID))
        val size = buttonSize(options)
        addButtons.forEachIndexed { i, buttonId ->
            views.setViewVisibility(buttonId, if (i == size) View.VISIBLE else View.GONE)
            paint.bg(views, buttonId, paint.d.cFab)
            paint.text(views, buttonId, paint.r.accent)
        }
        // Fill = accent, empty part = container (original design) (Android 12+; older versions keep Ink).
        paint.progress(views, R.id.budget_progress_bar, paint.r.accent, paint.r.container)
        if (id != PREVIEW_ID) {
            views.setOnClickPendingIntent(R.id.widget_container, WidgetPaletteStore.openPanelIntent(context, javaClass))
        }
        if (id != PREVIEW_ID) {
            val intent = Intent(context, javaClass).setAction(ACTION_ADD).uniqueFor(id, "add")
            views.setOnClickPendingIntent(
                addButtons[size],
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        }
        val budget = BudgetSnapshot.load(context).let {
            // Previews with nothing spent would look empty: show a typical mid-period state.
            if (id == PREVIEW_ID && it.spent <= 0.0) it.copy(spent = it.total * 0.4) else it
        }
        val target = budget.ratio.toFloat().coerceIn(0f, 1f)
        // Start from what the widget showed last time (or empty for a new widget) and glide.
        val start = if (id == PREVIEW_ID) target else BudgetAnimator.previousAndStore(context, id, target) ?: 0f
        fun at(ratio: Float) = if (ratio == target) budget else budget.copy(spent = budget.total * ratio)
        views.setProgressBar(R.id.budget_progress_bar, BudgetAnimator.MAX, (start * BudgetAnimator.MAX).toInt(), false)
        render(context, views, at(start), options, paint)
        if (start != target) {
            animations += BudgetAnimator.Job(id, start, target) { ratio ->
                RemoteViews(context.packageName, layoutRes).also { frame ->
                    frame.setProgressBar(R.id.budget_progress_bar, BudgetAnimator.MAX, (ratio * BudgetAnimator.MAX).toInt(), false)
                    render(context, frame, at(ratio), options, paint)
                }
            }
        }
        return views
    }
}

/** Tall pill (1×2) that fills from the bottom. */
class BudgetTrackerWidgetProvider : BudgetTrackerWidgetBase() {
    override val layoutRes = R.layout.budget_tracker_widget

    override fun render(context: Context, views: RemoteViews, budget: BudgetSnapshot, options: Bundle, paint: WidgetPaint) {
        paint.text(views, R.id.budget_total_text, paint.r.accent)
        paint.text(views, R.id.budget_used_marker_text, paint.r.container)
        val ratio = budget.ratio
        if (ratio < 0.66) {
            views.setViewVisibility(R.id.budget_total_text, View.VISIBLE)
            views.setTextViewText(R.id.budget_total_text, Money.format(budget.total, budget.currency))
        } else {
            views.setViewVisibility(R.id.budget_total_text, View.GONE)
        }

        if (ratio < 0.51) {
            views.setViewVisibility(R.id.budget_used_marker_text, View.GONE)
            return
        }
        // Spent amount follows the top of the filled area, just under the fill line.
        val density = context.resources.displayMetrics.density
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            .takeIf { it > 0 } ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val areaPx = ((heightDp * density) - 20 * density).toInt().coerceAtLeast(0)
        val fillTopFromBottom = (ratio.coerceIn(0.0, 1.0) * areaPx).toInt()
        val paddingTop = ((areaPx - fillTopFromBottom) + (2 * density).toInt()).coerceIn(0, areaPx)
        views.setViewPadding(R.id.used_marker_container, 0, paddingTop, 0, 0)
        views.setTextViewText(R.id.budget_used_marker_text, Money.format(budget.spent, budget.currency))
        views.setViewVisibility(R.id.budget_used_marker_text, View.VISIBLE)
    }
}

/** Wide bar (2×1) that fills from the left. */
class HorizontalBudgetTrackerWidgetProvider : BudgetTrackerWidgetBase() {
    override val layoutRes = R.layout.horizontal_budget_tracker_widget

    // The button sits in the height minus 2×10dp padding: 120/100/80/52dp.
    override fun buttonSize(options: Bundle): Int {
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) - 20
        return when { h >= 120 -> 0; h >= 100 -> 1; h >= 80 -> 2; else -> 3 }
    }

    override fun render(context: Context, views: RemoteViews, budget: BudgetSnapshot, options: Bundle, paint: WidgetPaint) {
        paint.texts(views, paint.r.container, R.id.budget_used_value_text, R.id.budget_used_currency_text)
        val density = context.resources.displayMetrics.density
        val widthPx = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) * density).toInt()
        val areaPx = (widthPx - (20 * density).toInt()).coerceAtLeast(0)
        val fillRight = (budget.ratio.coerceIn(0.0, 1.0) * areaPx).toInt()
        val leftPadding = (10 * density).toInt()
        // Keep the amount clear of the "+" button on the right (≈90dp of text width).
        val buttonDp = intArrayOf(120, 100, 80, 52)[buttonSize(options)]
        val maxLeft = (widthPx - ((buttonDp + 8 + 10 + 90) * density).toInt()).coerceAtLeast(leftPadding)
        val markerLeft = if (budget.ratio >= 0.65) leftPadding
        else (fillRight - (57 * density).toInt()).coerceIn(leftPadding, maxLeft)
        views.setViewPadding(R.id.used_marker_container, markerLeft, 0, 0, 0)
        views.setTextViewText(R.id.budget_used_value_text, Money.number(budget.spent, budget.currency))
        views.setTextViewText(R.id.budget_used_currency_text, budget.currency)
    }
}

/** Small square (1×1) with the spent amount. */
class MinimalBudgetTrackerWidgetProvider : BudgetTrackerWidgetBase() {
    override val layoutRes = R.layout.minimal_budget_tracker_widget

    private val texts = intArrayOf(
        R.id.budget_amount_text_xlarge, R.id.budget_amount_text_large,
        R.id.budget_amount_text_medium, R.id.budget_amount_text_small
    )

    override fun render(context: Context, views: RemoteViews, budget: BudgetSnapshot, options: Bundle, paint: WidgetPaint) {
        val size = sizeClass(options)
        val textId = texts[size]
        texts.forEach { views.setViewVisibility(it, if (it == textId) View.VISIBLE else View.GONE) }
        views.setTextViewText(textId, Money.format(budget.spent, budget.currency))
        // Text sits on the fill once it is past ~12%: switch to the light container colour.
        paint.text(views, textId, if (budget.percent <= 12) paint.r.accent else paint.r.container)
    }
}
