package com.fake.pennypal.auth
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to Penny Pal!",
                fontSize = 28.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Add navigation to other parts of the app later
                }
            ) {
                Text("Explore App")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // For demo, navigate back to login
                    navController.navigate("login")
                }
            ) {
                Text("Logout")
            }
        }
    }
}