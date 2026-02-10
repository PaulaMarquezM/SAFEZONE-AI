package com.example.safezoneai.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.EmergencyViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.cos

/**
 * 🗺️ MapScreen - OpenStreetMap GRATIS
 * Sin API key, sin costos, 100% funcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val currentDangerZone by viewModel.currentDangerZone.collectAsState()
    val detectedCity by viewModel.detectedCity.collectAsState()
    val isLoadingZones by viewModel.isLoadingZones.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val dangerousZones = detectedCity?.dangerousZones ?: emptyList()

    // 📱 Estados
    var showMapView by remember { mutableStateOf(true) }

    // ⚙️ Configurar osmdroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                        detectedCity?.let { city ->
                            Text(
                                "${city.name}, ${city.country}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showMapView = !showMapView }) {
                        Icon(
                            if (showMapView) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMapView) "Ver lista" else "Ver mapa",
                            tint = PureWhite
                        )
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
                .padding(padding)
        ) {
            if (isLoadingZones) {
                LoadingView()
            } else if (dangerousZones.isEmpty()) {
                EmptyZonesView(locationError = locationError)
            } else {
                if (showMapView) {
                    // 🗺️ VISTA DE MAPA
                    OSMMapView(
                        dangerousZones = dangerousZones,
                        currentLocation = currentLocation,
                        currentDangerZone = currentDangerZone,
                        detectedCity = detectedCity
                    )
                } else {
                    // 📋 VISTA DE LISTA
                    ListView(
                        dangerousZones = dangerousZones,
                        currentLocation = currentLocation,
                        currentDangerZone = currentDangerZone,
                        detectedCity = detectedCity
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 🗺️ COMPONENTE DE MAPA OPENSTREETMAP
// ═══════════════════════════════════════════════════════════

@Composable
fun OSMMapView(
    dangerousZones: List<SmartZoneDetector.DangerousZone>,
    currentLocation: android.location.Location?,
    currentDangerZone: SmartZoneDetector.DangerousZone?,
    detectedCity: SmartZoneDetector.DetectedCity?
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 🗺️ Mapa de OpenStreetMap
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // 🎯 Centrar en ubicación actual o ciudad detectada
                    val centerPoint = currentLocation?.let {
                        GeoPoint(it.latitude, it.longitude)
                    } ?: detectedCity?.let {
                        GeoPoint(it.latitude, it.longitude)
                    } ?: GeoPoint(-2.9006, -79.0047) // Cuenca por defecto

                    controller.setZoom(14.0)
                    controller.setCenter(centerPoint)

                    // 📍 Overlay de ubicación actual
                    val locationOverlay = MyLocationNewOverlay(
                        GpsMyLocationProvider(ctx),
                        this
                    ).apply {
                        enableMyLocation()
                        enableFollowLocation()
                    }
                    overlays.add(locationOverlay)

                    // 🎯 Agregar círculos de zonas
                    dangerousZones.forEach { zone ->
                        val circle = Polygon(this).apply {
                            points = createCirclePoints(
                                GeoPoint(zone.latitude, zone.longitude),
                                zone.radiusMeters.toDouble()
                            )

                            // 🎨 Color según nivel de peligro
                            val (fillColor, strokeColor) = when (zone.dangerLevel) {
                                SmartZoneDetector.DangerLevel.SAFE ->
                                    Pair(
                                        AndroidColor.argb(50, 76, 175, 80),  // Verde transparente
                                        AndroidColor.rgb(76, 175, 80)         // Verde sólido
                                    )
                                SmartZoneDetector.DangerLevel.LOW ->
                                    Pair(
                                        AndroidColor.argb(50, 255, 235, 59),
                                        AndroidColor.rgb(255, 235, 59)
                                    )
                                SmartZoneDetector.DangerLevel.MEDIUM ->
                                    Pair(
                                        AndroidColor.argb(50, 255, 152, 0),
                                        AndroidColor.rgb(255, 152, 0)
                                    )
                                SmartZoneDetector.DangerLevel.HIGH ->
                                    Pair(
                                        AndroidColor.argb(50, 255, 87, 34),
                                        AndroidColor.rgb(255, 87, 34)
                                    )
                                SmartZoneDetector.DangerLevel.CRITICAL ->
                                    Pair(
                                        AndroidColor.argb(50, 244, 67, 54),
                                        AndroidColor.rgb(244, 67, 54)
                                    )
                            }

                            this.fillPaint.color = fillColor
                            this.outlinePaint.color = strokeColor
                            this.outlinePaint.strokeWidth = 3f
                        }
                        overlays.add(circle)

                        // 📌 Marcador central de zona
                        val marker = Marker(this).apply {
                            position = GeoPoint(zone.latitude, zone.longitude)
                            title = zone.name
                            snippet = "${zone.dangerLevel.name} - ${zone.radiusMeters.toInt()}m"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        overlays.add(marker)
                    }

                    invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 🎴 Cards flotantes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            CurrentLocationFloatingCard(
                currentLocation = currentLocation,
                currentDangerZone = currentDangerZone,
                detectedCity = detectedCity
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            ZoneStatisticsFloatingCard(dangerousZones)

            Spacer(modifier = Modifier.height(8.dp))

            // Botón para abrir en Google Maps
            currentLocation?.let { location ->
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://maps.google.com/?q=${location.latitude},${location.longitude}")
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    containerColor = SafeGreen,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.OpenInNew, "Abrir en Maps", tint = PureWhite)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 🔧 FUNCIÓN AUXILIAR: Crear puntos de círculo
// ═══════════════════════════════════════════════════════════

fun createCirclePoints(center: GeoPoint, radiusMeters: Double): List<GeoPoint> {
    val points = mutableListOf<GeoPoint>()
    val earthRadius = 6371000.0 // Radio de la Tierra en metros
    val numPoints = 64 // Número de puntos para el círculo

    for (i in 0..numPoints) {
        val angle = Math.toRadians((i * 360.0) / numPoints)

        val dx = radiusMeters * cos(angle)
        val dy = radiusMeters * Math.sin(angle)

        val deltaLat = dy / earthRadius
        val deltaLon = dx / (earthRadius * cos(Math.toRadians(center.latitude)))

        val lat = center.latitude + Math.toDegrees(deltaLat)
        val lon = center.longitude + Math.toDegrees(deltaLon)

        points.add(GeoPoint(lat, lon))
    }

    return points
}

// ═══════════════════════════════════════════════════════════
// 🎴 CARDS FLOTANTES
// ═══════════════════════════════════════════════════════════

@Composable
fun CurrentLocationFloatingCard(
    currentLocation: android.location.Location?,
    currentDangerZone: SmartZoneDetector.DangerousZone?,
    detectedCity: SmartZoneDetector.DetectedCity?
) {
    val isSafe = currentDangerZone == null ||
            currentDangerZone.dangerLevel == SmartZoneDetector.DangerLevel.SAFE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSafe)
                SafeGreenLight.copy(alpha = 0.95f)
            else
                Color(0xFFFFE5E5).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                    .background(
                        color = if (isSafe) SafeGreen else DangerRed,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = PureWhite
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSafe) "✓ Zona Segura" else "⚠️ Zona Peligrosa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSafe) SafeGreen else DangerRed
                )

                if (currentDangerZone != null && !isSafe) {
                    Text(
                        text = currentDangerZone.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark
                    )
                } else {
                    Text(
                        text = detectedCity?.name ?: "Ubicación desconocida",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ZoneStatisticsFloatingCard(zones: List<SmartZoneDetector.DangerousZone>) {
    val safeCount = zones.count { it.dangerLevel == SmartZoneDetector.DangerLevel.SAFE }
    val criticalCount = zones.count { it.dangerLevel == SmartZoneDetector.DangerLevel.CRITICAL }
    val highCount = zones.count { it.dangerLevel == SmartZoneDetector.DangerLevel.HIGH }
    val mediumCount = zones.count { it.dangerLevel == SmartZoneDetector.DangerLevel.MEDIUM }
    val lowCount = zones.count { it.dangerLevel == SmartZoneDetector.DangerLevel.LOW }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (safeCount > 0) StatChip("✅", safeCount, SafeGreen)
            if (lowCount > 0) StatChip("ℹ️", lowCount, BeigeLight)
            if (mediumCount > 0) StatChip("⚠️", mediumCount, WarningBrown)
            if (highCount > 0) StatChip("🚨", highCount, WarningOrange)
            if (criticalCount > 0) StatChip("⛔", criticalCount, DangerRed)
        }
    }
}

@Composable
fun StatChip(emoji: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 📋 VISTA DE LISTA
// ═══════════════════════════════════════════════════════════

@Composable
fun ListView(
    dangerousZones: List<SmartZoneDetector.DangerousZone>,
    currentLocation: android.location.Location?,
    currentDangerZone: SmartZoneDetector.DangerousZone?,
    detectedCity: SmartZoneDetector.DetectedCity?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CurrentLocationCard(
                currentLocation = currentLocation,
                currentDangerZone = currentDangerZone,
                detectedCity = detectedCity
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
                    text = "Zonas Monitoreadas (${dangerousZones.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(dangerousZones.size) { index ->
                    DangerZoneCard(
                        zone = dangerousZones[index],
                        currentLocation = currentLocation,
                        isCurrentZone = dangerousZones[index] == currentDangerZone
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun CurrentLocationCard(
    currentLocation: android.location.Location?,
    currentDangerZone: SmartZoneDetector.DangerousZone?,
    detectedCity: SmartZoneDetector.DetectedCity?
) {
    val isSafe = currentDangerZone == null ||
            currentDangerZone.dangerLevel == SmartZoneDetector.DangerLevel.SAFE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
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
    zone: SmartZoneDetector.DangerousZone,
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
        SmartZoneDetector.DangerLevel.SAFE -> SafeGreen
        SmartZoneDetector.DangerLevel.CRITICAL -> DangerRed
        SmartZoneDetector.DangerLevel.HIGH -> WarningOrange
        SmartZoneDetector.DangerLevel.MEDIUM -> WarningBrown
        SmartZoneDetector.DangerLevel.LOW -> BeigeLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentZone)
                dangerColor.copy(alpha = 0.15f)
            else
                PureWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentZone) 8.dp else 2.dp
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
                        imageVector = if (zone.dangerLevel == SmartZoneDetector.DangerLevel.SAFE)
                            Icons.Default.Shield
                        else
                            Icons.Default.Dangerous,
                        contentDescription = null,
                        tint = dangerColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = zone.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DangerLevelBadge(zone.dangerLevel, dangerColor)
                }
            }

            if (zone.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = zone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            }

            if (distance != null && !isCurrentZone) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "📍 A ${formatDistance(distance)} de tu ubicación",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun DangerLevelBadge(level: SmartZoneDetector.DangerLevel, color: Color) {
    val (emoji, text) = when (level) {
        SmartZoneDetector.DangerLevel.SAFE -> "✅" to "Segura"
        SmartZoneDetector.DangerLevel.LOW -> "ℹ️" to "Precaución"
        SmartZoneDetector.DangerLevel.MEDIUM -> "⚠️" to "Alerta"
        SmartZoneDetector.DangerLevel.HIGH -> "🚨" to "Peligro"
        SmartZoneDetector.DangerLevel.CRITICAL -> "🚨" to "CRÍTICO"
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
            Text(text = emoji, fontSize = 14.sp)
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

fun formatDistance(meters: Float): String {
    return if (meters < 1000) {
        "${meters.toInt()}m"
    } else {
        "${String.format("%.1f", meters / 1000)}km"
    }
}

// ═══════════════════════════════════════════════════════════
// 🔄 VISTAS DE LOADING Y EMPTY
// ═══════════════════════════════════════════════════════════

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = SafeGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Cargando zonas...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun EmptyZonesView(locationError: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                Icons.Default.LocationOff,
                null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No se detectaron zonas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                locationError ?: "Tu ciudad podría no estar en nuestra base de datos aún",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
