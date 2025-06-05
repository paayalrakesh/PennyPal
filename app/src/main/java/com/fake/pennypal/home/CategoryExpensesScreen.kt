@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.fake.pennypal.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CategoryExpensesScreen(navController: NavController, categoryName: String) {
    val db = FirebaseFirestore.getInstance()
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    LaunchedEffect(selectedFilter, categoryName) {
        val (start, end) = getDateRange(selectedFilter)
        val allExpenses = db.collection("expenses")
            .whereEqualTo("category", categoryName)
            .get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val date = try { formatter.parse(it.date) } catch (e: Exception) { null }
                date != null && date >= start && date <= end
            }
        expenses = allExpenses
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FilterRow(selectedFilter) { selectedFilter = it }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("categorySummary") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text("View Category Summary", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }


            if (expenses.isEmpty()) {
                Text("No expenses found for this category and period.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(expenses) { expense ->
                        ExpenseCard(expense)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRow(selected: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("Daily", "Weekly", "Monthly", "Yearly")
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        filters.forEach { filter ->
            Button(
                onClick = { onFilterSelected(filter) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (filter == selected) Color(0xFFFFEB3B) else Color.LightGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(filter, color = Color.Black)
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date: ${expense.date}", fontWeight = FontWeight.Bold)
            Text("Amount: R${"%.2f".format(expense.amount)}")
            Text("Description: ${expense.description}")
            if (!expense.photoUri.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.parse(expense.photoUri)),
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
