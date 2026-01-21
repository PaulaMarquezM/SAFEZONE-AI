package com.example.safezoneai.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * 💬 UTILIDAD PARA COMPARTIR POR WHATSAPP
 * Permite enviar ubicación de emergencia directamente por WhatsApp
 */
class WhatsAppUtils(private val context: Context) {

    /**
     * Comparte ubicación de emergencia por WhatsApp
     *
     * @param latitude Latitud GPS
     * @param longitude Longitud GPS
     * @param phoneNumber Número de teléfono (opcional, con código de país)
     * @param customMessage Mensaje personalizado (opcional)
     */
    fun shareEmergencyLocation(
        latitude: Double,
        longitude: Double,
        phoneNumber: String? = null,
        customMessage: String? = null
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"

            val message = customMessage ?: buildDefaultMessage(latitude, longitude, mapsUrl)

            val intent = if (phoneNumber != null) {
                // Enviar a un contacto específico
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                }
            } else {
                // Abrir selector de contactos
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                }
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()

            // Si WhatsApp no está instalado, mostrar alternativas
            showWhatsAppNotInstalledDialog()
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
     * Comparte por WhatsApp Business (si está instalado)
     */
    fun shareViaWhatsAppBusiness(
        latitude: Double,
        longitude: Double,
        phoneNumber: String? = null
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            val message = buildDefaultMessage(latitude, longitude, mapsUrl)

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
            // Fallback a WhatsApp normal
            shareEmergencyLocation(latitude, longitude, phoneNumber)
        }
    }

    /**
     * Comparte ubicación por otras apps (SMS, Telegram, etc.)
     */
    fun shareLocationViaOtherApps(
        latitude: Double,
        longitude: Double
    ) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
            val message = buildDefaultMessage(latitude, longitude, mapsUrl)

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
                "Error al compartir ubicación",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Construye el mensaje por defecto
     */
    private fun buildDefaultMessage(
        latitude: Double,
        longitude: Double,
        mapsUrl: String
    ): String {
        return """
🚨 *ALERTA DE EMERGENCIA* - SafeZone AI

He activado el botón de emergencia.

📍 *Mi ubicación:*
$mapsUrl

📊 *Coordenadas GPS:*
Latitud: $latitude
Longitud: $longitude

⏰ *Hora:* ${getCurrentTime()}

Por favor, verifica mi seguridad lo antes posible.
        """.trimIndent()
    }

    /**
     * Obtiene la hora actual formateada
     */
    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    /**
     * Muestra diálogo cuando WhatsApp no está instalado
     */
    private fun showWhatsAppNotInstalledDialog() {
        Toast.makeText(
            context,
            "WhatsApp no está instalado. Usa la opción de compartir por otras apps.",
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {
        /**
         * Limpia y formatea un número de teléfono para WhatsApp
         * Ejemplo: +593 999 999 999 -> 593999999999
         */
        fun formatPhoneNumber(phone: String): String {
            return phone.replace(Regex("[^0-9+]"), "")
                .removePrefix("+")
        }
    }
}