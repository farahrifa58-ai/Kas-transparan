package com.example

import com.example.data.model.AppSettings
import com.example.data.model.ExcelColumnMode
import com.example.data.model.ExportPreferences
import com.example.data.model.Transaction
import com.example.util.DocReportGenerator
import com.example.util.ExcelExporter
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream

class ExportReportTest {

    private fun ts(dateTimeStr: String): Timestamp {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse(dateTimeStr)!!
        return Timestamp(date)
    }

    private fun sampleTransactions(): List<Transaction> = listOf(
        // Intentionally out of chronological order (newest first) + includes a VOID transaction
        Transaction(
            id = "3",
            type = "EXPENSE",
            title = "Beli Sapu",
            amount = 25_000L,
            category = "Kebersihan",
            date = "2026-11-26",
            description = "beli sapu lidi",
            paymentMethod = "Tunai",
            status = "ACTIVE",
            createdAt = ts("2026-11-26 09:15")
        ),
        Transaction(
            id = "void_1",
            type = "EXPENSE",
            title = "Salah Input",
            amount = 50_000L,
            category = "Lainnya",
            date = "2026-11-25",
            description = "dibatalkan",
            paymentMethod = "Tunai",
            status = "VOID",
            createdAt = ts("2026-11-25 12:00")
        ),
        Transaction(
            id = "2",
            type = "INCOME",
            title = "Iuran Harian 2",
            amount = 103_000L,
            category = "Iuran",
            date = "2026-11-25",
            description = "jumlah iuran kas hari ini",
            paymentMethod = "Tunai",
            status = "ACTIVE",
            createdAt = ts("2026-11-25 10:36")
        ),
        Transaction(
            id = "1",
            type = "INCOME",
            title = "Iuran Harian 1",
            amount = 110_000L,
            category = "Iuran",
            date = "2026-11-24",
            description = "jumlah iuran terkumpul hari ini",
            paymentMethod = "Tunai",
            status = "ACTIVE",
            createdAt = ts("2026-11-24 10:13")
        )
    )

    @Test
    fun exportPreferences_enforcesAtLeastOneActive() {
        val defaults = ExportPreferences()
        assertEquals(4, defaults.activeCount)

        val allOff = ExportPreferences(
            showShareImage = false,
            showSharePdf = false,
            showShareExcel = false,
            showShareDoc = false
        )
        assertEquals(0, allOff.activeCount)
        assertEquals(1, allOff.ensureAtLeastOneActive().activeCount)
        assertTrue(allOff.ensureAtLeastOneActive().showSharePdf)
    }

    @Test
    fun chronologicalRunningBalance_matchesFinalBalance() {
        val initialBalance = 50_000L
        val txs = sampleTransactions()
        val activeTxs = txs.filter { it.status == "ACTIVE" }
        val totalIncome = activeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val expectedFinalBalance = initialBalance + totalIncome - totalExpense // 50k + 213k - 25k = 238k

        val compactRows = ExcelExporter.prepareChronologicalRows(txs, initialBalance, onlyActive = true)
        assertEquals(3, compactRows.size)
        assertEquals("1", compactRows[0].transaction.id)
        assertEquals(160_000L, compactRows[0].runningBalance) // 50k + 110k
        assertEquals("2", compactRows[1].transaction.id)
        assertEquals(263_000L, compactRows[1].runningBalance) // 160k + 103k
        assertEquals("3", compactRows[2].transaction.id)
        assertEquals(expectedFinalBalance, compactRows[2].runningBalance)

        val completeRows = ExcelExporter.prepareChronologicalRows(txs, initialBalance, onlyActive = false)
        assertEquals(4, completeRows.size)
        assertEquals(expectedFinalBalance, completeRows.last().runningBalance)
    }

    @Test
    fun xlsxGenerator_producesValidCompactOoxmlWithRunningBalance() {
        val settings = AppSettings(
            groupName = "Kas Transparan",
            adminName = "Admin Kas",
            initialBalance = 0L,
            currencyCode = "IDR"
        )
        val txs = sampleTransactions()
        val activeTxs = txs.filter { it.status == "ACTIVE" }
        val totalIncome = activeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val finalBalance = settings.initialBalance + totalIncome - totalExpense

        val baos = ByteArrayOutputStream()
        ExcelExporter.writeXlsxContent(
            outputStream = baos,
            settings = settings,
            transactions = txs,
            periodTitle = "Bulan November 2026",
            initialBalance = settings.initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            columnMode = ExcelColumnMode.COMPACT
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(baos.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        val sheetXml = entries["xl/worksheets/sheet1.xml"]!!

        // Verify exact 7 compact headers + auto-fitted <cols>
        assertTrue(sheetXml.contains("<cols>"))
        assertTrue(sheetXml.contains("Tanggal &amp; Waktu"))
        assertTrue(sheetXml.contains("Jenis Transaksi"))
        assertTrue(sheetXml.contains("Jumlah Saldo"))
        assertTrue(sheetXml.contains("Catatan"))
        assertFalse(sheetXml.contains("Metode Pembayaran"))

        // Verify chronological running balances appear in order and match Saldo Akhir (188000)
        assertTrue(sheetXml.contains("<v>110000</v>"))
        assertTrue(sheetXml.contains("<v>213000</v>"))
        assertTrue(sheetXml.contains("<v>188000</v>"))
    }

    @Test
    fun docGenerator_producesStructuredHtmlTableInCompactAndCompleteModes() {
        val settings = AppSettings(
            groupName = "Kas RT 05",
            adminName = "Budi Santoso",
            initialBalance = 100_000L,
            currencyCode = "IDR"
        )
        val txs = sampleTransactions()
        val activeTxs = txs.filter { it.status == "ACTIVE" }
        val totalIncome = activeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val finalBalance = settings.initialBalance + totalIncome - totalExpense

        val compactHtml = DocReportGenerator.buildWordHtmlDocument(
            settings = settings,
            transactions = txs,
            periodTitle = "Bulan November 2026",
            initialBalance = settings.initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            columnMode = ExcelColumnMode.COMPACT
        )
        assertTrue(compactHtml.contains("LAPORAN KAS TRANSPARAN"))
        assertTrue(compactHtml.contains("RINGKASAN SALDO KAS"))
        assertTrue(compactHtml.contains("#0F6B5B"))
        assertTrue(compactHtml.contains("Tanggal &amp; Waktu"))
        assertTrue(compactHtml.contains("Jumlah Saldo"))
        assertTrue(compactHtml.contains("288.000")) // Final running balance 100k + 213k - 25k
        assertFalse(compactHtml.contains("Metode"))

        val completeHtml = DocReportGenerator.buildWordHtmlDocument(
            settings = settings,
            transactions = txs,
            periodTitle = "Bulan November 2026",
            initialBalance = settings.initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            columnMode = ExcelColumnMode.COMPLETE
        )
        assertTrue(completeHtml.contains("Metode"))
        assertTrue(completeHtml.contains("Kategori"))
        assertTrue(completeHtml.contains("288.000"))
    }
}
