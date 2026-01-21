package com.example.safezoneai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 📝 Entidad para guardar historial de emergencias
 */
@Entity(tableName = "emergency_records")
data class EmergencyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val timestamp: Long,                    // Fecha/hora en milisegundos
    val latitude: Double,                   // Latitud GPS
    val longitude: Double,                  // Longitud GPS
    val audioFilePath: String?,             // Ruta del audio grabado
    val contactsNotified: Int,              // Número de contactos notificados
    val dangerZoneName: String?,            // Nombre de zona peligrosa (si aplica)
    val dangerLevel: String?,               // Nivel de peligro
    val notes: String? = null               // Notas adicionales
)