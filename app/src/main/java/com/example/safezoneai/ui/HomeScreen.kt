package com.example.safezoneai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.ZoneRepository
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * HomeScreen - Pantalla principal de SafeZone AI
 *
 * PATRÓN DE DISEÑO: Composite Pattern
 * PRINCIPIO SOLID: Single Responsibility Principle (SRP)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToEmergency: () -> Unit
) {
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentDangerZone by viewModel.currentDangerZone.collectAsState()
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SafeZone AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = SafeGreenLight,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Map,
                            "Ver Mapa",
                            tint = SafeGreen
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SafeGreenLight.copy(alpha = 0.3f),
                            PureWhite
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                SafetyStatusCard(
                    currentDangerZone = currentDangerZone,
                    currentLocation = currentLocation
                )

                Spacer(modifier = Modifier.height(24.dp))

                EmergencyButton(
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

                InfoSection()
            }
        }
    }
}

@Composable
fun SafetyStatusCard(
    currentDangerZone: ZoneRepository.DangerousZone?,
    currentLocation: android.location.Location?
) {
    val isSafe = currentDangerZone == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (isSafe) SafeGreen else DangerRed
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSafe)
                SafeGreenLight.copy(alpha = 0.3f)
            else
                Color(0xFFFFE5E5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedSafetyIcon(isSafe = isSafe)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isSafe) "✓ ZONA SEGURA" else "⚠️ ZONA PELIGROSA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSafe) SafeGreen else DangerRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (currentDangerZone != null) {
                Surface(
                    color = DangerRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        DangerRed.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentDangerZone.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DangerLevelChip(currentDangerZone.dangerLevel)
                    }
                }
            } else {
                Text(
                    text = "Te encuentras en un área segura.\nDisfruta tu día con tranquilidad.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (currentLocation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
        }
    }
}

@Composable
fun AnimatedSafetyIcon(isSafe: Boolean) {
    var shouldAnimate by remember { mutableStateOf(false) }

    LaunchedEffect(isSafe) {
        shouldAnimate = true
    }

    val scale by animateFloatAsState(
        targetValue = if (shouldAnimate) (if (!isSafe) 1.15f else 1.05f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (!isSafe) 600 else 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .background(
                color = if (isSafe) SafeGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (isSafe) SafeGreen else DangerRed
        )
    }
}
@Composable
fun DangerLevelChip(level: ZoneRepository.DangerLevel) {
    val (text, color) = when (level) {
        ZoneRepository.DangerLevel.LOW -> "⚠️ Precaución" to BeigeLight
        ZoneRepository.DangerLevel.MEDIUM -> "⚠️ Alerta" to WarningBrown
        ZoneRepository.DangerLevel.HIGH -> "🚨 Peligro" to WarningOrange
        ZoneRepository.DangerLevel.CRITICAL -> "🚨 CRÍTICO" to DangerRed
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmergencyButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Animación de escala
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animación de sombra usando animateValue
    val shadowElevation by infiniteTransition.animateValue(
        initialValue = 8.dp,
        targetValue = if (isActive) 16.dp else 8.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .shadow(
                    elevation = shadowElevation,
                    shape = CircleShape,
                    spotColor = if (isActive) WarningOrange else DangerRed
                ),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) WarningOrange else DangerRed
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
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
                    modifier = Modifier.size(80.dp),
                    tint = PureWhite
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isActive) "DETENER" else "EMERGENCIA",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = if (isActive) WarningOrange.copy(alpha = 0.1f) else SafeGreenLight.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isActive)
                    "🔴 Emergencia activa - Grabando audio"
                else
                    "Presiona en caso de emergencia",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) WarningOrange else TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InfoSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhite
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    null,
                    tint = SafeGreen,
                    modifier = Modifier.size(28.dp)
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

            InfoItem("📍", "Registra tu ubicación GPS en tiempo real")
            InfoItem("🎙️", "Graba audio automáticamente para evidencia")
            InfoItem("📱", "Alerta a tus contactos de emergencia por SMS")
            InfoItem("📳", "Vibra para confirmar la activación")
            InfoItem("📊", "Guarda toda la información localmente")
        }
    }
}

@Composable
fun InfoItem(icon: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = SafeGreenLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark
        )
    }
}