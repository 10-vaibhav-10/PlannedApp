package com.dateabook.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dateabook.ui.theme.BookItem
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// 1. Book Data Model
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val ownerId: String = "",
    val imageUrl: String = "",
    val condition: String = "Good",
    val status: String = "available" // available/requested/swapped
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    firestore: FirebaseFirestore,
    userId: String = Firebase.auth.currentUser?.uid ?: ""
) {
    // 2. Firestore Setup
    val db = Firebase.firestore
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 3. Fetch Books
    LaunchedEffect(Unit) {
        db.collection("books")
            .whereEqualTo("status", "available")
            .whereNotEqualTo("ownerId", userId) // Exclude user's own books
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                error?.let {
                    errorMessage = it.message
                    return@addSnapshotListener
                }
                books = snapshot?.toObjects(Book::class.java) ?: emptyList()
            }
    }

    // 4. UI Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Books") },
                // actions = { ProfileButton(navController) } // Uncomment to add later
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                books.isEmpty() -> {
                    Text(
                        text = "No books available nearby",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books) { book ->
                            BookItem(
                                book = book,
                                onBookClick = {
                                    navController.navigate("bookDetail/${book.id}")
                                },
                                onSwapRequest = {
                                    // Handle swap request
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}