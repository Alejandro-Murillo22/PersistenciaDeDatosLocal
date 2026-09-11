package com.example.persistencialocal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.persistencialocal.ui.theme.StockCritical
import com.example.persistencialocal.ui.theme.StockLow
import com.example.persistencialocal.ui.theme.StockNormal

/**
 * Componente visual que indica el estado del stock.
 * Utiliza colores y texto para facilitar la comprensión del inventario.
 * Demuestra cómo la UI reacciona a los datos persistidos (stock) y a las preferencias (threshold).
 */
@Composable
fun StockIndicator(
    stock: Int,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val isOutOffStock = stock == 0
    val isLowStock = stock in 1..threshold

    val stockColor = when {
        isOutOffStock -> StockCritical
        isLowStock -> StockLow
        else -> StockNormal
    }

    val stockText = when {
        isOutOffStock -> "Sin stock"
        isLowStock -> "Stock bajo"
        else -> "Stock normal"
    }

    Surface(
        color = stockColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = stockText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = stockColor,
            fontWeight = FontWeight.Bold
        )
    }
}
