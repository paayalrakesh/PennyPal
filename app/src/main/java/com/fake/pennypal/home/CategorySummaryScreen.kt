/*
Title: Lists in Compose (LazyColumn, LazyRow)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/lists
*/

/*
Title: Side-effects in Jetpack Compose (LaunchedEffect, DisposableEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects
*/

/*
Title: Get real-time updates with Cloud Firestore
Description: Official Firebase documentation explaining how to use addSnapshotListener to listen for data changes in real time.
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
*/

/*
Title: Map custom objects (Firebase)
Description: Documentation explaining how to map Firestore documents to custom Kotlin data classes using toObject().
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/get-data#custom_objects
*/

/*
Title: Kotlin collection transformations (groupBy, mapValues, sumOf)
Author: JetBrains
Date: 2023
Code version: 1.9
Availability: https://kotlinlang.org/docs/collection-transformations.html
*/


@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.utils.CurrencyConverter
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Added a TAG for logging, which helps in filtering Logcat messages for this specific screen.
private const val TAG = "CategorySummaryScreen"

@Composable
fun CategorySummaryScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val username = sessionManager.getLoggedInUser() ?: ""
    val selectedCurrency = sessionManager.getSelectedCurrency()
    val db = FirebaseFirestore.getInstance()

    var selectedFilter by remember { mutableStateOf("Monthly") }
    // This state will hold the category totals ALREADY CONVERTED to the selected currency.
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }

    // NEW: State to hold the raw list of all expenses fetched by the real-time listener.
    var allExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // NEW: A DisposableEffect sets up a real-time listener for expenses.
    // It runs when the screen is first composed and cleans up (removes the listener) when the screen is left.
    // This ensures we always have the latest expense data without causing memory leaks.
    // Ref: https://developer.android.com/jetpack/compose/side-effects#disposableeffect
    DisposableEffect(username) {
        if (username.isEmpty()) {
            onDispose { } // Do nothing if there's no user.
        }

        Log.d(TAG, "Setting up real-time expense listener for user: $username")
        // Ref: https://firebase.google.com/docs/firestore/query-data/listen
        val listener = db.collection("users").document(username).collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Expense listener failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // When new data arrives, map it to Expense objects and update our state.
                    // Ref: https://firebase.google.com/docs/firestore/query-data/get-data#custom_objects
                    allExpenses = snapshot.toObjects(Expense::class.java)
                    Log.d(TAG, "Real-time expense data received. Count: ${allExpenses.size}")
                }
            }

        // The onDispose block is crucial for cleanup.
        onDispose {
            Log.d(TAG, "Removing expense listener.")
            listener.remove()
        }
    }

    // MODIFIED: This LaunchedEffect now re-runs whenever the raw data (allExpenses) changes,
    // or when the user changes the filter or currency. Its job is to process the data, not fetch it.
    LaunchedEffect(allExpenses, selectedFilter, selectedCurrency) {
        Log.d(TAG, "LaunchedEffect triggered for processing. Filter: $selectedFilter, Currency: $selectedCurrency, Expense Count: ${allExpenses.size}")

        val (start, end) = getDateRange(selectedFilter)

        // Filter the live list of expenses based on the selected date range.
        val expensesInPeriod = allExpenses.filter {
            val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
            parsedDate != null && parsedDate in start..end
        }

        // First, group and sum the filtered expenses in their base currency (ZAR).
        // Ref: https://kotlinlang.org/docs/collection-transformations.html
        val totalsInZAR = expensesInPeriod.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Convert the ZAR totals to the selected currency before updating the state for display.
        categoryTotals = totalsInZAR.mapValues { (_, totalZAR) ->
            CurrencyConverter.convert(totalZAR, "ZAR", selectedCurrency)
        }
        Log.d(TAG, "Processed and converted category totals for display: $categoryTotals")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Category Spending Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                FilterRow(selected = selectedFilter, onFilterSelected = { selectedFilter = it })
            }

            if (categoryTotals.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No expenses found for this period.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                items(categoryTotals.entries.toList()) { (category, total) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            // Display the dynamic currency symbol and the converted total.
                            Text(
                                text = "$selectedCurrency${"%.2f".format(total)}",
                                fontSize = 18.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A reusable composable for the filter button row.
 * @param selected The currently selected filter string.
 * @param onFilterSelected Callback invoked when a filter button is clicked.
 */
@Composable
fun FilterRow(selected: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Daily", "Weekly", "Monthly", "Yearly")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(filters) { filter ->
            Button(
                onClick = { onFilterSelected(filter) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filter == selected) Color(0xFFFFEB3B) else Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(100.dp)
            ) {
                Text(filter, color = Color.Black, fontSize = 12.sp)
            }
        }
    }
}