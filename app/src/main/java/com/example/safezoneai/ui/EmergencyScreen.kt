package com.example.safezoneai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.ui.theme.DangerRed
import com.example.safezoneai.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay

/**
 * Pantalla que se muestra cuando la emergencia está activa.
 * Muestra información en tiempo real sobre el estado de la emergencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onBack: () -> Unit
) {
    val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val emergencyContacts by viewModel.emergencyContacts.collectAsState()

    // Animación de parpadeo de alerta
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Temporizador de emergencia
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isEmergencyActive) {
        if (isEmergencyActive) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DangerRed.copy(alpha = 0.1f))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🚨 EMERGENCIA ACTIVA", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DangerRed,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Indicador de alerta parpadeante
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .alpha(alpha),
                        tint = DangerRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Emergencia Activada",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatTime(elapsedSeconds),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Estado de las acciones
                    EmergencyStatusCard(
                        isRecording = isRecording,
                        currentLocation = currentLocation,
                        contactsNotified = emergencyContacts.size
                    )
                }

                // Botón para detener emergencia
                Button(
                    onClick = {
                        viewModel.deactivateEmergency()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DETENER EMERGENCIA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmergencyStatusCard(
    isRecording: Boolean,
    currentLocation: android.location.Location?,
    contactsNotified: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Estado de la Emergencia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Estado de grabación
            EmergencyStatusItem(
                icon = Icons.Default.Mic,
                title = "Grabación de Audio",
                status = if (isRecording) "Grabando..." else "Finalizada",
                isActive = isRecording
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Estado de ubicación
            EmergencyStatusItem(
                icon = Icons.Default.LocationOn,
                title = "Ubicación GPS",
                status = if (currentLocation != null)
                    "${String.format("%.4f", currentLocation.latitude)}, ${String.format("%.4f", currentLocation.longitude)}"
                else
                    "Obteniendo...",
                isActive = currentLocation != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Estado de notificación
            EmergencyStatusItem(
                icon = Icons.Default.Notifications,
                title = "Contactos Notificados",
                status = "$contactsNotified contacto(s)",
                isActive = contactsNotified > 0
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Mantén la calma. Tu ubicación y audio están siendo registrados.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        if (isActive) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}