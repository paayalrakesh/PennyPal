package com.fake.pennypal.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Income
import com.fake.pennypal.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalysisScreen(navController: NavController) {
    var selectedFilter by remember { mutableStateOf("Monthly") }
    var incomeList by remember { mutableStateOf(emptyList<Income>()) }
    var expenseList by remember { mutableStateOf(emptyList<Expense>()) }
    var minGoal by remember { mutableStateOf(0.0) }
    var maxGoal by remember { mutableStateOf(20000.0) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val incomeTotal = incomeList.sumOf { it.amount }
    val expenseTotal = expenseList.sumOf { it.amount }
    val balance = incomeTotal - expenseTotal
    val goalProgress = if (maxGoal > 0) (expenseTotal / maxGoal).coerceIn(0.0, 1.0) else 0.0

    // 🔄 Load from Firebase
    LaunchedEffect(selectedFilter) {
        val (startDate, endDate) = getDateRange(selectedFilter)
        val db = FirebaseFirestore.getInstance()

        val incomes = db.collection("incomes").get().await()
            .mapNotNull { it.toObject(Income::class.java) }
            .filter {
                parseDate(it.date, dateFormat)?.let { d -> d in startDate..endDate } ?: false
            }

        val expenses = db.collection("expenses").get().await()
            .mapNotNull { it.toObject(Expense::class.java) }
            .filter {
                parseDate(it.date, dateFormat)?.let { d -> d in startDate..endDate } ?: false
            }

        val goalDoc = db.collection("goals").document("default").get().await()
        val goalData = goalDoc.data
        if (goalData != null) {
            minGoal = (goalData["minGoal"] as? Number)?.toDouble() ?: 0.0
            maxGoal = (goalData["maxGoal"] as? Number)?.toDouble() ?: 20000.0
        }

        incomeList = incomes
        expenseList = expenses
    }

    val dailyMap = mutableMapOf<String, Pair<Float, Float>>() // dateLabel -> (income, expense)
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormatter = SimpleDateFormat("E", Locale.getDefault())

    val allDates = (incomeList.map { it.date } + expenseList.map { it.date }).distinct()

    allDates.forEach { rawDate ->
        val parsedDate = parseDate(rawDate, formatter)
        val label = parsedDate?.let { labelFormatter.format(it) } ?: "?"

        val dailyIncome = incomeList.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()
        val dailyExpense = expenseList.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()

        dailyMap[label] = Pair(dailyIncome, dailyExpense)
    }

    val dailyChartData = dailyMap.entries.sortedBy { it.key }.map { it.toPair() }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("analysisScreen") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Analysis")
                }
                IconButton(onClick = { navController.navigate("categorySummary") }) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Category Summary",
                        tint = Color.Black
                    )
                }
                IconButton(onClick = { navController.navigate("categorySpendingPreview") }) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Category Spending Graph",
                        tint = Color.Black
                    )
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Header & Goal Display
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
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(label, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SimpleBarChartGrouped(dailyData = dailyChartData)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Income: R${"%.2f".format(incomeTotal)}", color = Color(0xFF388E3C))
                Text("Expense: R${"%.2f".format(expenseTotal)}", color = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
fun AnalysisFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(label)
    }
}

@Composable
fun SimpleBarChartGrouped(
    dailyData: List<Pair<String, Pair<Float, Float>>>,
    modifier: Modifier = Modifier
) {
    val maxValue = dailyData.maxOfOrNull { maxOf(it.second.first, it.second.second) }?.coerceAtLeast(1f) ?: 1f
    val barWidth = 30f
    val barSpacing = 40f
    val labelSpacing = 10f

    Canvas(modifier = modifier.height(200.dp).fillMaxWidth()) {
        val chartHeight = size.height
        val startX = 50f

        dailyData.forEachIndexed { index, (label, values) ->
            val xBase = startX + index * (2 * barWidth + barSpacing)
            val incomeHeight = (values.first / maxValue) * chartHeight
            val expenseHeight = (values.second / maxValue) * chartHeight

            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(x = xBase, y = chartHeight - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = CornerRadius(10f)
            )

            drawRoundRect(
                color = Color(0xFFF44336),
                topLeft = Offset(x = xBase + barWidth + labelSpacing, y = chartHeight - expenseHeight),
                size = Size(barWidth, expenseHeight),
                cornerRadius = CornerRadius(10f)
            )
        }
    }
}

fun getAnalysisDateRange(filter: String): Pair<Date, Date> {
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

fun parseDate(dateStr: String, formatter: SimpleDateFormat): Date? {
    return try {
        formatter.parse(dateStr)
    } catch (e: Exception) {
        null
    }
}
