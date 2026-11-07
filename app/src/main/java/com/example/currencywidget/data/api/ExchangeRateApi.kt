package com.example.currencywidget.data.api

import com.example.currencywidget.data.model.ExchangeRateResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {

    /** The free OpenExchangeRates plan only allows USD as base; we convert locally. */
    @GET("latest.json")
    suspend fun getLatestRates(
        @Query("app_id") apiKey: String
    ): Response<ExchangeRateResponse>

    /** Public endpoint, no key required. */
    @GET("currencies.json")
    suspend fun getCurrencies(): Response<Map<String, String>>
}
