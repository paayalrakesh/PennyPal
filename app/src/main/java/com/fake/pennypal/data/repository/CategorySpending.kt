package com.fake.pennypal.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.data.model.Goal
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import android.util.Log

data class CategorySpendingData(
    val category: String,
    val totalSpent: Double,
    val minGoal: Double?,
    val maxGoal: Double?
)

suspend fun getCategorySpendingData(
    db: FirebaseFirestore,
    filter: String // "daily", "weekly", "monthly", "yearly"
): List<CategorySpendingData> {
    val today = Calendar.getInstance()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val filteredExpenses = mutableListOf<Expense>()

    val snapshot = db.collection("expenses").get().await()
    for (doc in snapshot.documents) {
        val expense = doc.toObject(Expense::class.java)
        if (expense != null) {
            val rawDate = expense.date

            val parsedDate = try {
                if (rawDate.isNullOrBlank()) {
                    Log.e("ParseError", "Date is blank or null for expense: ${expense.id}")
                    null
                } else {
                    formatter.parse(rawDate)
                }
            } catch (e: Exception) {
                Log.e("ParseError", "Failed to parse date: '$rawDate' for expense: ${expense.id}")
                e.printStackTrace()
                null
            }

            if (parsedDate == null) continue

            val expenseDate = Calendar.getInstance().apply {
                time = parsedDate
            }

            val match = when (filter) {
                "daily" -> expenseDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                        && expenseDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                "weekly" -> expenseDate.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
                        && expenseDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                "monthly" -> expenseDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                        && expenseDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                "yearly" -> expenseDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                else -> false
            }

            if (match) filteredExpenses.add(expense)
        }
    }

    // Group by category
    val totals = filteredExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    // Fetch goals
    val goalMap = HashMap<String, Goal>()
    val goalsSnap = db.collection("goals").get().await()
    for (doc in goalsSnap.documents) {
        val goal = doc.toObject(Goal::class.java)
        if (goal != null) {
            goalMap[goal.id] = goal
        }
    }

    return totals.map { (category, total) ->
        val goal = goalMap[category]
        CategorySpendingData(
            category = category,
            totalSpent = total,
            minGoal = goal?.minGoal,
            maxGoal = goal?.maxGoal
        )
    }
}
