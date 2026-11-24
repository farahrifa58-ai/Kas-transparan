package com.example.data.model

enum class ExcelColumnMode(val key: String) {
    COMPLETE("COMPLETE"),
    COMPACT("COMPACT");

    companion object {
        fun fromKey(raw: String?): ExcelColumnMode {
            return entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: COMPLETE
        }
    }
}

data class ExportPreferences(
    val showShareImage: Boolean = true,
    val showSharePdf: Boolean = true,
    val showShareExcel: Boolean = true,
    val excelColumnMode: ExcelColumnMode = ExcelColumnMode.COMPLETE,
    val showShareDoc: Boolean = true
) {
    val activeCount: Int
        get() = listOf(showShareImage, showSharePdf, showShareExcel, showShareDoc).count { it }

    fun ensureAtLeastOneActive(): ExportPreferences {
        return if (activeCount == 0) copy(showSharePdf = true) else this
    }
}
