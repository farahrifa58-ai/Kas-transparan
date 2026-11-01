package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.AppSettings
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

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
            color = Color.parseColor("#555555")
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

        fun truncateText(p: Paint, text: String, maxWidth: Float): String {
            if (p.measureText(text) <= maxWidth) return text
            val ellipsis = "..."
            val ellipsisWidth = p.measureText(ellipsis)
            val available = maxWidth - ellipsisWidth
            if (available <= 0f) return ellipsis
            var count = text.length
            while (count > 0 && p.measureText(text.substring(0, count)) > available) {
                count--
            }
            return text.substring(0, count) + ellipsis
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
            canvas.drawText(hCategory, 345f, y + 14f, boldPaint)

            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(hAmount, 560f, y + 14f, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT

            y += 24f
        }

        drawHeaderAndSummary()
        drawTableHeader()

        val activeTransactions = transactions.filter { it.status == "ACTIVE" }

        for (tx in activeTransactions) {
            // Check if page overflow
            if (y > pageHeight - 75f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 35f
                drawTableHeader()
            }

            val isIncome = tx.type == "INCOME"

            // Col 1: Date & Time of input
            val inputTimeStr = tx.createdAt?.toDate()?.let { timeFormat.format(it) } ?: ""
            val dateTimeDisplay = if (inputTimeStr.isNotBlank()) "${tx.date} ($inputTimeStr)" else tx.date
            textPaint.textSize = 8.5f
            val safeDate = truncateText(textPaint, dateTimeDisplay, 80f)
            canvas.drawText(safeDate, 35f, y, textPaint)

            // Col 2: Type badge
            boldPaint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            val typeText = if (isIncome) (if (isEn) "INCOME" else "MASUK") else (if (isEn) "EXPENSE" else "KELUAR")
            canvas.drawText(typeText, 120f, y, boldPaint)
            boldPaint.color = Color.BLACK

            // Col 3: Title (line 1) & Notes/Keterangan (line 2)
            val safeTitle = truncateText(boldPaint, tx.title, 165f)
            canvas.drawText(safeTitle, 170f, y, boldPaint)
            if (tx.description.isNotBlank()) {
                val notePrefix = if (isEn) "Note: " else "Catatan: "
                val fullNote = "$notePrefix${tx.description}"
                val safeNote = truncateText(notePaint, fullNote, 165f)
                canvas.drawText(safeNote, 170f, y + 10.5f, notePaint)
            }

            // Col 4: Category & Payment Method
            val safeCat = truncateText(textPaint, tx.category, 105f)
            canvas.drawText(safeCat, 345f, y, textPaint)
            if (tx.paymentMethod.isNotBlank()) {
                val safeMethod = truncateText(notePaint, "[${tx.paymentMethod}]", 105f)
                canvas.drawText(safeMethod, 345f, y + 10.5f, notePaint)
            }

            // Col 5: Amount (Right aligned)
            val amountFormatted = (if (isIncome) "+ " else "- ") + CurrencyHelper.format(tx.amount, settings.currencyCode)
            boldPaint.color = if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountFormatted, 560f, y, boldPaint)
            boldPaint.textAlign = Paint.Align.LEFT
            boldPaint.color = Color.BLACK

            // Separator line
            val rowHeight = if (tx.description.isNotBlank() || tx.paymentMethod.isNotBlank()) 25f else 18f
            paint.color = Color.parseColor("#E0E0E0")
            paint.strokeWidth = 0.5f
            canvas.drawLine(30f, y + rowHeight - 6f, (pageWidth - 30).toFloat(), y + rowHeight - 6f, paint)

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
        val managerTitle = if (isEn) "Cash Manager / Admin:" else "Pengelola Kas / Admin:"
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
            val cleanValue = if (value.length > 38) value.take(36) + "..." else value
            canvas.drawText(cleanValue, 140f, curY, valuePaint)
            curY += 19f
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
}
