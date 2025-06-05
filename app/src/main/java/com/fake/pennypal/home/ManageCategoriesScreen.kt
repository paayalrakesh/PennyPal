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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Category
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.data.model.Income
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ManageCategoriesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var categoryName by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<Category>()) }
    var selectedExpenses by remember { mutableStateOf(listOf<Expense>()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpense by remember { mutableStateOf(0.0) }
    val totalBalance = totalIncome - totalExpense

    // Load categories and balance
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val catSnapshot = db.collection("categories").get().await()
            categories = catSnapshot.toObjects(Category::class.java)

            val incomeSnap = db.collection("incomes").get().await()
            totalIncome = incomeSnap.toObjects(Income::class.java).sumOf { it.amount }

            val expenseSnap = db.collection("expenses").get().await()
            totalExpense = expenseSnap.toObjects(Expense::class.java).sumOf { it.amount }
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                IconButton(onClick = { navController.navigate("home") }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { navController.navigate("analysisScreen") }) {
                    Icon(Icons.Default.BarChart, contentDescription = "Analysis")
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
                .background(Color(0xFFF1F8E9))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Manage Categories", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))

            Spacer(modifier = Modifier.height(12.dp))

            Text("Total Balance: R${"%.2f".format(totalBalance)}", color = Color.Black, fontWeight = FontWeight.Bold)
            Text("Income: R${"%.2f".format(totalIncome)} | Expenses: R${"%.2f".format(totalExpense)}", color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

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
                            db.collection("categories").add(Category(name = categoryName))
                            val catSnapshot = db.collection("categories").get().await()
                            categories = catSnapshot.toObjects(Category::class.java)
                            categoryName = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Category", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                category.name,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF388E3C),
                                modifier = Modifier.clickable {
                                    navController.navigate("categoryExpenses/${category.name}")
                                }

                            )
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    val catSnap = db.collection("categories")
                                        .whereEqualTo("name", category.name)
                                        .get().await()
                                    catSnap.documents.firstOrNull()?.reference?.delete()
                                    val catSnapshot = db.collection("categories").get().await()
                                    categories = catSnapshot.toObjects(Category::class.java)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Category")
                            }
                        }
                    }
                }
            }
        }
    }
}
