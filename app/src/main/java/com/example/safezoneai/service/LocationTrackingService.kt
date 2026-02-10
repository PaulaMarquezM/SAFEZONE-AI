package com.example.safezoneai.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import com.example.safezoneai.data.SmartZoneDetector
import com.example.safezoneai.utils.LocationUtils
import com.example.safezoneai.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

        private val _currentDangerZone = MutableStateFlow<SmartZoneDetector.DangerousZone?>(null)
        val currentDangerZone: StateFlow<SmartZoneDetector.DangerousZone?> = _currentDangerZone.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var locationUtils: LocationUtils
    private lateinit var smartZoneDetector: SmartZoneDetector
    private lateinit var notificationUtils: NotificationUtils

    private var lastAlertZoneName: String? = null
    private var lastDetectedCity: SmartZoneDetector.DetectedCity? = null

    override fun onCreate() {
        super.onCreate()
        locationUtils = LocationUtils(applicationContext)
        smartZoneDetector = SmartZoneDetector(applicationContext)
        notificationUtils = NotificationUtils(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationUtils.buildTrackingNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationUtils.NOTIFICATION_ID_TRACKING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NotificationUtils.NOTIFICATION_ID_TRACKING, notification)
        }

        _isRunning.value = true
        startTracking()

        return START_STICKY
    }

    private fun startTracking() {
        if (!locationUtils.hasLocationPermission()) {
            stopSelf()
            return
        }

        serviceScope.launch {
            locationUtils.getLocationUpdates(intervalMillis = 10_000)
                .catch { e -> e.printStackTrace() }
                .collect { location ->
                    _currentLocation.value = location
                    checkDangerZone(location)
                }
        }
    }

    private fun checkDangerZone(location: Location) {
        serviceScope.launch {
            try {
                val detectedCity = smartZoneDetector.detectAndLoadDangerousZones(
                    location.latitude, location.longitude
                )

                if (detectedCity != null) {
                    lastDetectedCity = detectedCity

                    val currentZone = smartZoneDetector.checkCurrentZone(
                        userLat = location.latitude,
                        userLon = location.longitude,
                        zones = detectedCity.dangerousZones
                    )

                    _currentDangerZone.value = currentZone

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

                    // Actualizar notificación persistente
                    val statusText = if (currentZone != null &&
                        currentZone.dangerLevel != SmartZoneDetector.DangerLevel.SAFE
                    ) {
                        "Zona: ${currentZone.name} (${currentZone.dangerLevel.name})"
                    } else {
                        "Monitoreando - ${detectedCity.name}"
                    }
                    notificationUtils.updateTrackingNotification(statusText)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        _currentLocation.value = null
        _currentDangerZone.value = null
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
