package com.example.ui.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KasUiState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftSkyBlueGradient
import com.example.util.CurrencyHelper
import com.example.util.ExcelExportResult
import com.example.util.ExcelExporter
import com.example.util.LanguageHelper
import com.example.util.PdfReportGenerator
import com.example.util.ReportImageGenerator
import com.example.util.WhatsAppShareHelper
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportScreen(
    state: KasUiState
) {
    val context = LocalContext.current
    val lang = state.settings.language
    val curr = state.settings.currencyCode
    val isEn = lang.equals("en", ignoreCase = true)

    var selectedPeriodType by remember { mutableStateOf("MONTHLY") }
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lastExcelResult by remember { mutableStateOf<ExcelExportResult?>(null) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateSdf = remember(isEn) {
        SimpleDateFormat("dd MMMM yyyy", if (isEn) Locale.ENGLISH else Locale("id", "ID"))
    }
    val now = Calendar.getInstance()
    val todayStr = sdf.format(now.time)

    val (periodTitle, filteredTransactions) = remember(
        state.transactions,
        selectedPeriodType,
        customStartDate,
        customEndDate,
        isEn
    ) {
        val cal = Calendar.getInstance()
        when (selectedPeriodType) {
            "DAILY" -> {
                val label = if (isEn) "Daily" else "Harian"
                val title = "$label (${displayDateSdf.format(cal.time)})"
                val txs = state.transactions.filter { it.date == todayStr }
                title to txs
            }
            "WEEKLY" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val startWeek = sdf.format(cal.time)
                val label = if (isEn) "Weekly" else "Mingguan"
                val title = "$label ($startWeek - $todayStr)"
                val txs = state.transactions.filter { it.date >= startWeek && it.date <= todayStr }
                title to txs
            }
            "MONTHLY" -> {
                val monthName = SimpleDateFormat("MMMM yyyy", if (isEn) Locale.ENGLISH else Locale("id", "ID")).format(cal.time)
                val label = if (isEn) "Month" else "Bulan"
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startMonth = sdf.format(cal.time)
                val title = "$label $monthName"
                val txs = state.transactions.filter { it.date >= startMonth }
                title to txs
            }
            "YEARLY" -> {
                val year = cal.get(Calendar.YEAR).toString()
                val label = if (isEn) "Year" else "Tahun"
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val startYear = sdf.format(cal.time)
                val title = "$label $year"
                val txs = state.transactions.filter { it.date >= startYear }
                title to txs
            }
            "CUSTOM" -> {
                val title = if (customStartDate.isNotBlank() && customEndDate.isNotBlank())
                    "$customStartDate - $customEndDate"
                else (if (isEn) "Custom Period" else "Periode Kustom")
                val txs = state.transactions.filter { tx ->
                    val after = if (customStartDate.isNotBlank()) tx.date >= customStartDate else true
                    val before = if (customEndDate.isNotBlank()) tx.date <= customEndDate else true
                    after && before
                }
                title to txs
            }
            else -> (if (isEn) "Financial Report" else "Laporan Keuangan") to state.transactions
        }
    }

    val activeTxs = remember(filteredTransactions) {
        filteredTransactions.filter { it.status == "ACTIVE" }
    }
    val totalIncome = remember(activeTxs) {
        activeTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(activeTxs) {
        activeTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val initialBalance = state.settings.initialBalance
    val finalBalance = initialBalance + totalIncome - totalExpense
    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.AppBackgroundLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isLight) Modifier.background(SoftSkyBlueGradient)
                else Modifier.background(MaterialTheme.colorScheme.background)
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LanguageHelper.getString("report_title", lang),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = LanguageHelper.getString("report_subtitle", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Period Selection Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val periods = listOf(
                        "MONTHLY" to LanguageHelper.getString("report_monthly", lang),
                        "WEEKLY" to LanguageHelper.getString("report_weekly", lang),
                        "DAILY" to LanguageHelper.getString("report_daily", lang),
                        "YEARLY" to LanguageHelper.getString("report_yearly", lang),
                        "CUSTOM" to LanguageHelper.getString("report_custom", lang)
                    )

                    items(periods) { (type, label) ->
                        FilterChip(
                            selected = selectedPeriodType == type,
                            onClick = { selectedPeriodType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("report_period_$type")
                        )
                    }
                }
            }

            // Action Buttons: WhatsApp (PDF + Text), Share as Image (Infographic), and Export options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. WhatsApp Button (PDF + formatted Text)
                    Button(
                        onClick = {
                            val pdf = PdfReportGenerator.generateReportPdf(
                                context = context,
                                settings = state.settings,
                                transactions = filteredTransactions,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance
                            )
                            val msg = WhatsAppShareHelper.generateWhatsAppMessage(
                                settings = state.settings,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance
                            )
                            WhatsAppShareHelper.shareReport(context, pdf, msg)
                            statusMessage = if (isEn) "PDF report generated and text copied to clipboard!" else "Laporan PDF disiapkan dan pesan disalin ke clipboard!"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_share_whatsapp"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("btn_share_wa", lang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Share as Image (Infographic Card)
                    Button(
                        onClick = {
                            try {
                                val imageFile = ReportImageGenerator.saveReportImage(
                                    context = context,
                                    settings = state.settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val caption = "📊 *LAPORAN KAS: ${state.settings.groupName}*\n" +
                                        "Periode: $periodTitle\n" +
                                        "Saldo Awal: ${CurrencyHelper.format(initialBalance, curr)}\n" +
                                        "Pemasukan: +${CurrencyHelper.format(totalIncome, curr)}\n" +
                                        "Pengeluaran: -${CurrencyHelper.format(totalExpense, curr)}\n" +
                                        "Saldo Akhir: *${CurrencyHelper.format(finalBalance, curr)}*"

                                ReportImageGenerator.shareReportImage(context, imageFile, caption)
                                statusMessage = if (isEn) "Infographic image generated and ready to share!" else "Gambar infografis laporan siap dibagikan!"
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_share_image"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("btn_share_image_full", lang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3 Export Buttons: [Unduh PDF] [Unduh Excel] [Cetak]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download PDF
                        OutlinedButton(
                            onClick = {
                                val file = PdfReportGenerator.generateReportPdf(
                                    context = context,
                                    settings = state.settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                Toast.makeText(context, "PDF: ${file.name}", Toast.LENGTH_SHORT).show()
                                statusMessage = "PDF: ${file.name}"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_download_pdf"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = LanguageHelper.getString("download_pdf", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Export Excel (Saved directly to phone's public Downloads folder)
                        OutlinedButton(
                            onClick = {
                                val result = ExcelExporter.exportAndSaveToDownloads(
                                    context = context,
                                    settings = state.settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                lastExcelResult = result
                                val toastText = if (isEn) "Excel saved to ${result.savedPathDisplay}" else "Excel disimpan di ${result.savedPathDisplay}"
                                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                                statusMessage = "${LanguageHelper.getString("excel_saved_download", lang)} ${result.savedPathDisplay}"
                            },
                            modifier = Modifier
                                .weight(1.05f)
                                .height(46.dp)
                                .testTag("btn_export_excel"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = LanguageHelper.getString("export_excel", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Cetak Laporan (Print)
                        OutlinedButton(
                            onClick = {
                                val pdf = PdfReportGenerator.generateReportPdf(
                                    context = context,
                                    settings = state.settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                val printAdapter = object : android.print.PrintDocumentAdapter() {
                                    override fun onLayout(
                                        oldAttributes: PrintAttributes?,
                                        newAttributes: PrintAttributes?,
                                        cancellationSignal: android.os.CancellationSignal?,
                                        callback: LayoutResultCallback?,
                                        extras: android.os.Bundle?
                                    ) {
                                        callback?.onLayoutFinished(
                                            android.print.PrintDocumentInfo.Builder("laporan_kas.pdf")
                                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                .build(),
                                            true
                                        )
                                    }

                                    override fun onWrite(
                                        pages: Array<out android.print.PageRange>?,
                                        destination: android.os.ParcelFileDescriptor?,
                                        cancellationSignal: android.os.CancellationSignal?,
                                        callback: WriteResultCallback?
                                    ) {
                                        try {
                                             FileInputStream(pdf).use { input ->
                                                android.os.ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                        } catch (e: Exception) {
                                            callback?.onWriteFailed(e.localizedMessage)
                                        }
                                    }
                                }
                                printManager?.print("Laporan Kas Transparan", printAdapter, null)
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(46.dp)
                                .testTag("btn_print_report"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = LanguageHelper.getString("print_report", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Notice Banner when Excel is saved directly to Download folder
            if (lastExcelResult != null && lastExcelResult?.success == true) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("excel_download_success_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = LanguageHelper.getString("excel_saved_download", lang),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF1B5E20)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastExcelResult?.savedPathDisplay ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = LanguageHelper.getString("excel_open_hint", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF388E3C)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        lastExcelResult?.let { res ->
                                            ExcelExporter.openOrShareCsv(context, res)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = LanguageHelper.getString("btn_open", lang) + " / " + (if (isEn) "Share" else "Bagikan"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (statusMessage != null && (lastExcelResult == null || lastExcelResult?.success == false)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Report Preview Section (Formal Paper Card)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_preview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "KAS TRANSPARAN",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = state.settings.groupName.ifBlank { "Kelompok Kas" },
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${LanguageHelper.getString("period_label", lang)}: $periodTitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${LanguageHelper.getString("manager_label", lang)}: ${state.settings.adminName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReportSummaryRow(LanguageHelper.getString("initial_balance", lang), CurrencyHelper.format(initialBalance, curr))
                            ReportSummaryRow(LanguageHelper.getString("total_income", lang), CurrencyHelper.format(totalIncome, curr), color = IncomeGreen)
                            ReportSummaryRow(LanguageHelper.getString("total_expense", lang), CurrencyHelper.format(totalExpense, curr), color = ExpenseRed)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportSummaryRow(
                                LanguageHelper.getString("final_balance", lang),
                                CurrencyHelper.format(finalBalance, curr),
                                isBold = true,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "${LanguageHelper.getString("list_tx_period", lang)} (${filteredTransactions.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredTransactions.isEmpty()) {
                            Text(
                                text = LanguageHelper.getString("no_tx_period", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            filteredTransactions.forEach { tx ->
                                val inputTime = tx.createdAt?.toDate()?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        val timeSuffix = if (inputTime.isNotBlank()) " ($inputTime)" else ""
                                        Text(
                                            text = "${tx.date}$timeSuffix • ${tx.category}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tx.description.isNotBlank()) {
                                            Text(
                                                text = tx.description,
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = (if (tx.type == "INCOME") "+ " else "- ") + CurrencyHelper.format(tx.amount, curr),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (tx.status == "VOID") Color.Gray
                                        else if (tx.type == "INCOME") IncomeGreen else ExpenseRed
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color.LightGray.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReportSummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
    }
}

