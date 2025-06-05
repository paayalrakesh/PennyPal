package com.fake.pennypal.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)

    fun setLoggedInUser(username: String) {
        prefs.edit().putString("loggedInUser", username).apply()
    }

    fun getLoggedInUser(): String? {
        return prefs.getString("loggedInUser", null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInUser() != null
    }
}