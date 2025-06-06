package com.fake.pennypal.utils

object CurrencyConverter {

    // Manually set your conversion rates
    private val rates = mapOf(
        "ZAR" to 1.0,
        "USD" to 0.054,
        "EUR" to 0.050,
        "GBP" to 0.042,
        "INR" to 4.5
    )

    fun convert(amount: Double, from: String, to: String): Double {
        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0
        return amount / fromRate * toRate
    }

    fun getAvailableCurrencies(): List<String> = rates.keys.toList()
}
