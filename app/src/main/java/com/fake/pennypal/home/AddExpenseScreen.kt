
/*
Title: Get a result from an activity (rememberLauncherForActivityResult)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/training/basics/intents/result
*/

/*
Title: Upload files with Cloud Storage on Android
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/storage/android/upload-files
*/

/*
Title: Save data in a local database using Room
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/training/data-storage/room
*/

/*
Title: Pickers (DatePickerDialog, TimePickerDialog)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/develop/ui/views/components/pickers
*/

/*
Title: Kotlin Coroutines on Android
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/kotlin/coroutines
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import com.fake.pennypal.data.local.entities.Expense as RoomExpense
import com.fake.pennypal.data.model.Expense as FirebaseExpense
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.room.Room
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.utils.CurrencyConverter
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.getDateRange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

// Added a TAG for logging, which helps in filtering Logcat messages for this specific screen.
private const val TAG = "AddExpenseScreen"

@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    // The currency the user is currently seeing and inputting values in.
    val selectedCurrency = sessionManager.getSelectedCurrency()

    val db = remember {
        Room.databaseBuilder(
            context,
            PennyPalDatabase::class.java, "pennypal-db"
        ).build()
    }
    val expenseDao = db.expenseDao()
    val coroutineScope = rememberCoroutineScope()
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> photoUri = uri }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            ) {
                // This Row structure for the bottom bar is preserved as is.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { navController.navigate("home") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Home, contentDescription = "Home") }
                    IconButton(onClick = { navController.navigate("categorySpendingPreview") }) { Icon(Icons.Default.BarChart, contentDescription = "Category Spending Graph") }
                    IconButton(onClick = { navController.navigate("manageCategories") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.List, contentDescription = "Categories") }
                    IconButton(onClick = { navController.navigate("addChoice") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, contentDescription = "Add") }
                    IconButton(onClick = { navController.navigate("goals") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Star, contentDescription = "Goals") }
                    IconButton(onClick = { navController.navigate("profile") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Person, contentDescription = "Profile") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF1F8E9))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Add New Expense",
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

            Spacer(modifier = Modifier.height(8.dp))

            // Start & End Time Pickers
            Button(onClick = {
                val cal = Calendar.getInstance()
                TimePickerDialog(context, { _, h, m -> startTime = "%02d:%02d".format(h, m) },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp)) {
                Text(if (startTime.isEmpty()) "Select Start Time" else "Start Time: $startTime", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                val cal = Calendar.getInstance()
                TimePickerDialog(context, { _, h, m -> endTime = "%02d:%02d".format(h, m) },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp)) {
                Text(if (endTime.isEmpty()) "Select End Time" else "End Time: $endTime", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount in $selectedCurrency") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
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

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (photoUri == null) "Add Photo" else "Photo Added", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (date.isNotEmpty() && amount.isNotEmpty() && category.isNotEmpty()) {
                        coroutineScope.launch {
                            val username = SessionManager(context).getLoggedInUser() ?: return@launch

                            var downloadUrl = ""
                            photoUri?.let { uri ->
                                try {
                                    val imageRef = storageRef.child("users/$username/expenses/${UUID.randomUUID()}.jpg")
                                    imageRef.putFile(uri).await()
                                    downloadUrl = imageRef.downloadUrl.await().toString()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Image upload failed", e)
                                    return@launch
                                }
                            }

                            // Step 1: Parse the user's input. This value is in the 'selectedCurrency'.
                            val amountInSelectedCurrency = amount.toDoubleOrNull() ?: return@launch
                            Log.d(TAG, "User entered amount: $amountInSelectedCurrency in $selectedCurrency")

                            // Step 2: Convert the input amount from the selected currency BACK to the base currency (ZAR).
                            // This ensures all data stored in the database is in a consistent, single currency.
                            val amountToSaveInZAR = CurrencyConverter.convert(amountInSelectedCurrency, selectedCurrency, "ZAR")
                            Log.d(TAG, "Converted amount to ZAR for saving: $amountToSaveInZAR")


                            // Step 3: Use the converted ZAR amount when creating the objects for Room and Firestore.
                            try {
                                val roomExpense = RoomExpense(
                                    date = date,
                                    amount = amountToSaveInZAR, // Use the converted amount
                                    category = category,
                                    description = description,
                                    photoUri = downloadUrl
                                )
                                expenseDao.insertExpense(roomExpense)
                            } catch (e: Exception) {
                                Log.e(TAG, "Room DB Error", e)
                            }

                            try {
                                val firebaseExpense = FirebaseExpense(
                                    date = date,
                                    amount = amountToSaveInZAR, // Use the converted amount
                                    category = category,
                                    description = description,
                                    startTime = startTime,
                                    endTime = endTime,
                                    photoUrl = downloadUrl
                                )
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(username)
                                    .collection("expenses")
                                    .add(firebaseExpense)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Expense saved successfully to Firestore.")
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Firestore Error", e)
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "An unexpected error occurred during Firestore operation", e)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Expense", color = Color.White)
            }

            Spacer(modifier = Modifier.height(80.dp)) // prevent bottom nav overlap
        }
    }
}
