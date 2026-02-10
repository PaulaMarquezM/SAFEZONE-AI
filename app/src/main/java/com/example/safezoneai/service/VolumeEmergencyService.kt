package com.example.safezoneai.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.example.safezoneai.utils.NotificationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio en background que detecta 3 pulsaciones rapidas del boton
 * de volumen para activar una emergencia automaticamente.
 *
 * Usa ContentObserver sobre Settings.System para detectar cambios
 * de volumen incluso con la pantalla apagada o la app en background.
 */
class VolumeEmergencyService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1004
        private const val REQUIRED_PRESSES = 3
        private const val TIME_WINDOW_MS = 2000L

        private val _emergencyTriggered = MutableStateFlow(false)
        val emergencyTriggered: StateFlow<Boolean> = _emergencyTriggered.asStateFlow()

        fun clearTrigger() {
            _emergencyTriggered.value = false
        }

        fun start(context: Context) {
            val intent = Intent(context, VolumeEmergencyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VolumeEmergencyService::class.java))
        }
    }

    private var volumeObserver: ContentObserver? = null
    private val volumeChangeTimestamps = mutableListOf<Long>()

    override fun onCreate() {
        super.onCreate()
        registerVolumeObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationUtils = NotificationUtils(applicationContext)
        val notification = notificationUtils.buildTrackingNotification(
            "Proteccion activa - Volumen x3 para emergencia"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun registerVolumeObserver() {
        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val now = System.currentTimeMillis()

                // Limpiar pulsaciones fuera de la ventana de tiempo
                volumeChangeTimestamps.removeAll { now - it > TIME_WINDOW_MS }
                volumeChangeTimestamps.add(now)

                if (volumeChangeTimestamps.size >= REQUIRED_PRESSES) {
                    volumeChangeTimestamps.clear()
                    triggerEmergency()
                }
            }
        }

        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )
    }

    private fun triggerEmergency() {
        _emergencyTriggered.value = true

        // Lanzar MainActivity para traer la app al frente
        val launchIntent = Intent(this, Class.forName("com.example.safezoneai.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TRIGGER_EMERGENCY", true)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
