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
import com.fake.pennypal.home.CategoryExpensesScreen
import com.fake.pennypal.home.AddIncomeScreen
import com.fake.pennypal.home.AddChoiceScreen
import com.fake.pennypal.home.ProfileScreen
import com.fake.pennypal.home.GoalScreen
import com.fake.pennypal.home.CategorySpendingPreviewScreen
import com.fake.pennypal.home.CategorySummaryScreen
import com.fake.pennypal.home.BadgeScreen
import com.fake.pennypal.ui.theme.PennyPalTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FirebaseFirestore.getInstance()
        val testData = hashMapOf("testField" to "Hello Firebase")

        db.collection("testCollection")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d("FirebaseTest", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseTest", "Error adding document", e)
            }
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

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
                        composable("addIncome") { AddIncomeScreen(navController) }
                        composable("addChoice") { AddChoiceScreen(navController) }
                        composable("manageCategories") { ManageCategoriesScreen(navController) }
                        composable("categoryExpenses/{categoryName}") { backStackEntry ->
                            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                            CategoryExpensesScreen(navController, categoryName) }
                        composable("profile") {ProfileScreen(navController)}
                        composable("goals") { GoalScreen(navController) }
                        composable("categorySpendingPreview") {
                            CategorySpendingPreviewScreen(navController)
                        }
                        composable("categorySummary") {
                            CategorySummaryScreen(navController)
                        }
                        composable("categoryExpenses/{categoryName}") { backStackEntry ->
                            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                            CategoryExpensesScreen(navController, categoryName)
                        }
                        composable("badgeScreen") {
                            BadgeScreen(navController)
                        }

                    }
                }
            }
        }
    }
}
