package com.fake.pennypal.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val sessionManager = SessionManager(application)

    fun signUp(
        username: String,
        password: String,
        fullName: String,
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            db.collection("users").document(username).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        onFailure("Username already exists.")
                    } else {
                        val user = hashMapOf(
                            "username" to username,
                            "password" to password,
                            "name" to fullName,
                            "userId" to userId
                        )
                        db.collection("users").document(username).set(user)
                            .addOnSuccessListener {
                                sessionManager.setLoggedInUser(username)
                                onSuccess()
                            }
                            .addOnFailureListener {
                                onFailure("Failed to create user.")
                            }
                    }
                }
                .addOnFailureListener {
                    onFailure("Signup failed. Check your connection.")
                }
        }
    }


    fun login(username: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            db.collection("users").document(username).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("password") == password) {
                        sessionManager.setLoggedInUser(username)
                        onSuccess()
                    } else {
                        onFailure("Invalid username or password.")
                    }
                }
                .addOnFailureListener {
                    onFailure("Login failed. Please try again.")
                }
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    fun getLoggedInUser(): String? {
        return sessionManager.getLoggedInUser()
    }
}