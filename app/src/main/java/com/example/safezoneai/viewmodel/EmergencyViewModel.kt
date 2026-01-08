package com.example.safezoneai.viewmodel

import android.app.Application
import android.location.Location
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
import java.io.File

/**
 * ViewModel principal que gestiona la lógica de emergencias y seguridad.
 * Coordina la ubicación, audio, notificaciones y detección de zonas.
 */
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val locationUtils = LocationUtils(application)
    private val audioRecorder = AudioRecorder(application)
    private val notificationUtils = NotificationUtils(application)
    private val zoneRepository = ZoneRepository()
    private val database = AppDatabase.getDatabase(application)

    // Estado de la ubicación actual
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // Estado de zona peligrosa actual
    private val _currentDangerZone = MutableStateFlow<ZoneRepository.DangerousZone?>(null)
    val currentDangerZone: StateFlow<ZoneRepository.DangerousZone?> = _currentDangerZone.asStateFlow()

    // Estado de emergencia activa
    private val _isEmergencyActive = MutableStateFlow(false)
    val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()

    // Estado de grabación de audio
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Archivo de audio actual
    private val _currentAudioFile = MutableStateFlow<File?>(null)
    val currentAudioFile: StateFlow<File?> = _currentAudioFile.asStateFlow()

    // Contactos de emergencia
    val emergencyContacts: StateFlow<List<EmergencyContact>> =
        database.emergencyDao().getAllContacts()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Estado de alerta mostrada (evita alertas repetidas)
    private var lastAlertedZone: String? = null

    init {
        startLocationMonitoring()
        insertDefaultContactsIfEmpty()
    }

    /**
     * Inicia el monitoreo de ubicación en tiempo real.
     */
    private fun startLocationMonitoring() {
        viewModelScope.launch {
            locationUtils.getLocationUpdates(5000L)
                .catch { e -> e.printStackTrace() }
                .collect { location ->
                    _currentLocation.value = location
                    checkDangerousZone(location)
                }
        }
    }

    /**
     * Verifica si la ubicación actual está en una zona peligrosa.
     */
    private fun checkDangerousZone(location: Location) {
        val dangerZone = zoneRepository.checkDangerousZone(
            location.latitude,
            location.longitude
        )

        _currentDangerZone.value = dangerZone

        // Mostrar alerta solo si es una zona nueva
        if (dangerZone != null && dangerZone.name != lastAlertedZone) {
            lastAlertedZone = dangerZone.name
            notificationUtils.showDangerZoneAlert(
                dangerZone.name,
                dangerZone.dangerLevel.name
            )
        } else if (dangerZone == null) {
            lastAlertedZone = null
        }
    }

    /**
     * Activa el modo de emergencia.
     * - Inicia grabación de audio
     * - Obtiene ubicación actual
     * - Envía alertas a contactos
     * - Vibra el dispositivo
     */
    fun activateEmergency() {
        if (_isEmergencyActive.value) return

        viewModelScope.launch {
            _isEmergencyActive.value = true

            // 1. Iniciar grabación de audio
            val audioFile = audioRecorder.startRecording()
            _isRecording.value = audioRecorder.isRecording()
            _currentAudioFile.value = audioFile

            // 2. Obtener ubicación actual
            val location = locationUtils.getCurrentLocation() ?: _currentLocation.value

            if (location != null) {
                // 3. Vibrar dispositivo
                notificationUtils.vibrateEmergency()

                // 4. Mostrar notificación
                notificationUtils.showEmergencyNotification(
                    location.latitude,
                    location.longitude
                )

                // 5. Enviar SMS a contactos
                val contacts = emergencyContacts.value
                if (contacts.isNotEmpty()) {
                    notificationUtils.sendEmergencySMS(
                        contacts,
                        location.latitude,
                        location.longitude,
                        audioFile?.absolutePath
                    )
                }
            }
        }
    }

    /**
     * Desactiva el modo de emergencia.
     */
    fun deactivateEmergency() {
        viewModelScope.launch {
            // Detener grabación si está activa
            if (_isRecording.value) {
                val finalAudioFile = audioRecorder.stopRecording()
                _currentAudioFile.value = finalAudioFile
                _isRecording.value = false
            }

            _isEmergencyActive.value = false
        }
    }

    /**
     * Obtiene la ubicación actual de forma síncrona.
     */
    suspend fun getCurrentLocationOnce(): Location? {
        return locationUtils.getCurrentLocation()
    }

    /**
     * Obtiene todas las zonas peligrosas (para mostrar en el mapa).
     */
    fun getAllDangerousZones(): List<ZoneRepository.DangerousZone> {
        return zoneRepository.getAllDangerousZones()
    }

    /**
     * Agrega un contacto de emergencia.
     */
    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            database.emergencyDao().insertContact(contact)
        }
    }

    /**
     * Elimina un contacto de emergencia.
     */
    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            database.emergencyDao().deleteContact(contact)
        }
    }

    /**
     * Inserta contactos por defecto si la BD está vacía (solo para demo).
     */
    private fun insertDefaultContactsIfEmpty() {
        viewModelScope.launch {
            val contacts = database.emergencyDao().getAllContacts().first()
            if (contacts.isEmpty()) {
                database.emergencyDao().insertContacts(
                    listOf(
                        EmergencyContact(
                            name = "Policía",
                            phoneNumber = "911",
                            relationship = "Autoridad",
                            isPrimary = true
                        ),
                        EmergencyContact(
                            name = "Contacto Familiar",
                            phoneNumber = "+521234567890",
                            relationship = "Familia",
                            isPrimary = true
                        )
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Asegurar que la grabación se detenga al cerrar la app
        if (_isRecording.value) {
            audioRecorder.stopRecording()
        }
    }
}