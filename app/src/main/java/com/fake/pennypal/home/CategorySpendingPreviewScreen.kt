/*
Title: Side-effects in Jetpack Compose (LaunchedEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects#launchedeffect
*/

/*
Title: State and Jetpack Compose (remember, mutableStateOf)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/state
*/

/*
Title: Get data with Cloud Firestore | Kotlin+KTX
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/get-data
*/

/*
Title: Kotlin collection transformations (groupBy, mapValues, sumOf)
Author: JetBrains
Date: 2023
Code version: 1.9
Availability: https://kotlinlang.org/docs/collection-transformations.html
*/

/*
Title: Layouts in Jetpack Compose (Column, Row, Box)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/layouts/basics
*/

/*
Title: Material Components for Compose (Card, Scaffold, Button)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/components
*/

/*
Title: Coil - Image loading for Android (rememberAsyncImagePainter)
Author: Coil Contributors
Date: 2023
Code version: 2.4.0
Availability: https://coil-kt.github.io/coil/compose/
*/

/*
Title: SimpleDateFormat
Author: Oracle
Date: 2023
Code version: Java 11
Availability: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/SimpleDateFormat.html
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import com.fake.pennypal.data.model.Badge
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
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
import com.fake.pennypal.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import com.fake.pennypal.utils.getCurrentUsername
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.CurrencyConverter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Added a TAG for logging, which helps in filtering Logcat messages for this specific screen.
private const val TAG = "CategorySpendingScreen"

@Composable
fun CurrencyDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = CurrencyConverter.getAvailableCurrencies()
    // FIX: Corrected the typo from "mutableState of" to "mutableStateOf"
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelected(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CategorySpendingPreviewScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val username = remember { SessionManager(context).getLoggedInUser() ?: "" }
    val sessionManager = remember { SessionManager(context) }
    var selectedCurrency by remember { mutableStateOf(sessionManager.getSelectedCurrency()) }
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }

    // FIX: Renamed state variables to clarify that they hold values converted to the selected currency.
    // This avoids confusion with the original ZAR values fetched from Firestore.
    var minGoalConverted by remember { mutableStateOf(0.0) }
    var maxGoalConverted by remember { mutableStateOf(20000.0) }

    var recentExpensesGrouped by remember { mutableStateOf<Map<String, List<Expense>>>(emptyMap()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // This LaunchedEffect will re-run whenever the 'selectedFilter' or 'selectedCurrency' changes.
    // It's the correct place for fetching and processing data based on these user-selected states.
    LaunchedEffect(selectedFilter, selectedCurrency) {
        if (username.isEmpty()) return@LaunchedEffect

        Log.d(TAG, "LaunchedEffect triggered. Filter: $selectedFilter, Currency: $selectedCurrency")

        val (start, end) = getDateRange(selectedFilter)

        val expenses = db.collection("users").document(username).collection("expenses").get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
                parsedDate != null && parsedDate in start..end
            }

        val recentExpensesByCategory = expenses.groupBy { it.category }
            .mapValues { it.value.sortedByDescending { it.date }.take(3) }
        recentExpensesGrouped = recentExpensesByCategory

        // Group expenses by category and sum their amounts. This sum is initially in the base currency (ZAR).
        val groupedInZAR = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Convert the ZAR totals to the user's selected currency.
        val converted = groupedInZAR.mapValues { (_, amount) ->
            CurrencyConverter.convert(amount, "ZAR", selectedCurrency)
        }
        categoryTotals = converted
        val totalSpent = converted.values.sum()
        Log.d(TAG, "Total spent in $selectedCurrency: $totalSpent")

        // Fetch goals from Firestore. These are stored in the base currency (ZAR).
        val goals = db.collection("users").document(username)
            .collection("goals").document("default").get().await().data

        val minGoalZAR: Double
        val maxGoalZAR: Double

        if (goals != null) {
            minGoalZAR = (goals["minSpendingGoal"] as? Number)?.toDouble() ?: 0.0
            maxGoalZAR = (goals["spendingLimit"] as? Number)?.toDouble() ?: 20000.0
        } else {
            minGoalZAR = 0.0
            maxGoalZAR = 20000.0
        }
        Log.d(TAG, "Fetched Goals in ZAR -> Min: $minGoalZAR, Max: $maxGoalZAR")

        // FIX: Convert the ZAR goal values to the selected currency before using them in calculations.
        // This ensures all comparisons and calculations are done in the same currency.
        minGoalConverted = CurrencyConverter.convert(minGoalZAR, "ZAR", selectedCurrency)
        maxGoalConverted = CurrencyConverter.convert(maxGoalZAR, "ZAR", selectedCurrency)
        Log.d(TAG, "Converted Goals in $selectedCurrency -> Min: $minGoalConverted, Max: $maxGoalConverted")


        val badgeCollection = db.collection("users").document(username).collection("badges")
        // Note: Badge logic should ideally use the base currency values to remain consistent,
        // as the goals are stored in ZAR in the database.
        val totalSpentZAR = groupedInZAR.values.sum()
        if (totalSpentZAR <= maxGoalZAR) {
            val badge = hashMapOf(
                "title" to "Budget Keeper",
                "description" to "You stayed under your spending limit!",
                "earnedDate" to formatter.format(Date())
            )
            // badgeCollection.add(badge) // Uncomment to enable badge creation
        }

        val balanceZAR = maxGoalZAR - totalSpentZAR
        if (balanceZAR >= minGoalZAR) {
            val badge = hashMapOf(
                "title" to "Savings Star",
                "description" to "You saved more than your minimum goal!",
                "earnedDate" to formatter.format(Date())
            )
            // badgeCollection.add(badge) // Uncomment to enable badge creation
        }
    }

    val totalSpent = categoryTotals.values.sum()
    // FIX: Calculate goal progress using the CONVERTED max goal value.
    val goalProgress = if (maxGoalConverted > 0) (totalSpent / maxGoalConverted).coerceIn(0.0, 1.0) else 0.0

    LaunchedEffect(goalProgress) {
        if (goalProgress < 1.0) {
            val badge = com.fake.pennypal.data.model.Badge(
                title = "Budget Master!",
                description = "You stayed under your monthly spending goal.",
                earnedDate = formatter.format(Date())
            )
//            db.collection("users").document(username)
//                .collection("badges")
//                .document("badge_budget_master")
//                .set(badge) // Uncomment to enable badge creation
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Spending vs Goal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFFFFDE7),
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurrencyDropdown(selectedCurrency) { newCurrency ->
                selectedCurrency = newCurrency
                sessionManager.setSelectedCurrency(newCurrency)
            }

            // FIX: Calculate the balance using the CONVERTED max goal value.
            val balance = maxGoalConverted - totalSpent
            val hasExpenses = totalSpent > 0

            if (hasExpenses) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Balance", fontSize = 14.sp)
                        Text("$selectedCurrency ${"%.2f".format(balance)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Expense: $selectedCurrency ${"%.2f".format(totalSpent)}", color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Goal Progress")
                        LinearProgressIndicator(
                            progress = goalProgress.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF4CAF50)
                        )
                        Text("${(goalProgress * 100).toInt()}% of your spending goal")
                    }
                }
            } else {
                Text("No expenses available for this period.", color = Color.Gray, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(listOf("Daily", "Weekly", "Monthly", "Yearly")) { label ->
                    Button(
                        onClick = { selectedFilter = label },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == label) Color(0xFFFFEB3B) else Color.LightGray
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text(label, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FIX: Pass the CONVERTED goal values to the BarChart composable.
            BarChartWithGoals(categoryTotals, minGoalConverted, maxGoalConverted, selectedCurrency)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Expenses", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            recentExpensesGrouped.forEach { (category, items) ->
                Text(category, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))

                items.forEach { expense ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // The individual expense amount is correctly converted on the fly below.
                                navController.navigate(
                                    "expenseDetail/${expense.date}/${expense.amount}/${expense.category}/${expense.description}/${expense.photoUrl}/${selectedCurrency}"
                                )
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date: ${expense.date}", fontSize = 12.sp)
                            val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", selectedCurrency)
                            Text("Amount: $selectedCurrency ${"%.2f".format(convertedAmount)}", fontSize = 12.sp)
                            Text("Description: ${expense.description}", fontSize = 12.sp)

                            if (!expense.photoUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    painter = rememberAsyncImagePainter(model = expense.photoUrl),
                                    contentDescription = "Expense Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Final bottom padding
        }
    }
}


@Composable
fun BarChartWithGoals(
    categoryTotals: Map<String, Double>,
    minGoal: Double,
    maxGoal: Double,
    currency: String
) {
    // All values passed into this composable (categoryTotals, minGoal, maxGoal) are now in the same currency.
    val maxAmount = (categoryTotals.values.maxOrNull() ?: 1.0).coerceAtLeast(maxGoal)
    val barColor = Color(0xFFAED581) // Pastel green
    val backgroundColor = Color(0xFFF3FCEB) // Light card background

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Income & Expenses",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF2E7D32) // Dark green title
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Draw horizontal axis lines (optional styling)
                repeat(4) { i ->
                    val y = 40.dp * i
                    Divider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .offset(y = y)
                    )
                }

                // Bars
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    categoryTotals.entries.forEach { (category, amount) ->
                        val heightRatio = (amount / maxAmount).coerceIn(0.0, 1.0)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(140.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height((heightRatio * 100).dp)
                                    .background(barColor, shape = RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(category.take(3), fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            // Optional: goal line info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Min: $currency ${"%.0f".format(minGoal)}", fontSize = 12.sp, color = Color(0xFF388E3C))
                Text("Max: $currency ${"%.0f".format(maxGoal)}", fontSize = 12.sp, color = Color.Red)
            }
        }
    }
}

