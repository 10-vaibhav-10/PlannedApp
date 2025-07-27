package com.dateabook

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.dateabook.Book
import com.dateabook.SwapRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    navController: NavController,
    bookId: String
) {
    // Firebase References
    val db = Firebase.firestore
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    // State Management
    var book by remember { mutableStateOf<Book?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRequested by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    // Fetch Book Data
    LaunchedEffect(bookId) {
        db.collection("books").document(bookId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                error = e?.message
                book = snapshot?.toObject(Book::class.java)?.apply {
                    id = snapshot.id // Ensure ID is set
                }

                // Check existing requests
                db.collection("swaps")
                    .whereEqualTo("bookId", bookId)
                    .whereEqualTo("requesterId", currentUserId)
                    .get()
                    .addOnSuccessListener {
                        isRequested = !it.isEmpty
                    }
            }
    }

    // Handle Swap Request
    fun requestSwap() {
        book?.let { currentBook ->
            val swap = SwapRequest(
                bookId = bookId,
                bookTitle = currentBook.title,
                requesterId = currentUserId,
                ownerId = currentBook.ownerId,
                status = "pending",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            db.runTransaction { transaction ->
                val bookRef = db.collection("books").document(bookId)
                val bookDoc = transaction.get(bookRef)

                if (bookDoc["status"] != "available") {
                    throw Exception("Book no longer available")
                }

                val swapRef = db.collection("swaps").document()
                transaction.set(swapRef, swap)
                transaction.update(bookRef, "status", "requested")
            }.addOnCompleteListener { task ->
                isRequested = task.isSuccessful
                showDialog = task.isSuccessful
            }
        }
    }

    // UI Scaffold
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = book?.title ?: stringResource(R.string.loading),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> FullScreenLoading()
            error != null -> FullScreenError(error!!)
            book == null -> FullScreenError(stringResource(R.string.book_not_found))
            else -> BookDetailContent(
                book = book!!,
                isRequested = isRequested,
                onRequestSwap = ::requestSwap,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.success)) },
            text = { Text(stringResource(R.string.swap_request_sent)) },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    isRequested: Boolean,
    onRequestSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Book Cover Image
        Image(
            painter = rememberAsyncImagePainter(book.imageUrl),
            contentDescription = stringResource(R.string.book_cover, book.title),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop
        )

        // Title Section
        Column {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.by_author, book.author),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text(book.condition) }
            )
            AssistChip(
                onClick = {},
                label = { Text(book.status.replaceFirstChar { it.uppercase() }) }
            )
        }

        // Description
        Text(
            text = book.description.ifEmpty { stringResource(R.string.no_description) },
            style = MaterialTheme.typography.bodyMedium
        )

        // Action Button
        Button(
            onClick = onRequestSwap,
            enabled = !isRequested,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                text = if (isRequested) stringResource(R.string.request_pending)
                else stringResource(R.string.request_swap)
            )
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FullScreenError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}