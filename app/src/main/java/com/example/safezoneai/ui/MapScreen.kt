package com.example.safezoneai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.ZoneRepository
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel
import kotlin.math.roundToInt

/**
 * MapScreen - Pantalla de visualización de zonas de seguridad
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Map,
                            null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Mapa de Zonas",
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
                modifier = Modifier.fillMaxSize()
            ) {
                CurrentLocationCard(
                    currentLocation = currentLocation,
                    currentDangerZone = currentDangerZone
                )

                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Dangerous,
                        null,
                        tint = DangerRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zonas Peligrosas Registradas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dangerousZones) { zone ->
                        DangerZoneCard(
                            zone = zone,
                            currentLocation = currentLocation,
                            isCurrentZone = zone == currentDangerZone
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
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
    val isSafe = currentDangerZone == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isSafe) SafeGreen else DangerRed
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSafe)
                SafeGreenLight.copy(alpha = 0.3f)
            else
                Color(0xFFFFE5E5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (isSafe) SafeGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isSafe) SafeGreen else DangerRed
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu Ubicación Actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (currentLocation != null) {
                    Text(
                        text = "${String.format("%.5f", currentLocation.latitude)}, ${String.format("%.5f", currentLocation.longitude)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = if (isSafe) SafeGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSafe) "✓ Zona segura" else "⚠️ ${currentDangerZone?.name}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSafe) SafeGreen else DangerRed
                        )
                    }
                } else {
                    Text(
                        text = "Obteniendo ubicación...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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

    val dangerColor = when (zone.dangerLevel) {
        ZoneRepository.DangerLevel.CRITICAL -> DangerRed
        ZoneRepository.DangerLevel.HIGH -> WarningOrange
        ZoneRepository.DangerLevel.MEDIUM -> WarningBrown
        ZoneRepository.DangerLevel.LOW -> BeigeLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isCurrentZone) 12.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dangerColor
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentZone)
                dangerColor.copy(alpha = 0.15f)
            else
                PureWhite
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = dangerColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dangerous,
                        contentDescription = null,
                        tint = dangerColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DangerLevelBadge(zone.dangerLevel, dangerColor)

                        if (isCurrentZone) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = DangerRed,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "📍 Ubicación actual",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = SafeGreenLight.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.LocationOn,
                        label = "Coordenadas",
                        value = "${String.format("%.4f", zone.latitude)}\n${String.format("%.4f", zone.longitude)}",
                        color = SafeGreen
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    InfoChip(
                        icon = Icons.Default.RadioButtonUnchecked,
                        label = "Radio",
                        value = "${zone.radiusMeters.roundToInt()}m",
                        color = WarningBrown
                    )
                }
            }

            if (distance != null && !isCurrentZone) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = SafeGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            null,
                            tint = SafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "A ${formatDistance(distance)} de tu ubicación",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DangerLevelBadge(level: ZoneRepository.DangerLevel, color: Color) {
    val (emoji, text) = when (level) {
        ZoneRepository.DangerLevel.LOW -> "⚠️" to "Precaución"
        ZoneRepository.DangerLevel.MEDIUM -> "⚠️" to "Alerta"
        ZoneRepository.DangerLevel.HIGH -> "🚨" to "Peligro"
        ZoneRepository.DangerLevel.CRITICAL -> "🚨" to "CRÍTICO"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
    }
}

fun formatDistance(meters: Float): String {
    return if (meters < 1000) {
        "${meters.roundToInt()}m"
    } else {
        "${String.format("%.1f", meters / 1000)}km"
    }
}