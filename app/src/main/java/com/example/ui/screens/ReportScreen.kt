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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.BorderStroke
import com.example.data.model.ExportPreferences
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.components.PublicTransparencyCard
import com.example.ui.theme.AppScreenBackgroundBrush
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftCardBorderColor
import com.example.ui.theme.SoftDangerBg
import com.example.ui.theme.SoftDangerFg
import com.example.ui.theme.SoftSuccessBg
import com.example.ui.theme.SoftSuccessFg
import com.example.ui.theme.isAppLightTheme
import com.example.util.CurrencyHelper
import com.example.util.DocExportResult
import com.example.util.DocReportGenerator
import com.example.util.ExcelExportResult
import com.example.util.ExcelExporter
import com.example.util.LanguageHelper
import com.example.util.PdfExportResult
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
    state: KasUiState,
    viewModel: KasViewModel,
    onOpenPublicPreview: (String) -> Unit = {}
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
    var lastPdfResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var lastDocResult by remember { mutableStateOf<DocExportResult?>(null) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showExportPrintDialog by remember { mutableStateOf(false) }
    val exportPrefs = remember(state.exportPreferences) { state.exportPreferences.ensureAtLeastOneActive() }

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
    val isLight = isAppLightTheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScreenBackgroundBrush)
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

            // Action Buttons: Consolidated Share Feature & Export options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Unified Primary Share Button (opens clean sharing options modal)
                    Button(
                        onClick = { showShareOptionsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_share_report_main"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLight) Color(0xFF1F9D55) else Color(0xFF217A49),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = LanguageHelper.getString("btn_share_report", lang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Single Consolidated Download & Print Button respecting active ExportPreferences
                    val activeFormatNames = remember(exportPrefs, isEn) {
                        buildList {
                            if (exportPrefs.showSharePdf) add("PDF")
                            if (exportPrefs.showShareExcel) add("Excel")
                            if (exportPrefs.showShareDoc) add("DOC")
                            if (exportPrefs.showShareImage) add(if (isEn) "Image" else "Gambar")
                        }.joinToString(" / ")
                    }
                    OutlinedButton(
                        onClick = { showExportPrintDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_export_print_options"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEn) {
                                "Download $activeFormatNames${if (exportPrefs.showSharePdf) " & Print" else ""} ▾"
                            } else {
                                "Unduh $activeFormatNames${if (exportPrefs.showSharePdf) " & Cetak" else ""} ▾"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Public 1-Link Real-Time Transparency Card
            item {
                PublicTransparencyCard(
                    state = state,
                    viewModel = viewModel,
                    onOpenPublicPreview = onOpenPublicPreview
                )
            }

            // Notice Banner when PDF is saved directly to Download folder
            if (lastPdfResult != null && lastPdfResult?.success == true) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_download_success_card"),
                        colors = CardDefaults.cardColors(containerColor = SoftDangerBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = SoftDangerFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) "PDF Report Saved to Downloads" else "Laporan PDF Berhasil Disimpan ke Download",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SoftDangerFg
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastPdfResult?.savedPathDisplay ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SoftDangerFg
                            )
                            Text(
                                text = if (isEn) "File is ready in device Download folder. Tap below to open or share."
                                else "File sudah ada di folder Download HP. Ketuk tombol di bawah untuk membuka atau membagikan.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        lastPdfResult?.let { res ->
                                            PdfReportGenerator.openOrSharePdf(context, res)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = (if (isEn) "Open / Share PDF" else "Buka / Bagikan PDF"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
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
                        colors = CardDefaults.cardColors(containerColor = SoftSuccessBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = SoftSuccessFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = LanguageHelper.getString("excel_saved_download", lang),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SoftSuccessFg
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastExcelResult?.savedPathDisplay ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SoftSuccessFg
                            )
                            Text(
                                text = LanguageHelper.getString("excel_open_hint", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Notice Banner when Word (.doc) is saved directly to Download folder
            if (lastDocResult != null && lastDocResult?.success == true) {
                item {
                    val docBg = if (isLight) Color(0xFFEFF6FF) else Color(0xFF172554)
                    val docFg = if (isLight) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_download_success_card"),
                        colors = CardDefaults.cardColors(containerColor = docBg),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, docFg.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = docFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) "Word Document (.doc) Saved to Downloads" else "Dokumen Word (.doc) Berhasil Disimpan ke Download",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = docFg
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastDocResult?.savedPathDisplay ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = docFg
                            )
                            Text(
                                text = if (isEn) "Formatted Word document ready to open in Microsoft Word, WPS, or Google Docs."
                                else "Dokumen Word (.doc) terstruktur rapi siap dibuka di Microsoft Word, WPS, atau Google Docs.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        lastDocResult?.let { res ->
                                            DocReportGenerator.openOrShareDoc(context, res)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isEn) "Open / Share DOC" else "Buka / Bagikan DOC",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = BorderStroke(1.dp, SoftCardBorderColor)
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

    if (showShareOptionsDialog) {
        ShareReportOptionsDialog(
            periodTitle = periodTitle,
            initialBalance = initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            filteredTransactions = filteredTransactions,
            settings = state.settings,
            exportPrefs = exportPrefs,
            onDismiss = { showShareOptionsDialog = false },
            onStatusMessage = { statusMessage = it },
            onExcelExport = { lastExcelResult = it },
            onDocExport = { lastDocResult = it }
        )
    }

    if (showExportPrintDialog) {
        ExportPrintOptionsDialog(
            periodTitle = periodTitle,
            initialBalance = initialBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            finalBalance = finalBalance,
            filteredTransactions = filteredTransactions,
            settings = state.settings,
            exportPrefs = exportPrefs,
            onDismiss = { showExportPrintDialog = false },
            onStatusMessage = { statusMessage = it },
            onPdfExport = { lastPdfResult = it },
            onExcelExport = { lastExcelResult = it },
            onDocExport = { lastDocResult = it }
        )
    }
}

@Composable
fun ShareReportOptionsDialog(
    periodTitle: String,
    initialBalance: Long,
    totalIncome: Long,
    totalExpense: Long,
    finalBalance: Long,
    filteredTransactions: List<com.example.data.model.Transaction>,
    settings: com.example.data.model.AppSettings,
    exportPrefs: ExportPreferences = ExportPreferences(),
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit,
    onExcelExport: (ExcelExportResult) -> Unit,
    onDocExport: (DocExportResult) -> Unit = {}
) {
    val context = LocalContext.current
    val lang = settings.language
    val curr = settings.currencyCode
    val isEn = lang.equals("en", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF1E7E34),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = LanguageHelper.getString("share_report_dialog_title", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${LanguageHelper.getString("period_label", lang)}: $periodTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = LanguageHelper.getString("share_report_dialog_subtitle", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. WhatsApp Option (PDF + Teks Rangkuman)
                if (exportPrefs.showSharePdf) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val pdf = PdfReportGenerator.generateReportPdf(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val msg = WhatsAppShareHelper.generateWhatsAppMessage(
                                    settings = settings,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                WhatsAppShareHelper.shareReport(context, pdf, msg)
                                onStatusMessage(
                                    if (isEn) "PDF report generated and text copied to clipboard!"
                                    else "Laporan PDF disiapkan dan pesan disalin ke clipboard!"
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_whatsapp"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF1E7E34),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_wa_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_wa_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 2. Infographic Image Option (.png)
                if (exportPrefs.showShareImage) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val imageFile = ReportImageGenerator.saveReportImage(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val caption = "📊 *LAPORAN KAS: ${settings.groupName}*\n" +
                                        "Periode: $periodTitle\n" +
                                        "Saldo Awal: ${CurrencyHelper.format(initialBalance, curr)}\n" +
                                        "Pemasukan: +${CurrencyHelper.format(totalIncome, curr)}\n" +
                                        "Pengeluaran: -${CurrencyHelper.format(totalExpense, curr)}\n" +
                                        "Saldo Akhir: *${CurrencyHelper.format(finalBalance, curr)}*"

                                ReportImageGenerator.shareReportImage(context, imageFile, caption)
                                onStatusMessage(
                                    if (isEn) "Infographic image generated and ready to share!"
                                    else "Gambar infografis laporan siap dibagikan!"
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_image"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B5E20).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_img_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_img_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 3. Official PDF File Option (.pdf)
                if (exportPrefs.showSharePdf) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val pdf = PdfReportGenerator.generateReportPdf(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val msg = WhatsAppShareHelper.generateWhatsAppMessage(
                                    settings = settings,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                WhatsAppShareHelper.shareFile(
                                    context = context,
                                    file = pdf,
                                    mimeType = "application/pdf",
                                    subject = "Laporan Kas - $periodTitle",
                                    captionText = msg
                                )
                                onStatusMessage(
                                    if (isEn) "PDF report file ready to share!"
                                    else "File PDF laporan siap dibagikan!"
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_pdf_direct"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_pdf_file_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_pdf_file_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 4. Excel (.xlsx) Spreadsheet Option
                if (exportPrefs.showShareExcel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val result = ExcelExporter.exportAndSaveToDownloads(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance,
                                    format = "XLSX",
                                    columnMode = exportPrefs.excelColumnMode
                                )
                                onExcelExport(result)
                                ExcelExporter.openOrShareCsv(context, result)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_excel_direct"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_excel_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_excel_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 5. Word Document (.doc) Option
                if (exportPrefs.showShareDoc) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val result = DocReportGenerator.exportAndSaveToDownloads(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance,
                                    columnMode = exportPrefs.excelColumnMode
                                )
                                onDocExport(result)
                                DocReportGenerator.openOrShareDoc(context, result)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_doc_direct"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Word Document (.doc)" else "Dokumen Word (.doc)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isEn) "Clean Excel-style table & running balance for Word / WPS"
                                else "Dokumen Word bertabel rapi seperti Excel dengan saldo berjalan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_close", lang))
            }
        }
    )
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

@Composable
fun ExportPrintOptionsDialog(
    periodTitle: String,
    initialBalance: Long,
    totalIncome: Long,
    totalExpense: Long,
    finalBalance: Long,
    filteredTransactions: List<com.example.data.model.Transaction>,
    settings: com.example.data.model.AppSettings,
    exportPrefs: ExportPreferences = ExportPreferences(),
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit,
    onPdfExport: (PdfExportResult) -> Unit,
    onExcelExport: (ExcelExportResult) -> Unit,
    onDocExport: (DocExportResult) -> Unit = {}
) {
    val context = LocalContext.current
    val lang = settings.language
    val curr = settings.currencyCode
    val isEn = lang.equals("en", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isEn) "Download & Print Report" else "Unduh & Cetak Laporan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${LanguageHelper.getString("period_label", lang)}: $periodTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isEn) "Select the export format or action you want:"
                    else "Pilih format dokumen atau tindakan yang diinginkan:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Unduh PDF Option (Saved directly to phone's public Download folder)
                if (exportPrefs.showSharePdf) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val result = PdfReportGenerator.generateAndSaveToDownloads(
                                context = context,
                                settings = settings,
                                transactions = filteredTransactions,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance
                            )
                            onPdfExport(result)
                            val toastText = if (isEn) "PDF saved to ${result.savedPathDisplay}" else "PDF berhasil disimpan di ${result.savedPathDisplay}"
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            onStatusMessage("${if (isEn) "PDF saved:" else "PDF disimpan:"} ${result.savedPathDisplay}")
                            onDismiss()
                        }
                        .testTag("dialog_btn_unduh_pdf"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("download_pdf_card_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("download_pdf_card_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 2. Unduh Excel (.xlsx) & CSV Options
                if (exportPrefs.showShareExcel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val result = ExcelExporter.exportAndSaveToDownloads(
                                context = context,
                                settings = settings,
                                transactions = filteredTransactions,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance,
                                format = "XLSX",
                                columnMode = exportPrefs.excelColumnMode
                            )
                            onExcelExport(result)
                            val toastText = if (isEn) "Excel (.xlsx) saved to ${result.savedPathDisplay}" else "Excel (.xlsx) disimpan di ${result.savedPathDisplay}"
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            onStatusMessage("${LanguageHelper.getString("excel_saved_download", lang)} ${result.savedPathDisplay}")
                            onDismiss()
                        }
                        .testTag("dialog_btn_unduh_excel_xls"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("download_xls_card_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("download_xls_card_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val result = ExcelExporter.exportAndSaveToDownloads(
                                context = context,
                                settings = settings,
                                transactions = filteredTransactions,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance,
                                format = "CSV",
                                columnMode = exportPrefs.excelColumnMode
                            )
                            onExcelExport(result)
                            val toastText = if (isEn) "CSV (.csv) saved to ${result.savedPathDisplay}" else "CSV (.csv) disimpan di ${result.savedPathDisplay}"
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            onStatusMessage("CSV: ${result.savedPathDisplay}")
                            onDismiss()
                        }
                        .testTag("dialog_btn_unduh_csv"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00796B).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF004D40),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("download_csv_card_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("download_csv_card_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 3. Unduh Dokumen Word (.doc) Option
                if (exportPrefs.showShareDoc) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val result = DocReportGenerator.exportAndSaveToDownloads(
                                context = context,
                                settings = settings,
                                transactions = filteredTransactions,
                                periodTitle = periodTitle,
                                initialBalance = initialBalance,
                                totalIncome = totalIncome,
                                totalExpense = totalExpense,
                                finalBalance = finalBalance,
                                columnMode = exportPrefs.excelColumnMode
                            )
                            onDocExport(result)
                            val toastText = if (isEn) "Word (.doc) saved to ${result.savedPathDisplay}" else "Dokumen (.doc) disimpan di ${result.savedPathDisplay}"
                            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            onStatusMessage("DOC: ${result.savedPathDisplay}")
                            onDismiss()
                        }
                        .testTag("dialog_btn_unduh_doc"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Download Word Document (.doc)" else "Unduh Dokumen Word (.doc)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isEn) "Professional bordered table layout matching Excel"
                                else "Dokumen Word bertabel rapi serapi Excel dengan saldo otomatis",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 4. Gambar Infografis (.png) Option
                if (exportPrefs.showShareImage) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val imageFile = ReportImageGenerator.saveReportImage(
                                    context = context,
                                    settings = settings,
                                    transactions = filteredTransactions,
                                    periodTitle = periodTitle,
                                    initialBalance = initialBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    finalBalance = finalBalance
                                )
                                val caption = "📊 *LAPORAN KAS: ${settings.groupName}*\n" +
                                    "Periode: $periodTitle\n" +
                                    "Saldo Awal: ${CurrencyHelper.format(initialBalance, curr)}\n" +
                                    "Pemasukan: +${CurrencyHelper.format(totalIncome, curr)}\n" +
                                    "Pengeluaran: -${CurrencyHelper.format(totalExpense, curr)}\n" +
                                    "Saldo Akhir: *${CurrencyHelper.format(finalBalance, curr)}*"
                                ReportImageGenerator.shareReportImage(context, imageFile, caption)
                                onStatusMessage(
                                    if (isEn) "Infographic image generated and ready to share!"
                                    else "Gambar infografis laporan siap dibagikan!"
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("dialog_btn_unduh_image"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B5E20).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF1B5E20),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEn) "Save / Share Infographic Image (.png)" else "Simpan / Bagikan Gambar Laporan (.png)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_img_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // 5. Cetak Laporan Option
                if (exportPrefs.showSharePdf) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val pdf = PdfReportGenerator.generateReportPdf(
                                context = context,
                                settings = settings,
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
                            onDismiss()
                        }
                        .testTag("dialog_btn_cetak_laporan"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0288D1).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Color(0xFF0277BD),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("print_report_card_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("print_report_card_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_close", lang))
            }
        }
    )
}

