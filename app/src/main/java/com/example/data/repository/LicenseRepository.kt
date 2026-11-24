package com.example.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.example.R
import com.example.data.model.PublicKasHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
    data class Failure(val message: String) : LicenseActivationResult
}

/**
 * Manages 1-device license activation using an isolated Secondary FirebaseApp ("LicenseAdminApp")
 * pointing to the License Admin Firestore database (`activation_codes` collection), so it never
 * conflicts with Kas Transparan's primary Firebase instance (`public_kas`).
 */
class LicenseRepository private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "LicenseRepository"
        private const val LICENSE_APP_NAME = "LicenseAdminApp"
        private const val PREFS_NAME = "kas_transparan_license_storage"
        private const val KEY_DEVICE_ID = "kas_transparan_device_id"
        private const val KEY_LICENSE_JSON = "kas_transparan_license"
        private const val COLLECTION_ACTIVATION_CODES = "activation_codes"

        private val VALID_CODE_REGEX = Regex("^[A-Z0-9][A-Z0-9_-]{2,63}$")

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
     * Returns a stable, reinstall-persistent device ID on Android (matching Capacitor Device.getId()
     * which reads Settings.Secure.ANDROID_ID), with a local SharedPreferences backup under
     * `kas_transparan_device_id`.
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

    /**
     * Lazily initializes the secondary FirebaseApp (`LicenseAdminApp`) and returns its isolated
     * Firestore instance targeting the License Admin database.
     */
    private fun getLicenseFirestore(): FirebaseFirestore {
        val existingApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == LICENSE_APP_NAME }
        val secondaryApp = existingApp ?: synchronized(this) {
            FirebaseApp.getApps(appContext).firstOrNull { it.name == LICENSE_APP_NAME }
                ?: run {
                    val primaryOptions = try {
                        FirebaseApp.getInstance().options
                    } catch (_: Exception) {
                        null
                    }

                    val projectId = appContext.getString(R.string.license_firebase_project_id)
                        .trim()
                        .ifBlank { primaryOptions?.projectId.orEmpty() }
                    val applicationId = appContext.getString(R.string.license_firebase_application_id)
                        .trim()
                        .ifBlank { primaryOptions?.applicationId.orEmpty() }
                    val gcmSenderId = appContext.getString(R.string.license_firebase_gcm_sender_id)
                        .trim()
                        .ifBlank { primaryOptions?.gcmSenderId.orEmpty() }
                    val storageBucket = appContext.getString(R.string.license_firebase_storage_bucket)
                        .trim()
                        .ifBlank { primaryOptions?.storageBucket.orEmpty() }
                    val apiKey = primaryOptions?.apiKey.orEmpty()

                    require(apiKey.isNotBlank() && projectId.isNotBlank() && applicationId.isNotBlank()) {
                        "Konfigurasi Firebase License Admin belum lengkap."
                    }

                    val optionsBuilder = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .setApplicationId(applicationId)
                    if (gcmSenderId.isNotBlank()) {
                        optionsBuilder.setGcmSenderId(gcmSenderId)
                    }
                    if (storageBucket.isNotBlank()) {
                        optionsBuilder.setStorageBucket(storageBucket)
                    }

                    FirebaseApp.initializeApp(appContext, optionsBuilder.build(), LICENSE_APP_NAME)
                }
        }

        val dbId = appContext.getString(R.string.license_firestore_database_id).trim()
        return if (dbId.isBlank() || dbId.equals("(default)", ignoreCase = true) || dbId.equals("default", ignoreCase = true)) {
            FirebaseFirestore.getInstance(secondaryApp)
        } else {
            FirebaseFirestore.getInstance(secondaryApp, dbId)
        }
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
     * `activation_codes` in License Admin Firestore for an active record bound to `currentDeviceId`
     * (Auto-Restore after reinstall / clear cache).
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
            val db = getLicenseFirestore()
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
                val restored = LocalLicenseInfo(
                    activated = true,
                    licenseCode = matchedDoc.id,
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
     * Validates and binds a license code document (`activation_codes/{code}`) to `currentDeviceId`.
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
                "Tidak ada koneksi internet. Hubungkan internet sekali untuk mengaktifkan kode lisensi."
            )
        }

        val currentDeviceId = getOrCreateDeviceId()

        return@withContext try {
            val db = getLicenseFirestore()
            val docRef = db.collection(COLLECTION_ACTIVATION_CODES).document(normalizedCode)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                return@withContext LicenseActivationResult.Failure("Kode lisensi tidak ditemukan.")
            }

            val status = snapshot.getString("status")?.trim()?.lowercase(Locale.ROOT).orEmpty()
            val boundDeviceId = snapshot.getString("deviceId")?.trim()?.uppercase(Locale.ROOT).orEmpty()
            val docAppId = snapshot.getString("appId") ?: snapshot.getString("product")

            // Validate revoked / blocked state
            if (status == "revoked" || status == "blocked" || status == "disabled" || status == "suspended") {
                return@withContext LicenseActivationResult.Failure("Kode lisensi ini telah dinonaktifkan.")
            }

            // Validate product scope if specified on the license code
            if (!isMatchingAppId(docAppId)) {
                return@withContext LicenseActivationResult.Failure(
                    "Kode lisensi ini bukan untuk aplikasi Kas Transparan."
                )
            }

            // Validate 1-device binding
            val boundToOtherDevice = boundDeviceId.isNotBlank() && !boundDeviceId.equals(currentDeviceId, ignoreCase = true)
            if (boundToOtherDevice || (status == "used" && !boundDeviceId.equals(currentDeviceId, ignoreCase = true))) {
                return@withContext LicenseActivationResult.Failure(
                    "Kode lisensi sudah digunakan di perangkat lain."
                )
            }

            val isEligible = status == "unused" ||
                status.isBlank() ||
                (status == "active" && (boundDeviceId.isBlank() || boundDeviceId.equals(currentDeviceId, ignoreCase = true))) ||
                (status == "used" && boundDeviceId.equals(currentDeviceId, ignoreCase = true))

            if (!isEligible) {
                return@withContext LicenseActivationResult.Failure("Kode lisensi ini telah dinonaktifkan.")
            }

            // Bind code to this device in License Admin Firestore
            val updatePayload = mutableMapOf<String, Any>(
                "status" to "active",
                "deviceId" to currentDeviceId,
                "activatedAt" to FieldValue.serverTimestamp()
            )
            if (docAppId.isNullOrBlank()) {
                updatePayload["appId"] = productAppId
            }

            docRef.update(updatePayload).await()

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
            val msg = when (e.code) {
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Database License Admin tidak ditemukan. Periksa ID database di konfigurasi."
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Akses verifikasi lisensi ditolak oleh aturan server License Admin."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Server lisensi sedang tidak terjangkau. Periksa koneksi internet Anda lalu coba lagi."
                else -> "Gagal memverifikasi lisensi: ${e.localizedMessage ?: "Terjadi kesalahan jaringan."}"
            }
            LicenseActivationResult.Failure(msg)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error activating license", e)
            LicenseActivationResult.Failure(
                "Gagal mengaktifkan lisensi: ${e.localizedMessage ?: "Periksa koneksi internet Anda."}"
            )
        }
    }
}
