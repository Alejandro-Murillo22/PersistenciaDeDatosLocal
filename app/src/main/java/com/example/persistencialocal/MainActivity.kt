package com.example.persistencialocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.persistencialocal.ui.screens.InventoryScreen
import com.example.persistencialocal.ui.theme.PersistenciaLocalTheme
import com.example.persistencialocal.viewmodel.InventoryViewModel

/**
 * Actividad principal del proyecto FieldInventory.
 *
 * Este proyecto tiene un fin EDUCATIVO: demostrar la persistencia de datos local en Android.
 *
 * Muestra el flujo:
 * UI (Compose) -> ViewModel -> Repository -> Room (Base de Datos) / DataStore (Preferencias).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersistenciaLocalTheme {
                // Obtenemos el ViewModel que conecta la lógica de persistencia con la UI.
                // La función viewModel() provee automáticamente la instancia necesaria.
                val viewModel: InventoryViewModel = viewModel()
                
                // Pantalla principal del inventario que maneja la visualización y operaciones CRUD.
                InventoryScreen(viewModel = viewModel)
            }
        }
    }
}
