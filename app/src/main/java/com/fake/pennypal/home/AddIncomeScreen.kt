@file:OptIn(ExperimentalMaterial3Api::class)

package com.fake.pennypal.home

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fake.pennypal.data.model.Income
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun AddIncomeScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val selectedCurrency = sessionManager.getSelectedCurrency()
    val username = remember { SessionManager(context).getLoggedInUser() }

    var date by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
                .background(Color(0xFFFFFDE7))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add New Income",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(context, { _, year, month, day ->
                        date = String.format("%04d-%02d-%02d", year, month + 1, day)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (date.isEmpty()) "Select Date" else "Date: $date", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount in $selectedCurrency") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    println("🔍 DEBUG: Username from SessionManager = $username")

                    if (username.isNullOrBlank()) {
                        println("❌ ERROR: No user is currently logged in!")
                        return@Button
                    }

                    if (date.isNotEmpty() && amount.isNotEmpty()) {
                        val income = Income(
                            date = date,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            description = description
                        )

                        val db = FirebaseFirestore.getInstance()
                        db.collection("users").document(username).collection("incomes")
                            .add(income)
                            .addOnSuccessListener {
                                println("✅ Income saved: $income")
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                                println("❌ Failed to save income: ${e.localizedMessage}")
                            }
                    } else {
                        println("⚠️ Incomplete fields: date=$date amount=$amount")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Income", color = Color.White)
            }

            Spacer(modifier = Modifier.height(80.dp)) // To avoid overlap with bottom nav
        }
    }
}
