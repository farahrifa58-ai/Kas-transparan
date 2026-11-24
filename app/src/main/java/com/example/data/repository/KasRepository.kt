package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.PublicAuthRequiredException
import com.example.data.model.PublicKasHelper
import com.example.data.model.PublicKasReport
import com.example.data.model.PublicKasTransaction
import com.example.data.model.PublicKasUiState
import com.example.data.model.Transaction
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
        FirebaseFirestore.getInstance(
            FirebaseApp.getInstance(),
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("kas_transparan_local_data", Context.MODE_PRIVATE)

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

    fun getSignedInTreasurerEmail(): String? {
        return auth?.currentUser?.email ?: auth?.currentUser?.displayName
    }

    fun isTreasurerSignedIn(): Boolean {
        return auth?.currentUser != null
    }

    private val _settingsFlow: MutableStateFlow<AppSettings>
        get() {
            if (_sharedSettingsFlow.value == null) {
                _sharedSettingsFlow.value = loadSettingsLocal()
            }
            @Suppress("UNCHECKED_CAST")
            return _sharedSettingsFlow as MutableStateFlow<AppSettings>
        }

    private val _transactionsFlow: MutableStateFlow<List<Transaction>>
        get() {
            if (_sharedTransactionsFlow.value == null) {
                _sharedTransactionsFlow.value = loadTransactionsLocal()
            }
            @Suppress("UNCHECKED_CAST")
            return _sharedTransactionsFlow as MutableStateFlow<List<Transaction>>
        }

    private val _auditLogsFlow: MutableStateFlow<List<AuditLog>>
        get() {
            if (_sharedAuditLogsFlow.value == null) {
                _sharedAuditLogsFlow.value = loadAuditLogsLocal()
            }
            @Suppress("UNCHECKED_CAST")
            return _sharedAuditLogsFlow as MutableStateFlow<List<AuditLog>>
        }

    fun loadSettingsLocal(): AppSettings {
        val jsonStr = prefs?.getString("settings_data", null)
        val parsed = if (jsonStr == null) {
            AppSettings()
        } else {
            try {
                val json = JSONObject(jsonStr)
                AppSettings(
                    userId = json.optString("userId", "admin_local"),
                    groupName = json.optString("groupName", "Kelompok Kas"),
                    adminName = json.optString("adminName", "Admin Kas"),
                    description = json.optString("description", ""),
                    initialBalance = json.optLong("initialBalance", 0L),
                    currencyCode = json.optString("currencyCode", "IDR"),
                    language = json.optString("language", "id"),
                    themeMode = json.optString("themeMode", "LIGHT"),
                    logoUrl = json.optString("logoUrl", ""),
                    publicKasId = json.optString("publicKasId", ""),
                    writeSecret = json.optString("writeSecret", ""),
                    publicAutoSync = json.optBoolean("publicAutoSync", false),
                    publicLastSyncedAt = json.optLong("publicLastSyncedAt", 0L),
                    publicBaseUrl = PublicKasHelper.sanitizePublicBaseUrl(
                        json.optString("publicBaseUrl", AppSettings.PUBLIC_WEB_BASE_URL)
                    )
                )
            } catch (_: Exception) {
                AppSettings()
            }
        }

        // Auto-generate and permanently persist unique Public Kas ID + 24-char local writeSecret on first run
        // Also migrate any legacy preview/localhost base URL to the fixed production PUBLIC_WEB_BASE_URL.
        val needsId = !PublicKasHelper.isValidPublicKasId(parsed.publicKasId)
        val needsSecret = !PublicKasHelper.isValidWriteSecret(parsed.writeSecret)
        val needsBaseMigration = parsed.publicBaseUrl != AppSettings.PUBLIC_WEB_BASE_URL
        return if (needsId || needsSecret || needsBaseMigration) {
            val initialized = parsed.copy(
                publicKasId = if (needsId) PublicKasHelper.generatePublicKasId() else parsed.publicKasId,
                writeSecret = if (needsSecret) PublicKasHelper.generateWriteSecret() else parsed.writeSecret,
                publicBaseUrl = AppSettings.PUBLIC_WEB_BASE_URL
            )
            saveSettingsLocal(initialized)
            initialized
        } else {
            parsed
        }
    }

    private fun saveSettingsLocal(settings: AppSettings) {
        try {
            val validPublicId = if (PublicKasHelper.isValidPublicKasId(settings.publicKasId)) {
                settings.publicKasId
            } else {
                PublicKasHelper.generatePublicKasId()
            }
            val validWriteSecret = if (PublicKasHelper.isValidWriteSecret(settings.writeSecret)) {
                settings.writeSecret
            } else {
                PublicKasHelper.generateWriteSecret()
            }
            val safeSettings = settings.copy(
                publicKasId = validPublicId,
                writeSecret = validWriteSecret,
                publicBaseUrl = PublicKasHelper.sanitizePublicBaseUrl(settings.publicBaseUrl)
            )
            val json = JSONObject().apply {
                put("userId", safeSettings.userId)
                put("groupName", safeSettings.groupName)
                put("adminName", safeSettings.adminName)
                put("description", safeSettings.description)
                put("initialBalance", safeSettings.initialBalance)
                put("currencyCode", safeSettings.currencyCode)
                put("language", safeSettings.language)
                put("themeMode", safeSettings.themeMode)
                put("logoUrl", safeSettings.logoUrl)
                put("publicKasId", safeSettings.publicKasId)
                put("writeSecret", safeSettings.writeSecret)
                put("publicAutoSync", safeSettings.publicAutoSync)
                put("publicLastSyncedAt", safeSettings.publicLastSyncedAt)
                put("publicBaseUrl", safeSettings.publicBaseUrl)
            }
            prefs?.edit()?.putString("settings_data", json.toString())?.apply()
            _sharedSettingsFlow.value = safeSettings
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
                val createdAt = if (createdAtMillis > 0L) Timestamp(java.util.Date(createdAtMillis)) else null
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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
        val targetUid = if (!currentAuthUid.isNullOrBlank()) currentAuthUid else null
        if (targetUid == null) return

        isFirestoreTransactionsSyncing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(targetUid).collection("transactions")
                    .snapshots()
                    .collect { snapshot ->
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Transaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                                    ?.copy(id = doc.id)
                            } catch (_: Exception) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "users/$targetUid/transactions")
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
        val targetUid = if (!currentAuthUid.isNullOrBlank()) currentAuthUid else null
        if (targetUid == null) return

        isFirestoreSettingsSyncing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users").document(targetUid).collection("settings").document("config")
                    .snapshots()
                    .collect { doc ->
                        if (doc.exists()) {
                            val remote = doc.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            if (remote != null) {
                                val currentLocal = _settingsFlow.value
                                val mergedId = if (PublicKasHelper.isValidPublicKasId(remote.publicKasId)) {
                                    remote.publicKasId
                                } else {
                                    currentLocal.publicKasId
                                }
                                val mergedSecret = if (PublicKasHelper.isValidWriteSecret(remote.writeSecret)) {
                                    remote.writeSecret
                                } else {
                                    currentLocal.writeSecret
                                }
                                saveSettingsLocal(
                                    remote.copy(
                                        publicKasId = mergedId,
                                        writeSecret = mergedSecret,
                                        publicLastSyncedAt = maxOf(remote.publicLastSyncedAt, currentLocal.publicLastSyncedAt)
                                    )
                                )
                            }
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "users/$targetUid/settings/config")
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

    // --- PUBLIC 1-LINK REAL-TIME TRANSPARENCY (public_kas/{publicKasId}) ---

    suspend fun publishPublicKasReport(): Result<Long> {
        val ctx = context
        if (ctx != null && !PublicKasHelper.isOnline(ctx)) {
            return Result.failure(
                Exception("Perangkat sedang offline. Hubungkan ke internet untuk memperbarui Laporan Publik.")
            )
        }

        val currentSettings = _settingsFlow.value
        val publicKasId = if (PublicKasHelper.isValidPublicKasId(currentSettings.publicKasId)) {
            currentSettings.publicKasId
        } else {
            PublicKasHelper.generatePublicKasId()
        }
        val writeSecret = if (PublicKasHelper.isValidWriteSecret(currentSettings.writeSecret)) {
            currentSettings.writeSecret
        } else {
            PublicKasHelper.generateWriteSecret()
        }
        if (publicKasId != currentSettings.publicKasId || writeSecret != currentSettings.writeSecret) {
            saveSettingsLocal(
                currentSettings.copy(
                    publicKasId = publicKasId,
                    writeSecret = writeSecret
                )
            )
        }

        val activeTransactions = _transactionsFlow.value
            .filter { it.status == "ACTIVE" }
            .sortedWith(
                compareByDescending<Transaction> { it.date }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
            )

        val totalIncome = activeTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = activeTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalBalance = currentSettings.initialBalance + totalIncome - totalExpense

        val publicTransactions = activeTransactions.take(2000).map { tx ->
            mapOf(
                "id" to tx.id,
                "title" to tx.title.take(200),
                "category" to tx.category.ifBlank { "Umum" }.take(100),
                "type" to if (tx.type == "INCOME") "income" else "expense",
                "amount" to tx.amount.coerceAtLeast(0L),
                "date" to tx.date,
                "notes" to tx.description.take(500)
            )
        }

        val payload = hashMapOf<String, Any>(
            "writeSecret" to writeSecret,
            "ownerUid" to (auth?.currentUser?.uid ?: ""),
            "groupName" to currentSettings.groupName.ifBlank { "Kas Transparan" }.take(120),
            "description" to currentSettings.description.take(500),
            "currencyCode" to currentSettings.currencyCode.ifBlank { "IDR" }.take(5),
            "initialBalance" to currentSettings.initialBalance,
            "totalBalance" to totalBalance,
            "totalIncome" to totalIncome.coerceAtLeast(0L),
            "totalExpense" to totalExpense.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
            "transactions" to publicTransactions
        )

        val path = "public_kas/$publicKasId"
        return try {
            db.collection("public_kas").document(publicKasId).set(payload).await()
            val syncedNow = System.currentTimeMillis()
            saveSettingsLocal(
                _settingsFlow.value.copy(
                    publicKasId = publicKasId,
                    writeSecret = writeSecret,
                    publicLastSyncedAt = syncedNow
                )
            )
            Result.success(syncedNow)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            val friendlyMsg = when {
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Kunci pengaman kelompok tidak cocok untuk ID Kas ini. Pulihkan cadangan data bendahara jika Anda baru berganti HP."
                e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Server tidak dapat dijangkau. Periksa koneksi internet Anda."
                else -> e.localizedMessage ?: "Gagal menyinkronkan laporan ke web."
            }
            Result.failure(Exception(friendlyMsg, e))
        }
    }

    private fun triggerAutoSyncIfEnabled() {
        val currentSettings = _settingsFlow.value
        val ctx = context ?: return
        if (!currentSettings.publicAutoSync) return
        if (!PublicKasHelper.isOnline(ctx)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                publishPublicKasReport()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("KasRepository", "Background public auto-sync skipped: ${e.message}")
            }
        }
    }

    fun observePublicKasReport(rawKasId: String): Flow<PublicKasUiState> {
        val cleanId = PublicKasHelper.normalizePublicKasId(rawKasId)
        if (!PublicKasHelper.isValidPublicKasId(cleanId)) {
            return flowOf(PublicKasUiState.NotFound(rawKasId.trim()))
        }

        val path = "public_kas/$cleanId"
        return db.collection("public_kas")
            .document(cleanId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    PublicKasUiState.NotFound(cleanId)
                } else {
                    val data = snapshot.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: return@map PublicKasUiState.NotFound(cleanId)

                    val rawTxList = data["transactions"] as? List<*> ?: emptyList<Any>()
                    val parsedTransactions = rawTxList.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val rawType = (map["type"] as? String)?.lowercase() ?: "income"
                        PublicKasTransaction(
                            id = (map["id"] as? String).orEmpty(),
                            title = (map["title"] as? String).orEmpty(),
                            category = (map["category"] as? String).orEmpty().ifBlank { "Umum" },
                            type = if (rawType == "expense") "expense" else "income",
                            amount = (map["amount"] as? Number)?.toLong() ?: 0L,
                            date = (map["date"] as? String).orEmpty(),
                            notes = (map["notes"] as? String).orEmpty()
                        )
                    }

                    val updatedAtMillis = when (val ts = data["updatedAt"]) {
                        is Timestamp -> ts.toDate().time
                        is Number -> ts.toLong()
                        else -> 0L
                    }

                    PublicKasUiState.Success(
                        PublicKasReport(
                            publicKasId = cleanId,
                            ownerUid = (data["ownerUid"] as? String).orEmpty(),
                            groupName = (data["groupName"] as? String).orEmpty().ifBlank { "Kas Transparan" },
                            description = (data["description"] as? String).orEmpty(),
                            currencyCode = (data["currencyCode"] as? String).orEmpty().ifBlank { "IDR" },
                            initialBalance = (data["initialBalance"] as? Number)?.toLong() ?: 0L,
                            totalBalance = (data["totalBalance"] as? Number)?.toLong() ?: 0L,
                            totalIncome = (data["totalIncome"] as? Number)?.toLong() ?: 0L,
                            totalExpense = (data["totalExpense"] as? Number)?.toLong() ?: 0L,
                            updatedAtMillis = updatedAtMillis,
                            transactions = parsedTransactions
                        )
                    )
                }
            }
            .onStart { emit(PublicKasUiState.Loading) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (error is CancellationException && firestoreError == null) {
                    throw error
                }
                val root = firestoreError ?: error
                handleFirestoreError(root, OperationType.GET, path)
                emit(
                    PublicKasUiState.Error(
                        root.localizedMessage ?: "Gagal memuat laporan publik. Periksa koneksi internet Anda."
                    )
                )
            }
    }

    // --- OFFLINE-FIRST TREASURER MUTATIONS ---

    suspend fun saveSettings(settings: AppSettings, triggerPublicSync: Boolean = true): Result<Unit> {
        val preservedId = if (PublicKasHelper.isValidPublicKasId(settings.publicKasId)) {
            settings.publicKasId
        } else {
            _settingsFlow.value.publicKasId
        }
        val preservedSecret = if (PublicKasHelper.isValidWriteSecret(settings.writeSecret)) {
            settings.writeSecret
        } else {
            _settingsFlow.value.writeSecret
        }
        val mergedSettings = settings.copy(
            publicKasId = preservedId,
            writeSecret = preservedSecret,
            publicLastSyncedAt = maxOf(settings.publicLastSyncedAt, _settingsFlow.value.publicLastSyncedAt),
            publicBaseUrl = PublicKasHelper.sanitizePublicBaseUrl(settings.publicBaseUrl)
        )
        saveSettingsLocal(mergedSettings)

        if (triggerPublicSync) {
            triggerAutoSyncIfEnabled()
        }

        // Asynchronously mirror to private Firestore if authenticated without blocking offline UI
        val currentAuthUid = auth?.currentUser?.uid
        if (!currentAuthUid.isNullOrBlank()) {
            val path = "users/$currentAuthUid/settings/config"
            val docRef = db.collection("users").document(currentAuthUid).collection("settings").document("config")
            val payload = mapOf(
                "userId" to currentAuthUid,
                "groupName" to mergedSettings.groupName.ifBlank { "Kas Transparan" }.take(200),
                "adminName" to mergedSettings.adminName.take(100),
                "description" to mergedSettings.description.take(500),
                "initialBalance" to mergedSettings.initialBalance,
                "currencyCode" to mergedSettings.currencyCode.ifBlank { "IDR" }.take(5),
                "language" to mergedSettings.language.ifBlank { "id" }.take(10),
                "themeMode" to mergedSettings.themeMode,
                "logoUrl" to mergedSettings.logoUrl.take(2000),
                "publicKasId" to mergedSettings.publicKasId,
                "writeSecret" to mergedSettings.writeSecret,
                "publicAutoSync" to mergedSettings.publicAutoSync,
                "publicLastSyncedAt" to mergedSettings.publicLastSyncedAt,
                "publicBaseUrl" to mergedSettings.publicBaseUrl.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.set(payload).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, path)
                }
            }
        }

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
        val timestampNow = transaction.createdAt ?: Timestamp.now()
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
        triggerAutoSyncIfEnabled()

        // Record audit log
        addAuditLog(
            transactionId = docId,
            action = "CREATE",
            details = "Transaksi ${transaction.type} baru ditambahkan: ${transaction.title} sebesar ${transaction.amount}",
            reason = "Pencatatan kas baru"
        )

        // Mirror to private Firestore in background if signed in
        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            val path = "users/$authUid/transactions/$docId"
            val docRef = db.collection("users").document(authUid).collection("transactions").document(docId)
            val payload = mutableMapOf<String, Any>(
                "userId" to authUid,
                "type" to transaction.type,
                "title" to transaction.title.take(200),
                "amount" to transaction.amount.coerceAtLeast(0L),
                "category" to transaction.category.ifBlank { "Umum" }.take(100),
                "date" to transaction.date,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (transaction.description.isNotBlank()) payload["description"] = transaction.description.take(1000)
            if (transaction.paymentMethod.isNotBlank()) payload["paymentMethod"] = transaction.paymentMethod.take(100)
            if (transaction.receiptUrl.isNotBlank()) payload["receiptUrl"] = transaction.receiptUrl.take(2000)
            if (transaction.voidReason.isNotBlank()) payload["voidReason"] = transaction.voidReason.take(500)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.set(payload).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
        }

        return Result.success(docId)
    }

    suspend fun updateTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = transaction
            saveTransactionsLocal(currentList)
        }
        triggerAutoSyncIfEnabled()

        addAuditLog(
            transactionId = transaction.id,
            action = "UPDATE",
            details = "Koreksi data transaksi ${transaction.title}",
            reason = reason.ifBlank { "Koreksi data oleh admin" }
        )

        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            val path = "users/$authUid/transactions/${transaction.id}"
            val docRef = db.collection("users").document(authUid).collection("transactions").document(transaction.id)
            val updates = mapOf(
                "type" to transaction.type,
                "title" to transaction.title.take(200),
                "amount" to transaction.amount.coerceAtLeast(0L),
                "category" to transaction.category.ifBlank { "Umum" }.take(100),
                "date" to transaction.date,
                "description" to transaction.description.take(1000),
                "paymentMethod" to transaction.paymentMethod.take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.update(updates).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
            }
        }

        return Result.success(Unit)
    }

    suspend fun voidTransaction(transaction: Transaction, reason: String): Result<Unit> {
        val currentList = _transactionsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(
                status = "VOID",
                voidReason = reason.ifBlank { "Dibatalkan oleh admin" }
            )
            saveTransactionsLocal(currentList)
        }
        triggerAutoSyncIfEnabled()

        addAuditLog(
            transactionId = transaction.id,
            action = "VOID",
            details = "Pembatalan transaksi ${transaction.title} sebesar ${transaction.amount}",
            reason = reason.ifBlank { "Dibatalkan oleh admin" }
        )

        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            val path = "users/$authUid/transactions/${transaction.id}"
            val docRef = db.collection("users").document(authUid).collection("transactions").document(transaction.id)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.update(
                        mapOf(
                            "status" to "VOID",
                            "voidReason" to reason.take(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
            }
        }

        return Result.success(Unit)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        val currentList = _transactionsFlow.value.toMutableList()
        currentList.removeAll { it.id == transactionId }
        saveTransactionsLocal(currentList)
        triggerAutoSyncIfEnabled()

        addAuditLog(
            transactionId = transactionId,
            action = "VOID",
            details = "Transaksi dihapus dari sistem",
            reason = "Penghapusan oleh admin"
        )

        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            val path = "users/$authUid/transactions/$transactionId"
            val docRef = db.collection("users").document(authUid).collection("transactions").document(transactionId)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    docRef.delete().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.DELETE, path)
                }
            }
        }

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

        val authUid = auth?.currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            val path = "users/$authUid/audit_logs/$logId"
            val logRef = db.collection("users").document(authUid).collection("audit_logs").document(logId)
            val logData = mapOf(
                "userId" to authUid,
                "transactionId" to transactionId.take(128),
                "action" to action,
                "details" to details.take(2000),
                "reason" to reason.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    logRef.set(logData).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, path)
                }
            }
        }
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "Kas Transparan")
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val s = _settingsFlow.value
        val settingsObj = JSONObject().apply {
            put("userId", s.userId)
            put("groupName", s.groupName)
            put("adminName", s.adminName)
            put("description", s.description)
            put("initialBalance", s.initialBalance)
            put("currencyCode", s.currencyCode)
            put("language", s.language)
            put("themeMode", s.themeMode)
            put("logoUrl", s.logoUrl)
            put("publicKasId", s.publicKasId)
            put("writeSecret", s.writeSecret)
            put("publicAutoSync", s.publicAutoSync)
            put("publicLastSyncedAt", s.publicLastSyncedAt)
            put("publicBaseUrl", s.publicBaseUrl)
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
                val existingPublicId = _settingsFlow.value.publicKasId
                val existingWriteSecret = _settingsFlow.value.writeSecret
                val backupPublicId = sObj.optString("publicKasId", "")
                val backupWriteSecret = sObj.optString("writeSecret", "")
                val restoredSettings = AppSettings(
                    userId = sObj.optString("userId", "admin_local"),
                    groupName = sObj.optString("groupName", "Kelompok Kas"),
                    adminName = sObj.optString("adminName", "Admin Kas"),
                    description = sObj.optString("description", ""),
                    initialBalance = sObj.optLong("initialBalance", 0L),
                    currencyCode = sObj.optString("currencyCode", "IDR"),
                    language = sObj.optString("language", "id"),
                    themeMode = sObj.optString("themeMode", "LIGHT"),
                    logoUrl = sObj.optString("logoUrl", ""),
                    publicKasId = if (PublicKasHelper.isValidPublicKasId(backupPublicId)) backupPublicId else existingPublicId,
                    writeSecret = if (PublicKasHelper.isValidWriteSecret(backupWriteSecret)) backupWriteSecret else existingWriteSecret,
                    publicAutoSync = sObj.optBoolean("publicAutoSync", false),
                    publicLastSyncedAt = sObj.optLong("publicLastSyncedAt", 0L),
                    publicBaseUrl = PublicKasHelper.sanitizePublicBaseUrl(
                        sObj.optString("publicBaseUrl", AppSettings.PUBLIC_WEB_BASE_URL)
                    )
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
