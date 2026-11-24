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
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExcelExportResult(
    val success: Boolean,
    val fileName: String,
    val savedPathDisplay: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val errorMessage: String? = null
)

internal data class ExportRowWithBalance(
    val transaction: Transaction,
    val incomeAmount: Long,
    val expenseAmount: Long,
    val runningBalance: Long
)

object ExcelExporter {

    private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val CSV_MIME = "text/csv"

    /**
     * Exports transactions to genuine Excel (.xlsx) or CSV (.csv) and saves directly into the phone's public Download folder.
     * Supports both COMPLETE and COMPACT (7 columns + running balance) column modes.
     */
    fun exportAndSaveToDownloads(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long,
        format: String = "XLSX",
        columnMode: ExcelColumnMode = ExcelColumnMode.COMPLETE
    ): ExcelExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }

        val isExcel = !format.equals("CSV", ignoreCase = true)
        val fileExt = if (isExcel) "xlsx" else "csv"
        val mimeType = if (isExcel) XLSX_MIME else CSV_MIME
        val fileName = "Laporan_Kas_${cleanGroup}_$timeStamp.$fileExt"

        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val cachedFile = File(exportDir, fileName)

        try {
            FileOutputStream(cachedFile).use { os ->
                if (isExcel) {
                    writeXlsxContent(
                        outputStream = os,
                        settings = settings,
                        transactions = transactions,
                        periodTitle = periodTitle,
                        initialBalance = initialBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        finalBalance = finalBalance,
                        columnMode = columnMode
                    )
                } else {
                    writeCsvContent(
                        outputStream = os,
                        settings = settings,
                        transactions = transactions,
                        periodTitle = periodTitle,
                        initialBalance = initialBalance,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        finalBalance = finalBalance,
                        columnMode = columnMode
                    )
                }
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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

        return ExcelExportResult(
            success = true,
            fileName = fileName,
            savedPathDisplay = displayLocation,
            file = cachedFile,
            contentUri = shareUri ?: publicUri
        )
    }

    /**
     * Sorts transactions chronologically ASCENDING (oldest -> newest) and computes
     * automatic cumulative running balance starting from [initialBalance].
     * Guarantees the last row's runningBalance equals Final Balance (Saldo Akhir).
     */
    internal fun prepareChronologicalRows(
        transactions: List<Transaction>,
        initialBalance: Long,
        onlyActive: Boolean
    ): List<ExportRowWithBalance> {
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
            ExportRowWithBalance(
                transaction = tx,
                incomeAmount = incVal,
                expenseAmount = expVal,
                runningBalance = runningBalance
            )
        }
    }

    private fun writeCsvContent(
        outputStream: OutputStream,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long,
        columnMode: ExcelColumnMode
    ) {
        val isCompact = columnMode == ExcelColumnMode.COMPACT
        val rows = prepareChronologicalRows(transactions, initialBalance, onlyActive = isCompact)
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write("\uFEFF")

            writer.write("LAPORAN KAS TRANSPARAN\n")
            writer.write("Organisasi / Kelompok,${escapeCsv(settings.groupName)}\n")
            writer.write("Admin / Pengelola,${escapeCsv(settings.adminName)}\n")
            writer.write("Periode Laporan,${escapeCsv(periodTitle)}\n")
            val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())
            writer.write("Tanggal Dibuat,${escapeCsv(printDate)}\n")
            writer.write("Mata Uang,${escapeCsv(settings.currencyCode)}\n\n")

            writer.write("RINGKASAN SALDO KAS\n")
            writer.write("Saldo Awal,$initialBalance\n")
            writer.write("Total Pemasukan,$totalIncome\n")
            writer.write("Total Pengeluaran,$totalExpense\n")
            writer.write("Saldo Akhir,$finalBalance\n\n")

            if (isCompact) {
                writer.write("No,Tanggal & Waktu,Jenis Transaksi,Pemasukan,Pengeluaran,Jumlah Saldo,Catatan\n")
                rows.forEachIndexed { index, item ->
                    val tx = item.transaction
                    val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: ""
                    val dateTimeText = if (inputTime.isNotBlank()) "${tx.date} $inputTime" else tx.date
                    val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
                    val noteText = tx.description.ifBlank { tx.title }

                    val row = listOf(
                        (index + 1).toString(),
                        escapeCsv(dateTimeText),
                        jenis,
                        item.incomeAmount.toString(),
                        item.expenseAmount.toString(),
                        item.runningBalance.toString(),
                        escapeCsv(noteText)
                    ).joinToString(",")
                    writer.write(row + "\n")
                }
            } else {
                writer.write("No,Tanggal,Jam Input,Jenis Transaksi,Nama / Sumber,Kategori,Metode Pembayaran,Pemasukan,Pengeluaran,Jumlah Saldo,Status,Keterangan Catatan\n")
                rows.forEachIndexed { index, item ->
                    val tx = item.transaction
                    val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: "-"
                    val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
                    val statusText = if (tx.status == "ACTIVE") "Aktif" else "Dibatalkan"

                    val row = listOf(
                        (index + 1).toString(),
                        escapeCsv(tx.date),
                        escapeCsv(inputTime),
                        jenis,
                        escapeCsv(tx.title),
                        escapeCsv(tx.category),
                        escapeCsv(tx.paymentMethod.ifBlank { "Tunai" }),
                        item.incomeAmount.toString(),
                        item.expenseAmount.toString(),
                        item.runningBalance.toString(),
                        statusText,
                        escapeCsv(tx.description)
                    ).joinToString(",")
                    writer.write(row + "\n")
                }
            }
        }
    }

    /**
     * Generates a genuine OOXML .xlsx workbook with:
     * - Styled kop header (Rows 1..6)
     * - Ringkasan Saldo Kas box (Rows 8..12, numeric format #,##0)
     * - Dark emerald table header (#0F6B5B, Row 14)
     * - Chronological transactions with auto-computed running balance (Rows 15+)
     * - Auto-adjusted column widths calculated from actual cell lengths so nothing is cut off on mobile
     */
    internal fun writeXlsxContent(
        outputStream: OutputStream,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long,
        columnMode: ExcelColumnMode
    ) {
        val isCompact = columnMode == ExcelColumnMode.COMPACT
        val rows = prepareChronologicalRows(transactions, initialBalance, onlyActive = isCompact)
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())

        val headers = if (isCompact) {
            listOf("No", "Tanggal & Waktu", "Jenis Transaksi", "Pemasukan", "Pengeluaran", "Jumlah Saldo", "Catatan")
        } else {
            listOf(
                "No", "Tanggal", "Jam Input", "Jenis Transaksi",
                "Nama / Sumber", "Kategori", "Metode Pembayaran",
                "Pemasukan", "Pengeluaran", "Jumlah Saldo", "Status", "Keterangan Catatan"
            )
        }

        val colCount = headers.size
        val maxLens = IntArray(colCount) { idx -> headers[idx].length }

        fun trackWidth(colIdx: Int, text: String) {
            if (colIdx in 0 until colCount) {
                if (text.length > maxLens[colIdx]) {
                    maxLens[colIdx] = text.length
                }
            }
        }

        fun formatNumLen(value: Long): Int {
            val absStr = kotlin.math.abs(value).toString()
            val commas = (absStr.length - 1) / 3
            return absStr.length + commas + (if (value < 0) 1 else 0)
        }

        // Track metadata & summary cell widths in Col A (0) and Col B (1)
        val metaEntries = listOf(
            "Organisasi / Kelompok" to settings.groupName.ifBlank { "Kas Transparan" },
            "Admin / Pengelola" to settings.adminName.ifBlank { "Admin Kas" },
            "Periode Laporan" to periodTitle,
            "Tanggal Dibuat" to printDate,
            "Mata Uang" to settings.currencyCode.ifBlank { "IDR" }
        )
        metaEntries.forEach { (label, value) ->
            trackWidth(0, label)
            trackWidth(1, value)
        }
        val summaryEntries = listOf(
            "Saldo Awal" to initialBalance,
            "Total Pemasukan" to totalIncome,
            "Total Pengeluaran" to totalExpense,
            "Saldo Akhir" to finalBalance
        )
        trackWidth(0, "RINGKASAN SALDO KAS")
        summaryEntries.forEach { (label, amount) ->
            trackWidth(0, label)
            val numLen = formatNumLen(amount)
            if (numLen > maxLens[1]) maxLens[1] = numLen
        }

        // Pre-compute transaction cell representations & track column widths
        val sheetXml = StringBuilder(16384)
        val rowsXml = StringBuilder(12288)

        // Row 1: LAPORAN KAS TRANSPARAN (A1:C1 merged)
        rowsXml.append("  <row r=\"1\" ht=\"24\" customHeight=\"1\">\n")
        rowsXml.append(inlineStrCell(0, 1, 1, "LAPORAN KAS TRANSPARAN"))
        rowsXml.append(emptyStyledCell(1, 1, 1))
        rowsXml.append(emptyStyledCell(2, 1, 1))
        rowsXml.append("  </row>\n")

        // Rows 2..6: Metadata
        metaEntries.forEachIndexed { idx, (label, value) ->
            val r = 2 + idx
            rowsXml.append("  <row r=\"$r\" ht=\"19\" customHeight=\"1\">\n")
            rowsXml.append(inlineStrCell(0, r, 2, label))
            rowsXml.append(inlineStrCell(1, r, 3, value))
            rowsXml.append("  </row>\n")
        }

        // Row 7: Blank spacer
        rowsXml.append("  <row r=\"7\" ht=\"10\" customHeight=\"1\"/>\n")

        // Row 8: RINGKASAN SALDO KAS
        rowsXml.append("  <row r=\"8\" ht=\"20\" customHeight=\"1\">\n")
        rowsXml.append(inlineStrCell(0, 8, 4, "RINGKASAN SALDO KAS"))
        rowsXml.append(emptyStyledCell(1, 8, 4))
        rowsXml.append("  </row>\n")

        // Rows 9..12: Summary balances
        summaryEntries.forEachIndexed { idx, (label, amount) ->
            val r = 9 + idx
            rowsXml.append("  <row r=\"$r\" ht=\"19\" customHeight=\"1\">\n")
            rowsXml.append(inlineStrCell(0, r, 4, label))
            rowsXml.append(numericCell(1, r, 5, amount))
            rowsXml.append("  </row>\n")
        }

        // Row 13: Blank spacer
        rowsXml.append("  <row r=\"13\" ht=\"10\" customHeight=\"1\"/>\n")

        // Row 14: Table Header
        rowsXml.append("  <row r=\"14\" ht=\"24\" customHeight=\"1\">\n")
        headers.forEachIndexed { colIdx, h ->
            rowsXml.append(inlineStrCell(colIdx, 14, 6, h))
        }
        rowsXml.append("  </row>\n")

        // Rows 15+: Transactions
        rows.forEachIndexed { index, item ->
            val r = 15 + index
            val tx = item.transaction
            val rowNo = (index + 1).toLong()
            val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: ""
            val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"

            rowsXml.append("  <row r=\"$r\" ht=\"20\" customHeight=\"1\">\n")
            if (isCompact) {
                val dateTimeText = if (inputTime.isNotBlank()) "${tx.date} $inputTime" else tx.date
                val noteText = tx.description.ifBlank { tx.title }

                trackWidth(0, rowNo.toString())
                trackWidth(1, dateTimeText)
                trackWidth(2, jenis)
                if (formatNumLen(item.incomeAmount) > maxLens[3]) maxLens[3] = formatNumLen(item.incomeAmount)
                if (formatNumLen(item.expenseAmount) > maxLens[4]) maxLens[4] = formatNumLen(item.expenseAmount)
                if (formatNumLen(item.runningBalance) > maxLens[5]) maxLens[5] = formatNumLen(item.runningBalance)
                trackWidth(6, noteText)

                rowsXml.append(numericCell(0, r, 8, rowNo))
                rowsXml.append(inlineStrCell(1, r, 8, dateTimeText))
                rowsXml.append(inlineStrCell(2, r, 7, jenis))
                rowsXml.append(numericCell(3, r, if (item.incomeAmount > 0L) 9 else 12, item.incomeAmount))
                rowsXml.append(numericCell(4, r, if (item.expenseAmount > 0L) 10 else 12, item.expenseAmount))
                rowsXml.append(numericCell(5, r, 11, item.runningBalance))
                rowsXml.append(inlineStrCell(6, r, 7, noteText))
            } else {
                val jamText = inputTime.ifBlank { "-" }
                val metode = tx.paymentMethod.ifBlank { "Tunai" }
                val statusText = if (tx.status == "ACTIVE") "Aktif" else "Dibatalkan"

                trackWidth(0, rowNo.toString())
                trackWidth(1, tx.date)
                trackWidth(2, jamText)
                trackWidth(3, jenis)
                trackWidth(4, tx.title)
                trackWidth(5, tx.category)
                trackWidth(6, metode)
                if (formatNumLen(item.incomeAmount) > maxLens[7]) maxLens[7] = formatNumLen(item.incomeAmount)
                if (formatNumLen(item.expenseAmount) > maxLens[8]) maxLens[8] = formatNumLen(item.expenseAmount)
                if (formatNumLen(item.runningBalance) > maxLens[9]) maxLens[9] = formatNumLen(item.runningBalance)
                trackWidth(10, statusText)
                trackWidth(11, tx.description)

                rowsXml.append(numericCell(0, r, 8, rowNo))
                rowsXml.append(inlineStrCell(1, r, 8, tx.date))
                rowsXml.append(inlineStrCell(2, r, 8, jamText))
                rowsXml.append(inlineStrCell(3, r, 7, jenis))
                rowsXml.append(inlineStrCell(4, r, 7, tx.title))
                rowsXml.append(inlineStrCell(5, r, 7, tx.category))
                rowsXml.append(inlineStrCell(6, r, 7, metode))
                rowsXml.append(numericCell(7, r, if (item.incomeAmount > 0L) 9 else 12, item.incomeAmount))
                rowsXml.append(numericCell(8, r, if (item.expenseAmount > 0L) 10 else 12, item.expenseAmount))
                rowsXml.append(numericCell(9, r, 11, item.runningBalance))
                rowsXml.append(inlineStrCell(10, r, 8, statusText))
                rowsXml.append(inlineStrCell(11, r, 7, tx.description))
            }
            rowsXml.append("  </row>\n")
        }

        // Build complete sheet1.xml with auto-computed <cols>
        sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sheetXml.append(" <cols>\n")
        for (c in 0 until colCount) {
            val minW = when {
                c == 0 -> 22.0 // accommodates "Organisasi / Kelompok" in A2 cleanly
                isCompact && c == 1 -> 24.0
                isCompact && c in 3..5 -> 15.0
                isCompact && c == 6 -> 28.0
                !isCompact && c in 7..9 -> 15.0
                else -> 12.0
            }
            val computed = (maxLens[c] + 4.0).coerceAtLeast(minW).coerceAtMost(58.0)
            val colOneBased = c + 1
            sheetXml.append("  <col min=\"$colOneBased\" max=\"$colOneBased\" width=\"${String.format(Locale.US, "%.1f", computed)}\" customWidth=\"1\"/>\n")
        }
        sheetXml.append(" </cols>\n")
        sheetXml.append(" <sheetData>\n")
        sheetXml.append(rowsXml)
        sheetXml.append(" </sheetData>\n")
        sheetXml.append(" <mergeCells count=\"1\">\n")
        sheetXml.append("  <mergeCell ref=\"A1:C1\"/>\n")
        sheetXml.append(" </mergeCells>\n")
        sheetXml.append("</worksheet>\n")

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml.toString())
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun colLetter(colIndex: Int): String {
        var idx = colIndex
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.insert(0, ('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.toString()
    }

    private fun inlineStrCell(colIdx: Int, row: Int, styleId: Int, text: String): String {
        val ref = "${colLetter(colIdx)}$row"
        val escaped = escapeXml(text)
        return "   <c r=\"$ref\" s=\"$styleId\" t=\"inlineStr\"><is><t xml:space=\"preserve\">$escaped</t></is></c>\n"
    }

    private fun numericCell(colIdx: Int, row: Int, styleId: Int, value: Long): String {
        val ref = "${colLetter(colIdx)}$row"
        return "   <c r=\"$ref\" s=\"$styleId\"><v>$value</v></c>\n"
    }

    private fun emptyStyledCell(colIdx: Int, row: Int, styleId: Int): String {
        val ref = "${colLetter(colIdx)}$row"
        return "   <c r=\"$ref\" s=\"$styleId\"/>\n"
    }

    private fun escapeXml(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            val code = ch.code
            val validXml = code == 0x9 || code == 0xA || code == 0xD || (code >= 0x20 && code <= 0xD7FF) || (code >= 0xE000 && code <= 0xFFFD)
            if (!validXml) continue
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";") || clean.contains("\t")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    fun openOrShareCsv(context: Context, result: ExcelExportResult) {
        val uri = result.contentUri ?: return
        val mimeType = when {
            result.fileName.endsWith(".xlsx", ignoreCase = true) -> XLSX_MIME
            result.fileName.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
            else -> CSV_MIME
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Buka Laporan Excel")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Excel Kas - ${result.fileName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Bagikan File Excel")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

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
        val result = exportAndSaveToDownloads(
            context = context,
            settings = settings,
            transactions = transactions,
            periodTitle = periodTitle,
            initialBalance = initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            format = "XLSX"
        )
        return result.file ?: File(context.cacheDir, result.fileName)
    }

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
 <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
 <Default Extension="xml" ContentType="application/xml"/>
 <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
 <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
 <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
 <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
 <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
 <sheets>
  <sheet name="Laporan Kas" sheetId="1" r:id="rId1"/>
 </sheets>
</workbook>
"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
 <numFmts count="1">
  <numFmt numFmtId="164" formatCode="#,##0"/>
 </numFmts>
 <fonts count="7">
  <font><sz val="10"/><color rgb="FF111827"/><name val="Arial"/></font>
  <font><b/><sz val="13"/><color rgb="FF0F6B5B"/><name val="Arial"/></font>
  <font><b/><sz val="10"/><color rgb="FF1F2937"/><name val="Arial"/></font>
  <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Arial"/></font>
  <font><b/><sz val="10"/><color rgb="FF15803D"/><name val="Arial"/></font>
  <font><b/><sz val="10"/><color rgb="FFC62828"/><name val="Arial"/></font>
  <font><b/><sz val="10"/><color rgb="FF0F6B5B"/><name val="Arial"/></font>
 </fonts>
 <fills count="4">
  <fill><patternFill patternType="none"/></fill>
  <fill><patternFill patternType="gray125"/></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FFD9EAD3"/></patternFill></fill>
  <fill><patternFill patternType="solid"><fgColor rgb="FF0F6B5B"/></patternFill></fill>
 </fills>
 <borders count="3">
  <border><left/><right/><top/><bottom/><diagonal/></border>
  <border>
   <left style="thin"><color rgb="FFCBD5E1"/></left>
   <right style="thin"><color rgb="FFCBD5E1"/></right>
   <top style="thin"><color rgb="FFCBD5E1"/></top>
   <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
   <diagonal/>
  </border>
  <border>
   <left style="thin"><color rgb="FF0A4A3F"/></left>
   <right style="thin"><color rgb="FF0A4A3F"/></right>
   <top style="thin"><color rgb="FF0A4A3F"/></top>
   <bottom style="thin"><color rgb="FF0A4A3F"/></bottom>
   <diagonal/>
  </border>
 </borders>
 <cellStyleXfs count="1">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
 </cellStyleXfs>
 <cellXfs count="13">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="center"/></xf>
  <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="164" fontId="2" fillId="2" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="0" fontId="3" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
  <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
  <xf numFmtId="164" fontId="4" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="164" fontId="5" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="164" fontId="6" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
  <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
 </cellXfs>
 <cellStyles count="1">
  <cellStyle name="Normal" xfId="0" builtinId="0"/>
 </cellStyles>
</styleSheet>
"""
}
