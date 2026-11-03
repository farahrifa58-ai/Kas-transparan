package com.example.util

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val region: String // "Asia Tenggara", "Asia", "Eropa", "Afrika", "Amerika"
)

object LanguageHelper {

    val REGIONS = listOf(
        "Semua",
        "Asia Tenggara",
        "Asia",
        "Eropa",
        "Afrika",
        "Amerika"
    )

    val SUPPORTED_LANGUAGES = listOf(
        // Asia Tenggara (Southeast Asia)
        LanguageOption("id", "Bahasa Indonesia", "Bahasa Indonesia", "🇮🇩", "Asia Tenggara"),
        LanguageOption("ms", "Melayu", "Bahasa Melayu", "🇲🇾", "Asia Tenggara"),
        LanguageOption("th", "Thailand", "ไทย (Thai)", "🇹🇭", "Asia Tenggara"),
        LanguageOption("vi", "Vietnam", "Tiếng Việt", "🇻🇳", "Asia Tenggara"),
        LanguageOption("fil", "Filipina", "Filipino / Tagalog", "🇵🇭", "Asia Tenggara"),

        // Asia
        LanguageOption("ja", "Jepang", "日本語 (Japanese)", "🇯🇵", "Asia"),
        LanguageOption("ko", "Korea Selatan", "한국어 (Korean)", "🇰🇷", "Asia"),
        LanguageOption("zh", "China / Tiongkok", "中文 (Simplified)", "🇨🇳", "Asia"),
        LanguageOption("hi", "India", "हिन्दी (Hindi)", "🇮🇳", "Asia"),
        LanguageOption("ar", "Arab Saudi", "العربية (Arabic)", "🇸🇦", "Asia"),
        LanguageOption("tr", "Turki", "Türkçe", "🇹🇷", "Asia"),

        // Eropa (Europe)
        LanguageOption("en", "Inggris (UK)", "English (UK)", "🇬🇧", "Eropa"),
        LanguageOption("de", "Jerman", "Deutsch", "🇩🇪", "Eropa"),
        LanguageOption("fr", "Prancis", "Français", "🇫🇷", "Eropa"),
        LanguageOption("es", "Spanyol", "Español", "🇪🇸", "Eropa"),
        LanguageOption("it", "Italia", "Italiano", "🇮🇹", "Eropa"),
        LanguageOption("nl", "Belanda", "Nederlands", "🇳🇱", "Eropa"),
        LanguageOption("ru", "Rusia", "Русский", "🇷🇺", "Eropa"),

        // Afrika (Africa)
        LanguageOption("sw", "Swahili (Kenya/Tanzania)", "Kiswahili", "🇰🇪", "Afrika"),
        LanguageOption("af", "Afrika Selatan", "Afrikaans", "🇿🇦", "Afrika"),
        LanguageOption("ha", "Nigeria (Hausa)", "Harshen Hausa", "🇳🇬", "Afrika"),
        LanguageOption("yo", "Nigeria (Yoruba)", "Èdè Yorùbá", "🇳🇬", "Afrika"),
        LanguageOption("am", "Etiopia", "አማርኛ (Amharic)", "🇪🇹", "Afrika"),

        // Amerika (Americas)
        LanguageOption("en_us", "Amerika Serikat", "English (US)", "🇺🇸", "Amerika"),
        LanguageOption("pt_br", "Brasil", "Português (Brasil)", "🇧🇷", "Amerika"),
        LanguageOption("es_mx", "Meksiko / Amerika Latin", "Español (México)", "🇲🇽", "Amerika")
    )

    fun getLanguageOption(code: String): LanguageOption {
        return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()
    }

