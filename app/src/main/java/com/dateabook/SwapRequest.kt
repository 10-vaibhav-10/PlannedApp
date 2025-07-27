package com.dateabook

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SwapRequest(
    @DocumentId val id: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val requesterId: String = "",
    val ownerId: String = "",
    val status: String = "pending", // pending/accepted/rejected
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)