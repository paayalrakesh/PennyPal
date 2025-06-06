@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GoalScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val username = sessionManager.getLoggedInUser() ?: ""

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var incomeGoal by remember { mutableStateOf("") }
    var spendingLimit by remember { mutableStateOf("") }
    var minSpendingGoal by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val doc = db.collection("users").document(username).collection("goals").document("default").get().await()
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
            NavigationBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("categorySpendingPreview") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Category Spending")
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF1F8E9))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Set Your Financial Goals",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = incomeGoal,
                onValueChange = { incomeGoal = it },
                label = { Text("Monthly Income Goal") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = spendingLimit,
                onValueChange = { spendingLimit = it },
                label = { Text("Max Monthly Spending Limit") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = minSpendingGoal,
                onValueChange = { minSpendingGoal = it },
                label = { Text("Min Monthly Spending Goal (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val income = incomeGoal.toDoubleOrNull()
                    val spending = spendingLimit.toDoubleOrNull()
                    val minSpending = minSpendingGoal.toDoubleOrNull()

                    scope.launch {
                        val username = SessionManager(context).getLoggedInUser() ?: return@launch

                        if (income != null && spending != null) {
                            try {
                                val data = mutableMapOf<String, Any>(
                                    "incomeGoal" to income,
                                    "spendingLimit" to spending
                                )
                                if (minSpending != null) {
                                    data["minSpendingGoal"] = minSpending
                                }

                                db.collection("users")
                                    .document(username)
                                    .collection("goals")
                                    .document("default")
                                    .set(data)
                                    .await()

                                snackbarHostState.showSnackbar("Goals saved successfully.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error saving goals: ${e.message}")
                            }
                        } else {
                            snackbarHostState.showSnackbar("Enter valid income and spending amounts.")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Goals", fontSize = 16.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Current Goals", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))

            if (incomeGoal.isNotEmpty() || spendingLimit.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Income Goal: R$incomeGoal", color = Color(0xFF388E3C), fontSize = 14.sp)
                        Text("Max Spending: R$spendingLimit", color = Color(0xFF388E3C), fontSize = 14.sp)
                        if (minSpendingGoal.isNotEmpty()) {
                            Text("Min Spending: R$minSpendingGoal", color = Color(0xFF388E3C), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Text("No goals saved yet.", color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(100.dp)) // extra space for bottom nav clearance
        }
    }
}
