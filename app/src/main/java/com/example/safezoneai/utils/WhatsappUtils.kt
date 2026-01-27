package com.example.safezoneai.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 💬 WHATSAPP UTILS - VERSIÓN SIMPLIFICADA PARA NUEVO FLUJO
 *
 * ESTRATEGIA:
 * 1. Mensaje se envía primero (solo texto)
 * 2. Audio se envía después (solo audio)
 *
 * Usuario regresa a la app entre ambos envíos
 */
class WhatsAppUtils(private val context: Context) {

    /**
     * 📝 Envía solo MENSAJE de emergencia por WhatsApp
     *
     * Este es el PRIMER paso
     * Usuario selecciona contacto, envía, y regresa a la app
     */
    fun shareEmergencyLocation(
        latitude: Double,
        longitude: Double,
        phoneNumber: String? = null,
        customMessage: String? = null
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            val message = customMessage ?: buildEmergencyMessage(latitude, longitude, mapsUrl)

            val intent = if (phoneNumber != null) {
                // Enviar a contacto específico
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                }
            } else {
                // Selector de contactos
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "WhatsApp no está instalado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 🎙️ Envía solo AUDIO de emergencia por WhatsApp
     *
     * Este es el SEGUNDO paso
     * Usuario selecciona el MISMO contacto, envía
     */
    fun openWhatsAppWithAudio(
        latitude: Double,
        longitude: Double,
        audioFilePath: String
    ) {
        try {
            val audioFile = java.io.File(audioFilePath)

            if (!audioFile.exists()) {
                Toast.makeText(
                    context,
                    "No se encontró el audio grabado",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Crear URI del audio usando FileProvider
            val audioUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )

            // Intent para compartir solo el audio
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, audioUri)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            // Instrucción al usuario
            Toast.makeText(
                context,
                "🎙️ Envía este audio al mismo contacto",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al compartir audio",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Verifica si WhatsApp está instalado
     */
    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * WhatsApp Business
     */
    fun shareViaWhatsAppBusiness(
        latitude: Double,
        longitude: Double,
        phoneNumber: String? = null
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            val message = buildEmergencyMessage(latitude, longitude, mapsUrl)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = if (phoneNumber != null) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                }
                setPackage("com.whatsapp.w4b")
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            shareEmergencyLocation(latitude, longitude, phoneNumber)
        }
    }

    /**
     * Compartir por otras apps
     */
    fun shareLocationViaOtherApps(
        latitude: Double,
        longitude: Double
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            val message = buildEmergencyMessage(latitude, longitude, mapsUrl)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "🚨 Alerta de Emergencia - SafeZone AI")
            }

            context.startActivity(
                Intent.createChooser(intent, "Compartir ubicación de emergencia")
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al compartir",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Mensaje de emergencia con ubicación
     */
    private fun buildEmergencyMessage(
        latitude: Double,
        longitude: Double,
        mapsUrl: String
    ): String {
        return """
🚨 *EMERGENCIA - SafeZone AI*

He activado el botón de emergencia.

📍 *Mi ubicación:*
$mapsUrl

📊 *Coordenadas GPS:*
Lat: $latitude
Lon: $longitude

⏰ *Hora:* ${getCurrentTime()}

🎙️ *Audio de emergencia siguiente*

⚠️ Por favor verifica mi seguridad
        """.trimIndent()
    }

    /**
     * Hora actual
     */
    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    companion object {
        fun formatPhoneNumber(phone: String): String {
            return phone.replace(Regex("[^0-9+]"), "")
                .removePrefix("+")
        }
    }
}