package com.example.persistencialocal.repository

import com.example.persistencialocal.data.local.ProductDao
import com.example.persistencialocal.data.local.ProductEntity
import com.example.persistencialocal.data.preferences.SettingsManager
import kotlinx.coroutines.flow.Flow

/**
 * Actúa como intermediario entre la fuente de datos (Room/DataStore) y el ViewModel.
 * Esta capa permite abstraer de dónde provienen los datos.
 */
class InventoryRepository(
    private val productDao: ProductDao,
    private val settingsManager: SettingsManager
) {

    // --- Operaciones de Room (Productos) ---

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun updateStock(product: ProductEntity, newStock: Int) {
        if (newStock >= 0) {
            productDao.updateProduct(product.copy(stock = newStock))
        }
    }

    // --- Operaciones de DataStore (Preferencias) ---

    val lowStockThreshold: Flow<Int> = settingsManager.lowStockThreshold

    suspend fun saveLowStockThreshold(threshold: Int) {
        if (threshold >= 0) {
            settingsManager.saveLowStockThreshold(threshold)
        }
    }
}
