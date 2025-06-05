@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.content.Context
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
import com.fake.pennypal.utils.getCurrentUsername
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun BadgeScreen(navController: NavController) {
    val context = LocalContext.current
    val username = getCurrentUsername(context)


    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }

    LaunchedEffect(true) {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("users")
            .document(username)
            .collection("badges")
            .get()
            .await()

        badges = snapshot.documents.mapNotNull { it.toObject(Badge::class.java) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badges.isEmpty()) {
                Text("No badges earned yet.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(badges) { badge ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(badge.title, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(badge.description)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Earned on: ${badge.earnedDate}", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                            }
                        }
                    }
                }
            }
        }
    }
}
