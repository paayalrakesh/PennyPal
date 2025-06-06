package com.fake.pennypal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fake.pennypal.data.local.dao.UserDao
import com.fake.pennypal.data.local.dao.ExpenseDao
import com.fake.pennypal.data.local.dao.CategoryDao
import com.fake.pennypal.data.local.entities.User
import com.fake.pennypal.data.local.entities.Expense
import com.fake.pennypal.data.local.entities.Category

@Database(entities = [User::class, Expense::class, Category::class], version = 1,  exportSchema = false)
abstract class PennyPalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
}