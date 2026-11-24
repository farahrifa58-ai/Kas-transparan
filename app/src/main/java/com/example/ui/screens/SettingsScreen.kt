package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.PublicKasHelper
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.components.ExpandableSettingsCard
import com.example.ui.components.PublicTransparencyCard
import com.example.ui.components.SoftElevatedCard
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
import com.example.ui.theme.accentProfile
import com.example.ui.theme.accentPublicReport
import com.example.ui.theme.accentTutorial
import com.example.ui.theme.isAppLightTheme
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Independent expandable states for each Settings card
    var expandedProfile by remember { mutableStateOf(false) }
    var expandedAppearance by remember { mutableStateOf(false) }
    var expandedPublicReport by remember { mutableStateOf(false) }
    var expandedDomain by remember { mutableStateOf(false) }
    var expandedCurrency by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }
    var expandedTutorial by remember { mutableStateOf(false) }

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
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
    val backupAccent = accentBackup()
    val tutorialAccent = accentTutorial()
    val aboutAccent = accentAbout()

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
                        onClick = {
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
                                )
                            ) {
                                onBack()
                            }
                        },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. PROFIL KELOMPOK & ADMIN (Expandable)
                val parsedInitialPreview = CurrencyHelper.parseCleanAmount(initialBalanceText)
                ExpandableSettingsCard(
                    title = LanguageHelper.getString("profile_section", selectedLanguage),
                    subtitle = "${groupName.ifBlank { "Kas Transparan" }} • ${CurrencyHelper.format(parsedInitialPreview, selectedCurrency)}",
                    icon = Icons.Default.Person,
                    accent = profileAccent,
                    expanded = expandedProfile,
                    onToggle = { expandedProfile = !expandedProfile },
                    modifier = Modifier.testTag("settings_profile_card")
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

                    // Neat 2x2 Grid for Quick Addition Presets (never wraps awkwardly!)
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
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "+ ${CurrencyHelper.format(addAmt, selectedCurrency)}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
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

                // 2. TEMA & BAHASA (Digabung dalam SATU kartu yang bisa dibuka)
                ExpandableSettingsCard(
                    title = if (isEn) "Theme & Language" else "Tema & Bahasa",
                    subtitle = "$currentThemeLabel • ${currentLanguageOption.flag} ${currentLanguageOption.nativeName}",
                    icon = Icons.Default.Palette,
                    accent = appearanceAccent,
                    expanded = expandedAppearance,
                    onToggle = { expandedAppearance = !expandedAppearance },
                    modifier = Modifier.testTag("settings_theme_language_card")
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

                // 3. LAPORAN PUBLIK ONLINE (LINK ANGGOTA) (Expandable)
                val isPublicSynced = state.settings.publicLastSyncedAt > 0L
                ExpandableSettingsCard(
                    title = if (isEn) "Online Public Report" else "Laporan Publik Online",
                    subtitle = publicStatusSummary,
                    icon = Icons.Default.Public,
                    accent = publicAccent,
                    badgeText = if (isPublicSynced) (if (isEn) "Live" else "Aktif") else (if (isEn) "Draft" else "Offline"),
                    badgeColor = if (isPublicSynced) SoftSuccessFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    badgeContainerColor = if (isPublicSynced) SoftSuccessBg else MaterialTheme.colorScheme.surfaceVariant,
                    expanded = expandedPublicReport,
                    onToggle = { expandedPublicReport = !expandedPublicReport },
                    modifier = Modifier.testTag("settings_public_report_card")
                ) {
                    PublicTransparencyCard(
                        state = state,
                        viewModel = viewModel,
                        onOpenPublicPreview = onOpenPublicPreview,
                        embedded = true
                    )
                }

                // 4. DOMAIN WEB LAPORAN PUBLIK (Expandable — Fixed Production Domain)
                val fixedProductionBaseUrl = AppSettings.PUBLIC_WEB_BASE_URL
                val fixedPublicLinkPreview = remember(state.settings.publicKasId) {
                    PublicKasHelper.buildPublicUrl(state.settings.publicKasId.ifBlank { "KAS-..." })
                }
                ExpandableSettingsCard(
                    title = if (isEn) "Public Web Domain" else "Domain Web Laporan Publik",
                    subtitle = fixedProductionBaseUrl.removePrefix("https://"),
                    icon = Icons.Default.Link,
                    accent = domainAccent,
                    badgeText = if (isEn) "Fixed" else "Tetap",
                    badgeColor = domainAccent.tint,
                    badgeContainerColor = domainAccent.container,
                    expanded = expandedDomain,
                    onToggle = { expandedDomain = !expandedDomain },
                    modifier = Modifier.testTag("settings_public_url_card")
                ) {
                    Text(
                        text = if (isEn)
                            "All member links permanently use the fixed production web domain with hash route /#/lihat/${state.settings.publicKasId}."
                        else
                            "Semua tautan laporan anggota secara permanen memakai domain web produksi tetap dengan rute /#/lihat/${state.settings.publicKasId}.",
                        style = MaterialTheme.typography.bodySmall,
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
                    Text(
                        text = fixedPublicLinkPreview,
                        style = MaterialTheme.typography.labelSmall,
                        color = domainAccent.tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 5. MATA UANG (Expandable)
                ExpandableSettingsCard(
                    title = LanguageHelper.getString("currency_section", selectedLanguage),
                    subtitle = "${currentCurrencyOption?.code ?: selectedCurrency} (${currentCurrencyOption?.symbol ?: selectedCurrency}) • ${currentCurrencyOption?.name ?: ""}",
                    icon = Icons.Default.MonetizationOn,
                    accent = currencyAccent,
                    expanded = expandedCurrency,
                    onToggle = { expandedCurrency = !expandedCurrency },
                    modifier = Modifier.testTag("settings_currency_card")
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

                // 6. CADANGAN & PEMULIHAN (GOOGLE DRIVE) (Expandable)
                ExpandableSettingsCard(
                    title = LanguageHelper.getString("backup_drive_title", selectedLanguage),
                    subtitle = lastBackupDisplay,
                    icon = Icons.Default.CloudUpload,
                    accent = backupAccent,
                    expanded = expandedBackup,
                    onToggle = { expandedBackup = !expandedBackup },
                    modifier = Modifier.testTag("settings_google_drive_card")
                ) {
                    Text(
                        text = if (isEn)
                            "Backup safely stores all transactions, settings, Public Kas ID, and your Group Security Key so you can switch devices smoothly."
                        else
                            "Cadangan menyimpan seluruh transaksi, profil, ID Kas Publik, dan Kunci Pengaman Kelompok agar aman saat ganti HP.",
                        style = MaterialTheme.typography.bodySmall,
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
                            .height(48.dp)
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
                            .height(48.dp)
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

                // 7. TUTORIAL SINGKAT PENGGUNAAN APLIKASI (Kartu Baru Bisa Dibuka)
                ExpandableSettingsCard(
                    title = if (isEn) "Quick App Guide" else "Tutorial Singkat Penggunaan",
                    subtitle = if (isEn) "5 easy steps to manage transparent cashbooks" else "Panduan cepat 5 langkah pakai Kas Transparan",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = tutorialAccent,
                    badgeText = if (isEn) "Guide" else "Panduan",
                    expanded = expandedTutorial,
                    onToggle = { expandedTutorial = !expandedTutorial },
                    modifier = Modifier.testTag("settings_tutorial_card")
                ) {
                    val steps = if (isEn) {
                        listOf(
                            "Set Up Profile & Opening Balance" to "Open Profile & Admin above, enter your group/RT name, secretariat address, and initial cash balance, then tap Save.",
                            "Record Income & Expenses" to "Use the Income & Expense tabs (or quick buttons on Dashboard) to log dues, donations, and operational costs with receipt photos.",
                            "Track History & Audit Trail" to "Open History to search, filter, edit, or void transactions. Every change is recorded automatically in the Audit Log.",
                            "Share 1-Link Public Report (No Login)" to "Tap 'Sync / Update Report to Web' to publish real-time balances without signing in, then share the read-only link to your WhatsApp group.",
                            "Export PDF/Excel & Backup Data" to "Download formal PDF or Excel reports from the Report tab, and back up to Google Drive regularly so your data and Group Security Key stay safe."
                        )
                    } else {
                        listOf(
                            "Atur Profil & Saldo Awal" to "Buka kartu Profil Kelompok & Admin di atas, isi nama RT/organisasi, alamat sekretariat, dan saldo awal kas lalu tekan Simpan.",
                            "Catat Pemasukan & Pengeluaran" to "Gunakan menu Masuk & Keluar (atau tombol cepat di Beranda) untuk mencatat iuran warga, donasi, dan pengeluaran beserta foto struk.",
                            "Cek Riwayat & Audit Log" to "Buka menu Riwayat untuk mencari, memfilter, mengedit, atau membatalkan transaksi. Semua perubahan tercatat otomatis di Audit Log.",
                            "Bagikan Laporan Publik 1 Link (Tanpa Login)" to "Tekan 'Sinkronkan / Update Laporan ke Web' tanpa perlu login, lalu kirim link ke Grup WA agar warga bisa memantau saldo real-time (Mode Hanya Lihat).",
                            "Unduh PDF/Excel & Cadangkan Data" to "Cetak atau unduh laporan PDF/Excel di menu Laporan, serta lakukan Cadangan ke Google Drive secara rutin agar data & kunci pengaman aman saat ganti HP."
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
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
                                                fontSize = 13.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stepDesc,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. TENTANG PERANGKAT & APLIKASI
                SoftElevatedCard(
                    modifier = Modifier
                        .clickable { showAboutDialog = true }
                        .testTag("settings_about_card"),
                    cornerRadius = 20.dp,
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(aboutAccent.container),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = aboutAccent.tint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageHelper.getString("about_device_app", selectedLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "KAS TRANSPARAN v1.0.0 by Rafline Dev",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalButton(
                            onClick = { showAboutDialog = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
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
fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else Color.Transparent
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onSelect
        )
    }
}
