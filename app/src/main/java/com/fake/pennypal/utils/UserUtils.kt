package com.fake.pennypal.utils

import android.content.Context

fun getCurrentUsername(context: Context): String {
    val prefs = context.getSharedPreferences("PennyPalPrefs", Context.MODE_PRIVATE)
    return prefs.getString("username", "unknown_user") ?: "unknown_user"
}
