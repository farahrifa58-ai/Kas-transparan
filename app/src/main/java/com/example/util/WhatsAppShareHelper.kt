package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
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
}
