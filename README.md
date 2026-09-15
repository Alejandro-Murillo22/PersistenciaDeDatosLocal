# FieldInventory — Persistencia Local en Android

Aplicación Android de inventario de campo, construida como actividad académica para demostrar de
forma práctica la **persistencia de datos local offline**.

---

## 1. Característica evaluada

> **El foco de este proyecto es Room** (la capa de abstracción de Jetpack sobre SQLite) como
> mecanismo de persistencia local estructurada.

Room guarda el inventario —el dato con estructura, relaciones y consultas— en un archivo SQLite
dentro del almacenamiento privado de la app. Es lo que hay que mirar al evaluar.

**DataStore es complementario, no el foco.** Solo se usa para una preferencia simple: el *umbral de
stock bajo* (un único `Int`). Está incluido a propósito para mostrar el criterio de elección entre
los dos mecanismos, que es una decisión de diseño real:

| | **Room** (el foco) | **DataStore** (complemento) |
|---|---|---|
| Qué guarda aquí | La lista de productos | El umbral de stock bajo |
| Forma del dato | Registros estructurados, muchos | Un par clave-valor |
| Consultas | SQL (`SELECT ... ORDER BY`), filtros, joins | Lectura directa de la clave |
| Cuándo usarlo | Colecciones que crecen y se consultan | Ajustes y banderas del usuario |

La regla que se demuestra: *si el dato se consulta, se ordena o se relaciona, va en Room; si es un
ajuste suelto, va en DataStore.*

La app **no tiene ningún permiso declarado en el Manifest** (en particular, no tiene
`android.permission.INTERNET`) ni una sola llamada de red. Funciona íntegramente offline no porque
tolere la falta de conexión, sino porque nunca la necesitó.

---

## 2. Arquitectura

Flujo de escritura, unidireccional:

```
UI (Compose)  ->  ViewModel  ->  Repository  ->  Room / DataStore
```

Flujo de lectura, reactivo:

```
Room (Flow)  ->  Repository (Flow)  ->  ViewModel (StateFlow)  ->  UI (recomposición)
```

La clave del segundo flujo es que **la UI no consulta: se suscribe**. El DAO devuelve un
`Flow<List<ProductEntity>>`, así que cualquier cambio en la tabla `products` dispara una nueva
emisión y Compose recompone solo lo afectado. No hay ningún "refrescar" manual en el código.

### Estructura del código

| Paquete | Responsabilidad |
|---|---|
| `data/local` | `ProductEntity` (la tabla), `ProductDao` (las consultas), `AppDatabase` (la BD) |
| `data/preferences` | `SettingsManager` — DataStore para el umbral de stock |
| `repository` | `InventoryRepository` — única puerta de acceso a los datos |
| `viewmodel` | `InventoryViewModel` — estado de UI, validaciones y `StateFlow` |
| `ui` | Pantalla, componentes y tema Material 3 |

### Stack

Kotlin · Jetpack Compose · Material 3 · Room 2.6.1 · DataStore Preferences 1.1.1 ·
Coroutines & Flow · ViewModel · KSP · AGP 8.7.3 / Gradle 9.1 · minSdk 24 / targetSdk 35 · Java 17

---

## 3. Compilar y ejecutar desde cero

**Requisitos:** JDK 17, Android SDK con la plataforma 35 instalada, y un emulador o dispositivo con
API 24 o superior.

1. Clonar el repositorio y entrar en la carpeta.
2. Crear `local.properties` en la raíz apuntando al SDK (Android Studio lo genera solo al abrir el
   proyecto; si se compila solo por terminal, hay que escribirlo a mano):
   ```properties
   sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
   ```
   En macOS/Linux: `sdk.dir=/Users/<usuario>/Library/Android/sdk`.
3. Compilar el APK de debug:
   ```bash
   ./gradlew assembleDebug
   ```
   El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
4. Arrancar el emulador y comprobar que `adb` lo ve:
   ```bash
   adb devices
   ```
5. Instalar y abrir:
   ```bash
   ./gradlew installDebug
   adb shell am start -n com.example.persistencialocal/.MainActivity
   ```
6. Dentro de la app, pulsar **ℹ️ → "Cargar Muestras"** para insertar 5 productos de ejemplo.

En Windows usar `gradlew.bat` en lugar de `./gradlew` si se está en `cmd` o PowerShell.

Para una compilación totalmente limpia: `./gradlew clean assembleDebug`.

---

## 4. Tests

Los tests que importan son **instrumentados** (`app/src/androidTest/`), porque Room necesita el
motor SQLite real de Android: un test puramente JVM no probaría la persistencia de verdad.

`ProductDaoTest` usa `Room.inMemoryDatabaseBuilder()`, que crea una base de datos que vive solo en
memoria y se destruye al terminar cada caso. Se ejercita exactamente el mismo `ProductDao` que usa
la app en producción, pero sin tocar los datos del dispositivo ni contaminar un test con otro.

Cubre:

