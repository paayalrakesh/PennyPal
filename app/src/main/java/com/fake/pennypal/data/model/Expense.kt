package com.fake.pennypal.data.model

data class Expense(
    val id: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = ""
)
