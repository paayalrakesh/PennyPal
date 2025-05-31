package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.room.Room
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.data.local.entities.Expense
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(
            context,
            PennyPalDatabase::class.java, "pennypal-db"
        ).build()
    }
    val expenseDao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()
    var expenses by remember { mutableStateOf(listOf<Expense>()) }

    // Load expenses from RoomDB
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            expenses = expenseDao.getExpensesInRange("0000-00-00", "9999-12-31")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Your Expenses",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(expenses) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Date: ${expense.date}")
                            Text("Amount: ${expense.amount}")
                            Text("Category: ${expense.category}")
                            Text("Description: ${expense.description}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("addExpense")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Expense")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    navController.navigate("manageCategories")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Categories")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B))
            ) {
                Text("Logout", color = Color.Black)
            }
        }
    }
}