| Test | Qué verifica |
|---|---|
| `insertarProducto_quedaDisponibleEnLaConsulta` | Insertar y volver a consultar el producto, con id autogenerado |
| `actualizarStock_persisteElNuevoValor` | Cambiar el stock y releerlo **desde la base**, no del objeto en memoria |
| `eliminarProducto_desapareceDeLaConsulta` | Tras borrar, el producto ya no aparece en la consulta |
| `consultarProductos_devuelveOrdenadoPorNombre` | El `ORDER BY name ASC` lo aplica SQLite, no el ViewModel |
| `flowDeProductos_emiteDeNuevoAlInsertar` | Un único colector recibe una emisión nueva tras un insert: la base de la reactividad |
| `vaciarInventario_dejaLaTablaVacia` | La acción "Vaciar inventario" deja la tabla sin filas |

### Cómo correrlos

Con un emulador arrancado o un dispositivo conectado:

```bash
./gradlew connectedDebugAndroidTest
```

`./gradlew connectedAndroidTest` hace lo mismo (corre todas las variantes); en este proyecto, con
una sola variant de debug, ambos comandos son equivalentes.

Para correr solo la clase de tests de Room:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.persistencialocal.data.local.ProductDaoTest
```

Para verificar que los tests **compilan** sin necesidad de un dispositivo:

```bash
./gradlew assembleDebugAndroidTest
```

El informe HTML de resultados queda en
`app/build/reports/androidTests/connected/debug/index.html`.

> Los archivos `ExampleUnitTest.kt` y `ExampleInstrumentedTest.kt` que genera Android Studio por
> defecto fueron eliminados: comprobaban `2 + 2 == 4` y el nombre del paquete, es decir, nada
> relacionado con la persistencia.

---

## 5. Guía de pruebas en vivo

Para la sustentación hay un documento aparte con el guion completo:

### 👉 **[PRUEBAS.md](PRUEBAS.md)**

Cubre siete escenarios, cada uno con pasos numerados, los comandos `adb` exactos y la frase que hay
que decirle al profesor en cada caso:

1. Cierre normal y reapertura.
2. Cierre forzado (*Force stop* / deslizar de recientes).
3. Muerte de proceso decretada por el sistema (`adb shell am kill com.example.persistencialocal`).
4. Rotación de pantalla — que es **ViewModel**, no persistencia en disco; se muestra aparte
   justamente para no confundir los dos conceptos.
5. Reinstalación — aquí los datos **sí** se borran, y se usa como contraste deliberado.
6. Cambiar el umbral en caliente y ver las tarjetas cambiar de color al instante (Flow de DataStore).
7. Ciclo completo en modo avión: la prueba central del concepto "persistencia local offline".

---

## 6. Decisiones de diseño

### Por qué `AndroidViewModel` con instanciación manual, y no Hilt

`InventoryViewModel` extiende `AndroidViewModel` y construye sus propias dependencias en el bloque
`init`:

```kotlin
init {
    val database = AppDatabase.getDatabase(application)
    val settingsManager = SettingsManager(application)
    repository = InventoryRepository(database.productDao(), settingsManager)
}
```

Es una **decisión educativa deliberada, no una omisión.** El objetivo de la actividad es que se vea
la cadena de persistencia completa, y aquí queda escrita en cinco líneas legibles: de dónde sale la
base de datos, de dónde sale el DAO y cómo llegan al repositorio. Con Hilt ese mismo cableado
desaparece dentro de anotaciones (`@HiltViewModel`, `@Inject`, `@Module`, `@Provides`) y de código
generado, y lo que se estaría evaluando sería el grafo de inyección, no Room.

Ventajas concretas en este contexto:

- **Cero configuración añadida:** ni plugin de Hilt, ni `Application` anotada, ni módulos. El
  proyecto compila con las dependencias mínimas para el tema evaluado.
- **Trazabilidad:** se puede seguir con el dedo el camino `MainActivity → ViewModel → Repository →
  DAO → SQLite` sin salir de tres archivos.
- **`AndroidViewModel` en vez de `ViewModel` a secas:** Room y DataStore necesitan un `Context`.
  `AndroidViewModel` entrega el `Application` context, que vive tanto como el proceso y por tanto
  **no filtra la Activity**. Usar el contexto de la Activity aquí sí sería un error real.
- **`viewModel()` sin factory:** al no haber parámetros en el constructor más allá del
  `Application`, Compose puede crear el ViewModel sin una `ViewModelProvider.Factory` a medida, lo
  que elimina otra pieza de andamiaje del ejemplo.

**El límite de este enfoque, dicho explícitamente:** el ViewModel queda acoplado a implementaciones
concretas (`AppDatabase`, `SettingsManager`), así que no se puede sustituir el repositorio por un
doble de prueba en un test unitario. Es precisamente el problema que resuelve la inyección de
dependencias, y es la razón por la que en una app de producción se usaría Hilt. En este proyecto la
persistencia se prueba donde corresponde: contra Room de verdad, en los tests instrumentados del
punto 4.

### Validaciones que fallan fuerte en vez de fallar en silencio

`InventoryRepository.updateStock()` y `saveLowStockThreshold()` usan `require(...)` y lanzan
`IllegalArgumentException` ante un valor negativo, en vez de descartar la escritura sin avisar.

Descartar en silencio es peor que fallar: la UI seguiría mostrando el valor que el usuario
introdujo, pero nunca se habría guardado, y el error solo aparecería mucho después —al reabrir la
app— sin ninguna pista de su origen.

Esto es una **defensa adicional de la capa de datos, no un reemplazo** de la validación de la capa
superior. El `InventoryViewModel` sigue siendo quien decide si la operación procede (el chequeo de
`stock > 0` antes de decrementar y el tope `MAX_STOCK` antes de incrementar), así que en uso normal
la excepción nunca se lanza. Existe para que un error de programación futuro —una nueva llamada que
olvide validar— falle de forma visible. Del lado del ViewModel, las llamadas van dentro de
`try/catch` igual que `addProduct`/`updateProduct`/`deleteProduct`, de modo que cualquier fallo
inesperado (incluido un error de escritura de Room) llega al usuario como *snackbar* en lugar de
tumbar la corrutina.

### Migraciones: `fallbackToDestructiveMigration()`

`AppDatabase` está en `version = 1` y el builder declara `fallbackToDestructiveMigration()`, también
de forma deliberada y comentada en el propio código.

Si se sube el número de versión sin decirle a Room cómo transformar el esquema existente, la app
**crashea al arrancar** en cualquier dispositivo que ya tuviera instalada la versión anterior
(`IllegalStateException: A migration from 1 to 2 was required but not found`).
`fallbackToDestructiveMigration()` evita ese crash borrando y recreando las tablas, a costa de
**perder los datos del usuario**.

Para una demostración académica es el comportamiento correcto: el esquema puede cambiar mientras se
explica el tema y la demo no se cae. **En producción sería inaceptable** —borraría el inventario
real de cada usuario en una actualización— y lo correcto sería escribir una `Migration` explícita
(`addMigrations(MIGRATION_1_2)` con su `ALTER TABLE`), poner `exportSchema = true` para versionar el
esquema en Git, y cubrirla con un test de migración usando `MigrationTestHelper`.

### `android:allowBackup="false"` en el Manifest

Android Studio genera los proyectos con `android:allowBackup="true"`, que activa **Auto Backup**:
el sistema sube periódicamente a Google Drive el almacenamiento privado de la app —incluida la base
de datos de Room— y lo **restaura automáticamente** al reinstalarla en un dispositivo con la misma
cuenta.

Aquí está puesto en `false` a propósito, por dos razones:

1. **Rompería el Escenario 5 de [PRUEBAS.md](PRUEBAS.md).** Ese escenario demuestra, como contraste
   deliberado, que desinstalar la app borra sus datos. Con Auto Backup activo en un teléfono físico
   con cuenta de Google, la reinstalación podría **restaurar** el inventario desde la nube y la
   demostración diría exactamente lo contrario de lo que se quiere enseñar.
2. **Contradice el concepto evaluado.** La actividad trata de persistencia *local*: los datos viven
   y mueren en el dispositivo. Auto Backup mete una copia en la nube por la puerta de atrás, que es
   justo el mecanismo del que se quiere diferenciar.

En una app real la decisión sería la contraria en la mayoría de los casos: se deja el backup activo
para no perder los datos del usuario al cambiar de teléfono, y se excluyen solo los archivos
sensibles mediante `@xml/backup_rules` y `@xml/data_extraction_rules`.

### Validación de stock: por qué los dos límites no son simétricos

`decreaseStock` bloquea en 0 e `increaseStock` tope en `MAX_STOCK` (9 999), pero las dos reglas
tienen naturaleza distinta y conviene decirlo:

- El **límite inferior (0) es una regla del dominio**: no existe un stock negativo. Bloquearlo es
  obligatorio.
- El **límite superior es una regla arbitraria de negocio**. Se aplica de todos modos por una razón
  técnica concreta: `stock` es un `Int`, y un `+1` repetido sin tope acabaría desbordando a negativo
  (`Int.MAX_VALUE + 1`), dejando la base de datos en un estado imposible. El tope es holgado para un
  inventario de campo y se valida en el mismo punto y con el mismo tipo de mensaje que el inferior.

---

## 7. Validaciones implementadas

- Nombre y categoría no pueden quedar vacíos.
- Precio y stock deben ser numéricos y no negativos.
- Los botones **+ / −** de la tarjeta no bajan el stock de 0 ni lo suben por encima de `MAX_STOCK`
  (9 999); en ambos casos se avisa por *snackbar*. (El formulario de alta/edición valida que el
  stock sea numérico y no negativo, pero no aplica el tope superior: ese límite es una guarda del
  incremento repetido, explicada arriba.)
- El umbral de stock bajo no admite valores negativos.
- Borrar un producto y vaciar el inventario exigen confirmación en un diálogo.
