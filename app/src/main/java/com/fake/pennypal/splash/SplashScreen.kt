package com.fake.pennypal.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.fake.pennypal.R

@Composable
fun SplashScreen(navController: NavController) {
    // Manage visibility state of splash screen content
    var visible by remember { mutableStateOf(true) }

    // LaunchedEffect runs side effects on composition
    LaunchedEffect(Unit) {
        delay(2000) // Display splash for 2 seconds
        visible = false // Start fade o
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true } // Remove splash from back stack
        }
    }

    // Composable UI layout with fade-out animation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF59D)), // Light yellow background
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            exit = fadeOut() // Animate fade out
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_wallet_logo), // Replace with your logo
                    contentDescription = "Penny Pal Logo",
                    modifier = Modifier.size(150.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Penny Pal",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}