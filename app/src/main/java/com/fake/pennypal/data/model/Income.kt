package com.fake.pennypal.data.model

import androidx.annotation.Keep

@Keep
data class Income(
    var id: String = "",
    var date: String = "",
    var amount: Double = 0.0,
    var description: String = ""
)
