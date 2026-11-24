package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- PRIMARY & SEMANTIC PALETTE (SOFT SERENE TEAL & VELVET SLATE) ---

// Light Mode Core
val EmeraldGreenPrimary = Color(0xFF0F766E)           // Serene Deep Teal
val EmeraldGreenOnPrimary = Color(0xFFFFFFFF)
val EmeraldGreenPrimaryContainer = Color(0xFFCCFBF1)  // Soft Mint-Teal Mist
val EmeraldGreenOnPrimaryContainer = Color(0xFF073B37)

val TealSecondary = Color(0xFF3B6EA5)                 // Calm Slate Blue
val TealOnSecondary = Color(0xFFFFFFFF)
val TealSecondaryContainer = Color(0xFFDCEBFA)
val TealOnSecondaryContainer = Color(0xFF0F2B46)

val AmberTertiary = Color(0xFFB45309)                 // Soft Warm Amber
val AmberOnTertiary = Color(0xFFFFFFFF)
val AmberTertiaryContainer = Color(0xFFFEF3C7)
val AmberOnTertiaryContainer = Color(0xFF451A03)

// Dark Mode Core (Velvet Midnight Slate — calm, refined, low-glare)
val EmeraldGreenPrimaryDark = Color(0xFF4FD1C5)       // Soft Luminous Teal
val EmeraldGreenOnPrimaryDark = Color(0xFF042F2C)
val EmeraldGreenPrimaryContainerDark = Color(0xFF134E4A)
val EmeraldGreenOnPrimaryContainerDark = Color(0xFF99F6E4)

val TealSecondaryDark = Color(0xFF7BB4F5)             // Soft Sky Periwinkle
val TealOnSecondaryDark = Color(0xFF082442)
val TealSecondaryContainerDark = Color(0xFF1A3552)
val TealOnSecondaryContainerDark = Color(0xFFD8EAFF)

val AmberTertiaryDark = Color(0xFFF6B756)
val AmberOnTertiaryDark = Color(0xFF3A2200)
val AmberTertiaryContainerDark = Color(0xFF3D2A0E)
val AmberOnTertiaryContainerDark = Color(0xFFFDE68A)

// Income & Expense Semantic Colors
val IncomeGreenLight = Color(0xFF15803D)              // Calm Emerald
val ExpenseRedLight = Color(0xFFC83E4D)               // Soft Rose-Crimson

val IncomeGreenDark = Color(0xFF5FD18A)               // Soft Mint-Emerald
val ExpenseRedDark = Color(0xFFF08080)                // Soft Warm Coral-Rose

val VoidGreyLight = Color(0xFF64748B)
val VoidGreyDark = Color(0xFF94A3B8)

// Light & Dark Canvas / Card Surfaces
// Light background has a gentle cool cloud tint so elevated white cards stand out cleanly
val AppBackgroundLight = Color(0xFFF1F5F9)
val AppSurfaceLight = Color(0xFFFFFFFF)
val AppSurfaceVariantLight = Color(0xFFE7EFF7)
val AppCardLight = Color(0xFFFFFFFF)
val AppOutlineLight = Color(0xFFCBD5E1)
val AppOutlineVariantLight = Color(0xFFDCE6F0)

// Dark background is deep velvet slate; cards lift clearly with a soft slate tint & crisp rim
val AppBackgroundDark = Color(0xFF0E141B)
val AppSurfaceDark = Color(0xFF18222D)
val AppSurfaceVariantDark = Color(0xFF223040)
val AppCardDark = Color(0xFF18222D)
val AppOutlineDark = Color(0xFF3A4C5E)
val AppOutlineVariantDark = Color(0xFF283848)

// Legacy aliases kept for compatibility
val TopBarBlue = Color(0xFF0F766E)
val TopBarBlueDark = Color(0xFF141E28)

val SoftSkyBlueGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE9F2F9),
        Color(0xFFF1F5F9),
        Color(0xFFF6F9FC)
    )
)

val DarkAmbientGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0C1219),
        Color(0xFF101822),
        Color(0xFF0E141B)
    )
)

// --- ADAPTIVE THEME TOKENS ---

val isAppLightTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background == AppBackgroundLight

val AppScreenBackgroundBrush: Brush
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) SoftSkyBlueGradient else DarkAmbientGradient

val SoftCardBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFFDBE5F0) else Color(0xFF2A3B4D)

val SoftTopBarColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFF0F766E) else Color(0xFF141E28)

val SoftTopBarContentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color.White else Color(0xFFF1F5F9)

val SoftHeroGradient: Brush
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F766E),
                Color(0xFF0D9488),
                Color(0xFF0E7490)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF124642),
                Color(0xFF14535E),
                Color(0xFF1A4263)
            )
        )
    }

val VoidGrey: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) VoidGreyLight else VoidGreyDark

val IncomeGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) IncomeGreenLight else IncomeGreenDark

val ExpenseRed: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) ExpenseRedLight else ExpenseRedDark

// Adaptive soft status pills (Success / Info / Warning)
val SoftSuccessBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFFE7F8EF) else Color(0xFF123225)

val SoftSuccessFg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFF15803D) else Color(0xFF6EE7B7)

val SoftDangerBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFFFDECEC) else Color(0xFF3A1D23)

val SoftDangerFg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isAppLightTheme) Color(0xFFB91C1C) else Color(0xFFFCA5A5)

// Per-Section Adaptive Accent Pairs (Icon Tint + Soft Badge Container)
data class SectionAccent(val tint: Color, val container: Color)

@Composable
@ReadOnlyComposable
fun accentProfile(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF0F766E), container = Color(0xFFE2F7F4))
} else {
    SectionAccent(tint = Color(0xFF5EEAD4), container = Color(0xFF133B37))
}

@Composable
@ReadOnlyComposable
fun accentAppearance(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF5B5FC7), container = Color(0xFFECEEFF))
} else {
    SectionAccent(tint = Color(0xFFA5B4FC), container = Color(0xFF21264A))
}

@Composable
@ReadOnlyComposable
fun accentPublicReport(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF0284C7), container = Color(0xFFE0F2FE))
} else {
    SectionAccent(tint = Color(0xFF60B8F5), container = Color(0xFF132F47))
}

@Composable
@ReadOnlyComposable
fun accentDomain(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF0891B2), container = Color(0xFFDEF7FA))
} else {
    SectionAccent(tint = Color(0xFF67E8F9), container = Color(0xFF12343F))
}

@Composable
@ReadOnlyComposable
fun accentCurrency(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFFB45309), container = Color(0xFFFEF3C7))
} else {
    SectionAccent(tint = Color(0xFFFBBF24), container = Color(0xFF3B2B11))
}

@Composable
@ReadOnlyComposable
fun accentBackup(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF2563EB), container = Color(0xFFE8F0FE))
} else {
    SectionAccent(tint = Color(0xFF7CA7FF), container = Color(0xFF182A4D))
}

@Composable
@ReadOnlyComposable
fun accentTutorial(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF059669), container = Color(0xFFDCFCE7))
} else {
    SectionAccent(tint = Color(0xFF6EE7B7), container = Color(0xFF123526))
}

@Composable
@ReadOnlyComposable
fun accentAbout(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF475569), container = Color(0xFFEDF2F7))
} else {
    SectionAccent(tint = Color(0xFFCBD5E1), container = Color(0xFF233040))
}

@Composable
@ReadOnlyComposable
fun accentExportCustomization(): SectionAccent = if (isAppLightTheme) {
    SectionAccent(tint = Color(0xFF0F6B5B), container = Color(0xFFD9F3ED))
} else {
    SectionAccent(tint = Color(0xFF5EEAD4), container = Color(0xFF123832))
}

