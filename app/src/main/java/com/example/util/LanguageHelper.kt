package com.example.util

object LanguageHelper {

    fun getString(key: String, lang: String = "id"): String {
        val isId = lang.equals("id", ignoreCase = true)
        return when (key) {
            // Navigation (concise labels to avoid awkward line wrapping)
            "nav_dashboard" -> if (isId) "Beranda" else "Home"
            "nav_income" -> if (isId) "Masuk" else "Income"
            "nav_expense" -> if (isId) "Keluar" else "Expense"
            "nav_history" -> if (isId) "Riwayat" else "History"
            "nav_report" -> if (isId) "Laporan" else "Reports"

            // Dashboard
            "app_title" -> "KAS TRANSPARAN"
            "app_tagline" -> if (isId) "Aplikasi Pengelolaan Kas Modern, Akuntabel, dan Transparan" else "Modern, Accountable, and Transparent Cash Management"
            "admin_label" -> if (isId) "Admin" else "Admin"
            "current_balance" -> if (isId) "Saldo Kas Saat Ini" else "Current Cash Balance"
            "initial_balance" -> if (isId) "Saldo Awal" else "Opening Balance"
            "total_income" -> if (isId) "Pemasukan" else "Income"
            "total_expense" -> if (isId) "Pengeluaran" else "Expense"
            "final_balance" -> if (isId) "Saldo Akhir" else "Final Balance"
            "balance_formula" -> if (isId) "Saldo Akhir = Saldo Awal + Total Masuk - Total Keluar" else "Final Balance = Initial + Total In - Total Out"
            "recent_transactions" -> if (isId) "Ringkasan Transaksi Terbaru" else "Recent Transactions"
            "view_all" -> if (isId) "Lihat Semua" else "View All"
            "cashflow_chart" -> if (isId) "Grafik Pemasukan & Pengeluaran" else "Cash Flow Overview"
            "quick_add_income" -> if (isId) "Kas Masuk" else "Income"
            "quick_add_expense" -> if (isId) "Kas Keluar" else "Expense"
            "period_all" -> if (isId) "Semua" else "All"
            "period_today" -> if (isId) "Hari Ini" else "Today"
            "period_this_week" -> if (isId) "Minggu Ini" else "This Week"
            "period_this_month" -> if (isId) "Bulan Ini" else "This Month"
            "period_this_year" -> if (isId) "Tahun Ini" else "This Year"
            "period_custom" -> if (isId) "Kustom" else "Custom"

            // Forms
            "form_income_title" -> if (isId) "Pencatatan Kas Masuk" else "Income Entry"
            "form_income_subtitle" -> if (isId) "Catat penerimaan kas masuk dengan akurat dan rapi" else "Record cash receipts accurately"
            "form_expense_title" -> if (isId) "Pencatatan Kas Keluar" else "Expense Entry"
            "form_expense_subtitle" -> if (isId) "Catat nota dan pengeluaran kas dengan bukti terlampir" else "Record expenses with receipt proof"
            "field_date" -> if (isId) "Tanggal Transaksi" else "Transaction Date"
            "field_income_source" -> if (isId) "Nama / Sumber Pemasukan" else "Income Source / From"
            "field_expense_name" -> if (isId) "Nama Pengeluaran" else "Expense Name / Payee"
            "placeholder_income_title" -> if (isId) "Contoh: Iuran Bulanan Blok A" else "e.g., Monthly Dues Block A"
            "placeholder_expense_title" -> if (isId) "Contoh: Pembelian Lampu Jalan" else "e.g., Street Light Purchase"
            "field_amount" -> if (isId) "Jumlah Uang" else "Amount"
            "field_category" -> if (isId) "Kategori" else "Category"
            "field_description" -> if (isId) "Keterangan (Opsional)" else "Notes / Remarks (Optional)"
            "field_payment_method" -> if (isId) "Metode Pembayaran" else "Payment Method"
            "field_upload_receipt" -> if (isId) "Upload Foto Nota / Bukti Transaksi" else "Upload Receipt Photo / Proof"
            "btn_pick_photo" -> if (isId) "Pilih Foto Bukti / Nota" else "Select Receipt Photo"
            "btn_save" -> if (isId) "Simpan Transaksi" else "Save Transaction"
            "btn_saving" -> if (isId) "Menyimpan..." else "Saving..."
            "btn_cancel" -> if (isId) "Batal" else "Cancel"
            "btn_close" -> if (isId) "Tutup" else "Close"

            // History
            "history_title" -> if (isId) "Riwayat Transaksi Kas" else "Transaction History"
            "search_hint" -> if (isId) "Cari transaksi..." else "Search transactions..."
            "btn_audit" -> if (isId) "Audit Log" else "Audit Log"
            "filter_type" -> if (isId) "Jenis" else "Type"
            "filter_all" -> if (isId) "Semua" else "All"
            "filter_income" -> if (isId) "Pemasukan" else "Income"
            "filter_expense" -> if (isId) "Pengeluaran" else "Expense"
            "filter_void" -> if (isId) "Dibatalkan" else "Voided"
            "all_categories" -> if (isId) "Semua Kategori" else "All Categories"
            "transaction_detail" -> if (isId) "Detail Transaksi" else "Transaction Details"
            "status_active" -> if (isId) "STATUS: AKTIF" else "STATUS: ACTIVE"
            "status_void" -> if (isId) "STATUS: DIBATALKAN" else "STATUS: VOIDED"
            "badge_voided" -> if (isId) "DIBATALKAN" else "VOIDED"
            "btn_edit" -> if (isId) "Koreksi / Edit" else "Edit / Correct"
            "btn_void" -> if (isId) "Batalkan Transaksi" else "Void Transaction"
            "edit_dialog_title" -> if (isId) "Koreksi / Edit Transaksi" else "Edit / Correct Transaction"
            "edit_reason_label" -> if (isId) "Alasan Koreksi (Wajib)" else "Correction Reason (Required)"
            "btn_save_changes" -> if (isId) "Simpan Perubahan" else "Save Changes"
            "void_dialog_title" -> if (isId) "Batalkan Transaksi (Void)" else "Void Transaction"
            "void_reason_prompt" -> if (isId) "Alasan Pembatalan Transaksi" else "Reason for Cancellation"
            "void_reason_hint" -> if (isId) "Contoh: Dobel input / salah transaksi" else "e.g., Duplicate input / incorrect entry"
            "btn_confirm_void" -> if (isId) "Ya, Batalkan Transaksi" else "Yes, Void Transaction"
            "audit_trail_title" -> if (isId) "Riwayat Perubahan Data (Audit Log)" else "Audit Trail (Change History)"
            "audit_empty" -> if (isId) "Belum ada riwayat perubahan data." else "No audit trail records yet."
            "reason_label" -> if (isId) "Alasan" else "Reason"
            "input_time_label" -> if (isId) "Waktu Input" else "Recorded At"
            "no_transactions" -> if (isId) "Belum ada data transaksi" else "No transactions found"

            // Reports
            "report_title" -> if (isId) "Laporan Keuangan Kas" else "Financial Cash Report"
            "report_subtitle" -> if (isId) "Pilih periode untuk membuat, mengunduh, mencetak, atau membagikan laporan." else "Select period to generate, download, print, or share report."
            "report_daily" -> if (isId) "Laporan Harian" else "Daily Report"
            "report_weekly" -> if (isId) "Laporan Mingguan" else "Weekly Report"
            "report_monthly" -> if (isId) "Laporan Bulanan" else "Monthly Report"
            "report_yearly" -> if (isId) "Laporan Tahunan" else "Annual Report"
            "report_custom" -> if (isId) "Periode Kustom" else "Custom Period"
            "btn_share_wa" -> if (isId) "Bagikan via WhatsApp (PDF + Teks)" else "Share via WhatsApp (PDF + Text)"
            "btn_share_image" -> if (isId) "Bagikan Gambar" else "Share Image"
            "btn_share_image_full" -> if (isId) "Bagikan Gambar Laporan" else "Share Report as Image"
            "download_pdf" -> if (isId) "Unduh PDF" else "Download PDF"
            "export_excel" -> if (isId) "Unduh Excel (CSV)" else "Download Excel"
            "print_report" -> if (isId) "Cetak" else "Print"
            "list_tx_period" -> if (isId) "Daftar Transaksi Periode Ini" else "Transactions in This Period"
            "no_tx_period" -> if (isId) "Tidak ada transaksi pada periode yang dipilih." else "No transactions found in selected period."
            "manager_label" -> if (isId) "Pengelola Kas / Admin" else "Cash Manager / Admin"
            "created_at" -> if (isId) "Dibuat pada" else "Created on"
            "period_label" -> if (isId) "Periode" else "Period"
            "excel_saved_download" -> if (isId) "File Excel berhasil disimpan di folder Download:" else "Excel file successfully saved in Downloads folder:"
            "excel_open_hint" -> if (isId) "Dapat dibuka langsung di Excel, Google Sheets, atau aplikasi file manager HP Anda." else "Can be opened directly in Excel, Google Sheets, or your phone's file manager."

            // Settings
            "settings_title" -> if (isId) "Pengaturan" else "Settings"
            "btn_save_settings" -> if (isId) "Simpan" else "Save"
            "profile_section" -> if (isId) "Profil Kelompok & Admin" else "Organization & Admin Profile"
            "group_name" -> if (isId) "Nama Kelompok / Organisasi" else "Group / Organization Name"
            "admin_name" -> if (isId) "Nama Admin" else "Admin Name"
            "initial_balance_label" -> if (isId) "Saldo Awal Kas" else "Opening Cash Balance"
            "theme_section" -> if (isId) "Tema Tampilan" else "Application Theme"
            "theme_light" -> if (isId) "Tema Terang (Light Mode)" else "Light Mode"
            "theme_dark" -> if (isId) "Tema Gelap (Dark Mode)" else "Dark Mode"
            "theme_system" -> if (isId) "Ikuti Sistem Perangkat" else "Follow System"
            "language_section" -> if (isId) "Bahasa (Language)" else "Language"
            "currency_section" -> if (isId) "Mata Uang (Format Nominal)" else "Currency (Display Format)"
            "currency_desc" -> if (isId) "Format nominal disesuaikan dengan kode ISO 4217 tanpa mengonversi nilai asli transaksi." else "Currency display is formatted per ISO 4217 standard without converting original numeric values."
            "about_section" -> if (isId) "Tentang Aplikasi" else "About Application"
            "btn_open" -> if (isId) "Buka" else "Open"
            "app_version" -> if (isId) "Versi 1.0.0" else "Version 1.0.0"
            "developer" -> if (isId) "Pengembang: Kas Transparan Team" else "Developer: Kas Transparan Team"
            "platform" -> if (isId) "Platform: Android (Jetpack Compose)" else "Platform: Android (Jetpack Compose)"
            "copyright" -> if (isId) "© 2026 Kas Transparan. Hak Cipta Dilindungi." else "© 2026 Kas Transparan. All rights reserved."
            "app_desc" -> if (isId)
                "Aplikasi pengelolaan keuangan kas yang modern, sederhana, transparan, dan mudah digunakan untuk akuntabilitas organisasi dan komunitas."
            else
                "A modern, simple, transparent, and easy-to-use cash management application for community and organizational financial accountability."
            "btn_sign_out" -> if (isId) "Keluar (Sign Out)" else "Sign Out"

            // Auth Screen
            "auth_card_desc" -> if (isId) "Akses Khusus Admin Kas\nSilakan masuk dengan Akun Google Admin untuk mengelola keuangan kas." else "Admin Cash Access Only\nPlease sign in with your Admin Google Account to manage the cashbook."
            "btn_sign_in_admin" -> if (isId) "Masuk Sebagai Admin" else "Sign in as Admin"

            // Alerts & Errors
            "error_empty_title" -> if (isId) "Nama/sumber transaksi wajib diisi" else "Transaction title is required"
            "error_invalid_amount" -> if (isId) "Jumlah nominal harus lebih dari 0" else "Amount must be greater than 0"
            "error_empty_reason" -> if (isId) "Alasan wajib diisi" else "Reason is required"
            "error_edit_validation" -> if (isId) "Semua field dan alasan koreksi wajib diisi." else "All fields and correction reason are required."

            else -> key
        }
    }
}
