package com.example.persistencialocal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.persistencialocal.data.local.ProductEntity
import java.text.NumberFormat
import java.util.*

/**
 * Tarjeta que muestra la información detallada de un producto.
 * Permite realizar acciones rápidas como aumentar/disminuir stock, editar o eliminar.
 */
@Composable
fun ProductCard(
    product: ProductEntity,
    lowStockThreshold: Int,
    onIncreaseStock: () -> Unit,
    onDecreaseStock: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = currencyFormatter.format(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de Stock reactivo al umbral de DataStore
                Column {
                    Text(
                        text = "Stock: ${product.stock}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    StockIndicator(stock = product.stock, threshold = lowStockThreshold)
                }

                // Controles de stock y acciones CRUD
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDecreaseStock) {
                        Icon(Icons.Default.Remove, contentDescription = "Disminuir stock")
                    }
                    IconButton(onClick = onIncreaseStock) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar stock")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar producto", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar producto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
