package com.example.persistencialocal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa la estructura de una tabla en la base de datos Room.
 * Cada instancia de esta clase es una fila en la tabla "products".
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double,
    val stock: Int,
    val category: String
)
