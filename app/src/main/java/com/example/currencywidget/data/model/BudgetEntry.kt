package com.example.currencywidget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "budget_entries")
data class BudgetEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String,
    val description: String? = null,
    val category: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
