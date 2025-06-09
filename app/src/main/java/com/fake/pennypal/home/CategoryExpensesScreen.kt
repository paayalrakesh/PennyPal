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
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CategoryExpensesScreen"

@Composable
fun CategoryExpensesScreen(
    navController: NavController,
    categoryName: String
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val username = sessionManager.getLoggedInUser() ?: ""
    val selectedCurrency = sessionManager.getSelectedCurrency()

    var selectedFilter by remember { mutableStateOf("Monthly") }
    var allCategoryExpenses by remember { mutableStateOf(listOf<Expense>()) }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    DisposableEffect(username, categoryName) {
        if (username.isBlank()) return@DisposableEffect onDispose { }

        val listener = db.collection("users")
            .document(username)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore listener failed", error)
                    return@addSnapshotListener
                }
                val newExpenses = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject(Expense::class.java)
                    item?.takeIf {
                        it.category.trim().equals(categoryName.trim(), ignoreCase = true)
                    }?.apply {
                        documentId = doc.id
                    }
                }.orEmpty()
                allCategoryExpenses = newExpenses
                Log.d(TAG, "Real-time expense list updated. Count: ${newExpenses.size}")
            }

        onDispose {
            Log.d(TAG, "Listener removed for category $categoryName")
            listener.remove()
        }
    }

    LaunchedEffect(allCategoryExpenses, selectedFilter) {
        val (start, end) = getDateRange(selectedFilter)
        expenses = allCategoryExpenses.filter {
            try {
                val parsed = formatter.parse(it.date)
                parsed != null && parsed >= start && parsed <= end
            } catch (e: Exception) {
                false
            }
        }
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                cFilterRow(selectedFilter) { selectedFilter = it }
            }

            if (expenses.isEmpty()) {
                item {
                    Text("No expenses found for this category and period.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(12.dp))
                }
            } else {
                items(expenses, key = { it.documentId }) { expense ->
                    ExpenseCard(expense, selectedCurrency)
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense, currency: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Date: ${expense.date}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", currency)
            Text("Amount: $currency ${"%.2f".format(convertedAmount)}", fontSize = 14.sp)
            Text("Description: ${expense.description}", fontSize = 14.sp)
            if (!expense.photoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(model = expense.photoUrl),
                    contentDescription = "Expense Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun cFilterRow(selected: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Daily", "Weekly", "Monthly", "Yearly")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.fillMaxWidth()
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
                Text(filter, color = Color.Black)
            }
        }
    }
}

fun getDateRange(filter: String): Pair<Date, Date> {
    val now = Calendar.getInstance()
    val end = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time

    val start = Calendar.getInstance().apply {
        when (filter) {
            "Daily" -> add(Calendar.DAY_OF_YEAR, 0)
            "Weekly" -> add(Calendar.DAY_OF_YEAR, -6)
            "Monthly" -> add(Calendar.MONTH, -1)
            "Yearly" -> add(Calendar.YEAR, -1)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    return start.time to end
}
