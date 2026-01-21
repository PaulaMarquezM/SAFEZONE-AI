package com.example.safezoneai.ui

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.utils.WhatsAppUtils
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * ✨ HomeScreen MINIMALISTA - Versión limpia y profesional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentDangerZone by viewModel.currentDangerZone.collectAsState()
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
    val detectedCity by viewModel.detectedCity.collectAsState()
    val isLoadingZones by viewModel.isLoadingZones.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = PureWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "SafeZone AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = PureWhite
                            )
                            detectedCity?.let { city ->
                                Text(
                                    "${city.name}, ${city.country}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = PureWhite.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Historial
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            "Historial",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Configuración
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            "Configuración",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Contactos
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(
                            Icons.Default.ContactPhone,
                            "Contactos",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Mapa
                    IconButton(onClick = onNavigateToMap) {
                        Icon(
                            Icons.Default.Map,
                            "Ver Mapa",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeGreen,
                    titleContentColor = PureWhite,
                    actionIconContentColor = PureWhite
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ✅ TARJETA DE ESTADO - MINIMALISTA
                if (isLoadingZones) {
                    LoadingZonesCardMinimalist()
                } else {
                    SafetyStatusCardMinimalist(
                        currentDangerZone = currentDangerZone,
                        currentLocation = currentLocation,
                        detectedCity = detectedCity
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ✅ BOTÓN DE EMERGENCIA
                EmergencyButtonMinimalist(
                    isActive = isEmergencyActive,
                    onClick = {
                        if (!isEmergencyActive) {
                            viewModel.activateEmergency()
                            onNavigateToEmergency()
                        } else {
                            viewModel.deactivateEmergency()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // WhatsApp Button
                WhatsAppShareButtonMinimalist(
                    viewModel = viewModel,
                    currentLocation = currentLocation
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Info Section
                InfoSectionMinimalist()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ TARJETA DE ESTADO MINIMALISTA
// ═══════════════════════════════════════════════════════════

@Composable
fun SafetyStatusCardMinimalist(
    currentDangerZone: SmartZoneDetector.DangerousZone?,
    currentLocation: android.location.Location?,
    detectedCity: SmartZoneDetector.DetectedCity?
) {
    val isSafe = currentDangerZone == null ||
            currentDangerZone.dangerLevel == SmartZoneDetector.DangerLevel.SAFE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ ICONO SIMPLE - Sin círculo grande
            Icon(
                imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = if (isSafe) SafeGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ TEXTO SIMPLE - Sin duplicar
            Text(
                text = if (isSafe) "Zona Segura" else "Zona Peligrosa",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSafe) SafeGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Información de la zona
            if (currentDangerZone != null && !isSafe) {
                // Nombre de la zona
                Text(
                    text = currentDangerZone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de nivel de peligro
                DangerLevelBadgeMinimalist(currentDangerZone.dangerLevel)

                // Descripción
                if (currentDangerZone.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentDangerZone.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Fuente
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fuente: ${currentDangerZone.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else if (currentDangerZone?.dangerLevel == SmartZoneDetector.DangerLevel.SAFE) {
                // Zona segura identificada
                Text(
                    text = currentDangerZone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = SafeGreen,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentDangerZone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Te encuentras en un área segura.\nDisfruta tu día con tranquilidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Ubicación GPS
            if (currentLocation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.4f", currentLocation.latitude)}, ${String.format("%.4f", currentLocation.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Número de zonas monitoreadas
            detectedCity?.let { city ->
                if (city.dangerousZones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${city.dangerousZones.size} zonas monitoreadas en ${city.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingZonesCardMinimalist() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SafeGreen,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Detectando tu ubicación...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DangerLevelBadgeMinimalist(level: SmartZoneDetector.DangerLevel) {
    val (text, color) = when (level) {
        SmartZoneDetector.DangerLevel.SAFE -> "Segura" to SafeGreen
        SmartZoneDetector.DangerLevel.LOW -> "Precaución" to BeigeLight
        SmartZoneDetector.DangerLevel.MEDIUM -> "Alerta" to WarningBrown
        SmartZoneDetector.DangerLevel.HIGH -> "Peligro" to WarningOrange
        SmartZoneDetector.DangerLevel.CRITICAL -> "CRÍTICO" to DangerRed
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ BOTÓN DE EMERGENCIA MINIMALISTA
// ═══════════════════════════════════════════════════════════

@Composable
fun EmergencyButtonMinimalist(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(180.dp)
                .scale(scale),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) WarningOrange else DangerRed
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = PureWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isActive) "DETENER" else "EMERGENCIA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isActive)
                "Emergencia activa - Grabando audio"
            else
                "Presiona en caso de emergencia",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ WHATSAPP BUTTON MINIMALISTA
// ═══════════════════════════════════════════════════════════

@Composable
fun WhatsAppShareButtonMinimalist(
    viewModel: EmergencyViewModel,
    currentLocation: Location?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val whatsAppUtils = remember { WhatsAppUtils(context) }
    val isWhatsAppInstalled = remember { whatsAppUtils.isWhatsAppInstalled() }

    if (isWhatsAppInstalled && currentLocation != null) {
        Button(
            onClick = {
                viewModel.shareLocationViaWhatsApp()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF25D366)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp
            )
        ) {
            Icon(
                Icons.Default.Chat,
                null,
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Compartir ubicación por WhatsApp",
                color = PureWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ INFO SECTION MINIMALISTA
// ═══════════════════════════════════════════════════════════

@Composable
fun InfoSectionMinimalist() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = SafeGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "¿Qué hace el botón de emergencia?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoItemMinimalist(Icons.Default.LocationOn, "Registra tu ubicación GPS")
            InfoItemMinimalist(Icons.Default.Mic, "Graba audio automáticamente")
            InfoItemMinimalist(Icons.Default.Sms, "Alerta a tus contactos por SMS")
            InfoItemMinimalist(Icons.Default.Vibration, "Vibra para confirmar")
        }
    }
}

@Composable
fun InfoItemMinimalist(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = SafeGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark
        )
    }
}