package com.fake.pennypal.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // Unique username
    val password: String // Hashed or plain password (simple for now)
)