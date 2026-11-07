package com.example.currencywidget.widget

import com.example.currencywidget.R

/** Extended keypad converter (3×4), up to 5 output currencies. */
class CurrencyConverterWidgetProvider : ConverterWidgetProvider() {
    override val layoutRes = R.layout.currency_converter_widget
    override val hasBudget = false
    override val outputSlots = 5
    override val outputTextSizes = floatArrayOf(20f, 19f, 18f, 16f, 14f)
}
