package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GoalScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var incomeGoal by remember { mutableStateOf("") }
    var spendingLimit by remember { mutableStateOf("") }
    var minSpendingGoal by remember { mutableStateOf("") }

    // Load existing goals
    LaunchedEffect(Unit) {
        try {
            val doc = db.collection("goals").document("default").get().await()
            if (doc.exists()) {
                incomeGoal = doc.getDouble("incomeGoal")?.toString() ?: ""
                spendingLimit = doc.getDouble("spendingLimit")?.toString() ?: ""
                minSpendingGoal = doc.getDouble("minSpendingGoal")?.toString() ?: ""
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load goals: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("manageCategories") }) {
                    Icon(Icons.Default.List, contentDescription = "Categories")
                }
                IconButton(onClick = { navController.navigate("addChoice") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                IconButton(onClick = { navController.navigate("goals") }) {
                    Icon(Icons.Default.Star, contentDescription = "Goals")
                }
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .background(Color(0xFFF1F8E9)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set Your Financial Goals", fontSize = 24.sp, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = incomeGoal,
                onValueChange = { incomeGoal = it },
                label = { Text("Monthly Income Goal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = spendingLimit,
                onValueChange = { spendingLimit = it },
                label = { Text("Maximum Monthly Spending Limit") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = minSpendingGoal,
                onValueChange = { minSpendingGoal = it },
                label = { Text("Minimum Monthly Spending Goal (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val income = incomeGoal.toDoubleOrNull()
                    val spending = spendingLimit.toDoubleOrNull()
                    val minSpending = minSpendingGoal.toDoubleOrNull()

                    scope.launch {
                        if (income != null && spending != null) {
                            try {
                                val data = mutableMapOf<String, Any>(
                                    "incomeGoal" to income,
                                    "spendingLimit" to spending
                                )
                                if (minSpending != null) {
                                    data["minSpendingGoal"] = minSpending
                                }
                                db.collection("goals").document("default").set(data).await()
                                snackbarHostState.showSnackbar("Goals saved successfully.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving goals: ${e.message}")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Please enter valid income and max spending amounts.")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Goals", color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Current Goals", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (incomeGoal.isNotEmpty() || spendingLimit.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Income Goal: R$incomeGoal", color = Color(0xFF388E3C))
                    Text("Max Spending: R$spendingLimit", color = Color(0xFF388E3C))
                    if (minSpendingGoal.isNotEmpty()) {
                        Text("Min Spending: R$minSpendingGoal", color = Color(0xFF388E3C))
                    }
                }
            } else {
                Text("No goals saved yet.", color = Color.Gray)
            }
        }
    }
}
