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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import androidx.room.Room
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.data.local.entities.Expense
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryExpensesScreen(navController: NavController, categoryName: String) {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(context, PennyPalDatabase::class.java, "pennypal-db").build()
    }
    val expenseDao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf(listOf<Expense>()) }

    LaunchedEffect(categoryName) {
        coroutineScope.launch {
            expenses = expenseDao.getExpensesByCategory(categoryName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses in $categoryName") },
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
            LazyColumn {
                items(expenses) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Date: ${expense.date}")
                            Text("Amount: R${expense.amount}")
                            Text("Description: ${expense.description}")
                        }
                    }
                }
            }
        }
    }
}