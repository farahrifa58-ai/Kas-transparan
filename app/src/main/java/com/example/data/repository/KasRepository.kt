package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Transaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class KasRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTransactions(userId: String): Flow<List<Transaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Transaction::class.java).sortedByDescending { it.date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSettings(userId: String): Flow<AppSettings?> = flow {
        val path = "users/$userId/settings/config"
        emitAll(
            db.collection("users").document(userId).collection("settings").document("config")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AppSettings::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeAuditLogs(userId: String): Flow<List<AuditLog>> = flow {
        val path = "users/$userId/audit_logs"
        emitAll(
            db.collection("users").document(userId).collection("audit_logs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AuditLog::class.java).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveSettings(settings: AppSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("config")
        val payload = mapOf(
            "userId" to uid,
            "groupName" to settings.groupName,
            "adminName" to settings.adminName,
            "initialBalance" to settings.initialBalance,
            "currencyCode" to settings.currencyCode,
            "language" to settings.language,
            "themeMode" to settings.themeMode,
            "logoUrl" to settings.logoUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun addTransaction(transaction: Transaction): Result<String> {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("transactions").document(docId)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "type" to transaction.type,
            "title" to transaction.title,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "date" to transaction.date,
            "status" to "ACTIVE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.description.isNotBlank()) payload["description"] = transaction.description
        if (transaction.paymentMethod.isNotBlank()) payload["paymentMethod"] = transaction.paymentMethod
        if (transaction.receiptUrl.isNotBlank()) payload["receiptUrl"] = transaction.receiptUrl
        if (transaction.voidReason.isNotBlank()) payload["voidReason"] = transaction.voidReason

        return try {
            docRef.set(payload).await()
            // Record audit log
            addAuditLog(
                transactionId = docId,
                action = "CREATE",
                details = "Transaksi ${transaction.type} baru ditambahkan: ${transaction.title} sebesar ${transaction.amount}",
                reason = "Pencatatan kas baru"
            )
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID required for update" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "type" to transaction.type,
            "title" to transaction.title,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "date" to transaction.date,
            "status" to transaction.status,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.createdAt != null) {
            payload["createdAt"] = transaction.createdAt
        }
        if (transaction.description.isNotBlank()) payload["description"] = transaction.description
        if (transaction.paymentMethod.isNotBlank()) payload["paymentMethod"] = transaction.paymentMethod
        if (transaction.receiptUrl.isNotBlank()) payload["receiptUrl"] = transaction.receiptUrl
        if (transaction.voidReason.isNotBlank()) payload["voidReason"] = transaction.voidReason

        return try {
            docRef.set(payload).await()
            addAuditLog(
                transactionId = transaction.id,
                action = "UPDATE",
                details = "Koreksi data transaksi ${transaction.title}",
                reason = reason.ifBlank { "Koreksi data oleh admin" }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun voidTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val uid = requireUserId()
        require(transaction.id.isNotBlank()) { "Transaction ID required for void" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)

        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "type" to transaction.type,
            "title" to transaction.title,
            "amount" to transaction.amount,
            "category" to transaction.category,
            "date" to transaction.date,
            "status" to "VOID",
            "voidReason" to reason.ifBlank { "Dibatalkan oleh admin" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (transaction.createdAt != null) {
            payload["createdAt"] = transaction.createdAt
        }
        if (transaction.description.isNotBlank()) payload["description"] = transaction.description
        if (transaction.paymentMethod.isNotBlank()) payload["paymentMethod"] = transaction.paymentMethod
        if (transaction.receiptUrl.isNotBlank()) payload["receiptUrl"] = transaction.receiptUrl

        return try {
            docRef.set(payload).await()
            addAuditLog(
                transactionId = transaction.id,
                action = "VOID",
                details = "Pembatalan transaksi ${transaction.title} sebesar ${transaction.amount}",
                reason = reason.ifBlank { "Dibatalkan oleh admin" }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
        return try {
            docRef.delete().await()
            addAuditLog(
                transactionId = transactionId,
                action = "VOID",
                details = "Transaksi dihapus dari sistem",
                reason = "Penghapusan oleh admin"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun addAuditLog(
        transactionId: String,
        action: String,
        details: String,
        reason: String
    ) {
        val uid = requireUserId()
        val logId = UUID.randomUUID().toString()
        val logRef = db.collection("users").document(uid).collection("audit_logs").document(logId)
        val logData = mapOf(
            "userId" to uid,
            "transactionId" to transactionId,
            "action" to action,
            "details" to details,
            "reason" to reason,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            logRef.set(logData).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path)
        }
    }
}
