@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CategorySummaryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Load and group expenses by category
    LaunchedEffect(selectedFilter) {
        val (start, end) = getDateRange(selectedFilter)
        val expenses = db.collection("expenses").get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
                parsedDate != null && parsedDate >= start && parsedDate <= end
            }

        categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FilterRow(selected = selectedFilter, onFilterSelected = { selectedFilter = it })

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                Text("No expenses found for this period.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categoryTotals.entries.toList()) { (category, total) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("R${"%.2f".format(total)}", fontSize = 18.sp, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        }
    }
}
