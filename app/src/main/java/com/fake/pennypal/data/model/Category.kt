package com.fake.pennypal.data.model

import androidx.annotation.Keep

@Keep
data class Category(
    var name: String = "",
    var userId: String = ""
)
