package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportImageGenerator {

    fun generateReportBitmap(
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): Bitmap {
        val width = 1080
        val padding = 48f
        val curr = settings.currencyCode

        // Determine height dynamically based on transaction count
        val maxTxToShow = minOf(transactions.size, 25)
        val txItemHeight = 96f
        val headerHeight = 320f
        val summaryCardHeight = 340f
        val footerHeight = 160f
        val txSectionHeaderHeight = 80f
        val txListHeight = if (transactions.isEmpty()) 100f else (maxTxToShow * txItemHeight) + 40f
        val extraNoteHeight = if (transactions.size > maxTxToShow) 60f else 0f

        val totalHeight = (headerHeight + summaryCardHeight + txSectionHeaderHeight + txListHeight + extraNoteHeight + footerHeight + (padding * 2)).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F4F6F8")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        // Header Card (Deep Green Emerald)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            isAntiAlias = true
        }
        val headerRect = RectF(padding, padding, width - padding, padding + 280f)
        canvas.drawRoundRect(headerRect, 28f, 28f, headerPaint)

        // Decorative subtle top accent bar inside header
        val accentPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(padding + 24f, padding + 20f, padding + 160f, padding + 28f), 4f, 4f, accentPaint)

        // App Label
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            letterSpacing = 0.08f
        }
        canvas.drawText("KAS TRANSPARAN", padding + 28f, padding + 76f, textPaint)

        // Group / Organization Name
        val groupName = settings.groupName.ifBlank { "Kelompok Kas" }
        textPaint.textSize = 46f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(groupName, padding + 28f, padding + 140f, textPaint)

        // Period & Manager
        val subTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#E8F5E9")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Periode: $periodTitle", padding + 28f, padding + 190f, subTextPaint)
        canvas.drawText("Pengelola: ${settings.adminName}", padding + 28f, padding + 230f, subTextPaint)

        // Date of Generation on right side
        val nowStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        val dateTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#C8E6C9")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(nowStr, width - padding - 28f, padding + 76f, dateTextPaint)

        // Summary Card (White Container with shadow/stroke)
        var currentY = padding + 310f
        val summaryRect = RectF(padding, currentY, width - padding, currentY + summaryCardHeight - 20f)
        val cardBgPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(summaryRect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(summaryRect, 24f, 24f, cardBorderPaint)

        val summaryTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#263238")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("RINGKASAN KEUANGAN", padding + 28f, currentY + 48f, summaryTitlePaint)

        // Rows inside summary
        val rowLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#546E7A")
            textSize = 26f
        }
        val rowValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#212121")
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 1. Saldo Awal
        val row1Y = currentY + 100f
        canvas.drawText("Saldo Awal", padding + 28f, row1Y, rowLabelPaint)
        canvas.drawText(CurrencyHelper.format(initialBalance, curr), width - padding - 28f, row1Y, rowValPaint)

        // 2. Total Pemasukan
        val row2Y = currentY + 145f
        canvas.drawText("Total Pemasukan (+)", padding + 28f, row2Y, rowLabelPaint)
        rowValPaint.color = Color.parseColor("#2E7D32")
        canvas.drawText("+ " + CurrencyHelper.format(totalIncome, curr), width - padding - 28f, row2Y, rowValPaint)

        // 3. Total Pengeluaran
        val row3Y = currentY + 190f
        canvas.drawText("Total Pengeluaran (-)", padding + 28f, row3Y, rowLabelPaint)
        rowValPaint.color = Color.parseColor("#C62828")
        canvas.drawText("- " + CurrencyHelper.format(totalExpense, curr), width - padding - 28f, row3Y, rowValPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 2f
        }
        canvas.drawLine(padding + 28f, currentY + 225f, width - padding - 28f, currentY + 225f, dividerPaint)

        // 4. Saldo Akhir (Highlighted)
        val row4Y = currentY + 275f
        val finalLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1B5E20")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val finalValPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1B5E20")
            textSize = 34f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("SALDO AKHIR", padding + 28f, row4Y, finalLabelPaint)
        canvas.drawText(CurrencyHelper.format(finalBalance, curr), width - padding - 28f, row4Y, finalValPaint)

        // Section: Daftar Transaksi
        currentY += summaryCardHeight + 10f
        val txSectionRect = RectF(padding, currentY, width - padding, currentY + txSectionHeaderHeight + txListHeight + extraNoteHeight)
        canvas.drawRoundRect(txSectionRect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(txSectionRect, 24f, 24f, cardBorderPaint)

        val txHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#263238")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Daftar Transaksi (${transactions.size} item)", padding + 28f, currentY + 48f, txHeaderPaint)
        canvas.drawLine(padding + 28f, currentY + 68f, width - padding - 28f, currentY + 68f, dividerPaint)

        var txItemY = currentY + 115f
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        if (transactions.isEmpty()) {
            val emptyPaint = Paint().apply {
                isAntiAlias = true
                color = Color.GRAY
                textSize = 24f
            }
            canvas.drawText("Tidak ada transaksi pada periode ini.", padding + 28f, txItemY, emptyPaint)
        } else {
            for (i in 0 until maxTxToShow) {
                val tx = transactions[i]
                val isIncome = tx.type == "INCOME"
                val isVoid = tx.status == "VOID"

                // Title
                val titlePaint = Paint().apply {
                    isAntiAlias = true
                    color = if (isVoid) Color.GRAY else Color.parseColor("#212121")
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val safeTitle = if (tx.title.length > 32) tx.title.take(30) + ".." else tx.title
                val voidPrefix = if (isVoid) "[BATAL] " else ""
                canvas.drawText("$voidPrefix$safeTitle", padding + 28f, txItemY, titlePaint)

                // Subtitle (Date, Time, Category, Notes)
                val timeStr = tx.createdAt?.toDate()?.let { " (${timeSdf.format(it)})" } ?: ""
                val noteStr = if (tx.description.isNotBlank()) " • " + (if (tx.description.length > 25) tx.description.take(23) + ".." else tx.description) else ""
                val subTxPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#78909C")
                    textSize = 21f
                }
                canvas.drawText("${tx.date}$timeStr • ${tx.category}$noteStr", padding + 28f, txItemY + 30f, subTxPaint)

                // Amount
                val amountPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = when {
                        isVoid -> Color.GRAY
                        isIncome -> Color.parseColor("#2E7D32")
                        else -> Color.parseColor("#C62828")
                    }
                }
                val sign = if (isIncome) "+ " else "- "
                canvas.drawText(sign + CurrencyHelper.format(tx.amount, curr), width - padding - 28f, txItemY + 12f, amountPaint)

                // Subtle item separator
                if (i < maxTxToShow - 1) {
                    val subDivider = Paint().apply {
                        color = Color.parseColor("#ECEFF1")
                        strokeWidth = 1.5f
                    }
                    canvas.drawLine(padding + 28f, txItemY + 48f, width - padding - 28f, txItemY + 48f, subDivider)
                }

                txItemY += txItemHeight
            }

            if (transactions.size > maxTxToShow) {
                val remaining = transactions.size - maxTxToShow
                val remPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#546E7A")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                canvas.drawText("... dan $remaining transaksi lainnya (lihat laporan PDF / Excel lengkap)", padding + 28f, txItemY, remPaint)
            }
        }

        // Footer
        val footerY = totalHeight - padding - 40f
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#78909C")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Aplikasi Kas Transparan • Laporan Keuangan Akuntabel, Rapi & Transparan", width / 2f, footerY, footerPaint)
        val footerSubPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#90A4AE")
            textSize = 19f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Bagikan ke grup WhatsApp atau simpan sebagai dokumentasi kas organisasi", width / 2f, footerY + 30f, footerSubPaint)

        return bitmap
    }

    /**
     * Saves generated bitmap to a PNG file in cacheDir
     */
    fun saveReportImage(
        context: Context,
        settings: AppSettings,
        transactions: List<Transaction>,
        periodTitle: String,
        initialBalance: Long,
        totalIncome: Long,
        totalExpense: Long,
        finalBalance: Long
    ): File {
        val bitmap = generateReportBitmap(settings, transactions, periodTitle, initialBalance, totalIncome, totalExpense, finalBalance)
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanGroup = settings.groupName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(15)
            .ifBlank { "Kas" }
        val imageFile = File(exportDir, "Laporan_Kas_${cleanGroup}_$timeStamp.png")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return imageFile
    }

    /**
     * Shares report image directly to WhatsApp, Telegram, or any messaging app
     */
    fun shareReportImage(
        context: Context,
        imageFile: File,
        captionText: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, captionText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Bagikan Gambar Laporan Kas")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun generateTransactionReceiptBitmap(
        settings: AppSettings,
        tx: Transaction
    ): Bitmap {
        val width = 800
        val height = 1000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isIncome = tx.type == "INCOME"
        val isVoid = tx.status == "VOID"
        val curr = settings.currencyCode

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F4F6F8")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Main Receipt Card Container
        val cardRect = RectF(36f, 36f, width - 36f, height - 36f)
        val cardBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 24f, 24f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardBorderPaint)

        // Header Background
        val headerRect = RectF(36f, 36f, width - 36f, 200f)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1B5E20")
            isAntiAlias = true
        }
        canvas.drawRoundRect(headerRect, 24f, 24f, headerPaint)
        canvas.drawRect(36f, 160f, width - 36f, 200f, headerPaint)

        // Header Texts
        val titlePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("KAS TRANSPARAN", 64f, 85f, titlePaint)

        val groupPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val groupName = settings.groupName.ifBlank { "Kelompok Kas" }
        canvas.drawText(if (groupName.length > 24) groupName.take(22) + "..." else groupName, 64f, 135f, groupPaint)

        val receiptBadgePaint = Paint().apply {
            color = Color.parseColor("#E8F5E9")
            isAntiAlias = true
            textSize = 20f
        }
        val receiptTypeLabel = if (isIncome) "BUKTI KAS MASUK" else "BUKTI KAS KELUAR"
        canvas.drawText(receiptTypeLabel, 64f, 175f, receiptBadgePaint)

        // Amount Box
        val amountBoxRect = RectF(64f, 230f, width - 64f, 350f)
        val amountBoxBg = Paint().apply {
            color = if (isIncome) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val amountBoxBorder = Paint().apply {
            color = if (isIncome) Color.parseColor("#C8E6C9") else Color.parseColor("#FFCDD2")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(amountBoxRect, 18f, 18f, amountBoxBg)
        canvas.drawRoundRect(amountBoxRect, 18f, 18f, amountBoxBorder)

        val amountSubPaint = Paint().apply {
            color = Color.parseColor("#555555")
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("Nominal Transaksi:", 90f, 268f, amountSubPaint)

        val amountValPaint = Paint().apply {
            color = if (isVoid) Color.GRAY else if (isIncome) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val prefix = if (isIncome) "+ " else "- "
        canvas.drawText(prefix + CurrencyHelper.format(tx.amount, curr), 90f, 320f, amountValPaint)

        if (isVoid) {
            val voidBadgePaint = Paint().apply {
                color = Color.RED
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.drawText("[DIBATALKAN]", width - 90f, 268f, voidBadgePaint)
        }

        // Details Section
        var rowY = 405f
        val labelPaint = Paint().apply {
            color = Color.parseColor("#546E7A")
            textSize = 22f
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#ECEFF1")
            strokeWidth = 1.5f
        }

        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val inputTimeStr = tx.createdAt?.toDate()?.let { " (${timeSdf.format(it)})" } ?: ""

        fun drawDetailRow(label: String, value: String) {
            canvas.drawText(label, 64f, rowY, labelPaint)
            val cleanVal = if (value.length > 28) value.take(26) + "..." else value
            canvas.drawText(cleanVal, 300f, rowY, valPaint)
            canvas.drawLine(64f, rowY + 16f, width - 64f, rowY + 16f, dividerPaint)
            rowY += 52f
        }

        drawDetailRow("No. Transaksi", tx.id.take(16))
        drawDetailRow("Tanggal & Waktu", "${tx.date}$inputTimeStr")
        drawDetailRow("Judul / Sumber", tx.title)
        drawDetailRow("Kategori", tx.category)
        drawDetailRow("Metode Bayar", tx.paymentMethod.ifBlank { "Tunai" })
        if (tx.description.isNotBlank()) {
            drawDetailRow("Catatan", tx.description)
        }
        if (isVoid && tx.voidReason.isNotBlank()) {
            drawDetailRow("Alasan Batal", tx.voidReason)
        }

        // Signatures & Admin
        val adminY = 820f
        val adminLabel = Paint().apply {
            color = Color.parseColor("#78909C")
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val adminVal = Paint().apply {
            color = Color.parseColor("#263238")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Pengelola Kas:", width - 64f, adminY, adminLabel)
        canvas.drawText(settings.adminName.ifBlank { "Admin" }, width - 64f, adminY + 36f, adminVal)

        // Footer Watermark
        val footerPaint = Paint().apply {
            color = Color.parseColor("#90A4AE")
            textSize = 19f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Dibuat otomatis oleh Kas Transparan by Rafline Dev", width / 2f, height - 60f, footerPaint)

        return bitmap
    }

    fun saveTransactionReceiptImage(
        context: Context,
        settings: AppSettings,
        tx: Transaction
    ): File {
        val bitmap = generateTransactionReceiptBitmap(settings, tx)
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(exportDir, "Struk_Transaksi_${tx.id.take(8)}_$timeStamp.png")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return imageFile
    }
}
