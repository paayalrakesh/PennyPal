@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement

@Composable
fun CategoryExpensesScreen(navController: NavController, categoryName: String) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    var selectedFilter by remember { mutableStateOf("Monthly") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 🔁 Load expenses from Firestore when category or filter changes
    LaunchedEffect(categoryName, selectedFilter) {
        coroutineScope.launch {
            val (startDate, endDate) = getDateRange(selectedFilter)
            try {
                val snapshot = db.collection("expenses").get().await()
                val allExpenses = snapshot.toObjects(Expense::class.java)

                val filtered = allExpenses.filter { expense ->
                    expense.category == categoryName &&
                            parseDate(expense.date, dateFormat)?.let { d ->
                                d in startDate..endDate
                            } == true
                }

                expenses = filtered
            } catch (e: Exception) {
                // You can log the error or show a toast
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF1F8E9))
                .padding(16.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(listOf("Daily", "Weekly", "Monthly", "Yearly")) { label ->
                    FilterButton(label = label, selected = selectedFilter == label) {
                        selectedFilter = label
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                items(groupExpensesByMonth(expenses).toList()) { (month, monthExpenses) ->
                    Text(
                        text = month,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    monthExpenses.forEach { expense ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Title: ${expense.description}", fontWeight = FontWeight.Bold)
                                Text("Date: ${expense.date}")
                                if (expense.startTime.isNotEmpty() && expense.endTime.isNotEmpty()) {
                                    Text("Time: ${expense.startTime} - ${expense.endTime}")
                                }
                                Text("Amount: R${"%.2f".format(expense.amount)}", color = Color.Red)

                                if (!expense.photoUri.isNullOrEmpty()) {
                                    val bitmap = remember(expense.photoUri) {
                                        try {
                                            val url = URL(expense.photoUri)
                                            BitmapFactory.decodeStream(url.openConnection().getInputStream())
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    bitmap?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Photo",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFFFFEB3B) else Color.LightGray
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = Color.Black, fontSize = 14.sp)
    }
}


fun getDateRange(filter: String): Pair<Date, Date> {
    val now = Calendar.getInstance()
    val end = now.time
    val start = Calendar.getInstance()
    when (filter) {
        "Daily" -> start.add(Calendar.DAY_OF_YEAR, -1)
        "Weekly" -> start.add(Calendar.WEEK_OF_YEAR, -1)
        "Monthly" -> start.add(Calendar.MONTH, -1)
        "Yearly" -> start.add(Calendar.YEAR, -1)
    }
    return start.time to end
}

fun parseDate(dateStr: String, formatter: SimpleDateFormat): Date? {
    return try {
        formatter.parse(dateStr)
    } catch (e: Exception) {
        null
    }
}

fun groupExpensesByMonth(expenses: List<Expense>): Map<String, List<Expense>> {
    val formatter = SimpleDateFormat("MMMM", Locale.getDefault())
    return expenses.groupBy {
        parseDate(it.date, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()))?.let { date ->
            formatter.format(date)
        } ?: "Unknown"
    }
}
