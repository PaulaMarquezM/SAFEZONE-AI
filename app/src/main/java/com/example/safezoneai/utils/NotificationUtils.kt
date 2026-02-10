package com.example.safezoneai.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.safezoneai.MainActivity
import com.example.safezoneai.R
import com.example.safezoneai.data.local.EmergencyContact

/**
 * Utilidad para manejar notificaciones, alertas y envío de SMS.
 */
class NotificationUtils(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    companion object {
        private const val CHANNEL_ID_EMERGENCY = "emergency_channel"
        private const val CHANNEL_ID_ZONE_ALERT = "zone_alert_channel"
        const val CHANNEL_ID_TRACKING = "tracking_service_channel"
        private const val NOTIFICATION_ID_EMERGENCY = 1001
        private const val NOTIFICATION_ID_ZONE = 1002
        const val NOTIFICATION_ID_TRACKING = 1003
    }

    init {
        createNotificationChannels()
    }

    /**
     * Crea los canales de notificación (requerido en Android 8+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para emergencias
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Alertas de Emergencia",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de situaciones de emergencia"
                enableVibration(true)
                setShowBadge(true)
            }

            // Canal para alertas de zona
            val zoneChannel = NotificationChannel(
                CHANNEL_ID_ZONE_ALERT,
                "Alertas de Zona Peligrosa",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas cuando entras a una zona insegura"
                enableVibration(true)
            }

            // Canal para servicio de monitoreo en background (sin sonido)
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Monitoreo de ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente del monitoreo en segundo plano"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(emergencyChannel)
            notificationManager.createNotificationChannel(zoneChannel)
            notificationManager.createNotificationChannel(trackingChannel)
        }
    }

    /**
     * Muestra una notificación de emergencia.
     */
    fun showEmergencyNotification(latitude: Double, longitude: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Emergencia Activada")
            .setContentText("Se ha activado el botón de emergencia. Ubicación registrada.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Emergencia activada.\nUbicación: $latitude, $longitude\nSe ha notificado a tus contactos de emergencia."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_EMERGENCY, notification)
    }

    /**
     * Muestra una alerta cuando el usuario entra en una zona peligrosa.
     */
    fun showDangerZoneAlert(zoneName: String, dangerLevel: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ZONE_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠️ Zona Peligrosa Detectada")
            .setContentText("Has entrado a: $zoneName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("⚠️ Alerta de seguridad\n\nZona: $zoneName\nNivel de peligro: $dangerLevel\n\nMantente alerta y considera salir de esta área."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ZONE, notification)
    }

    /**
     * Vibra el dispositivo en patrón de emergencia.
     */
    fun vibrateEmergency() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }

    /**
     * Envía SMS de emergencia a los contactos.
     * NOTA: Requiere permiso SEND_SMS.
     */
    fun sendEmergencySMS(
        contacts: List<EmergencyContact>,
        latitude: Double,
        longitude: Double,
        audioFilePath: String?,
        hasLocation: Boolean = true
    ) {
        if (!hasSMSPermission()) return

        val message = buildEmergencyMessage(latitude, longitude, audioFilePath, hasLocation)

        try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (smsManager == null) return

            contacts.forEach { contact ->
                try {
                    val parts = smsManager.divideMessage(message)
                    smsManager.sendMultipartTextMessage(
                        contact.phoneNumber, null, parts, null, null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Construye el mensaje de emergencia.
     */
    private fun buildEmergencyMessage(
        latitude: Double,
        longitude: Double,
        audioFilePath: String?,
        hasLocation: Boolean = true
    ): String {
        return if (hasLocation) {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            """
                🚨 ALERTA DE EMERGENCIA - SafeZone AI

                He activado el botón de emergencia.

                📍 Mi ubicación:
                $mapsUrl

                Coordenadas: $latitude, $longitude

                ${if (audioFilePath != null) "🎙️ Audio de emergencia grabado" else ""}

                Por favor, verifica mi seguridad.
            """.trimIndent()
        } else {
            """
                🚨 ALERTA DE EMERGENCIA - SafeZone AI

                He activado el botón de emergencia.

                📍 No se pudo obtener la ubicación GPS.

                ${if (audioFilePath != null) "🎙️ Audio de emergencia grabado" else ""}

                Por favor, verifica mi seguridad.
            """.trimIndent()
        }
    }

    /**
     * Construye la notificación persistente del servicio de monitoreo.
     */
    fun buildTrackingNotification(contentText: String = "Monitoreando tu ubicación"): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("SafeZone AI - Monitoreo activo")
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Actualiza la notificación persistente del servicio de monitoreo.
     */
    fun updateTrackingNotification(contentText: String) {
        val notification = buildTrackingNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID_TRACKING, notification)
    }

    /**
     * Verifica si el permiso de SMS está concedido.
     */
    private fun hasSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}