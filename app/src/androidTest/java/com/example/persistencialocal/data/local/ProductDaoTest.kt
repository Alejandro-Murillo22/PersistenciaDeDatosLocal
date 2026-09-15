package com.example.persistencialocal.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados del DAO de Room, que es el mecanismo de persistencia evaluado.
 *
 * Se usa [Room.inMemoryDatabaseBuilder] en vez de la base de datos real del dispositivo:
 * la BD vive solo en memoria y se destruye al terminar cada test, así que los casos no se
 * contaminan entre sí ni tocan los datos de la app instalada. La API ejercitada es
 * exactamente la misma que en producción (`ProductDao`), por lo que lo que se valida aquí
 * es el contrato real de persistencia: insertar, consultar, actualizar y eliminar.
 */
@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ProductDao

    @Before
    fun crearBaseDeDatosEnMemoria() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // El test corre en el hilo de instrumentación; sin esto Room bloquearía las consultas.
            .allowMainThreadQueries()
            .build()
        dao = database.productDao()
    }

    @After
    fun cerrarBaseDeDatos() {
        database.close()
    }

    /** Insertar un producto y verificar que se puede consultar. */
    @Test
    fun insertarProducto_quedaDisponibleEnLaConsulta() = runBlocking {
        val producto = ProductEntity(
            name = "Taladro percutor",
            price = 320.0,
            stock = 4,
            category = "Herramientas"
        )

        dao.insertProduct(producto)

        val productos = dao.getAllProducts().first()
        assertEquals(1, productos.size)
        assertEquals("Taladro percutor", productos[0].name)
        assertEquals(320.0, productos[0].price, 0.001)
        assertEquals(4, productos[0].stock)
        assertEquals("Herramientas", productos[0].category)
        // El id lo genera Room (autoGenerate), por lo que debe dejar de ser 0.
        assertTrue(productos[0].id > 0)
    }

    /** Actualizar el stock de un producto y verificar que el cambio persiste. */
    @Test
    fun actualizarStock_persisteElNuevoValor() = runBlocking {
        dao.insertProduct(
            ProductEntity(name = "Casco de seguridad", price = 45.0, stock = 10, category = "EPP")
        )
        val original = dao.getAllProducts().first().single()

        dao.updateProduct(original.copy(stock = 7))

        // Se relee desde la base de datos, no desde el objeto en memoria.
        val recargado = dao.getProductById(original.id)
        assertNotNull(recargado)
        assertEquals(7, recargado!!.stock)
        // El resto de campos no debe haberse alterado.
        assertEquals("Casco de seguridad", recargado.name)
        assertEquals(1, dao.getAllProducts().first().size)
    }

    /** Eliminar un producto y verificar que ya no aparece en la consulta. */
    @Test
    fun eliminarProducto_desapareceDeLaConsulta() = runBlocking {
        dao.insertProduct(
            ProductEntity(name = "Guantes de nitrilo", price = 8.0, stock = 30, category = "EPP")
        )
        dao.insertProduct(
            ProductEntity(name = "Linterna LED", price = 15.0, stock = 6, category = "Herramientas")
        )
        val aEliminar = dao.getAllProducts().first().first { it.name == "Linterna LED" }

        dao.deleteProduct(aEliminar)

        val restantes = dao.getAllProducts().first()
        assertEquals(1, restantes.size)
        assertEquals("Guantes de nitrilo", restantes[0].name)
        assertNull(dao.getProductById(aEliminar.id))
    }

    /**
     * La consulta declara `ORDER BY name ASC`: verificar el orden confirma que la lista que
     * llega a la UI la ordena SQLite, no el ViewModel.
     */
    @Test
    fun consultarProductos_devuelveOrdenadoPorNombre() = runBlocking {
        dao.insertProduct(ProductEntity(name = "Zapatos", price = 1.0, stock = 1, category = "EPP"))
        dao.insertProduct(ProductEntity(name = "Arnés", price = 1.0, stock = 1, category = "EPP"))
        dao.insertProduct(ProductEntity(name = "Martillo", price = 1.0, stock = 1, category = "Herramientas"))

        val nombres = dao.getAllProducts().first().map { it.name }

        assertEquals(listOf("Arnés", "Martillo", "Zapatos"), nombres)
    }

    /**
     * El corazón del flujo reactivo: `getAllProducts()` devuelve un `Flow`, así que un solo
     * colector recibe una emisión NUEVA cada vez que la tabla cambia, sin volver a consultar.
     * Eso es lo que hace que la UI de Compose se recomponga sola tras un insert.
     */
    @Test
    fun flowDeProductos_emiteDeNuevoAlInsertar() = runBlocking {
        val emisiones = mutableListOf<List<ProductEntity>>()
        val colector = launch(Dispatchers.IO) {
            dao.getAllProducts().collect { emisiones.add(it) }
        }

        // Primera emisión: la tabla arranca vacía.
        withTimeout(TIEMPO_ESPERA_MS) { while (emisiones.size < 1) delay(10) }

        dao.insertProduct(
            ProductEntity(name = "Nivel láser", price = 210.0, stock = 2, category = "Herramientas")
        )

        // Segunda emisión: provocada por el insert, no por un nuevo `collect`.
        withTimeout(TIEMPO_ESPERA_MS) { while (emisiones.size < 2) delay(10) }
        colector.cancel()

        assertTrue(emisiones[0].isEmpty())
        assertEquals(1, emisiones[1].size)
        assertEquals("Nivel láser", emisiones[1][0].name)
    }

    /** `deleteAllProducts` debe dejar la tabla vacía (acción "Vaciar inventario" de la UI). */
    @Test
    fun vaciarInventario_dejaLaTablaVacia() = runBlocking {
        dao.insertProduct(ProductEntity(name = "A", price = 1.0, stock = 1, category = "X"))
        dao.insertProduct(ProductEntity(name = "B", price = 2.0, stock = 2, category = "X"))

        dao.deleteAllProducts()

        assertTrue(dao.getAllProducts().first().isEmpty())
    }

    private companion object {
        /** Margen para que la invalidación de Room propague la emisión del Flow. */
        const val TIEMPO_ESPERA_MS = 5_000L
    }
}
