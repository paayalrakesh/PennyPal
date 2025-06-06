package com.fake.pennypal.home

import com.fake.pennypal.data.local.entities.Expense as RoomExpense
import com.fake.pennypal.data.model.Expense as FirebaseExpense
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.room.Room
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.fake.pennypal.data.local.PennyPalDatabase
import com.fake.pennypal.utils.SessionManager
import com.fake.pennypal.utils.getCurrentUsername
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AddExpenseScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val selectedCurrency = sessionManager.getSelectedCurrency()
    //val username = remember { SessionManager(context).getLoggedInUser() }


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
            Text("Add New Expense", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            Button(
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(context, { _, year, month, day ->
                        date = String.format("%04d-%02d-%02d", year, month + 1, day)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (date.isEmpty()) "Select Date" else "Date: $date", color = Color.Black)
            }

            // Start and End Time Pickers
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(if (photoUri == null) "Add Photo" else "Photo Added", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (date.isNotEmpty() && amount.isNotEmpty() && category.isNotEmpty()) {
                        coroutineScope.launch {
                            println("🔄 Starting save process...")

                            val username = SessionManager(context).getLoggedInUser()
                            println("👤 Username: $username")

                            if (username.isNullOrBlank()) {
                                println("❌ Username is null or blank")
                                return@launch
                            }

                            var downloadUrl = ""

                            // Upload Photo
                            if (photoUri != null) {
                                try {
                                    val imageRef = storageRef.child("users/$username/expenses/${UUID.randomUUID()}.jpg")
                                    val uploadTask = imageRef.putFile(photoUri!!)
                                    uploadTask.await()
                                    downloadUrl = imageRef.downloadUrl.await().toString()
                                    println("📸 Photo uploaded successfully: $downloadUrl")
                                } catch (e: Exception) {
                                    println("❌ Photo upload failed: ${e.localizedMessage}")
                                    return@launch
                                }
                            } else {
                                println("⚠️ No photo selected")
                            }

                            // Convert amount
                            val expenseAmount = amount.toDoubleOrNull()
                            if (expenseAmount == null) {
                                println("❌ Invalid amount entered")
                                return@launch
                            }

                            // Save to Room (optional)
                            try {
                                val roomExpense = RoomExpense(
                                    date = date,
                                    amount = expenseAmount,
                                    category = category,
                                    description = description,
                                    photoUri = downloadUrl
                                )
                                expenseDao.insertExpense(roomExpense)
                                println("📦 Saved to Room DB: $roomExpense")
                            } catch (e: Exception) {
                                println("❌ Failed to save to Room: ${e.localizedMessage}")
                            }

                            // Save to Firebase
                            try {
                                val firebaseExpense = FirebaseExpense(
                                    date = date,
                                    amount = expenseAmount,
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
                                        println("✅ Expense saved to Firestore: $firebaseExpense")
                                        navController.popBackStack()
                                    }
                                    .addOnFailureListener { e ->
                                        println("❌ Firestore save failed: ${e.localizedMessage}")
                                    }
                            } catch (e: Exception) {
                                println("❌ Error creating Firestore document: ${e.localizedMessage}")
                            }
                        }
                    } else {
                        println("⚠️ Missing fields. Please complete all inputs.")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Expense", color = Color.White)
            }
        }
    }
}
