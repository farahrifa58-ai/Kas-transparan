package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
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
    val displayShortUrl = remember(publicUrl) {
        publicUrl.removePrefix("https://").removePrefix("http://")
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
    val isBusy = state.isPublishingPublic
    val waButtonColor = if (isLight) Color(0xFF1F9D55) else Color(0xFF217A49)

    var showOpenOtherIdDialog by remember { mutableStateOf(false) }
    var inputOtherKasId by remember { mutableStateOf("") }

    val cardBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (embedded) Modifier else Modifier.padding(horizontal = 14.dp, vertical = 12.dp)),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // BARIS 1: Header Ringkas (Globe kecil + Judul + Badge ID KAS-XXXXXX + Titik Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(publicAccent.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = publicAccent.tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEn) "Online Public Report" else "Laporan Publik Online",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(publicAccent.container)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = publicKasId,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp
                        ),
                        color = publicAccent.tint,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPublished) IncomeGreen
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }

            // BARIS 2: Kotak URL Singkat (+ tombol Salin di dalam) + Tombol Padat "Update Web"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = SoftCardBorderColor,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable {
                            WhatsAppShareHelper.copyTextToClipboard(
                                context = context,
                                label = "Link Laporan Publik",
                                text = publicUrl,
                                toastMessage = if (isEn) "Public link copied!" else "Tautan laporan publik disalin!"
                            )
                        }
                        .padding(start = 10.dp, end = 4.dp)
                        .testTag("public_link_box"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayShortUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                WhatsAppShareHelper.copyTextToClipboard(
                                    context = context,
                                    label = "Link Laporan Publik",
                                    text = publicUrl,
                                    toastMessage = if (isEn) "Public link copied!" else "Tautan laporan publik disalin!"
                                )
                            }
                            .testTag("btn_copy_url_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = if (isEn) "Copy link" else "Salin tautan",
                            tint = publicAccent.tint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.publishPublicReport() },
                    enabled = !isBusy,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("btn_sync_public_report"),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBusy) (if (isEn) "Syncing" else "Proses") else (if (isEn) "Update Web" else "Update Web"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // BARIS 3: Tombol Bagikan & Opsi Cepat sejajar 1 baris ("Salin WA", "Kirim WA", "Pratinjau", ikon Cari)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        .height(38.dp)
                        .testTag("btn_copy_link_wa"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SoftCardBorderColor),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Copy WA" else "Salin WA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
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
                        .height(38.dp)
                        .testTag("btn_share_public_whatsapp"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = waButtonColor,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Send WA" else "Kirim WA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { onOpenPublicPreview(publicKasId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_preview_public_readonly"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SoftCardBorderColor),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEn) "Preview" else "Pratinjau",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    onClick = {
                        inputOtherKasId = ""
                        showOpenOtherIdDialog = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("btn_lookup_other_kas"),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, SoftCardBorderColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = if (isEn) "Lookup Another Cash ID" else "Cek ID Kas Lain",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            // BARIS 4: Footer Kartu Tipis (Sinkronisasi Otomatis + Switch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEn) "Auto-Sync Online" else "Sinkronisasi Otomatis",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.publicAutoSync,
                    onCheckedChange = { enabled ->
                        viewModel.setPublicAutoSync(enabled = enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IncomeGreen
                    ),
                    modifier = Modifier
                        .scale(0.8f)
                        .testTag("switch_public_auto_sync")
                )
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
            cornerRadius = 18.dp,
            elevation = 4.dp
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
