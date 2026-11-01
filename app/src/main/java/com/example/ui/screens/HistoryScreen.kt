package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AuditLog
import com.example.data.model.Transaction
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftSkyBlueGradient
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper
import com.example.util.PdfReportGenerator
import com.example.util.ReportImageGenerator
import com.example.util.WhatsAppShareHelper
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    state: KasUiState,
    viewModel: KasViewModel
) {
    val lang = state.settings.language
    val curr = state.settings.currencyCode
    val isEn = lang.equals("en", ignoreCase = true)

    var selectedTxForDetail by remember { mutableStateOf<Transaction?>(null) }
    var selectedTxForEdit by remember { mutableStateOf<Transaction?>(null) }
    var selectedTxForVoid by remember { mutableStateOf<Transaction?>(null) }
    var selectedTxForShare by remember { mutableStateOf<Transaction?>(null) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }

    val filteredList = remember(
        state.transactions,
        state.searchQuery,
        state.filterType,
        state.filterCategory
    ) {
        state.transactions.filter { tx ->
            val matchQuery = if (state.searchQuery.isNotBlank()) {
                tx.title.contains(state.searchQuery, ignoreCase = true) ||
                tx.description.contains(state.searchQuery, ignoreCase = true) ||
                tx.category.contains(state.searchQuery, ignoreCase = true)
            } else true

            val matchType = when (state.filterType) {
                "INCOME" -> tx.type == "INCOME" && tx.status == "ACTIVE"
                "EXPENSE" -> tx.type == "EXPENSE" && tx.status == "ACTIVE"
                "VOID" -> tx.status == "VOID"
                else -> true
            }

            val matchCategory = if (state.filterCategory != "ALL") {
                tx.category.equals(state.filterCategory, ignoreCase = true)
            } else true

            matchQuery && matchType && matchCategory
        }
    }

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.AppBackgroundLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isLight) Modifier.background(SoftSkyBlueGradient)
                else Modifier.background(MaterialTheme.colorScheme.background)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar & Audit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = LanguageHelper.getString("search_hint", lang),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showAuditLogsDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("btn_audit_logs"),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = LanguageHelper.getString("btn_audit", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }

            // Filter Type Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val typeFilters = listOf(
                    "ALL" to LanguageHelper.getString("filter_all", lang),
                    "INCOME" to LanguageHelper.getString("filter_income", lang),
                    "EXPENSE" to LanguageHelper.getString("filter_expense", lang),
                    "VOID" to LanguageHelper.getString("filter_void", lang)
                )

                items(typeFilters) { (type, label) ->
                    FilterChip(
                        selected = state.filterType == type,
                        onClick = { viewModel.setFilterType(type) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (type) {
                                "INCOME" -> IncomeGreen
                                "EXPENSE" -> ExpenseRed
                                else -> MaterialTheme.colorScheme.primary
                            },
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_type_$type")
                    )
                }
            }

            // Category Filter Chips
            val availableCategories = remember(state.transactions) {
                state.transactions.map { it.category }.distinct().filter { it.isNotBlank() }
            }

            if (availableCategories.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = state.filterCategory == "ALL",
                            onClick = { viewModel.setFilterCategory("ALL") },
                            label = { Text(LanguageHelper.getString("all_categories", lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(availableCategories) { cat ->
                        FilterChip(
                            selected = state.filterCategory == cat,
                            onClick = { viewModel.setFilterCategory(cat) },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Transactions List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = LanguageHelper.getString("no_transactions", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { tx ->
                        HistoryItemCard(
                            tx = tx,
                            currencyCode = curr,
                            onClick = { selectedTxForDetail = tx },
                            onShare = { selectedTxForShare = tx }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            tx = tx,
            currencyCode = curr,
            lang = lang,
            onDismiss = { selectedTxForDetail = null },
            onEdit = {
                selectedTxForDetail = null
                selectedTxForEdit = tx
            },
            onVoid = {
                selectedTxForDetail = null
                selectedTxForVoid = tx
            },
            onShare = {
                selectedTxForShare = tx
            }
        )
    }

    // Share Options Dialog (PDF / Image)
    selectedTxForShare?.let { tx ->
        ShareTransactionOptionsDialog(
            tx = tx,
            settings = state.settings,
            onDismiss = { selectedTxForShare = null }
        )
    }

    // Edit Dialog
    selectedTxForEdit?.let { tx ->
        EditTransactionDialog(
            tx = tx,
            currencyCode = curr,
            lang = lang,
            onDismiss = { selectedTxForEdit = null },
            onConfirm = { updatedTx, reason ->
                viewModel.updateTransaction(updatedTx, reason) {
                    selectedTxForEdit = null
                }
            }
        )
    }

    // Void Dialog
    selectedTxForVoid?.let { tx ->
        VoidTransactionDialog(
            tx = tx,
            lang = lang,
            onDismiss = { selectedTxForVoid = null },
            onConfirm = { reason ->
                viewModel.voidTransaction(tx, reason) {
                    selectedTxForVoid = null
                }
            }
        )
    }

    // Audit Logs Dialog
    if (showAuditLogsDialog) {
        AuditLogsDialog(
            auditLogs = state.auditLogs,
            lang = lang,
            onDismiss = { showAuditLogsDialog = false }
        )
    }
}

@Composable
fun HistoryItemCard(
    tx: Transaction,
    currencyCode: String,
    onClick: () -> Unit,
    onShare: () -> Unit = {}
) {
    val isIncome = tx.type == "INCOME"
    val isVoid = tx.status == "VOID"
    val inputTime = tx.createdAt?.toDate()?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${tx.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isVoid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isVoid) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVoid) Color.Gray.copy(alpha = 0.2f)
                        else if (isIncome) IncomeGreen.copy(alpha = 0.15f)
                        else ExpenseRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVoid) Icons.Default.Block
                    else if (isIncome) Icons.Default.ArrowDownward
                    else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isVoid) Color.Gray else if (isIncome) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isVoid) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isVoid) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeStr = if (inputTime.isNotBlank()) " ($inputTime)" else ""
                Text(
                    text = "${tx.date}$timeStr • ${tx.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + CurrencyHelper.format(tx.amount, currencyCode),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isVoid) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isVoid) Color.Gray else if (isIncome) IncomeGreen else ExpenseRed,
                    maxLines = 1
                )
                if (isVoid) {
                    Text(
                        text = "VOID",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_share_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan Bukti",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    tx: Transaction,
    currencyCode: String,
    lang: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onVoid: () -> Unit,
    onShare: () -> Unit
) {
    val isIncome = tx.type == "INCOME"
    val isVoid = tx.status == "VOID"
    val isEn = lang.equals("en", ignoreCase = true)
    val inputFullTime = tx.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMMM yyyy HH:mm", if (isEn) Locale.ENGLISH else Locale("id", "ID")).format(it)
    } ?: "-"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_close", lang))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEn) "Share" else "Bagikan", maxLines = 1)
                }
                if (!isVoid) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageHelper.getString("btn_edit", lang), maxLines = 1)
                    }
                    Button(
                        onClick = onVoid,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageHelper.getString("btn_void", lang), maxLines = 1)
                    }
                }
            }
        },
        title = {
            Text(
                text = LanguageHelper.getString("transaction_detail", lang),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncome) (if (isEn) "INCOME" else "KAS MASUK") else (if (isEn) "EXPENSE" else "KAS KELUAR"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isIncome) IncomeGreen else ExpenseRed
                    )
                    Text(
                        text = if (isVoid) LanguageHelper.getString("status_void", lang) else LanguageHelper.getString("status_active", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isVoid) Color.Gray else IncomeGreen
                    )
                }

                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = CurrencyHelper.format(tx.amount, currencyCode),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )

                Spacer(modifier = Modifier.height(4.dp))
                DetailRow(label = LanguageHelper.getString("field_date", lang), value = tx.date)
                DetailRow(label = LanguageHelper.getString("input_time_label", lang), value = inputFullTime)
                DetailRow(label = LanguageHelper.getString("field_category", lang), value = tx.category)
                DetailRow(label = LanguageHelper.getString("field_payment_method", lang), value = tx.paymentMethod)
                if (tx.description.isNotBlank()) {
                    DetailRow(label = LanguageHelper.getString("field_description", lang), value = tx.description)
                }
                if (isVoid && tx.voidReason.isNotBlank()) {
                    DetailRow(label = LanguageHelper.getString("void_reason_prompt", lang), value = tx.voidReason, isHighlight = true)
                }

                if (tx.receiptUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = LanguageHelper.getString("field_upload_receipt", lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Image(
                        painter = rememberAsyncImagePainter(tx.receiptUrl),
                        contentDescription = "Bukti Nota",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    )
}

@Composable
fun EditTransactionDialog(
    tx: Transaction,
    currencyCode: String,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (Transaction, String) -> Unit
) {
    var title by remember { mutableStateOf(tx.title) }
    var amountText by remember { mutableStateOf(tx.amount.toString()) }
    var category by remember { mutableStateOf(tx.category) }
    var description by remember { mutableStateOf(tx.description) }
    var paymentMethod by remember { mutableStateOf(tx.paymentMethod) }
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LanguageHelper.getString("edit_dialog_title", lang)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(LanguageHelper.getString("field_income_source", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("${LanguageHelper.getString("field_amount", lang)} ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(LanguageHelper.getString("field_category", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(LanguageHelper.getString("field_description", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(LanguageHelper.getString("edit_reason_label", lang)) },
                    placeholder = { Text("e.g. Typo on amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (title.isBlank() || amount <= 0L || reason.isBlank()) {
                        errorMsg = LanguageHelper.getString("error_edit_validation", lang)
                        return@Button
                    }
                    val updated = tx.copy(
                        title = title.trim(),
                        amount = amount,
                        category = category.trim(),
                        description = description.trim(),
                        paymentMethod = paymentMethod
                    )
                    onConfirm(updated, reason)
                }
            ) {
                Text(LanguageHelper.getString("btn_save_changes", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_cancel", lang))
            }
        }
    )
}

@Composable
fun VoidTransactionDialog(
    tx: Transaction,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val isEn = lang.equals("en", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LanguageHelper.getString("void_dialog_title", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val promptText = if (isEn) {
                    "Transaction '${tx.title}' (${CurrencyHelper.format(tx.amount)}) will be voided. It will not be permanently deleted so it remains in the audit trail."
                } else {
                    "Transaksi '${tx.title}' sebesar ${CurrencyHelper.format(tx.amount)} akan dibatalkan. Data tidak dihapus permanen agar tetap tercatat di audit log."
                }
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(LanguageHelper.getString("void_reason_prompt", lang)) },
                    placeholder = { Text(LanguageHelper.getString("void_reason_hint", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorMsg = LanguageHelper.getString("error_empty_reason", lang)
                        return@Button
                    }
                    onConfirm(reason.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text(LanguageHelper.getString("btn_confirm_void", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_cancel", lang))
            }
        }
    )
}

@Composable
fun AuditLogsDialog(
    auditLogs: List<AuditLog>,
    lang: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(LanguageHelper.getString("audit_trail_title", lang), fontSize = 17.sp)
            }
        },
        text = {
            if (auditLogs.isEmpty()) {
                Text(LanguageHelper.getString("audit_empty", lang))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (log.action) {
                                            "CREATE" -> IncomeGreen
                                            "VOID" -> ExpenseRed
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Text(
                                        text = log.createdAt?.toDate()?.toString()?.take(16) ?: "-",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (log.reason.isNotBlank()) {
                                    Text(
                                        text = "${LanguageHelper.getString("reason_label", lang)}: ${log.reason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
private fun DetailRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ShareTransactionOptionsDialog(
    tx: Transaction,
    settings: com.example.data.model.AppSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = settings.language
    val isIncome = tx.type == "INCOME"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageHelper.getString("share_tx_title", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (isIncome) "+ " else "- "}${CurrencyHelper.format(tx.amount, settings.currencyCode)} • ${tx.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: PDF Document
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val pdfFile = PdfReportGenerator.generateTransactionPdf(context, settings, tx)
                                val shareText = WhatsAppShareHelper.generateTransactionShareText(settings, tx)
                                WhatsAppShareHelper.shareFile(
                                    context = context,
                                    file = pdfFile,
                                    mimeType = "application/pdf",
                                    subject = "Bukti Transaksi - ${tx.title}",
                                    captionText = shareText
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_tx_pdf"),
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_pdf_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_pdf_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Image PNG Receipt
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val imageFile = ReportImageGenerator.saveTransactionReceiptImage(context, settings, tx)
                                val shareText = WhatsAppShareHelper.generateTransactionShareText(settings, tx)
                                WhatsAppShareHelper.shareFile(
                                    context = context,
                                    file = imageFile,
                                    mimeType = "image/png",
                                    subject = "Struk Transaksi - ${tx.title}",
                                    captionText = shareText
                                )
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuat Gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("btn_share_tx_image"),
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
                                .background(IncomeGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("share_image_title", lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("share_image_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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


