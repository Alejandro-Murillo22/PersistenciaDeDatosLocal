# Guía de pruebas en vivo — FieldInventory

Guion paso a paso para demostrar, frente al profesor, que **Room** conserva los datos en disco y
que la app funciona **100 % offline**. Cada escenario trae: qué se prueba, los pasos numerados, los
comandos `adb` exactos y **la frase concreta que hay que decir** al sustentar.

---

## 0. Preparación (hacer una sola vez, antes de sustentar)

Datos del proyecto que se repiten en todos los comandos:

| Dato | Valor |
|---|---|
| Package / applicationId | `com.example.persistencialocal` |
| Activity de arranque | `com.example.persistencialocal/.MainActivity` |
| Archivo de Room (SQLite) | `/data/data/com.example.persistencialocal/databases/field_inventory_db` |
| Archivo de DataStore | `/data/data/com.example.persistencialocal/files/datastore/settings.preferences_pb` |

Pasos:

1. Arrancar el emulador (o conectar el teléfono con **Depuración USB** activada).
2. Verificar que `adb` ve el dispositivo. Debe listar exactamente uno:
   ```bash
   adb devices
   ```
   > Si aparecen varios, agregar `-s <serial>` a **todos** los comandos de esta guía,
   > por ejemplo `adb -s emulator-5554 shell am kill com.example.persistencialocal`.
3. Instalar la app compilada desde cero:
   ```bash
   ./gradlew installDebug
   ```
4. Abrirla una vez desde el launcher y pulsar el icono **ℹ️ (Información) → "Cargar Muestras"**
   para tener 5 productos de arranque.
5. Dejar **dos ventanas abiertas** durante la sustentación: el emulador y una terminal ya ubicada
   en la carpeta del proyecto. Así no se pierde tiempo buscando comandos.

> **Consejo de sustentación:** antes de empezar, aclarar la diferencia que se va a demostrar:
> *"Voy a mostrar dos cosas distintas que suelen confundirse: que el ESTADO sobrevive a la
> rotación (eso es mérito del ViewModel, en memoria RAM) y que los DATOS sobreviven a la muerte
> del proceso (eso es mérito de Room, en disco). Son mecanismos diferentes."*

---

## Escenario 1 — Cierre normal y reapertura

**Qué prueba:** que los datos no viven solo en la pantalla; al salir y volver a entrar siguen ahí.
Es el caso base.

1. Abrir la app desde el launcher.
2. Pulsar el botón **+** y crear un producto nuevo: nombre `Prueba Sustentación`, precio `99`,
   stock `3`, categoría `Demo`. Confirmar.
3. Pulsar **⋮ → Configurar Umbral** y cambiar el valor de `5` a `2`. Guardar.
4. Anotar en voz alta qué hay en pantalla: el producto nuevo y el umbral en 2.
5. Cerrar la app con el **botón/gesto de atrás** hasta salir al launcher.
6. Volver a abrirla desde el icono del launcher.
7. Verificar que `Prueba Sustentación` sigue en la lista y que el umbral sigue en `2`
   (**⋮ → Configurar Umbral** debe mostrar `2`, no `5`).

**Qué decir:** *"Esto prueba lo mínimo: que los datos se escribieron y se volvieron a leer. Todavía
no prueba que estén en disco, porque Android pudo haber mantenido el proceso vivo en background.
Eso lo demuestro en el siguiente caso."*

---

## Escenario 2 — Cierre forzado (Force stop / deslizar de recientes)

**Qué prueba:** que los datos están en el **archivo SQLite en disco**, no en la memoria del
proceso. Al forzar la detención, Android destruye el proceso entero: se pierde toda la RAM de la
app, incluidos el ViewModel y los StateFlow.

1. Con la app abierta y con datos visibles, crear otro producto: `Forzado`, stock `8`.
2. Matar el proceso **por cualquiera de estas tres vías** (la primera es la más visual para el
   profesor):
   - **Ajustes del dispositivo:** `Ajustes → Aplicaciones → FieldInventory → Forzar detención`
     y aceptar la advertencia.
   - **Recientes:** abrir la vista de apps recientes y deslizar FieldInventory fuera de la pantalla.
   - **Por terminal** (equivalente exacto al "Forzar detención" del sistema):
     ```bash
     adb shell am force-stop com.example.persistencialocal
     ```
3. Confirmar que el proceso ya no existe. El comando no debe imprimir **nada**:
   ```bash
   adb shell pidof com.example.persistencialocal
   ```
