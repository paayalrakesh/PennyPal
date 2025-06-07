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
Title: Lists in Compose (LazyColumn)
Author: Google
Date: 2023
Code version: N/A
Availability: https://developer.android.com/jetpack/compose/lists
*/

@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fake.pennypal.data.model.Badge
import com.fake.pennypal.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

private const val TAG = "BadgeScreen"

@Composable
fun BadgeScreen(navController: NavController) {
    val context = LocalContext.current
    // FIX: Get username directly and remember it. This is cleaner and avoids race conditions.
    val username = remember { SessionManager(context).getLoggedInUser() }

    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }

    // Use DisposableEffect with the username as a key.
    // This effect sets up a REAL-TIME listener that automatically updates the UI.
    // It will only run if the username is not null, solving the race condition.
    DisposableEffect(username) {
        if (username.isNullOrBlank()) {
            // If there's no user, do nothing.
            Log.w(TAG, "No username found, skipping badge listener setup.")
            onDispose { } // Still need to return an onDispose block.
        } else {
            Log.d(TAG, "Setting up real-time badge listener for user: $username")
            val db = FirebaseFirestore.getInstance()
            val listener = db.collection("users").document(username)
                .collection("badges")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Badge listener failed.", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        // When data changes, map it to our Badge objects and update the state.
                        badges = snapshot.documents.mapNotNull { it.toObject(Badge::class.java) }
                        Log.d(TAG, "Real-time badge update. Found ${badges.size} badges.")
                    }
                }

            // This cleanup block runs when the user navigates away, preventing memory leaks.
            onDispose {
                Log.d(TAG, "Removing badge listener.")
                listener.remove()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your Badges") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFFFFFDE7)
    ) { padding ->
        //  LazyColumn handles its own scrolling.
        if (badges.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No badges earned yet. Try meeting a goal!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(badges) { badge ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(badge.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(badge.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Earned on: ${badge.earnedDate}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}