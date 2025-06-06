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
        val currency = getSelectedCurrency()
        prefs.edit().clear().apply()
        setSelectedCurrency(currency) // ✅ preserve currency
    }


    fun isLoggedIn(): Boolean {
        return getLoggedInUser() != null
    }
    fun setSelectedCurrency(currencyCode: String) {
        prefs.edit().putString("currency", currencyCode).apply()
    }

    fun getSelectedCurrency(): String {
        return prefs.getString("currency", "ZAR") ?: "ZAR" // Default to ZAR
    }


}