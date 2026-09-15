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

    suspend fun deleteAllProducts() {
        productDao.deleteAllProducts()
    }

    /**
     * Defensa adicional de la capa de repositorio: un stock negativo es un estado imposible del
     * dominio y nunca debe llegar a la base de datos.
     *
     * El ViewModel ya valida antes de llamar aquí, así que en condiciones normales esta excepción
     * no se lanza. Se lanza en vez de ignorar el valor para que un error de programación futuro
     * (un nuevo `caller` que olvide validar) falle de forma visible y no de forma silenciosa: si
     * se descartara la escritura sin avisar, la UI mostraría un valor que nunca se guardó.
     *
     * @throws IllegalArgumentException si [newStock] es negativo.
     */
    suspend fun updateStock(product: ProductEntity, newStock: Int) {
        require(newStock >= 0) {
            "El stock no puede ser negativo (se recibió $newStock para '${product.name}')"
        }
        productDao.updateProduct(product.copy(stock = newStock))
    }

    // --- Operaciones de DataStore (Preferencias) ---

    val lowStockThreshold: Flow<Int> = settingsManager.lowStockThreshold

    /**
     * Mismo criterio que [updateStock]: un umbral negativo no tiene sentido, y descartarlo en
     * silencio dejaría al usuario creyendo que su ajuste se guardó.
     *
     * @throws IllegalArgumentException si [threshold] es negativo.
     */
    suspend fun saveLowStockThreshold(threshold: Int) {
        require(threshold >= 0) {
            "El umbral de stock bajo no puede ser negativo (se recibió $threshold)"
        }
        settingsManager.saveLowStockThreshold(threshold)
    }
}
