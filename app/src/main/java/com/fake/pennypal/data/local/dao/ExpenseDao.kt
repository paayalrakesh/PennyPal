package com.fake.pennypal.data.local.dao

import androidx.room.*
import com.fake.pennypal.data.local.entities.Expense

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpensesInRange(startDate: String, endDate: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE category = :category")
    suspend fun getExpensesByCategory(category: String): List<Expense>
}