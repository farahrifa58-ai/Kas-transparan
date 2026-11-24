package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.AuditLog
import com.example.data.model.ExportPreferences
import com.example.data.model.PublicAuthRequiredException
import com.example.data.model.PublicKasUiState
import com.example.data.model.Transaction
import com.example.data.repository.KasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class PeriodFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM
}

data class KasUiState(
    val transactions: List<Transaction> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val exportPreferences: ExportPreferences = ExportPreferences(),
    val auditLogs: List<AuditLog> = emptyList(),
    val selectedPeriod: PeriodFilter = PeriodFilter.THIS_MONTH,
    val customStartDate: String = "",
    val customEndDate: String = "",
    val searchQuery: String = "",
    val filterType: String = "ALL", // "ALL", "INCOME", "EXPENSE", "VOID"
    val filterCategory: String = "ALL",
    val isLoading: Boolean = false,
    val isPublishingPublic: Boolean = false,
    val linkedTreasurerEmail: String? = null,
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null
)

class KasViewModel(
    private val repository: KasRepository,
    private val userId: String
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.THIS_MONTH)
    private val _customStartDate = MutableStateFlow("")
    private val _customEndDate = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("ALL")
    private val _filterCategory = MutableStateFlow("ALL")
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isPublishingPublic = MutableStateFlow(false)
    private val _linkedTreasurerEmail = MutableStateFlow(repository.getSignedInTreasurerEmail())

    val transactions: StateFlow<List<Transaction>> = repository.observeTransactions(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.observeSettings(userId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        .combine(MutableStateFlow(AppSettings(userId = userId))) { remote, default ->
            remote ?: default
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings(userId = userId))

    val auditLogs: StateFlow<List<AuditLog>> = repository.observeAuditLogs(userId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportPreferences: StateFlow<ExportPreferences> = repository.observeExportPreferences()
        .catch { emit(ExportPreferences()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExportPreferences())

    val isPublishingPublic: StateFlow<Boolean> = _isPublishingPublic.asStateFlow()

    val uiState: StateFlow<KasUiState> = combine(
        transactions,
        settings,
        auditLogs,
        _selectedPeriod,
        _searchQuery,
        _filterType,
        _filterCategory,
        _feedbackMessage,
        _isLoading,
        _isPublishingPublic,
        _linkedTreasurerEmail,
        exportPreferences
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val txs = args[0] as List<Transaction>
        val currentSettings = args[1] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val logs = args[2] as List<AuditLog>
        val period = args[3] as PeriodFilter
        val query = args[4] as String
        val type = args[5] as String
        val category = args[6] as String
        val feedback = args[7] as String?
        val loading = args[8] as Boolean
        val publishingPublic = args[9] as Boolean
        val linkedEmail = args[10] as String?
        val currentExportPrefs = args[11] as ExportPreferences

        KasUiState(
            transactions = txs,
            settings = currentSettings,
            exportPreferences = currentExportPrefs,
            auditLogs = logs,
            selectedPeriod = period,
            customStartDate = _customStartDate.value,
            customEndDate = _customEndDate.value,
            searchQuery = query,
            filterType = type,
            filterCategory = category,
            isLoading = loading,
            isPublishingPublic = publishingPublic,
            linkedTreasurerEmail = linkedEmail,
            userFeedbackMessage = feedback
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KasUiState())

    fun updateExportPreferences(newPrefs: ExportPreferences): Boolean {
        if (newPrefs.activeCount == 0) {
            _feedbackMessage.value = "Minimal 1 opsi ekspor harus tetap aktif"
            return false
        }
        repository.saveExportPreferences(newPrefs)
        return true
    }

    fun refreshAuthStatus() {
        _linkedTreasurerEmail.value = repository.getSignedInTreasurerEmail()
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun publishPublicReport(
        onComplete: (Result<Long>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isPublishingPublic.value = true
            val result = repository.publishPublicKasReport()
            _isPublishingPublic.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Laporan publik berhasil diperbarui!"
            } else {
                val ex = result.exceptionOrNull()
                _feedbackMessage.value = ex?.localizedMessage ?: "Gagal menyinkronkan laporan ke Web."
            }
            onComplete(result)
        }
    }

    fun setPublicAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            val updated = settings.value.copy(publicAutoSync = enabled)
            repository.saveSettings(updated, triggerPublicSync = false)
            _feedbackMessage.value = if (enabled) {
                "Sinkronisasi otomatis saat online diaktifkan!"
            } else {
                "Sinkronisasi otomatis dinonaktifkan."
            }
            if (enabled) {
                publishPublicReport()
            }
        }
    }

    fun observePublicKasReport(publicKasId: String): Flow<PublicKasUiState> {
        return repository.observePublicKasReport(publicKasId)
    }

    fun setPeriodFilter(filter: PeriodFilter, start: String = "", end: String = "") {
        _selectedPeriod.value = filter
        _customStartDate.value = start
        _customEndDate.value = end
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun addIncome(
        title: String,
        amount: Long,
        category: String,
        date: String,
        description: String,
        paymentMethod: String,
        receiptUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val tx = Transaction(
                userId = userId,
                type = "INCOME",
                title = title.trim(),
                amount = amount,
                category = category.trim(),
                date = date,
                description = description.trim(),
                paymentMethod = paymentMethod.ifBlank { "Tunai" },
                receiptUrl = receiptUrl,
                status = "ACTIVE"
            )
            val result = repository.addTransaction(tx)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Pemasukan berhasil dicatat!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal mencatat pemasukan: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addExpense(
        title: String,
        amount: Long,
        category: String,
        date: String,
        description: String,
        paymentMethod: String,
        receiptUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val tx = Transaction(
                userId = userId,
                type = "EXPENSE",
                title = title.trim(),
                amount = amount,
                category = category.trim(),
                date = date,
                description = description.trim(),
                paymentMethod = paymentMethod.ifBlank { "Tunai" },
                receiptUrl = receiptUrl,
                status = "ACTIVE"
            )
            val result = repository.addTransaction(tx)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Pengeluaran berhasil dicatat!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal mencatat pengeluaran: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateTransaction(
        transaction: Transaction,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateTransaction(transaction, reason)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Transaksi berhasil diperbarui!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal memperbarui: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun voidTransaction(
        transaction: Transaction,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.voidTransaction(transaction, reason)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Transaksi berhasil dibatalkan."
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal membatalkan transaksi: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun saveSettings(newSettings: AppSettings, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveSettings(newSettings.copy(userId = userId))
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Pengaturan berhasil disimpan!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal menyimpan pengaturan: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun addInitialBalance(addedAmount: Long, note: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addInitialBalance(addedAmount, note)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Saldo awal berhasil ditambahkan!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal menambah saldo awal: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun setInitialBalance(newAmount: Long, note: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.setInitialBalance(newAmount, note)
            _isLoading.value = false
            if (result.isSuccess) {
                _feedbackMessage.value = "Saldo awal berhasil diperbarui!"
                onSuccess()
            } else {
                _feedbackMessage.value = "Gagal memperbarui saldo awal: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun getFilteredTransactions(
        list: List<Transaction>,
        period: PeriodFilter,
        customStart: String,
        customEnd: String
    ): List<Transaction> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val now = Calendar.getInstance()
        val todayStr = sdf.format(now.time)

        now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
        val weekStartStr = sdf.format(now.time)

        val calMonth = Calendar.getInstance()
        calMonth.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartStr = sdf.format(calMonth.time)

        val calYear = Calendar.getInstance()
        calYear.set(Calendar.DAY_OF_YEAR, 1)
        val yearStartStr = sdf.format(calYear.time)

        return list.filter { tx ->
            when (period) {
                PeriodFilter.ALL -> true
                PeriodFilter.TODAY -> tx.date == todayStr
                PeriodFilter.THIS_WEEK -> tx.date >= weekStartStr && tx.date <= todayStr
                PeriodFilter.THIS_MONTH -> tx.date >= monthStartStr
                PeriodFilter.THIS_YEAR -> tx.date >= yearStartStr
                PeriodFilter.CUSTOM -> {
                    val afterStart = if (customStart.isNotBlank()) tx.date >= customStart else true
                    val beforeEnd = if (customEnd.isNotBlank()) tx.date <= customEnd else true
                    afterStart && beforeEnd
                }
            }
        }
    }

    fun createBackupFile(): java.io.File? {
        return repository.createBackupFile()
    }

    fun restoreBackup(jsonString: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.restoreBackup(jsonString)
            _isLoading.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _feedbackMessage.value = "Data berhasil dipulihkan! ($count transaksi dimuat)"
            } else {
                _feedbackMessage.value = "Gagal memulihkan cadangan: ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete(result)
        }
    }

    fun getLastBackupTimestamp(): Long {
        return repository.getLastBackupTimestamp()
    }

    fun setLastBackupTimestamp(time: Long) {
        repository.setLastBackupTimestamp(time)
    }
}
