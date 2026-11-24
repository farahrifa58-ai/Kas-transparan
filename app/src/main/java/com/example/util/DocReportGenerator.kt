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
import com.example.data.model.ExcelColumnMode
import com.example.data.model.Transaction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DocExportResult(
    val success: Boolean,
    val fileName: String,
    val savedPathDisplay: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val errorMessage: String? = null
)

internal data class DocRowWithBalance(
    val transaction: Transaction,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val runningBalance: Long
)

object DocReportGenerator {

    private const val DOC_MIME = "application/msword"

    fun exportAndSaveToDownloads(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long,
        columnMode: ExcelColumnMode = ExcelColumnMode.COMPLETE
    ): DocExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }

        val fileName = "Laporan_Kas_${cleanGroup}_$timeStamp.doc"
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val cachedFile = File(exportDir, fileName)

        try {
            FileOutputStream(cachedFile).use { os ->
                OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                    writer.write(
                        buildWordHtmlDocument(
                            settings = settings,
                            transactions = transactions,
                            periodTitle = periodTitle,
                            initialBalance = initialBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            finalBalance = finalBalance,
                            columnMode = columnMode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return DocExportResult(
                success = false,
                fileName = fileName,
                savedPathDisplay = "",
                errorMessage = e.localizedMessage
            )
        }

        var publicUri: Uri? = null
        var displayLocation = "Download/$fileName"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, DOC_MIME)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        FileInputStream(cachedFile).use { input ->
                            input.copyTo(os)
                        }
                    }
                    publicUri = uri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val publicFile = File(downloadsDir, fileName)
                FileInputStream(cachedFile).use { input ->
                    FileOutputStream(publicFile).use { os ->
                        input.copyTo(os)
                    }
                }
                displayLocation = publicFile.absolutePath
            }
        } catch (_: Exception) {
            displayLocation = "Internal Storage ($fileName)"
        }

        val shareUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachedFile)
        } catch (_: Exception) {
            publicUri
        }

        return DocExportResult(
            success = true,
            fileName = fileName,
            savedPathDisplay = displayLocation,
            file = cachedFile,
            contentUri = shareUri ?: publicUri
        )
    }

    internal fun prepareChronologicalRows(
        transactions: List<Transaction>,
        initialBalance: Long,
        onlyActive: Boolean
    ): List<DocRowWithBalance> {
        val sorted = transactions
            .let { list -> if (onlyActive) list.filter { it.status == "ACTIVE" } else list }
            .sortedWith(
                compareBy<Transaction> { it.date }
                    .thenBy { it.createdAt?.toDate()?.time ?: 0L }
            )

        var runningBalance = initialBalance
        return sorted.map { tx ->
            val isActive = tx.status == "ACTIVE"
            val incVal = if (tx.type == "INCOME" && isActive) tx.amount else 0L
            val expVal = if (tx.type == "EXPENSE" && isActive) tx.amount else 0L
            runningBalance = runningBalance + incVal - expVal
            DocRowWithBalance(
                transaction = tx,
                incomeAmount = incVal,
                expenseAmount = expVal,
                runningBalance = runningBalance
            )
        }
    }

    private fun formatPlainNumber(amount: Long, currencyCode: String): String {
        val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
            groupingSeparator = if (currencyCode.equals("IDR", ignoreCase = true)) '.' else ','
            decimalSeparator = if (currencyCode.equals("IDR", ignoreCase = true)) ',' else '.'
        }
        return DecimalFormat("#,##0", symbols).format(amount)
    }

    internal fun buildWordHtmlDocument(
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long,
        columnMode: ExcelColumnMode
    ): String {
        val isCompact = columnMode == ExcelColumnMode.COMPACT
        val rows = prepareChronologicalRows(transactions, initialBalance, onlyActive = isCompact)
        val curr = settings.currencyCode.ifBlank { "IDR" }
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())

        val pageOrientationCss = if (isCompact) {
            "size: 21.0cm 29.7cm; margin: 1.4cm 1.4cm 1.4cm 1.4cm;"
        } else {
            "size: 29.7cm 21.0cm; margin: 1.2cm 1.2cm 1.2cm 1.2cm;"
        }

        val sb = StringBuilder(16384)
        sb.append(
            """
            <html xmlns:o="urn:schemas-microsoft-com:office:office"
                  xmlns:w="urn:schemas-microsoft-com:office:word"
                  xmlns="http://www.w3.org/TR/REC-html40">
            <head>
              <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
              <title>Laporan Kas Transparan</title>
              <!--[if gte mso 9]>
              <xml>
                <w:WordDocument>
                  <w:View>Print</w:View>
                  <w:Zoom>100</w:Zoom>
                  <w:DoNotOptimizeForBrowser/>
                </w:WordDocument>
              </xml>
              <![endif]-->
              <style>
                @page Section1 { $pageOrientationCss }
                div.Section1 { page: Section1; }
                body {
                  font-family: Arial, Calibri, sans-serif;
                  font-size: 10pt;
                  color: #111827;
                  margin: 0;
                  padding: 0;
                }
                .title-bar {
                  background-color: #D9EAD3;
                  border-left: 5px solid #0F6B5B;
                  padding: 10px 12px;
                  margin-bottom: 10px;
                }
                .title-text {
                  font-size: 14pt;
                  font-weight: bold;
                  color: #0F6B5B;
                  margin: 0;
                  letter-spacing: 0.3px;
                }
                .meta-table {
                  width: 100%;
                  border-collapse: collapse;
                  margin-bottom: 12px;
                }
                .meta-table td {
                  padding: 4px 6px;
                  font-size: 9.5pt;
                  border-bottom: 1px solid #E5E7EB;
                  vertical-align: top;
                }
                .meta-label {
                  width: 28%;
                  font-weight: bold;
                  color: #374151;
                }
                .meta-value {
                  width: 72%;
                  color: #111827;
                }
                .summary-table {
                  width: 58%;
                  border-collapse: collapse;
                  margin-bottom: 14px;
                  border: 1px solid #A7D7C5;
                }
                .summary-header td {
                  background-color: #D9EAD3;
                  color: #0F6B5B;
                  font-weight: bold;
                  font-size: 10pt;
                  padding: 6px 10px;
                  border-bottom: 1px solid #A7D7C5;
                }
                .summary-row td {
                  background-color: #F4FAF7;
                  padding: 5px 10px;
                  font-size: 9.5pt;
                  border-bottom: 1px solid #DCEFE6;
                }
                .summary-row-final td {
                  background-color: #D9EAD3;
                  padding: 6px 10px;
                  font-size: 10pt;
                  font-weight: bold;
                  color: #0F6B5B;
                }
                .summary-label {
                  font-weight: bold;
                  color: #1F2937;
                  text-align: left;
                }
                .summary-val {
                  font-weight: bold;
                  text-align: right;
                  color: #111827;
                }
                .tx-table {
                  width: 100%;
                  border-collapse: collapse;
                  table-layout: fixed;
                }
                .tx-table th {
                  background-color: #0F6B5B;
                  color: #FFFFFF;
                  font-weight: bold;
                  font-size: 9pt;
                  padding: 7px 6px;
                  border: 1px solid #0A4A3F;
                  text-align: center;
                  vertical-align: middle;
                }
                .tx-table td {
                  font-size: 8.8pt;
                  padding: 6px 6px;
                  border: 1px solid #CBD5E1;
                  vertical-align: middle;
                  word-wrap: break-word;
                }
                .row-even td {
                  background-color: #FFFFFF;
                }
                .row-odd td {
                  background-color: #F8FAFC;
                }
                .c-center { text-align: center; }
                .c-left { text-align: left; }
                .c-right { text-align: right; }
                .amt-in { text-align: right; font-weight: bold; color: #15803D; }
                .amt-out { text-align: right; font-weight: bold; color: #C62828; }
                .amt-bal { text-align: right; font-weight: bold; color: #0F6B5B; }
                .amt-zero { text-align: right; color: #64748B; }
                .footer-sign {
                  margin-top: 22px;
                  width: 100%;
                  border-collapse: collapse;
                }
                .footer-sign td {
                  font-size: 9pt;
                  color: #374151;
                }
              </style>
            </head>
            <body>
            <div class="Section1">
            """.trimIndent()
        )

        // Header Kop Laporan
        sb.append("<div class=\"title-bar\"><p class=\"title-text\">LAPORAN KAS TRANSPARAN</p></div>\n")
        sb.append("<table class=\"meta-table\">\n")
        sb.append("  <tr><td class=\"meta-label\">Organisasi / Kelompok</td><td class=\"meta-value\">${escapeHtml(settings.groupName.ifBlank { "Kas Transparan" })}</td></tr>\n")
        sb.append("  <tr><td class=\"meta-label\">Admin / Pengelola</td><td class=\"meta-value\">${escapeHtml(settings.adminName.ifBlank { "Admin Kas" })}</td></tr>\n")
        sb.append("  <tr><td class=\"meta-label\">Periode Laporan</td><td class=\"meta-value\">${escapeHtml(periodTitle)}</td></tr>\n")
        sb.append("  <tr><td class=\"meta-label\">Tanggal Dibuat</td><td class=\"meta-value\">${escapeHtml(printDate)}</td></tr>\n")
        sb.append("  <tr><td class=\"meta-label\">Mata Uang</td><td class=\"meta-value\">${escapeHtml(curr)}</td></tr>\n")
        sb.append("</table>\n")

        // Kotak Ringkasan Saldo
        sb.append("<table class=\"summary-table\">\n")
        sb.append("  <tr class=\"summary-header\"><td colspan=\"2\">RINGKASAN SALDO KAS</td></tr>\n")
        sb.append("  <tr class=\"summary-row\"><td class=\"summary-label\">Saldo Awal</td><td class=\"summary-val\">${escapeHtml(formatPlainNumber(initialBalance, curr))}</td></tr>\n")
        sb.append("  <tr class=\"summary-row\"><td class=\"summary-label\">Total Pemasukan</td><td class=\"summary-val\" style=\"color:#15803D;\">${escapeHtml(formatPlainNumber(totalIncome, curr))}</td></tr>\n")
        sb.append("  <tr class=\"summary-row\"><td class=\"summary-label\">Total Pengeluaran</td><td class=\"summary-val\" style=\"color:#C62828;\">${escapeHtml(formatPlainNumber(totalExpense, curr))}</td></tr>\n")
        sb.append("  <tr class=\"summary-row-final\"><td>Saldo Akhir</td><td class=\"summary-val\" style=\"color:#0F6B5B;\">${escapeHtml(formatPlainNumber(finalBalance, curr))}</td></tr>\n")
        sb.append("</table>\n")

        // Tabel Rincian Transaksi Berbingkai
        sb.append("<table class=\"tx-table\">\n")
        if (isCompact) {
            sb.append(
                """
                <thead>
                  <tr>
                    <th style="width:5%;">No</th>
                    <th style="width:17%;">Tanggal &amp; Waktu</th>
                    <th style="width:13%;">Jenis Transaksi</th>
                    <th style="width:13%;">Pemasukan</th>
                    <th style="width:13%;">Pengeluaran</th>
                    <th style="width:14%;">Jumlah Saldo</th>
                    <th style="width:25%;">Catatan</th>
                  </tr>
                </thead>
                <tbody>
                """.trimIndent()
            )
            if (rows.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"c-center\">Tidak ada transaksi pada periode ini.</td></tr>\n")
            } else {
                rows.forEachIndexed { index, item ->
                    val tx = item.transaction
                    val rowClass = if (index % 2 == 0) "row-even" else "row-odd"
                    val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: ""
                    val dateTimeText = if (inputTime.isNotBlank()) "${tx.date} $inputTime" else tx.date
                    val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
                    val noteText = tx.description.ifBlank { tx.title }
                    val inClass = if (item.incomeAmount > 0L) "amt-in" else "amt-zero"
                    val outClass = if (item.expenseAmount > 0L) "amt-out" else "amt-zero"

                    sb.append("<tr class=\"$rowClass\">")
                    sb.append("<td class=\"c-center\">${index + 1}</td>")
                    sb.append("<td class=\"c-center\">${escapeHtml(dateTimeText)}</td>")
                    sb.append("<td class=\"c-left\">$jenis</td>")
                    sb.append("<td class=\"$inClass\">${escapeHtml(formatPlainNumber(item.incomeAmount, curr))}</td>")
                    sb.append("<td class=\"$outClass\">${escapeHtml(formatPlainNumber(item.expenseAmount, curr))}</td>")
                    sb.append("<td class=\"amt-bal\">${escapeHtml(formatPlainNumber(item.runningBalance, curr))}</td>")
                    sb.append("<td class=\"c-left\">${escapeHtml(noteText)}</td>")
                    sb.append("</tr>\n")
                }
            }
        } else {
            sb.append(
                """
                <thead>
                  <tr>
                    <th style="width:4%;">No</th>
                    <th style="width:9%;">Tanggal</th>
                    <th style="width:6%;">Jam</th>
                    <th style="width:9%;">Jenis</th>
                    <th style="width:13%;">Nama / Sumber</th>
                    <th style="width:9%;">Kategori</th>
                    <th style="width:8%;">Metode</th>
                    <th style="width:9%;">Pemasukan</th>
                    <th style="width:9%;">Pengeluaran</th>
                    <th style="width:9%;">Jumlah Saldo</th>
                    <th style="width:6%;">Status</th>
                    <th style="width:9%;">Catatan</th>
                  </tr>
                </thead>
                <tbody>
                """.trimIndent()
            )
            if (rows.isEmpty()) {
                sb.append("<tr><td colspan=\"12\" class=\"c-center\">Tidak ada transaksi pada periode ini.</td></tr>\n")
            } else {
                rows.forEachIndexed { index, item ->
                    val tx = item.transaction
                    val rowClass = if (index % 2 == 0) "row-even" else "row-odd"
                    val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: "-"
                    val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
                    val statusText = if (tx.status == "ACTIVE") "Aktif" else "Batal"
                    val inClass = if (item.incomeAmount > 0L) "amt-in" else "amt-zero"
                    val outClass = if (item.expenseAmount > 0L) "amt-out" else "amt-zero"

                    sb.append("<tr class=\"$rowClass\">")
                    sb.append("<td class=\"c-center\">${index + 1}</td>")
                    sb.append("<td class=\"c-center\">${escapeHtml(tx.date)}</td>")
                    sb.append("<td class=\"c-center\">${escapeHtml(inputTime)}</td>")
                    sb.append("<td class=\"c-left\">$jenis</td>")
                    sb.append("<td class=\"c-left\">${escapeHtml(tx.title)}</td>")
                    sb.append("<td class=\"c-left\">${escapeHtml(tx.category)}</td>")
                    sb.append("<td class=\"c-left\">${escapeHtml(tx.paymentMethod.ifBlank { "Tunai" })}</td>")
                    sb.append("<td class=\"$inClass\">${escapeHtml(formatPlainNumber(item.incomeAmount, curr))}</td>")
                    sb.append("<td class=\"$outClass\">${escapeHtml(formatPlainNumber(item.expenseAmount, curr))}</td>")
                    sb.append("<td class=\"amt-bal\">${escapeHtml(formatPlainNumber(item.runningBalance, curr))}</td>")
                    sb.append("<td class=\"c-center\">$statusText</td>")
                    sb.append("<td class=\"c-left\">${escapeHtml(tx.description)}</td>")
                    sb.append("</tr>\n")
                }
            }
        }
        sb.append("</tbody>\n</table>\n")

        // Footer Signature
        sb.append(
            """
            <table class="footer-sign">
              <tr>
                <td style="width:65%;"></td>
                <td style="width:35%; text-align:center;">
                  Pengelola Kas / Admin,<br/><br/><br/>
                  <b>${escapeHtml(settings.adminName.ifBlank { "Administrator" })}</b>
                </td>
              </tr>
            </table>
            </div>
            </body>
            </html>
            """.trimIndent()
        )
        return sb.toString()
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun openOrShareDoc(context: Context, result: DocExportResult) {
        val uri = result.contentUri ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DOC_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Buka Dokumen Word")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = DOC_MIME
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Dokumen Kas - ${result.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Bagikan Dokumen Word (.doc)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
