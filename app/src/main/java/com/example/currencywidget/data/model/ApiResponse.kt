package com.example.currencywidget.data.model

/** OpenExchangeRates latest.json (only the fields we use). */
data class ExchangeRateResponse(
    val timestamp: Long,
    val base: String,
    val rates: Map<String, Double>
)
