package com.example.safezoneai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa un contacto de emergencia.
 * Almacena información del contacto para enviar alertas en caso de emergencia.
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,           // Nombre del contacto
    val phoneNumber: String,    // Número telefónico (formato: +52XXXXXXXXXX)
    val relationship: String,   // Relación: "Familiar", "Amigo", "Policía", etc.
    val isPrimary: Boolean = false  // Contacto principal (se notifica primero)
)