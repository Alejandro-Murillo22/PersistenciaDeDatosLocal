package com.example.persistencialocal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * La base de datos principal de la aplicación.
 * Utiliza el patrón Singleton para asegurar una única instancia.
 */
@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "field_inventory_db"
                )
                    // DECISIÓN DELIBERADA (proyecto educativo), no un descuido:
                    //
                    // Room exige que, al subir `version`, se le indique cómo transformar la base
                    // de datos existente. Si no se le dice nada, la app lanza
                    // IllegalStateException ("Room cannot verify the data integrity... / A
                    // migration from 1 to 2 was required but not found") y CRASHEA al arrancar
                    // en cualquier dispositivo que ya tuviera la versión anterior instalada.
                    //
                    // `fallbackToDestructiveMigration()` resuelve ese caso borrando y recreando
                    // las tablas: la app nunca crashea, pero se PIERDEN los datos del usuario.
                    // Para esta actividad es lo correcto, porque el esquema puede cambiar
                    // mientras se explica el tema y no queremos que la demo se caiga.
                    //
                    // En PRODUCCIÓN esto sería inaceptable: borraría el inventario real de cada
                    // usuario en una actualización. Ahí lo correcto es escribir una `Migration`
                    // explícita (`addMigrations(MIGRATION_1_2, ...)`) con el ALTER TABLE
                    // correspondiente, activar `exportSchema = true` para versionar el esquema
                    // en Git, y cubrirla con un test de migración (`MigrationTestHelper`).
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
