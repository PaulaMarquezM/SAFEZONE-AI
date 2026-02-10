package com.example.safezoneai.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.data.local.AppDatabase
import com.example.safezoneai.service.LocationTrackingService
import com.example.safezoneai.data.local.EmergencyContact
import com.example.safezoneai.data.local.EmergencyRecord
import com.example.safezoneai.utils.AudioRecorder
import com.example.safezoneai.utils.LocationUtils
import com.example.safezoneai.utils.NotificationUtils
import com.example.safezoneai.utils.WhatsAppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.location.Geocoder
import java.util.Locale


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

    private var lastAlertZoneName: String? = null

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

    private val _isLoadingZones = MutableStateFlow(true)
    val isLoadingZones: StateFlow<Boolean> = _isLoadingZones.asStateFlow()

    private val _currentAddress = MutableStateFlow<String?>(null)
    val currentAddress: StateFlow<String?> = _currentAddress.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    // Trigger para emergencia desde botón de volumen
    private val _volumeEmergencyTriggered = MutableStateFlow(false)
    val volumeEmergencyTriggered: StateFlow<Boolean> = _volumeEmergencyTriggered.asStateFlow()

    fun triggerEmergencyFromVolume() {
        if (!_isEmergencyActive.value) {
            _volumeEmergencyTriggered.value = true
            activateEmergency()
        }
    }

    fun clearVolumeEmergencyTrigger() {
        _volumeEmergencyTriggered.value = false
    }


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
        observeServiceState()
    }

    // ══════════════════════════════════════════════════════════
    // 🌍 DETECCIÓN INTELIGENTE
    // ══════════════════════════════════════════════════════════

    private fun updateAddressFromLocation(location: Location) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                val addr = results?.firstOrNull()

                val street = addr?.thoroughfare ?: addr?.subThoroughfare
                val number = addr?.subThoroughfare
                val avenue = addr?.featureName
                val neighborhood = addr?.subLocality
                val city = addr?.locality ?: addr?.subAdminArea
                val state = addr?.adminArea
                val country = addr?.countryName

                // Armado “bonito” con lo disponible
                val line1 = listOfNotNull(
                    street?.let { s -> if (number != null) "$s $number" else s },
                    neighborhood
                ).joinToString(", ").takeIf { it.isNotBlank() }

                val line2 = listOfNotNull(city, state, country)
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }

                _currentAddress.value = listOfNotNull(line1, line2).joinToString(" • ")
                    .ifBlank { null }

            } catch (e: Exception) {
                // Si falla, no rompemos la app
                _currentAddress.value = null
                e.printStackTrace()
            }
        }
    }

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
                    oldLoc.distanceTo(newLoc) < 50
                }
                .collect { location ->
                    detectCityAndLoadZones(location.latitude, location.longitude)
                }
        }
    }

    /**
     * Observa el estado del servicio de background para mantener la UI sincronizada.
     */
    private fun observeServiceState() {
        viewModelScope.launch {
            LocationTrackingService.isRunning.collect { running ->
                if (running) {
                    // Observar ubicación del servicio
                    launch {
                        LocationTrackingService.currentLocation
                            .filterNotNull()
                            .collect { location ->
                                _currentLocation.value = location
                            }
                    }
                    // Observar zona peligrosa del servicio
                    launch {
                        LocationTrackingService.currentDangerZone
                            .collect { zone ->
                                _currentDangerZone.value = zone
                            }
                    }
                }
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
                    lastAlertZoneName != currentZone.name
                ) {
                    lastAlertZoneName = currentZone.name

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

                // 1️⃣ VIBRAR INMEDIATAMENTE
                notificationUtils.vibrateEmergency()

                // 2️⃣ ✅ ENVIAR SMS AL INSTANTE con la ubicación que ya tenemos
                val quickLocation = _currentLocation.value
                sendEmergencySMSToAll(
                    latitude = quickLocation?.latitude ?: 0.0,
                    longitude = quickLocation?.longitude ?: 0.0,
                    audioFilePath = null,
                    hasLocation = quickLocation != null
                )

                // 3️⃣ Obtener ubicación fresca para WhatsApp/notificación
                val freshLocation = getCurrentLocation()
                val location = freshLocation ?: quickLocation

                // 4️⃣ Mostrar notificación
                if (location != null) {
                    notificationUtils.showEmergencyNotification(
                        latitude = location.latitude,
                        longitude = location.longitude
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

    fun stopOnlyAudioRecording() {
        viewModelScope.launch {
            try {
                if (_isRecording.value) {
                    audioRecorder.stopRecording()
                    _isRecording.value = false
                }
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
        if (!locationUtils.hasLocationPermission()) {
            _locationError.value = "Permisos de ubicación no concedidos"
            _isLoadingZones.value = false
            return
        }

        if (!locationUtils.isGpsEnabled()) {
            _locationError.value = "Activa el GPS para detectar zonas"
            _isLoadingZones.value = false
            return
        }

        viewModelScope.launch {
            try {
                _locationError.value = null
                locationUtils.getLocationUpdates(intervalMillis = 5000)
                    .catch { e ->
                        e.printStackTrace()
                        _locationError.value = "Error obteniendo ubicación"
                        _isLoadingZones.value = false
                    }
                    .collect { location ->
                        _currentLocation.value = location
                        updateAddressFromLocation(location)
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
                _locationError.value = "Error de permisos de ubicación"
                _isLoadingZones.value = false
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
        audioFilePath: String?,
        hasLocation: Boolean = true
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
                audioFilePath = audioFilePath,
                hasLocation = hasLocation
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