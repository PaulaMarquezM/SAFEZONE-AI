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
 * MainActivity - Actividad principal de SafeZone AI
 *
 * PATRÓN DE DISEÑO: Front Controller Pattern
 * Esta actividad actúa como punto de entrada único de la aplicación,
 * gestionando la navegación y los permisos.
 *
 * PRINCIPIO SOLID: Single Responsibility Principle (SRP)
 * Responsabilidad única: Gestionar el ciclo de vida de la aplicación,
 * solicitar permisos y coordinar la navegación.
 *
 * ARQUITECTURA: MVVM (Model-View-ViewModel)
 * - View: Composables (HomeScreen, EmergencyScreen, MapScreen)
 * - ViewModel: EmergencyViewModel
 * - Model: Data layer (Room Database, Repositories)
 */
class MainActivity : ComponentActivity() {

    // ═══════════════════════════════════════════════════════════
    // GESTIÓN DE PERMISOS
    // ═══════════════════════════════════════════════════════════

    /**
     * PATRÓN: Callback Pattern
     * Launcher para solicitar múltiples permisos a la vez
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            Toast.makeText(
                this,
                "✓ Permisos concedidos correctamente",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys
            Toast.makeText(
                this,
                "⚠️ Algunos permisos fueron denegados: ${deniedPermissions.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos necesarios
        checkAndRequestPermissions()

        // Configurar interfaz con Jetpack Compose
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

    // ═══════════════════════════════════════════════════════════
    // GESTIÓN DE PERMISOS
    // ═══════════════════════════════════════════════════════════

    /**
     * Verifica y solicita los permisos necesarios para la aplicación.
     *
     * PRINCIPIO: Defensive Programming
     * Verifica cada permiso antes de solicitarlo, evitando solicitudes innecesarias.
     *
     * Permisos requeridos:
     * - ACCESS_FINE_LOCATION: Ubicación GPS precisa
     * - ACCESS_COARSE_LOCATION: Ubicación aproximada
     * - RECORD_AUDIO: Grabación de audio de emergencia
     * - SEND_SMS: Envío de alertas por SMS
     * - VIBRATE: Feedback háptico
     * - POST_NOTIFICATIONS: Notificaciones (Android 13+)
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

        // Filtrar solo los permisos que NO están concedidos
        // PRINCIPIO: Don't Repeat Yourself (DRY)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        // Solicitar solo los permisos necesarios
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos
     * (Método legacy, mantenido por compatibilidad)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (!allGranted) {
            Toast.makeText(
                this,
                "⚠️ La aplicación requiere todos los permisos para funcionar correctamente",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

// ═══════════════════════════════════════════════════════════
// NAVEGACIÓN DE LA APLICACIÓN
// ═══════════════════════════════════════════════════════════

/**
 * Composable principal con sistema de navegación.
 *
 * PATRÓN DE DISEÑO: State Pattern
 * El comportamiento de la aplicación cambia según la pantalla actual.
 *
 * PATRÓN DE DISEÑO: Strategy Pattern
 * Cada pantalla implementa su propia estrategia de visualización.
 *
 * Navegación simple basada en estado String:
 * - "home" -> HomeScreen
 * - "emergency" -> EmergencyScreen
 * - "map" -> MapScreen
 *
 * NOTA: En una aplicación más grande, se recomienda usar
 * Navigation Compose o Voyager para navegación más robusta.
 */
@Composable
fun SafeZoneApp() {
    // Estado de navegación
    // PATRÓN: State Management
    var currentScreen by remember { mutableStateOf("home") }

    // ViewModel compartido entre pantallas
    // PRINCIPIO SOLID: Dependency Inversion Principle (DIP)
    // Las vistas dependen de la abstracción (ViewModel) no de implementaciones concretas
    val viewModel: EmergencyViewModel = viewModel()

    // Sistema de navegación basado en estado
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

/**
 * VENTAJAS DE ESTA ARQUITECTURA:
 *
 * 1. SEPARACIÓN DE RESPONSABILIDADES:
 *    - MainActivity: Gestión de permisos y ciclo de vida
 *    - SafeZoneApp: Navegación
 *    - Screens: Presentación
 *    - ViewModel: Lógica de negocio
 *    - Repository: Acceso a datos
 *
 * 2. TESTABILIDAD:
 *    - Cada componente puede ser testeado independientemente
 *    - ViewModel no depende de Android Framework (excepto Application)
 *
 * 3. MANTENIBILIDAD:
 *    - Cambios en una capa no afectan a las demás
 *    - Código organizado y fácil de entender
 *
 * 4. ESCALABILIDAD:
 *    - Fácil agregar nuevas pantallas
 *    - Fácil agregar nueva funcionalidad
 *
 * 5. CUMPLIMIENTO SOLID:
 *    - SRP: Cada clase tiene una responsabilidad
 *    - OCP: Abierto a extensión, cerrado a modificación
 *    - LSP: Las subclases pueden sustituir a sus clases base
 *    - ISP: Interfaces específicas por cliente
 *    - DIP: Dependencia de abstracciones
 */