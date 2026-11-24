package com.example.data.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.annotation.Keep
import com.example.util.CurrencyHelper
import com.google.firebase.firestore.IgnoreExtraProperties
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class PublicKasTransaction(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val type: String = "income", // "income" or "expense"
    val amount: Long = 0L,
    val date: String = "",
    val notes: String = ""
)

@Keep
@IgnoreExtraProperties
data class PublicKasReport(
    val publicKasId: String = "",
    val ownerUid: String = "",
    val groupName: String = "",
    val description: String = "",
    val currencyCode: String = "IDR",
    val initialBalance: Long = 0L,
    val totalBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val transactions: List<PublicKasTransaction> = emptyList()
)

sealed interface PublicKasUiState {
    data object Loading : PublicKasUiState
    data class Success(val report: PublicKasReport) : PublicKasUiState
    data class NotFound(val kasId: String) : PublicKasUiState
    data class Error(val message: String) : PublicKasUiState
}

class PublicAuthRequiredException : Exception("Autentikasi Google diperlukan untuk mempublikasikan laporan.")

object PublicKasHelper {
    private const val ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val SECRET_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val secureRandom = SecureRandom()
    private val VALID_ID_REGEX = Regex("^KAS-[A-Z0-9]{4,28}$")

    fun generatePublicKasId(): String {
        val sb = StringBuilder("KAS-")
        repeat(8) {
            sb.append(ID_CHARS[secureRandom.nextInt(ID_CHARS.length)])
        }
        return sb.toString()
    }

    fun generateWriteSecret(): String {
        val sb = StringBuilder(24)
        repeat(24) {
            sb.append(SECRET_CHARS[secureRandom.nextInt(SECRET_CHARS.length)])
        }
        return sb.toString()
    }

    fun isValidWriteSecret(secret: String): Boolean {
        val trimmed = secret.trim()
        return trimmed.length in 16..64
    }

    fun isValidPublicKasId(id: String): Boolean {
        return VALID_ID_REGEX.matches(id.trim().uppercase(Locale.ROOT))
    }

    fun normalizePublicKasId(raw: String): String {
        val cleaned = raw.trim().uppercase(Locale.ROOT)
        return if (cleaned.startsWith("KAS-")) cleaned else if (cleaned.isNotBlank()) "KAS-$cleaned" else ""
    }

    fun buildPublicUrl(baseUrl: String, publicKasId: String): String {
        val cleanBase = baseUrl.trim().ifBlank { AppSettings.DEFAULT_PUBLIC_BASE_URL }.trimEnd('/')
        return "$cleanBase/#/lihat/$publicKasId"
    }

    fun extractPublicKasIdFromUri(uri: Uri?): String? {
        if (uri == null) return null

        // 1. Query parameter ?lihat=KAS-XXXXXX
        val queryParam = uri.getQueryParameter("lihat")?.trim()?.uppercase(Locale.ROOT)
        if (!queryParam.isNullOrBlank() && isValidPublicKasId(queryParam)) {
            return queryParam
        }

        // 2. Fragment #/lihat/KAS-XXXXXX or #lihat/KAS-XXXXXX
        val fragment = uri.fragment?.trim()
        if (!fragment.isNullOrBlank()) {
            val normalizedFrag = fragment.trimStart('/')
            if (normalizedFrag.startsWith("lihat/", ignoreCase = true)) {
                val id = normalizedFrag.substringAfter("lihat/").substringBefore("?").substringBefore("/").uppercase(Locale.ROOT)
                if (isValidPublicKasId(id)) return id
            }
        }

        // 3. Path /lihat/KAS-XXXXXX or custom scheme kastransparan://lihat/KAS-XXXXXX
        if (uri.scheme.equals("kastransparan", ignoreCase = true) && uri.host.equals("lihat", ignoreCase = true)) {
            val id = uri.lastPathSegment?.trim()?.uppercase(Locale.ROOT)
            if (!id.isNullOrBlank() && isValidPublicKasId(id)) return id
        }

        val segments = uri.pathSegments
        val lihatIdx = segments.indexOfFirst { it.equals("lihat", ignoreCase = true) }
        if (lihatIdx != -1 && lihatIdx + 1 < segments.size) {
            val id = segments[lihatIdx + 1].trim().uppercase(Locale.ROOT)
            if (isValidPublicKasId(id)) return id
        }

        return null
    }

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun formatSyncTimestamp(timestampMillis: Long, lang: String): String {
        val isEn = lang.startsWith("en", ignoreCase = true)
        if (timestampMillis <= 0L) {
            return if (isEn) "Not published yet" else "Belum dipublikasikan"
        }
        val locale = if (isEn) Locale.ENGLISH else Locale("id", "ID")
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
        val formatted = sdf.format(Date(timestampMillis))
        return if (isEn) "Last updated: $formatted" else "Terakhir diperbarui: $formatted"
    }

    fun buildWhatsAppPublicLinkMessage(
        settings: AppSettings,
        totalBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        publicUrl: String
    ): String {
        val curr = settings.currencyCode.ifBlank { "IDR" }
        val group = settings.groupName.ifBlank { "Kas Transparan" }
        val descLine = if (settings.description.isNotBlank()) "${settings.description}\n" else ""
        val isEn = settings.language.startsWith("en", ignoreCase = true)

        return if (isEn) {
            """
            📊 *REAL-TIME PUBLIC CASH REPORT*
            *$group*
            ${descLine}
            💰 *Current Balance:* ${CurrencyHelper.format(totalBalance, curr)}
            📈 Total Income: +${CurrencyHelper.format(totalIncome, curr)}
            📉 Total Expense: -${CurrencyHelper.format(totalExpense, curr)}

            🔗 View live transactions (Read-Only, no login required):
            $publicUrl

            🆔 Public Cash ID: *${settings.publicKasId}*

            _Updated via Kas Transparan for open community accountability._
            """.trimIndent()
        } else {
            """
            📊 *LAPORAN KAS TRANSPARAN (REAL-TIME)*
            *$group*
            ${descLine}
            💰 *Saldo Kas Saat Ini:* ${CurrencyHelper.format(totalBalance, curr)}
            📈 Total Pemasukan: +${CurrencyHelper.format(totalIncome, curr)}
            📉 Total Pengeluaran: -${CurrencyHelper.format(totalExpense, curr)}

            🔗 Pantau saldo & rincian transaksi terbaru (Mode Hanya Lihat, tanpa login):
            $publicUrl

            🆔 ID Kas Publik: *${settings.publicKasId}*

            _Disampaikan oleh Bendahara sebagai wujud transparansi pengelolaan kas bersama._
            """.trimIndent()
        }
    }
}
