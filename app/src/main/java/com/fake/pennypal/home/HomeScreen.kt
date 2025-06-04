package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.data.model.Income
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun HomeScreen(navController: NavController) {
    var incomeList by remember { mutableStateOf(emptyList<Income>()) }
    var expenseList by remember { mutableStateOf(emptyList<Expense>()) }

    // Load income and expenses from Firebase Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        val incomes = db.collection("incomes").get().await()
            .mapNotNull { it.toObject(Income::class.java) }
        val expenses = db.collection("expenses").get().await()
            .mapNotNull { it.toObject(Expense::class.java) }

        incomeList = incomes
        expenseList = expenses
    }

    val totalIncome = incomeList.sumOf { it.amount }
    val totalExpenses = expenseList.sumOf { it.amount }
    val balance = totalIncome - totalExpenses
    val progress = if (totalIncome > 0) (totalExpenses / totalIncome).toFloat() else 0f

    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("manageCategories") }) {
                    Icon(Icons.Default.List, contentDescription = "Categories")
                }
                IconButton(onClick = { navController.navigate("addChoice") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                IconButton(onClick = { navController.navigate("goals") }) {
                    Icon(Icons.Default.Star, contentDescription = "Goals") // ⭐ NEW
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
                .background(Color(0xFFF1F8E9))
                .padding(16.dp)
        ) {
            Text("Hi, Welcome Back", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Balance: R${"%.2f".format(balance)}", fontWeight = FontWeight.Bold)
                    Text("Total Income: R${"%.2f".format(totalIncome)}", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    Text("Total Expenses: -R${"%.2f".format(totalExpenses)}", fontWeight = FontWeight.SemiBold, color = Color.Red)
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        color = Color(0xFF388E3C),
                        trackColor = Color.LightGray,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Text("${(progress * 100).toInt()}% of your income spent", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(expenseList) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(expense.category, fontWeight = FontWeight.Bold)
                                Text(expense.description, fontSize = 12.sp)
                            }
                            Text("R${expense.amount}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}