4. Reabrir la app desde el launcher.
5. Verificar que `Forzado` y todos los demás productos siguen ahí, y el umbral también.

**Opcional (muy convincente):** mostrar el archivo físico de la base de datos:
```bash
adb shell run-as com.example.persistencialocal ls -l databases/
```
Deben aparecer `field_inventory_db`, `field_inventory_db-shm` y `field_inventory_db-wal`.

**Qué decir:** *"Esto prueba que los datos sobreviven a la destrucción del proceso, no solo al
cierre normal. `Forzar detención` libera toda la memoria de la app; si los productos estuvieran en
una lista en RAM, habrían desaparecido. Siguen ahí porque Room los escribió en un archivo SQLite
dentro del almacenamiento privado de la app."*

---

## Escenario 3 — Muerte de proceso decretada por el sistema (*process death*)

**Qué prueba:** el caso real más común y el que más se olvida: Android mata la app en background
cuando necesita RAM, **sin que el usuario haga nada**. No se usa la UI para cerrarla.

1. Con la app abierta, crear el producto `Process Death`, stock `1`.
2. Mandar la app a **background** sin cerrarla (pulsar el botón **Home**, no atrás).
   O por terminal:
   ```bash
   adb shell input keyevent KEYCODE_HOME
   ```
3. Matar el proceso simulando lo que hace el sistema por falta de memoria:
   ```bash
   adb shell am kill com.example.persistencialocal
   ```
   > `am kill` solo mata procesos que estén en background y sean *seguros de matar*, que es
   > exactamente el criterio que usa Android en condiciones de poca memoria. Por eso es la
   > simulación fiel de *process death*, mientras que `am force-stop` (Escenario 2) es la acción
   > deliberada del usuario. Si la app está en primer plano, `am kill` no hace nada: por eso el
   > paso 2 es obligatorio.
4. Confirmar que el proceso murió (no debe imprimir nada):
   ```bash
   adb shell pidof com.example.persistencialocal
   ```
5. Reabrir la app (desde recientes o desde el launcher):
   ```bash
   adb shell am start -n com.example.persistencialocal/.MainActivity
   ```
6. Verificar que `Process Death` y el resto del inventario siguen intactos.

**Qué decir:** *"Esto prueba que los datos sobreviven a que el sistema operativo mate la app por su
cuenta, que es lo que pasa en un teléfono real cuando el usuario abre otras apps. No hice nada en
la interfaz: el proceso simplemente dejó de existir. Room ya había hecho commit en disco en cada
operación, así que al reabrir se leen de nuevo desde SQLite."*

---

## Escenario 4 — Rotación de pantalla (cambio de configuración)

**Qué prueba:** algo **distinto** a la persistencia en disco. Aquí la Activity se destruye y se
recrea, pero el proceso sigue vivo; quien conserva el estado es el **ViewModel**, no Room. Conviene
mostrarlo aparte justamente para no mezclar los dos conceptos.

1. Abrir la app con datos cargados.
2. Activar la rotación automática en el emulador/dispositivo.
3. Escribir algo en el **buscador** (icono 🔍), por ejemplo `Lap`, para que haya estado de UI vivo.
4. Rotar el dispositivo a horizontal:
   ```bash
   adb shell settings put system accelerometer_rotation 0
   adb shell settings put system user_rotation 1
   ```
5. Verificar que:
   - La lista **no** queda vacía ni parpadea recargando.
   - Los productos **no** se duplican.
   - El filtro de búsqueda se mantiene aplicado.
6. Volver a vertical:
   ```bash
   adb shell settings put system user_rotation 0
   ```
7. Repetir la rotación 3 o 4 veces seguidas y confirmar que el número de "Items" del resumen
   superior no cambia.

**Qué decir:** *"Ojo, esto NO es persistencia en disco. Al rotar, Android destruye y recrea la
Activity, pero el proceso sigue vivo y el ViewModel sobrevive a esa recreación. Como la lista
está en un StateFlow dentro del ViewModel y no en una variable de la Activity, no se recarga ni se
duplica. Es el mecanismo complementario: el ViewModel resuelve los cambios de configuración, Room
resuelve la muerte del proceso."*

