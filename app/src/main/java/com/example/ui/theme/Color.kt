package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Colors
val EmeraldGreenPrimary = Color(0xFF1B5E20)
val EmeraldGreenOnPrimary = Color(0xFFFFFFFF)
val EmeraldGreenPrimaryContainer = Color(0xFFA5D6A7)
val EmeraldGreenOnPrimaryContainer = Color(0xFF002105)

val TealSecondary = Color(0xFF00796B)
val TealOnSecondary = Color(0xFFFFFFFF)
val TealSecondaryContainer = Color(0xFFB2DFDB)
val TealOnSecondaryContainer = Color(0xFF00201C)

val AmberTertiary = Color(0xFFE65100)
val AmberOnTertiary = Color(0xFFFFFFFF)

val IncomeGreen = Color(0xFF2E7D32)
val ExpenseRed = Color(0xFFC62828)
val VoidGrey = Color(0xFF757575)

// Requested Light Background: Soft light sky blue on top blending down to pure white
val LightBgTopBlue = Color(0xFFE3F2FD) // Soft subtle light blue
val LightBgMidBlue = Color(0xFFF0F7FD)
val LightBgBottomWhite = Color(0xFFFFFFFF)

val SoftSkyBlueGradient = Brush.verticalGradient(
    colors = listOf(
        LightBgTopBlue,
        LightBgMidBlue,
        LightBgBottomWhite,
        LightBgBottomWhite
    )
)

val AppBackgroundLight = Color(0xFFFFFFFF)
val AppSurfaceLight = Color(0xFFFFFFFF)
val AppCardLight = Color(0xFFFFFFFF)

// Dark Colors
val EmeraldGreenPrimaryDark = Color(0xFF81C784)
val EmeraldGreenOnPrimaryDark = Color(0xFF00390F)
val EmeraldGreenPrimaryContainerDark = Color(0xFF00531A)
val EmeraldGreenOnPrimaryContainerDark = Color(0xFFA5D6A7)

val TealSecondaryDark = Color(0xFF80CBC4)
val TealOnSecondaryDark = Color(0xFF003730)

val AppBackgroundDark = Color(0xFF111712)
val AppSurfaceDark = Color(0xFF19231B)
val AppCardDark = Color(0xFF202C23)
