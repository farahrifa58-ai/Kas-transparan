package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "config",
    val userId: String = "",
    val groupName: String = "Kas Transparan",
    val adminName: String = "Admin Kas",
    val initialBalance: Long = 0L,
    val currencyCode: String = "IDR",
    val language: String = "id", // "id" or "en"
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val logoUrl: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
