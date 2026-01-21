package com.example.safezoneai.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.data.local.EmergencyRecord
import com.example.safezoneai.ui.theme.*
import com.example.safezoneai.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📜 PANTALLA DE HISTORIAL DE EMERGENCIAS
 * Muestra todas las emergencias registradas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onBack: () -> Unit
) {
    val records by viewModel.emergencyRecords.collectAsState()
    val recordCount by viewModel.recordCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Historial",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                "$recordCount emergencia(s)",
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
                    if (records.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.deleteAllRecords() }
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                "Eliminar todo",
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SafeGreen,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite,
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
            if (records.isEmpty()) {
                EmptyHistoryMessage()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(records) { record ->
                        EmergencyRecordCard(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record) }
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
fun EmptyHistoryMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                modifier = Modifier.size(80.dp),
                tint = SafeGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Sin emergencias registradas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Todas tus emergencias activadas aparecerán aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyRecordCard(
    record: EmergencyRecord,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val date = Date(record.timestamp)

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.dangerLevel == "HIGH" || record.dangerLevel == "CRITICAL")
                DangerRed.copy(alpha = 0.05f)
            else
                PureWhite
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Cabecera con fecha y hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = DangerRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = dateFormatter.format(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = timeFormatter.format(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        "Eliminar",
                        tint = DangerRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ubicación
            InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Ubicación",
                value = "${String.format("%.5f", record.latitude)}, ${String.format("%.5f", record.longitude)}"
            )

            // Zona peligrosa (si aplica)
            record.dangerZoneName?.let { zoneName ->
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    icon = Icons.Default.Dangerous,
                    label = "Zona",
                    value = zoneName
                )
            }

            // Contactos notificados
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                icon = Icons.Default.People,
                label = "Contactos notificados",
                value = "${record.contactsNotified} contacto(s)"
            )

            // Audio
            record.audioFilePath?.let {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    icon = Icons.Default.Mic,
                    label = "Audio",
                    value = "Grabado"
                )
            }

            // ✅ BOTÓN PARA VER EN MAPA - ACTUALIZADO
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    openGoogleMaps(
                        context = context,
                        latitude = record.latitude,
                        longitude = record.longitude
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafeGreen
                )
            ) {
                Icon(Icons.Default.Map, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver en mapa")
            }
        }
    }

    // Diálogo de confirmación de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = DangerRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("¿Eliminar registro?")
            },
            text = {
                Text("Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * ✅ FUNCIÓN PARA ABRIR GOOGLE MAPS
 *
 * Intenta:
 * 1. Abrir en la app de Google Maps (preferido)
 * 2. Si no está instalada, abre en el navegador
 */
fun openGoogleMaps(
    context: android.content.Context,
    latitude: Double,
    longitude: Double
) {
    try {
        // Intento 1: Abrir en la app de Google Maps
        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)

    } catch (e: Exception) {
        // Intento 2: Si Google Maps no está instalado, abrir en navegador
        try {
            val browserUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        } catch (ex: Exception) {
            ex.printStackTrace()
            // Si nada funciona, mostrar mensaje de error
            android.widget.Toast.makeText(
                context,
                "No se pudo abrir el mapa",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            null,
            tint = SafeGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }
    }
}