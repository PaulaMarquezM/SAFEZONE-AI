package com.example.safezoneai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.service.LocationTrackingService
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.SettingsViewModel

/**
 * ⚙️ PANTALLA DE CONFIGURACIÓN
 * Permite personalizar comportamiento de la app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val autoRecordEnabled by viewModel.autoRecordEnabled.collectAsState()
    val backgroundTrackingEnabled by viewModel.backgroundTrackingEnabled.collectAsState()
    val gpsInterval by viewModel.gpsInterval.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Configuración",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeGreen,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECCIÓN: NOTIFICACIONES
                SettingsSection(
                    title = "🔔 Notificaciones",
                    icon = Icons.Default.Notifications
                ) {
                    SettingsSwitchItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Sonido",
                        description = "Reproducir sonido al entrar en zona peligrosa",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) }
                    )

                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibración",
                        description = "Vibrar al activar emergencia o detectar peligro",
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )
                }

                // SECCIÓN: EMERGENCIA
                SettingsSection(
                    title = "🚨 Emergencia",
                    icon = Icons.Default.Warning
                ) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Mic,
                        title = "Grabación automática",
                        description = "Grabar audio al presionar botón de emergencia",
                        checked = autoRecordEnabled,
                        onCheckedChange = { viewModel.setAutoRecordEnabled(it) }
                    )

                    SettingsSliderItem(
                        icon = Icons.Default.Timer,
                        title = "Duración de grabación",
                        description = "$recordingDuration segundos",
                        value = recordingDuration.toFloat(),
                        valueRange = 10f..300f,
                        onValueChange = { viewModel.setRecordingDuration(it.toInt()) }
                    )
                }

                // SECCIÓN: UBICACIÓN
                SettingsSection(
                    title = "📍 Ubicación",
                    icon = Icons.Default.LocationOn
                ) {
                    SettingsSliderItem(
                        icon = Icons.Default.Speed,
                        title = "Intervalo de actualización GPS",
                        description = "Cada $gpsInterval segundos",
                        value = gpsInterval.toFloat(),
                        valueRange = 5f..60f,
                        onValueChange = { viewModel.setGpsInterval(it.toInt()) }
                    )
                }

                // SECCIÓN: MONITOREO EN SEGUNDO PLANO
                SettingsSection(
                    title = "🔄 Monitoreo en segundo plano",
                    icon = Icons.Default.MyLocation
                ) {
                    SettingsSwitchItem(
                        icon = Icons.Default.GpsFixed,
                        title = "Monitoreo en background",
                        description = "Detectar zonas peligrosas con la app cerrada",
                        checked = backgroundTrackingEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setBackgroundTrackingEnabled(enabled)
                            if (enabled) {
                                LocationTrackingService.start(context)
                            } else {
                                LocationTrackingService.stop(context)
                            }
                        }
                    )
                }

                // SECCIÓN: ACERCA DE
                SettingsSection(
                    title = "ℹ️ Acerca de",
                    icon = Icons.Default.Info
                ) {
                    SettingsInfoItem(
                        icon = Icons.Default.Code,
                        title = "Versión",
                        value = "1.0.0"
                    )

                    SettingsInfoItem(
                        icon = Icons.Default.Person,
                        title = "Desarrollador",
                        value = "Grupo 3"
                    )

                    SettingsInfoItem(
                        icon = Icons.Default.Build,
                        title = "Tecnología",
                        value = "Kotlin"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// COMPONENTES REUTILIZABLES
// ═══════════════════════════════════════════════════════════

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                icon,
                null,
                tint = SafeGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = SafeGreen,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SafeGreen,
                checkedTrackColor = SafeGreenLight
            )
        )
    }
}

@Composable
fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = SafeGreen,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = SafeGreen,
                activeTrackColor = SafeGreen,
                inactiveTrackColor = SafeGreenLight
            )
        )
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = SafeGreen,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
    }
}