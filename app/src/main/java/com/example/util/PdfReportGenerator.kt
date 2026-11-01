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
}
