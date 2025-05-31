package com.fake.pennypal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fake.pennypal.data.local.entities.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun getUser(username: String, password: String): User?
}