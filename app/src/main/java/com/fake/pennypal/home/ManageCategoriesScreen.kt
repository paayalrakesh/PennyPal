package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.room.Room
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.data.local.entities.Category
import com.fake.pennypal.data.local.entities.Expense
import com.fake.pennypal.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun ManageCategoriesScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(
            context,
            PennyPalDatabase::class.java, "pennypal-db"
        ).build()
    }
    val categoryDao = db.categoryDao()
    val expenseDao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var selectedExpenses by remember { mutableStateOf(listOf<Expense>()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Load categories
    LaunchedEffect(Unit) {
        coroutineScope.launch { categories = categoryDao.getAllCategories() }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("manageCategories") }) {
                    Icon(Icons.Default.List, contentDescription = "Categories")
                }
                IconButton(onClick = { navController.navigate("addExpense") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Manage Categories", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (categoryName.isNotEmpty()) {
                        coroutineScope.launch {
                            categoryDao.insertCategory(Category(categoryName))
                            categories = categoryDao.getAllCategories()
                            categoryName = ""

                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add Category", color = Color.Black) }

            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                coroutineScope.launch {
                                    navController.navigate("categoryExpenses/${category.name}")
                                    selectedExpenses = expenseDao.getExpensesByCategory(category.name)
                                    selectedCategory = category.name
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(category.name, fontWeight = FontWeight.Medium, color = Color(0xFF388E3C))
                        }
                    }
                }
            }

            selectedCategory?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Expenses for $it", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                selectedExpenses.forEach { expense ->
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
