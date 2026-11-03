package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: KasUiState,
    viewModel: KasViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val lang = state.settings.language

    var groupName by remember(state.settings.groupName) { mutableStateOf(state.settings.groupName) }
    var adminName by remember(state.settings.adminName) { mutableStateOf(state.settings.adminName) }
    var initialBalanceText by remember(state.settings.initialBalance) { mutableStateOf(CurrencyHelper.formatInput(state.settings.initialBalance.toString())) }
    var selectedThemeMode by remember(state.settings.themeMode) { mutableStateOf(state.settings.themeMode) }
    var selectedLanguage by remember(state.settings.language) { mutableStateOf(state.settings.language) }
    var selectedCurrency by remember(state.settings.currencyCode) { mutableStateOf(state.settings.currencyCode) }

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var lastBackupTimestamp by remember { mutableStateOf(viewModel.getLastBackupTimestamp()) }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }
    var restoredCount by remember { mutableIntStateOf(0) }
    var showRestoreErrorDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf("") }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!jsonString.isNullOrBlank()) {
                    viewModel.restoreBackup(jsonString) { result ->
                        if (result.isSuccess) {
                            restoredCount = result.getOrNull() ?: 0
                            showRestoreSuccessDialog = true
                            val currentSettings = viewModel.settings.value
                            groupName = currentSettings.groupName
                            adminName = currentSettings.adminName
                            initialBalanceText = CurrencyHelper.formatInput(currentSettings.initialBalance.toString())
                            selectedThemeMode = currentSettings.themeMode
                            selectedLanguage = currentSettings.language
                            selectedCurrency = currentSettings.currencyCode
                        } else {
                            restoreErrorMessage = result.exceptionOrNull()?.localizedMessage ?: "File cadangan tidak valid atau rusak"
                            showRestoreErrorDialog = true
                        }
                    }
                } else {
                    restoreErrorMessage = "File cadangan kosong"
                    showRestoreErrorDialog = true
                }
            } catch (e: Exception) {
                restoreErrorMessage = e.localizedMessage ?: "Gagal membaca file dari penyimpanan"
                showRestoreErrorDialog = true
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.AppBackgroundLight

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageHelper.getString("settings_title", selectedLanguage),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectedLanguage == "en") "Back" else "Kembali"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val initial = CurrencyHelper.parseCleanAmount(initialBalanceText)
                            viewModel.saveSettings(
                                state.settings.copy(
                                    groupName = groupName.trim(),
                                    adminName = adminName.trim(),
                                    initialBalance = initial,
                                    themeMode = selectedThemeMode,
                                    language = selectedLanguage,
                                    currencyCode = selectedCurrency
                                )
                            ) {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("save_settings_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LanguageHelper.getString("btn_save_settings", selectedLanguage),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLight) Color(0xFF0288D1) else MaterialTheme.colorScheme.surface,
                    titleContentColor = if (isLight) Color.White else MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = if (isLight) Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(
                    if (isLight) Modifier.background(com.example.ui.theme.SoftSkyBlueGradient)
                    else Modifier.background(MaterialTheme.colorScheme.background)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            // A. PROFIL APLIKASI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("profile_section", selectedLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text(LanguageHelper.getString("group_name", selectedLanguage)) },
                        placeholder = { Text("Contoh: Kas RT 05 / RW 02") },
                        modifier = Modifier.fillMaxWidth().testTag("input_group_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text(LanguageHelper.getString("admin_name", selectedLanguage)) },
                        placeholder = { Text("Contoh: Budi Santoso") },
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_name"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { initialBalanceText = CurrencyHelper.formatInput(it) },
                        label = { Text(LanguageHelper.getString("initial_balance_label", selectedLanguage)) },
                        placeholder = { Text("0") },
                        prefix = { Text("${CurrencyHelper.getSymbol(selectedCurrency)} ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_initial_balance"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick addition helper chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = LanguageHelper.getString("quick_presets", selectedLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presetList = listOf(100_000L, 500_000L, 1_000_000L, 5_000_000L)
                            presetList.forEach { addAmt ->
                                SuggestionChip(
                                    onClick = {
                                        val cur = CurrencyHelper.parseCleanAmount(initialBalanceText)
                                        initialBalanceText = CurrencyHelper.formatInput((cur + addAmt).toString())
                                    },
                                    label = {
                                        Text(
                                            "+${CurrencyHelper.format(addAmt, selectedCurrency)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // B. TEMA APLIKASI
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_theme_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("theme_section", selectedLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    ThemeOptionRow(
                        title = LanguageHelper.getString("theme_light", selectedLanguage),
                        icon = Icons.Default.LightMode,
                        isSelected = selectedThemeMode == "LIGHT",
                        onSelect = {
                            selectedThemeMode = "LIGHT"
                            viewModel.saveSettings(state.settings.copy(themeMode = "LIGHT")) {}
                        }
                    )
                    ThemeOptionRow(
                        title = LanguageHelper.getString("theme_dark", selectedLanguage),
                        icon = Icons.Default.DarkMode,
                        isSelected = selectedThemeMode == "DARK",
                        onSelect = {
                            selectedThemeMode = "DARK"
                            viewModel.saveSettings(state.settings.copy(themeMode = "DARK")) {}
                        }
                    )
                    ThemeOptionRow(
                        title = LanguageHelper.getString("theme_system", selectedLanguage),
                        icon = Icons.Default.Smartphone,
                        isSelected = selectedThemeMode == "SYSTEM",
                        onSelect = {
                            selectedThemeMode = "SYSTEM"
                            viewModel.saveSettings(state.settings.copy(themeMode = "SYSTEM")) {}
                        }
                    )
                }
            }

            // C. BAHASA (Language)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_language_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("language_section", selectedLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    val currentOpt = LanguageHelper.getLanguageOption(selectedLanguage)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = currentOpt.flag, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentOpt.nativeName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${currentOpt.name} • ${currentOpt.region}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_open_language_selector"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${LanguageHelper.getString("select_language_btn", selectedLanguage)} (24 Bahasa Dunia)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // D. MATA UANG (Currencies ISO 4217)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_currency_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("currency_section", selectedLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = LanguageHelper.getString("currency_desc", selectedLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = currencyDropdownExpanded,
                        onExpandedChange = { currencyDropdownExpanded = it }
                    ) {
                        val currentOpt = CurrencyHelper.SUPPORTED_CURRENCIES.find { it.code == selectedCurrency }
                        OutlinedTextField(
                            value = "${currentOpt?.name ?: selectedCurrency} (${currentOpt?.symbol ?: selectedCurrency}) - ${currentOpt?.region ?: ""}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = currencyDropdownExpanded,
                            onDismissRequest = { currencyDropdownExpanded = false }
                        ) {
                            CurrencyHelper.SUPPORTED_CURRENCIES.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${option.code} - ${option.name} (${option.symbol}) [${option.region}]")
                                    },
                                    onClick = {
                                        selectedCurrency = option.code
                                        currencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // E. BACKUP & PULIHKAN DATA (GOOGLE DRIVE)
            // Ditempatkan tepat di bawah sebelum Tentang Ponsel / Tentang Aplikasi
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_google_drive_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFF1967D2),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = LanguageHelper.getString("backup_drive_title", selectedLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = LanguageHelper.getString("backup_drive_subtitle", selectedLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status backup terakhir
                    val lastBackupTime = remember(lastBackupTimestamp, selectedLanguage) {
                        if (lastBackupTimestamp > 0) {
                            val locale = if (selectedLanguage.startsWith("en", ignoreCase = true)) Locale.ENGLISH else Locale("id", "ID")
                            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", locale)
                            "${LanguageHelper.getString("backup_status_prefix", selectedLanguage)}: ${sdf.format(Date(lastBackupTimestamp))}"
                        } else {
                            LanguageHelper.getString("backup_status_none", selectedLanguage)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastBackupTimestamp > 0) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (lastBackupTimestamp > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastBackupTime,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Tombol Cadangkan ke Google Drive
                    Button(
                        onClick = {
                            val file = viewModel.createBackupFile()
                            if (file != null && file.exists()) {
                                try {
                                    val fileUri: Uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, fileUri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Backup Kas Transparan - ${file.name}")
                                        putExtra(Intent.EXTRA_TEXT, "File cadangan data Kas Transparan. Simpan file ini di Google Drive Anda agar aman.")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(shareIntent, "Simpan Cadangan ke Google Drive")
                                    context.startActivity(chooser)
                                    lastBackupTimestamp = System.currentTimeMillis()
                                    viewModel.setLastBackupTimestamp(lastBackupTimestamp)
                                    Toast.makeText(context, "Silakan pilih 'Simpan ke Drive'", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuat cadangan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Gagal membuat file cadangan", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_backup_google_drive"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("btn_backup_drive", selectedLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    // Tombol Pulihkan dari Google Drive
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_restore_google_drive"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("btn_restore_drive", selectedLanguage),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Opsi Buka Aplikasi Google Drive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val driveIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.docs")
                                    if (driveIntent != null) {
                                        context.startActivity(driveIntent)
                                    } else {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com"))
                                        context.startActivity(webIntent)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, LanguageHelper.getString("drive_app_not_found", selectedLanguage), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = LanguageHelper.getString("btn_open_drive_app", selectedLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // F. TENTANG PONSEL & APLIKASI (About Phone & App)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true }
                    .testTag("settings_about_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LanguageHelper.getString("about_device_app", selectedLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "KAS TRANSPARAN v1.0.0 by Rafline Dev",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    FilledTonalButton(
                        onClick = { showAboutDialog = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 36.dp)
                    ) {
                        Text(
                            text = LanguageHelper.getString("btn_open", selectedLanguage),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ABOUT APPLICATION DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Kas Transparan", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = LanguageHelper.getString("app_desc", selectedLanguage),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = LanguageHelper.getString("app_version", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                    Text(text = LanguageHelper.getString("developer", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                    Text(text = LanguageHelper.getString("platform", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                    Text(text = LanguageHelper.getString("release_year", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                    Text(text = LanguageHelper.getString("copyright", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(LanguageHelper.getString("btn_close", selectedLanguage))
                }
            }
        )
    }

    // DIALOG HASIL PEMULIHAN DARI GOOGLE DRIVE
    if (showRestoreSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(LanguageHelper.getString("restore_success_title", selectedLanguage), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(String.format(LanguageHelper.getString("restore_success_msg", selectedLanguage), restoredCount))
            },
            confirmButton = {
                Button(onClick = { showRestoreSuccessDialog = false }) {
                    Text(LanguageHelper.getString("btn_done", selectedLanguage))
                }
            }
        )
    }

    if (showRestoreErrorDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreErrorDialog = false },
            title = {
                Text(LanguageHelper.getString("restore_fail_title", selectedLanguage), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Text(restoreErrorMessage)
            },
            confirmButton = {
                TextButton(onClick = { showRestoreErrorDialog = false }) {
                    Text(LanguageHelper.getString("btn_close", selectedLanguage))
                }
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelectLanguage = { langCode ->
                selectedLanguage = langCode
                viewModel.saveSettings(state.settings.copy(language = langCode)) {}
                showLanguageDialog = false
            }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegionTab by remember { mutableStateOf("Semua") }

    val filteredList = remember(searchQuery, selectedRegionTab) {
        LanguageHelper.SUPPORTED_LANGUAGES.filter { opt ->
            val matchesRegion = if (selectedRegionTab == "Semua") true else opt.region.equals(selectedRegionTab, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                opt.name.contains(searchQuery, ignoreCase = true) ||
                opt.nativeName.contains(searchQuery, ignoreCase = true) ||
                opt.code.contains(searchQuery, ignoreCase = true) ||
                opt.region.contains(searchQuery, ignoreCase = true)
            }
            matchesRegion && matchesSearch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageHelper.getString("select_language_btn", currentLanguage),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(LanguageHelper.getString("search_language_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Region filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LanguageHelper.REGIONS) { region ->
                        FilterChip(
                            selected = selectedRegionTab == region,
                            onClick = { selectedRegionTab = region },
                            label = { Text(if (region == "Semua") LanguageHelper.getString("region_all", currentLanguage) else region, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // List of languages
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList) { opt ->
                        val isSelected = opt.code.equals(currentLanguage, ignoreCase = true)
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLanguage(opt.code) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = opt.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = opt.nativeName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${opt.name} • ${opt.region}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
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
                Text(LanguageHelper.getString("btn_close", currentLanguage))
            }
        }
    )
}

@Composable
fun ThemeOptionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
