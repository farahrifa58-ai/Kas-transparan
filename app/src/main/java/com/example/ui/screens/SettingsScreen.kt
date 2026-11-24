package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.ExcelColumnMode
import com.example.data.model.ExportPreferences
import com.example.data.model.PublicKasHelper
import com.example.data.repository.LicenseRepository
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.components.PublicTransparencyCard
import com.example.ui.components.SettingsModalCard
import com.example.ui.components.SettingsPopupDialog
import com.example.ui.theme.AppScreenBackgroundBrush
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftCardBorderColor
import com.example.ui.theme.SoftSuccessBg
import com.example.ui.theme.SoftSuccessFg
import com.example.ui.theme.SoftTopBarColor
import com.example.ui.theme.SoftTopBarContentColor
import com.example.ui.theme.accentAbout
import com.example.ui.theme.accentAppearance
import com.example.ui.theme.accentBackup
import com.example.ui.theme.accentCurrency
import com.example.ui.theme.accentDomain
import com.example.ui.theme.accentExportCustomization
import com.example.ui.theme.accentProfile
import com.example.ui.theme.accentPublicReport
import com.example.ui.theme.accentTutorial
import com.example.ui.theme.isAppLightTheme
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsModalSection {
    PROFILE,
    APPEARANCE,
    PUBLIC_REPORT,
    DOMAIN,
    CURRENCY,
    EXPORT_CUSTOMIZATION,
    BACKUP,
    TUTORIAL,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: KasUiState,
    viewModel: KasViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenPublicPreview: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isLight = isAppLightTheme

    var groupName by remember(state.settings.groupName) { mutableStateOf(state.settings.groupName) }
    var description by remember(state.settings.description) { mutableStateOf(state.settings.description) }
    var adminName by remember(state.settings.adminName) { mutableStateOf(state.settings.adminName) }
    var initialBalanceText by remember(state.settings.initialBalance) {
        mutableStateOf(CurrencyHelper.formatInput(state.settings.initialBalance.toString()))
    }
    var selectedThemeMode by remember(state.settings.themeMode) { mutableStateOf(state.settings.themeMode) }
    var selectedLanguage by remember(state.settings.language) { mutableStateOf(state.settings.language) }
    var selectedCurrency by remember(state.settings.currencyCode) { mutableStateOf(state.settings.currencyCode) }

    val isEn = selectedLanguage.startsWith("en", ignoreCase = true)

    // Unified modal state: tapping any Settings card opens a focused Popup Dialog
    var activeModal by remember { mutableStateOf<SettingsModalSection?>(null) }
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                            description = currentSettings.description
                            adminName = currentSettings.adminName
                            initialBalanceText = CurrencyHelper.formatInput(currentSettings.initialBalance.toString())
                            selectedThemeMode = currentSettings.themeMode
                            selectedLanguage = currentSettings.language
                            selectedCurrency = currentSettings.currencyCode
                        } else {
                            restoreErrorMessage = result.exceptionOrNull()?.localizedMessage
                                ?: "File cadangan tidak valid atau rusak"
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

    val profileAccent = accentProfile()
    val appearanceAccent = accentAppearance()
    val publicAccent = accentPublicReport()
    val domainAccent = accentDomain()
    val currencyAccent = accentCurrency()
    val exportCustomizationAccent = accentExportCustomization()
    val backupAccent = accentBackup()
    val tutorialAccent = accentTutorial()
    val aboutAccent = accentAbout()

    val exportPrefs = state.exportPreferences

    val openLabel = LanguageHelper.getString("btn_open", selectedLanguage)
    val saveAndCloseLabel = if (isEn) "Save & Close" else "Simpan & Tutup"
    val closeLabel = LanguageHelper.getString("btn_close", selectedLanguage)

    val currentLanguageOption = remember(selectedLanguage) {
        LanguageHelper.getLanguageOption(selectedLanguage)
    }
    val currentCurrencyOption = remember(selectedCurrency) {
        CurrencyHelper.SUPPORTED_CURRENCIES.find { it.code == selectedCurrency }
    }
    val currentThemeLabel = when (selectedThemeMode) {
        "DARK" -> LanguageHelper.getString("theme_dark", selectedLanguage)
        "SYSTEM" -> LanguageHelper.getString("theme_system", selectedLanguage)
        else -> LanguageHelper.getString("theme_light", selectedLanguage)
    }

    val lastBackupDisplay = remember(lastBackupTimestamp, selectedLanguage) {
        if (lastBackupTimestamp > 0L) {
            val locale = if (isEn) Locale.ENGLISH else Locale("id", "ID")
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", locale)
            "${LanguageHelper.getString("backup_status_prefix", selectedLanguage)}: ${sdf.format(Date(lastBackupTimestamp))}"
        } else {
            LanguageHelper.getString("backup_status_none", selectedLanguage)
        }
    }

    val publicStatusSummary = remember(state.settings.publicLastSyncedAt, state.settings.publicKasId, selectedLanguage) {
        if (state.settings.publicLastSyncedAt > 0L) {
            "${state.settings.publicKasId} • ${PublicKasHelper.formatSyncTimestamp(state.settings.publicLastSyncedAt, selectedLanguage)}"
        } else {
            "${state.settings.publicKasId.ifBlank { "KAS-..." }} • ${if (isEn) "Not published yet" else "Belum dipublikasikan"}"
        }
    }

    fun persistAllSettings(onComplete: () -> Unit = {}) {
        val initial = CurrencyHelper.parseCleanAmount(initialBalanceText)
        viewModel.saveSettings(
            state.settings.copy(
                groupName = groupName.trim(),
                description = description.trim(),
                adminName = adminName.trim(),
                initialBalance = initial,
                publicBaseUrl = AppSettings.PUBLIC_WEB_BASE_URL,
                themeMode = selectedThemeMode,
                language = selectedLanguage,
                currencyCode = selectedCurrency
            ),
            onSuccess = onComplete
        )
    }

    val parsedInitialPreview = CurrencyHelper.parseCleanAmount(initialBalanceText)
    val isPublicSynced = state.settings.publicLastSyncedAt > 0L
    val fixedProductionBaseUrl = AppSettings.PUBLIC_WEB_BASE_URL
    val fixedPublicLinkPreview = remember(state.settings.publicKasId) {
        PublicKasHelper.buildPublicUrl(state.settings.publicKasId.ifBlank { "KAS-..." })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageHelper.getString("settings_title", selectedLanguage),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "Kembali"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { persistAllSettings { onBack() } },
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLight) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (isLight) SoftTopBarColor else MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = LanguageHelper.getString("btn_save_settings", selectedLanguage),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftTopBarColor,
                    titleContentColor = SoftTopBarContentColor,
                    navigationIconContentColor = SoftTopBarContentColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppScreenBackgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                // 1. PROFIL KELOMPOK & ADMIN -> Modal Popup
                SettingsModalCard(
                    title = LanguageHelper.getString("profile_section", selectedLanguage),
                    subtitle = "${groupName.ifBlank { "Kas Transparan" }} • ${CurrencyHelper.format(parsedInitialPreview, selectedCurrency)}",
                    icon = Icons.Default.Person,
                    accent = profileAccent,
                    actionLabel = openLabel,
                    onClick = { activeModal = SettingsModalSection.PROFILE },
                    modifier = Modifier.testTag("settings_profile_card")
                )

                // 2. TEMA & BAHASA -> Modal Popup
                SettingsModalCard(
                    title = if (isEn) "Theme & Language" else "Tema & Bahasa",
                    subtitle = "$currentThemeLabel • ${currentLanguageOption.flag} ${currentLanguageOption.nativeName}",
                    icon = Icons.Default.Palette,
                    accent = appearanceAccent,
                    actionLabel = openLabel,
                    onClick = { activeModal = SettingsModalSection.APPEARANCE },
                    modifier = Modifier.testTag("settings_theme_language_card")
                )

                // 3. LAPORAN PUBLIK ONLINE -> Modal Popup
                SettingsModalCard(
                    title = if (isEn) "Online Public Report" else "Laporan Publik Online",
                    subtitle = publicStatusSummary,
                    icon = Icons.Default.Public,
                    accent = publicAccent,
                    actionLabel = openLabel,
                    badgeText = if (isPublicSynced) (if (isEn) "Live" else "Aktif") else (if (isEn) "Draft" else "Offline"),
                    badgeColor = if (isPublicSynced) SoftSuccessFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    badgeContainerColor = if (isPublicSynced) SoftSuccessBg else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { activeModal = SettingsModalSection.PUBLIC_REPORT },
                    modifier = Modifier.testTag("settings_public_report_card")
                )

                // 4. DOMAIN WEB LAPORAN PUBLIK -> Modal Popup
                SettingsModalCard(
                    title = if (isEn) "Public Web Domain" else "Domain Web Laporan",
                    subtitle = fixedProductionBaseUrl.removePrefix("https://"),
                    icon = Icons.Default.Link,
                    accent = domainAccent,
                    actionLabel = openLabel,
                    badgeText = if (isEn) "Fixed" else "Tetap",
                    badgeColor = domainAccent.tint,
                    badgeContainerColor = domainAccent.container,
                    onClick = { activeModal = SettingsModalSection.DOMAIN },
                    modifier = Modifier.testTag("settings_public_url_card")
                )

                // 5. MATA UANG -> Modal Popup
                SettingsModalCard(
                    title = LanguageHelper.getString("currency_section", selectedLanguage),
                    subtitle = "${currentCurrencyOption?.code ?: selectedCurrency} (${currentCurrencyOption?.symbol ?: selectedCurrency}) • ${currentCurrencyOption?.name ?: ""}",
                    icon = Icons.Default.MonetizationOn,
                    accent = currencyAccent,
                    actionLabel = openLabel,
                    onClick = { activeModal = SettingsModalSection.CURRENCY },
                    modifier = Modifier.testTag("settings_currency_card")
                )

                // 6. KUSTOMISASI EKSPOR & BAGIKAN -> Modal Popup
                val excelModeShortLabel = if (exportPrefs.excelColumnMode == ExcelColumnMode.COMPACT) {
                    if (isEn) "Compact (7 Cols)" else "Ringkas (7 Kolom)"
                } else {
                    if (isEn) "Complete" else "Lengkap"
                }
                SettingsModalCard(
                    title = if (isEn) "Customize Export & Share" else "Kustomisasi Ekspor & Bagikan",
                    subtitle = if (exportPrefs.showShareExcel) {
                        "${exportPrefs.activeCount}/4 ${if (isEn) "active" else "aktif"} • Excel $excelModeShortLabel"
                    } else {
                        "${exportPrefs.activeCount}/4 ${if (isEn) "active formats" else "opsi ekspor aktif"}"
                    },
                    icon = Icons.Default.Share,
                    accent = exportCustomizationAccent,
                    actionLabel = openLabel,
                    badgeText = "${exportPrefs.activeCount}/4",
                    badgeColor = exportCustomizationAccent.tint,
                    badgeContainerColor = exportCustomizationAccent.container,
                    onClick = { activeModal = SettingsModalSection.EXPORT_CUSTOMIZATION },
                    modifier = Modifier.testTag("settings_export_customization_card")
                )

                // 7. CADANGAN & PEMULIHAN (GOOGLE DRIVE) -> Modal Popup
                SettingsModalCard(
                    title = LanguageHelper.getString("backup_drive_title", selectedLanguage),
                    subtitle = lastBackupDisplay,
                    icon = Icons.Default.CloudUpload,
                    accent = backupAccent,
                    actionLabel = openLabel,
                    onClick = { activeModal = SettingsModalSection.BACKUP },
                    modifier = Modifier.testTag("settings_google_drive_card")
                )

                // 7. TUTORIAL SINGKAT -> Modal Popup
                SettingsModalCard(
                    title = if (isEn) "Quick App Guide" else "Tutorial Singkat",
                    subtitle = if (isEn) "5 easy steps to manage transparent cashbooks" else "Panduan cepat 5 langkah pakai Kas Transparan",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = tutorialAccent,
                    actionLabel = openLabel,
                    badgeText = if (isEn) "Guide" else "Panduan",
                    onClick = { activeModal = SettingsModalSection.TUTORIAL },
                    modifier = Modifier.testTag("settings_tutorial_card")
                )

                // 8. TENTANG PERANGKAT & APLIKASI -> Modal Popup
                SettingsModalCard(
                    title = LanguageHelper.getString("about_device_app", selectedLanguage),
                    subtitle = "KAS TRANSPARAN v1.0.0 by Rafline Dev",
                    icon = Icons.Default.Smartphone,
                    accent = aboutAccent,
                    actionLabel = openLabel,
                    onClick = { activeModal = SettingsModalSection.ABOUT },
                    modifier = Modifier.testTag("settings_about_card")
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ==================== MODAL POPUPS FOR EACH SETTINGS SECTION ====================
    when (activeModal) {
        SettingsModalSection.PROFILE -> {
            SettingsPopupDialog(
                title = LanguageHelper.getString("profile_section", selectedLanguage),
                subtitle = if (isEn) "Group identity, admin & opening cash balance" else "Identitas kelompok, bendahara & saldo awal kas",
                icon = Icons.Default.Person,
                accent = profileAccent,
                primaryButtonText = saveAndCloseLabel,
                showCheckIcon = true,
                onDismiss = { activeModal = null },
                onPrimaryAction = {
                    persistAllSettings {
                        Toast.makeText(
                            context,
                            if (isEn) "Profile settings saved" else "Profil & saldo awal disimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                        activeModal = null
                    }
                }
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(LanguageHelper.getString("group_name", selectedLanguage)) },
                    placeholder = { Text("Contoh: Kas RT 05 / RW 02") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(
                            if (isEn) "Description / Secretariat Address (Optional)"
                            else "Keterangan / Alamat Sekretariat (Opsional)"
                        )
                    },
                    placeholder = { Text("Contoh: Jl. Melati No. 12, Kel. Sukamaju") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_description"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text(LanguageHelper.getString("admin_name", selectedLanguage)) },
                    placeholder = { Text("Contoh: Budi Santoso") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_admin_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = CurrencyHelper.formatInput(it) },
                    label = { Text(LanguageHelper.getString("initial_balance_label", selectedLanguage)) },
                    placeholder = { Text("0") },
                    prefix = { Text("${CurrencyHelper.getSymbol(selectedCurrency)} ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_initial_balance"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Neat 2x2 Grid for Quick Addition Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageHelper.getString("quick_presets", selectedLanguage),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (CurrencyHelper.parseCleanAmount(initialBalanceText) > 0L) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { initialBalanceText = "0" }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isEn) "Reset" else "Reset 0",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    val presetRows = listOf(
                        listOf(100_000L, 500_000L),
                        listOf(1_000_000L, 5_000_000L)
                    )
                    presetRows.forEach { rowAmounts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowAmounts.forEach { addAmt ->
                                Surface(
                                    onClick = {
                                        val cur = CurrencyHelper.parseCleanAmount(initialBalanceText)
                                        initialBalanceText = CurrencyHelper.formatInput((cur + addAmt).toString())
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = profileAccent.container.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, profileAccent.tint.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "+ ${CurrencyHelper.format(addAmt, selectedCurrency)}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = profileAccent.tint,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsModalSection.APPEARANCE -> {
            SettingsPopupDialog(
                title = if (isEn) "Theme & Language" else "Tema & Bahasa",
                subtitle = if (isEn) "Customize visual appearance & app language" else "Atur tampilan terang/gelap & bahasa aplikasi",
                icon = Icons.Default.Palette,
                accent = appearanceAccent,
                primaryButtonText = saveAndCloseLabel,
                showCheckIcon = true,
                onDismiss = { activeModal = null },
                onPrimaryAction = {
                    persistAllSettings { activeModal = null }
                }
            ) {
                Text(
                    text = LanguageHelper.getString("theme_section", selectedLanguage),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themeItems = listOf(
                        Triple("LIGHT", LanguageHelper.getString("theme_light", selectedLanguage), Icons.Default.LightMode),
                        Triple("DARK", LanguageHelper.getString("theme_dark", selectedLanguage), Icons.Default.DarkMode),
                        Triple("SYSTEM", LanguageHelper.getString("theme_system", selectedLanguage), Icons.Default.Smartphone)
                    )
                    themeItems.forEach { (modeKey, label, icon) ->
                        val isSelected = selectedThemeMode == modeKey
                        Surface(
                            onClick = {
                                selectedThemeMode = modeKey
                                viewModel.saveSettings(state.settings.copy(themeMode = modeKey)) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                                .testTag("theme_option_$modeKey"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) appearanceAccent.container
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) appearanceAccent.tint else SoftCardBorderColor
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) appearanceAccent.tint else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) appearanceAccent.tint else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = LanguageHelper.getString("language_section", selectedLanguage),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = appearanceAccent.container.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, appearanceAccent.tint.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentLanguageOption.flag, fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentLanguageOption.nativeName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currentLanguageOption.name} • ${currentLanguageOption.region}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_open_language_selector"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = appearanceAccent.container,
                        contentColor = appearanceAccent.tint
                    )
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${LanguageHelper.getString("select_language_btn", selectedLanguage)} (24 Bahasa)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        SettingsModalSection.PUBLIC_REPORT -> {
            SettingsPopupDialog(
                title = if (isEn) "Online Public Report" else "Laporan Publik Online",
                subtitle = publicStatusSummary,
                icon = Icons.Default.Public,
                accent = publicAccent,
                primaryButtonText = closeLabel,
                onDismiss = { activeModal = null },
                onPrimaryAction = { activeModal = null }
            ) {
                PublicTransparencyCard(
                    state = state,
                    viewModel = viewModel,
                    onOpenPublicPreview = { kasId ->
                        activeModal = null
                        onOpenPublicPreview(kasId)
                    },
                    embedded = true
                )
            }
        }

        SettingsModalSection.DOMAIN -> {
            SettingsPopupDialog(
                title = if (isEn) "Public Web Domain" else "Domain Web Laporan Publik",
                subtitle = if (isEn) "Fixed production domain for all member links" else "Domain produksi tetap untuk tautan anggota",
                icon = Icons.Default.Link,
                accent = domainAccent,
                primaryButtonText = closeLabel,
                onDismiss = { activeModal = null },
                onPrimaryAction = { activeModal = null }
            ) {
                Text(
                    text = if (isEn)
                        "All member links permanently use the fixed production web domain with hash route /#/lihat/${state.settings.publicKasId}."
                    else
                        "Semua tautan laporan anggota secara permanen memakai domain web produksi tetap dengan rute /#/lihat/${state.settings.publicKasId}.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = fixedProductionBaseUrl,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isEn) "Production Domain (Fixed)" else "Domain Produksi Tetap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_public_base_url"),
                    shape = RoundedCornerShape(12.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = domainAccent.container.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, domainAccent.tint.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEn) "Active Member URL:" else "Tautan Aktif Anggota:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fixedPublicLinkPreview,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = domainAccent.tint
                        )
                    }
                }
            }
        }

        SettingsModalSection.CURRENCY -> {
            SettingsPopupDialog(
                title = LanguageHelper.getString("currency_section", selectedLanguage),
                subtitle = if (isEn) "Choose default currency format" else "Pilih format simbol & mata uang utama",
                icon = Icons.Default.MonetizationOn,
                accent = currencyAccent,
                primaryButtonText = saveAndCloseLabel,
                showCheckIcon = true,
                onDismiss = { activeModal = null },
                onPrimaryAction = {
                    persistAllSettings {
                        Toast.makeText(
                            context,
                            if (isEn) "Currency updated" else "Mata uang diperbarui",
                            Toast.LENGTH_SHORT
                        ).show()
                        activeModal = null
                    }
                }
            ) {
                Text(
                    text = LanguageHelper.getString("currency_desc", selectedLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${currentCurrencyOption?.name ?: selectedCurrency} (${currentCurrencyOption?.symbol ?: selectedCurrency}) - ${currentCurrencyOption?.region ?: ""}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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

        SettingsModalSection.EXPORT_CUSTOMIZATION -> {
            val minOneWarning = if (isEn) {
                "At least 1 export option must stay active."
            } else {
                "Minimal 1 opsi ekspor harus tetap aktif."
            }

            fun applyExportUpdate(candidate: ExportPreferences) {
                if (!viewModel.updateExportPreferences(candidate)) {
                    Toast.makeText(context, minOneWarning, Toast.LENGTH_SHORT).show()
                }
            }

            SettingsPopupDialog(
                title = if (isEn) "Customize Export & Share" else "Kustomisasi Ekspor & Bagikan",
                subtitle = if (isEn) "Show/hide report buttons & choose Excel columns" else "Atur tombol ekspor Laporan & mode kolom Excel",
                icon = Icons.Default.Share,
                accent = exportCustomizationAccent,
                primaryButtonText = saveAndCloseLabel,
                showCheckIcon = true,
                onDismiss = { activeModal = null },
                onPrimaryAction = {
                    Toast.makeText(
                        context,
                        if (isEn) "Export settings saved" else "Pengaturan ekspor & bagikan disimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                    activeModal = null
                }
            ) {
                Text(
                    text = if (isEn)
                        "Choose which export buttons appear on the Report page (at least 1 option remains active)."
                    else
                        "Pilih format ekspor yang tampil di halaman Laporan (minimal 1 opsi tetap aktif).",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportToggleRow(
                    title = if (isEn) "Show Share Image" else "Tampilkan Share Image",
                    description = if (isEn) "Infographic image (.png/.jpg) button in Report" else "Tombol ekspor gambar laporan (.png/.jpg)",
                    icon = Icons.Default.Image,
                    tint = Color(0xFF1B5E20),
                    checked = exportPrefs.showShareImage,
                    onCheckedChange = { checked ->
                        applyExportUpdate(exportPrefs.copy(showShareImage = checked))
                    },
                    testTag = "switch_share_image"
                )

                ExportToggleRow(
                    title = if (isEn) "Show Share PDF" else "Tampilkan Share PDF",
                    description = if (isEn) "PDF document (.pdf) & print button in Report" else "Tombol ekspor dokumen PDF & cetak laporan",
                    icon = Icons.Default.PictureAsPdf,
                    tint = Color(0xFFC62828),
                    checked = exportPrefs.showSharePdf,
                    onCheckedChange = { checked ->
                        applyExportUpdate(exportPrefs.copy(showSharePdf = checked))
                    },
                    testTag = "switch_share_pdf"
                )

                ExportToggleRow(
                    title = if (isEn) "Show Share Excel (.xlsx)" else "Tampilkan Share Excel (.xlsx)",
                    description = if (isEn) "Excel workbook (.xlsx/.csv) with running balance" else "Tombol ekspor spreadsheet Excel (.xlsx) dengan saldo berjalan",
                    icon = Icons.Default.TableChart,
                    tint = exportCustomizationAccent.tint,
                    checked = exportPrefs.showShareExcel,
                    onCheckedChange = { checked ->
                        applyExportUpdate(exportPrefs.copy(showShareExcel = checked))
                    },
                    testTag = "switch_share_excel"
                )

                AnimatedVisibility(
                    visible = exportPrefs.showShareExcel,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = exportCustomizationAccent.container.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, exportCustomizationAccent.tint.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isEn) "Excel Column Mode" else "Mode Kolom Excel",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = exportCustomizationAccent.tint
                            )

                            ExcelColumnModeRadioOption(
                                title = if (isEn) "Complete Excel (Default)" else "Excel Lengkap (Default)",
                                description = if (isEn)
                                    "Displays all detail columns (Date, Time, Type, Title, Category, Method, Income, Expense, Balance, Status, Notes)"
                                else
                                    "Menampilkan seluruh kolom detail (Tanggal, Jam, Jenis, Nama, Kategori, Metode, Pemasukan, Pengeluaran, Jumlah Saldo, Status, Catatan)",
                                selected = exportPrefs.excelColumnMode == ExcelColumnMode.COMPLETE,
                                accentTint = exportCustomizationAccent.tint,
                                onClick = {
                                    applyExportUpdate(exportPrefs.copy(excelColumnMode = ExcelColumnMode.COMPLETE))
                                },
                                testTag = "radio_excel_complete"
                            )

                            ExcelColumnModeRadioOption(
                                title = if (isEn) "Compact Excel (7 Columns + Running Balance)" else "Excel Ringkas (7 Kolom + Saldo Berjalan)",
                                description = if (isEn)
                                    "7 clean columns: No, Date & Time, Type, Income, Expense, Running Balance, Notes"
                                else
                                    "7 kolom padat: No, Tanggal & Waktu, Jenis Transaksi, Pemasukan, Pengeluaran, Jumlah Saldo, Catatan",
                                selected = exportPrefs.excelColumnMode == ExcelColumnMode.COMPACT,
                                accentTint = exportCustomizationAccent.tint,
                                onClick = {
                                    applyExportUpdate(exportPrefs.copy(excelColumnMode = ExcelColumnMode.COMPACT))
                                },
                                testTag = "radio_excel_compact"
                            )
                        }
                    }
                }

                ExportToggleRow(
                    title = if (isEn) "Show Share Document (.doc)" else "Tampilkan Share Dokumen (.doc)",
                    description = if (isEn) "Formatted Word document (.doc) with Excel-style table" else "Tombol ekspor Dokumen Word (.doc) bertabel rapi seperti Excel",
                    icon = Icons.Default.Description,
                    tint = Color(0xFF1D4ED8),
                    checked = exportPrefs.showShareDoc,
                    onCheckedChange = { checked ->
                        applyExportUpdate(exportPrefs.copy(showShareDoc = checked))
                    },
                    testTag = "switch_share_doc"
                )
            }
        }

        SettingsModalSection.BACKUP -> {
            SettingsPopupDialog(
                title = LanguageHelper.getString("backup_drive_title", selectedLanguage),
                subtitle = if (isEn) "Safe local backup & restore via Google Drive" else "Cadangkan & pulihkan data aman via Google Drive",
                icon = Icons.Default.CloudUpload,
                accent = backupAccent,
                primaryButtonText = closeLabel,
                onDismiss = { activeModal = null },
                onPrimaryAction = { activeModal = null }
            ) {
                Text(
                    text = if (isEn)
                        "Backup safely stores all transactions, settings, Public Kas ID, and your Group Security Key so you can switch devices smoothly."
                    else
                        "Cadangan menyimpan seluruh transaksi, profil, ID Kas Publik, dan Kunci Pengaman Kelompok agar aman saat ganti HP.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (lastBackupTimestamp > 0L) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (lastBackupTimestamp > 0L) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastBackupDisplay,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                        .height(46.dp)
                        .testTag("btn_backup_google_drive"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = backupAccent.tint,
                        contentColor = if (isLight) Color.White else Color(0xFF0A192F)
                    )
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageHelper.getString("btn_backup_drive", selectedLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_restore_google_drive"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = backupAccent.tint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageHelper.getString("btn_restore_drive", selectedLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = backupAccent.tint
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
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
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = backupAccent.tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LanguageHelper.getString("btn_open_drive_app", selectedLanguage),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = backupAccent.tint
                    )
                }
            }
        }

        SettingsModalSection.TUTORIAL -> {
            SettingsPopupDialog(
                title = if (isEn) "Quick App Guide" else "Tutorial Singkat Penggunaan",
                subtitle = if (isEn) "5 easy steps to manage transparent cashbooks" else "5 langkah cepat mengelola kas secara transparan",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accent = tutorialAccent,
                primaryButtonText = closeLabel,
                onDismiss = { activeModal = null },
                onPrimaryAction = { activeModal = null }
            ) {
                val steps = if (isEn) {
                    listOf(
                        "Set Up Profile & Opening Balance" to "Open Profile & Admin, enter your group/RT name, secretariat address, and initial cash balance, then tap Save & Close.",
                        "Record Income & Expenses" to "Use the Income & Expense tabs (or quick buttons on Dashboard) to log dues, donations, and operational costs with receipt photos.",
                        "Track History & Audit Trail" to "Open History to search, filter, edit, or void transactions. Every change is recorded automatically in the Audit Log.",
                        "Share 1-Link Public Report (No Login)" to "Tap 'Update Web' to publish real-time balances without signing in, then share the read-only link to your WhatsApp group.",
                        "Export PDF/Excel & Backup Data" to "Download formal PDF or Excel reports from the Report tab, and back up to Google Drive regularly so your data stays safe."
                    )
                } else {
                    listOf(
                        "Atur Profil & Saldo Awal" to "Buka kartu Profil Kelompok & Admin, isi nama RT/organisasi, alamat sekretariat, dan saldo awal kas lalu tekan Simpan & Tutup.",
                        "Catat Pemasukan & Pengeluaran" to "Gunakan menu Masuk & Keluar (atau tombol cepat di Beranda) untuk mencatat iuran warga, donasi, dan pengeluaran beserta foto struk.",
                        "Cek Riwayat & Audit Log" to "Buka menu Riwayat untuk mencari, memfilter, mengedit, atau membatalkan transaksi. Semua perubahan tercatat otomatis di Audit Log.",
                        "Bagikan Laporan Publik 1 Link (Tanpa Login)" to "Tekan 'Update Web' tanpa perlu login, lalu kirim link ke Grup WA agar warga bisa memantau saldo real-time (Mode Hanya Lihat).",
                        "Unduh PDF/Excel & Cadangkan Data" to "Cetak atau unduh laporan PDF/Excel di menu Laporan, serta lakukan Cadangan ke Google Drive secara rutin agar data & kunci pengaman aman."
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    steps.forEachIndexed { index, (stepTitle, stepDesc) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, SoftCardBorderColor.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(11.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(tutorialAccent.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tutorialAccent.tint
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = stepTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stepDesc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsModalSection.ABOUT -> {
            val licenseRepo = remember(context) { LicenseRepository.getInstance(context) }
            val activeLicense = remember { licenseRepo.getLocalLicense() }
            val currentDeviceId = remember { licenseRepo.getOrCreateDeviceId() }

            SettingsPopupDialog(
                title = "Kas Transparan",
                subtitle = "v1.0.0 • Product by Rafline Dev",
                icon = Icons.Default.Smartphone,
                accent = aboutAccent,
                primaryButtonText = closeLabel,
                onDismiss = { activeModal = null },
                onPrimaryAction = { activeModal = null }
            ) {
                Text(
                    text = LanguageHelper.getString("app_desc", selectedLanguage),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, SoftCardBorderColor.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = LanguageHelper.getString("app_version", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                        Text(text = LanguageHelper.getString("developer", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                        Text(text = LanguageHelper.getString("platform", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                        Text(text = LanguageHelper.getString("release_year", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                        Text(text = LanguageHelper.getString("copyright", selectedLanguage), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeLicense?.activated == true) SoftSuccessBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (activeLicense?.activated == true) IncomeGreen.copy(alpha = 0.35f) else SoftCardBorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Status Lisensi: ${if (activeLicense?.activated == true) "Aktif (${activeLicense.licenseCode})" else "Belum Aktif"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (activeLicense?.activated == true) SoftSuccessFg else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ID Perangkat: $currentDeviceId",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        null -> Unit
    }

    // DIALOG HASIL PEMULIHAN DARI GOOGLE DRIVE
    if (showRestoreSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = IncomeGreen)
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
                Text(
                    LanguageHelper.getString("restore_fail_title", selectedLanguage),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = { Text(restoreErrorMessage) },
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            LanguageHelper.getString("search_language_hint", currentLanguage),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LanguageHelper.REGIONS) { region ->
                        FilterChip(
                            selected = selectedRegionTab == region,
                            onClick = { selectedRegionTab = region },
                            label = {
                                Text(
                                    if (region == "Semua") LanguageHelper.getString("region_all", currentLanguage) else region,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList) { opt ->
                        val isSelected = opt.code.equals(currentLanguage, ignoreCase = true)
                        Card(
                            shape = RoundedCornerShape(12.dp),
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
private fun ExportToggleRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, SoftCardBorderColor.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun ExcelColumnModeRadioOption(
    title: String,
    description: String,
    selected: Boolean,
    accentTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentTint else SoftCardBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = if (selected) accentTint else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

