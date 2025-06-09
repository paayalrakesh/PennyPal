/*
Title: Get real-time updates with Cloud Firestore
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
*/

/*
Title: Side-effects in Jetpack Compose (DisposableEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects#disposableeffect
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.CurrencyConverter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

private const val TAG = "HomeScreen"

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val username = sessionManager.getLoggedInUser() ?: return
    val selectedCurrency = sessionManager.getSelectedCurrency()

    var incomeList by remember { mutableStateOf(emptyList<Income>()) }
    var expenseList by remember { mutableStateOf(emptyList<Expense>()) }

    // This effect will run when the screen is first composed and will clean up when it's disposed.
    DisposableEffect(username) {
        if (username.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        val db = FirebaseFirestore.getInstance()

        // Listener for Incomes
        val incomeListener = db.collection("users").document(username).collection("incomes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Income listener failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val incomes = snapshot.documents.mapNotNull { it.toObject(Income::class.java) }
                    incomeList = incomes
                    Log.d(TAG, "Income data updated in real-time. Count: ${incomes.size}")
                }
            }

        // Listener for Expenses
        // In HomeScreen's DisposableEffect
        val expenseListener = db.collection("users").document(username).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                // ...
                if (snapshot != null) {
                    // --- APPLY THE FIX HERE ---
                    val expenses = snapshot.documents.mapNotNull { document ->
                        document.toObject(Expense::class.java)?.apply {
                            this.documentId = document.id // Manually set the unique ID
                        }
                    }
                    expenseList = expenses
                    // ...
                }
            }

        // onDispose is the cleanup block. It's called when the composable leaves the screen.
        // We remove the listeners here to prevent memory leaks and unnecessary background work.
        onDispose {
            incomeListener.remove()
            expenseListener.remove()
        }
    }

    val totalIncomeZAR = incomeList.sumOf { it.amount }
    val totalExpensesZAR = expenseList.sumOf { it.amount }
    val balanceZAR = totalIncomeZAR - totalExpensesZAR

    val totalIncome = CurrencyConverter.convert(totalIncomeZAR, "ZAR", selectedCurrency)
    val totalExpenses = CurrencyConverter.convert(totalExpensesZAR, "ZAR", selectedCurrency)
    val balance = CurrencyConverter.convert(balanceZAR, "ZAR", selectedCurrency)

    val progress = if (totalIncome > 0) (totalExpenses / totalIncome).toFloat().coerceIn(0f, 1f) else 0f

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { navController.navigate("home") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Home, contentDescription = "Home") }
                    IconButton(onClick = { navController.navigate("categorySpendingPreview") }) { Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph") }
                    IconButton(onClick = { navController.navigate("manageCategories") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.List, contentDescription = "Categories") }
                    IconButton(onClick = { navController.navigate("addChoice") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, contentDescription = "Add") }
                    IconButton(onClick = { navController.navigate("goals") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Star, contentDescription = "Goals") }
                    IconButton(onClick = { navController.navigate("profile") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Person, contentDescription = "Profile") }
                }
            }
        }
    ) { paddingValues ->
        // The rest of your UI remains exactly the same.
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFFF1F8E9)).padding(16.dp)
        ) {
            Text(text = "Hi, Welcome Back", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Total Balance: $selectedCurrency ${"%.2f".format(balance)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "Total Income: $selectedCurrency ${"%.2f".format(totalIncome)}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                    Text(text = "Total Expenses: -$selectedCurrency ${"%.2f".format(totalExpenses)}", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Red)
                    LinearProgressIndicator(progress = progress, color = Color(0xFF388E3C), trackColor = Color.LightGray, modifier = Modifier.fillMaxWidth().height(8.dp))
                    Text(text = "${(progress * 100).toInt()}% of your income spent", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            expenseList.forEach { expense ->
                val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", selectedCurrency)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(expense.category, fontWeight = FontWeight.Bold)
                            Text(expense.description, fontSize = 12.sp)
                        }
                        Text(text = "$selectedCurrency ${"%.2f".format(convertedAmount)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}