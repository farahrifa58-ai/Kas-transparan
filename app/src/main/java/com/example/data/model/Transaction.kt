package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Transaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "INCOME", // "INCOME" or "EXPENSE"
    val title: String = "",
    val amount: Long = 0L,
    val category: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val description: String = "",
    val paymentMethod: String = "Tunai",
    val receiptUrl: String = "",
    val status: String = "ACTIVE", // "ACTIVE" or "VOID"
    val voidReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
