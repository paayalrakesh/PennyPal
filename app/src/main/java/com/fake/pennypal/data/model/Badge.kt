package com.fake.pennypal.data.model

import androidx.annotation.Keep

@Keep
data class Badge(
    var title: String = "",
    var description: String = "",
    var earnedDate: String = ""
)
