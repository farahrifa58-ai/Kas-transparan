package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfExportResult(
    val success: Boolean,
    val fileName: String,
    val savedPathDisplay: String,
    val file: File? = null,
    val contentUri: Uri? = null,
    val errorMessage: String? = null
)

object PdfReportGenerator {

    /**
     * Splits text into multiple lines so that no line exceeds [maxWidth] when measured with [p].
     * Preserves words where possible, breaking long words if necessary.
     */
    fun wrapTextToLines(p: Paint, text: String, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val lines = mutableListOf<String>()
        val words = text.split(Regex("\\s+"))
        var currentLine = StringBuilder()

        for (word in words) {
            if (word.isEmpty()) continue
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (p.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                if (p.measureText(word) > maxWidth) {
                    var chunk = StringBuilder()
                    for (ch in word) {
                        if (p.measureText(chunk.toString() + ch) <= maxWidth) {
                            chunk.append(ch)
                        } else {
                            if (chunk.isNotEmpty()) lines.add(chunk.toString())
                            chunk = StringBuilder().append(ch)
                        }
                    }
                    if (chunk.isNotEmpty()) {
                        currentLine = chunk
                    }
                } else {
                    currentLine = StringBuilder(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    fun generateReportPdf(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): File {
        val pdfDir = File(context.cacheDir, "reports")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val file = File(pdfDir, "Laporan_Kas_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        val isEn = settings.language.equals("en", ignoreCase = true)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val generatedDateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", if (isEn) Locale.ENGLISH else Locale("id", "ID"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#444444")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20") // Emerald green
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 35f

        fun drawHeaderAndSummary() {
            // Header accent bar
            paint.color = Color.parseColor("#1B5E20")
            canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 4f, paint)
            y += 20f

            canvas.drawText("KAS TRANSPARAN", 35f, y, headerPaint)
            y += 16f
            canvas.drawText(settings.groupName.ifBlank { "Kelompok / Organisasi" }, 35f, y, subHeaderPaint)
            y += 14f

            textPaint.textSize = 9f
            val periodLabel = if (isEn) "Period" else "Periode"
            val createdLabel = if (isEn) "Generated at" else "Dibuat pada"
            canvas.drawText("$periodLabel: $periodTitle", 35f, y, textPaint)
            canvas.drawText("$createdLabel: ${generatedDateFormat.format(Date())}", 35f, y + 12f, textPaint)
            y += 28f

            // Summary Card (Box)
            paint.color = Color.parseColor("#F1F8E9")
            canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 54f, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#C8E6C9")
            canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 54f, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            val cardY = y + 16f
            val initialLabel = if (isEn) "Opening Balance" else "Saldo Awal"
            val incomeLabel = if (isEn) "Total Income" else "Total Pemasukan"
            val expenseLabel = if (isEn) "Total Expense" else "Total Pengeluaran"
            val finalLabel = if (isEn) "FINAL BALANCE" else "SALDO AKHIR"

            canvas.drawText("$initialLabel: ${CurrencyHelper.format(initialBalance, settings.currencyCode)}", 45f, cardY, textPaint)
            canvas.drawText("$incomeLabel: ${CurrencyHelper.format(totalIncome, settings.currencyCode)}", 45f, cardY + 15f, boldPaint)
            canvas.drawText("$expenseLabel: ${CurrencyHelper.format(totalExpense, settings.currencyCode)}", 300f, cardY + 15f, boldPaint)

            boldPaint.textSize = 11f
            boldPaint.color = Color.parseColor("#1B5E20")
            canvas.drawText("$finalLabel: ${CurrencyHelper.format(finalBalance, settings.currencyCode)}", 45f, cardY + 31f, boldPaint)
            boldPaint.color = Color.BLACK
            boldPaint.textSize = 9f
            y += 70f
        }

        fun drawTableHeader() {
            paint.color = Color.parseColor("#E8F5E9")
            canvas.drawRect(30f, y, (pageWidth - 30).toFloat(), y + 20f, paint)

            val hDate = if (isEn) "Date & Time" else "Tanggal & Waktu"
            val hType = if (isEn) "Type" else "Jenis"
            val hTitle = if (isEn) "Title & Notes" else "Judul & Keterangan"
            val hCategory = if (isEn) "Category & Method" else "Kategori & Metode"
            val hAmount = if (isEn) "Amount" else "Nominal"

            canvas.drawText(hDate, 35f, y + 14f, boldPaint)
            canvas.drawText(hType, 120f, y + 14f, boldPaint)
            canvas.drawText(hTitle, 170f, y + 14f, boldPaint)
            canvas.drawText(hCategory, 350f, y + 14f, boldPaint)

            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(hAmount, 560f, y + 14f, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT

            y += 24f
        }

        drawHeaderAndSummary()
        drawTableHeader()

        val activeTransactions = transactions.filter { it.status == "ACTIVE" }

        for (tx in activeTransactions) {
            val isIncome = tx.type == "INCOME"

            // Col 1: Date & Time of input
            val inputTimeStr = tx.createdAt?.toDate()?.let { timeFormat.format(it) } ?: ""
            val dateTimeDisplay = if (inputTimeStr.isNotBlank()) "${tx.date}\n($inputTimeStr)" else tx.date

            // Col 2: Type badge text
            val typeText = if (isIncome) (if (isEn) "MASUK" else "MASUK") else (if (isEn) "KELUAR" else "KELUAR")

            // Col 3: Title & Wrapped Notes (Description)
            val titleLines = wrapTextToLines(boldPaint, tx.title, 170f)
            val notePrefix = if (isEn) "Catatan: " else "Catatan: "
            val noteLines = if (tx.description.isNotBlank()) {
                wrapTextToLines(notePaint, "$notePrefix${tx.description}", 170f)
            } else emptyList()

            // Col 4: Category & Payment Method
            val catLines = wrapTextToLines(textPaint, tx.category, 95f)
            val methodLines = if (tx.paymentMethod.isNotBlank()) {
                wrapTextToLines(notePaint, "[${tx.paymentMethod}]", 95f)
            } else emptyList()

            // Calculate dynamic row height based on actual number of wrapped lines
            val col3Height = (titleLines.size * 11.5f) + (noteLines.size * 10f)
            val col4Height = (catLines.size * 11.5f) + (methodLines.size * 10f)
            val contentMaxHeight = maxOf(col3Height, col4Height, 16f)
            val rowHeight = contentMaxHeight + 10f // generous spacing between rows

            // Check if page overflow
            if (y + rowHeight > pageHeight - 75f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
                drawTableHeader()
            }

            // Draw Col 1: Date & Time
            textPaint.textSize = 8.5f
            if (inputTimeStr.isNotBlank()) {
                canvas.drawText(tx.date, 35f, y, textPaint)
                canvas.drawText("($inputTimeStr)", 35f, y + 10.5f, notePaint)
            } else {
                canvas.drawText(tx.date, 35f, y, textPaint)
            }

            // Draw Col 2: Type
            boldPaint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            canvas.drawText(typeText, 120f, y, boldPaint)
            boldPaint.color = Color.BLACK

            // Draw Col 3: Title Lines
            var curYCol3 = y
            for (tLine in titleLines) {
                canvas.drawText(tLine, 170f, curYCol3, boldPaint)
                curYCol3 += 11.5f
            }
            // Draw Col 3: All Wrapped Note Lines (Never cut off!)
            for (nLine in noteLines) {
                canvas.drawText(nLine, 170f, curYCol3, notePaint)
                curYCol3 += 10f
            }

            // Draw Col 4: Category Lines
            var curYCol4 = y
            for (cLine in catLines) {
                canvas.drawText(cLine, 350f, curYCol4, textPaint)
                curYCol4 += 11.5f
            }
            // Draw Col 4: Method Lines
            for (mLine in methodLines) {
                canvas.drawText(mLine, 350f, curYCol4, notePaint)
                curYCol4 += 10f
            }

            // Draw Col 5: Amount (Right aligned)
            val amountFormatted = (if (isIncome) "+ " else "- ") + CurrencyHelper.format(tx.amount, settings.currencyCode)
            boldPaint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountFormatted, 560f, y, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT
            boldPaint.color = Color.BLACK

            // Separator line under this row
            val lineY = y + rowHeight - 4f
            paint.color = Color.parseColor("#E0E0E0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, lineY, (pageWidth - 30).toFloat(), lineY, paint)

            y += rowHeight
        }

        // Footer / Signature block
        if (y > pageHeight - 80f) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 45f
        } else {
            y += 24f
        }

        textPaint.textSize = 9f
        val managerTitle = if (isEn) "Pengelola Kas / Admin:" else "Pengelola Kas / Admin:"
        canvas.drawText(managerTitle, (pageWidth - 190).toFloat(), y, textPaint)
        y += 38f
        canvas.drawText(settings.adminName.ifBlank { "Administrator" }, (pageWidth - 190).toFloat(), y, boldPaint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    fun generateTransactionPdf(
        context: Context,
        settings: AppSettings,
        tx: Transaction
    ): File {
        val pdfDir = File(context.cacheDir, "reports")
        if (!pdfDir.exists()) pdfDir.mkdirs()
        val file = File(pdfDir, "Bukti_Transaksi_${tx.id.take(8)}_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        val pageWidth = 420
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val isEn = settings.language.equals("en", ignoreCase = true)
        val isIncome = tx.type == "INCOME"
        val isVoid = tx.status == "VOID"

        val timeFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", if (isEn) Locale.ENGLISH else Locale("id", "ID"))
        val dateCreatedStr = tx.createdAt?.toDate()?.let { timeFormat.format(it) } ?: tx.date

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val headerColor = Color.parseColor("#1B5E20")
        val typeColor = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")

        // Top accent header
        paint.color = headerColor
        canvas.drawRect(24f, 24f, (pageWidth - 24).toFloat(), 28f, paint)

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = headerColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("KAS TRANSPARAN", 28f, 50f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        canvas.drawText(settings.groupName.ifBlank { "Kelompok / Organisasi" }, 28f, 65f, subPaint)

        val receiptTitle = if (isIncome) (if (isEn) "CASH IN RECEIPT" else "BUKTI KAS MASUK")
                           else (if (isEn) "CASH OUT RECEIPT" else "BUKTI KAS KELUAR")
        val receiptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typeColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(receiptTitle, (pageWidth - 28).toFloat(), 50f, receiptPaint)

        if (isVoid) {
            val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("[DIBATALKAN / VOID]", (pageWidth - 28).toFloat(), 65f, voidPaint)
        }

        // Amount Box Container
        paint.color = Color.parseColor("#F9FBF9")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(24f, 80f, (pageWidth - 24).toFloat(), 155f, 8f, 8f, paint)

        paint.color = Color.parseColor("#E0E0E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(24f, 80f, (pageWidth - 24).toFloat(), 155f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 9.5f
        }
        canvas.drawText(if (isEn) "Total Amount:" else "Nominal Transaksi:", 38f, 104f, amountLabelPaint)

        val amountValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = typeColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val prefix = if (isIncome) "+ " else "- "
        canvas.drawText(prefix + CurrencyHelper.format(tx.amount, settings.currencyCode), 38f, 134f, amountValPaint)

        // Details Table
        var curY = 180f
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 30f, curY, labelPaint)
            val lines = wrapTextToLines(valuePaint, value, (pageWidth - 28 - 140).toFloat())
            if (lines.isEmpty()) {
                curY += 19f
                return
            }
            lines.forEachIndexed { i, line ->
                canvas.drawText(line, 140f, curY + (i * 12.5f), valuePaint)
            }
            curY += maxOf(19f, (lines.size * 12.5f) + 4f)
        }

        drawRow(if (isEn) "Transaction ID" else "No. Transaksi", tx.id.take(16))
        drawRow(if (isEn) "Date & Time" else "Tanggal & Waktu", dateCreatedStr)
        drawRow(if (isEn) "Title / Payee" else "Judul / Sumber", tx.title)
        drawRow(if (isEn) "Category" else "Kategori", tx.category)
        drawRow(if (isEn) "Payment Method" else "Metode Pembayaran", tx.paymentMethod.ifBlank { "Tunai" })
        if (tx.description.isNotBlank()) {
            drawRow(if (isEn) "Notes" else "Catatan", tx.description)
        }
        if (isVoid && tx.voidReason.isNotBlank()) {
            drawRow(if (isEn) "Void Reason" else "Alasan Batal", tx.voidReason)
        }

        // Line separator
        curY += 10f
        paint.color = Color.parseColor("#E0E0E0")
        paint.strokeWidth = 0.8f
        canvas.drawLine(24f, curY, (pageWidth - 24).toFloat(), curY, paint)

        // Signatures / Footer
        curY += 28f
        canvas.drawText(if (isEn) "Cash Manager / Admin:" else "Pengelola Kas / Admin:", (pageWidth - 170).toFloat(), curY, labelPaint)
        curY += 34f
        canvas.drawText(settings.adminName.ifBlank { "Administrator" }, (pageWidth - 170).toFloat(), curY, valuePaint)

        // Watermark footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Dibuat otomatis oleh Kas Transparan by Rafline Dev", (pageWidth / 2).toFloat(), (pageHeight - 20).toFloat(), footerPaint)

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    /**
     * Generates report PDF and saves directly into the phone's public Download folder.
     */
    fun generateAndSaveToDownloads(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): PdfExportResult {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }
        val fileName = "Laporan_Kas_${cleanGroup}_$timeStamp.pdf"

        // 1. First generate to internal cache so we have a ready-to-share file
        val cachedFile = try {
            val pdf = generateReportPdf(
                context = context,
                settings = settings,
                transactions = transactions,
                periodTitle = periodTitle,
                initialBalance = initialBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                finalBalance = finalBalance
            )
            val destFile = File(cachedFileDir(context), fileName)
            pdf.copyTo(destFile, overwrite = true)
            destFile
        } catch (e: Exception) {
            return PdfExportResult(
                success = false,
                fileName = fileName,
                savedPathDisplay = "",
                errorMessage = e.localizedMessage
            )
        }

        var publicUri: Uri? = null
        var displayLocation = "Download/$fileName"

        // 2. Save directly to device public Download folder
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
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

        return PdfExportResult(
            success = true,
            fileName = fileName,
            savedPathDisplay = displayLocation,
            file = cachedFile,
            contentUri = shareUri ?: publicUri
        )
    }

    private fun cachedFileDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Opens or shares the exported PDF file with standard Android Intent.
     */
    fun openOrSharePdf(context: Context, result: PdfExportResult) {
        val uri = result.contentUri ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Buka Dokumen PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Bagikan Dokumen PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
