package com.example.persistencialocal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Gestiona las preferencias de la aplicación utilizando Jetpack DataStore.
 * Ideal para datos simples como el umbral de stock bajo.
 */
class SettingsManager(private val context: Context) {

    private val LOW_STOCK_THRESHOLD = intPreferencesKey("low_stock_threshold")

    /**
     * Obtiene el umbral de stock bajo como un Flow.
     * Si no existe, devuelve el valor predeterminado de 5.
     */
    val lowStockThreshold: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[LOW_STOCK_THRESHOLD] ?: 5
        }

    /**
     * Actualiza el umbral de stock bajo de forma persistente.
     */
    suspend fun saveLowStockThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOW_STOCK_THRESHOLD] = threshold
        }
    }
}