    private val ID_STRINGS = mapOf(
        // Navigation
        "nav_dashboard" to "Beranda",
        "nav_income" to "Masuk",
        "nav_expense" to "Keluar",
        "nav_history" to "Riwayat",
        "nav_report" to "Laporan",

        // Dashboard
        "app_title" to "KAS TRANSPARAN",
        "app_tagline" to "Aplikasi Pengelolaan Kas Modern, Akuntabel, dan Transparan",
        "admin_label" to "Admin",
        "current_balance" to "Saldo Kas Saat Ini",
        "initial_balance" to "Saldo Awal",
        "total_income" to "Pemasukan",
        "total_expense" to "Pengeluaran",
        "final_balance" to "Saldo Akhir",
        "balance_formula" to "Saldo Akhir = Saldo Awal + Total Masuk - Total Keluar",
        "recent_transactions" to "Ringkasan Transaksi Terbaru",
        "view_all" to "Lihat Semua",
        "cashflow_chart" to "Grafik Pemasukan & Pengeluaran",
        "quick_add_income" to "Kas Masuk",
        "quick_add_expense" to "Kas Keluar",
        "quick_manage_initial_balance" to "+ Saldo Awal",
        "period_all" to "Semua",
        "period_today" to "Hari Ini",
        "period_this_week" to "Minggu Ini",
        "period_this_month" to "Bulan Ini",
        "period_this_year" to "Tahun Ini",
        "period_custom" to "Kustom",

        // Saldo Awal Dialog
        "dialog_manage_initial_balance_title" to "Kelola Saldo Awal Kas",
        "tab_add_initial_balance" to "Tambah Saldo",
        "tab_set_initial_balance" to "Atur Ulang Saldo",
        "current_initial_balance" to "Saldo Awal Saat Ini",
        "amount_to_add" to "Nominal Tambahan Saldo Awal",
        "amount_new_balance" to "Nominal Saldo Awal Baru",
        "new_initial_balance_preview" to "Estimasi Saldo Awal Baru",
        "quick_presets" to "Pilihan Cepat Tambah Nominal:",
        "initial_balance_note" to "Catatan / Keterangan (Opsional)",
        "initial_balance_note_placeholder" to "Contoh: Sisa kas periode lalu / modal awal",
        "btn_confirm_add_balance" to "Simpan & Tambah Saldo Awal",
        "btn_confirm_set_balance" to "Perbarui Saldo Awal",
        "badge_manage_balance" to "Atur / Tambah",

        // Forms
        "form_income_title" to "Pencatatan Kas Masuk",
        "form_income_subtitle" to "Catat penerimaan kas masuk dengan akurat dan rapi",
        "form_expense_title" to "Pencatatan Kas Keluar",
        "form_expense_subtitle" to "Catat nota dan pengeluaran kas dengan bukti terlampir",
        "field_date" to "Tanggal Transaksi",
        "field_income_source" to "Nama / Sumber Pemasukan",
        "field_expense_name" to "Nama Pengeluaran",
        "placeholder_income_title" to "Contoh: Iuran Bulanan Blok A",
        "placeholder_expense_title" to "Contoh: Pembelian Lampu Jalan",
        "field_amount" to "Jumlah Uang",
        "field_category" to "Kategori",
        "field_description" to "Keterangan (Opsional)",
        "field_payment_method" to "Metode Pembayaran",
        "field_upload_receipt" to "Upload Foto Nota / Bukti Transaksi",
        "btn_pick_photo" to "Pilih Foto Bukti / Nota",
        "btn_save" to "Simpan Transaksi",
        "btn_saving" to "Menyimpan...",
        "btn_cancel" to "Batal",
        "btn_close" to "Tutup",

        // History
        "history_title" to "Riwayat Transaksi Kas",
        "search_hint" to "Cari transaksi...",
        "btn_audit" to "Audit Log",
        "filter_type" to "Jenis",
        "filter_all" to "Semua",
        "filter_income" to "Pemasukan",
        "filter_expense" to "Pengeluaran",
        "filter_void" to "Dibatalkan",
        "all_categories" to "Semua Kategori",
        "transaction_detail" to "Detail Transaksi",
        "status_active" to "STATUS: AKTIF",
        "status_void" to "STATUS: DIBATALKAN",
        "badge_voided" to "DIBATALKAN",
        "btn_edit" to "Koreksi / Edit",
        "btn_void" to "Batalkan Transaksi",
        "edit_dialog_title" to "Koreksi / Edit Transaksi",
        "edit_reason_label" to "Alasan Koreksi (Wajib)",
        "btn_save_changes" to "Simpan Perubahan",
        "void_dialog_title" to "Batalkan Transaksi (Void)",
        "void_reason_prompt" to "Alasan Pembatalan Transaksi",
        "void_reason_hint" to "Contoh: Dobel input / salah transaksi",
        "btn_confirm_void" to "Ya, Batalkan Transaksi",
        "audit_trail_title" to "Riwayat Perubahan Data (Audit Log)",
        "audit_empty" to "Belum ada riwayat perubahan data.",
        "reason_label" to "Alasan",
        "input_time_label" to "Waktu Input",
        "no_transactions" to "Belum ada data transaksi",

        // Reports
        "report_title" to "Laporan Keuangan Kas",
        "report_subtitle" to "Pilih periode untuk membuat, mengunduh, mencetak, atau membagikan laporan.",
        "report_daily" to "Laporan Harian",
        "report_weekly" to "Laporan Mingguan",
        "report_monthly" to "Laporan Bulanan",
        "report_yearly" to "Laporan Tahunan",
        "report_custom" to "Periode Kustom",
        "btn_share_report" to "Bagikan Laporan",
        "btn_share_wa" to "Bagikan via WhatsApp (PDF + Teks)",
        "btn_share_image" to "Bagikan Gambar",
        "btn_share_image_full" to "Bagikan Gambar Laporan",
        "share_report_dialog_title" to "Pilih Format Berbagi Laporan",
        "share_report_dialog_subtitle" to "Pilih format atau cara membagikan laporan keuangan periode ini:",
        "share_wa_title" to "WhatsApp (PDF + Teks Rangkuman)",
        "share_wa_desc" to "Kirim file PDF resmi & teks rangkuman saldo kas ke chat/grup WA",
        "share_img_title" to "Gambar Infografis Laporan (.png)",
        "share_img_desc" to "Kartu visual ringkasan kas, sangat cocok untuk status WA & medsos",
        "share_pdf_file_title" to "Dokumen PDF (.pdf)",
        "share_pdf_file_desc" to "Kirim berkas PDF resmi ke aplikasi lain (Email, Telegram, Drive, dll)",
        "share_excel_title" to "File Spreadsheet Excel (.csv)",
        "share_excel_desc" to "Kirim berkas tabel data transaksi untuk pembukuan Excel / Sheets",
        "download_pdf" to "Unduh PDF",
        "export_excel" to "Unduh Excel (CSV)",
        "print_report" to "Cetak",
        "list_tx_period" to "Daftar Transaksi Periode Ini",
        "no_tx_period" to "Tidak ada transaksi pada periode yang dipilih.",
        "manager_label" to "Pengelola Kas / Admin",
        "created_at" to "Dibuat pada",
        "period_label" to "Periode",
        "excel_saved_download" to "File Excel berhasil disimpan di folder Download:",
        "excel_open_hint" to "Dapat dibuka langsung di Excel, Google Sheets, atau aplikasi file manager HP Anda.",

        // Settings
        "settings_title" to "Pengaturan",
        "btn_save_settings" to "Simpan",
        "profile_section" to "Profil Kelompok & Admin",
        "group_name" to "Nama Kelompok / Organisasi",
        "admin_name" to "Nama Admin",
        "initial_balance_label" to "Saldo Awal Kas",
        "theme_section" to "Tema Tampilan",
        "theme_light" to "Tema Terang (Light Mode)",
        "theme_dark" to "Tema Gelap (Dark Mode)",
        "theme_system" to "Ikuti Sistem Perangkat",
        "language_section" to "Bahasa Aplikasi (Multibahasa Dunia)",
        "select_language_btn" to "Pilih Bahasa",
        "currency_section" to "Mata Uang (Format Nominal)",
        "currency_desc" to "Format nominal disesuaikan dengan kode ISO 4217 tanpa mengonversi nilai asli transaksi.",
        "about_section" to "Tentang Aplikasi",
        "btn_open" to "Buka",
        "app_version" to "Versi 1.0.0",
        "developer" to "Pengembang: Rafline Dev",
        "platform" to "Platform: Android (Jetpack Compose)",
        "copyright" to "© 2026 Kas Transparan. Hak Cipta Dilindungi.",
        "app_desc" to "Aplikasi pengelolaan keuangan kas yang modern, sederhana, transparan, dan mudah digunakan untuk akuntabilitas organisasi dan komunitas.",
        "btn_sign_out" to "Keluar (Sign Out)",
        "share_tx_title" to "Bagikan Bukti Transaksi",
        "share_pdf_title" to "Dokumen PDF (.pdf)",
        "share_pdf_desc" to "Bukti kuitansi transaksi resmi format PDF",
        "share_image_title" to "Gambar Struk (.png)",
        "share_image_desc" to "Kuitansi visual untuk WhatsApp & media sosial",

        // Google Drive Backup & Restore
        "backup_drive_title" to "Cadangan Data (Google Drive)",
        "backup_drive_subtitle" to "Simpan ke Google Drive / Pulihkan data kas",
        "backup_status_none" to "Status: Belum ada cadangan",
        "backup_status_prefix" to "Status: Cadangan terakhir",
        "btn_backup_drive" to "Cadangkan ke Google Drive",
        "btn_restore_drive" to "Pulihkan dari Google Drive",
        "btn_open_drive_app" to "Buka Aplikasi Google Drive",
        "drive_app_not_found" to "Aplikasi Google Drive tidak ditemukan",

        // Device & App Info
        "about_device_app" to "Tentang Ponsel & Aplikasi",
        "release_year" to "Tahun Pembuatan: 2026",

        // Dialogs & Selectors
        "restore_success_title" to "Pemulihan Berhasil",
        "restore_success_msg" to "Berhasil memulihkan %d data transaksi dan pengaturan kas dari file cadangan Google Drive.",
        "btn_done" to "Selesai",
        "restore_fail_title" to "Gagal Memulihkan Cadangan",
        "search_language_hint" to "Cari bahasa / negara...",
        "region_all" to "Semua",

        // Download dialog
        "download_pdf_card_title" to "Unduh Dokumen PDF (.pdf)",
        "download_pdf_card_desc" to "Tersimpan di folder Download HP, catatan lengkap rapi",
        "download_xls_card_title" to "Unduh Format Excel (.xls)",
        "download_xls_card_desc" to "Tersusun rapi di kolom A-K terpisah dengan tabel dan warna",
        "download_csv_card_title" to "Unduh Format CSV (.csv)",
        "download_csv_card_desc" to "File tabel standar koma universal dengan UTF-8",
        "print_report_card_title" to "Cetak Laporan (Print)",
        "print_report_card_desc" to "Cetak langsung via printer atau simpan PDF",

        // Auth Screen
        "auth_card_desc" to "Akses Khusus Admin Kas\nSilakan masuk dengan Akun Google Admin untuk mengelola keuangan kas.",
        "btn_sign_in_admin" to "Masuk Sebagai Admin",

        // Alerts & Errors
        "error_empty_title" to "Nama/sumber transaksi wajib diisi",
        "error_invalid_amount" to "Jumlah nominal harus lebih dari 0",
        "error_empty_reason" to "Alasan wajib diisi",
        "error_edit_validation" to "Semua field dan alasan koreksi wajib diisi."
    )

