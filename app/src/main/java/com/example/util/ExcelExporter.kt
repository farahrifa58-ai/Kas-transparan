package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExcelExportResult(
    val success: Boolean,
    val fileName: String,
    val savedPathDisplay: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val errorMessage: String? = null
)

object ExcelExporter {

    /**
     * Exports transactions to CSV and saves directly into the phone's public Download folder.
     */
    fun exportAndSaveToDownloads(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): ExcelExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }
        val fileName = "Laporan_Kas_${cleanGroup}_$timeStamp.csv"

        // 1. First always write to app cache so we guarantee a shareable File/Uri
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val cachedFile = File(exportDir, fileName)

        try {
            FileOutputStream(cachedFile).use { os ->
                writeCsvContent(os, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
            }
        } catch (e: Exception) {
            return ExcelExportResult(
                success = false,
                fileName = fileName,
                savedPathDisplay = "",
                errorMessage = e.localizedMessage
            )
        }

        var publicUri: Uri? = null
        var displayLocation = "Download/$fileName"

        // 2. Save directly to public Downloads folder
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        writeCsvContent(os, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
                    }
                    publicUri = uri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val publicFile = File(downloadsDir, fileName)
                FileOutputStream(publicFile).use { os ->
                    writeCsvContent(os, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
                }
                displayLocation = publicFile.absolutePath
            }
        } catch (e: Exception) {
            // Even if public storage hit a permission issue, cachedFile is ready
            displayLocation = "Internal Cache ($fileName)"
        }

        val shareUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachedFile)
        } catch (e: Exception) {
            publicUri
        }

        return ExcelExportResult(
            success = true,
            fileName = fileName,
            savedPathDisplay = displayLocation,
            file = cachedFile,
            contentUri = shareUri ?: publicUri
        )
    }

    private fun writeCsvContent(
        outputStream: OutputStream,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ) {
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            // Write UTF-8 BOM so Microsoft Excel & Google Sheets parse accents & Indonesian characters properly
            writer.write("\uFEFF")

            writer.write("LAPORAN KEUANGAN KAS TRANSPARAN\n")
            writer.write("Organisasi / Kelompok;${escapeCsv(settings.groupName)}\n")
            writer.write("Admin / Pengelola;${escapeCsv(settings.adminName)}\n")
            writer.write("Periode Laporan;${escapeCsv(periodTitle)}\n")
            val printDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.write("Tanggal Dibuat;${escapeCsv(printDate)}\n")
            writer.write("Mata Uang;${settings.currencyCode}\n\n")

            writer.write("RINGKASAN SALDO KAS\n")
            writer.write("Saldo Awal;${initialBalance}\n")
            writer.write("Total Pemasukan;${totalIncome}\n")
            writer.write("Total Pengeluaran;${totalExpense}\n")
            writer.write("Saldo Akhir (Saldo Awal + Masuk - Keluar);${finalBalance}\n\n")

            writer.write("No;Tanggal;Jam Input;Jenis Transaksi;Nama / Sumber;Kategori;Metode Pembayaran;Pemasukan;Pengeluaran;Status;Keterangan Catatan\n")

            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            transactions.forEachIndexed { index, tx ->
                val incomeVal = if (tx.type == "INCOME" && tx.status == "ACTIVE") tx.amount.toString() else "0"
                val expenseVal = if (tx.type == "EXPENSE" && tx.status == "ACTIVE") tx.amount.toString() else "0"
                val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: "-"

                val row = listOf(
                    (index + 1).toString(),
                    escapeCsv(tx.date),
                    escapeCsv(inputTime),
                    if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran",
                    escapeCsv(tx.title),
                    escapeCsv(tx.category),
                    escapeCsv(tx.paymentMethod),
                    incomeVal,
                    expenseVal,
                    if (tx.status == "ACTIVE") "Aktif" else "Dibatalkan (Void)",
                    escapeCsv(tx.description)
                ).joinToString(";")
                writer.write(row + "\n")
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Helper to open the exported file or share it directly
     */
    fun openOrShareCsv(context: Context, result: ExcelExportResult) {
        val uri = result.contentUri ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Excel Kas - ${result.fileName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Buka atau Bagikan File Excel")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Backwards-compatible overload
    fun exportToCsv(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): File {
        val result = exportAndSaveToDownloads(context, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
        return result.file ?: File(context.cacheDir, result.fileName)
    }
}
