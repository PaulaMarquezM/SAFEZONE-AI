package com.example.safezoneai.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.safezoneai.R

/**
 * Tipografía de SafeZone AI
 * Basada en la familia Poppins para una apariencia moderna y legible.
 *
 * PRINCIPIO SOLID: Single Responsibility Principle (SRP)
 * Esta clase tiene una única responsabilidad: definir la tipografía del sistema.
 *
 * NOTA: Para usar Poppins, debes agregar las fuentes en res/font/
 * Descarga desde: https://fonts.google.com/specimen/Poppins
 */

// ═══════════════════════════════════════════════════════════
// FAMILIA DE FUENTES POPPINS
// ═══════════════════════════════════════════════════════════

/**
 * INSTRUCCIONES DE INSTALACIÓN:
 *
 * 1. Descarga Poppins de Google Fonts
 * 2. Crea la carpeta: app/src/main/res/font/
 * 3. Añade estos archivos:
 *    - poppins_regular.ttf
 *    - poppins_medium.ttf
 *    - poppins_semibold.ttf
 *    - poppins_bold.ttf
 *
 * Si no tienes las fuentes, el sistema usará la fuente por defecto.
 */

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

// ═══════════════════════════════════════════════════════════
// TIPOGRAFÍA MATERIAL 3
// ═══════════════════════════════════════════════════════════

val Typography = Typography(
    // ───────────────────────────────────────────────────────
    // Display - Títulos muy grandes (pantallas de bienvenida)
    // ───────────────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // ───────────────────────────────────────────────────────
    // Headline - Títulos de secciones importantes
    // ───────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ───────────────────────────────────────────────────────
    // Title - Títulos de tarjetas y diálogos
    // ───────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ───────────────────────────────────────────────────────
    // Body - Texto principal de la aplicación
    // ───────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ───────────────────────────────────────────────────────
    // Label - Textos de botones y etiquetas
    // ───────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ═══════════════════════════════════════════════════════════
// FALLBACK SI NO HAY FUENTES
// ═══════════════════════════════════════════════════════════

/**
 * Si no puedes agregar las fuentes Poppins, usa esta versión con fuentes del sistema:
 */
val TypographyFallback = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)