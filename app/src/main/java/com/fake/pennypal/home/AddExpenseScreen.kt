package com.fake.pennypal.home

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import java.util.*

@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember {
        Room.databaseBuilder(
            context,
            PennyPalDatabase::class.java, "pennypal-db"
        ).build()
    }
    val expenseDao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()

    var date by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF7F1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Add New Expense", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            Button(onClick = {
                val calendar = Calendar.getInstance()
                DatePickerDialog(context, { _, year, month, day ->
                    date = "$year-${month + 1}-$day"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }) {
                Text(if (date.isEmpty()) "Select Date" else "Date: $date")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount Input (modified)
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category Input
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Photo Picker
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text(if (photoUri == null) "Add Photo" else "Photo Added")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (date.isNotEmpty() && amount.isNotEmpty() && category.isNotEmpty()) {
                        coroutineScope.launch {
                            expenseDao.insertExpense(
                                Expense(
                                    date = date,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    category = category,
                                    description = description,
                                    photoUri = photoUri?.toString()
                                )
                            )
                            navController.popBackStack() // Go back to HomeScreen
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense", color = Color.Black)
            }
        }
    }
}