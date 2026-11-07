package com.example.currencywidget.widget

import com.example.currencywidget.R

/** Extended converter with the budget bar and a "+" button that adds the typed amount. */
class CurrencyConverterExpandedBudgetWidgetProvider : ConverterWidgetProvider() {
    override val layoutRes = R.layout.currency_converter_expanded_budget_widget
    override val hasBudget = true
    override val outputSlots = 5
    override val outputTextSizes = floatArrayOf(20f, 19f, 18f, 16f, 14f)
}
