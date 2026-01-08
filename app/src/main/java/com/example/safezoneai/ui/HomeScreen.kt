package com.example.safezoneai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.ZoneRepository
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * Pantalla principal de SafeZone AI.
 * Muestra el estado de seguridad actual y el botón de emergencia.
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
                title = { Text("SafeZone AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, "Ver Mapa")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Estado de zona actual
            SafetyStatusCard(
                currentDangerZone = currentDangerZone,
                currentLocation = currentLocation
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de emergencia
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

            // Información adicional
            InfoSection()
        }
    }
}

@Composable
fun SafetyStatusCard(
    currentDangerZone: ZoneRepository.DangerousZone?,
    currentLocation: android.location.Location?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentDangerZone != null)
                DangerRed.copy(alpha = 0.1f)
            else
                SafeGreen.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (currentDangerZone != null)
                    Icons.Default.Warning
                else
                    Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (currentDangerZone != null) DangerRed else SafeGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (currentDangerZone != null)
                    "⚠️ ZONA PELIGROSA"
                else
                    "✓ ZONA SEGURA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (currentDangerZone != null) DangerRed else SafeGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentDangerZone != null) {
                Text(
                    text = currentDangerZone.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Nivel: ${currentDangerZone.dangerLevel.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Te encuentras en un área segura",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (currentLocation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "📍 ${String.format("%.4f", currentLocation.latitude)}, ${String.format("%.4f", currentLocation.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun EmergencyButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Animación de pulsación
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
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
                .size(200.dp)
                .scale(scale),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) WarningOrange else DangerRed
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
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isActive) "DETENER" else "EMERGENCIA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🛡️ ¿Qué hace el botón de emergencia?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoItem("📍", "Registra tu ubicación GPS")
            InfoItem("🎙️", "Graba audio automáticamente")
            InfoItem("📱", "Alerta a tus contactos de emergencia")
            InfoItem("📳", "Vibra para confirmar activación")
        }
    }
}

@Composable
fun InfoItem(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}