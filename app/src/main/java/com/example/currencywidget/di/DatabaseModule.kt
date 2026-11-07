package com.example.currencywidget.di

import android.content.Context
import androidx.room.Room
import com.example.currencywidget.data.database.CurrencyDatabase
import com.example.currencywidget.data.database.CurrencyDao
import com.example.currencywidget.data.database.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCurrencyDatabase(@ApplicationContext context: Context): CurrencyDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            CurrencyDatabase::class.java,
            "currency_database"
        )
        .fallbackToDestructiveMigration() // Recreate database if schema changes
        .build()
    }

    @Provides
    fun provideCurrencyDao(database: CurrencyDatabase): CurrencyDao {
        return database.currencyDao()
    }



    @Provides
    fun provideSettingsDao(database: CurrencyDatabase): SettingsDao {
        return database.settingsDao()
    }
}
