package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val transactionId: String = "",
    val action: String = "CREATE", // "CREATE", "UPDATE", "VOID"
    val details: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
