package com.example.currencywidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.example.currencywidget.R
import com.example.currencywidget.data.repository.SharedBudgetManager
import com.example.currencywidget.data.repository.WidgetSettingsCache

/**
 * Shared logic of the four keypad converter widgets (compact / extended, with or without
 * budget). They used to be four ~400 line copies that each had their own bugs; the
 * layouts share the same view ids so one implementation drives all of them.
 *
 * Rendering only reads SharedPreferences: no network, no database, no coroutines.
 */
abstract class ConverterWidgetProvider : AppWidgetProvider() {

    @get:LayoutRes protected abstract val layoutRes: Int
    protected abstract val hasBudget: Boolean
    /** Number of output lines the layout provides. */
    protected abstract val outputSlots: Int
    /** Text sizes (sp) for 1, 2, 3, 4 and 5 visible outputs. */
    protected abstract val outputTextSizes: FloatArray

    companion object {
        private const val INPUT_PREFS = "ConverterWidgetInput"
        private const val ACTION_KEY = "com.example.currencywidget.converter.KEY"
        private const val ACTION_CURRENCY = "com.example.currencywidget.converter.CURRENCY"
        private const val ACTION_ADD_BUDGET = "com.example.currencywidget.converter.ADD_BUDGET"
        private const val EXTRA_TOKEN = "token"
        private const val EXTRA_CURRENCY = "currency"
        private const val TOKEN_CLEAR = "C"
        private const val TOKEN_BACK = "<"

        /** Used by the in-app previews: shows a sample amount, no click handlers. */
        const val PREVIEW_ID = -42
        private const val PREVIEW_AMOUNT = "100"

        private val keys = linkedMapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
            R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
            R.id.btn_8 to "8", R.id.btn_9 to "9", R.id.btn_comma to ",", R.id.btn_000 to "000",
            R.id.btn_clear to TOKEN_CLEAR, R.id.btn_backspace to TOKEN_BACK
        )
        private val currencyButtons = intArrayOf(R.id.currency_btn_1, R.id.currency_btn_2, R.id.currency_btn_3)
        private val outputIds = intArrayOf(
            R.id.currency_display_1, R.id.currency_display_2, R.id.currency_display_3,
            R.id.currency_display_4, R.id.currency_display_5
        )

        private fun prefs(context: Context) =
            context.getSharedPreferences(INPUT_PREFS, Context.MODE_PRIVATE)

