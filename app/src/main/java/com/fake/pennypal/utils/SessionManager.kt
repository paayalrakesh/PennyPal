package com.fake.pennypal.utils

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("penny_pal_prefs", Context.MODE_PRIVATE)

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit().putBoolean("isLoggedIn", loggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("isLoggedIn", false)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}