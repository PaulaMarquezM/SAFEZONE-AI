package com.example.safezoneai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.safezoneai.service.LocationTrackingService
import com.example.safezoneai.service.VolumeEmergencyService
import com.example.safezoneai.ui.AnimatedSplashScreen
import com.example.safezoneai.ui.ContactsScreen
import com.example.safezoneai.ui.EmergencyScreen
import com.example.safezoneai.ui.HistoryScreen
import com.example.safezoneai.ui.HomeScreen
import com.example.safezoneai.ui.MapScreen
import com.example.safezoneai.ui.SettingsScreen
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
 * - View: Composables (HomeScreen, EmergencyScreen, MapScreen, ContactsScreen)
 * - ViewModel: EmergencyViewModel
 * - Model: Data layer (Room Database, Repositories)
 */
class MainActivity : ComponentActivity() {

    // ═══════════════════════════════════════════════════════════
    // VIEWMODEL (compartido con Composables)
    // ═══════════════════════════════════════════════════════════

    private val emergencyViewModel: EmergencyViewModel by viewModels()

    // ═══════════════════════════════════════════════════════════
    // DETECCIÓN DE BOTÓN DE VOLUMEN (3 presses = emergencia)
    // ═══════════════════════════════════════════════════════════

    private val volumePressTimestamps = mutableListOf<Long>()
    private val requiredPresses = 3
    private val timeWindowMs = 2000L // 2 segundos para las 3 pulsaciones

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val now = System.currentTimeMillis()

            // Limpiar pulsaciones antiguas fuera de la ventana de tiempo
            volumePressTimestamps.removeAll { now - it > timeWindowMs }
            volumePressTimestamps.add(now)

            if (volumePressTimestamps.size >= requiredPresses) {
                volumePressTimestamps.clear()
                // Activar emergencia desde botón de volumen
                emergencyViewModel.triggerEmergencyFromVolume()
                Toast.makeText(this, "EMERGENCIA ACTIVADA", Toast.LENGTH_SHORT).show()
                return true // Consumir el evento, no cambia volumen
            }
        }
        return super.onKeyDown(keyCode, event)
    }

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
                "✅ Permisos concedidos correctamente",
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

        // Después de los permisos base, solicitar background location si aplica
        requestBackgroundLocationIfNeeded()
    }

    /**
     * Launcher separado para ACCESS_BACKGROUND_LOCATION (Android requiere pedirlo aparte)
     */
    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startTrackingServiceIfEnabled()
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CICLO DE VIDA
    // ═══════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        // Iniciar servicio de deteccion de volumen en background
        VolumeEmergencyService.start(this)

        // Si viene de un intent de emergencia por volumen (app estaba en background)
        handleEmergencyIntent(intent)

        setContent {
            SafeZoneAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafeZoneApp(viewModel = emergencyViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleEmergencyIntent(intent)
    }

    private fun handleEmergencyIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("TRIGGER_EMERGENCY", false) == true) {
            emergencyViewModel.triggerEmergencyFromVolume()
            Toast.makeText(this, "EMERGENCIA ACTIVADA", Toast.LENGTH_SHORT).show()
            // Limpiar el extra para no re-triggear
            intent.removeExtra("TRIGGER_EMERGENCY")
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
     * Solicita ACCESS_BACKGROUND_LOCATION si el usuario tiene monitoreo activado.
     * Android requiere que se pida por separado de los permisos normales.
     */
    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val prefs = getSharedPreferences("safezone_settings", Context.MODE_PRIVATE)
        val trackingEnabled = prefs.getBoolean("background_tracking_enabled", false)
        if (!trackingEnabled) return

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation && !hasBackgroundLocation) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    /**
     * Arranca el servicio de monitoreo si está habilitado en settings y tiene permisos.
     */
    private fun startTrackingServiceIfEnabled() {
        val prefs = getSharedPreferences("safezone_settings", Context.MODE_PRIVATE)
        val trackingEnabled = prefs.getBoolean("background_tracking_enabled", false)
        if (!trackingEnabled) return

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation) {
            LocationTrackingService.start(this)
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
 * - "contacts" -> ContactsScreen ✅ NUEVO
 *
 * NOTA: En una aplicación más grande, se recomienda usar
 * Navigation Compose o Voyager para navegación más robusta.
 */
// ✅ REEMPLAZAR SafeZoneApp() COMPLETO EN MainActivity.kt

@Composable
fun SafeZoneApp(viewModel: EmergencyViewModel) {

    var currentScreen by remember { mutableStateOf("splash") }

    // Observar trigger de emergencia por botón de volumen (desde ViewModel)
    val volumeTriggered by viewModel.volumeEmergencyTriggered.collectAsState()

    // Observar trigger de emergencia desde el servicio de background
    val serviceTriggered by VolumeEmergencyService.emergencyTriggered.collectAsState()

    LaunchedEffect(volumeTriggered) {
        if (volumeTriggered) {
            currentScreen = "emergency"
            viewModel.clearVolumeEmergencyTrigger()
        }
    }

    LaunchedEffect(serviceTriggered) {
        if (serviceTriggered) {
            if (!viewModel.isEmergencyActive.value) {
                viewModel.activateEmergency()
            }
            currentScreen = "emergency"
            VolumeEmergencyService.clearTrigger()
        }
    }

    when (currentScreen) {

        "splash" -> {
            AnimatedSplashScreen(
                onFinished = { currentScreen = "home" }
            )
        }

        "home" -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToMap = { currentScreen = "map" },
                onNavigateToEmergency = { currentScreen = "emergency" },
                onNavigateToContacts = { currentScreen = "contacts" },
                onNavigateToSettings = { currentScreen = "settings" },
                onNavigateToHistory = { currentScreen = "history" }
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

        "contacts" -> {
            ContactsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "home" }
            )
        }

        "settings" -> {
            SettingsScreen(
                onBack = { currentScreen = "home" }
            )
        }

        "history" -> {
            HistoryScreen(
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
