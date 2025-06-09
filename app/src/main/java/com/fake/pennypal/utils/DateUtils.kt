package com.fake.pennypal.utils

import android.util.Log
import java.util.*

private const val TAG = "DateUtils"

/**
 * A helper function to calculate a start and end date based on a filter string.
 * This is the single source of truth for date range calculations in the app.
 * It uses calendar-aligned periods for more intuitive filtering.
 *
 * @param filter The string representing the desired period ("Daily", "Weekly", etc.).
 * @return A Pair of Date objects representing the start and end of the period.
 */
fun getDateRange(filter: String): Pair<Date, Date> {
    val calendar = Calendar.getInstance()

    // --- End Date ---
    // The end date is always the very end of today.
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val end = calendar.time

    // --- Start Date ---
    // Reset the calendar to the very beginning of today for start date calculations.
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    when (filter) {
        "Daily" -> {
            // No change needed. The start is the beginning of today.
        }
        "Weekly" -> {
            // Go back 6 days to get a 7-day period (including today).
            calendar.add(Calendar.DAY_OF_YEAR, -6)
        }
        "Monthly" -> {
            // Set the date to the first day of the current month.
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        "Yearly" -> {
            // Set the date to the first day of the current year.
            calendar.set(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val start = calendar.time

    Log.d(TAG, "Date Range for '$filter': From $start To $end")
    return start to end
}