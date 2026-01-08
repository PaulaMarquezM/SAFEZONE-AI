package com.example.safezoneai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room de la aplicación (Singleton).
 * Gestiona la persistencia local de datos.
 */
@Database(
    entities = [EmergencyContact::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emergencyDao(): EmergencyDao

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
                    .fallbackToDestructiveMigration() // Para desarrollo, elimina datos en cambios de esquema
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}