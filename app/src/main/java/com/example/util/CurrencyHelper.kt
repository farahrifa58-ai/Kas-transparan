package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    val region: String
)

object CurrencyHelper {

    val SUPPORTED_CURRENCIES = listOf(
        // Asia Tenggara
        CurrencyOption("IDR", "Indonesia Rupiah", "Rp", "Asia Tenggara"),
        CurrencyOption("MYR", "Ringgit Malaysia", "RM", "Asia Tenggara"),
        CurrencyOption("SGD", "Dolar Singapura", "S$", "Asia Tenggara"),
        CurrencyOption("THB", "Baht Thailand", "฿", "Asia Tenggara"),
        CurrencyOption("PHP", "Peso Filipina", "₱", "Asia Tenggara"),
        CurrencyOption("VND", "Dong Vietnam", "₫", "Asia Tenggara"),
        CurrencyOption("BND", "Dolar Brunei", "B$", "Asia Tenggara"),
        CurrencyOption("KHR", "Riel Kamboja", "៛", "Asia Tenggara"),
        CurrencyOption("LAK", "Kip Laos", "₭", "Asia Tenggara"),
        CurrencyOption("MMK", "Kyat Myanmar", "K", "Asia Tenggara"),
        // Asia
        CurrencyOption("JPY", "Yen Jepang", "¥", "Asia"),
        CurrencyOption("CNY", "Yuan China", "¥", "Asia"),
        CurrencyOption("KRW", "Won Korea Selatan", "₩", "Asia"),
        CurrencyOption("INR", "Rupee India", "₹", "Asia"),
        CurrencyOption("HKD", "Dolar Hong Kong", "HK$", "Asia"),
        CurrencyOption("TWD", "Dolar Baru Taiwan", "NT$", "Asia"),
        CurrencyOption("AED", "Dirham Uni Emirat Arab", "د.إ", "Asia"),
        CurrencyOption("SAR", "Riyal Arab Saudi", "﷼", "Asia"),
        CurrencyOption("TRY", "Lira Turki", "₺", "Asia"),
        // Eropa
        CurrencyOption("EUR", "Euro", "€", "Eropa"),
        CurrencyOption("GBP", "Poundsterling Inggris", "£", "Eropa"),
        CurrencyOption("CHF", "Franc Swiss", "CHF", "Eropa"),
        // Rusia
        CurrencyOption("RUB", "Rubel Rusia", "₽", "Rusia"),
        // Amerika
        CurrencyOption("USD", "Dolar Amerika Serikat", "$", "Amerika"),
        CurrencyOption("CAD", "Dolar Kanada", "CA$", "Amerika"),
        CurrencyOption("MXN", "Peso Meksiko", "Mex$", "Amerika"),
        // Afrika
        CurrencyOption("ZAR", "Rand Afrika Selatan", "R", "Afrika"),
        CurrencyOption("NGN", "Naira Nigeria", "₦", "Afrika"),
        CurrencyOption("EGP", "Pound Mesir", "E£", "Afrika"),
        CurrencyOption("KES", "Shilling Kenya", "KSh", "Afrika")
    )

    fun getSymbol(code: String): String {
        return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }?.symbol ?: code
    }

    fun format(amount: Long, currencyCode: String = "IDR"): String {
        val symbol = getSymbol(currencyCode)
        val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
            groupingSeparator = if (currencyCode == "IDR") '.' else ','
            decimalSeparator = if (currencyCode == "IDR") ',' else '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedNumber = formatter.format(amount)
        return "$symbol $formattedNumber"
    }

    /**
     * Formats user typing input with thousand dots (e.g. 1.000, 10.000, 100.000)
     */
    fun formatInput(input: String): String {
        val clean = input.filter { it.isDigit() }
        if (clean.isEmpty()) return ""
        val num = clean.toLongOrNull() ?: return clean
        val symbols = DecimalFormatSymbols(Locale.GERMANY).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(num)
    }

    /**
     * Extracts pure Long amount from user input that contains thousand separators
     */
    fun parseCleanAmount(text: String): Long {
        val clean = text.filter { it.isDigit() }
        return clean.toLongOrNull() ?: 0L
    }
}
