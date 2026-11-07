package com.example.currencywidget.widget

import com.example.currencywidget.R

/** Compact converter whose background fills up as the budget is spent; "+" adds the typed amount. */
class CurrencyConverterCompactBudgetWidgetProvider : ConverterWidgetProvider() {
    override val layoutRes = R.layout.currency_converter_widget_compact_budget
    override val hasBudget = true
    override val outputSlots = 3
    override val outputTextSizes = floatArrayOf(16f, 15f, 12f)
    override val fillsBackground = true
}
