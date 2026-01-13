package com.example.safezoneai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema de SafeZone AI
 *
 * PATRÓN DE DISEÑO: Strategy Pattern
 * El tema puede cambiar dinámicamente entre claro/oscuro según las preferencias del sistema.
 *
 * PRINCIPIO SOLID: Open/Closed Principle (OCP)
 * El tema está abierto para extensión (nuevos colores) pero cerrado para modificación.
 */

// ═══════════════════════════════════════════════════════════
// ESQUEMA DE COLORES MODO CLARO
// ═══════════════════════════════════════════════════════════

private val LightColorScheme = lightColorScheme(
    // Colores principales
    primary = SafeGreen,
    onPrimary = PureWhite,
    primaryContainer = SafeGreenLight,
    onPrimaryContainer = TextDark,

    // Colores secundarios
    secondary = WarningBrown,
    onSecondary = PureWhite,
    secondaryContainer = BeigeLight,
    onSecondaryContainer = TextDark,

    // Colores terciarios (para acciones adicionales)
    tertiary = SafeGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SafeGreenLight,
    onTertiaryContainer = TextDark,

    // Fondos
    background = PureWhite,
    onBackground = TextDark,
    surface = PureWhite,
    onSurface = TextDark,
    surfaceVariant = SafeGreenLight,
    onSurfaceVariant = TextSecondary,

    // Estados de error
    error = DangerRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Bordes y divisores
    outline = BorderGray,
    outlineVariant = SafeGreenLight,

    // Capas de superficie
    surfaceTint = SafeGreen,
    inverseSurface = TextDark,
    inverseOnSurface = PureWhite,
    inversePrimary = SafeGreenLight
)

// ═══════════════════════════════════════════════════════════
// ESQUEMA DE COLORES MODO OSCURO
// ═══════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    // Colores principales
    primary = SafeGreenLight,
    onPrimary = TextDark,
    primaryContainer = SafeGreen,
    onPrimaryContainer = PureWhite,

    // Colores secundarios
    secondary = BeigeLight,
    onSecondary = TextDark,
    secondaryContainer = WarningBrown,
    onSecondaryContainer = PureWhite,

    // Colores terciarios
    tertiary = SafeGreenLight,
    onTertiary = TextDark,
    tertiaryContainer = SafeGreen,
    onTertiaryContainer = PureWhite,

    // Fondos
    background = DarkBackground,
    onBackground = PureWhite,
    surface = Color(0xFF2C2C2C),
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFBDBDBD),

    // Estados de error
    error = DangerRed,
    onError = PureWhite,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Bordes y divisores
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF3A3A3A),

    // Capas de superficie
    surfaceTint = SafeGreenLight,
    inverseSurface = PureWhite,
    inverseOnSurface = TextDark,
    inversePrimary = SafeGreen
)

// ═══════════════════════════════════════════════════════════
// COMPOSABLE DEL TEMA
// ═══════════════════════════════════════════════════════════

/**
 * Tema principal de SafeZone AI
 *
 * @param darkTheme Si true, usa el tema oscuro. Por defecto detecta el tema del sistema.
 * @param dynamicColor Si true y API ≥ 31, usa colores dinámicos del sistema (Material You).
 * @param content Contenido de la aplicación que usará este tema.
 */
@Composable
fun SafeZoneAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivado por defecto para mantener nuestra paleta
    content: @Composable () -> Unit
) {
    // ───────────────────────────────────────────────────────
    // Selección del esquema de colores
    // ───────────────────────────────────────────────────────
    val colorScheme = when {
        // Material You (Android 12+) - Solo si se habilita explícitamente
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Tema oscuro personalizado
        darkTheme -> DarkColorScheme
        // Tema claro personalizado (por defecto)
        else -> LightColorScheme
    }

    // ───────────────────────────────────────────────────────
    // Configuración de la barra de estado
    // ───────────────────────────────────────────────────────
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Color de la barra de estado
            window.statusBarColor = colorScheme.primary.toArgb()

            // Iconos de la barra de estado (claros en tema oscuro, oscuros en tema claro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // ───────────────────────────────────────────────────────
    // Aplicación del tema
    // ───────────────────────────────────────────────────────
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Usa Typography si tienes Poppins, si no usa TypographyFallback
        content = content
    )
}

// ═══════════════════════════════════════════════════════════
// FUNCIÓN DE UTILIDAD PARA OBTENER EL COLOR DE ZONA
// ═══════════════════════════════════════════════════════════

/**
 * Obtiene el color apropiado según el nivel de peligro de una zona.
 *
 * PATRÓN DE DISEÑO: Factory Method
 * Centraliza la lógica de selección de colores según el tipo de zona.
 */
@Composable
fun getDangerLevelColor(dangerLevel: String): Color {
    return when (dangerLevel.uppercase()) {
        "CRITICAL" -> DangerRed
        "HIGH" -> WarningOrange
        "MEDIUM" -> WarningBrown
        "LOW" -> BeigeLight
        else -> SafeGreen
    }
}

/**
 * Obtiene el color de fondo apropiado para una zona.
 */
@Composable
fun getDangerLevelBackgroundColor(dangerLevel: String): Color {
    return when (dangerLevel.uppercase()) {
        "CRITICAL" -> DangerRed.copy(alpha = 0.1f)
        "HIGH" -> WarningOrange.copy(alpha = 0.1f)
        "MEDIUM" -> WarningBrown.copy(alpha = 0.1f)
        "LOW" -> BeigeLight.copy(alpha = 0.2f)
        else -> SafeGreenLight.copy(alpha = 0.3f)
    }
}