        /** Clears what was typed in every converter widget (after it was added to the budget). */
        fun clearAllInputs(context: Context) {
            val p = prefs(context)
            val e = p.edit()
            p.all.keys.filter { it.startsWith("amount_") }.forEach { e.remove(it) }
            e.apply()
        }
    }

    // ---- State --------------------------------------------------------------------------

    private fun amount(context: Context, id: Int): String =
        if (id == PREVIEW_ID) PREVIEW_AMOUNT else prefs(context).getString("amount_$id", "") ?: ""

    private fun selectedCurrency(context: Context, id: Int, inputs: List<String>): String {
        val saved = prefs(context).getString("currency_$id", null)
        return if (saved != null && saved in inputs) saved else inputs.firstOrNull() ?: "USD"
    }

    // ---- AppWidgetProvider ----------------------------------------------------------------

    /** Budget fill animations collected while building views, played after the update. */
    private val animations = mutableListOf<BudgetAnimator.Job>()

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { manager.updateAppWidget(it, buildViews(context, it)) }
        BudgetAnimator.run(this, context, animations.toList())
        animations.clear()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        BudgetAnimator.forget(context, appWidgetIds)
        val e = prefs(context).edit()
        appWidgetIds.forEach { e.remove("amount_$it").remove("currency_$it") }
        e.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        when (intent.action) {
            SharedBudgetManager.ACTION_BUDGET_UPDATED -> if (hasBudget) WidgetRefresher.update(context, javaClass)
            ACTION_KEY -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                onKey(context, id, intent.getStringExtra(EXTRA_TOKEN) ?: return)
            }
            ACTION_CURRENCY -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs(context).edit().putString("currency_$id", intent.getStringExtra(EXTRA_CURRENCY)).apply()
                publishInput(context, id)
                redraw(context, id)
            }
            ACTION_ADD_BUDGET -> if (id != AppWidgetManager.INVALID_APPWIDGET_ID) addToBudget(context, id)
        }
    }

    private fun onKey(context: Context, id: Int, token: String) {
        val current = amount(context, id)
        val next = when (token) {
            TOKEN_CLEAR -> ""
            TOKEN_BACK -> AmountInput.backspace(current)
            else -> AmountInput.append(current, token)
        }
        prefs(context).edit().putString("amount_$id", next).apply()
        publishInput(context, id)
        redraw(context, id)
    }

    /** Lets the budget tracker widgets add "what was last typed" with their + button. */
    private fun publishInput(context: Context, id: Int) {
        val inputs = WidgetSettingsCache.getInstance(context).getInputCurrencies()
        SharedBudgetManager.getInstance(context)
            .setLastInput(AmountInput.parse(amount(context, id)), selectedCurrency(context, id, inputs))
    }

    private fun addToBudget(context: Context, id: Int) {
        val value = AmountInput.parse(amount(context, id))
        if (value <= 0.0) return
        val inputs = WidgetSettingsCache.getInstance(context).getInputCurrencies()
        val budget = SharedBudgetManager.getInstance(context)
        budget.addToBudget(value, selectedCurrency(context, id, inputs)) // notifies every budget widget
        prefs(context).edit().putString("amount_$id", "").apply()
        budget.setLastInput(0.0, selectedCurrency(context, id, inputs))
        redraw(context, id)
    }

    private fun redraw(context: Context, id: Int) {
        AppWidgetManager.getInstance(context).updateAppWidget(id, buildViews(context, id))
        BudgetAnimator.run(this, context, animations.toList())
        animations.clear()
    }

    // ---- Rendering ------------------------------------------------------------------------

    fun buildViews(context: Context, id: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        val settings = WidgetSettingsCache.getInstance(context)
        val inputs = settings.getInputCurrencies().take(3).ifEmpty { listOf("USD") }
        val selected = selectedCurrency(context, id, inputs)
        val typed = amount(context, id)
        val interactive = id != PREVIEW_ID
        val paint = WidgetPaint(context, WidgetPaletteStore.get(context, javaClass, preview = !interactive))

        views.setTextViewText(R.id.input_amount_display, "$selected ${AmountInput.display(typed)}")
        // The decimal key shows the separator of the phone's language.
        views.setTextViewText(R.id.btn_comma, Money.decimalSeparator().toString())

        // Currency selector (rounded squares)
        currencyButtons.forEachIndexed { i, buttonId ->
            val code = inputs.getOrNull(i)
            if (code == null) {
                views.setViewVisibility(buttonId, View.GONE)
                return@forEachIndexed
            }
            val isSelected = code == selected
            views.setViewVisibility(buttonId, View.VISIBLE)
            views.setTextViewText(buttonId, code)
            paint.bg(views, buttonId, if (isSelected) paint.d.cCurOn else paint.d.cCurOff)
            paint.text(views, buttonId, if (isSelected) paint.r.onAccent else paint.r.accent)
            if (interactive) {
                views.setOnClickPendingIntent(buttonId, broadcast(context, id, ACTION_CURRENCY, code) {
                    putExtra(EXTRA_CURRENCY, code)
                })
            }
        }

        // Keypad
        if (interactive) {
            keys.forEach { (viewId, token) ->
                views.setOnClickPendingIntent(viewId, broadcast(context, id, ACTION_KEY, token) {
                    putExtra(EXTRA_TOKEN, token)
                })
            }
            if (hasBudget) {
                views.setOnClickPendingIntent(R.id.btn_add_to_cart, broadcast(context, id, ACTION_ADD_BUDGET, "add") {})
            }
        }

        renderOutputs(context, views, AmountInput.parse(typed), selected, settings.getOutputCurrencies(), preview = !interactive)
        if (hasBudget) renderBudget(context, views, id, preview = !interactive)
        applyPalette(views, paint)
        if (interactive) {
            views.setOnClickPendingIntent(R.id.widget_container, WidgetPaletteStore.openPanelIntent(context, javaClass))
        }
        return views
    }

    /**
     * Colours of the original design, taken from the chosen palette:
     * container background, accent keys with light digits (or the inverse on the
     * compact budget widget, whose background is the budget gauge).
     */
    private fun applyPalette(views: RemoteViews, p: WidgetPaint) {
        val r = p.r
        val d = p.d
        if (!fillsBackground) p.bg(views, R.id.widget_container, d.cBg)
        p.bg(views, R.id.output_container, d.cOut)
        p.bg(views, R.id.classic_box_1, if (fillsBackground) d.cOutFlat else d.cOut)
        p.bg(views, R.id.classic_box_2, d.cOutFlat)
        p.texts(views, r.accent, R.id.input_amount_display, *outputIds)
        val keyBg = if (fillsBackground) d.cNumInv else d.cNum
        val keyText = if (fillsBackground) r.accent else r.onAccent
        keys.keys.forEach { p.bg(views, it, keyBg); p.text(views, it, keyText) }
        if (hasBudget) {
            p.bg(views, R.id.btn_add_to_cart, keyBg)
            p.text(views, R.id.btn_add_to_cart, keyText)
            p.progress(views, R.id.budget_progress_bar, r.accent, r.container)
            if (!fillsBackground) {
                p.bg(views, R.id.budget_used_text, d.cTextBg)
                p.text(views, R.id.budget_used_text, r.onAccent)
                p.text(views, R.id.budget_remaining_text, r.accent)
            }
        }
    }

    private fun renderOutputs(context: Context, views: RemoteViews, value: Double, from: String, outputs: List<String>, preview: Boolean) {
        // Previews show sample numbers until real rates exist; real widgets never fake rates.
        val real = RatesCache.hasRates(context)
        val rates = if (real || !preview) RatesCache.usdRates(context) else RatesCache.SAMPLE
        val hasRates = real || preview
        val shown = outputs.filter { it != from }.take(outputSlots)
        val size = outputTextSizes[(shown.size - 1).coerceIn(0, outputTextSizes.size - 1)]
        // Original spacing: fewer lines, more air between them.
        val spacingPx = ((if (outputSlots <= 3) 0 else when (shown.size) { 0, 1, 2 -> 10; 3 -> 8; 4 -> 6; else -> 4 }) *
            context.resources.displayMetrics.density).toInt()

        outputIds.forEachIndexed { i, viewId ->
            val code = shown.getOrNull(i)
            if (i >= outputSlots || code == null) {
                views.setViewVisibility(viewId, View.GONE)
                return@forEachIndexed
            }
            val converted = if (hasRates) RatesCache.convert(value, from, code, rates) else null
            views.setViewVisibility(viewId, View.VISIBLE)
            views.setTextViewText(viewId, converted?.let { Money.format(it, code) } ?: "— $code")
            views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, size)
            if (outputSlots > 3) views.setViewPadding(viewId, 0, 0, 0, if (i == shown.size - 1) 0 else spacingPx)
        }
    }

    private fun renderBudget(context: Context, views: RemoteViews, id: Int, preview: Boolean) {
        val budget = SharedBudgetManager.getInstance(context)
        budget.initializeFromSettingsCache()
        budget.checkAndResetBudgetIfNeeded()
        val total = budget.getScaledBudgetAmount()
        // Previews show a typical mid-period state instead of an empty gauge.
        val spent = budget.getTotalSpending().let { if (preview && it <= 0.0) total * 0.4 else it }
        val currency = budget.getBudgetCurrency()
        val target = (if (total > 0) spent / total else 0.0).toFloat().coerceIn(0f, 1f)
        // Glide from the value shown last time instead of jumping.
        val start = if (preview) target else BudgetAnimator.previousAndStore(context, id, target) ?: 0f

        fun paintBudget(v: RemoteViews, ratio: Float) {
            v.setProgressBar(R.id.budget_progress_bar, BudgetAnimator.MAX, (ratio * BudgetAnimator.MAX).toInt(), false)
            if (fillsBackground) return
            val shownSpent = if (ratio == target) spent else total * ratio
            v.setTextViewText(R.id.budget_used_text, Money.format(shownSpent, currency))
            // The total sits on the right of the bar; hide it once the used amount reaches it.
            v.setViewVisibility(R.id.budget_remaining_text, if (ratio >= 0.6f) View.GONE else View.VISIBLE)
        }

        if (!fillsBackground) views.setTextViewText(R.id.budget_remaining_text, Money.format(total, currency))
        paintBudget(views, start)
        if (start != target) {
            animations += BudgetAnimator.Job(id, start, target) { ratio ->
                RemoteViews(context.packageName, layoutRes).also { paintBudget(it, ratio) }
            }
        }
    }

    /** Compact + budget: the background is the gauge, so keys use the inverted colours. */
    protected open val fillsBackground: Boolean = false

    private inline fun broadcast(
        context: Context, id: Int, action: String, key: String, extras: Intent.() -> Unit
    ): PendingIntent {
        val intent = Intent(context, javaClass).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            extras()
        }.uniqueFor(id, key)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