    private val EN_STRINGS = mapOf(
        "nav_dashboard" to "Home",
        "nav_income" to "Income",
        "nav_expense" to "Expense",
        "nav_history" to "History",
        "nav_report" to "Reports",

        "app_title" to "KAS TRANSPARAN",
        "app_tagline" to "Modern, Accountable, and Transparent Cash Management",
        "admin_label" to "Admin",
        "current_balance" to "Current Cash Balance",
        "initial_balance" to "Opening Balance",
        "total_income" to "Income",
        "total_expense" to "Expense",
        "final_balance" to "Final Balance",
        "balance_formula" to "Final Balance = Initial Balance + Income - Expense",
        "recent_transactions" to "Recent Transactions",
        "view_all" to "View All",
        "cashflow_chart" to "Cash Flow Overview",
        "quick_add_income" to "Income",
        "quick_add_expense" to "Expense",
        "quick_manage_initial_balance" to "+ Opening Balance",
        "period_all" to "All",
        "period_today" to "Today",
        "period_this_week" to "This Week",
        "period_this_month" to "This Month",
        "period_this_year" to "This Year",
        "period_custom" to "Custom",

        "dialog_manage_initial_balance_title" to "Manage Opening Cash Balance",
        "tab_add_initial_balance" to "Add to Balance",
        "tab_set_initial_balance" to "Reset Balance",
        "current_initial_balance" to "Current Opening Balance",
        "amount_to_add" to "Amount to Add",
        "amount_new_balance" to "New Opening Balance Amount",
        "new_initial_balance_preview" to "Estimated New Opening Balance",
        "quick_presets" to "Quick Preset Additions:",
        "initial_balance_note" to "Notes / Remarks (Optional)",
        "initial_balance_note_placeholder" to "e.g., Previous period surplus / seed funds",
        "btn_confirm_add_balance" to "Save & Add to Opening Balance",
        "btn_confirm_set_balance" to "Update Opening Balance",
        "badge_manage_balance" to "Manage / Add",

        "form_income_title" to "Income Entry",
        "form_income_subtitle" to "Record cash receipts accurately",
        "form_expense_title" to "Expense Entry",
        "form_expense_subtitle" to "Record expenses with receipt proof",
        "field_date" to "Transaction Date",
        "field_income_source" to "Income Source / From",
        "field_expense_name" to "Expense Name / Payee",
        "placeholder_income_title" to "e.g., Monthly Dues Block A",
        "placeholder_expense_title" to "e.g., Street Light Purchase",
        "field_amount" to "Amount",
        "field_category" to "Category",
        "field_description" to "Notes / Remarks (Optional)",
        "field_payment_method" to "Payment Method",
        "field_upload_receipt" to "Upload Receipt Photo / Proof",
        "btn_pick_photo" to "Select Receipt Photo",
        "btn_save" to "Save Transaction",
        "btn_saving" to "Saving...",
        "btn_cancel" to "Cancel",
        "btn_close" to "Close",

        "history_title" to "Transaction History",
        "search_hint" to "Search transactions...",
        "btn_audit" to "Audit Log",
        "filter_type" to "Type",
        "filter_all" to "All",
        "filter_income" to "Income",
        "filter_expense" to "Expense",
        "filter_void" to "Voided",
        "all_categories" to "All Categories",
        "transaction_detail" to "Transaction Details",
        "status_active" to "STATUS: ACTIVE",
        "status_void" to "STATUS: VOIDED",
        "badge_voided" to "VOIDED",
        "btn_edit" to "Edit / Correct",
        "btn_void" to "Void Transaction",
        "edit_dialog_title" to "Edit / Correct Transaction",
        "edit_reason_label" to "Correction Reason (Required)",
        "btn_save_changes" to "Save Changes",
        "void_dialog_title" to "Void Transaction",
        "void_reason_prompt" to "Reason for Cancellation",
        "void_reason_hint" to "e.g., Duplicate input / incorrect entry",
        "btn_confirm_void" to "Yes, Void Transaction",
        "audit_trail_title" to "Audit Trail (Change History)",
        "audit_empty" to "No audit trail records yet.",
        "reason_label" to "Reason",
        "input_time_label" to "Recorded At",
        "no_transactions" to "No transactions found",

        "report_title" to "Financial Cash Report",
        "report_subtitle" to "Select period to generate, download, print, or share report.",
        "report_daily" to "Daily Report",
        "report_weekly" to "Weekly Report",
        "report_monthly" to "Monthly Report",
        "report_yearly" to "Annual Report",
        "report_custom" to "Custom Period",
        "btn_share_report" to "Share Report",
        "btn_share_wa" to "Share via WhatsApp (PDF + Text)",
        "btn_share_image" to "Share Image",
        "btn_share_image_full" to "Share Report as Image",
        "share_report_dialog_title" to "Select Report Share Format",
        "share_report_dialog_subtitle" to "Choose how you want to share this period's financial report:",
        "share_wa_title" to "WhatsApp (PDF + Summary Text)",
        "share_wa_desc" to "Send official PDF file & cash summary text to WhatsApp chat/group",
        "share_img_title" to "Infographic Image (.png)",
        "share_img_desc" to "Visual cash summary card, ideal for WhatsApp status & social media",
        "share_pdf_file_title" to "PDF Document (.pdf)",
        "share_pdf_file_desc" to "Send official PDF report to other apps (Email, Telegram, Drive, etc.)",
        "share_excel_title" to "Excel Spreadsheet (.csv)",
        "share_excel_desc" to "Send transaction table data for accounting in Excel / Google Sheets",
        "download_pdf" to "Download PDF",
        "export_excel" to "Download Excel",
        "print_report" to "Print",
        "list_tx_period" to "Transactions in This Period",
        "no_tx_period" to "No transactions found in selected period.",
        "manager_label" to "Cash Manager / Admin",
        "created_at" to "Created on",
        "period_label" to "Period",
        "excel_saved_download" to "Excel file successfully saved in Downloads folder:",
        "excel_open_hint" to "Can be opened directly in Excel, Google Sheets, or your phone's file manager.",

        "settings_title" to "Settings",
        "btn_save_settings" to "Save",
        "profile_section" to "Organization & Admin Profile",
        "group_name" to "Group / Organization Name",
        "admin_name" to "Admin Name",
        "initial_balance_label" to "Opening Cash Balance",
        "theme_section" to "Application Theme",
        "theme_light" to "Light Mode",
        "theme_dark" to "Dark Mode",
        "theme_system" to "Follow System",
        "language_section" to "Application Language (Global Multi-Language)",
        "select_language_btn" to "Choose Language",
        "currency_section" to "Currency (Display Format)",
        "currency_desc" to "Currency display is formatted per ISO 4217 standard without converting original numeric values.",
        "about_section" to "About Application",
        "btn_open" to "Open",
        "app_version" to "Version 1.0.0",
        "developer" to "Developer: Rafline Dev",
        "platform" to "Platform: Android (Jetpack Compose)",
        "copyright" to "© 2026 Kas Transparan. All rights reserved.",
        "app_desc" to "A modern, simple, transparent, and easy-to-use cash management application for community and organizational financial accountability.",
        "btn_sign_out" to "Sign Out",
        "share_tx_title" to "Share Transaction Receipt",
        "share_pdf_title" to "PDF Document (.pdf)",
        "share_pdf_desc" to "Official transaction receipt in PDF format",
        "share_image_title" to "Receipt Image (.png)",
        "share_image_desc" to "Visual receipt image for WhatsApp & social media",

        // Google Drive Backup & Restore
        "backup_drive_title" to "Cloud Backup (Google Drive)",
        "backup_drive_subtitle" to "Save to Google Drive / Restore cash records",
        "backup_status_none" to "Status: No backup found",
        "backup_status_prefix" to "Status: Last backup",
        "btn_backup_drive" to "Backup to Google Drive",
        "btn_restore_drive" to "Restore from Google Drive",
        "btn_open_drive_app" to "Open Google Drive App",
        "drive_app_not_found" to "Google Drive application not found",

        // Device & App Info
        "about_device_app" to "Device & App Info",
        "release_year" to "Release Year: 2026",

        // Dialogs & Selectors
        "restore_success_title" to "Restore Successful",
        "restore_success_msg" to "Successfully restored %d transactions and settings from Google Drive backup.",
        "btn_done" to "Done",
        "restore_fail_title" to "Restore Failed",
        "search_language_hint" to "Search language / country...",
        "region_all" to "All",

        // Download dialog
        "download_pdf_card_title" to "Download PDF Document (.pdf)",
        "download_pdf_card_desc" to "Saved in Downloads folder, complete clean records",
        "download_xls_card_title" to "Download Excel Spreadsheet (.xls)",
        "download_xls_card_desc" to "Neatly arranged columns A-K with formatted tables",
        "download_csv_card_title" to "Download CSV File (.csv)",
        "download_csv_card_desc" to "Universal comma-separated format with UTF-8",
        "print_report_card_title" to "Print Report",
        "print_report_card_desc" to "Print directly to printer or save as system PDF",

        "auth_card_desc" to "Admin Cash Access Only\nPlease sign in with your Admin Google Account to manage the cashbook.",
        "btn_sign_in_admin" to "Sign in as Admin",

        "error_empty_title" to "Transaction title is required",
        "error_invalid_amount" to "Amount must be greater than 0",
        "error_empty_reason" to "Reason is required",
        "error_edit_validation" to "All fields and correction reason are required."
    )

