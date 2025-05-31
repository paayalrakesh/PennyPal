package com.fake.pennypal.data.repository

import com.fake.pennypal.data.local.dao.UserDao
import com.fake.pennypal.data.local.entities.User

class UserRepository(private val userDao: UserDao) {
    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun getUser(username: String, password: String): User? {
        return userDao.getUser(username, password)
    }
}