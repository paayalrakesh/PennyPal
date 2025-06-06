package com.fake.pennypal.data.model

import androidx.annotation.Keep

@Keep
data class User(
    var username: String = "",
    var password: String = ""
)
