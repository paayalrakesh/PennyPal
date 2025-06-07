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
Title: Side-effects in Jetpack Compose (LaunchedEffect, DisposableEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects
*/

/*
Title: Get real-time updates with Cloud Firestore
Description: Official Firebase documentation explaining how to use addSnapshotListener to listen for data changes in real time, including with queries.
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
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
    val username = sessionManager.getLoggedInUser() ?: ""

    var selectedFilter by remember { mutableStateOf("Monthly") }
    // This state will hold the final, filtered list of expenses to be displayed in the UI.
    var expenses by remember { mutableStateOf(listOf<Expense>()) }

    // NEW: This state holds the raw, unfiltered list of expenses for the category from the real-time listener.
    var allCategoryExpenses by remember { mutableStateOf(listOf<Expense>()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // NEW: A DisposableEffect sets up a real-time listener for expenses in the specific category.
    // It attaches when the screen appears and detaches when it's left, preventing memory leaks.
    // Ref: https://developer.android.com/jetpack/compose/side-effects#disposableeffect
    DisposableEffect(username, categoryName) {
        if (username.isEmpty()) {
            onDispose { } // Do nothing if there's no user.
        }

        Log.d(TAG, "Setting up real-time listener for category: '$categoryName'")

        // The listener queries for expenses matching the category and listens for any changes.
        // Ref: https://firebase.google.com/docs/firestore/query-data/listen
        val listener = db.collection("users").document(username)
            .collection("expenses")
            .whereEqualTo("category", categoryName) // Query by category
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listener for category '$categoryName' failed.", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // When new data arrives, update the raw list of expenses.
                    allCategoryExpenses = snapshot.toObjects(Expense::class.java)
                    Log.d(TAG, "Live data received. Found ${allCategoryExpenses.size} total expenses for category '$categoryName'.")
                }
            }

        // The onDispose block is crucial for cleanup when the screen is navigated away from.
        onDispose {
            Log.d(TAG, "Removing listener for category: '$categoryName'")
            listener.remove()
        }
    }

    // MODIFIED: This side-effect now processes the data from the listener instead of fetching it.
    // It runs whenever the raw data (allCategoryExpenses) or the filter changes.
    LaunchedEffect(allCategoryExpenses, selectedFilter) {
        Log.d(TAG, "Processing ${allCategoryExpenses.size} expenses with filter '$selectedFilter'")
        val (start, end) = getDateRange(selectedFilter)

        // Filter the live list of expenses by date range locally.
        val filteredExpenses = allCategoryExpenses.filter {
            val date = try { formatter.parse(it.date) } catch (e: Exception) { null }
            date != null && date >= start && date <= end
        }

        expenses = filteredExpenses
        Log.d(TAG, "UI updated with ${expenses.size} filtered expenses.")
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
                // CORRECTED: The function name is rememberAsyncImagePainter (with a capital P).
                // The Image composable uses Coil's painter to asynchronously load the image from the URL.
                // Ref: https://coil-kt.github.io/coil/compose/
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
 * This version sets the time to the very start and end of the day for accurate filtering.
 * @param filter The string representing the desired period ("Daily", "Weekly", etc.).
 * @return A Pair of Date objects representing the start and end of the period.
 */
fun getDateRange(filter: String): Pair<Date, Date> {
    val now = Calendar.getInstance()
    // Clone 'now' for the end date to avoid modification, and set time to end of day.
    val end = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time

    val start = Calendar.getInstance()
    when (filter) {
        "Daily" -> start.add(Calendar.DAY_OF_YEAR, 0) // Just today
        "Weekly" -> start.add(Calendar.DAY_OF_YEAR, -6) // Last 7 days including today
        "Monthly" -> start.add(Calendar.MONTH, -1)
        "Yearly" -> start.add(Calendar.YEAR, -1)
    }
    // Set start time to beginning of the day for accurate filtering.
    start.set(Calendar.HOUR_OF_DAY, 0)
    start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0)

    return start.time to end
}