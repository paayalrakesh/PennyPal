package com.fake.pennypal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val amount: Double,
    val category: String,
    val description: String,
    val photoUri: String? = null // Optional photo
)