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
import com.fake.pennypal.data.local.entities.Category
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
    val coroutineScope = rememberCoroutineScope()

    var categoryName by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf(listOf<Category>()) }

    // Load categories from RoomDB
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            categories = categoryDao.getAllCategories()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF7F1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                text = "Manage Categories",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (categoryName.isNotEmpty()) {
                        coroutineScope.launch {
                            categoryDao.insertCategory(Category(categoryName))
                            categories = categoryDao.getAllCategories() // Refresh list
                            categoryName = "" // Clear input
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Category", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Text(
                            text = category.name,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Back", color = Color.White)
            }
        }
    }
}