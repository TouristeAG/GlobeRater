package com.example.currencywidget.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Open-Meteo is free and needs no API key. Only the fields we display are requested.

data class OpenMeteoCurrent(
    val temperature_2m: Double?,
    val weather_code: Int?,
    val is_day: Int?
)

data class OpenMeteoDaily(
    val time: List<String>?,
    val temperature_2m_min: List<Double?>?,
    val temperature_2m_max: List<Double?>?,
    val precipitation_probability_max: List<Double?>?,
    val weather_code: List<Int?>?
)

data class OpenMeteoForecastResponse(
    val timezone: String?,
    val current: OpenMeteoCurrent?,
    val daily: OpenMeteoDaily?
)

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("forecast_days") days: Int = 3,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoForecastResponse>
}

data class OpenMeteoGeocodingResult(
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val country: String?,
    val admin1: String?,
    val timezone: String?
)

data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>?
)

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "en"
    ): Response<OpenMeteoGeocodingResponse>
}

data class GeoCodingResult(
    val name: String?,
    val lat: Double?,
    val lon: Double?,
    val country: String?,
    val state: String?,
    val timezone: String? = null
) {
    val label: String get() = listOfNotNull(name, state, country).distinct().joinToString(", ")
}
