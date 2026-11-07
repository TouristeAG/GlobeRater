package com.example.currencywidget.data.database

import androidx.room.TypeConverter
import com.example.currencywidget.data.model.WidgetLayoutType
import com.example.currencywidget.data.model.BudgetTimeSpan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromWidgetLayoutType(value: WidgetLayoutType): String {
        return value.name
    }
    
    @TypeConverter
    fun toWidgetLayoutType(value: String): WidgetLayoutType {
        return WidgetLayoutType.valueOf(value)
    }
    
    @TypeConverter
    fun fromBudgetTimeSpan(value: BudgetTimeSpan): String {
        return value.name
    }
    
    @TypeConverter
    fun toBudgetTimeSpan(value: String): BudgetTimeSpan {
        return BudgetTimeSpan.valueOf(value)
    }
}
