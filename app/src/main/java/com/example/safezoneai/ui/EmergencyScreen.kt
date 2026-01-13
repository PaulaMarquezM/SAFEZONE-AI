package com.example.safezoneai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel
import kotlinx.coroutines.delay

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

    // Animación parpadeo
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Temporizador
    var elapsedSeconds by remember { mutableIntStateOf(0) }

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
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFE5E5), PureWhite)
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .alpha(alpha)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🚨 EMERGENCIA ACTIVA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DangerRed,
                        titleContentColor = PureWhite,
                        navigationIconContentColor = PureWhite
                    )
                )
            },

            // 🔴 FIX CLAVE DEL BUG VISUAL
            bottomBar = {
                StopEmergencyButton {
                    viewModel.deactivateEmergency()
                    onBack()
                }
            },

            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                EmergencyAlertIndicator(alpha)

                Spacer(modifier = Modifier.height(24.dp))

                EmergencyTimer(elapsedSeconds)

                Spacer(modifier = Modifier.height(32.dp))

                EmergencyStatusCard(
                    isRecording = isRecording,
                    currentLocation = currentLocation,
                    contactsNotified = emergencyContacts.size
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────────────────

@Composable
fun EmergencyAlertIndicator(alpha: Float) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .alpha(alpha)
            .shadow(16.dp, RoundedCornerShape(70.dp), spotColor = DangerRed)
            .background(
                DangerRed.copy(alpha = 0.15f),
                RoundedCornerShape(70.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = DangerRed
        )
    }
}

// ────────────────────────────────────────────────────────────

@Composable
fun EmergencyTimer(elapsedSeconds: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Emergencia Activada",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = DangerRed
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = DangerRed.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, DangerRed.copy(alpha = 0.3f))
        ) {
            Text(
                formatTime(elapsedSeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = DangerRed,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────

@Composable
fun EmergencyStatusCard(
    isRecording: Boolean,
    currentLocation: android.location.Location?,
    contactsNotified: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = SafeGreen, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Estado de la Emergencia",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            EmergencyStatusItem(
                Icons.Default.Mic,
                "Grabación de Audio",
                if (isRecording) "🔴 Grabando..." else "✓ Finalizada",
                isRecording,
                if (isRecording) DangerRed else SafeGreen
            )

            Spacer(modifier = Modifier.height(16.dp))

            EmergencyStatusItem(
                Icons.Default.LocationOn,
                "Ubicación GPS",
                currentLocation?.let {
                    "${"%.4f".format(it.latitude)}, ${"%.4f".format(it.longitude)}"
                } ?: "Obteniendo...",
                currentLocation != null,
                if (currentLocation != null) SafeGreen else WarningBrown
            )

            Spacer(modifier = Modifier.height(16.dp))

            EmergencyStatusItem(
                Icons.Default.Notifications,
                "Contactos Notificados",
                "$contactsNotified contacto(s)",
                contactsNotified > 0,
                if (contactsNotified > 0) SafeGreen else TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────

@Composable
fun EmergencyStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String,
    isActive: Boolean,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(status, color = TextSecondary)
            }

            if (isActive) {
                Icon(Icons.Default.CheckCircle, null, tint = SafeGreen)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// 🔴 BOTÓN FIXEADO
// ────────────────────────────────────────────────────────────

@Composable
fun StopEmergencyButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .height(64.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Stop, null, tint = PureWhite)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "DETENER EMERGENCIA",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PureWhite
            )
        }
    }
}

// ────────────────────────────────────────────────────────────

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
