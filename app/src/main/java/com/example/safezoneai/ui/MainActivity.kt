package com.example.safezoneai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safezoneai.ui.EmergencyScreen
import com.example.safezoneai.ui.HomeScreen
import com.example.safezoneai.ui.MapScreen
import com.example.safezoneai.ui.theme.SafeZoneAITheme
import com.example.safezoneai.viewmodel.EmergencyViewModel

/**
 * Actividad principal de SafeZone AI.
 * Gestiona la navegación entre pantallas y los permisos necesarios.
 */
class MainActivity : ComponentActivity() {

    // Launcher para solicitar permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Algunos permisos fueron denegados", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos al iniciar
        checkAndRequestPermissions()

        setContent {
            SafeZoneAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeZoneApp()
                }
            }
        }
    }

    /**
     * Verifica y solicita los permisos necesarios.
     */
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.VIBRATE
        )

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Filtrar permisos que no están concedidos
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

/**
 * Composable principal con navegación entre pantallas.
 */
@Composable
fun SafeZoneApp() {
    var currentScreen by remember { mutableStateOf("home") }
    val viewModel: EmergencyViewModel = viewModel()

    when (currentScreen) {
        "home" -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMap = { currentScreen = "map" },
                onNavigateToEmergency = { currentScreen = "emergency" }
            )
        }

        "emergency" -> {
            EmergencyScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "home" }
            )
        }

        "map" -> {
            MapScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "home" }
            )
        }
    }
}