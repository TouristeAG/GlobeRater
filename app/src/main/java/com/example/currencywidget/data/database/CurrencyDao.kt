package com.example.currencywidget.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.currencywidget.data.model.Currency
import kotlinx.coroutines.flow.Flow

/** Currency names list shown in the pickers (fetched once from OpenExchangeRates). */
@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies ORDER BY code")
    fun getAllCurrencies(): Flow<List<Currency>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<Currency>)
}
