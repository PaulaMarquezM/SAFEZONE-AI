package com.example.safezoneai.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.data.local.AppDatabase
import com.example.safezoneai.data.local.EmergencyContact
import com.example.safezoneai.data.local.EmergencyRecord
import com.example.safezoneai.utils.AudioRecorder
import com.example.safezoneai.utils.LocationUtils
import com.example.safezoneai.utils.NotificationUtils
import com.example.safezoneai.utils.WhatsAppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 🧠 ViewModel - VERSIÓN FINAL
 *
 * FLUJO NUEVO:
 * 1. Usuario presiona emergencia
 * 2. Se abre WhatsApp con MENSAJE
 * 3. Usuario envía mensaje y regresa a la app
 * 4. App graba audio mientras está en pantalla de emergencia
 * 5. Cuando termina grabación: Se abre WhatsApp con AUDIO
 * 6. Usuario envía audio
 */
class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val locationUtils = LocationUtils(context)
    private val audioRecorder = AudioRecorder(context)
    private val notificationUtils = NotificationUtils(context)

    private val smartZoneDetector = SmartZoneDetector(context)
    private val database = AppDatabase.getDatabase(context)
    private val emergencyDao = database.emergencyDao()
    private val prefs = context.getSharedPreferences("safezone_settings", Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // ESTADOS REACTIVOS
    // ══════════════════════════════════════════════════════════

    private val _isEmergencyActive = MutableStateFlow(false)
    val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _currentDangerZone = MutableStateFlow<SmartZoneDetector.DangerousZone?>(null)
    val currentDangerZone: StateFlow<SmartZoneDetector.DangerousZone?> =
        _currentDangerZone.asStateFlow()

    private val _detectedCity = MutableStateFlow<SmartZoneDetector.DetectedCity?>(null)
    val detectedCity: StateFlow<SmartZoneDetector.DetectedCity?> = _detectedCity.asStateFlow()

    private val _isLoadingZones = MutableStateFlow(false)
    val isLoadingZones: StateFlow<Boolean> = _isLoadingZones.asStateFlow()

    val emergencyContacts: StateFlow<List<EmergencyContact>> = emergencyDao
        .getAllContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        startLocationTracking()
        observeLocationForSmartDetection()
    }

    // ══════════════════════════════════════════════════════════
    // 🌍 DETECCIÓN INTELIGENTE
    // ══════════════════════════════════════════════════════════

    private fun observeLocationForSmartDetection() {
        viewModelScope.launch {
            _currentLocation
                .filterNotNull()
                .distinctUntilChanged { old, new ->
                    val oldLoc = Location("").apply {
                        latitude = old.latitude
                        longitude = old.longitude
                    }
                    val newLoc = Location("").apply {
                        latitude = new.latitude
                        longitude = new.longitude
                    }
                    oldLoc.distanceTo(newLoc) < 500
                }
                .collect { location ->
                    detectCityAndLoadZones(location.latitude, location.longitude)
                }
        }
    }

    private suspend fun detectCityAndLoadZones(latitude: Double, longitude: Double) {
        _isLoadingZones.value = true

        try {
            val detectedCity = smartZoneDetector.detectAndLoadDangerousZones(latitude, longitude)

            if (detectedCity != null) {
                _detectedCity.value = detectedCity

                val currentZone = smartZoneDetector.checkCurrentZone(
                    userLat = latitude,
                    userLon = longitude,
                    zones = detectedCity.dangerousZones
                )

                if (currentZone != null &&
                    currentZone.dangerLevel != SmartZoneDetector.DangerLevel.SAFE &&
                    _currentDangerZone.value != currentZone
                ) {

                    notificationUtils.showDangerZoneAlert(
                        zoneName = currentZone.name,
                        dangerLevel = currentZone.dangerLevel.name
                    )
                    notificationUtils.vibrateEmergency()
                }

                _currentDangerZone.value = currentZone
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoadingZones.value = false
        }
    }

    // ══════════════════════════════════════════════════════════
    // 🚨 EMERGENCIA - NUEVO FLUJO
    // ══════════════════════════════════════════════════════════

    /**
     * 🚨 ACTIVAR EMERGENCIA - NUEVO FLUJO
     *
     * PASO 0: ✅ ENVÍA SMS INMEDIATAMENTE
     *
     * PASO 1: Abre WhatsApp con MENSAJE
     * - Usuario envía mensaje
     * - Usuario regresa a la app
     *
     * PASO 2: La app inicia grabación automáticamente
     * - Se muestra en pantalla de emergencia
     *
     * PASO 3: Cuando termina grabación
     * - Se abre WhatsApp con AUDIO
     */
    fun activateEmergency() {
        if (_isEmergencyActive.value) return

        viewModelScope.launch {
            try {
                _isEmergencyActive.value = true

                // 1️⃣ Obtener ubicación GPS
                val location = getCurrentLocation()

                // 2️⃣ ✅ ENVIAR SMS INMEDIATAMENTE (ANTES DE TODO)
                location?.let { loc ->
                    sendEmergencySMSToAll(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        audioFilePath = null  // Todavía no hay audio
                    )
                }

                // 3️⃣ Vibrar
                notificationUtils.vibrateEmergency()

                // 4️⃣ Mostrar notificación
                location?.let {
                    notificationUtils.showEmergencyNotification(
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                }

                // 5️⃣ 💬 ABRIR WHATSAPP CON MENSAJE
                location?.let { loc ->
                    openWhatsAppWithMessage(
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                }

                // 6️⃣ Iniciar grabación de audio
                startAudioRecording()

                // 7️⃣ ⏱️ ESPERAR según duración configurada
                val recordingDuration = getRecordingDuration()
                delay(recordingDuration * 1000L)

                // 8️⃣ Detener grabación
                val audioFile = stopAudioRecording()

                // 9️⃣ 💬 ABRIR WHATSAPP CON AUDIO
                location?.let { loc ->
                    openWhatsAppWithAudio(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        audioFilePath = audioFile?.absolutePath
                    )
                }

                // 🔟 Guardar en historial
                val contactsCount = emergencyDao.getAllContacts().first().size
                saveEmergencyToHistory(location, audioFile?.absolutePath, contactsCount)

            } catch (e: Exception) {
                e.printStackTrace()
                _isEmergencyActive.value = false
            }
        }
    }

    fun deactivateEmergency() {
        viewModelScope.launch {
            try {
                stopAudioRecording()
                _isEmergencyActive.value = false
                _isRecording.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // 💬 WHATSAPP - ENVÍO EN 2 PASOS
    // ══════════════════════════════════════════════════════════

    /**
     * 📝 PASO 1: Abre WhatsApp solo con MENSAJE
     */
    private fun openWhatsAppWithMessage(
        latitude: Double,
        longitude: Double
    ) {
        try {
            val whatsAppUtils = WhatsAppUtils(context)
            whatsAppUtils.shareEmergencyLocation(
                latitude = latitude,
                longitude = longitude
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 🎙️ PASO 2: Abre WhatsApp solo con AUDIO
     */
    private fun openWhatsAppWithAudio(
        latitude: Double,
        longitude: Double,
        audioFilePath: String?
    ) {
        try {
            if (audioFilePath == null) return

            val whatsAppUtils = WhatsAppUtils(context)
            whatsAppUtils.openWhatsAppWithAudio(
                latitude = latitude,
                longitude = longitude,
                audioFilePath = audioFilePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 💬 Comparte ubicación por WhatsApp MANUALMENTE (desde botón)
     */
    fun shareLocationViaWhatsApp(phoneNumber: String? = null) {
        viewModelScope.launch {
            try {
                val location = _currentLocation.value
                if (location != null) {
                    val whatsAppUtils = WhatsAppUtils(context)
                    whatsAppUtils.shareEmergencyLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        phoneNumber = phoneNumber
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // 📍 UBICACIÓN GPS
    // ══════════════════════════════════════════════════════════

    private fun startLocationTracking() {
        if (!locationUtils.hasLocationPermission()) return

        viewModelScope.launch {
            try {
                locationUtils.getLocationUpdates(intervalMillis = 5000)
                    .catch { e -> e.printStackTrace() }
                    .collect { location ->
                        _currentLocation.value = location
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (!locationUtils.hasLocationPermission()) return null
            locationUtils.getCurrentLocation()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ══════════════════════════════════════════════════════════
    // 🎙️ GRABACIÓN DE AUDIO
    // ══════════════════════════════════════════════════════════

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

    private fun stopAudioRecording(): java.io.File? {
        return try {
            val audioFile = audioRecorder.stopRecording()
            _isRecording.value = false
            audioFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene duración de grabación desde Settings
     */
    private fun getRecordingDuration(): Int {
        return prefs.getInt("recording_duration", 60)
    }

    // ══════════════════════════════════════════════════════════
    // 📱 ENVÍO DE SMS (RESPALDO)
    // ══════════════════════════════════════════════════════════

    private suspend fun sendEmergencySMSToAll(
        latitude: Double,
        longitude: Double,
        audioFilePath: String?
    ) {
        try {
            val contacts = emergencyDao.getAllContacts().first()

            if (contacts.isEmpty()) {
                return
            }

            notificationUtils.sendEmergencySMS(
                contacts = contacts,
                latitude = latitude,
                longitude = longitude,
                audioFilePath = audioFilePath
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ══════════════════════════════════════════════════════════
    // 📞 CONTACTOS DE EMERGENCIA - CRUD
    // ══════════════════════════════════════════════════════════

    fun addEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyDao.insertContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyDao.updateContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                emergencyDao.deleteContact(contact)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // 🗺️ ZONAS PELIGROSAS
    // ══════════════════════════════════════════════════════════

    fun getAllDangerousZones(): List<SmartZoneDetector.DangerousZone> {
        return _detectedCity.value?.dangerousZones ?: emptyList()
    }

    // ══════════════════════════════════════════════════════════
    // 📜 HISTORIAL DE EMERGENCIAS
    // ══════════════════════════════════════════════════════════

    private suspend fun saveEmergencyToHistory(
        location: Location?,
        audioPath: String?,
        contactsNotified: Int
    ) {
        if (location == null) return

        try {
            val record = EmergencyRecord(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                audioFilePath = audioPath,
                contactsNotified = contactsNotified,
                dangerZoneName = _currentDangerZone.value?.name,
                dangerLevel = _currentDangerZone.value?.dangerLevel?.name,
                notes = null
            )

            database.emergencyRecordDao().insertRecord(record)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isRecording.value) {
            audioRecorder.stopRecording()
        }
    }
}