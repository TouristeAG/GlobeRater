package com.example.currencywidget.data.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    private const val BASE_URL_EXCHANGE = "https://openexchangerates.org/api/"
    private const val BASE_URL_OPEN_METEO_FORECAST = "https://api.open-meteo.com/"
    private const val BASE_URL_OPEN_METEO_GEOCODING = "https://geocoding-api.open-meteo.com/"

    // No logging interceptor: it would print the API key (query parameter) to logcat.
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideExchangeRateApi(client: OkHttpClient): ExchangeRateApi =
        retrofit(BASE_URL_EXCHANGE, client).create(ExchangeRateApi::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoGeocodingApi(client: OkHttpClient): OpenMeteoGeocodingApi =
        retrofit(BASE_URL_OPEN_METEO_GEOCODING, client).create(OpenMeteoGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideOpenMeteoForecastApi(client: OkHttpClient): OpenMeteoForecastApi =
        retrofit(BASE_URL_OPEN_METEO_FORECAST, client).create(OpenMeteoForecastApi::class.java)
}
