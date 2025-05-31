package com.fake.pennypal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fake.pennypal.data.local.entities.Category

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<Category>
}