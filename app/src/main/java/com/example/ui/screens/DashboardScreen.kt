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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Transaction
import com.example.ui.KasUiState
import com.example.ui.PeriodFilter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftSkyBlueGradient
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper

@Composable
fun DashboardScreen(
    state: KasUiState,
    onPeriodSelected: (PeriodFilter) -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onAddInitialBalance: ((Long, String) -> Unit)? = null,
    onSetInitialBalance: ((Long, String) -> Unit)? = null
) {
    val lang = state.settings.language
    val curr = state.settings.currencyCode

    var showManageInitialBalanceDialog by remember { mutableStateOf(false) }

    val activeTransactions = remember(state.transactions, state.selectedPeriod) {
        state.transactions.filter { it.status == "ACTIVE" }
    }

    val totalIncome = remember(activeTransactions) {
        activeTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(activeTransactions) {
        activeTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
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
            // Welcome Header with custom logo
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_custom_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.settings.groupName.ifBlank { "Kas Transparan" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${LanguageHelper.getString("admin_label", lang)}: ${state.settings.adminName.ifBlank { "Admin Kas" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            // Period Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val periods = listOf(
                        PeriodFilter.THIS_MONTH to LanguageHelper.getString("period_this_month", lang),
                        PeriodFilter.TODAY to LanguageHelper.getString("period_today", lang),
                        PeriodFilter.THIS_WEEK to LanguageHelper.getString("period_this_week", lang),
                        PeriodFilter.THIS_YEAR to LanguageHelper.getString("period_this_year", lang),
                        PeriodFilter.ALL to LanguageHelper.getString("period_all", lang)
                    )

                    items(periods) { (filter, label) ->
                        FilterChip(
                            selected = state.selectedPeriod == filter,
                            onClick = { onPeriodSelected(filter) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isLight) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface,
                                labelColor = if (isLight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("period_chip_${filter.name}")
                        )
                    }
                }
            }

            // Hero Card: Saldo Kas Saat Ini
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_balance_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LanguageHelper.getString("current_balance", lang),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = curr,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = CurrencyHelper.format(finalBalance, curr),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 30.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = LanguageHelper.getString("balance_formula", lang),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Row of 3 Metric Cards: Saldo Awal, Pemasukan, Pengeluaran
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Saldo Awal (Clickable to manage / add initial balance)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("initial_balance_card")
                            .clickable { showManageInitialBalanceDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = LanguageHelper.getString("initial_balance", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = LanguageHelper.getString("badge_manage_balance", lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyHelper.format(initialBalance, curr),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Total Pemasukan
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_income_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = LanguageHelper.getString("total_income", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyHelper.format(totalIncome, curr),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = IncomeGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Total Pengeluaran
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_expense_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = LanguageHelper.getString("total_expense", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyHelper.format(totalExpense, curr),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = ExpenseRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Cash Flow Visual Bar Comparison
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashflow_chart_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LanguageHelper.getString("cashflow_chart", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        val maxFlow = maxOf(totalIncome, totalExpense, 1L).toFloat()
                        val incomeRatio = (totalIncome.toFloat() / maxFlow).coerceIn(0f, 1f)
                        val expenseRatio = (totalExpense.toFloat() / maxFlow).coerceIn(0f, 1f)

                        // Income bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = LanguageHelper.getString("total_income", lang),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = IncomeGreen
                            )
                            Text(
                                text = CurrencyHelper.format(totalIncome, curr),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { incomeRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = IncomeGreen,
                            trackColor = IncomeGreen.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Expense bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = LanguageHelper.getString("total_expense", lang),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = ExpenseRed
                            )
                            Text(
                                text = CurrencyHelper.format(totalExpense, curr),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { expenseRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = ExpenseRed,
                            trackColor = ExpenseRed.copy(alpha = 0.15f)
                        )
                    }
                }
            }

            // Quick Action Buttons: Kas Masuk, Kas Keluar, + Saldo Awal
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToIncome,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("dashboard_btn_income"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageHelper.getString("quick_add_income", lang),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onNavigateToExpense,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("dashboard_btn_expense"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageHelper.getString("quick_add_expense", lang),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Button to Add / Manage Initial Balance directly
                    OutlinedButton(
                        onClick = { showManageInitialBalanceDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dashboard_btn_manage_initial_balance"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("quick_manage_initial_balance", lang),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LanguageHelper.getString("recent_transactions", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = LanguageHelper.getString("view_all", lang),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                            .padding(4.dp)
                    )
                }
            }

            // Recent Transactions List (first 5)
            val recentList = state.transactions.take(5)
            if (recentList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = LanguageHelper.getString("no_transactions", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentList) { tx ->
                    HistoryItemCard(
                        tx = tx,
                        currencyCode = curr,
                        onClick = onNavigateToHistory
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showManageInitialBalanceDialog) {
        ManageInitialBalanceDialog(
            currentBalance = initialBalance,
            currencyCode = curr,
            lang = lang,
            onDismiss = { showManageInitialBalanceDialog = false },
            onAdd = { amount, note ->
                onAddInitialBalance?.invoke(amount, note)
                showManageInitialBalanceDialog = false
            },
            onSet = { amount, note ->
                onSetInitialBalance?.invoke(amount, note)
                showManageInitialBalanceDialog = false
            }
        )
    }
}

@Composable
fun ManageInitialBalanceDialog(
    currentBalance: Long,
    currencyCode: String,
    lang: String,
    onDismiss: () -> Unit,
    onAdd: (Long, String) -> Unit,
    onSet: (Long, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tambah Saldo, 1: Atur Ulang Saldo
    var inputAmountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val enteredAmount = CurrencyHelper.parseCleanAmount(inputAmountText)
    val projectedBalance = if (selectedTab == 0) currentBalance + enteredAmount else enteredAmount

    val presets = listOf(50_000L, 100_000L, 500_000L, 1_000_000L, 5_000_000L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageHelper.getString("dialog_manage_initial_balance_title", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            inputAmountText = ""
                        },
                        text = {
                            Text(
                                LanguageHelper.getString("tab_add_initial_balance", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            inputAmountText = CurrencyHelper.formatInput(currentBalance.toString())
                        },
                        text = {
                            Text(
                                LanguageHelper.getString("tab_set_initial_balance", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                }

                // Current balance display
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageHelper.getString("current_initial_balance", lang),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = CurrencyHelper.format(currentBalance, currencyCode),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Amount input field
                OutlinedTextField(
                    value = inputAmountText,
                    onValueChange = { inputAmountText = CurrencyHelper.formatInput(it) },
                    label = {
                        Text(
                            if (selectedTab == 0) LanguageHelper.getString("amount_to_add", lang)
                            else LanguageHelper.getString("amount_new_balance", lang)
                        )
                    },
                    prefix = { Text("${CurrencyHelper.getSymbol(currencyCode)} ") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manage_initial_balance")
                )

                // Quick preset chips
                Column {
                    Text(
                        text = LanguageHelper.getString("quick_presets", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { preset ->
                            SuggestionChip(
                                onClick = {
                                    val current = CurrencyHelper.parseCleanAmount(inputAmountText)
                                    val updated = if (selectedTab == 0) current + preset else preset
                                    inputAmountText = CurrencyHelper.formatInput(updated.toString())
                                },
                                label = {
                                    Text(
                                        "+${CurrencyHelper.format(preset, currencyCode)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                // Live Preview calculation
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = LanguageHelper.getString("new_initial_balance_preview", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyHelper.format(projectedBalance, currencyCode),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Note / Keterangan
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(LanguageHelper.getString("initial_balance_note", lang)) },
                    placeholder = { Text(LanguageHelper.getString("initial_balance_note_placeholder", lang)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_manage_initial_balance_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        if (enteredAmount > 0) {
                            onAdd(enteredAmount, noteText.trim())
                        }
                    } else {
                        onSet(enteredAmount, noteText.trim())
                    }
                },
                enabled = if (selectedTab == 0) enteredAmount > 0 else true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_confirm_manage_initial_balance")
            ) {
                Text(
                    text = if (selectedTab == 0) LanguageHelper.getString("btn_confirm_add_balance", lang)
                    else LanguageHelper.getString("btn_confirm_set_balance", lang),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LanguageHelper.getString("btn_cancel", lang))
            }
        }
    )
}
