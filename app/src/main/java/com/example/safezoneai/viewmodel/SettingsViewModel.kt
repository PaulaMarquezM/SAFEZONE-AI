package com.example.safezoneai.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ⚙️ ViewModel para gestionar configuraciones de la app
 * Usa SharedPreferences para persistencia
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("safezone_settings", Context.MODE_PRIVATE)

    // ══════════════════════════════════════════════════════════
    // ESTADOS DE CONFIGURACIÓN
    // ══════════════════════════════════════════════════════════

    private val _soundEnabled = MutableStateFlow(
        prefs.getBoolean("sound_enabled", true)
    )
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(
        prefs.getBoolean("vibration_enabled", true)
    )
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _autoRecordEnabled = MutableStateFlow(
        prefs.getBoolean("auto_record_enabled", true)
    )
    val autoRecordEnabled: StateFlow<Boolean> = _autoRecordEnabled.asStateFlow()

    private val _gpsInterval = MutableStateFlow(
        prefs.getInt("gps_interval", 5)
    )
    val gpsInterval: StateFlow<Int> = _gpsInterval.asStateFlow()

    private val _recordingDuration = MutableStateFlow(
        prefs.getInt("recording_duration", 60)
    )
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    // ══════════════════════════════════════════════════════════
    // MÉTODOS PARA ACTUALIZAR CONFIGURACIONES
    // ══════════════════════════════════════════════════════════

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()
    }

    fun setAutoRecordEnabled(enabled: Boolean) {
        _autoRecordEnabled.value = enabled
        prefs.edit().putBoolean("auto_record_enabled", enabled).apply()
    }

    fun setGpsInterval(interval: Int) {
        _gpsInterval.value = interval
        prefs.edit().putInt("gps_interval", interval).apply()
    }

    fun setRecordingDuration(duration: Int) {
        _recordingDuration.value = duration
        prefs.edit().putInt("recording_duration", duration).apply()
    }

    /**
     * Restablecer configuraciones por defecto
     */
    fun resetToDefaults() {
        setSoundEnabled(true)
        setVibrationEnabled(true)
        setAutoRecordEnabled(true)
        setGpsInterval(5)
        setRecordingDuration(60)
    }
}