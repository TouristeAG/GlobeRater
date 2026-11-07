package com.example.currencywidget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.widget.RemoteViews
import kotlin.math.abs
import kotlin.math.pow

/**
 * Smoothly fills / empties budget bars on home-screen widgets.
 *
 * RemoteViews cannot animate a ProgressBar by themselves, so after the normal update the
 * widget is pushed ~18 partial updates (only the bar and the amounts) over 600 ms with an
 * ease-out curve. It only runs when the budget value actually changes (add, reset, new
 * period): no timers, no work while nothing happens.
 */
object BudgetAnimator {

    /** Fine-grained progress so intermediate frames look smooth. */
    const val MAX = 1000

    private const val PREFS = "BudgetAnimation"
    private const val DURATION_MS = 600L
    private const val FRAMES = 18

    /** One widget to animate: [frame] builds the partial RemoteViews for a ratio (0..1). */
    class Job(val appWidgetId: Int, val from: Float, val to: Float, val frame: (Float) -> RemoteViews)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Ratio the widget was showing last time (null for a brand-new widget) and remembers the
     * new one. Previews never animate.
     */
    fun previousAndStore(context: Context, appWidgetId: Int, target: Float): Float? {
        val key = "r_$appWidgetId"
        val p = prefs(context)
        val previous = if (p.contains(key)) p.getFloat(key, target) else null
        p.edit().putFloat(key, target).apply()
        return previous
    }

    fun forget(context: Context, appWidgetIds: IntArray) {
        val e = prefs(context).edit()
        appWidgetIds.forEach { e.remove("r_$it") }
        e.apply()
    }

    /**
     * Plays the jobs. Call right after the full `updateAppWidget`. When called from a
     * receiver, [receiver] keeps the process alive (goAsync) until the last frame.
     */
    fun run(receiver: BroadcastReceiver?, context: Context, jobs: List<Job>) {
        val active = jobs.filter { abs(it.to - it.from) > 0.002f }
        if (active.isEmpty()) return
        val pending = runCatching { receiver?.goAsync() }.getOrNull()
        val manager = AppWidgetManager.getInstance(context.applicationContext)
        Thread {
            try {
                val step = DURATION_MS / FRAMES
                for (i in 1..FRAMES) {
                    Thread.sleep(step)
                    val t = i / FRAMES.toFloat()
                    val eased = 1f - (1f - t).pow(3) // ease-out cubic
                    active.forEach { job ->
                        val value = job.from + (job.to - job.from) * eased
                        runCatching { manager.partiallyUpdateAppWidget(job.appWidgetId, job.frame(if (i == FRAMES) job.to else value)) }
                    }
                }
            } finally {
                pending?.finish()
            }
        }.apply { name = "budget-animation" }.start()
    }
}
