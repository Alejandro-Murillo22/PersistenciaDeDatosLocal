package com.example.persistencialocal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.persistencialocal.data.local.AppDatabase
import com.example.persistencialocal.data.local.ProductEntity
import com.example.persistencialocal.data.preferences.SettingsManager
import com.example.persistencialocal.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Conecta la UI con el repositorio.
 * Expone el estado de la aplicación mediante StateFlow para que Compose pueda reaccionar.
 */
class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository
    
    // Consulta de búsqueda actual
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Lista filtrada que combina los productos y la búsqueda
    val filteredProducts: StateFlow<List<ProductEntity>>
    
    // Umbral de stock bajo observado desde DataStore
    val lowStockThreshold: StateFlow<Int>

    // Estado para manejar mensajes de error o éxito
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage = _uiMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val settingsManager = SettingsManager(application)
        repository = InventoryRepository(database.productDao(), settingsManager)
        
        lowStockThreshold = repository.lowStockThreshold.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

        // Combinamos la lista de la DB con el query de búsqueda de forma reactiva
        filteredProducts = combine(repository.allProducts, _searchQuery) { products, query ->
            if (query.isBlank()) {
                products
            } else {
                products.filter { 
                    it.name.contains(query, ignoreCase = true) || 
                    it.category.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addProduct(name: String, price: Double, stock: Int, category: String) {
        viewModelScope.launch {
            try {
                val newProduct = ProductEntity(
                    name = name.trim(),
                    price = price,
                    stock = stock,
                    category = category.trim()
                )
                repository.insertProduct(newProduct)
                _uiMessage.value = "Producto agregado correctamente"
            } catch (e: Exception) {
                _uiMessage.value = "Error al agregar producto"
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.updateProduct(product)
                _uiMessage.value = "Producto actualizado"
            } catch (e: Exception) {
                _uiMessage.value = "Error al actualizar"
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _uiMessage.value = "Producto eliminado"
            } catch (e: Exception) {
                _uiMessage.value = "Error al eliminar"
            }
        }
    }

    fun deleteAllProducts() {
        viewModelScope.launch {
            try {
                repository.deleteAllProducts()
                _uiMessage.value = "Inventario vaciado"
            } catch (e: Exception) {
                _uiMessage.value = "Error al vaciar inventario"
            }
        }
    }

    fun increaseStock(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateStock(product, product.stock + 1)
        }
    }

    fun decreaseStock(product: ProductEntity) {
        if (product.stock > 0) {
            viewModelScope.launch {
                repository.updateStock(product, product.stock - 1)
            }
        } else {
            _uiMessage.value = "El stock no puede ser menor a 0"
        }
    }

    fun updateThreshold(newThreshold: Int) {
        viewModelScope.launch {
            repository.saveLowStockThreshold(newThreshold)
        }
    }

    /**
     * Inserta datos de prueba para facilitar la demostración de la persistencia.
     */
    fun loadSampleData() {
        viewModelScope.launch {
            val samples = listOf(
                ProductEntity(name = "Laptop Dell", price = 1200.0, stock = 10, category = "Electrónica"),
                ProductEntity(name = "Mouse Inalámbrico", price = 25.0, stock = 3, category = "Accesorios"),
                ProductEntity(name = "Teclado Mecánico", price = 80.0, stock = 0, category = "Accesorios"),
                ProductEntity(name = "Monitor 24\"", price = 150.0, stock = 5, category = "Electrónica"),
                ProductEntity(name = "Cable USB-C", price = 12.0, stock = 20, category = "Cables")
            )
            samples.forEach { repository.insertProduct(it) }
            _uiMessage.value = "Datos de prueba cargados"
        }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }
}
