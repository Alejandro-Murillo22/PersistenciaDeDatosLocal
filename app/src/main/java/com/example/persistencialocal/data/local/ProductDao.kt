package com.example.persistencialocal.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Define las operaciones de acceso a datos para la tabla de productos.
 * Room generará la implementación de esta interfaz.
 */
@Dao
interface ProductDao {
    
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
