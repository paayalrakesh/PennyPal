package com.fake.pennypal.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.room.Room
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.data.repository.UserRepository
import com.fake.pennypal.viewmodel.AuthViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fake.pennypal.utils.SessionManager


@Composable
fun LoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginSuccess by remember { mutableStateOf<Boolean?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }

    // Initialize RoomDB and AuthViewModel
    val db = remember {
        Room.databaseBuilder(
            context,
            PennyPalDatabase::class.java, "pennypal-db"
        ).build()
    }
    val repository = remember { UserRepository(db.userDao()) }
    val authViewModel = remember { AuthViewModel(repository) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF7F1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                text = "Penny Pal",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    coroutineScope.launch {
                        val success = withContext(Dispatchers.IO) {
                            authViewModel.loginUser(username, password)
                        }
                        if (success) {
                            sessionManager.setLoggedIn(true)
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            loginSuccess = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Login", fontSize = 18.sp, color = Color.Black)
            }

            loginSuccess?.let {
                if (!it) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Login failed. Please check your credentials.", color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Don't have an account? Sign Up", color = Color(0xFF388E3C))
            }
        }
    }
}