> **Por qué no se duplica:** los productos se insertan solo cuando el usuario pulsa un botón, nunca
> en `onCreate`, y el `StateFlow` usa `SharingStarted.WhileSubscribed(5000)`: durante la rotación
> la suscripción se mantiene viva 5 segundos, así que ni siquiera se vuelve a consultar la base.
>
> Matiz honesto que conviene reconocer si el profesor pregunta: el texto del buscador vive en el
> ViewModel (`_searchQuery`), así que sobrevive a la rotación; pero el estado puramente visual de
> los diálogos (`remember { mutableStateOf(...) }` dentro del Composable) **no** sobrevive, y por
> eso un diálogo abierto se cierra al rotar. Es el comportamiento esperado para estado efímero de
> UI, y refuerza la diferencia entre las tres capas: disco (Room), ViewModel y composición.

---

## Escenario 5 — Reinstalación (contraste: aquí SÍ se borra)

**Qué prueba:** el límite del alcance. La persistencia local es **de la app**: si se desinstala, el
sistema borra su almacenamiento privado y con él la base de datos. Sirve como contraste para que
quede claro qué sobrevive y qué no.

1. Antes de desinstalar, mostrar que hay datos en pantalla y contar cuántos items hay.
2. Desinstalar:
   ```bash
   adb uninstall com.example.persistencialocal
   ```
3. Confirmar que el directorio de datos ya no existe (debe dar error o "No such file"):
   ```bash
   adb shell run-as com.example.persistencialocal ls databases/
   ```
4. Reinstalar:
   ```bash
   ./gradlew installDebug
   ```
5. Abrir la app: debe aparecer **el estado vacío** ("no hay productos") y el umbral de vuelta en su
   valor por defecto `5`.

> ✅ **Este escenario está blindado.** El `AndroidManifest.xml` declara
> `android:allowBackup="false"` de forma deliberada. Con el valor por defecto de Android Studio
> (`"true"`), en un dispositivo físico con cuenta de Google y Auto Backup activo la reinstalación
> podría **restaurar** la base de datos desde la nube y la demostración diría justo lo contrario de
> lo que se quiere enseñar. Con `false`, el sistema nunca sube una copia, así que los datos se
> borran siempre. La razón completa está en la sección "Decisiones de diseño" del README.
>
> Aun así, si se quiere borrar los datos sin desinstalar (por ejemplo para repetir la demo rápido):
> ```bash
> adb shell pm clear com.example.persistencialocal
> ```
> que borra el almacenamiento privado de la app sin desinstalarla.

**Qué decir:** *"Este caso es el contraste. La persistencia local es privada de la aplicación:
vive en `/data/data/<package>/`, un directorio que solo esta app puede leer. Si se desinstala, el
sistema borra ese directorio completo. Eso es lo esperado en Room por defecto y marca la frontera
entre 'persistencia local' y 'persistencia en la nube'."*

---

## Escenario 6 — Reactividad sin reiniciar (Flow de DataStore)

**Qué prueba:** que la UI se actualiza sola cuando cambia una preferencia, sin cerrar ni recargar
nada. Esto no demuestra persistencia: demuestra el **flujo reactivo**.

1. Asegurarse de tener productos con stocks variados. Si no, usar
   **ℹ️ → "Cargar Muestras"**: deja stocks de 10, 3, 0, 5 y 20.
2. Dejar el umbral en `2`. Observar los indicadores de color de las tarjetas: con umbral 2, el
   producto de stock 3 debe decir **"Stock normal"** (verde).
3. **Sin cerrar nada**, abrir **⋮ → Configurar Umbral**, poner `5` y pulsar Guardar.
4. En el instante en que se cierra el diálogo, verificar que:
   - El producto de stock 3 pasó a **"Stock bajo"** (naranja).
   - El producto de stock 5 también pasó a **"Stock bajo"**.
   - El contador **"Bajo"** de la tarjeta de resumen superior subió.
5. Repetirlo al revés (de `5` a `1`) para que se vea el cambio en vivo otra vez.

**Qué decir:** *"Aquí no cerré ni recargué nada. Al guardar el umbral, DataStore emite el nuevo
valor por un Flow; el ViewModel lo expone como StateFlow y Compose recompone solo las tarjetas
afectadas. Esto es el flujo reactivo: la UI no consulta, se suscribe. Y de paso el valor quedó
guardado en disco, como ya demostré en el Escenario 1."*

---

## Escenario 7 — Ciclo completo en modo avión (prueba central del concepto)

**Qué prueba:** lo esencial de la actividad. La app hace **todo** su trabajo sin red, porque nunca
la necesitó.

