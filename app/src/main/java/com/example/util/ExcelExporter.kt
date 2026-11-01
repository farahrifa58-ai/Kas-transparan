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
import java.io.FileInputStream
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
     * Exports transactions to Excel (.xls) or CSV (.csv) and saves directly into the phone's public Download folder.
     * @param format "XLS" for styled XML Spreadsheet (native columns in Google Sheets & MS Excel), or "CSV" for comma-delimited.
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
        format: String = "XLS"
    ): ExcelExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }

        val isXls = format.equals("XLS", ignoreCase = true)
        val fileExt = if (isXls) "xls" else "csv"
        val mimeType = if (isXls) "application/vnd.ms-excel" else "text/csv"
        val fileName = "Laporan_Kas_${cleanGroup}_$timeStamp.$fileExt"

        // 1. Write to internal cache first
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val cachedFile = File(exportDir, fileName)

        try {
            FileOutputStream(cachedFile).use { os ->
                if (isXls) {
                    writeSpreadsheetXmlContent(os, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
                } else {
                    writeCsvContent(os, settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
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

        // 2. Save directly to public Downloads folder
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
                        FileInputStream(cachedFile).use { `is` ->
                            `is`.copyTo(os)
                        }
                    }
                    publicUri = uri
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val publicFile = File(downloadsDir, fileName)
                FileInputStream(cachedFile).use { `is` ->
                    FileOutputStream(publicFile).use { os ->
                        `is`.copyTo(os)
                    }
                }
                displayLocation = publicFile.absolutePath
            }
        } catch (e: Exception) {
            displayLocation = "Internal Storage ($fileName)"
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

    /**
     * Writes standard RFC-4180 comma-delimited CSV with UTF-8 BOM so mobile apps separate columns cleanly.
     */
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
            // Write UTF-8 BOM so Google Sheets, Excel & WPS recognize UTF-8 characters and columns
            writer.write("\uFEFF")

            writer.write("LAPORAN KEUANGAN KAS TRANSPARAN\n")
            writer.write("Organisasi / Kelompok,${escapeCsv(settings.groupName)}\n")
            writer.write("Admin / Pengelola,${escapeCsv(settings.adminName)}\n")
            writer.write("Periode Laporan,${escapeCsv(periodTitle)}\n")
            val printDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.write("Tanggal Dibuat,${escapeCsv(printDate)}\n")
            writer.write("Mata Uang,${settings.currencyCode}\n\n")

            writer.write("RINGKASAN SALDO KAS\n")
            writer.write("Saldo Awal,${initialBalance}\n")
            writer.write("Total Pemasukan,${totalIncome}\n")
            writer.write("Total Pengeluaran,${totalExpense}\n")
            writer.write("Saldo Akhir (Saldo Awal + Masuk - Keluar),${finalBalance}\n\n")

            // Headers: Comma separated so mobile Excel / Sheets display distinct columns A to K!
            writer.write("No,Tanggal,Jam Input,Jenis Transaksi,Nama / Sumber,Kategori,Metode Pembayaran,Pemasukan,Pengeluaran,Status,Keterangan Catatan\n")

            // Sort chronologically ascending: oldest at top, newer transactions downwards
            val chronologicalTransactions = transactions.reversed().sortedWith(
                compareBy<Transaction> { it.date }
                    .thenBy { it.createdAt?.toDate()?.time ?: 0L }
            )

            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            chronologicalTransactions.forEachIndexed { index, tx ->
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
                ).joinToString(",")
                writer.write(row + "\n")
            }
        }
    }

    /**
     * Writes genuine SpreadsheetML (XML Spreadsheet 2003) with .xls extension.
     * Natively supported by Google Sheets, Microsoft Excel (mobile & desktop), and WPS Office.
     * Guarantees separate columns, bold green header styling, explicit column widths, and proper cell types!
     */
    private fun writeSpreadsheetXmlContent(
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
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n")
            writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            writer.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

            // Styles
            writer.write(" <Styles>\n")
            writer.write("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Color=\"#000000\"/>\n")
            writer.write("  </Style>\n")

            // App Title Style
            writer.write("  <Style ss:ID=\"AppTitle\">\n")
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"14\" ss:Bold=\"1\" ss:Color=\"#1B5E20\"/>\n")
            writer.write("  </Style>\n")

            // Meta Label Style
            writer.write("  <Style ss:ID=\"MetaLabel\">\n")
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#333333\"/>\n")
            writer.write("  </Style>\n")

            // Table Header Style (Emerald Green with White Text)
            writer.write("  <Style ss:ID=\"HeaderStyle\">\n")
            writer.write("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
            writer.write("   <Borders>\n")
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0D3813\"/>\n")
            writer.write("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0D3813\"/>\n")
            writer.write("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0D3813\"/>\n")
            writer.write("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#0D3813\"/>\n")
            writer.write("   </Borders>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
            writer.write("   <Interior ss:Color=\"#1B5E20\" ss:Pattern=\"Solid\"/>\n")
            writer.write("  </Style>\n")

            // Data Cell Bordered
            writer.write("  <Style ss:ID=\"DataCell\">\n")
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n")
            writer.write("   <Borders>\n")
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("   </Borders>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\"/>\n")
            writer.write("  </Style>\n")

            // Center Data Cell
            writer.write("  <Style ss:ID=\"DataCellCenter\">\n")
            writer.write("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
            writer.write("   <Borders>\n")
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("   </Borders>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\"/>\n")
            writer.write("  </Style>\n")

            // Income Amount Style (Green Text)
            writer.write("  <Style ss:ID=\"IncomeStyle\">\n")
            writer.write("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            writer.write("   <Borders>\n")
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("   </Borders>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#2E7D32\"/>\n")
            writer.write("   <NumberFormat ss:Format=\"#,##0\"/>\n")
            writer.write("  </Style>\n")

            // Expense Amount Style (Red Text)
            writer.write("  <Style ss:ID=\"ExpenseStyle\">\n")
            writer.write("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            writer.write("   <Borders>\n")
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("    <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E0E0E0\"/>\n")
            writer.write("   </Borders>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#C62828\"/>\n")
            writer.write("   <NumberFormat ss:Format=\"#,##0\"/>\n")
            writer.write("  </Style>\n")

            // Summary Card Style
            writer.write("  <Style ss:ID=\"SummaryStyle\">\n")
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#1B5E20\"/>\n")
            writer.write("   <Interior ss:Color=\"#F1F8E9\" ss:Pattern=\"Solid\"/>\n")
            writer.write("  </Style>\n")

            writer.write("  <Style ss:ID=\"SummaryNumStyle\">\n")
            writer.write("   <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>\n")
            writer.write("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#1B5E20\"/>\n")
            writer.write("   <Interior ss:Color=\"#F1F8E9\" ss:Pattern=\"Solid\"/>\n")
            writer.write("   <NumberFormat ss:Format=\"#,##0\"/>\n")
            writer.write("  </Style>\n")

            writer.write(" </Styles>\n")

            // Worksheet & Table definition
            writer.write(" <Worksheet ss:Name=\"Laporan Kas\">\n")
            writer.write("  <Table>\n")

            // Explicit column widths so columns are perfectly proportioned!
            writer.write("   <Column ss:Width=\"35\"/>\n")  // Col A: No
            writer.write("   <Column ss:Width=\"85\"/>\n")  // Col B: Tanggal
            writer.write("   <Column ss:Width=\"65\"/>\n")  // Col C: Jam Input
            writer.write("   <Column ss:Width=\"90\"/>\n")  // Col D: Jenis
            writer.write("   <Column ss:Width=\"170\"/>\n") // Col E: Nama / Sumber
            writer.write("   <Column ss:Width=\"110\"/>\n") // Col F: Kategori
            writer.write("   <Column ss:Width=\"90\"/>\n")  // Col G: Metode
            writer.write("   <Column ss:Width=\"100\"/>\n") // Col H: Pemasukan
            writer.write("   <Column ss:Width=\"100\"/>\n") // Col I: Pengeluaran
            writer.write("   <Column ss:Width=\"75\"/>\n")  // Col J: Status
            writer.write("   <Column ss:Width=\"240\"/>\n") // Col K: Catatan Keterangan

            // Row 1: Title
            writer.write("   <Row ss:Height=\"24\">\n")
            writer.write("    <Cell ss:MergeAcross=\"3\" ss:StyleID=\"AppTitle\"><Data ss:Type=\"String\">LAPORAN KAS TRANSPARAN</Data></Cell>\n")
            writer.write("   </Row>\n")

            // Metadata rows
            writeXmlMetaRow(writer, "Organisasi / Kelompok", settings.groupName)
            writeXmlMetaRow(writer, "Admin / Pengelola", settings.adminName)
            writeXmlMetaRow(writer, "Periode Laporan", periodTitle)
            val printDate = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            writeXmlMetaRow(writer, "Tanggal Dibuat", printDate)
            writeXmlMetaRow(writer, "Mata Uang", settings.currencyCode)

            // Blank row
            writer.write("   <Row ss:Height=\"10\"/>\n")

            // Summary Table
            writer.write("   <Row ss:Height=\"20\">\n")
            writer.write("    <Cell ss:MergeAcross=\"2\" ss:StyleID=\"SummaryStyle\"><Data ss:Type=\"String\">RINGKASAN SALDO KAS</Data></Cell>\n")
            writer.write("   </Row>\n")
            writeXmlSummaryRow(writer, "Saldo Awal", initialBalance)
            writeXmlSummaryRow(writer, "Total Pemasukan", totalIncome)
            writeXmlSummaryRow(writer, "Total Pengeluaran", totalExpense)
            writeXmlSummaryRow(writer, "Saldo Akhir", finalBalance)

            // Blank row
            writer.write("   <Row ss:Height=\"12\"/>\n")

            // Table Header: Row 15
            writer.write("   <Row ss:Height=\"24\">\n")
            val headers = listOf(
                "No", "Tanggal", "Jam Input", "Jenis Transaksi",
                "Nama / Sumber", "Kategori", "Metode Pembayaran",
                "Pemasukan", "Pengeluaran", "Status", "Keterangan Catatan"
            )
            for (h in headers) {
                writer.write("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">$h</Data></Cell>\n")
            }
            writer.write("   </Row>\n")

            // Sort chronologically ascending: oldest transactions at top (Row 15, No 1), newer transactions downwards
            val chronologicalTransactions = transactions.reversed().sortedWith(
                compareBy<Transaction> { it.date }
                    .thenBy { it.createdAt?.toDate()?.time ?: 0L }
            )

            // Transactions rows
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            chronologicalTransactions.forEachIndexed { index, tx ->
                val isIncome = tx.type == "INCOME" && tx.status == "ACTIVE"
                val isExpense = tx.type == "EXPENSE" && tx.status == "ACTIVE"
                val inputTime = tx.createdAt?.toDate()?.let { timeSdf.format(it) } ?: "-"

                writer.write("   <Row ss:Height=\"20\">\n")
                // No
                writer.write("    <Cell ss:StyleID=\"DataCellCenter\"><Data ss:Type=\"Number\">${index + 1}</Data></Cell>\n")
                // Tanggal
                writer.write("    <Cell ss:StyleID=\"DataCellCenter\"><Data ss:Type=\"String\">${escapeXml(tx.date)}</Data></Cell>\n")
                // Jam
                writer.write("    <Cell ss:StyleID=\"DataCellCenter\"><Data ss:Type=\"String\">${escapeXml(inputTime)}</Data></Cell>\n")
                // Jenis
                val jenis = if (tx.type == "INCOME") "Pemasukan" else "Pengeluaran"
                writer.write("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">$jenis</Data></Cell>\n")
                // Nama
                writer.write("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(tx.title)}</Data></Cell>\n")
                // Kategori
                writer.write("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(tx.category)}</Data></Cell>\n")
                // Metode
                writer.write("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(tx.paymentMethod.ifBlank { "Tunai" })}</Data></Cell>\n")
                // Pemasukan
                val incVal = if (isIncome) tx.amount else 0L
                writer.write("    <Cell ss:StyleID=\"IncomeStyle\"><Data ss:Type=\"Number\">$incVal</Data></Cell>\n")
                // Pengeluaran
                val expVal = if (isExpense) tx.amount else 0L
                writer.write("    <Cell ss:StyleID=\"ExpenseStyle\"><Data ss:Type=\"Number\">$expVal</Data></Cell>\n")
                // Status
                val statusText = if (tx.status == "ACTIVE") "Aktif" else "Dibatalkan"
                writer.write("    <Cell ss:StyleID=\"DataCellCenter\"><Data ss:Type=\"String\">$statusText</Data></Cell>\n")
                // Catatan Keterangan
                writer.write("    <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(tx.description)}</Data></Cell>\n")
                writer.write("   </Row>\n")
            }

            writer.write("  </Table>\n")
            writer.write(" </Worksheet>\n")
            writer.write("</Workbook>\n")
        }
    }

    private fun writeXmlMetaRow(writer: OutputStreamWriter, label: String, value: String) {
        writer.write("   <Row ss:Height=\"18\">\n")
        writer.write("    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"MetaLabel\"><Data ss:Type=\"String\">${escapeXml(label)}</Data></Cell>\n")
        writer.write("    <Cell ss:MergeAcross=\"2\" ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(value)}</Data></Cell>\n")
        writer.write("   </Row>\n")
    }

    private fun writeXmlSummaryRow(writer: OutputStreamWriter, label: String, amount: Long) {
        writer.write("   <Row ss:Height=\"18\">\n")
        writer.write("    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"SummaryStyle\"><Data ss:Type=\"String\">${escapeXml(label)}</Data></Cell>\n")
        writer.write("    <Cell ss:MergeAcross=\"1\" ss:StyleID=\"SummaryNumStyle\"><Data ss:Type=\"Number\">$amount</Data></Cell>\n")
        writer.write("   </Row>\n")
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"") || clean.contains(";") || clean.contains("\t")) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    /**
     * Helper to open the exported file or share it directly
     */
    fun openOrShareCsv(context: Context, result: ExcelExportResult) {
        val uri = result.contentUri ?: return
        val isXls = result.fileName.endsWith(".xls", ignoreCase = true)
        val mimeType = if (isXls) "application/vnd.ms-excel" else "text/csv"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Buka Laporan Excel")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
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
        val result = exportAndSaveToDownloads(
            context = context,
            settings = settings,
            transactions = transactions,
            periodTitle = periodTitle,
            initialBalance = initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            format = "XLS"
        )
        return result.file ?: File(context.cacheDir, result.fileName)
    }
}