    // Regional languages definitions
    private val REGIONAL_TRANSLATIONS: Map<String, Map<String, String>> = mapOf(
        // Asia Tenggara: Melayu (ms)
        "ms" to mapOf(
            "nav_dashboard" to "Utama", "nav_income" to "Masuk", "nav_expense" to "Keluar",
            "nav_history" to "Sejarah", "nav_report" to "Laporan",
            "current_balance" to "Baki Tunai Semasa", "initial_balance" to "Baki Permulaan",
            "total_income" to "Jumlah Masuk", "total_expense" to "Jumlah Keluar", "final_balance" to "Baki Akhir",
            "quick_add_income" to "Wang Masuk", "quick_add_expense" to "Wang Keluar", "quick_manage_initial_balance" to "+ Baki Awal",
            "dialog_manage_initial_balance_title" to "Urus Baki Awal Kas", "tab_add_initial_balance" to "Tambah Baki",
            "tab_set_initial_balance" to "Tetap Semula Baki", "btn_confirm_add_balance" to "Simpan & Tambah Baki Awal",
            "settings_title" to "Tetapan", "btn_save_settings" to "Simpan", "theme_light" to "Tema Cerah (Light Mode)"
        ),
        // Asia Tenggara: Thai (th)
        "th" to mapOf(
            "nav_dashboard" to "หน้าหลัก", "nav_income" to "รายรับ", "nav_expense" to "รายจ่าย",
            "nav_history" to "ประวัติ", "nav_report" to "รายงาน",
            "current_balance" to "ยอดเงินสดคงเหลือ", "initial_balance" to "ยอดยกมา",
            "total_income" to "รายรับรวม", "total_expense" to "รายจ่ายรวม", "final_balance" to "ยอดสุทธิ",
            "quick_add_income" to "รับเงิน", "quick_add_expense" to "จ่ายเงิน", "quick_manage_initial_balance" to "+ เพิ่มยอดยกมา",
            "dialog_manage_initial_balance_title" to "จัดการยอดยกมาเริ่มต้น", "tab_add_initial_balance" to "เพิ่มยอด",
            "tab_set_initial_balance" to "กำหนดยอดใหม่", "btn_confirm_add_balance" to "บันทึกและเพิ่มยอดยกมา",
            "settings_title" to "การตั้งค่า", "btn_save_settings" to "บันทึก", "theme_light" to "โหมดสว่าง"
        ),
        // Asia Tenggara: Vietnamese (vi)
        "vi" to mapOf(
            "nav_dashboard" to "Trang chủ", "nav_income" to "Thu", "nav_expense" to "Chi",
            "nav_history" to "Lịch sử", "nav_report" to "Báo cáo",
            "current_balance" to "Số dư tiền mặt hiện tại", "initial_balance" to "Số dư đầu kỳ",
            "total_income" to "Tổng thu", "total_expense" to "Tổng chi", "final_balance" to "Số dư cuối kỳ",
            "quick_add_income" to "Thu tiền", "quick_add_expense" to "Chi tiền", "quick_manage_initial_balance" to "+ Thêm số dư đầu",
            "dialog_manage_initial_balance_title" to "Quản lý số dư đầu kỳ", "tab_add_initial_balance" to "Thêm vào số dư",
            "tab_set_initial_balance" to "Đặt lại số dư", "btn_confirm_add_balance" to "Lưu & Thêm vào số dư",
            "settings_title" to "Cài đặt", "btn_save_settings" to "Lưu", "theme_light" to "Giao diện sáng"
        ),
        // Asia Tenggara: Tagalog / Filipino (fil)
        "fil" to mapOf(
            "nav_dashboard" to "Home", "nav_income" to "Kita", "nav_expense" to "Gastos",
            "nav_history" to "Kasaysayan", "nav_report" to "Ulat",
            "current_balance" to "Kasalukuyang Balanse ng Pondo", "initial_balance" to "Simulang Balanse",
            "total_income" to "Kabuuang Kita", "total_expense" to "Kabuuang Gastos", "final_balance" to "Huling Balanse",
            "quick_add_income" to "Magpasok ng Pera", "quick_add_expense" to "Maglabas ng Pera", "quick_manage_initial_balance" to "+ Simulang Balanse",
            "dialog_manage_initial_balance_title" to "Ayusin ang Simulang Balanse", "tab_add_initial_balance" to "Magdagdag sa Balanse",
            "tab_set_initial_balance" to "I-reset ang Balanse", "btn_confirm_add_balance" to "I-save at Idagdag",
            "settings_title" to "Mga Setting", "btn_save_settings" to "I-save", "theme_light" to "Maliwanag na Tema"
        ),

        // Asia: Japanese (ja)
        "ja" to mapOf(
            "nav_dashboard" to "ホーム", "nav_income" to "入金", "nav_expense" to "出金",
            "nav_history" to "履歴", "nav_report" to "レポート",
            "current_balance" to "現在の残高", "initial_balance" to "期首残高 (初期資金)",
            "total_income" to "総収入", "total_expense" to "総支出", "final_balance" to "最終残高",
            "quick_add_income" to "入金記録", "quick_add_expense" to "出金記録", "quick_manage_initial_balance" to "+ 期首残高の追加",
            "dialog_manage_initial_balance_title" to "期首残高の管理・追加", "tab_add_initial_balance" to "残高に追加",
            "tab_set_initial_balance" to "残高を再設定", "btn_confirm_add_balance" to "期首残高を保存して追加",
            "settings_title" to "設定", "btn_save_settings" to "保存", "theme_light" to "ライトモード (明るいテーマ)"
        ),
        // Asia: Korean (ko)
        "ko" to mapOf(
            "nav_dashboard" to "홈", "nav_income" to "입금", "nav_expense" to "출금",
            "nav_history" to "내역", "nav_report" to "보고서",
            "current_balance" to "현재 현금 잔액", "initial_balance" to "기초 잔액 (초기 자금)",
            "total_income" to "총 수입", "total_expense" to "총 지출", "final_balance" to "최종 잔액",
            "quick_add_income" to "수입 등록", "quick_add_expense" to "지출 등록", "quick_manage_initial_balance" to "+ 기초 잔액 추가",
            "dialog_manage_initial_balance_title" to "기초 잔액 관리 및 추가", "tab_add_initial_balance" to "잔액에 추가",
            "tab_set_initial_balance" to "잔액 재설정", "btn_confirm_add_balance" to "기초 잔액 저장 및 추가",
            "settings_title" to "설정", "btn_save_settings" to "저장", "theme_light" to "라이트 모드"
        ),
        // Asia: Chinese (zh)
        "zh" to mapOf(
            "nav_dashboard" to "首页", "nav_income" to "收入", "nav_expense" to "支出",
            "nav_history" to "明细", "nav_report" to "报表",
            "current_balance" to "当前现金余额", "initial_balance" to "期初余额",
            "total_income" to "总收入", "total_expense" to "总支出", "final_balance" to "期末余额",
            "quick_add_income" to "记收入", "quick_add_expense" to "记支出", "quick_manage_initial_balance" to "+ 增加期初余额",
            "dialog_manage_initial_balance_title" to "管理期初余额", "tab_add_initial_balance" to "追加余额",
            "tab_set_initial_balance" to "重设余额", "btn_confirm_add_balance" to "保存并追加期初余额",
            "settings_title" to "设置", "btn_save_settings" to "保存", "theme_light" to "浅色模式 (明亮主题)"
        ),
        // Asia: Hindi (hi)
        "hi" to mapOf(
            "nav_dashboard" to "होम", "nav_income" to "आय", "nav_expense" to "व्यय",
            "nav_history" to "इतिहास", "nav_report" to "रिपोर्ट",
            "current_balance" to "वर्तमान नकद शेष", "initial_balance" to "प्रारंभिक शेष (शुरुआती राशि)",
            "total_income" to "कुल आय", "total_expense" to "कुल व्यय", "final_balance" to "अंतिम शेष",
            "quick_add_income" to "आय जोड़ें", "quick_add_expense" to "खर्च जोड़ें", "quick_manage_initial_balance" to "+ प्रारंभिक शेष जोड़ें",
            "dialog_manage_initial_balance_title" to "प्रारंभिक शेष प्रबंधित करें", "tab_add_initial_balance" to "शेष में जोड़ें",
            "tab_set_initial_balance" to "शेष रीसेट करें", "btn_confirm_add_balance" to "सहेजें और जोड़ें",
            "settings_title" to "सेटिंग्स", "btn_save_settings" to "सहेजें", "theme_light" to "लाइट मोड (उज्ज्वल)"
        ),
        // Asia: Arabic (ar)
        "ar" to mapOf(
            "nav_dashboard" to "الرئيسية", "nav_income" to "الوارد", "nav_expense" to "المنصرف",
            "nav_history" to "السجل", "nav_report" to "التقارير",
            "current_balance" to "الرصيد النقدي الحالي", "initial_balance" to "الرصيد الافتتاحي",
            "total_income" to "إجمالي الإيرادات", "total_expense" to "إجمالي المصروفات", "final_balance" to "الرصيد النهائي",
            "quick_add_income" to "إضافة دخل", "quick_add_expense" to "إضافة منصرف", "quick_manage_initial_balance" to "+ الرصيد الافتتاحي",
            "dialog_manage_initial_balance_title" to "إدارة الرصيد الافتتاحي", "tab_add_initial_balance" to "إضافة إلى الرصيد",
            "tab_set_initial_balance" to "إعادة ضبط الرصيد", "btn_confirm_add_balance" to "حفظ وإضافة الرصيد",
            "settings_title" to "الإعدادات", "btn_save_settings" to "حفظ", "theme_light" to "الوضع الفاتح"
        ),
        // Asia: Turkish (tr)
        "tr" to mapOf(
            "nav_dashboard" to "Ana Sayfa", "nav_income" to "Gelir", "nav_expense" to "Gider",
            "nav_history" to "Geçmiş", "nav_report" to "Raporlar",
            "current_balance" to "Mevcut Kasa Bakiyesi", "initial_balance" to "Açılış Bakiyesi",
            "total_income" to "Toplam Gelir", "total_expense" to "Toplam Gider", "final_balance" to "Kapanış Bakiyesi",
            "quick_add_income" to "Gelir Ekle", "quick_add_expense" to "Gider Ekle", "quick_manage_initial_balance" to "+ Açılış Bakiyesi",
            "dialog_manage_initial_balance_title" to "Açılış Bakiyesini Yönet", "tab_add_initial_balance" to "Bakiyeye Ekle",
            "tab_set_initial_balance" to "Bakiyeyi Sıfırla", "btn_confirm_add_balance" to "Kaydet ve Ekle",
            "settings_title" to "Ayarlar", "btn_save_settings" to "Kaydet", "theme_light" to "Açık Tema"
        ),

        // Eropa: German (de)
        "de" to mapOf(
            "nav_dashboard" to "Übersicht", "nav_income" to "Einnahmen", "nav_expense" to "Ausgaben",
            "nav_history" to "Verlauf", "nav_report" to "Berichte",
            "current_balance" to "Aktueller Kassenbestand", "initial_balance" to "Anfangsbestand",
            "total_income" to "Gesamteinnahmen", "total_expense" to "Gesamtausgaben", "final_balance" to "Endbestand",
            "quick_add_income" to "Einnahme buchen", "quick_add_expense" to "Ausgabe buchen", "quick_manage_initial_balance" to "+ Anfangsbestand",
            "dialog_manage_initial_balance_title" to "Anfangsbestand verwalten", "tab_add_initial_balance" to "Betrag hinzufügen",
            "tab_set_initial_balance" to "Bestand neu festlegen", "btn_confirm_add_balance" to "Speichern & Hinzufügen",
            "settings_title" to "Einstellungen", "btn_save_settings" to "Speichern", "theme_light" to "Helles Design (Light Mode)"
        ),
        // Eropa: French (fr)
        "fr" to mapOf(
            "nav_dashboard" to "Accueil", "nav_income" to "Entrées", "nav_expense" to "Sorties",
            "nav_history" to "Historique", "nav_report" to "Rapports",
            "current_balance" to "Solde Actuel de Caisse", "initial_balance" to "Solde Initial",
            "total_income" to "Total Entrées", "total_expense" to "Total Dépenses", "final_balance" to "Solde Final",
            "quick_add_income" to "Ajouter Recette", "quick_add_expense" to "Ajouter Dépense", "quick_manage_initial_balance" to "+ Solde Initial",
            "dialog_manage_initial_balance_title" to "Gérer le Solde Initial", "tab_add_initial_balance" to "Ajouter au Solde",
            "tab_set_initial_balance" to "Réinitialiser Solde", "btn_confirm_add_balance" to "Enregistrer et Ajouter",
            "settings_title" to "Paramètres", "btn_save_settings" to "Enregistrer", "theme_light" to "Thème Clair"
        ),
        // Eropa: Spanish (es)
        "es" to mapOf(
            "nav_dashboard" to "Inicio", "nav_income" to "Ingresos", "nav_expense" to "Gastos",
            "nav_history" to "Historial", "nav_report" to "Informes",
            "current_balance" to "Saldo Actual de Caja", "initial_balance" to "Saldo Inicial",
            "total_income" to "Total Ingresos", "total_expense" to "Total Gastos", "final_balance" to "Saldo Final",
            "quick_add_income" to "Registrar Ingreso", "quick_add_expense" to "Registrar Gasto", "quick_manage_initial_balance" to "+ Saldo Inicial",
            "dialog_manage_initial_balance_title" to "Administrar Saldo Inicial", "tab_add_initial_balance" to "Añadir al Saldo",
            "tab_set_initial_balance" to "Restablecer Saldo", "btn_confirm_add_balance" to "Guardar y Añadir",
            "settings_title" to "Ajustes", "btn_save_settings" to "Guardar", "theme_light" to "Modo Claro"
        ),
        // Eropa: Italian (it)
        "it" to mapOf(
            "nav_dashboard" to "Home", "nav_income" to "Entrate", "nav_expense" to "Uscite",
            "nav_history" to "Cronologia", "nav_report" to "Report",
            "current_balance" to "Saldo Cassa Attuale", "initial_balance" to "Saldo Iniziale",
            "total_income" to "Totale Entrate", "total_expense" to "Totale Uscite", "final_balance" to "Saldo Finale",
            "quick_add_income" to "Nuova Entrata", "quick_add_expense" to "Nuova Spesa", "quick_manage_initial_balance" to "+ Saldo Iniziale",
            "dialog_manage_initial_balance_title" to "Gestisci Saldo Iniziale", "tab_add_initial_balance" to "Aggiungi al Saldo",
            "tab_set_initial_balance" to "Reimposta Saldo", "btn_confirm_add_balance" to "Salva e Aggiungi",
            "settings_title" to "Impostazioni", "btn_save_settings" to "Salva", "theme_light" to "Tema Chiaro"
        ),
        // Eropa: Dutch (nl)
        "nl" to mapOf(
            "nav_dashboard" to "Overzicht", "nav_income" to "Inkomsten", "nav_expense" to "Uitgaven",
            "nav_history" to "Geschiedenis", "nav_report" to "Rapporten",
            "current_balance" to "Huidig Kassaldo", "initial_balance" to "Beginsaldo",
            "total_income" to "Totale Inkomsten", "total_expense" to "Totale Uitgaven", "final_balance" to "Eindsaldo",
            "quick_add_income" to "Inkomst Boeken", "quick_add_expense" to "Uitgave Boeken", "quick_manage_initial_balance" to "+ Beginsaldo",
            "dialog_manage_initial_balance_title" to "Beginsaldo Beheren", "tab_add_initial_balance" to "Bedrag Toevoegen",
            "tab_set_initial_balance" to "Saldo Aanpassen", "btn_confirm_add_balance" to "Opslaan en Toevoegen",
            "settings_title" to "Instellingen", "btn_save_settings" to "Opslaan", "theme_light" to "Licht Thema"
        ),
        // Eropa: Russian (ru)
        "ru" to mapOf(
            "nav_dashboard" to "Главная", "nav_income" to "Приход", "nav_expense" to "Расход",
            "nav_history" to "История", "nav_report" to "Отчёты",
            "current_balance" to "Текущий остаток кассы", "initial_balance" to "Начальный остаток",
            "total_income" to "Всего приход", "total_expense" to "Всего расход", "final_balance" to "Итоговый остаток",
            "quick_add_income" to "Внести приход", "quick_add_expense" to "Внести расход", "quick_manage_initial_balance" to "+ Начальный остаток",
            "dialog_manage_initial_balance_title" to "Управление начальным остатком", "tab_add_initial_balance" to "Добавить к балансу",
            "tab_set_initial_balance" to "Задать баланс заново", "btn_confirm_add_balance" to "Сохранить и добавить",
            "settings_title" to "Настройки", "btn_save_settings" to "Сохранить", "theme_light" to "Светлая тема"
        ),

        // Afrika: Swahili (sw)
        "sw" to mapOf(
            "nav_dashboard" to "Nyumbani", "nav_income" to "Mapato", "nav_expense" to "Matumizi",
            "nav_history" to "Kumbukumbu", "nav_report" to "Ripoti",
            "current_balance" to "Salio la Fedha Taslimu", "initial_balance" to "Salio la Kuanzia",
            "total_income" to "Jumla ya Mapato", "total_expense" to "Jumla ya Matumizi", "final_balance" to "Salio la Mwisho",
            "quick_add_income" to "Rekodi Mapato", "quick_add_expense" to "Rekodi Matumizi", "quick_manage_initial_balance" to "+ Salio la Kuanzia",
            "dialog_manage_initial_balance_title" to "Simamia Salio la Kuanzia", "tab_add_initial_balance" to "Ongeza Kwenye Salio",
            "tab_set_initial_balance" to "Weka Upya Salio", "btn_confirm_add_balance" to "Hifadhi na Ongeza",
            "settings_title" to "Mipangilio", "btn_save_settings" to "Hifadhi", "theme_light" to "Mandhari Meupe"
        ),
        // Afrika: Afrikaans (af)
        "af" to mapOf(
            "nav_dashboard" to "Tuis", "nav_income" to "Inkomste", "nav_expense" to "Uitgawes",
            "nav_history" to "Geskiedenis", "nav_report" to "Verslae",
            "current_balance" to "Huidige Kontantsaldo", "initial_balance" to "Beginsaldo",
            "total_income" to "Totale Inkomste", "total_expense" to "Totale Uitgawes", "final_balance" to "Eindsaldo",
            "quick_add_income" to "Voeg Inkomste By", "quick_add_expense" to "Voeg Uitgawe By", "quick_manage_initial_balance" to "+ Beginsaldo",
            "dialog_manage_initial_balance_title" to "Bestuur Beginsaldo", "tab_add_initial_balance" to "Voeg By Saldo",
            "tab_set_initial_balance" to "Stel Saldo Terug", "btn_confirm_add_balance" to "Stoor en Voeg By",
            "settings_title" to "Instellings", "btn_save_settings" to "Stoor", "theme_light" to "Ligte Tema"
        ),
        // Afrika: Hausa (ha)
        "ha" to mapOf(
            "nav_dashboard" to "Gida", "nav_income" to "Kudin Shiga", "nav_expense" to "Kudin Fita",
            "nav_history" to "Tarihi", "nav_report" to "Rahotanni",
            "current_balance" to "Kudin Da Ke Akwai", "initial_balance" to "Kudin Farko",
            "total_income" to "Jimlar Shiga", "total_expense" to "Jimlar Fita", "final_balance" to "Ragowar Kudi",
            "quick_add_income" to "Shigar da Kudi", "quick_add_expense" to "Biyan Kudi", "quick_manage_initial_balance" to "+ Kudin Farko",
            "dialog_manage_initial_balance_title" to "Sarrafa Kudin Farko", "tab_add_initial_balance" to "Kara a Salo",
            "tab_set_initial_balance" to "Sake Tsara Kudin", "btn_confirm_add_balance" to "Ajiye da Kara",
            "settings_title" to "Saituna", "btn_save_settings" to "Ajiye", "theme_light" to "Haske"
        ),
        // Afrika: Yoruba (yo)
        "yo" to mapOf(
            "nav_dashboard" to "Ile", "nav_income" to "Wọle", "nav_expense" to "Inawo",
            "nav_history" to "Itan", "nav_report" to "Ijabọ",
            "current_balance" to "Owo Owo Lọwọlọwọ", "initial_balance" to "Owo Ibẹrẹ",
            "total_income" to "Gbogbo Owo Wọle", "total_expense" to "Gbogbo Inawo", "final_balance" to "Owo To Ku",
            "quick_add_income" to "Fi Owo Wọle Kun", "quick_add_expense" to "Fi Inawo Kun", "quick_manage_initial_balance" to "+ Owo Ibẹrẹ",
            "dialog_manage_initial_balance_title" to "Ṣakoso Owo Ibẹrẹ", "tab_add_initial_balance" to "Ṣafikun Owo",
            "tab_set_initial_balance" to "Tun Ṣeto Owo", "btn_confirm_add_balance" to "Fipamọ & Fi Kun",
            "settings_title" to "Ètò", "btn_save_settings" to "Fipamọ", "theme_light" to "Ipo Imọlẹ"
        ),
        // Afrika: Amharic (am)
        "am" to mapOf(
            "nav_dashboard" to "ዋና ገጽ", "nav_income" to "ገቢ", "nav_expense" to "ወጪ",
            "nav_history" to "ታሪክ", "nav_report" to "ሪፖርቶች",
            "current_balance" to "የአሁኑ የገንዘብ ሚዛን", "initial_balance" to "የመነሻ ሂሳብ",
            "total_income" to "ጠቅላላ ገቢ", "total_expense" to "ጠቅላላ ወጪ", "final_balance" to "የመጨረሻ ሂሳብ",
            "quick_add_income" to "ገቢ መዝግብ", "quick_add_expense" to "ወጪ መዝግብ", "quick_manage_initial_balance" to "+ የመነሻ ሂሳብ ጨምር",
            "dialog_manage_initial_balance_title" to "የመነሻ ሂሳብ አስተዳድር", "tab_add_initial_balance" to "ወደ ሂሳብ ጨምር",
            "tab_set_initial_balance" to "ሂሳብ እንደገና አዘጋጅ", "btn_confirm_add_balance" to "አስቀምጥ እና ጨምር",
            "settings_title" to "ቅንብሮች", "btn_save_settings" to "አስቀምጥ", "theme_light" to "ብሩህ ገጽታ"
        ),

        // Amerika: US English (en_us)
        "en_us" to EN_STRINGS,
        // Amerika: Portuguese Brazil (pt_br)
        "pt_br" to mapOf(
            "nav_dashboard" to "Início", "nav_income" to "Entradas", "nav_expense" to "Saídas",
            "nav_history" to "Histórico", "nav_report" to "Relatórios",
            "current_balance" to "Saldo Atual em Caixa", "initial_balance" to "Saldo Inicial",
            "total_income" to "Total de Entradas", "total_expense" to "Total de Saídas", "final_balance" to "Saldo Final",
            "quick_add_income" to "Registrar Entrada", "quick_add_expense" to "Registrar Saída", "quick_manage_initial_balance" to "+ Saldo Inicial",
            "dialog_manage_initial_balance_title" to "Gerenciar Saldo Inicial", "tab_add_initial_balance" to "Adicionar ao Saldo",
            "tab_set_initial_balance" to "Redefinir Saldo", "btn_confirm_add_balance" to "Salvar e Adicionar",
            "settings_title" to "Configurações", "btn_save_settings" to "Salvar", "theme_light" to "Modo Claro"
        ),
        // Amerika: Spanish Mexico / LatAm (es_mx)
        "es_mx" to mapOf(
            "nav_dashboard" to "Inicio", "nav_income" to "Ingresos", "nav_expense" to "Egresos",
            "nav_history" to "Historial", "nav_report" to "Reportes",
            "current_balance" to "Saldo Actual en Caja", "initial_balance" to "Saldo Inicial",
            "total_income" to "Ingresos Totales", "total_expense" to "Gastos Totales", "final_balance" to "Saldo Final",
            "quick_add_income" to "Anotar Ingreso", "quick_add_expense" to "Anotar Gasto", "quick_manage_initial_balance" to "+ Saldo Inicial",
            "dialog_manage_initial_balance_title" to "Administrar Saldo Inicial", "tab_add_initial_balance" to "Sumar al Saldo",
            "tab_set_initial_balance" to "Reajustar Saldo", "btn_confirm_add_balance" to "Guardar y Sumar",
            "settings_title" to "Configuración", "btn_save_settings" to "Guardar", "theme_light" to "Modo Claro"
        )
    )

    fun getString(key: String, lang: String = "id"): String {
        val lowerCode = lang.lowercase().trim()

        if (lowerCode == "id") {
            return ID_STRINGS[key] ?: EN_STRINGS[key] ?: key
        }
        if (lowerCode == "en") {
            return EN_STRINGS[key] ?: ID_STRINGS[key] ?: key
        }

        val regionalMap = REGIONAL_TRANSLATIONS[lowerCode]
        if (regionalMap != null) {
            val translated = regionalMap[key]
            if (translated != null) return translated
        }

        // Fallback chain: English -> Indonesian -> key
        return EN_STRINGS[key] ?: ID_STRINGS[key] ?: key
    }
}
