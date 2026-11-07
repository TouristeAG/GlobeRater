package com.example.currencywidget.widget

import com.example.currencywidget.R

/** Compact keypad converter (3×3), up to 3 output currencies. */
class CurrencyConverterCompactWidgetProvider : ConverterWidgetProvider() {
    override val layoutRes = R.layout.currency_converter_widget_compact
    override val hasBudget = false
    override val outputSlots = 3
    override val outputTextSizes = floatArrayOf(16f, 15f, 12f)
}
