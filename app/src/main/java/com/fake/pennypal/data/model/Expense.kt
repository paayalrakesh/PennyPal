package com.fake.pennypal.data.model

import androidx.annotation.Keep

@Keep
data class Expense(
    var id: String = "",
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var description: String = "",
    var startTime: String = "",
    var endTime: String = "",
    var photoUrl: String? = null
)
