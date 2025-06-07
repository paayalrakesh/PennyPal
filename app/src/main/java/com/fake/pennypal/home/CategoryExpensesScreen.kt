/*
Title: Lists in Compose (LazyColumn, LazyRow)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/lists
*/

/*
Title: Coil - Image loading for Android (rememberAsyncImagePainter)
Author: Coil Contributors
Date: 2023
Code version: 2.4.0
Availability: https://coil-kt.github.io/coil/compose/
*/

/*
Title: Basic layouts in Compose (Image Composable)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/layouts/basics#image
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

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.utils.CurrencyConverter
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Added a TAG for logging, which helps in filtering Logcat messages for this specific screen.
private const val TAG = "CategoryExpensesScreen"

/**
 * Displays a detailed list of expenses for a specific category.
 * Allows filtering by a date range (Daily, Weekly, etc.).
 * @param navController The NavController for navigation.
 * @param categoryName The name of the category to display expenses for.
 */
@Composable
fun CategoryExpensesScreen(navController: NavController, categoryName: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val selectedCurrency = sessionManager.getSelectedCurrency()
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // This side-effect fetches expenses from Firestore whenever the filter or category changes.
    LaunchedEffect(selectedFilter, categoryName) {
        val (start, end) = getDateRange(selectedFilter)
        val username = SessionManager(context).getLoggedInUser() ?: return@LaunchedEffect
        Log.d(TAG, "Fetching expenses for category '$categoryName' with filter '$selectedFilter'")

        val fetchedExpenses = db.collection("users").document(username)
            .collection("expenses")
            .whereEqualTo("category", categoryName) // Query by category
            .get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter { // Filter by date range locally
                val date = try { formatter.parse(it.date) } catch (e: Exception) { null }
                date != null && date >= start && date <= end
            }

        expenses = fetchedExpenses
        Log.d(TAG, "Found ${fetchedExpenses.size} expenses.")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Expenses: $categoryName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { padding ->
        // LazyColumn is efficient for displaying long, scrollable lists.
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp, bottom = 100.dp) // Padding at the bottom
        ) {
            // The filter row is the first item in the list.
            item {
                CFilterRow(selected = selectedFilter, onFilterSelected = { selectedFilter = it })
            }

            // Display a message if no expenses are found.
            if (expenses.isEmpty()) {
                item {
                    Text(
                        "No expenses found for this category and period.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                // Display the list of expense cards.
                items(expenses) { expense ->
                    ExpenseCard(expense, selectedCurrency)
                }
            }
        }
    }
}

/**
 * A card composable that displays the details of a single expense,
 * including its date, amount, description, and an optional photo.
 * @param expense The Expense data object to display.
 * @param selectedCurrency The currency to display the amount in.
 */
@Composable
fun ExpenseCard(expense: Expense, selectedCurrency: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Date: ${expense.date}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", selectedCurrency)
            Text("Amount: $selectedCurrency ${"%.2f".format(convertedAmount)}", fontSize = 14.sp)
            Text("Description: ${expense.description}", fontSize = 14.sp)

            // Check if the photoUrl from Firestore is valid before attempting to display it.
            if (!expense.photoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Log.d(TAG, "Loading image from URL: ${expense.photoUrl}")
                // The Image composable uses Coil's painter to asynchronously load the image from the URL.
                // Passing the string URL directly to the model is the most robust way.
                Image(
                    painter = rememberAsyncImagePainter(model = expense.photoUrl),
                    contentDescription = "Expense Photo for ${expense.description}",
                    contentScale = ContentScale.Crop, // Ensures the image fills the container without stretching.
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

/**
 * A horizontal row of buttons for selecting a date range filter.
 * @param selected The currently selected filter string.
 * @param onFilterSelected A callback function to be invoked when a new filter is chosen.
 */
@Composable
fun CFilterRow(selected: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Daily", "Weekly", "Monthly", "Yearly")
    // LazyRow is efficient for horizontal lists.
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

/**
 * A helper function to calculate a start and end date based on a filter string.
 * @param filter The string representing the desired period ("Daily", "Weekly", etc.).
 * @return A Pair of Date objects representing the start and end of the period.
 */
fun getDateRange(filter: String): Pair<Date, Date> {
    val now = Calendar.getInstance()
    val end = now.time
    val start = Calendar.getInstance()
    when (filter) {
        "Daily" -> start.add(Calendar.DAY_OF_YEAR, -1)
        "Weekly" -> start.add(Calendar.DAY_OF_YEAR, -7)
        "Monthly" -> start.add(Calendar.MONTH, -1)
        "Yearly" -> start.add(Calendar.YEAR, -1)
    }
    return start.time to end
}