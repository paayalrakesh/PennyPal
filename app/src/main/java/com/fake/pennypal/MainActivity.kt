package com.fake.pennypal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fake.pennypal.splash.SplashScreen
import com.fake.pennypal.auth.LoginScreen
import com.fake.pennypal.home.HomeScreen
import com.fake.pennypal.auth.SignUpScreen
import com.fake.pennypal.home.AddExpenseScreen
import com.fake.pennypal.home.ManageCategoriesScreen
import com.fake.pennypal.ui.theme.PennyPalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PennyPalTheme {
                val navController = rememberNavController() // Set up navigation controller
                @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    // Set up NavHost to manage navigation between screens
                    NavHost(
                        navController = navController,
                        startDestination = "splash" // Start at splash screen
                    ) {
                        composable("splash") { SplashScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("signup") { SignUpScreen(navController) }
                        composable("home") { HomeScreen(navController) }
                        composable("addExpense") { AddExpenseScreen(navController) }
                        composable("manageCategories") { ManageCategoriesScreen(navController) }


                        // Add more composable routes here (home, categories, etc.)
                    }
                }
            }
        }
    }
}
