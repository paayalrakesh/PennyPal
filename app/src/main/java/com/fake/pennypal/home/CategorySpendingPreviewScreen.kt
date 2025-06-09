/*
Title: Get real-time updates with Cloud Firestore
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
*/

/*
Title: Side-effects in Jetpack Compose (DisposableEffect, LaunchedEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects
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
import com.fake.pennypal.data.model.Income
import com.google.firebase.firestore.FirebaseFirestore
import com.fake.pennypal.utils.getCurrentUsername
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.getDateRange
import com.fake.pennypal.utils.CurrencyConverter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CategorySpendingScreen"

@Composable
fun CurrencyDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = CurrencyConverter.getAvailableCurrencies()
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onSelected(currency)
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

    // State for calculated data to be displayed in the UI
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }
    var recentExpensesGrouped by remember { mutableStateOf<Map<String, List<Expense>>>(emptyMap()) }
    var trueBalance by remember { mutableStateOf(0.0) }
    var totalSpentInPeriod by remember { mutableStateOf(0.0) }
    var goalProgress by remember { mutableStateOf(0.0) }

    var totalAllTimeIncome by remember { mutableStateOf(0.0) }
    var totalAllTimeExpenses by remember { mutableStateOf(0.0) }

    // CHANGE: Create state variables to hold the converted goal values
    var minSpendingGoal by remember { mutableStateOf(0.0) }
    var maxSpendingGoal by remember { mutableStateOf(0.0) }

    // State for raw data from Firestore real-time listeners
    var allExpenses by remember { mutableStateOf(listOf<Expense>()) }
    var allIncomes by remember { mutableStateOf(listOf<Income>()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // EFFECT 1: REAL-TIME LISTENERS
    DisposableEffect(username) {
        if (username.isBlank()) { onDispose {} }

        Log.d(TAG, "Setting up real-time listeners for user: $username")
        val expensesListener = db.collection("users").document(username).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                // ...
                if (snapshot != null) {
                    // --- APPLY THE FIX HERE ---
                    allExpenses = snapshot.documents.mapNotNull { document ->
                        document.toObject(Expense::class.java)?.apply {
                            this.documentId = document.id // Manually set the unique ID
                        }
                    }
                }
            }

        val incomesListener = db.collection("users").document(username).collection("incomes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Income listener failed.", error); return@addSnapshotListener }
                if (snapshot != null) {
                    allIncomes = snapshot.documents.mapNotNull { it.toObject(Income::class.java) }
                }
            }

        onDispose {
            Log.d(TAG, "Removing listeners.")
            expensesListener.remove()
            incomesListener.remove()
        }
    }

    // EFFECT 2: DATA PROCESSOR, CALCULATOR, AND BADGE AWARDER
    LaunchedEffect(allExpenses, allIncomes, selectedFilter, selectedCurrency, username) {
        if (username.isBlank()) return@LaunchedEffect

        Log.d(TAG, "Processing data and checking goals...")

        // Calculate True Account Balance (All-Time)
        val totalIncomeZAR = allIncomes.sumOf { it.amount }
        val totalExpensesZAR = allExpenses.sumOf { it.amount }
        val trueBalanceZAR = totalIncomeZAR - totalExpensesZAR

        trueBalance = CurrencyConverter.convert(trueBalanceZAR, "ZAR", selectedCurrency)
        totalAllTimeIncome = CurrencyConverter.convert(totalIncomeZAR, "ZAR", selectedCurrency)
        totalAllTimeExpenses = CurrencyConverter.convert(totalExpensesZAR, "ZAR", selectedCurrency)

        // Filter Expenses for the selected period
        val (start, end) = getDateRange(selectedFilter)
        val filteredExpenses = allExpenses.filter {
            val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
            parsedDate != null && parsedDate in start..end
        }
        recentExpensesGrouped = filteredExpenses.groupBy { it.category }
            .mapValues { it.value.sortedByDescending { exp -> exp.date }.take(3) }

        // Calculate Totals for the selected period
        val totalsInPeriodZAR = filteredExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { exp -> exp.amount } }
        categoryTotals = totalsInPeriodZAR.mapValues { (_, total) ->
            CurrencyConverter.convert(total, "ZAR", selectedCurrency)
        }
        totalSpentInPeriod = categoryTotals.values.sum()

        // Fetch Goals and Check for Badges
        try {
            val goalsDoc = db.collection("users").document(username).collection("goals").document("default").get().await()
            val minGoalZAR = goalsDoc.getDouble("minSpendingGoal") ?: 0.0
            val maxGoalZAR = goalsDoc.getDouble("spendingLimit") ?: 0.0

            // CHANGE: Update the state variables with the converted goal values
            minSpendingGoal = CurrencyConverter.convert(minGoalZAR, "ZAR", selectedCurrency)
            maxSpendingGoal = CurrencyConverter.convert(maxGoalZAR, "ZAR", selectedCurrency)

            goalProgress = if (maxSpendingGoal > 0) (totalSpentInPeriod / maxSpendingGoal).coerceIn(0.0, 1.0) else 0.0

            // BADGE AWARDING LOGIC
            val totalSpentInPeriodZAR = totalsInPeriodZAR.values.sum()
            val badgeCollection = db.collection("users").document(username).collection("badges")
            val yearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            if (totalSpentInPeriodZAR <= maxGoalZAR && maxGoalZAR > 0) {
                val badgeId = "budget_keeper_$yearMonth"
                val badgeRef = badgeCollection.document(badgeId)
                if (badgeRef.get().await().exists().not()) {
                    badgeCollection.document(badgeId).set(Badge("Budget Keeper!", "You stayed under your spending limit for the month.", formatter.format(Date()))).await()
                    Log.i(TAG, "Awarded new badge: $badgeId")
                }
            }

            val balanceInPeriodZAR = maxGoalZAR - totalSpentInPeriodZAR
            if (balanceInPeriodZAR >= minGoalZAR && minGoalZAR > 0) {
                val badgeId = "savings_star_$yearMonth"
                val badgeRef = badgeCollection.document(badgeId)
                if (badgeRef.get().await().exists().not()) {
                    badgeCollection.document(badgeId).set(Badge("Savings Star!", "You saved more than your minimum savings goal for the month.", formatter.format(Date()))).await()
                    Log.i(TAG, "Awarded new badge: $badgeId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching goals or awarding badges", e)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Spending vs Goal") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } })
        },
        containerColor = Color(0xFFFFFDE7),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { navController.navigate("home") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Home, contentDescription = "Home") }
                    IconButton(onClick = { navController.navigate("categorySpendingPreview") }) { Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph") }
                    IconButton(onClick = { navController.navigate("manageCategories") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.List, contentDescription = "Categories") }
                    IconButton(onClick = { navController.navigate("addChoice") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, contentDescription = "Add") }
                    IconButton(onClick = { navController.navigate("goals") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Star, contentDescription = "Goals") }
                    IconButton(onClick = { navController.navigate("profile") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Person, contentDescription = "Profile") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurrencyDropdown(selectedCurrency) { newCurrency ->
                selectedCurrency = newCurrency
                sessionManager.setSelectedCurrency(newCurrency)
            }

            val hasTransactions = allIncomes.isNotEmpty() || allExpenses.isNotEmpty()

            if (hasTransactions) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- All-Time Summary (Consistent with HomeScreen) ---
                        Text(
                            text = "Total Balance: $selectedCurrency ${"%.2f".format(trueBalance)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Total Income: $selectedCurrency ${"%.2f".format(totalAllTimeIncome)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Total Expenses: -$selectedCurrency ${"%.2f".format(totalAllTimeExpenses)}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.Red
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // --- Period-Specific Summary ---
                        Text(
                            text = "${selectedFilter} Expense: $selectedCurrency ${"%.2f".format(totalSpentInPeriod)}",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Goal Progress for ${selectedFilter.lowercase()}")
                        LinearProgressIndicator(
                            progress = goalProgress.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color.LightGray
                        )
                        Text(
                            text = "${(goalProgress * 100).toInt()}% of your spending goal",
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Text("No transactions found.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(listOf("Daily", "Weekly", "Monthly", "Yearly")) { label ->
                    Button(onClick = { selectedFilter = label }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedFilter == label) Color(0xFFFFEB3B) else Color.LightGray), shape = RoundedCornerShape(16.dp), modifier = Modifier.width(100.dp)) {
                        Text(label, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // CHANGE: Pass the state variables to the bar chart
            BarChartWithGoals(categoryTotals, minSpendingGoal, maxSpendingGoal, selectedCurrency)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Expenses", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            recentExpensesGrouped.forEach { (category, items) ->
                Text(category, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                items.forEach { expense ->
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { /* ... */ }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Date: ${expense.date}", fontSize = 12.sp)
                            val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", selectedCurrency)
                            Text("Amount: $selectedCurrency ${"%.2f".format(convertedAmount)}", fontSize = 12.sp)
                            Text("Description: ${expense.description}", fontSize = 12.sp)
                            if (!expense.photoUrl.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(painter = rememberAsyncImagePainter(model = expense.photoUrl), contentDescription = "Expense Image", modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray, shape = RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
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
    // Safely calculate maxAmount and prevent it from being zero
    val maxAmount = (categoryTotals.values.maxOrNull() ?: 0.0)
        .coerceAtLeast(maxGoal)
        .coerceAtLeast(1.0) // Ensure the divisor is at least 1.0 to prevent division by zero
    val barColor = Color(0xFFAED581)
    val backgroundColor = Color(0xFFF3FCEB)

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
                text = "Spending by Category", // Changed title for clarity
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF2E7D32)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Background grid lines
                repeat(4) { i ->
                    val y = 40.dp * i
                    Divider(
                        color = Color.LightGray.copy(alpha = 0.5f), // Made them fainter
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
                        // Calculate height as a fraction of the total available height (140.dp)
                        val heightRatio = (amount / maxAmount).toFloat().coerceIn(0.0f, 1.0f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(140.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp) // Made bars slightly wider
                                    .fillMaxHeight(heightRatio) // Use fillMaxHeight for proportional scaling
                                    .background(barColor, shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = category.take(3).uppercase(), // Abbreviate category name
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
            // Goal labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Min: $currency ${"%.0f".format(minGoal)}",
                    fontSize = 12.sp,
                    color = Color(0xFF388E3C)
                )
                Text(
                    "Max: $currency ${"%.0f".format(maxGoal)}",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
        }
    }
}