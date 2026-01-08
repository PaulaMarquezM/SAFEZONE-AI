package com.example.safezoneai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para la tabla de contactos de emergencia.
 * Define las operaciones CRUD para la base de datos.
 */
@Dao
interface EmergencyDao {

    /**
     * Obtiene todos los contactos de emergencia como Flow.
     * Se actualiza automáticamente cuando hay cambios en la BD.
     */
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    /**
     * Obtiene solo los contactos principales.
     */
    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1")
    suspend fun getPrimaryContacts(): List<EmergencyContact>

    /**
     * Obtiene un contacto por ID.
     */
    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: Int): EmergencyContact?

    /**
     * Inserta un nuevo contacto de emergencia.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    /**
     * Inserta múltiples contactos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContact>)

    /**
     * Actualiza un contacto existente.
     */
    @Update
    suspend fun updateContact(contact: EmergencyContact)

    /**
     * Elimina un contacto.
     */
    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    /**
     * Elimina todos los contactos.
     */
    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAllContacts()
}