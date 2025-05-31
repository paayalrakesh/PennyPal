package com.fake.pennypal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.pennypal.data.repository.UserRepository
import com.fake.pennypal.data.local.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: UserRepository) : ViewModel() {
    fun registerUser(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertUser(User(username, password))
        }
    }

    suspend fun loginUser(username: String, password: String): Boolean {
        return repository.getUser(username, password) != null
    }

}