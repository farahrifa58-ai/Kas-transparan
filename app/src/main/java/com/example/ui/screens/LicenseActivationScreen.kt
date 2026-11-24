package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.LicenseActivationResult
import com.example.data.repository.LicenseGateState
import com.example.data.repository.LicenseRepository
import com.example.data.repository.LocalLicenseInfo
import com.example.ui.components.SoftElevatedCard
import com.example.ui.theme.AppScreenBackgroundBrush
import com.example.ui.theme.SoftCardBorderColor
import com.example.ui.theme.SoftDangerBg
import com.example.ui.theme.SoftDangerFg
import com.example.ui.theme.SoftSuccessBg
import com.example.ui.theme.SoftSuccessFg
import com.example.ui.theme.isAppLightTheme
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale

@Composable
fun LicenseCheckingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScreenBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SoftElevatedCard(
            modifier = Modifier.widthIn(max = 420.dp),
            cornerRadius = 24.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_custom_logo),
                        contentDescription = "Logo Kas Transparan",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )

                Text(
                    text = "Memeriksa Lisensi Perangkat...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Memverifikasi aktivasi otomatis untuk perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LicenseActivationScreen(
    deviceId: String,
    licenseRepository: LicenseRepository,
    initialInfoMessage: String? = null,
    onActivated: (LocalLicenseInfo) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLight = isAppLightTheme

    var licenseCodeInput by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf(initialInfoMessage) }
    var errorDetail by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun submitActivation() {
        if (isActivating || isRestoring) return
        keyboardController?.hide()
        errorMessage = null
        errorDetail = null
        statusMessage = null
        val cleaned = licenseCodeInput.trim().uppercase(Locale.ROOT)
        licenseCodeInput = cleaned

        coroutineScope.launch {
            isActivating = true
            when (val result = licenseRepository.activateLicenseCode(cleaned)) {
                is LicenseActivationResult.Success -> {
                    statusMessage = "Lisensi aktif! Membuka Kas Transparan..."
                    onActivated(result.info)
                }
                is LicenseActivationResult.Failure -> {
                    errorMessage = result.message
                    errorDetail = result.errorDetail
                }
            }
            isActivating = false
        }
    }

    fun retryAutoRestore() {
        if (isActivating || isRestoring) return
        keyboardController?.hide()
        errorMessage = null
        errorDetail = null
        statusMessage = null
        coroutineScope.launch {
            isRestoring = true
            when (val gate = licenseRepository.checkOrAutoRestoreLicense()) {
                is LicenseGateState.Activated -> {
                    statusMessage = "Lisensi perangkat ditemukan & dipulihkan otomatis!"
                    onActivated(gate.info)
                }
                is LicenseGateState.Unlicensed -> {
                    errorMessage = "Perangkat ini belum terdaftar aktif. Silakan masukkan kode lisensi."
                }
                is LicenseGateState.Checking -> Unit
            }
            isRestoring = false
        }
    }

    fun openEmailAdmin() {
        val adminEmail = licenseRepository.adminEmail
        val subject = "Pemesanan Lisensi Kas Transparan"
        val body = """
            Halo Admin Rafline Dev,
            Saya ingin membeli / mengaktifkan kode lisensi Kas Transparan (1 Perangkat).

            ID Perangkat: $deviceId
        """.trimIndent()

        try {
            val mailtoUri = Uri.parse(
                "mailto:${Uri.encode(adminEmail)}?subject=${URLEncoder.encode(subject, "UTF-8").replace("+", "%20")}&body=${URLEncoder.encode(body, "UTF-8").replace("+", "%20")}"
            )
            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(adminEmail))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                val chooser = Intent.createChooser(fallback, "Kirim Email ke $adminEmail")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (_: Exception) {
                WhatsAppShareHelper.copyTextToClipboard(
                    context = context,
                    label = "Email Admin Lisensi",
                    text = adminEmail,
                    toastMessage = "Email admin disalin: $adminEmail"
                )
            }
        }
    }

    val emailButtonBg = if (isLight) Color(0xFFE6F4F1) else Color(0xFF1F3836)
    val emailButtonFg = if (isLight) Color(0xFF0F766E) else Color(0xFF5EEAD4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScreenBackgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Identity Header
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, SoftCardBorderColor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_custom_logo),
                    contentDescription = "Logo Kas Transparan",
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Aktivasi Lisensi • 1 Perangkat",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aktivasi Lisensi Kas Transparan",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Masukkan kode lisensi bendahara untuk membuka pembukuan kas secara penuh dan offline-first di perangkat ini.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Soft UI Activation Card
            SoftElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("license_activation_card"),
                cornerRadius = 22.dp,
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = licenseCodeInput,
                        onValueChange = { raw ->
                            // Auto uppercase & strip spaces/newlines cleanly
                            licenseCodeInput = raw.replace(" ", "").replace("\n", "").uppercase(Locale.ROOT)
                            if (errorMessage != null) errorMessage = null
                            if (errorDetail != null) errorDetail = null
                        },
                        label = { Text("Kode Lisensi") },
                        placeholder = { Text("Contoh: RAFDEV-LFZA-L3K2") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        enabled = !isActivating && !isRestoring,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitActivation() }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_license_code")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftDangerBg,
                            border = BorderStroke(1.dp, SoftDangerFg.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("license_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = SoftDangerFg,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(18.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SoftDangerFg
                                    )
                                    if (!errorDetail.isNullOrBlank()) {
                                        Text(
                                            text = errorDetail.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                lineHeight = 14.sp
                                            ),
                                            color = SoftDangerFg.copy(alpha = 0.85f),
                                            modifier = Modifier.testTag("license_error_detail")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftSuccessBg,
                            border = BorderStroke(1.dp, SoftSuccessFg.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SoftSuccessFg,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { submitActivation() },
                        enabled = !isActivating && !isRestoring && licenseCodeInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_activate_license"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isActivating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Memverifikasi Lisensi...",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aktifkan Lisensi",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }

                    // Auto-restore retry button for users who reinstalled/cleared cache
                    TextButton(
                        onClick = { retryAutoRestore() },
                        enabled = !isActivating && !isRestoring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_retry_restore_license"),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mengecek Lisensi Perangkat...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sudah Pernah Aktif di HP Ini? Pulihkan Otomatis",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                    // Buy / Contact Admin via Email
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { openEmailAdmin() },
                            enabled = !isActivating && !isRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_buy_license_email"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emailButtonBg,
                                contentColor = emailButtonFg
                            ),
                            border = BorderStroke(1.dp, emailButtonFg.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MailOutline,
                                contentDescription = null,
                                tint = emailButtonFg,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Beli / Hubungi Admin via Email",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = emailButtonFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = licenseRepository.adminEmail,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Device ID Footer Card with Copy Button
            SoftElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_id_info_card"),
                cornerRadius = 16.dp,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ID Perangkat (untuk bantuan admin)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            WhatsAppShareHelper.copyTextToClipboard(
                                context = context,
                                label = "ID Perangkat Kas Transparan",
                                text = deviceId,
                                toastMessage = "ID Perangkat berhasil disalin!"
                            )
                        },
                        modifier = Modifier.testTag("btn_copy_device_id")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Salin ID Perangkat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Aktivasi hanya dilakukan sekali per perangkat. Setelah aktif, pembukuan kas tetap berjalan 100% offline.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
