package com.example.safezoneai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyHistoryDao {
    @Query("SELECT * FROM emergency_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<EmergencyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EmergencyRecord)

    @Delete
    suspend fun deleteRecord(record: EmergencyRecord)

    @Query("DELETE FROM emergency_records")
    suspend fun deleteAllRecords()
}