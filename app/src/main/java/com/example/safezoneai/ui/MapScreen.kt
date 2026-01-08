package com.example.safezoneai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.ZoneRepository
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel
import kotlin.math.roundToInt

/**
 * Pantalla del mapa que muestra zonas peligrosas.
 * En una versión completa integraría Google Maps.
 * Por ahora muestra una lista de zonas con información.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onBack: () -> Unit
) {
    val dangerousZones = remember { viewModel.getAllDangerousZones() }
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentDangerZone by viewModel.currentDangerZone.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Zonas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
        ) {
            // Ubicación actual
            CurrentLocationCard(
                currentLocation = currentLocation,
                currentDangerZone = currentDangerZone
            )

            // Lista de zonas peligrosas
            Text(
                text = "Zonas Peligrosas Registradas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(dangerousZones) { zone ->
                    DangerZoneCard(
                        zone = zone,
                        currentLocation = currentLocation,
                        isCurrentZone = zone == currentDangerZone
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentLocationCard(
    currentLocation: android.location.Location?,
    currentDangerZone: ZoneRepository.DangerousZone?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentDangerZone != null)
                DangerRed.copy(alpha = 0.1f)
            else
                SafeGreen.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (currentDangerZone != null) DangerRed else SafeGreen
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Tu Ubicación Actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (currentLocation != null) {
                    Text(
                        text = "${String.format("%.5f", currentLocation.latitude)}, ${String.format("%.5f", currentLocation.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = if (currentDangerZone != null)
                            "⚠️ En zona peligrosa: ${currentDangerZone.name}"
                        else
                            "✓ En zona segura",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (currentDangerZone != null) DangerRed else SafeGreen
                    )
                } else {
                    Text(
                        text = "Obteniendo ubicación...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun DangerZoneCard(
    zone: ZoneRepository.DangerousZone,
    currentLocation: android.location.Location?,
    isCurrentZone: Boolean
) {
    val distance = currentLocation?.let {
        val zoneLoc = android.location.Location("zone").apply {
            latitude = zone.latitude
            longitude = zone.longitude
        }
        it.distanceTo(zoneLoc)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCurrentZone -> DangerRed.copy(alpha = 0.2f)
                zone.dangerLevel == ZoneRepository.DangerLevel.CRITICAL -> DangerRed.copy(alpha = 0.1f)
                zone.dangerLevel == ZoneRepository.DangerLevel.HIGH -> WarningOrange.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Dangerous,
                    contentDescription = null,
                    tint = getDangerColor(zone.dangerLevel),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DangerLevelBadge(zone.dangerLevel)

                        if (isCurrentZone) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = DangerRed,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Ubicación actual",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    label = "Coordenadas",
                    value = "${String.format("%.4f", zone.latitude)}, ${String.format("%.4f", zone.longitude)}"
                )

                InfoChip(
                    label = "Radio",
                    value = "${zone.radiusMeters.roundToInt()}m"
                )
            }

            if (distance != null && !isCurrentZone) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📍 A ${(distance / 1000).let { if (it < 1) "${distance.roundToInt()}m" else "${String.format("%.1f", it)}km" }} de tu ubicación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DangerLevelBadge(level: ZoneRepository.DangerLevel) {
    val (text, color) = when (level) {
        ZoneRepository.DangerLevel.LOW -> "Precaución" to AlertYellow
        ZoneRepository.DangerLevel.MEDIUM -> "Alerta" to WarningOrange
        ZoneRepository.DangerLevel.HIGH -> "Peligro" to DangerRed
        ZoneRepository.DangerLevel.CRITICAL -> "Crítico" to DangerRed
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getDangerColor(level: ZoneRepository.DangerLevel): Color {
    return when (level) {
        ZoneRepository.DangerLevel.LOW -> AlertYellow
        ZoneRepository.DangerLevel.MEDIUM -> WarningOrange
        ZoneRepository.DangerLevel.HIGH -> DangerRed
        ZoneRepository.DangerLevel.CRITICAL -> DangerRed
    }
}