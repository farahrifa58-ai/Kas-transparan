package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.Transaction
import java.io.File

object WhatsAppShareHelper {

    fun generateWhatsAppMessage(
        settings: AppSettings,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): String {
        val curr = settings.currencyCode
        val isEn = settings.language.equals("en", ignoreCase = true)

        return if (isEn) {
            """
            *FINANCIAL CASH REPORT*
            ${settings.groupName.ifBlank { "Kas Transparan" }}

            Period: $periodTitle

            Opening Balance: ${CurrencyHelper.format(initialBalance, curr)}
            Total Income: ${CurrencyHelper.format(totalIncome, curr)}
            Total Expense: ${CurrencyHelper.format(totalExpense, curr)}

            *FINAL BALANCE: ${CurrencyHelper.format(finalBalance, curr)}*

            Detailed report is attached in PDF format.

            This financial report is submitted as a form of transparency and accountability in cash management.
            """.trimIndent()
        } else {
            """
            *LAPORAN KEUANGAN KAS*
            ${settings.groupName.ifBlank { "Kas Transparan" }}

            Periode: $periodTitle

            Saldo Awal: ${CurrencyHelper.format(initialBalance, curr)}
            Total Pemasukan: ${CurrencyHelper.format(totalIncome, curr)}
            Total Pengeluaran: ${CurrencyHelper.format(totalExpense, curr)}

            *SALDO AKHIR: ${CurrencyHelper.format(finalBalance, curr)}*

            Laporan lengkap terlampir dalam format PDF.

            Demikian laporan keuangan ini kami sampaikan sebagai bentuk transparansi dan pertanggungjawaban pengelolaan kas.
            """.trimIndent()
        }
    }

    fun shareReport(
        context: Context,
        pdfFile: File,
        message: String,
        targetWhatsAppOnly: Boolean = false
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan Kas")
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetWhatsAppOnly) {
                    setPackage("com.whatsapp")
                }
            }

            // Also copy text to clipboard for convenience
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Laporan Kas", message)
            clipboard.setPrimaryClip(clip)

            try {
                if (targetWhatsAppOnly) {
                    context.startActivity(intent)
                } else {
                    val chooser = Intent.createChooser(intent, "Bagikan Laporan Kas via WhatsApp / Aplikasi Lain")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                // If direct WhatsApp package not found, open general chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Bagikan Laporan Kas")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan laporan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateTransactionShareText(
        settings: AppSettings,
        tx: Transaction
    ): String {
        val curr = settings.currencyCode
        val isEn = settings.language.equals("en", ignoreCase = true)
        val isIncome = tx.type == "INCOME"
        val statusStr = if (tx.status == "VOID") (if (isEn) " [VOIDED]" else " [DIBATALKAN]") else ""

        return if (isEn) {
            """
            *TRANSACTION RECEIPT$statusStr*
            ${settings.groupName.ifBlank { "Kas Transparan" }}
            
            Type: ${if (isIncome) "Income" else "Expense"}
            Title: ${tx.title}
            Amount: ${if (isIncome) "+" else "-"} ${CurrencyHelper.format(tx.amount, curr)}
            Date: ${tx.date}
            Category: ${tx.category}
            Payment: ${tx.paymentMethod.ifBlank { "Cash" }}
            ${if (tx.description.isNotBlank()) "Note: ${tx.description}\n" else ""}${if (tx.status == "VOID" && tx.voidReason.isNotBlank()) "Void Reason: ${tx.voidReason}\n" else ""}
            Admin: ${settings.adminName.ifBlank { "Admin" }}
            """.trimIndent()
        } else {
            """
            *BUKTI TRANSAKSI KAS$statusStr*
            ${settings.groupName.ifBlank { "Kas Transparan" }}
            
            Jenis: ${if (isIncome) "Kas Masuk" else "Kas Keluar"}
            Judul: ${tx.title}
            Nominal: ${if (isIncome) "+" else "-"} ${CurrencyHelper.format(tx.amount, curr)}
            Tanggal: ${tx.date}
            Kategori: ${tx.category}
            Metode: ${tx.paymentMethod.ifBlank { "Tunai" }}
            ${if (tx.description.isNotBlank()) "Catatan: ${tx.description}\n" else ""}${if (tx.status == "VOID" && tx.voidReason.isNotBlank()) "Alasan Batal: ${tx.voidReason}\n" else ""}
            Pengelola: ${settings.adminName.ifBlank { "Admin" }}
            """.trimIndent()
        }
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String,
        captionText: String = ""
    ) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, subject)
                if (captionText.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, captionText)
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (captionText.isNotBlank()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Bukti Transaksi", captionText)
                clipboard.setPrimaryClip(clip)
            }

            val chooser = Intent.createChooser(intent, "Bagikan ke...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

