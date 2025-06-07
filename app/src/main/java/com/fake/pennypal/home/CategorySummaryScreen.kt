/*
Title: Lists in Compose (LazyColumn, LazyRow)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/lists
*/

/*
Title: Side-effects in Jetpack Compose (LaunchedEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects#launchedeffect
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
    // Step 1: Get the currently selected currency.
    val selectedCurrency = sessionManager.getSelectedCurrency()
    val db = FirebaseFirestore.getInstance()

    var selectedFilter by remember { mutableStateOf("Monthly") }
    // This state will now hold the category totals ALREADY CONVERTED to the selected currency.
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Step 2: The LaunchedEffect now re-runs if the filter OR the currency changes.
    LaunchedEffect(selectedFilter, selectedCurrency) {
        if (username.isEmpty()) return@LaunchedEffect
        Log.d(TAG, "LaunchedEffect triggered. Filter: $selectedFilter, Currency: $selectedCurrency")

        val (start, end) = getDateRange(selectedFilter)

        val expenses = db.collection("users").document(username)
            .collection("expenses")
            .get()
            .await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
                parsedDate != null && parsedDate in start..end
            }

        // First, group and sum the expenses in their base currency (ZAR).
        val totalsInZAR = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Step 3: Convert the ZAR totals to the selected currency before updating the state.
        categoryTotals = totalsInZAR.mapValues { (_, totalZAR) ->
            CurrencyConverter.convert(totalZAR, "ZAR", selectedCurrency)
        }
        Log.d(TAG, "Converted category totals for display: $categoryTotals")
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
                            // Step 4: Display the dynamic currency symbol and the converted total.
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
