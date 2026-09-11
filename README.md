# FieldInventory — Proyecto Educativo de Persistencia Local

FieldInventory es una aplicación Android diseñada para demostrar de manera práctica y didáctica los mecanismos de **persistencia de datos local** en el ecosistema moderno de Android.

## 🎯 Objetivo del Proyecto
El propósito central es enseñar cómo una aplicación puede conservar información de forma permanente en el dispositivo, permitiendo que los datos sobrevivan al cierre de la aplicación o al reinicio del dispositivo.

## 🛠️ Tecnologías Utilizadas
- **Kotlin**: Lenguaje de programación principal.
- **Jetpack Compose**: Kit de herramientas moderno para construir interfaces nativas.
- **Room Database**: Abstracción sobre SQLite para el almacenamiento de datos estructurados (Productos).
- **DataStore Preferences**: Solución moderna para guardar preferencias de usuario y datos simples (Umbral de stock).
- **Kotlin Coroutines & Flow**: Manejo de operaciones asíncronas y flujos de datos reactivos.
- **ViewModel**: Gestión de estados de UI consciente del ciclo de vida.
- **Material 3**: Sistema de diseño para una interfaz profesional y accesible.

## 🏗️ Arquitectura
El proyecto sigue un flujo de datos claro y unidireccional:
`UI (Compose) -> ViewModel -> Repository -> Local Data (Room / DataStore)`

Y una observación reactiva para las actualizaciones:
`Room (Flow) -> Repository -> ViewModel (StateFlow) -> UI (Compose Recomposition)`

## 📁 Estructura del Código
- `data/local`: Definición de la Entidad, el DAO y la Base de Datos Room.
- `data/preferences`: Gestión de configuraciones mediante DataStore.
- `repository`: Capa intermedia que centraliza el acceso a todos los datos.
- `viewmodel`: Lógica de negocio y preparación del estado para la interfaz.
- `ui`: Componentes visuales, pantallas y temas de diseño.

## 🚀 Cómo Probar la Persistencia
1. **Cargar datos**: Use el botón "Cargar datos de prueba" en la sección de información o agregue productos manualmente.
2. **Cerrar la App**: Cierre completamente la aplicación desde el gestor de tareas de Android.
3. **Reabrir**: Al volver a entrar, todos los productos y el umbral configurado seguirán allí.
4. **Reactividad**: Cambie el umbral de stock en los ajustes y observe cómo las tarjetas de producto se actualizan instantáneamente sin recargar la pantalla.

## 📝 Validaciones Implementadas
- Los nombres y categorías no pueden estar vacíos.
- El precio y el stock deben ser valores numéricos no negativos.
- El stock nunca puede disminuir por debajo de cero.
