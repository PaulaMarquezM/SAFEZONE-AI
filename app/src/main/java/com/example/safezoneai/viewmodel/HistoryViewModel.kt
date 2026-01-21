package com.example.safezoneai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safezoneai.data.local.AppDatabase
import com.example.safezoneai.data.local.EmergencyRecord
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 📊 ViewModel para el historial de emergencias
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val emergencyRecordDao = database.emergencyRecordDao()

    // ══════════════════════════════════════════════════════════
    // ESTADOS REACTIVOS
    // ══════════════════════════════════════════════════════════

    val emergencyRecords: StateFlow<List<EmergencyRecord>> = emergencyRecordDao
        .getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()

    init {
        // Actualizar contador
        viewModelScope.launch {
            emergencyRecords.collect { records ->
                _recordCount.value = records.size
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // OPERACIONES CRUD
    // ══════════════════════════════════════════════════════════

    /**
     * Elimina un registro específico
     */
    fun deleteRecord(record: EmergencyRecord) {
        viewModelScope.launch {
            try {
                emergencyRecordDao.deleteRecord(record)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Elimina todos los registros
     */
    fun deleteAllRecords() {
        viewModelScope.launch {
            try {
                emergencyRecordDao.deleteAllRecords()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Obtiene registros por rango de fechas
     */
    suspend fun getRecordsByDateRange(startTimestamp: Long, endTimestamp: Long): List<EmergencyRecord> {
        return try {
            emergencyRecordDao.getRecordsByDate(startTimestamp, endTimestamp)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}