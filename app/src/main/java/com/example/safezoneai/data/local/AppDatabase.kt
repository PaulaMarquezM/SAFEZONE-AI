package com.example.safezoneai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room de la aplicación (Singleton).
 * Gestiona la persistencia local de datos.
 *
 * ✅ ACTUALIZADO: Ahora incluye EmergencyRecord
 */
@Database(
    entities = [
        EmergencyContact::class,
        EmergencyRecord::class  // ✅ NUEVA ENTIDAD
    ],
    version = 2,  // ✅ INCREMENTADA LA VERSIÓN
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emergencyDao(): EmergencyDao
    abstract fun emergencyRecordDao(): EmergencyRecordDao  // ✅ NUEVO DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos.
         * Usa Double-Check Locking para thread-safety.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "safezone_database"
                )
                    .fallbackToDestructiveMigration() // Para desarrollo
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}