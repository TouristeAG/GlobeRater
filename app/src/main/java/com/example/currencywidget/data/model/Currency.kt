package com.example.currencywidget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class Currency(
    @PrimaryKey
    val code: String,
    val name: String,
    val symbol: String,
    val isInputCurrency: Boolean = false,
    val isOutputCurrency: Boolean = false,
    val isDefaultCurrency: Boolean = false
)
