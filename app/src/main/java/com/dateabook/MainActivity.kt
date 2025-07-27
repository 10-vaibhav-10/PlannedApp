@file:Suppress("DEPRECATION")

package com.dateabook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dateabook.ui.theme.AuthScreen
import com.dateabook.ui.theme.BookDetailScreen
import com.dateabook.ui.theme.HomeScreen
import com.dateabook.ui.theme.DateABookTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase
        Firebase.initialize(this)

        setContent {
            DateABookTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val navController = rememberNavController()

    // Safe null check for currentUser
    val startDestination = if (auth.currentUser?.uid != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                firestore = db
            )
        }

        composable(
            route = "bookDetail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            BookDetailScreen(
                navController = navController,
                bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            )
        }

        composable("signup") {
            Text("Signup Screen")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    DateABookTheme {
        AppNavigation()
    }
}