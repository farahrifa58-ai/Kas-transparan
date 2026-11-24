package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.example.R
import com.example.data.model.PublicKasHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

data class LocalLicenseInfo(
    val activated: Boolean = false,
    val licenseCode: String = "",
    val deviceId: String = "",
    val appId: String = "kas_transparan",
    val activatedAtMillis: Long = 0L
)

sealed interface LicenseGateState {
    data object Checking : LicenseGateState
    data class Activated(val info: LocalLicenseInfo) : LicenseGateState
    data class Unlicensed(
        val deviceId: String,
        val infoMessage: String? = null
    ) : LicenseGateState
}

sealed interface LicenseActivationResult {
    data class Success(val info: LocalLicenseInfo) : LicenseActivationResult
    data class Failure(
        val message: String,
        val errorDetail: String? = null
    ) : LicenseActivationResult
}

/**
 * Manages 1-device license activation using the EXACT SAME primary Firestore instance (`db`)
 * configured with `R.string.firestore_database_id` (`ai-studio-android-kastrans-...`) as `public_kas`.
 */
class LicenseRepository private constructor(private val appContext: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        appContext.getString(R.string.firestore_database_id)
    )

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "LicenseRepository"
        private const val PREFS_NAME = "kas_transparan_license_storage"
        private const val KEY_DEVICE_ID = "kas_transparan_device_id"
        private const val KEY_LICENSE_JSON = "kas_transparan_license"
        private const val COLLECTION_ACTIVATION_CODES = "activation_codes"

        private val VALID_CODE_REGEX = Regex("^[A-Z0-9][A-Z0-9_-]{2,63}$")
        private val REVOKED_STATUSES = setOf("revoked", "blocked", "disabled", "suspended")
        private val UNUSED_STATUSES = setOf("", "unused", "available", "ready")

        @Volatile
        private var instance: LicenseRepository? = null

        fun getInstance(context: Context): LicenseRepository {
            return instance ?: synchronized(this) {
                instance ?: LicenseRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    val productAppId: String
        get() = appContext.getString(R.string.license_product_app_id)
            .trim()
            .ifBlank { "kas_transparan" }

    val adminEmail: String
        get() = appContext.getString(R.string.license_admin_email)
            .trim()
            .ifBlank { "rendykurniawan0311@gmail.com" }

    /**
     * Returns a stable, reinstall-persistent device ID on Android (Settings.Secure.ANDROID_ID),
     * with a local SharedPreferences backup under `kas_transparan_device_id`.
     */
    @SuppressLint("HardwareIds")
    fun getOrCreateDeviceId(): String {
        val stored = prefs.getString(KEY_DEVICE_ID, null)?.trim()

        val androidId = try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.equals("9774d56d682e549c", ignoreCase = true) }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read ANDROID_ID", e)
            null
        }

        val resolved = when {
            !androidId.isNullOrBlank() -> androidId.uppercase(Locale.ROOT)
            !stored.isNullOrBlank() -> stored.uppercase(Locale.ROOT)
            else -> "DEV-${UUID.randomUUID().toString().replace("-", "").take(16).uppercase(Locale.ROOT)}"
        }

        if (stored != resolved) {
            prefs.edit().putString(KEY_DEVICE_ID, resolved).apply()
        }
        return resolved
    }

    /**
     * Reads locally persisted activation state (`kas_transparan_license`) for instant offline-first startup.
     */
    fun getLocalLicense(): LocalLicenseInfo? {
        val raw = prefs.getString(KEY_LICENSE_JSON, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val activated = obj.optBoolean("activated", false)
            val code = obj.optString("licenseCode", "").trim()
            val deviceId = obj.optString("deviceId", "").trim()
            val appId = obj.optString("appId", productAppId).trim()
            val activatedAt = obj.optLong("activatedAtMillis", 0L)
            if (activated && code.isNotBlank() && deviceId.isNotBlank()) {
                LocalLicenseInfo(
                    activated = true,
                    licenseCode = code,
                    deviceId = deviceId,
                    appId = appId,
                    activatedAtMillis = activatedAt
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Corrupted local license cache", e)
            null
        }
    }

    private fun saveLocalLicense(info: LocalLicenseInfo) {
        val json = JSONObject().apply {
            put("activated", info.activated)
            put("licenseCode", info.licenseCode)
            put("deviceId", info.deviceId)
            put("appId", info.appId)
            put("activatedAtMillis", info.activatedAtMillis)
        }
        prefs.edit()
            .putString(KEY_LICENSE_JSON, json.toString())
            .putString(KEY_DEVICE_ID, info.deviceId)
            .apply()
    }

    fun clearLocalLicense() {
        prefs.edit().remove(KEY_LICENSE_JSON).apply()
    }

    private fun isMatchingAppId(docAppId: String?): Boolean {
        val cleaned = docAppId?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (cleaned.isBlank()) return true // Universal code without app restriction
        val target = productAppId.lowercase(Locale.ROOT)
        return cleaned == target ||
            cleaned == "kas_transparan" ||
            cleaned == "kas-transparan" ||
            cleaned == "kastransparan" ||
            cleaned == "kas transparan"
    }

    /**
     * Checks local cache first (offline-first). If not yet cached locally, automatically queries
     * `activation_codes` in the primary Firestore database for an active record bound to
     * `currentDeviceId` (Auto-Restore after reinstall / clear cache).
     */
    suspend fun checkOrAutoRestoreLicense(): LicenseGateState = withContext(Dispatchers.IO) {
        val currentDeviceId = getOrCreateDeviceId()

        // 1. Offline-first local check
        val local = getLocalLicense()
        if (local != null && local.activated && local.deviceId.equals(currentDeviceId, ignoreCase = true)) {
            return@withContext LicenseGateState.Activated(local)
        }

        // 2. If offline and not locally activated, show Activation screen immediately
        if (!PublicKasHelper.isOnline(appContext)) {
            return@withContext LicenseGateState.Unlicensed(
                deviceId = currentDeviceId,
                infoMessage = null
            )
        }

        // 3. Auto-restore lookup by deviceId + status == "active"
        return@withContext try {
            val querySnap = db.collection(COLLECTION_ACTIVATION_CODES)
                .whereEqualTo("deviceId", currentDeviceId)
                .whereEqualTo("status", "active")
                .limit(10)
                .get()
                .await()

            val matchedDoc = querySnap.documents.firstOrNull { doc ->
                isMatchingAppId(doc.getString("appId") ?: doc.getString("product"))
            }

            if (matchedDoc != null) {
                val restoredCode = matchedDoc.getString("code")?.trim()?.ifBlank { matchedDoc.id } ?: matchedDoc.id
                val restored = LocalLicenseInfo(
                    activated = true,
                    licenseCode = restoredCode,
                    deviceId = currentDeviceId,
                    appId = matchedDoc.getString("appId")?.ifBlank { productAppId } ?: productAppId,
                    activatedAtMillis = System.currentTimeMillis()
                )
                saveLocalLicense(restored)
                LicenseGateState.Activated(restored)
            } else {
                LicenseGateState.Unlicensed(deviceId = currentDeviceId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-restore license check failed: ${e.message}", e)
            LicenseGateState.Unlicensed(
                deviceId = currentDeviceId,
                infoMessage = null
            )
        }
    }

    /**
     * Flexible license lookup & 1-device binding on primary Firestore (`activation_codes`):
     * 1) Try direct document ID: `activation_codes/{normalizedCode}`
     * 2) Fallback query by field: `whereEqualTo("code", normalizedCode).limit(1)`
     * Accepts unused initial states: "unused", "available", "ready", or blank/missing.
     * Surfaces exact Firestore error code & message for diagnostics on failure.
     */
    suspend fun activateLicenseCode(rawCode: String): LicenseActivationResult = withContext(Dispatchers.IO) {
        val normalizedCode = rawCode.trim().uppercase(Locale.ROOT)
        if (normalizedCode.isBlank()) {
            return@withContext LicenseActivationResult.Failure("Silakan masukkan kode lisensi terlebih dahulu.")
        }
        if (!VALID_CODE_REGEX.matches(normalizedCode)) {
            return@withContext LicenseActivationResult.Failure("Format kode lisensi tidak valid.")
        }
        if (!PublicKasHelper.isOnline(appContext)) {
            return@withContext LicenseActivationResult.Failure(
                message = "Tidak ada koneksi internet. Hubungkan internet sekali untuk mengaktifkan kode lisensi.",
                errorDetail = "NETWORK_OFFLINE"
            )
        }

        val currentDeviceId = getOrCreateDeviceId()

        return@withContext try {
            val collectionRef = db.collection(COLLECTION_ACTIVATION_CODES)

            // Step 1: Direct lookup by Document ID
            var matchedSnapshot = collectionRef.document(normalizedCode).get().await()

            // Step 2: Fallback query by `code` field if doc ID is auto-generated
            if (!matchedSnapshot.exists()) {
                val querySnap = collectionRef
                    .whereEqualTo("code", normalizedCode)
                    .limit(1)
                    .get()
                    .await()
                if (!querySnap.isEmpty) {
                    matchedSnapshot = querySnap.documents.first()
                }
            }

            if (!matchedSnapshot.exists()) {
                return@withContext LicenseActivationResult.Failure("Kode lisensi tidak ditemukan.")
            }

            val status = matchedSnapshot.getString("status")?.trim()?.lowercase(Locale.ROOT).orEmpty()
            val boundDeviceId = matchedSnapshot.getString("deviceId")?.trim()?.uppercase(Locale.ROOT).orEmpty()
            val docAppId = matchedSnapshot.getString("appId") ?: matchedSnapshot.getString("product")

            // Validate revoked / blocked state
            if (status in REVOKED_STATUSES) {
                return@withContext LicenseActivationResult.Failure("Kode lisensi ini telah dinonaktifkan.")
            }

            // Validate product scope if explicitly specified on the license code
            if (!isMatchingAppId(docAppId)) {
                return@withContext LicenseActivationResult.Failure(
                    "Kode lisensi ini bukan untuk aplikasi Kas Transparan."
                )
            }

            // Validate 1-device binding
            val isSameDevice = boundDeviceId.equals(currentDeviceId, ignoreCase = true)
            val boundToOtherDevice = boundDeviceId.isNotBlank() && !isSameDevice
            if (boundToOtherDevice || (status == "used" && !isSameDevice)) {
                return@withContext LicenseActivationResult.Failure(
                    "Kode lisensi sudah digunakan di perangkat lain."
                )
            }

            val isEligible = (status in UNUSED_STATUSES && (boundDeviceId.isBlank() || isSameDevice)) ||
                (status == "active" && (boundDeviceId.isBlank() || isSameDevice)) ||
                (status == "used" && isSameDevice)

            if (!isEligible) {
                return@withContext LicenseActivationResult.Failure("Kode lisensi ini telah dinonaktifkan.")
            }

            // Bind code to this device on the matched document
            val updatePayload = mapOf<String, Any>(
                "status" to "active",
                "deviceId" to currentDeviceId,
                "activatedAt" to FieldValue.serverTimestamp()
            )
            matchedSnapshot.reference.update(updatePayload).await()

            val activatedInfo = LocalLicenseInfo(
                activated = true,
                licenseCode = normalizedCode,
                deviceId = currentDeviceId,
                appId = docAppId?.takeIf { it.isNotBlank() } ?: productAppId,
                activatedAtMillis = System.currentTimeMillis()
            )
            saveLocalLicense(activatedInfo)
            LicenseActivationResult.Success(activatedInfo)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Firestore error activating license: ${e.code} - ${e.message}", e)
            val friendlyMessage = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Akses verifikasi lisensi ditolak oleh aturan Firestore."
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Dokumen atau koleksi activation_codes tidak ditemukan."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Gagal terhubung ke server Firestore. Periksa koneksi internet Anda."
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    "Kueri Firestore memerlukan indeks atau prasyarat tambahan."
                else ->
                    "Gagal memverifikasi lisensi."
            }
            val rawDetail = "Error [${e.code.name}]: ${e.message ?: "Unknown Firestore error"}"
            LicenseActivationResult.Failure(message = friendlyMessage, errorDetail = rawDetail)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error activating license", e)
            val rawDetail = "Error [${e.javaClass.simpleName}]: ${e.message ?: e.localizedMessage ?: "Unknown error"}"
            LicenseActivationResult.Failure(
                message = "Gagal mengaktifkan lisensi.",
                errorDetail = rawDetail
            )
        }
    }
}
