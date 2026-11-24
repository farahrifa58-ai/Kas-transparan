package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublicKasHelper
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SoftCardBorderColor
import com.example.ui.theme.SoftSuccessBg
import com.example.ui.theme.SoftSuccessFg
import com.example.ui.theme.accentPublicReport
import com.example.ui.theme.isAppLightTheme
import com.example.util.WhatsAppShareHelper

@Composable
fun PublicTransparencyCard(
    state: KasUiState,
    viewModel: KasViewModel,
    onOpenPublicPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val context = LocalContext.current
    val publicAccent = accentPublicReport()
    val isLight = isAppLightTheme

    val settings = state.settings
    val lang = settings.language
    val isEn = lang.startsWith("en", ignoreCase = true)
    val publicKasId = settings.publicKasId.ifBlank { "KAS-PENDING" }
    val publicUrl = remember(publicKasId) {
        PublicKasHelper.buildPublicUrl(publicKasId)
    }

    val activeTransactions = remember(state.transactions) {
        state.transactions.filter { it.status == "ACTIVE" }
    }
    val totalIncome = remember(activeTransactions) {
        activeTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(activeTransactions) {
        activeTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalBalance = settings.initialBalance + totalIncome - totalExpense

    val isPublished = settings.publicLastSyncedAt > 0L
    val syncStatusText = remember(settings.publicLastSyncedAt, lang) {
        PublicKasHelper.formatSyncTimestamp(settings.publicLastSyncedAt, lang)
    }

    var showOpenOtherIdDialog by remember { mutableStateOf(false) }
    var inputOtherKasId by remember { mutableStateOf("") }

    val cardBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (embedded) Modifier else Modifier.padding(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!embedded) {
                // Header (shown when standalone on Dashboard / Report)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(publicAccent.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = publicAccent.tint,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Online Public Report (Member Link)" else "Laporan Publik Online (Link Anggota)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEn) "1-Link Read-Only transparency • Offline-first" else "Transparansi 1 Link Mode Hanya Lihat • Offline-First",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status Pill + Permanent Public Kas ID Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPublished) SoftSuccessBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPublished) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncStatusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = if (isPublished) SoftSuccessFg else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(publicAccent.container)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = publicKasId,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = publicAccent.tint
                    )
                }
            }

            // Automatic Local Group Security Key indicator (never shows secret token)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEn)
                        "Group Security Key Active • Direct sync without login"
                    else
                        "Kunci Pengaman Kelompok Aktif • Sinkron langsung tanpa login",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Read-Only Public URL Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = SoftCardBorderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        WhatsAppShareHelper.copyTextToClipboard(
                            context = context,
                            label = "Link Laporan Publik",
                            text = publicUrl,
                            toastMessage = if (isEn) "Public link copied!" else "Tautan laporan publik disalin!"
                        )
                    }
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    .testTag("public_link_box"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = publicAccent.tint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = publicUrl,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        WhatsAppShareHelper.copyTextToClipboard(
                            context = context,
                            label = "Link Laporan Publik",
                            text = publicUrl,
                            toastMessage = if (isEn) "Public link copied!" else "Tautan laporan publik disalin!"
                        )
                    },
                    modifier = Modifier.testTag("btn_copy_url_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = if (isEn) "Copy link" else "Salin tautan",
                        tint = publicAccent.tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Primary Action Button: Sinkronkan / Update Laporan ke Web (Direct without login)
            val isBusy = state.isPublishingPublic
            Button(
                onClick = { viewModel.publishPublicReport() },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_sync_public_report"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isEn) "Syncing to Web..." else "Menyinkronkan ke Web...",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEn) "Sync / Update Report to Web" else "Sinkronkan / Update Laporan ke Web",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Copy Link & Share to WhatsApp Row
            val waButtonColor = if (isLight) Color(0xFF1F9D55) else Color(0xFF217A49)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        WhatsAppShareHelper.copyTextToClipboard(
                            context = context,
                            label = "Link Grup WA",
                            text = publicUrl,
                            toastMessage = if (isEn) "Link copied for WhatsApp group!" else "Link disalin! Siap ditempel di Grup WA."
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_copy_link_wa"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Copy Link" else "Salin Link WA",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        val message = PublicKasHelper.buildWhatsAppPublicLinkMessage(
                            settings = settings,
                            totalBalance = totalBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            publicUrl = publicUrl
                        )
                        WhatsAppShareHelper.shareTextToWhatsApp(context, message)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_public_whatsapp"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = waButtonColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "WhatsApp" else "Bagikan ke WA",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Toggle: Sinkronisasi Otomatis saat Online
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (isEn) "Auto-Sync When Online" else "Sinkronisasi Otomatis saat Online",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) "Automatically update web report whenever transactions change while online"
                        else "Otomatis perbarui laporan web tiap transaksi ditambah/diubah saat online",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.publicAutoSync,
                    onCheckedChange = { enabled ->
                        viewModel.setPublicAutoSync(enabled = enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IncomeGreen
                    ),
                    modifier = Modifier.testTag("switch_public_auto_sync")
                )
            }

            // Preview Read-Only Mode & Open Another Group ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenPublicPreview(publicKasId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_preview_public_readonly"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Read-Only Preview" else "Pratinjau Warga",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        inputOtherKasId = ""
                        showOpenOtherIdDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_lookup_other_kas"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Open ID Kas" else "Cek ID Kas Lain",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    if (embedded) {
        Box(modifier = modifier.fillMaxWidth().testTag("public_transparency_card")) {
            cardBody()
        }
    } else {
        SoftElevatedCard(
            modifier = modifier.testTag("public_transparency_card"),
            cornerRadius = 20.dp,
            elevation = 5.dp
        ) {
            cardBody()
        }
    }

    if (showOpenOtherIdDialog) {
        AlertDialog(
            onDismissRequest = { showOpenOtherIdDialog = false },
            title = {
                Text(
                    text = if (isEn) "Open Public Cash Report" else "Buka Laporan Kas Publik",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEn) "Enter a Public Cash ID (e.g. KAS-8A49B2X7) to monitor cash balance and transactions in Read-Only mode."
                        else "Masukkan ID Kas Publik (contoh: KAS-8A49B2X7) untuk memantau saldo & riwayat transaksi dalam Mode Hanya Lihat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputOtherKasId,
                        onValueChange = { inputOtherKasId = it.uppercase() },
                        label = { Text("ID Kas Publik") },
                        placeholder = { Text("KAS-8A49B2X7") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lookup_public_kas_id")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val normalized = PublicKasHelper.normalizePublicKasId(inputOtherKasId)
                        if (normalized.isNotBlank()) {
                            showOpenOtherIdDialog = false
                            onOpenPublicPreview(normalized)
                        }
                    },
                    enabled = inputOtherKasId.trim().length >= 4,
                    modifier = Modifier.testTag("btn_confirm_lookup_kas_id")
                ) {
                    Text(if (isEn) "View Report" else "Lihat Laporan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenOtherIdDialog = false }) {
                    Text(if (isEn) "Cancel" else "Batal")
                }
            }
        )
    }
}
