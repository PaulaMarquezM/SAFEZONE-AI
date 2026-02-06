package com.example.safezoneai.ui

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.utils.WhatsAppUtils
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * ✨ HomeScreen MINIMALISTA - Versión limpia, profesional y 100% RESPONSIVE
 * ✅ Optimizado para pantallas pequeñas y grandes
 * ✅ Scroll vertical completo
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
    val isRecording by viewModel.isRecording.collectAsState()

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
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "Historial", tint = PureWhite, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Configuración", tint = PureWhite, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(Icons.Default.ContactPhone, "Contactos", tint = PureWhite, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, "Ver Mapa", tint = PureWhite, modifier = Modifier.size(22.dp))
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
            // ✅ SCROLL VERTICAL COMPLETO
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de estado
                if (isLoadingZones) {
                    LoadingZonesCardMinimalist()
                } else {
                    SafetyStatusCardMinimalist(
                        currentDangerZone = currentDangerZone,
                        currentLocation = currentLocation,
                        detectedCity = detectedCity
                    )
                }

                // Botón de emergencia
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

                // 👉 BOTÓN NUEVO SOLO AUDIO (debajo del grande)
                if (isRecording) {
                    StopAudioButtonMinimalist(
                        onClick = { viewModel.stopOnlyAudioRecording() }
                    )
                }

                // WhatsApp Button
                WhatsAppShareButtonMinimalist(
                    viewModel = viewModel,
                    currentLocation = currentLocation
                )

                // Info Section
                InfoSectionMinimalist()

                // Espacio final
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ TARJETA DE ESTADO MINIMALISTA Y COMPACTA
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
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),  // ✅ Compacto
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(44.dp),  // ✅ Más pequeño
                tint = if (isSafe) SafeGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isSafe) "Zona Segura" else "Zona Peligrosa",
                style = MaterialTheme.typography.titleLarge,  // ✅ Reducido
                fontWeight = FontWeight.Bold,
                color = if (isSafe) SafeGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentDangerZone != null && !isSafe) {
                Text(
                    text = currentDangerZone.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                DangerLevelBadgeMinimalist(currentDangerZone.dangerLevel)

                if (currentDangerZone.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentDangerZone.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fuente: ${currentDangerZone.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else if (currentDangerZone?.dangerLevel == SmartZoneDetector.DangerLevel.SAFE) {
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

            if (currentLocation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${String.format("%.4f", currentLocation.latitude)}, ${String.format("%.4f", currentLocation.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            detectedCity?.let { city ->
                if (city.dangerousZones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SafeGreen,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
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
// ✨ BOTÓN DE EMERGENCIA - MÁS COMPACTO
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
                .size(200.dp)  // ✅ Reducido de 180dp a 150dp
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
                    modifier = Modifier.size(52.dp),  // ✅ Reducido
                    tint = PureWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isActive) "DETENER" else "Emergencia",  // ✅ "SOS" es más corto
                    fontSize = 17.sp,  // ✅ Reducido
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isActive)
                "Grabando evidencia..."
            else
                "Presiona en caso de emergencia",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ WHATSAPP BUTTON - MÁS COMPACTO
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
                .height(48.dp),  // ✅ Reducido
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
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Compartir ubicación por WhatsApp",
                color = PureWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ INFO SECTION - MÁS COMPACTA
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
            modifier = Modifier.padding(18.dp)  // ✅ Reducido de 20dp a 18dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = SafeGreen,
                    modifier = Modifier.size(22.dp)  // ✅ Reducido
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "¿Qué hace el botón de emergencia?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))  // ✅ Reducido

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
            .padding(vertical = 5.dp),  // ✅ Reducido de 6dp a 5dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = SafeGreen,
            modifier = Modifier.size(19.dp)  // ✅ Reducido
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark
        )
    }
}

@Composable
fun StopAudioButtonMinimalist(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WarningBrown
        )
    ) {
        Icon(
            Icons.Default.MicOff,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Detener grabación de audio",
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
