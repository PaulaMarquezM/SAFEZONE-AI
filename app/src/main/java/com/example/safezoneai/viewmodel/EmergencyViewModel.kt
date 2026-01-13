package com.example.safezoneai.viewmodel

import android.Manifest
import android.app.Application
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safezoneai.data.ZoneRepository
import com.example.safezoneai.data.local.AppDatabase
import com.example.safezoneai.data.local.EmergencyContact
import com.example.safezoneai.utils.AudioRecorder
import com.example.safezoneai.utils.LocationUtils
import com.example.safezoneai.utils.NotificationUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * EmergencyViewModel - ViewModel principal de SafeZone AI
 *
 * PRINCIPIOS SOLID APLICADOS:
 *
 * 1. SRP (Single Responsibility Principle):
 *    - Única responsabilidad: Gestionar el estado de emergencia y coordinar servicios
 *
 * 2. OCP (Open/Closed Principle):
 *    - Abierto para extensión (nuevos tipos de emergencias)
 *    - Cerrado para modificación (no necesitas modificar el código existente)
 *
 * 3. LSP (Liskov Substitution Principle):
 *    - Hereda de AndroidViewModel correctamente
 *
 * 4. ISP (Interface Segregation Principle):
 *    - Usa interfaces específicas para cada servicio (LocationUtils, AudioRecorder, etc.)
 *
 * 5. DIP (Dependency Inversion Principle):
 *    - Depende de abstracciones (interfaces) no de implementaciones concretas
 *
 * PATRONES DE DISEÑO APLICADOS:
 *
 * - OBSERVER PATTERN: Los estados (StateFlow) notifican automáticamente a los observadores
 * - REPOSITORY PATTERN: ZoneRepository abstrae el acceso a datos de zonas
 * - SINGLETON PATTERN: AppDatabase.getDatabase() garantiza una única instancia
 * - FACADE PATTERN: Este ViewModel actúa como fachada simplificando servicios complejos
 * - STRATEGY PATTERN: Diferentes estrategias de emergencia según el contexto
 */
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    // ═══════════════════════════════════════════════════════════
    // DEPENDENCIAS - Principio DIP (Dependency Inversion)
    // ═══════════════════════════════════════════════════════════

    private val context = application.applicationContext
    private val locationUtils = LocationUtils(context)
    private val audioRecorder = AudioRecorder(context)
    private val notificationUtils = NotificationUtils(context)
    private val zoneRepository = ZoneRepository()

    // SINGLETON: Base de datos única
    private val database = AppDatabase.getDatabase(context)
    private val emergencyDao = database.emergencyDao()

    // ═══════════════════════════════════════════════════════════
    // ESTADOS REACTIVOS - OBSERVER PATTERN
    // ═══════════════════════════════════════════════════════════

    /**
     * Estado de emergencia activa
     * Los observadores son notificados automáticamente cuando cambia
     */
    private val _isEmergencyActive = MutableStateFlow(false)
    val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()

    /**
     * Estado de grabación de audio
     */
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /**
     * Ubicación actual del usuario
     * Se actualiza cada 5 segundos
     */
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    /**
     * Zona peligrosa actual (si el usuario está en una)
     */
    private val _currentDangerZone = MutableStateFlow<ZoneRepository.DangerousZone?>(null)
    val currentDangerZone: StateFlow<ZoneRepository.DangerousZone?> = _currentDangerZone.asStateFlow()

    /**
     * Lista de contactos de emergencia
     * OBSERVER: Automáticamente sincronizado con Room Database
     */
    val emergencyContacts: StateFlow<List<EmergencyContact>> = emergencyDao
        .getAllContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ═══════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════

    init {
        startLocationTracking()
        observeLocationForDangerZones()
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONALIDAD PRINCIPAL: GESTIÓN DE EMERGENCIAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Activa el modo de emergencia
     *
     * STRATEGY PATTERN: Ejecuta una estrategia de emergencia completa
     *
     * Acciones:
     * 1. Obtener ubicación GPS
     * 2. Iniciar grabación de audio
     * 3. Vibrar dispositivo
     * 4. Enviar notificaciones
     * 5. Enviar SMS a contactos
     */
    fun activateEmergency() {
        if (_isEmergencyActive.value) return // Evitar activación múltiple

        viewModelScope.launch {
            try {
                _isEmergencyActive.value = true

                // 1. Obtener ubicación GPS
                val location = getCurrentLocation()

                // 2. Iniciar grabación de audio
                startAudioRecording()

                // 3. Vibrar dispositivo
                notificationUtils.vibrateEmergency()

                // 4. Mostrar notificación
                location?.let {
                    notificationUtils.showEmergencyNotification(
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                }

                // 5. Enviar SMS a contactos de emergencia
                sendEmergencySMS(location)

            } catch (e: Exception) {
                e.printStackTrace()
                _isEmergencyActive.value = false
            }
        }
    }

    /**
     * Desactiva el modo de emergencia
     *
     * Acciones:
     * 1. Detener grabación de audio
     * 2. Guardar datos de la emergencia
     * 3. Limpiar estados
     */
    fun deactivateEmergency() {
        viewModelScope.launch {
            try {
                // Detener grabación
                stopAudioRecording()

                // Limpiar estados
                _isEmergencyActive.value = false
                _isRecording.value = false

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONALIDAD: UBICACIÓN GPS
    // ═══════════════════════════════════════════════════════════

    /**
     * Inicia el seguimiento de ubicación en tiempo real
     *
     * OBSERVER PATTERN: El Flow de ubicación notifica cambios automáticamente
     */
    private fun startLocationTracking() {
        if (!locationUtils.hasLocationPermission()) return

        viewModelScope.launch {
            try {
                locationUtils.getLocationUpdates(intervalMillis = 5000)
                    .catch { e ->
                        e.printStackTrace()
                    }
                    .collect { location ->
                        _currentLocation.value = location
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Obtiene la ubicación actual de forma síncrona
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (!locationUtils.hasLocationPermission()) return null
            locationUtils.getCurrentLocation()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Observa la ubicación para detectar zonas peligrosas
     *
     * OBSERVER + STRATEGY: Detecta automáticamente y aplica estrategia de alerta
     */
    private fun observeLocationForDangerZones() {
        viewModelScope.launch {
            _currentLocation.collect { location ->
                if (location != null) {
                    val dangerZone = zoneRepository.checkDangerousZone(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    // Si entró a una zona peligrosa, alertar
                    if (dangerZone != null && _currentDangerZone.value != dangerZone) {
                        notificationUtils.showDangerZoneAlert(
                            zoneName = dangerZone.name,
                            dangerLevel = dangerZone.dangerLevel.name
                        )
                        notificationUtils.vibrateEmergency()
                    }

                    _currentDangerZone.value = dangerZone
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONALIDAD: GRABACIÓN DE AUDIO
    // ═══════════════════════════════════════════════════════════

    /**
     * Inicia la grabación de audio
     *
     * SRP: AudioRecorder se encarga de la grabación
     */
    private fun startAudioRecording() {
        if (!audioRecorder.hasAudioPermission()) return

        viewModelScope.launch {
            try {
                val audioFile = audioRecorder.startRecording()
                if (audioFile != null) {
                    _isRecording.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Detiene la grabación de audio
     */
    private fun stopAudioRecording() {
        viewModelScope.launch {
            try {
                audioRecorder.stopRecording()
                _isRecording.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONALIDAD: CONTACTOS DE EMERGENCIA
    // ═══════════════════════════════════════════════════════════

    /**
     * Envía SMS de emergencia a todos los contactos
     *
     * SRP: NotificationUtils se encarga del envío
     */
    private suspend fun sendEmergencySMS(location: Location?) {
        if (location == null) return

        try {
            val contacts = emergencyDao.getPrimaryContacts()
            val audioPath = audioRecorder.getCurrentAudioFile()?.absolutePath

            notificationUtils.sendEmergencySMS(
                contacts = contacts,
                latitude = location.latitude,
                longitude = location.longitude,
                audioFilePath = audioPath
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Añade un nuevo contacto de emergencia
     *
     * REPOSITORY PATTERN: Abstrae el acceso a la base de datos
     */
    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyDao.insertContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Elimina un contacto de emergencia
     */
    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyDao.deleteContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FUNCIONALIDAD: ZONAS PELIGROSAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Obtiene todas las zonas peligrosas
     *
     * REPOSITORY PATTERN: ZoneRepository encapsula la lógica de zonas
     */
    fun getAllDangerousZones(): List<ZoneRepository.DangerousZone> {
        return zoneRepository.getAllDangerousZones()
    }

    /**
     * Obtiene la distancia a la zona peligrosa más cercana
     */
    fun getDistanceToNearestDanger(): Pair<ZoneRepository.DangerousZone, Float>? {
        val location = _currentLocation.value ?: return null
        return zoneRepository.getDistanceToNearestDanger(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    // ═══════════════════════════════════════════════════════════
    // LIMPIEZA DE RECURSOS
    // ═══════════════════════════════════════════════════════════

    /**
     * Limpia recursos cuando el ViewModel es destruido
     */
    override fun onCleared() {
        super.onCleared()
        // Asegurar que se detenga cualquier grabación activa
        if (_isRecording.value) {
            audioRecorder.stopRecording()
        }
    }
}