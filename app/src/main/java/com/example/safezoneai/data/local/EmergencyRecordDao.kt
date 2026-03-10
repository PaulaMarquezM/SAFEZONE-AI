package com.example.safezoneai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 *  DAO para historial de emergencias
 */
@Dao
interface EmergencyRecordDao {

    /**
     * Obtiene todos los registros ordenados por fecha (más reciente primero)
     */
    @Query("SELECT * FROM emergency_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<EmergencyRecord>>

    /**
     * Obtiene un registro por ID
     */
    @Query("SELECT * FROM emergency_records WHERE id = :recordId")
    suspend fun getRecordById(recordId: Int): EmergencyRecord?

    /**
     * Obtiene registros de una fecha específica
     */
    @Query("SELECT * FROM emergency_records WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    suspend fun getRecordsByDate(startTimestamp: Long, endTimestamp: Long): List<EmergencyRecord>

    /**
     * Inserta un nuevo registro
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EmergencyRecord): Long

    /**
     * Actualiza un registro
     */
    @Update
    suspend fun updateRecord(record: EmergencyRecord)

    /**
     * Elimina un registro
     */
    @Delete
    suspend fun deleteRecord(record: EmergencyRecord)

    /**
     * Elimina todos los registros
     */
    @Query("DELETE FROM emergency_records")
    suspend fun deleteAllRecords()

    /**
     * Cuenta total de emergencias
     */
    @Query("SELECT COUNT(*) FROM emergency_records")
    suspend fun getRecordCount(): Int
}