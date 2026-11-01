package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.KasUiState
import com.example.ui.KasViewModel
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyHelper
import com.example.util.LanguageHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun IncomeScreen(
    state: KasUiState,
    viewModel: KasViewModel,
    onSuccessSaved: () -> Unit
) {
    val context = LocalContext.current
    val lang = state.settings.language
    val curr = state.settings.currencyCode
    val isEn = lang.equals("en", ignoreCase = true)
    val isLight = MaterialTheme.colorScheme.background == com.example.ui.theme.AppBackgroundLight

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (isEn) "Member Dues" else "Iuran Warga") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(if (isEn) "Cash" else "Tunai") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var errorValidation by remember { mutableStateOf<String?>(null) }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                date = sdf.format(cal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val incomeCategories = remember(isEn) {
        if (isEn) {
            listOf("Member Dues", "Donations", "Government Aid", "Fundraising / Bazaar", "Cross-Subsidies", "Bank Interest", "Other")
        } else {
            listOf("Iuran Warga", "Donasi", "Bantuan Pemerintah", "Penjualan / Bazar", "Kas Silang", "Bunga Bank", "Lainnya")
        }
    }

    val paymentMethods = remember(isEn) {
        if (isEn) {
            listOf("Cash", "Bank Transfer", "QRIS", "E-Wallet", "Other")
        } else {
            listOf("Tunai", "Transfer Bank", "QRIS", "E-Wallet", "Lainnya")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LanguageHelper.getString("form_income_title", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                        Text(
                            text = LanguageHelper.getString("form_income_subtitle", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        // Tanggal Transaksi
        OutlinedTextField(
            value = date,
            onValueChange = {},
            readOnly = true,
            label = { Text(LanguageHelper.getString("field_date", lang)) },
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() }
                .testTag("income_date_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Nama / Sumber Pemasukan
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(LanguageHelper.getString("field_income_source", lang)) },
            placeholder = { Text(LanguageHelper.getString("placeholder_income_title", lang)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("income_title_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Jumlah Uang (Nominal)
        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    amountText = input
                }
            },
            label = { Text(LanguageHelper.getString("field_amount", lang)) },
            placeholder = { Text("0") },
            prefix = { Text("${CurrencyHelper.getSymbol(curr)} ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("income_amount_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Kategori Pemasukan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = LanguageHelper.getString("field_category", lang),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(incomeCategories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("cat_chip_$cat")
                    )
                }
            }
        }

        // Metode Pembayaran
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = LanguageHelper.getString("field_payment_method", lang),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(paymentMethods) { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }
        }

        // Keterangan Tambahan
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(LanguageHelper.getString("field_description", lang)) },
            placeholder = { Text(if (isEn) "Additional notes (optional)" else "Catatan tambahan (opsional)") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("income_desc_input"),
            minLines = 2,
            shape = RoundedCornerShape(12.dp)
        )

        // Upload Bukti Pembayaran
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("income_upload_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = LanguageHelper.getString("field_upload_receipt", lang),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (receiptUri != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(receiptUri),
                            contentDescription = "Bukti Pembayaran",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { receiptUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(50))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus Foto")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageHelper.getString("btn_pick_photo", lang),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (errorValidation != null) {
            Text(
                text = errorValidation ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Tombol Simpan Transaksi
        Button(
            onClick = {
                val amount = amountText.toLongOrNull() ?: 0L
                if (title.isBlank()) {
                    errorValidation = LanguageHelper.getString("error_empty_title", lang)
                    return@Button
                }
                if (amount <= 0L) {
                    errorValidation = LanguageHelper.getString("error_invalid_amount", lang)
                    return@Button
                }
                errorValidation = null
                viewModel.addIncome(
                    title = title,
                    amount = amount,
                    category = category,
                    date = date,
                    description = description,
                    paymentMethod = paymentMethod,
                    receiptUrl = receiptUri?.toString() ?: "",
                    onSuccess = {
                        title = ""
                        amountText = ""
                        description = ""
                        receiptUri = null
                        onSuccessSaved()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_income_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            } else {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageHelper.getString("btn_save", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
}
