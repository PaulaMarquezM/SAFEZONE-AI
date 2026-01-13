package com.example.safezoneai.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores de SafeZone AI
 * Diseñada para transmitir seguridad, confianza y naturalidad.
 *
 * PRINCIPIO SOLID: Single Responsibility Principle (SRP)
 * Esta clase tiene una única responsabilidad: definir los colores del sistema.
 */

// ═══════════════════════════════════════════════════════════
// COLORES PRINCIPALES - Identidad de marca
// ═══════════════════════════════════════════════════════════

/**
 * Verde principal - Representa zonas seguras y confianza
 * Uso: Elementos principales, estados seguros, confirmaciones
 */
val SafeGreen = Color(0xFF6E7D4E)

/**
 * Verde claro - Fondos suaves y elementos secundarios
 * Uso: Fondos de tarjetas en zonas seguras, estados pasivos
 */
val SafeGreenLight = Color(0xFFD8E0C7)

/**
 * Blanco base - Fondos principales
 * Uso: Fondo de pantallas, tarjetas principales
 */
val PureWhite = Color(0xFFFFFFFF)

// ═══════════════════════════════════════════════════════════
// COLORES DE ALERTA - Sistema de advertencias
// ═══════════════════════════════════════════════════════════

/**
 * Marrón oscuro - Alertas secundarias y advertencias
 * Uso: Zonas de precaución, alertas medias
 */
val WarningBrown = Color(0xFFAA7229)

/**
 * Beige claro - Fondos alternos en alertas
 * Uso: Fondos de tarjetas de advertencia
 */
val BeigeLight = Color(0xFFF0C48C)

// ═══════════════════════════════════════════════════════════
// COLORES ADICIONALES - Estados críticos
// ═══════════════════════════════════════════════════════════

/**
 * Rojo intenso - Solo para EMERGENCIAS críticas
 * Uso: Botón de emergencia, alertas de peligro extremo
 */
val DangerRed = Color(0xFFD32F2F)

/**
 * Naranja - Alertas de peligro alto
 * Uso: Zonas peligrosas de nivel HIGH
 */
val WarningOrange = Color(0xFFFF8A65)

// ═══════════════════════════════════════════════════════════
// COLORES NEUTROS - UI Elements
// ═══════════════════════════════════════════════════════════

/**
 * Gris oscuro - Textos principales
 */
val TextDark = Color(0xFF2C2C2C)

/**
 * Gris medio - Textos secundarios
 */
val TextSecondary = Color(0xFF757575)

/**
 * Gris claro - Bordes y divisores
 */
val BorderGray = Color(0xFFE0E0E0)

/**
 * Fondo oscuro - Modo oscuro
 */
val DarkBackground = Color(0xFF1A1A1A)

// ═══════════════════════════════════════════════════════════
// COLORES MATERIAL 3 - Compatibilidad con tema
// ═══════════════════════════════════════════════════════════

val md_theme_light_primary = SafeGreen
val md_theme_light_onPrimary = PureWhite
val md_theme_light_secondary = WarningBrown
val md_theme_light_onSecondary = PureWhite
val md_theme_light_background = PureWhite
val md_theme_light_surface = SafeGreenLight
val md_theme_light_error = DangerRed

val md_theme_dark_primary = SafeGreenLight
val md_theme_dark_onPrimary = TextDark
val md_theme_dark_secondary = BeigeLight
val md_theme_dark_onSecondary = TextDark
val md_theme_dark_background = DarkBackground
val md_theme_dark_surface = Color(0xFF2C2C2C)
val md_theme_dark_error = DangerRed