1. **Antes de abrir la app**, activar el modo avión:
   - En la UI: `Ajustes → Red e Internet → Modo avión`, o desplegar el panel rápido.
   - Por terminal (abre directamente el panel de ajustes correspondiente):
     ```bash
     adb shell am start -a android.settings.AIRPLANE_MODE_SETTINGS
     ```
   - En un emulador también sirve apagar wifi y datos explícitamente:
     ```bash
     adb shell svc wifi disable
     adb shell svc data disable
     ```
2. Confirmar que efectivamente no hay red (debe fallar, ese es el resultado esperado):
   ```bash
   adb shell ping -c 2 8.8.8.8
   ```
3. Abrir la app **ya sin conexión** y hacer el ciclo completo, sin saltarse ningún paso:
   1. Crear dos productos nuevos desde el botón **+**.
   2. Editar uno de ellos y cambiarle nombre y precio.
   3. Subir y bajar el stock con los botones **+ / −** de una tarjeta.
   4. Buscar un producto con el 🔍 y comprobar que filtra.
   5. Cambiar el umbral desde **⋮ → Configurar Umbral**.
   6. Eliminar un producto y confirmar el diálogo.
4. Cerrar la app por completo, **todavía en modo avión**:
   ```bash
   adb shell am force-stop com.example.persistencialocal
   ```
5. Reabrirla, **todavía en modo avión**, y verificar que todos los cambios del paso 3 siguen ahí.
6. Confirmar en voz alta que en ningún momento hubo un *spinner* de carga, un error de red ni un
   congelamiento.
7. Restaurar la conexión al terminar:
   ```bash
   adb shell svc wifi enable
   adb shell svc data enable
   ```

**Prueba documental que acompaña la demo** — mostrar el `AndroidManifest.xml` en pantalla y señalar
que **no existe** la línea `<uses-permission android:name="android.permission.INTERNET" />`:
```bash
grep -i "uses-permission" app/src/main/AndroidManifest.xml
```
El comando **no devuelve nada**: la app no declara ningún permiso.

**Qué decir (esto es lo más importante de la sustentación):** *"Fíjese en el matiz: la app no es que
'resista' la falta de conexión, es que nunca la necesitó. En el Manifest no hay ni un solo permiso
declarado, en particular no está `android.permission.INTERNET`, y en el código no hay ninguna
llamada de red. Android bloquearía cualquier intento de abrir un socket sin ese permiso. Esa es
justamente la esencia de la persistencia local frente a una solución en la nube: los datos nacen,
se guardan y se leen en el propio dispositivo, sin backend, sin latencia y sin depender de que haya
señal."*

---

## Tabla resumen para la sustentación

| # | Escenario | ¿Sobreviven los datos? | Mecanismo que lo explica |
|---|---|---|---|
| 1 | Cierre normal y reapertura | Sí | Room (SQLite en disco) |
| 2 | Force stop / deslizar de recientes | Sí | Room (el proceso muere, el archivo no) |
| 3 | `am kill` (process death) | Sí | Room (el SO mata la app, los datos ya estaban en disco) |
| 4 | Rotación de pantalla | Sí, y sin recargar | **ViewModel + StateFlow** (RAM, no disco) |
| 5 | Desinstalar y reinstalar | No, y es lo correcto | El SO borra `/data/data/<package>/` |
| 6 | Cambiar el umbral en caliente | Sí, y se refleja al instante | **Flow de DataStore** (reactividad) |
| 7 | Ciclo completo en modo avión | Sí | No hay red involucrada en ningún punto |

## Comandos, todos juntos (chuleta)

```bash
adb devices                                                        # dispositivos conectados
./gradlew installDebug                                             # instalar
adb shell am start -n com.example.persistencialocal/.MainActivity  # abrir
adb shell input keyevent KEYCODE_HOME                              # mandar a background
adb shell am kill com.example.persistencialocal                    # process death (background)
adb shell am force-stop com.example.persistencialocal              # cierre forzado (usuario)
adb shell pidof com.example.persistencialocal                      # ¿sigue vivo el proceso?
adb shell run-as com.example.persistencialocal ls -l databases/    # ver el archivo SQLite
adb shell settings put system user_rotation 1                      # rotar a horizontal
adb shell settings put system user_rotation 0                      # rotar a vertical
adb shell svc wifi disable && adb shell svc data disable           # apagar la red
adb shell svc wifi enable  && adb shell svc data enable            # encender la red
adb shell pm clear com.example.persistencialocal                   # borrar datos sin desinstalar
adb uninstall com.example.persistencialocal                        # desinstalar
```
