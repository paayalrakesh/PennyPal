@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import com.fake.pennypal.data.model.Badge
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.google.firebase.firestore.FirebaseFirestore
import com.fake.pennypal.utils.getCurrentUsername
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun CategorySpendingPreviewScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val username = getCurrentUsername(context)


    var selectedFilter by remember { mutableStateOf("Monthly") }
    var categoryTotals by remember { mutableStateOf(mapOf<String, Double>()) }
    var minGoal by remember { mutableStateOf(0.0) }
    var maxGoal by remember { mutableStateOf(20000.0) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    LaunchedEffect(selectedFilter) {
        val (start, end) = getDateRange(selectedFilter)

        val expenses = db.collection("expenses").get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                val parsedDate = try { formatter.parse(it.date) } catch (e: Exception) { null }
                parsedDate != null && parsedDate in start..end
            }

        val grouped = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        categoryTotals = grouped

        val totalSpent = grouped.values.sum()

        val goals = db.collection("goals").document("default").get().await().data
        if (goals != null) {
            minGoal = (goals["minSpendingGoal"] as? Number)?.toDouble() ?: 0.0
            maxGoal = (goals["spendingLimit"] as? Number)?.toDouble() ?: 20000.0
        }

        // 🔸 AUTOMATIC BADGE REWARD SYSTEM 🔸
        val badgeCollection = db.collection("users").document(username).collection("badges")

        if (totalSpent <= maxGoal) {
            val badge = hashMapOf(
                "title" to "Budget Keeper",
                "description" to "You stayed under your spending limit!",
                "earnedDate" to formatter.format(Date())
            )
            badgeCollection.add(badge)
        }

        val balance = maxGoal - totalSpent
        if (balance >= minGoal) {
            val badge = hashMapOf(
                "title" to "Savings Star",
                "description" to "You saved more than your minimum goal!",
                "earnedDate" to formatter.format(Date())
            )
            badgeCollection.add(badge)
        }
    }

    val totalSpent = categoryTotals.values.sum()
    val goalProgress = if (maxGoal > 0) (totalSpent / maxGoal).coerceIn(0.0, 1.0) else 0.0

    LaunchedEffect(goalProgress) {
        if (goalProgress < 1.0) {
            val badge = Badge(
                title = "Budget Master!",
                description = "You stayed under your monthly spending goal.",
                earnedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            db.collection("users").document(username)
                .collection("badges")
                .document("badge_budget_master")
                .set(badge)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending vs Goal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFFFFDE7),
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("categorySpendingPreview") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph")
                }

                IconButton(onClick = { navController.navigate("manageCategories") }) {
                    Icon(Icons.Default.List, contentDescription = "Categories")
                }
                IconButton(onClick = { navController.navigate("addChoice") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                IconButton(onClick = { navController.navigate("goals") }) {
                    Icon(Icons.Default.Star, contentDescription = "Goals")
                }
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val balance = maxGoal - categoryTotals.values.sum()
            val goalProgress = if (maxGoal > 0) (categoryTotals.values.sum() / maxGoal).coerceIn(0.0, 1.0) else 0.0
            val expenseTotal = categoryTotals.values.sum()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Balance", style = MaterialTheme.typography.labelMedium)
                    Text("R${"%.2f".format(balance)}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Expense: R${"%.2f".format(expenseTotal)}", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Goal Progress")
                    LinearProgressIndicator(
                        progress = goalProgress.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF4CAF50)
                    )
                    Text("${(goalProgress * 100).toInt()}% of your spending goal")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(listOf("Daily", "Weekly", "Monthly", "Yearly")) { label ->
                    Button(
                        onClick = { selectedFilter = label },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == label) Color(0xFFFFEB3B) else Color.LightGray
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(100.dp) // Ensures consistent button size
                    ) {
                        Text(label, color = Color.Black, fontSize = 12.sp)
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            BarChartWithGoals(categoryTotals, minGoal, maxGoal)
        }
    }
}

@Composable
fun BarChartWithGoals(data: Map<String, Double>, minGoal: Double, maxGoal: Double) {
    val maxValue = (data.values.maxOrNull() ?: 1.0).coerceAtLeast(maxGoal)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(12.dp)),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { (category, amount) ->
                    val barHeightRatio = amount / maxValue
                    val barHeight = (barHeightRatio * 200).coerceAtLeast(10.0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.height(260.dp)
                    ) {
                        Text("R${"%.0f".format(amount)}", fontSize = 12.sp)

                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(barHeight.dp)
                                .background(
                                    if (amount > maxGoal) Color.Red else Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(category.take(6), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(modifier = Modifier.size(16.dp, 4.dp).background(Color.Red))
                Text("Max Goal", fontSize = 12.sp)

                Box(modifier = Modifier.size(16.dp, 4.dp).background(Color(0xFFFFEB3B)))
                Text("Min Goal", fontSize = 12.sp)
            }
        }
    }
}
