@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.fake.pennypal.data.model.Category
import com.fake.pennypal.data.model.Expense
import com.fake.pennypal.data.model.Income
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.CurrencyConverter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ManageCategoriesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val username = SessionManager(context).getLoggedInUser() ?: return
    val sessionManager = remember { SessionManager(context) }
    val selectedCurrency = sessionManager.getSelectedCurrency()
    val coroutineScope = rememberCoroutineScope()
    var categoryName by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<Category>()) }

    var totalIncomeZAR by remember { mutableStateOf(0.0) }
    var totalExpenseZAR by remember { mutableStateOf(0.0) }

    val totalIncomeConverted by remember(totalIncomeZAR, selectedCurrency) {
        derivedStateOf {
            CurrencyConverter.convert(totalIncomeZAR, "ZAR", selectedCurrency)
        }
    }
    val totalExpenseConverted by remember(totalExpenseZAR, selectedCurrency) {
        derivedStateOf {
            CurrencyConverter.convert(totalExpenseZAR, "ZAR", selectedCurrency)
        }
    }
    val totalBalanceConverted by remember(totalIncomeZAR, totalExpenseZAR, selectedCurrency) {
        derivedStateOf {
            CurrencyConverter.convert(totalIncomeZAR - totalExpenseZAR, "ZAR", selectedCurrency)
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val catSnapshot = db.collection("users").document(username)
                .collection("categories").get().await()
            categories = catSnapshot.toObjects(Category::class.java)

            val incomeSnap = db.collection("users").document(username)
                .collection("incomes").get().await()
            totalIncomeZAR = incomeSnap.toObjects(Income::class.java).sumOf { it.amount }

            val expenseSnap = db.collection("users").document(username)
                .collection("expenses").get().await()
            totalExpenseZAR = expenseSnap.toObjects(Expense::class.java).sumOf { it.amount }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { navController.navigate("home") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { navController.navigate("categorySpendingPreview") }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph")
                    }
                    IconButton(onClick = { navController.navigate("manageCategories") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.List, contentDescription = "Categories")
                    }
                    IconButton(onClick = { navController.navigate("addChoice") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = { navController.navigate("goals") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, contentDescription = "Goals")
                    }
                    IconButton(onClick = { navController.navigate("profile") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF1F8E9))
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Manage Categories",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Total Balance: $selectedCurrency ${"%.2f".format(totalBalanceConverted)}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                "Income: $selectedCurrency ${"%.2f".format(totalIncomeConverted)} | Expenses: $selectedCurrency ${"%.2f".format(totalExpenseConverted)}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (categoryName.isNotEmpty()) {
                        coroutineScope.launch {
                            db.collection("users").document(username).collection("categories")
                                .add(Category(name = categoryName, userId = username))
                            val catSnapshot = db.collection("users").document(username)
                                .collection("categories").get().await()
                            categories = catSnapshot.toObjects(Category::class.java)
                            categoryName = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Category", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate("categorySummary") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF176)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Category Summary", color = Color.Black, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(categories) { category ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                navController.navigate("categoryExpenses/${category.name}")
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF388E3C)
                            )
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val catSnap = db.collection("users").document(username)
                                            .collection("categories")
                                            .whereEqualTo("name", category.name)
                                            .get().await()
                                        catSnap.documents.firstOrNull()?.reference?.delete()
                                        val catSnapshot = db.collection("users").document(username)
                                            .collection("categories").get().await()
                                        categories = catSnapshot.toObjects(Category::class.java)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Category")
                            }
                        }
                    }
                }
            }
        }
    }
}
