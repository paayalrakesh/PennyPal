/*
Title: Get real-time updates with Cloud Firestore
Author: Google
Date: 2023
Code version: N/A
Availability: https://firebase.google.com/docs/firestore/query-data/listen
*/

/*
Title: Side-effects in Jetpack Compose (DisposableEffect)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/side-effects#disposableeffect
*/

/*
Title: Material Components for Compose (AlertDialog, DropdownMenu)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/components/dialog
*/


@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Badge
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

private const val TAG = "ProfileScreen"

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val sessionManager = remember { SessionManager(context) }

    var fullName by remember { mutableStateOf("Loading...") }
    var userId by remember { mutableStateOf("Loading...") }
    var badges by remember { mutableStateOf(listOf<Badge>()) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    var showBadgesDialog by remember { mutableStateOf(false) }

    val currencyOptions = listOf("ZAR", "USD", "EUR", "GBP", "INR")
    var selectedCurrency by remember { mutableStateOf(sessionManager.getSelectedCurrency()) }

    DisposableEffect(Unit) {
        val currentUsername = sessionManager.getLoggedInUser()
        if (currentUsername.isNullOrBlank()) {
            isLoading = false
            onDispose { }
        } else {
            db.collection("users").document(currentUsername).get()
                .addOnSuccessListener { userDoc ->
                    fullName = userDoc.getString("name") ?: currentUsername
                    userId = userDoc.getString("userId") ?: "Unknown"
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch user info", e)
                    fullName = "Error"
                    userId = "Error"
                }

            val badgesListener = db.collection("users").document(currentUsername)
                .collection("badges")
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        Log.e(TAG, "Badge listener failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        badges = snapshot.documents.mapNotNull { it.toObject(Badge::class.java) }
                        Log.d(TAG, "Real-time badge update. Found ${badges.size} badges.")
                    }
                }

            onDispose {
                Log.d(TAG, "Removing badge listener.")
                badgesListener.remove()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF1F8E9))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(fullName, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("User ID: $userId", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("🎖️ Your Badges", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (badges.isEmpty()) {
                Text("No badges earned yet. Keep working on your goals!")
            } else {
                // CHANGE: Changed button color to primary yellow
                Button(
                    onClick = { showBadgesDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEB3B),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Badges Icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("View Your ${badges.size} Badges")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text("🌍 Preferred Currency", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            DropdownMenuBox(
                selected = selectedCurrency,
                onSelected = {
                    selectedCurrency = it
                    sessionManager.setSelectedCurrency(it)
                },
                options = currencyOptions
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CHANGE: Changed logout button from Red to thematic Green
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout", color = Color.White, fontSize = 16.sp)
            }
        }

        if (showBadgesDialog) {
            BadgesDialog(
                badges = badges,
                onDismiss = { showBadgesDialog = false }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sessionManager.logout()
                            showLogoutDialog = false
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    ) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
                },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out?") }
            )
        }
    }
}

@Composable
fun BadgesDialog(badges: List<Badge>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Earned Badges", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(badges) { badge ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF59D)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(badge.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(badge.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Earned on: ${badge.earnedDate}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DropdownMenuBox(
    selected: String,
    onSelected: (String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        onSelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}