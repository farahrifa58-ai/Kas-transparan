package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.Transaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class KasRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) {

    constructor(context: Context) : this(
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (_: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Exception) {
                FirebaseFirestore.getInstance()
            }
        },
        context.applicationContext
    )

    private val prefs: SharedPreferences? = context?.getSharedPreferences("kas_transparan_local_data", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth?
        get() = try {
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.auth
            } else if (FirebaseApp.getApps(db.app.applicationContext).isNotEmpty()) {
                Firebase.auth
            } else null
        } catch (_: Exception) {
            null
        }

    companion object {
        @Volatile
        private var instance: KasRepository? = null

        private val _sharedSettingsFlow = MutableStateFlow<AppSettings?>(null)
        private val _sharedTransactionsFlow = MutableStateFlow<List<Transaction>?>(null)
        private val _sharedAuditLogsFlow = MutableStateFlow<List<AuditLog>?>(null)

        fun getInstance(context: Context): KasRepository {
            return instance ?: synchronized(this) {
                instance ?: KasRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun requireUserId(): String {
        return auth?.currentUser?.uid ?: "admin_local"
    }

    private val _settingsFlow: MutableStateFlow<AppSettings>
        get() {
            if (_sharedSettingsFlow.value == null) {
                _sharedSettingsFlow.value = loadSettingsLocal()
            }
            return _sharedSettingsFlow as MutableStateFlow<AppSettings>
        }

    private val _transactionsFlow: MutableStateFlow<List<Transaction>>
        get() {
            if (_sharedTransactionsFlow.value == null) {
                _sharedTransactionsFlow.value = loadTransactionsLocal()
            }
            return _sharedTransactionsFlow as MutableStateFlow<List<Transaction>>
        }

    private val _auditLogsFlow: MutableStateFlow<List<AuditLog>>
        get() {
            if (_sharedAuditLogsFlow.value == null) {
                _sharedAuditLogsFlow.value = loadAuditLogsLocal()
            }
            return _sharedAuditLogsFlow as MutableStateFlow<List<AuditLog>>
        }

    fun loadSettingsLocal(): AppSettings {
        val jsonStr = prefs?.getString("settings_data", null) ?: return AppSettings()
        return try {
            val json = JSONObject(jsonStr)
            AppSettings(
                userId = json.optString("userId", "admin_local"),
                groupName = json.optString("groupName", "Kelompok Kas"),
                adminName = json.optString("adminName", "Admin Kas"),
                initialBalance = json.optLong("initialBalance", 0L),
                currencyCode = json.optString("currencyCode", "IDR"),
                language = json.optString("language", "id"),
                themeMode = json.optString("themeMode", "LIGHT"),
                logoUrl = json.optString("logoUrl", "")
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    private fun saveSettingsLocal(settings: AppSettings) {
        try {
            val json = JSONObject().apply {
                put("userId", settings.userId)
                put("groupName", settings.groupName)
                put("adminName", settings.adminName)
                put("initialBalance", settings.initialBalance)
                put("currencyCode", settings.currencyCode)
                put("language", settings.language)
                put("themeMode", settings.themeMode)
                put("logoUrl", settings.logoUrl)
            }
            prefs?.edit()?.putString("settings_data", json.toString())?.apply()
            _sharedSettingsFlow.value = settings
        } catch (e: Exception) {
            Log.e("KasRepository", "Error saving settings locally", e)
        }
    }

    private fun loadTransactionsLocal(): List<Transaction> {
        val jsonStr = prefs?.getString("transactions_data", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Transaction>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val createdAtMillis = obj.optLong("createdAtMillis", 0L)
                val createdAt = if (createdAtMillis > 0L) com.google.firebase.Timestamp(java.util.Date(createdAtMillis)) else null
                list.add(
                    Transaction(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", "admin_local"),
                        type = obj.optString("type", "INCOME"),
                        title = obj.optString("title", ""),
                        amount = obj.optLong("amount", 0L),
                        category = obj.optString("category", "Umum"),
                        date = obj.optString("date", ""),
                        description = obj.optString("description", ""),
                        paymentMethod = obj.optString("paymentMethod", "Tunai"),
                        receiptUrl = obj.optString("receiptUrl", ""),
                        status = obj.optString("status", "ACTIVE"),
                        voidReason = obj.optString("voidReason", ""),
                        createdAt = createdAt
                    )
                )
            }
            list.sortedWith(
                compareByDescending<Transaction> { it.date }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTransactionsLocal(list: List<Transaction>) {
        try {
            val array = JSONArray()
            for (tx in list) {
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("userId", tx.userId)
                    put("type", tx.type)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("date", tx.date)
                    put("description", tx.description)
                    put("paymentMethod", tx.paymentMethod)
                    put("receiptUrl", tx.receiptUrl)
                    put("status", tx.status)
                    put("voidReason", tx.voidReason)
                    put("createdAtMillis", tx.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("transactions_data", array.toString())?.apply()
            _transactionsFlow.value = list
        } catch (e: Exception) {
            Log.e("KasRepository", "Error saving transactions locally", e)
        }
    }

    private fun loadAuditLogsLocal(): List<AuditLog> {
        val jsonStr = prefs?.getString("audit_logs_data", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AuditLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AuditLog(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", "admin_local"),
                        transactionId = obj.optString("transactionId", ""),
                        action = obj.optString("action", "CREATE"),
                        details = obj.optString("details", ""),
                        reason = obj.optString("reason", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAuditLogsLocal(list: List<AuditLog>) {
        try {
            val array = JSONArray()
            for (log in list) {
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("userId", log.userId)
                    put("transactionId", log.transactionId)
                    put("action", log.action)
                    put("details", log.details)
                    put("reason", log.reason)
                }
                array.put(obj)
            }
            prefs?.edit()?.putString("audit_logs_data", array.toString())?.apply()
            _auditLogsFlow.value = list
        } catch (e: Exception) {
            Log.e("KasRepository", "Error saving audit logs locally", e)
        }
    }

    @Volatile
    private var isFirestoreTransactionsSyncing = false

    private fun syncFirestoreTransactionsInBackground(userId: String) {
        if (isFirestoreTransactionsSyncing) return
        val currentAuthUid = auth?.currentUser?.uid
        val targetUid = if (!currentAuthUid.isNullOrBlank()) currentAuthUid else if (userId.isNotBlank() && userId != "local_admin") userId else null
        if (targetUid == null) return

        isFirestoreTransactionsSyncing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(targetUid).collection("transactions")
                    .snapshots()
                    .collect { snapshot ->
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedWith(
                            compareByDescending<Transaction> { it.date }
                                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                        )
                        if (remoteList.isNotEmpty()) {
                            saveTransactionsLocal(remoteList)
                        }
                    }
            } catch (e: Exception) {
                Log.w("KasRepository", "Firestore observe transactions error: ${e.message}")
            } finally {
                isFirestoreTransactionsSyncing = false
            }
        }
    }

    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        syncFirestoreTransactionsInBackground(userId)
        return _transactionsFlow.asStateFlow()
    }

    @Volatile
    private var isFirestoreSettingsSyncing = false

    private fun syncFirestoreSettingsInBackground(userId: String) {
        if (isFirestoreSettingsSyncing) return
        val currentAuthUid = auth?.currentUser?.uid
        val targetUid = if (!currentAuthUid.isNullOrBlank()) currentAuthUid else if (userId.isNotBlank() && userId != "local_admin") userId else null
        if (targetUid == null) return

        isFirestoreSettingsSyncing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(targetUid).collection("settings").document("config")
                    .snapshots()
                    .collect { doc ->
                        if (doc.exists()) {
                            val remote = doc.toObject(AppSettings::class.java)
                            if (remote != null) {
                                saveSettingsLocal(remote)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w("KasRepository", "Firestore observe settings error: ${e.message}")
            } finally {
                isFirestoreSettingsSyncing = false
            }
        }
    }

    fun observeSettings(userId: String): Flow<AppSettings?> {
        syncFirestoreSettingsInBackground(userId)
        return _settingsFlow.asStateFlow()
    }

    fun observeAuditLogs(userId: String): Flow<List<AuditLog>> {
        return _auditLogsFlow.asStateFlow()
    }

    suspend fun saveSettings(settings: AppSettings): Result<Unit> {
        saveSettingsLocal(settings)

        // Asynchronously mirror to Firestore if available without blocking UI
        val uid = requireUserId()
        try {
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
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.set(payload).await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    suspend fun addInitialBalance(addedAmount: Long, note: String = ""): Result<Long> {
        val current = _settingsFlow.value
        val newBalance = (current.initialBalance + addedAmount).coerceAtLeast(0L)
        val updated = current.copy(initialBalance = newBalance)
        saveSettings(updated)
        addAuditLog(
            transactionId = "INITIAL_BALANCE",
            action = "UPDATE",
            details = "Penambahan saldo awal sebesar $addedAmount (Saldo awal baru: $newBalance)",
            reason = note.ifBlank { "Penambahan saldo awal kas" }
        )
        return Result.success(newBalance)
    }

    suspend fun setInitialBalance(newAmount: Long, note: String = ""): Result<Long> {
        val current = _settingsFlow.value
        val prevBalance = current.initialBalance
        val updated = current.copy(initialBalance = newAmount.coerceAtLeast(0L))
        saveSettings(updated)
        addAuditLog(
            transactionId = "INITIAL_BALANCE",
            action = "UPDATE",
            details = "Perubahan saldo awal dari $prevBalance menjadi $newAmount",
            reason = note.ifBlank { "Penyesuaian saldo awal kas" }
        )
        return Result.success(newAmount)
    }

    suspend fun addTransaction(transaction: Transaction): Result<String> {
        val uid = requireUserId()
        val docId = if (transaction.id.isNotBlank()) transaction.id else UUID.randomUUID().toString()
        val timestampNow = transaction.createdAt ?: com.google.firebase.Timestamp.now()
        val newTx = transaction.copy(
            id = docId,
            userId = uid,
            status = "ACTIVE",
            createdAt = timestampNow,
            updatedAt = timestampNow
        )
        val currentList = _transactionsFlow.value.toMutableList()
        currentList.add(0, newTx)
        val sortedList = currentList.sortedWith(
            compareByDescending<Transaction> { it.date }
                .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
        )
        saveTransactionsLocal(sortedList)

        // Record audit log
        addAuditLog(
            transactionId = docId,
            action = "CREATE",
            details = "Transaksi ${transaction.type} baru ditambahkan: ${transaction.title} sebesar ${transaction.amount}",
            reason = "Pencatatan kas baru"
        )

        // Mirror to Firestore in background
        if (auth?.currentUser != null) {
            try {
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

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        docRef.set(payload).await()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        return Result.success(docId)
    }

    suspend fun updateTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val uid = requireUserId()
        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = transaction
            saveTransactionsLocal(currentList)
        }
        addAuditLog(
            transactionId = transaction.id,
            action = "UPDATE",
            details = "Koreksi data transaksi ${transaction.title}",
            reason = reason.ifBlank { "Koreksi data oleh admin" }
        )

        try {
            val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.set(transaction).await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    suspend fun voidTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val uid = requireUserId()
        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                status = "VOID",
                voidReason = reason.ifBlank { "Dibatalkan oleh admin" }
            )
            saveTransactionsLocal(currentList)
        }
        addAuditLog(
            transactionId = transaction.id,
            action = "VOID",
            details = "Pembatalan transaksi ${transaction.title} sebesar ${transaction.amount}",
            reason = reason.ifBlank { "Dibatalkan oleh admin" }
        )

        try {
            val docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.update(mapOf("status" to "VOID", "voidReason" to reason)).await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val uid = requireUserId()
        val currentList = _transactionsFlow.value.toMutableList()
        currentList.removeAll { it.id == transactionId }
        saveTransactionsLocal(currentList)

        addAuditLog(
            transactionId = transactionId,
            action = "VOID",
            details = "Transaksi dihapus dari sistem",
            reason = "Penghapusan oleh admin"
        )

        try {
            val docRef = db.collection("users").document(uid).collection("transactions").document(transactionId)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.delete().await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        return Result.success(Unit)
    }

    private suspend fun addAuditLog(
        transactionId: String,
        action: String,
        details: String,
        reason: String
    ) {
        val uid = requireUserId()
        val logId = UUID.randomUUID().toString()
        val currentLogs = _auditLogsFlow.value.toMutableList()
        val newLog = AuditLog(
            id = logId,
            userId = uid,
            transactionId = transactionId,
            action = action,
            details = details,
            reason = reason
        )
        currentLogs.add(0, newLog)
        saveAuditLogsLocal(currentLogs)

        try {
            val logRef = db.collection("users").document(uid).collection("audit_logs").document(logId)
            val logData = mapOf(
                "userId" to uid,
                "transactionId" to transactionId,
                "action" to action,
                "details" to details,
                "reason" to reason,
                "createdAt" to FieldValue.serverTimestamp()
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    logRef.set(logData).await()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Kas Transparan")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val s = _settingsFlow.value
        val settingsObj = JSONObject().apply {
            put("userId", s.userId)
            put("groupName", s.groupName)
            put("adminName", s.adminName)
            put("initialBalance", s.initialBalance)
            put("currencyCode", s.currencyCode)
            put("language", s.language)
            put("themeMode", s.themeMode)
            put("logoUrl", s.logoUrl)
        }
        root.put("settings", settingsObj)

        val txArray = JSONArray()
        for (tx in _transactionsFlow.value) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("userId", tx.userId)
                put("type", tx.type)
                put("title", tx.title)
                put("amount", tx.amount)
                put("category", tx.category)
                put("date", tx.date)
                put("description", tx.description)
                put("paymentMethod", tx.paymentMethod)
                put("receiptUrl", tx.receiptUrl)
                put("status", tx.status)
                put("voidReason", tx.voidReason)
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val auditArray = JSONArray()
        for (log in _auditLogsFlow.value) {
            val obj = JSONObject().apply {
                put("id", log.id)
                put("userId", log.userId)
                put("transactionId", log.transactionId)
                put("action", log.action)
                put("details", log.details)
                put("reason", log.reason)
            }
            auditArray.put(obj)
        }
        root.put("auditLogs", auditArray)

        return root.toString(2)
    }

    fun createBackupFile(): java.io.File? {
        val ctx = context ?: return null
        return try {
            val dir = java.io.File(ctx.cacheDir, "backups").apply { mkdirs() }
            val timeStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val file = java.io.File(dir, "backup_kas_transparan_$timeStr.json")
            file.writeText(exportBackupJson())
            setLastBackupTimestamp(System.currentTimeMillis())
            file
        } catch (e: Exception) {
            Log.e("KasRepository", "Failed to create backup file", e)
            null
        }
    }

    fun restoreBackup(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)

            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val restoredSettings = AppSettings(
                    userId = sObj.optString("userId", "admin_local"),
                    groupName = sObj.optString("groupName", "Kelompok Kas"),
                    adminName = sObj.optString("adminName", "Admin Kas"),
                    initialBalance = sObj.optLong("initialBalance", 0L),
                    currencyCode = sObj.optString("currencyCode", "IDR"),
                    language = sObj.optString("language", "id"),
                    themeMode = sObj.optString("themeMode", "LIGHT"),
                    logoUrl = sObj.optString("logoUrl", "")
                )
                saveSettingsLocal(restoredSettings)
            }

            var restoredCount = 0
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                val list = mutableListOf<Transaction>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    list.add(
                        Transaction(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = obj.optString("userId", "admin_local"),
                            type = obj.optString("type", "INCOME"),
                            title = obj.optString("title", ""),
                            amount = obj.optLong("amount", 0L),
                            category = obj.optString("category", "Umum"),
                            date = obj.optString("date", ""),
                            description = obj.optString("description", ""),
                            paymentMethod = obj.optString("paymentMethod", "Tunai"),
                            receiptUrl = obj.optString("receiptUrl", ""),
                            status = obj.optString("status", "ACTIVE"),
                            voidReason = obj.optString("voidReason", "")
                        )
                    )
                }
                list.sortByDescending { it.date }
                saveTransactionsLocal(list)
                restoredCount = list.size
            }

            if (root.has("auditLogs")) {
                val logArray = root.getJSONArray("auditLogs")
                val list = mutableListOf<AuditLog>()
                for (i in 0 until logArray.length()) {
                    val obj = logArray.getJSONObject(i)
                    list.add(
                        AuditLog(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = obj.optString("userId", "admin_local"),
                            transactionId = obj.optString("transactionId", ""),
                            action = obj.optString("action", "CREATE"),
                            details = obj.optString("details", ""),
                            reason = obj.optString("reason", "")
                        )
                    )
                }
                saveAuditLogsLocal(list)
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e("KasRepository", "Failed to restore backup", e)
            Result.failure(e)
        }
    }

    fun getLastBackupTimestamp(): Long {
        return prefs?.getLong("last_google_drive_backup_time", 0L) ?: 0L
    }

    fun setLastBackupTimestamp(time: Long) {
        prefs?.edit()?.putLong("last_google_drive_backup_time", time)?.apply()
    }
}
