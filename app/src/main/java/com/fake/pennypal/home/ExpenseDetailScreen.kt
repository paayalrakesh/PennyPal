@file:OptIn(ExperimentalMaterial3Api::class)
package com.fake.pennypal.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun ExpenseDetailScreen(
    navController: NavController,
    date: String,
    category: String,
    description: String,
    amount: Double,
    photoUrl: String,
    currency: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details") },
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Date: $date", fontWeight = FontWeight.Bold)
            Text("Category: $category")
            Text("Amount: $currency ${"%.2f".format(amount)}")
            Text("Description: $description")

            if (photoUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = Uri.parse(photoUrl)),
                    contentDescription = "Expense Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No photo attached.")
            }
        }
    }
}
