package com.example.persistencialocal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.persistencialocal.data.local.ProductEntity
import com.example.persistencialocal.ui.components.EmptyInventoryState
import com.example.persistencialocal.ui.components.ProductCard
import com.example.persistencialocal.ui.components.ProductFormDialog
import com.example.persistencialocal.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val threshold by viewModel.lowStockThreshold.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showInfoSection by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FieldInventory")
                        Text(
                            "Demostración de persistencia local",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoSection = !showInfoSection }) {
                        Icon(Icons.Default.Info, contentDescription = "Información educativa")
                    }
                    IconButton(onClick = { showThresholdDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar umbral")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showInfoSection) {
                EducationSection(onLoadSampleData = { viewModel.loadSampleData() })
            }

            InventorySummary(products = products, threshold = threshold)

            if (products.isEmpty()) {
                EmptyInventoryState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            lowStockThreshold = threshold,
                            onIncreaseStock = { viewModel.increaseStock(product) },
                            onDecreaseStock = { viewModel.decreaseStock(product) },
                            onEdit = { productToEdit = product },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Agregar
    if (showAddDialog) {
        ProductFormDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, stock, category ->
                viewModel.addProduct(name, price, stock, category)
                showAddDialog = false
            }
        )
    }

    // Diálogo para Editar
    productToEdit?.let { product ->
        ProductFormDialog(
            product = product,
            onDismiss = { productToEdit = null },
            onConfirm = { name, price, stock, category ->
                viewModel.updateProduct(product.copy(name = name, price = price, stock = stock, category = category))
                productToEdit = null
            }
        )
    }

    // Diálogo de Confirmación para Eliminar
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("¿Eliminar producto?") },
            text = { Text("Esta acción no se puede deshacer. El producto se borrará permanentemente de Room Database.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para configurar Umbral (DataStore)
    if (showThresholdDialog) {
        ThresholdDialog(
            currentThreshold = threshold,
            onDismiss = { showThresholdDialog = false },
            onConfirm = { newThreshold ->
                viewModel.updateThreshold(newThreshold)
                showThresholdDialog = false
            }
        )
    }
}

@Composable
fun EducationSection(onLoadSampleData: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Persistencia Local",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Room: Almacena los productos (datos estructurados) en una base SQLite local.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• DataStore: Almacena el umbral de stock (preferencia simple) de forma persistente.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "• Reactividad: Los cambios en la base de datos se reflejan automáticamente en la UI mediante Flow.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLoadSampleData,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cargar datos de prueba", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun InventorySummary(products: List<ProductEntity>, threshold: Int) {
    val lowStockCount = products.count { it.stock <= threshold && it.stock > 0 }
    val outOfStockCount = products.count { it.stock == 0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryItem(label = "Total", value = products.size.toString())
            SummaryItem(label = "Stock Bajo", value = lowStockCount.toString(), color = Color(0xFFED6C02))
            SummaryItem(label = "Sin Stock", value = outOfStockCount.toString(), color = Color(0xFFD32F2F))
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ThresholdDialog(
    currentThreshold: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var thresholdStr by remember { mutableStateOf(currentThreshold.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Umbral de Stock Bajo") },
        text = {
            Column {
                Text("Los productos con stock igual o inferior a este valor se marcarán como 'Stock bajo'.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            thresholdStr = it
                        }
                    },
                    label = { Text("Umbral") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = thresholdStr.toIntOrNull() ?: 0
                onConfirm(if (value < 0) 0 else value)
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
