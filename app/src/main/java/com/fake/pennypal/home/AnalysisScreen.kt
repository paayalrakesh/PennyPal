package com.fake.pennypal.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.fake.pennypal.data.model.Income
import com.fake.pennypal.data.model.Expense
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement


@Composable
fun AnalysisScreen() {
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

    // Load data when filter changes
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
// Group and sum by day
        val dailyMap = mutableMapOf<String, Pair<Float, Float>>() // dateLabel -> (incomeTotal, expenseTotal)

        val allDates = (incomes.map { it.date } + expenses.map { it.date }).distinct()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormatter = SimpleDateFormat("E", Locale.getDefault()) // "Mon", "Tue", etc.

        allDates.forEach { rawDate ->
            val parsedDate = parseDate(rawDate, formatter)
            val label = parsedDate?.let { labelFormatter.format(it) } ?: "?"

            val dailyIncome = incomes.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()
            val dailyExpense = expenses.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()
            dailyMap[label] = Pair(dailyIncome, dailyExpense)
        }

        val groupedDailyData = dailyMap.entries.sortedBy { it.key }.map { it.toPair() }

        incomeList = incomes
        expenseList = expenses
    }

    val dailyMap = mutableMapOf<String, Pair<Float, Float>>() // dateLabel -> (income, expense)

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormatter = SimpleDateFormat("E", Locale.getDefault()) // "Mon", "Tue", etc.

    val allDates = (incomeList.map { it.date } + expenseList.map { it.date }).distinct()

    allDates.forEach { rawDate ->
        val parsedDate = parseDate(rawDate, formatter)
        val label = parsedDate?.let { labelFormatter.format(it) } ?: "?"

        val dailyIncome = incomeList.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()
        val dailyExpense = expenseList.filter { it.date == rawDate }.sumOf { it.amount }.toFloat()

        dailyMap[label] = Pair(dailyIncome, dailyExpense)
    }

    val dailyChartData = dailyMap.entries.sortedBy { it.key }.map { it.toPair() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔹 Totals and Goal Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Balance", style = MaterialTheme.typography.labelMedium)
                Text(
                    "R${"%.2f".format(balance)}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
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

        // 🔹 Filter Buttons
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


        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Bar Chart
        SimpleBarChartGrouped(dailyData = dailyChartData)

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Summary Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Income: R${"%.2f".format(incomeTotal)}", color = Color(0xFF388E3C))
            Text("Expense: R${"%.2f".format(expenseTotal)}", color = Color(0xFFD32F2F))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🔹 Bottom Navigation (basic)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("🏠", "📊", "➕", "📂", "⚙️").forEach { emoji ->
                Button(onClick = {}, shape = CircleShape) {
                    Text(emoji)
                }
            }
        }
    }
}


@Composable
fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
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
    dailyData: List<Pair<String, Pair<Float, Float>>>, // dateLabel to (income, expense)
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

            // Income bar (green)
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(x = xBase, y = chartHeight - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = CornerRadius(10f)
            )

            // Expense bar (red)
            drawRoundRect(
                color = Color(0xFFF44336),
                topLeft = Offset(x = xBase + barWidth + labelSpacing, y = chartHeight - expenseHeight),
                size = Size(barWidth, expenseHeight),
                cornerRadius = CornerRadius(10f)
            )
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

fun parseDate(dateStr: String, formatter: SimpleDateFormat): Date? {
    return try {
        formatter.parse(dateStr)
    } catch (e: Exception) {
        null
    }

}
