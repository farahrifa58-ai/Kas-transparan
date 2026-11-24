package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "config",
    val userId: String = "",
    val groupName: String = "Kas Transparan",
    val adminName: String = "Admin Kas",
    val description: String = "",
    val initialBalance: Long = 0L,
    val currencyCode: String = "IDR",
    val language: String = "id", // "id" or "en" or other supported ISO codes
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val logoUrl: String = "",
    val publicKasId: String = "",
    val writeSecret: String = "",
    val publicAutoSync: Boolean = false,
    val publicLastSyncedAt: Long = 0L,
    val publicBaseUrl: String = DEFAULT_PUBLIC_BASE_URL,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_PUBLIC_BASE_URL =
            "https://ais-pre-cozyojnxqhrnof7n4jttop-79309788941.asia-northeast1.run.app"
    }
}
