/*
Title: State and Jetpack Compose (remember, mutableStateOf)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/state
*/

/*
Title: Side-effects in Jetpack Compose (LaunchedEffect, rememberCoroutineScope)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects
*/

/*
Title: Get data with Cloud Firestore | Kotlin+KTX
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/get-data
*/

/*
Title: Add data to Cloud Firestore | Kotlin+KTX
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/manage-data/add-data
*/

/*
Title: Material Components for Compose (Scaffold, OutlinedTextField, Button, Card)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/components
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.utils.CurrencyConverter
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

// Added a TAG for logging, which helps in filtering Logcat messages for this specific screen.
private const val TAG = "GoalScreen"

@Composable
fun GoalScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val username = sessionManager.getLoggedInUser() ?: ""
    // Get the currently selected currency from SessionManager.
    val selectedCurrency = sessionManager.getSelectedCurrency()

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State variables for the text fields. They will hold strings representing amounts in the selected currency.
    var incomeGoal by remember { mutableStateOf("") }
    var spendingLimit by remember { mutableStateOf("") }
    var minSpendingGoal by remember { mutableStateOf("") }

    // LaunchedEffect will run when the screen is first composed and whenever the selectedCurrency changes.
    // This ensures that if the user changes currency on another screen and comes back, the values will update.
    LaunchedEffect(username, selectedCurrency) {
        if (username.isBlank()) return@LaunchedEffect

        Log.d(TAG, "LaunchedEffect triggered for user: $username, currency: $selectedCurrency")
        try {
            val doc = db.collection("users").document(username).collection("goals").document("default").get().await()
            if (doc.exists()) {
                // Step 1: Fetch goal values from Firestore. They are stored in the base currency (ZAR).
                val incomeGoalZAR = doc.getDouble("incomeGoal") ?: 0.0
                val spendingLimitZAR = doc.getDouble("spendingLimit") ?: 0.0
                val minSpendingGoalZAR = doc.getDouble("minSpendingGoal") ?: 0.0
                Log.d(TAG, "Fetched goals from DB (in ZAR): Income=$incomeGoalZAR, Limit=$spendingLimitZAR, Min=$minSpendingGoalZAR")


                // Step 2: Convert the ZAR values to the user's selected currency for display.
                val incomeGoalConverted = CurrencyConverter.convert(incomeGoalZAR, "ZAR", selectedCurrency)
                val spendingLimitConverted = CurrencyConverter.convert(spendingLimitZAR, "ZAR", selectedCurrency)
                val minSpendingGoalConverted = CurrencyConverter.convert(minSpendingGoalZAR, "ZAR", selectedCurrency)
                Log.d(TAG, "Converted goals to $selectedCurrency: Income=$incomeGoalConverted, Limit=$spendingLimitConverted, Min=$minSpendingGoalConverted")

                // Step 3: Update the state with the converted and formatted string values.
                // We only show a value if it's greater than 0, otherwise the field is empty.
                incomeGoal = if (incomeGoalConverted > 0) String.format(Locale.US, "%.2f", incomeGoalConverted) else ""
                spendingLimit = if (spendingLimitConverted > 0) String.format(Locale.US, "%.2f", spendingLimitConverted) else ""
                minSpendingGoal = if (minSpendingGoalConverted > 0) String.format(Locale.US, "%.2f", minSpendingGoalConverted) else ""

            } else {
                Log.d(TAG, "No default goals document found for user: $username")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load goals", e)
            scope.launch {
                snackbarHostState.showSnackbar("Failed to load goals: ${e.message}")
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { navController.navigate("home") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { navController.navigate("categorySpendingPreview") }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph")
                    }
                    IconButton(onClick = { navController.navigate("manageCategories") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.List, contentDescription = "Categories")
                    }
                    IconButton(onClick = { navController.navigate("addChoice") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = { navController.navigate("goals") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, contentDescription = "Goals")
                    }
                    IconButton(onClick = { navController.navigate("profile") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
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
                label = { Text("Monthly Income Goal ($selectedCurrency)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                // FIX: Use KeyboardType.Decimal for decimal input
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = spendingLimit,
                onValueChange = { spendingLimit = it },
                label = { Text("Max Monthly Spending Limit ($selectedCurrency)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                // FIX: Use KeyboardType.Decimal for decimal input
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = minSpendingGoal,
                onValueChange = { minSpendingGoal = it },
                label = { Text("Min Monthly Spending Goal ($selectedCurrency)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                // FIX: Use KeyboardType.Decimal for decimal input
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Get user input from the text fields. These values are in the selected currency.
                    val incomeInput = incomeGoal.toDoubleOrNull()
                    val spendingInput = spendingLimit.toDoubleOrNull()
                    val minSpendingInput = minSpendingGoal.toDoubleOrNull()

                    Log.d(TAG, "Save button clicked. Input values in $selectedCurrency: Income=$incomeInput, Limit=$spendingInput, Min=$minSpendingInput")

                    scope.launch {
                        if (username.isBlank()) return@launch

                        if (incomeInput != null && spendingInput != null) {
                            try {
                                // Step 4: Convert the user's input from the selected currency BACK to ZAR before saving.
                                // This ensures data consistency in Firestore.
                                // In GoalScreen.kt -> Button onClick -> scope.launch -> try block

// after currency conversion ...
                                val incomeToSaveZAR = CurrencyConverter.convert(incomeInput, selectedCurrency, "ZAR")
                                val spendingToSaveZAR = CurrencyConverter.convert(spendingInput, selectedCurrency, "ZAR")
                                val minSpendingToSaveZAR = minSpendingInput?.let { CurrencyConverter.convert(it, selectedCurrency, "ZAR") }

// LOGIC FOR SAVING DATA ---
                                val data = mutableMapOf<String, Any>(
                                    "incomeGoal" to incomeToSaveZAR,
                                    "spendingLimit" to spendingToSaveZAR
                                )

                                if (minSpendingToSaveZAR != null) {
                                    // If there's a value, add it to the map
                                    data["minSpendingGoal"] = minSpendingToSaveZAR
                                } else {
                                    // If there's NO value, explicitly tell Firestore to delete the field
                                    data["minSpendingGoal"] = com.google.firebase.firestore.FieldValue.delete()
                                }

// Use .update() instead of .set() for more precise control with FieldValue.delete()
                                db.collection("users").document(username)
                                    .collection("goals").document("default")
                                    .set(data, com.google.firebase.firestore.SetOptions.merge()) // Use set with merge to handle creation and updates
                                    .await()

                                db.collection("users").document(username).collection("goals").document("default").set(data).await()

                                snackbarHostState.showSnackbar("Goals saved successfully.")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving goals", e)
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

            // The "Current Goals" card now dynamically displays the selected currency.
            if (incomeGoal.isNotEmpty() || spendingLimit.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Income Goal: $selectedCurrency $incomeGoal", color = Color(0xFF388E3C), fontSize = 14.sp)
                        Text("Max Spending: $selectedCurrency $spendingLimit", color = Color(0xFF388E3C), fontSize = 14.sp)
                        if (minSpendingGoal.isNotEmpty()) {
                            Text("Min Spending: $selectedCurrency $minSpendingGoal", color = Color(0xFF388E3C), fontSize = 14.sp)
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