@file:OptIn(ExperimentalMaterial3Api::class)

package com.fake.pennypal.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun CategoryExpensesScreen(navController: NavController, categoryName: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val selectedCurrency = sessionManager.getSelectedCurrency()
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    LaunchedEffect(selectedFilter, categoryName) {
        val (start, end) = getDateRange(selectedFilter)
        val username = SessionManager(context).getLoggedInUser() ?: return@LaunchedEffect
        val fetchedExpenses = db.collection("users").document(username)
            .collection("expenses")
            .whereEqualTo("category", categoryName)
            .get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val date = try { formatter.parse(it.date) } catch (e: Exception) { null }
                date != null && date >= start && date <= end
            }

        expenses = fetchedExpenses
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
            contentPadding = PaddingValues(16.dp, bottom = 100.dp)
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { label ->
                        Button(
                            onClick = { selectedFilter = label },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedFilter == label) Color(0xFFFFEB3B) else Color.LightGray
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, color = Color.Black, fontSize = 12.sp)
                        }
                    }
                }
            }


            if (expenses.isEmpty()) {
                item {
                    Text(
                        "No expenses found for this category and period.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(expense, selectedCurrency)
                }
            }
        }
    }
}

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
                "Date: ${expense.date}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            val convertedAmount = CurrencyConverter.convert(expense.amount, "ZAR", selectedCurrency)
            Text("Amount: $selectedCurrency ${"%.2f".format(convertedAmount)}", fontSize = 14.sp)
            Text("Description: ${expense.description}", fontSize = 14.sp)

            if (!expense.photoUrl.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.parse(expense.photoUrl)),
                    contentDescription = "Expense Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

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
