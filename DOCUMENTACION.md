# SafeZone AI - Documentacion Completa del Proyecto

## Tabla de Contenidos
1. [Descripcion General](#1-descripcion-general)
2. [Stack Tecnologico](#2-stack-tecnologico)
3. [Estructura del Proyecto](#3-estructura-del-proyecto)
4. [Arquitectura MVVM](#4-arquitectura-mvvm)
5. [Patrones de Diseno](#5-patrones-de-diseno)
6. [Principios SOLID](#6-principios-solid)
7. [Requerimientos Funcionales](#7-requerimientos-funcionales)
8. [Requerimientos No Funcionales](#8-requerimientos-no-funcionales)
9. [Flujos Principales de la Aplicacion](#9-flujos-principales-de-la-aplicacion)
10. [Capa de Datos (Room)](#10-capa-de-datos-room)
11. [Servicios en Background](#11-servicios-en-background)
12. [Permisos de la Aplicacion](#12-permisos-de-la-aplicacion)
13. [Limitaciones Conocidas](#13-limitaciones-conocidas)
14. [Mejoras a Futuro](#14-mejoras-a-futuro)
15. [Estadisticas del Proyecto](#15-estadisticas-del-proyecto)

---

## 1. Descripcion General

**SafeZone AI** es una aplicacion Android de seguridad personal desarrollada en Kotlin con Jetpack Compose. Su objetivo principal es proteger al usuario en situaciones de peligro mediante:

- Deteccion automatica de zonas peligrosas por GPS
- Activacion de emergencia con boton en pantalla o 3 pulsaciones rapidas del boton de volumen (incluso en background)
- Envio automatico de SMS con ubicacion a contactos de emergencia
- Grabacion de audio como evidencia durante la emergencia
- Comparticion de ubicacion y audio por WhatsApp
- Monitoreo continuo de ubicacion en segundo plano

---

## 2. Stack Tecnologico

| Tecnologia | Version | Uso |
|---|---|---|
| **Kotlin** | 1.9.x | Lenguaje principal |
| **Jetpack Compose** | BOM 2024.10.00 | Framework de UI declarativa |
| **Material 3** | (via Compose BOM) | Sistema de diseno |
| **Room** | 2.6.1 | Base de datos local (SQLite) |
| **Coroutines** | 1.7.3 | Programacion asincrona |
| **Play Services Location** | 21.1.0 | GPS (FusedLocationProviderClient) |
| **osmdroid** | 6.1.18 | Mapas OpenStreetMap (gratuito, sin API key) |
| **Accompanist Permissions** | 0.32.0 | Manejo de permisos en Compose |
| **SplashScreen** | core-splashscreen | Pantalla de inicio animada |
| **Gradle Kotlin DSL** | — | Sistema de compilacion |

**Compilacion:** compileSdk 35 | targetSdk 34 | minSdk 28 (Android 9+) | Java 17

---

## 3. Estructura del Proyecto

```
app/src/main/java/com/example/safezoneai/
│
├── MainActivity.kt                    (413 lineas) - Punto de entrada, permisos, navegacion
│
├── ui/                                (Capa de Presentacion - Views)
│   ├── HomeScreen.kt                  (558 lineas) - Pantalla principal con boton de emergencia
│   ├── EmergencyScreen.kt             (411 lineas) - Pantalla activa durante emergencia
│   ├── MapScreen.kt                   (804 lineas) - Mapa OpenStreetMap con zonas peligrosas
│   ├── ContactScreen.kt               (816 lineas) - CRUD de contactos de emergencia
│   ├── SettingsScreen.kt              (390 lineas) - Configuracion de la app
│   ├── HistoryScreen.kt               (399 lineas) - Historial de emergencias activadas
│   ├── SplashScreen.kt                (89 lineas)  - Splash animado al iniciar
│   ├── WhatsappButton.kt              (166 lineas) - Componente boton WhatsApp
│   └── theme/
│       ├── Color.kt                   (108 lineas) - Paleta de colores
│       ├── Theme.kt                   (215 lineas) - Tema Material 3
│       └── Type.kt                    (205 lineas) - Tipografia
│
├── viewmodel/                         (Capa de Logica - ViewModels)
│   ├── EmergencyViewModel.kt          (615 lineas) - ViewModel central de emergencia
│   ├── SettingsViewModel.kt           (96 lineas)  - Configuracion con SharedPreferences
│   └── HistoryViewModel.kt            (83 lineas)  - Consulta de historial
│
├── data/                              (Capa de Datos - Model)
│   ├── SmartZoneDetector.kt           (388 lineas) - Deteccion de ciudad + zonas peligrosas
│   ├── ZoneRepository.kt              (109 lineas) - Repositorio de zonas
│   └── local/
│       ├── AppDatabase.kt             (49 lineas)  - Room Database (Singleton)
│       ├── EmergencyContact.kt        (18 lineas)  - Entity: contacto de emergencia
│       ├── EmergencyRecord.kt         (21 lineas)  - Entity: registro de emergencia
│       ├── EmergencyDao.kt            (60 lineas)  - DAO: operaciones sobre contactos
│       ├── EmergencyRecordDao.kt      (58 lineas)  - DAO: operaciones sobre registros
│       └── EmergencyHistoryDao.kt     (18 lineas)  - DAO: consultas de historial
│
├── utils/                             (Utilidades)
│   ├── LocationUtils.kt              (139 lineas) - GPS: permisos, updates, ubicacion
│   ├── NotificationUtils.kt          (276 lineas) - Notificaciones, SMS, vibracion
│   ├── AudioRecorder.kt              (160 lineas) - Grabacion de audio (MediaRecorder)
│   └── WhatsappUtils.kt              (234 lineas) - Compartir por WhatsApp
│
└── service/                           (Servicios en Background)
    ├── LocationTrackingService.kt     (156 lineas) - Foreground service: monitoreo GPS
    └── VolumeEmergencyService.kt      (169 lineas) - Foreground service: volumen x3
```

**Total: ~7,200 lineas de codigo Kotlin**

> **Nota importante:** `MainActivity.kt` esta fisicamente en la carpeta `ui/` pero su package es `com.example.safezoneai` (no `com.example.safezoneai.ui`). Esto es intencional.

---

## 4. Arquitectura MVVM

La aplicacion sigue el patron **Model-View-ViewModel (MVVM)**, que separa la logica de negocio de la interfaz de usuario.

### Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                      VIEW (UI Layer)                    │
│  HomeScreen · EmergencyScreen · MapScreen · ContactScreen│
│  SettingsScreen · HistoryScreen · SplashScreen          │
│                                                         │
│  Observa estados con collectAsState()                   │
│  Solo presentacion, sin logica de negocio               │
└──────────────────────┬──────────────────────────────────┘
                       │ StateFlow / collectAsState()
                       ▼
┌─────────────────────────────────────────────────────────┐
│                   VIEWMODEL (Logic Layer)                │
│  EmergencyViewModel · SettingsViewModel · HistoryViewModel│
│                                                         │
│  Gestiona estado reactivo con MutableStateFlow          │
│  Coordina operaciones de emergencia, GPS, SMS, audio    │
│  Expone datos como StateFlow inmutables                 │
└──────────────────────┬──────────────────────────────────┘
                       │ Room DAOs / Utils
                       ▼
┌─────────────────────────────────────────────────────────┐
│                    MODEL (Data Layer)                    │
│  Room Entities · DAOs · SmartZoneDetector · ZoneRepository│
│  LocationUtils · NotificationUtils · AudioRecorder      │
│                                                         │
│  Persistencia local (Room/SQLite)                       │
│  Acceso a GPS, SMS, grabacion de audio                  │
└─────────────────────────────────────────────────────────┘
```

### Comunicacion entre capas

La comunicacion View ↔ ViewModel se realiza de forma **reactiva**:

```kotlin
// VIEWMODEL: Expone estado reactivo
private val _isEmergencyActive = MutableStateFlow(false)
val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()

// VIEW: Observa y se recompone automaticamente
val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
```

Cuando el ViewModel cambia un valor en `MutableStateFlow`, la View se **recompone automaticamente** mostrando el nuevo estado sin necesidad de callbacks manuales.

---

## 5. Patrones de Diseno

### 5.1 Patron MVVM (Model-View-ViewModel)

**Proposito:** Separar la logica de presentacion de la logica de negocio.

| Capa | Ubicacion | Responsabilidad |
|---|---|---|
| **View** | `ui/*.kt` | Solo presentacion, sin logica |
| **ViewModel** | `viewmodel/*.kt` | Estado reactivo y logica de negocio |
| **Model** | `data/`, `utils/` | Entidades, DAOs, utilidades |

**Ejemplo concreto:**
- `HomeScreen.kt` (View) observa `isEmergencyActive` del `EmergencyViewModel`
- `EmergencyViewModel` coordina la activacion de emergencia
- `EmergencyContact` (Model) almacena datos en Room

---

### 5.2 Patron Observer

**Proposito:** Notificar automaticamente a los observadores cuando el estado cambia.

**Implementacion:** `StateFlow` y `MutableStateFlow` en los ViewModels.

```kotlin
// EmergencyViewModel.kt - El sujeto observable
private val _currentLocation = MutableStateFlow<Location?>(null)
val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

private val _detectedCity = MutableStateFlow("")
val detectedCity: StateFlow<String> = _detectedCity.asStateFlow()

private val _isLoadingZones = MutableStateFlow(false)
val isLoadingZones: StateFlow<Boolean> = _isLoadingZones.asStateFlow()
```

```kotlin
// HomeScreen.kt - El observador
val currentLocation by viewModel.currentLocation.collectAsState()
val detectedCity by viewModel.detectedCity.collectAsState()
val isLoadingZones by viewModel.isLoadingZones.collectAsState()
// La pantalla se recompone automaticamente cuando cualquier valor cambia
```

Las pantallas no necesitan "pedir" datos; se **suscriben** y reciben actualizaciones automaticamente.

---

### 5.3 Patron Repository

**Proposito:** Abstraer el origen de datos del consumidor.

**Implementacion:**
- `ZoneRepository` como interfaz para acceso a datos de zonas peligrosas
- Los DAOs de Room actuan como repositorios para datos locales

```kotlin
// EmergencyDao.kt - Repositorio de contactos
@Dao
interface EmergencyDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}
```

El ViewModel no sabe si los datos vienen de SQLite, una API o memoria. Solo interactua con la interfaz del DAO.

---

### 5.4 Patron Singleton

**Proposito:** Garantizar una unica instancia de un recurso compartido.

**Implementacion:** `AppDatabase` usa double-checked locking para una instancia unica.

```kotlin
// AppDatabase.kt
companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "safezone_database"
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}
```

Tambien se aplica en los servicios con `companion object` para estado compartido:

```kotlin
// LocationTrackingService.kt
companion object {
    val isRunning = MutableStateFlow(false)
    val currentLocation = MutableStateFlow<Location?>(null)
}
```

---

### 5.5 Patron Strategy

**Proposito:** Seleccionar un algoritmo en tiempo de ejecucion segun el contexto.

**Implementacion:** `SmartZoneDetector` selecciona diferentes conjuntos de zonas peligrosas segun la ciudad detectada.

```kotlin
// SmartZoneDetector.kt
private suspend fun fetchDangerousZones(cityInfo: CityInfo): List<DangerousZone> {
    return when {
        cityInfo.name.contains("Cuenca", ignoreCase = true) -> getCuencaZones()
        cityInfo.name.contains("Quito", ignoreCase = true) -> getQuitoZones()
        cityInfo.name.contains("Guayaquil", ignoreCase = true) -> getGuayaquilZones()
        cityInfo.name.contains("Portoviejo", ignoreCase = true) -> getPortoviejoZones()
        cityInfo.name.contains("Ciudad de Mexico", ignoreCase = true) -> getCDMXZones()
        cityInfo.name.contains("Bogota", ignoreCase = true) -> getBogotaZones()
        else -> getGenericZones(cityInfo)  // Estrategia por defecto
    }
}
```

Cada ciudad tiene su propia "estrategia" de datos sin modificar la logica principal.

---

### 5.6 Patron State

**Proposito:** Cambiar el comportamiento de la aplicacion segun su estado actual.

**Implementacion:** Navegacion basada en estado String en `MainActivity`.

```kotlin
// MainActivity.kt
var currentScreen by remember { mutableStateOf("splash") }

when (currentScreen) {
    "splash" -> AnimatedSplashScreen(onFinish = { currentScreen = "home" })
    "home" -> HomeScreen(
        onNavigateToMap = { currentScreen = "map" },
        onNavigateToEmergency = { currentScreen = "emergency" },
        // ...
    )
    "emergency" -> EmergencyScreen(onBack = { currentScreen = "home" })
    "map" -> MapScreen(onBack = { currentScreen = "home" })
    "contacts" -> ContactsScreen(onBack = { currentScreen = "home" })
    "settings" -> SettingsScreen(onBack = { currentScreen = "home" })
    "history" -> HistoryScreen(onBack = { currentScreen = "home" })
}
```

El comportamiento completo de la app cambia segun el valor de `currentScreen`.

---

### 5.7 Patron Callback

**Proposito:** Ejecutar codigo cuando un evento externo ocurre.

**Implementacion:**
- `ActivityResultContracts` para resultados de permisos y seleccion de contactos
- Lambdas para navegacion entre pantallas

```kotlin
// MainActivity.kt - Callback de permisos
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions ->
    // Este bloque se ejecuta cuando el usuario responde a la solicitud de permisos
    val allGranted = permissions.values.all { it }
    if (allGranted) startServices()
}

// ContactScreen.kt - Callback de seleccion de contacto
val contactPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        // Extraer nombre y telefono del contacto seleccionado
    }
}
```

---

### 5.8 Patron Front Controller

**Proposito:** Un unico punto de entrada que coordina todas las solicitudes.

**Implementacion:** `MainActivity` actua como controlador central de la aplicacion.

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    // Punto de entrada unico que gestiona:
    // - Solicitud y manejo de permisos
    // - Navegacion entre todas las pantallas
    // - Ciclo de vida de la aplicacion
    // - Deteccion de pulsaciones de volumen (onKeyDown)
    // - Recepcion de intents de emergencia desde servicios
}
```

---

### 5.9 Patron Factory Method

**Proposito:** Centralizar la creacion de objetos sin exponer la logica de instanciacion.

**Implementacion:** `getDangerLevelColor()` en `Theme.kt` actua como factory de colores.

```kotlin
// Theme.kt
fun getDangerLevelColor(dangerLevel: String): Color {
    return when (dangerLevel.uppercase()) {
        "CRITICAL" -> DangerRed
        "HIGH" -> WarningOrange
        "MEDIUM" -> WarningYellow
        "LOW" -> SafeGreen
        "SAFE" -> SafeGreenLight
        else -> TextSecondary
    }
}
```

---

## 6. Principios SOLID

### S - Single Responsibility Principle (Principio de Responsabilidad Unica)

Cada clase tiene una **unica razon para cambiar**.

| Clase | Responsabilidad unica |
|---|---|
| `MainActivity` | Permisos, ciclo de vida y navegacion |
| `EmergencyViewModel` | Logica de emergencia y estado reactivo |
| `SettingsViewModel` | Gestion de configuracion |
| `HistoryViewModel` | Consulta de historial |
| `LocationUtils` | Operaciones de GPS exclusivamente |
| `NotificationUtils` | Notificaciones, SMS y vibracion |
| `AudioRecorder` | Grabacion de audio exclusivamente |
| `WhatsappUtils` | Integracion con WhatsApp exclusivamente |
| `SmartZoneDetector` | Deteccion de ciudad y zonas peligrosas |
| Cada Screen (`HomeScreen`, etc.) | Solo presentacion de su funcionalidad |

**Ejemplo:**
```kotlin
// LocationUtils.kt - SOLO operaciones de GPS
class LocationUtils(private val context: Context) {
    fun hasLocationPermission(): Boolean { /* ... */ }
    fun isGpsEnabled(): Boolean { /* ... */ }
    fun getLocationUpdates(interval: Long): Flow<Location> { /* ... */ }
    suspend fun getCurrentLocation(): Location? { /* ... */ }
    // No maneja SMS, no graba audio, no muestra notificaciones
}
```

---

### O - Open/Closed Principle (Principio Abierto/Cerrado)

Las clases estan **abiertas para extension** pero **cerradas para modificacion**.

**Ejemplo 1: SmartZoneDetector**
Para agregar una nueva ciudad (ej: Lima), solo se agrega un nuevo caso al `when` y un nuevo metodo `getLimaZones()`, sin modificar la logica de las ciudades existentes:

```kotlin
// Agregar nueva ciudad sin tocar las existentes:
cityInfo.name.contains("Lima", ignoreCase = true) -> getLimaZones()
```

**Ejemplo 2: Navegacion**
Agregar una nueva pantalla solo requiere un nuevo caso en el `when(currentScreen)`:

```kotlin
// Se agrega sin modificar las pantallas existentes:
"newScreen" -> NewScreen(onBack = { currentScreen = "home" })
```

**Ejemplo 3: Tema de colores**
`Color.kt` se puede extender con nuevos colores sin modificar los existentes.

---

### L - Liskov Substitution Principle (Principio de Sustitucion de Liskov)

Los subtipos pueden sustituir a sus tipos base sin alterar el comportamiento.

**Ejemplo 1:**
```kotlin
// EmergencyViewModel extiende AndroidViewModel
class EmergencyViewModel(application: Application) : AndroidViewModel(application)
// Puede usarse en cualquier lugar donde se espere un ViewModel
```

**Ejemplo 2:**
Todas las entidades de Room son `data class` que pueden intercambiarse en listas y flujos:
```kotlin
// EmergencyContact puede usarse en cualquier List<EmergencyContact>
// EmergencyRecord puede usarse en cualquier Flow<List<EmergencyRecord>>
```

---

### I - Interface Segregation Principle (Principio de Segregacion de Interfaces)

Los clientes no dependen de interfaces que no utilizan.

**Ejemplo 1: DAOs separados por responsabilidad**
```kotlin
// Solo operaciones de contactos
interface EmergencyDao {
    fun getAllContacts(): Flow<List<EmergencyContact>>
    suspend fun insertContact(contact: EmergencyContact)
    suspend fun deleteContact(contact: EmergencyContact)
}

// Solo operaciones de registros
interface EmergencyRecordDao {
    fun getAllRecords(): Flow<List<EmergencyRecord>>
    suspend fun insertRecord(record: EmergencyRecord)
    suspend fun deleteRecord(record: EmergencyRecord)
}
```

**Ejemplo 2: Cada Screen recibe solo los callbacks que necesita**
```kotlin
// HomeScreen solo recibe callbacks de navegacion relevantes
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: EmergencyViewModel
)

// EmergencyScreen solo recibe lo minimo
fun EmergencyScreen(
    viewModel: EmergencyViewModel,
    onBack: () -> Unit
)
```

---

### D - Dependency Inversion Principle (Principio de Inversion de Dependencias)

Los modulos de alto nivel no dependen de modulos de bajo nivel; ambos dependen de **abstracciones**.

**Ejemplo 1: ViewModels dependen de abstracciones**
```kotlin
// EmergencyViewModel depende de StateFlow (interfaz), no de MutableStateFlow (implementacion)
val isEmergencyActive: StateFlow<Boolean>  // La View solo ve la interfaz inmutable
val currentLocation: StateFlow<Location?>
val detectedCity: StateFlow<String>
```

**Ejemplo 2: DAOs como abstraccion**
```kotlin
// El DAO es una interfaz que abstrae SQLite
@Dao
interface EmergencyDao { /* ... */ }
// El ViewModel no sabe que detras hay SQLite
```

**Ejemplo 3: LocationUtils abstrae el proveedor de ubicacion**
```kotlin
// LocationUtils.kt abstrae FusedLocationProviderClient
// El ViewModel llama a getLocationUpdates() sin saber los detalles de implementacion GPS
```

---

## 7. Requerimientos Funcionales

### RF01 - Activacion de Emergencia por Boton
| Aspecto | Detalle |
|---|---|
| **Descripcion** | El usuario activa emergencia presionando el boton rojo en HomeScreen |
| **Acciones** | Vibra, envia SMS, abre WhatsApp, graba audio, guarda en historial |
| **Ubicacion en codigo** | `HomeScreen.kt` → `EmergencyViewModel.activateEmergency()` |

### RF02 - Emergencia por Boton de Volumen
| Aspecto | Detalle |
|---|---|
| **Descripcion** | 3 pulsaciones rapidas del boton de volumen (subir o bajar) en 2 segundos |
| **Con app abierta** | `onKeyDown()` en `MainActivity.kt` |
| **Con app en background** | `VolumeEmergencyService` con `ContentObserver` |
| **Resultado** | Trae la app al frente y navega a `EmergencyScreen` |

### RF03 - Deteccion de Zonas Peligrosas
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Detecta la ciudad del usuario via Geocoder y muestra zonas peligrosas |
| **Niveles** | SAFE, LOW, MEDIUM, HIGH, CRITICAL |
| **Alertas** | Notificacion y vibracion al entrar en zona peligrosa |
| **Ubicacion en codigo** | `SmartZoneDetector.detectAndLoadDangerousZones()` |

### RF04 - Mapa de Zonas
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Visualizacion en mapa OpenStreetMap con circulos de colores |
| **Vistas** | Mapa con overlays y lista alternativa con distancias |
| **Ubicacion en codigo** | `MapScreen.kt` con osmdroid |

### RF05 - Gestion de Contactos de Emergencia
| Aspecto | Detalle |
|---|---|
| **Descripcion** | CRUD completo de contactos |
| **Funciones** | Agregar, editar, eliminar, importar de agenda, marcar como principal |
| **Ubicacion en codigo** | `ContactScreen.kt` + `EmergencyDao` |

### RF06 - Envio de SMS de Emergencia
| Aspecto | Detalle |
|---|---|
| **Descripcion** | SMS automatico a todos los contactos registrados |
| **Contenido** | Enlace de Google Maps con ubicacion actual |
| **Fallback** | Mensaje alternativo si no hay ubicacion disponible |
| **Ubicacion en codigo** | `NotificationUtils.sendEmergencySMS()` |

### RF07 - Grabacion de Audio
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Graba audio automaticamente durante la emergencia |
| **Formato** | .m4a (AAC, 128kbps, 44.1kHz) |
| **Duracion** | Configurable en Settings (default 60 segundos) |
| **Ubicacion en codigo** | `AudioRecorder.kt` |

### RF08 - Compartir por WhatsApp
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Envia ubicacion como texto y audio como archivo adjunto |
| **Requisito** | WhatsApp instalado en el dispositivo |
| **Ubicacion en codigo** | `WhatsappUtils.kt` |

### RF09 - Historial de Emergencias
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Registro de cada emergencia con fecha, ubicacion, zona, audio, contactos |
| **Funciones** | Ver listado, eliminar registros, abrir ubicacion en Google Maps |
| **Ubicacion en codigo** | `HistoryScreen.kt` + `EmergencyRecordDao` |

### RF10 - Configuracion
| Aspecto | Detalle |
|---|---|
| **Opciones** | Duracion de grabacion, sonido, vibracion, grabacion automatica, intervalo GPS, monitoreo background |
| **Persistencia** | SharedPreferences con StateFlow reactivo |
| **Ubicacion en codigo** | `SettingsScreen.kt` + `SettingsViewModel.kt` |

### RF11 - Monitoreo en Background
| Aspecto | Detalle |
|---|---|
| **Descripcion** | Foreground service que monitorea ubicacion continuamente |
| **Deteccion** | Alerta al entrar en zonas peligrosas |
| **Notificacion** | Persistente con estado actual de la ubicacion |
| **Ubicacion en codigo** | `LocationTrackingService.kt` |

---

## 8. Requerimientos No Funcionales

### RNF01 - Rendimiento
- SMS se envia en menos de 1 segundo tras activar emergencia
- Actualizaciones GPS cada 5 segundos (foreground) / 10 segundos (background)
- UI responsiva gracias a recomposicion parcial de Compose

### RNF02 - Disponibilidad
- Deteccion por volumen funciona en background via foreground service
- `START_STICKY` en servicios para reinicio automatico si el sistema los mata
- Funciona sin internet (SMS y GPS) excepto mapas y Geocoder

### RNF03 - Seguridad
- No se almacenan datos sensibles en texto plano
- Permisos solicitados solo los estrictamente necesarios
- SMS enviado via `SmsManager` nativo (sin APIs externas)

### RNF04 - Usabilidad
- Boton de emergencia grande (200dp) y accesible
- Activacion por volumen x3 para situaciones donde no se puede usar la pantalla
- Importacion de contactos desde agenda para evitar errores de tipeo
- Interfaz en espanol con mensajes claros

### RNF05 - Compatibilidad
- Android 9+ (API 28) hasta Android 14+ (API 34)
- Funciona sin Google Maps API key (usa OpenStreetMap gratuito)
- Compatible con dispositivos sin WhatsApp (SMS como respaldo)

### RNF06 - Mantenibilidad
- Arquitectura MVVM con separacion clara de capas
- Principios SOLID aplicados en todo el proyecto
- Codigo organizado por funcionalidad en paquetes

---

## 9. Flujos Principales de la Aplicacion

### 9.1 Flujo de Emergencia (Boton en Pantalla)

```
Usuario presiona el boton rojo en HomeScreen
    │
    ▼
EmergencyViewModel.activateEmergency()
    │
    ├── 1. _isEmergencyActive = true
    ├── 2. VIBRAR inmediatamente (patron haptico de emergencia)
    ├── 3. ENVIAR SMS a todos los contactos con ubicacion actual
    │       └── Incluye link de Google Maps: https://maps.google.com/?q=lat,lng
    │
    ├── 4. getCurrentLocation() → obtener ubicacion fresca del GPS
    ├── 5. Mostrar NOTIFICACION con ubicacion
    ├── 6. Abrir WHATSAPP con mensaje + link de Maps
    │
    ├── 7. Iniciar GRABACION de audio (MediaRecorder)
    ├── 8. Esperar duracion configurada (default 60 segundos)
    ├── 9. Detener grabacion
    ├── 10. Abrir WHATSAPP con archivo de audio
    │
    └── 11. GUARDAR en Room DB (EmergencyRecord)
            └── timestamp, lat, lng, audioPath, contactsNotified, dangerZone
```

### 9.2 Flujo de Emergencia por Volumen (Background)

```
Usuario presiona boton de volumen 3 veces en 2 segundos
    │
    ▼
VolumeEmergencyService → ContentObserver detecta cambio en Settings.System
    │
    ├── Registra timestamp de cada pulsacion
    ├── Verifica: ¿3 pulsaciones en < 2 segundos?
    │
    ▼ SI
triggerEmergency()
    │
    ├── 1. VIBRAR inmediatamente
    ├── 2. Crear Intent hacia MainActivity con FLAG_ACTIVITY_NEW_TASK
    ├── 3. Mostrar fullScreenIntent notification (abre la app)
    │
    ▼
MainActivity.handleEmergencyIntent()
    │
    ├── Navegar a EmergencyScreen
    └── EmergencyViewModel.triggerEmergencyFromVolume()
        └── Continua el flujo normal de emergencia (SMS, audio, etc.)
```

### 9.3 Flujo de Deteccion de Zonas Peligrosas

```
GPS emite nueva ubicacion cada 5-10 segundos
    │
    ▼
EmergencyViewModel observa _currentLocation via Flow
    │
    ├── distinctUntilChanged() → solo procesa si cambio significativo
    │
    ▼
detectCityAndLoadZones(latitude, longitude)
    │
    ├── Geocoder.getFromLocation() → detecta ciudad
    ├── SmartZoneDetector.fetchDangerousZones(cityInfo)
    │   └── Retorna zonas segun la ciudad (Cuenca, Quito, etc.)
    │
    ├── Calcular distancia del usuario a cada zona
    ├── ¿Esta dentro de alguna zona peligrosa?
    │
    ▼ SI
    ├── Mostrar NOTIFICACION de alerta
    ├── VIBRAR con patron de advertencia
    └── Actualizar UI via StateFlow (_nearestDangerZone)
```

### 9.4 Flujo de Gestion de Contactos

```
Usuario abre ContactsScreen
    │
    ├── Si no hay contactos → Mostrar EmptyContactsState
    ├── Si hay contactos → Mostrar ContactsList con ContactCards
    │
    ▼ Usuario presiona "Agregar"
ContactFormDialog se muestra
    │
    ├── Opcion 1: Escribir manualmente (nombre, telefono, relacion)
    ├── Opcion 2: "Importar de la agenda" → ContactPicker
    │   └── Extrae nombre y telefono automaticamente
    │
    ├── Seleccionar relacion: Familiar, Amigo/a, Policia, Otro
    ├── Toggle: ¿Contacto principal?
    │
    ▼ "Guardar"
    ├── Validar: nombre no vacio, telefono >= 10 digitos
    ├── cleanPhoneNumber() → eliminar caracteres invalidos
    └── EmergencyViewModel.addEmergencyContact() → Room INSERT
```

---

## 10. Capa de Datos (Room)

### Entidades

**EmergencyContact** - Contacto de emergencia
```kotlin
@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,           // Nombre del contacto
    val phoneNumber: String,    // Numero limpio (solo digitos y +)
    val relationship: String,   // Familiar, Amigo/a, Policia, Otro
    val isPrimary: Boolean = false  // Si es contacto principal
)
```

**EmergencyRecord** - Registro de emergencia
```kotlin
@Entity(tableName = "emergency_records")
data class EmergencyRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,         // Momento de activacion
    val latitude: Double,        // Coordenada GPS
    val longitude: Double,       // Coordenada GPS
    val audioFilePath: String?,  // Ruta al archivo .m4a grabado
    val contactsNotified: Int,   // Cantidad de contactos notificados
    val dangerZoneName: String?, // Nombre de la zona (si aplica)
    val dangerLevel: String?,    // Nivel: SAFE, LOW, MEDIUM, HIGH, CRITICAL
    val notes: String? = null    // Notas adicionales
)
```

### DAOs (Data Access Objects)

| DAO | Operaciones |
|---|---|
| `EmergencyDao` | CRUD de contactos, busqueda por primario, ordenamiento |
| `EmergencyRecordDao` | CRUD de registros, consulta por rango de fechas |
| `EmergencyHistoryDao` | Consultas simplificadas de historial |

### Base de datos

- **Nombre:** `safezone_database`
- **Version:** 2
- **Migracion:** `fallbackToDestructiveMigration()` (en desarrollo)
- **Patron:** Singleton con double-checked locking

---

## 11. Servicios en Background

### LocationTrackingService

| Aspecto | Detalle |
|---|---|
| **Tipo** | Foreground Service (tipo: location) |
| **Funcion** | Monitoreo GPS continuo en segundo plano |
| **Intervalo** | Cada 10 segundos |
| **Notificacion** | Persistente con estado "Monitoreando tu ubicacion" |
| **Deteccion** | Alerta al entrar en zonas peligrosas |
| **Reinicio** | `START_STICKY` - se reinicia si el sistema lo mata |
| **Estado compartido** | `companion object` con `StateFlow<Location?>` |

### VolumeEmergencyService

| Aspecto | Detalle |
|---|---|
| **Tipo** | Foreground Service (tipo: special use) |
| **Funcion** | Detectar 3 pulsaciones rapidas de volumen |
| **Mecanismo** | `ContentObserver` en `Settings.System.CONTENT_URI` |
| **Umbral** | 3 cambios de volumen en < 2 segundos |
| **Resultado** | Vibra, abre la app, activa emergencia |
| **Notificacion** | Persistente "Proteccion activa" |
| **Estado compartido** | `companion object` con `StateFlow<Boolean>` para comunicar con `MainActivity` |

---

## 12. Permisos de la Aplicacion

| Permiso | Tipo | Uso |
|---|---|---|
| `ACCESS_FINE_LOCATION` | Peligroso | GPS preciso para ubicacion |
| `ACCESS_COARSE_LOCATION` | Peligroso | Ubicacion aproximada como fallback |
| `ACCESS_BACKGROUND_LOCATION` | Peligroso | Monitoreo GPS en background |
| `RECORD_AUDIO` | Peligroso | Grabacion de evidencia |
| `SEND_SMS` | Peligroso | Envio de alertas SMS |
| `POST_NOTIFICATIONS` | Normal (API 33+) | Notificaciones en Android 13+ |
| `VIBRATE` | Normal | Feedback haptico |
| `INTERNET` | Normal | Mapas OSM y Geocoder |
| `ACCESS_NETWORK_STATE` | Normal | Verificar conectividad |
| `FOREGROUND_SERVICE` | Normal | Ejecutar servicios en foreground |
| `FOREGROUND_SERVICE_LOCATION` | Normal | Servicio de monitoreo GPS |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal | Servicio de deteccion de volumen |
| `USE_FULL_SCREEN_INTENT` | Normal | Abrir app desde emergencia por volumen |
| `WRITE_EXTERNAL_STORAGE` | Normal (maxSdk=32) | Cache de mapas en Android < 13 |
| `READ_EXTERNAL_STORAGE` | Normal (maxSdk=32) | Cache de mapas en Android < 13 |

---

## 13. Limitaciones Conocidas

### Limitaciones Funcionales

| # | Limitacion | Impacto |
|---|---|---|
| 1 | **Zonas peligrosas hardcoded** | Solo 6 ciudades con datos reales (Cuenca, Quito, Guayaquil, Portoviejo, CDMX, Bogota). Otras ciudades reciben zonas genericas. |
| 2 | **Geocoder requiere internet** | La deteccion de ciudad falla sin conexion. El GPS funciona offline pero no la identificacion de la ciudad. |
| 3 | **Navegacion basada en String** | No usa Navigation Compose. Puede causar problemas de escalabilidad si crece la app. |
| 4 | **WhatsApp obligatorio para audio** | Sin WhatsApp instalado, el audio solo se guarda localmente sin compartirse. |
| 5 | **Sin autenticacion** | No hay login. Cualquiera con acceso al dispositivo puede ver/modificar contactos e historial. |

### Limitaciones Tecnicas

| # | Limitacion | Impacto |
|---|---|---|
| 6 | **SMS requiere saldo** | Depende del plan/saldo del operador movil del usuario. |
| 7 | **SmsManager en Android 12+** | Puede ser null en dispositivos sin telefonia. Se maneja con null-check. |
| 8 | **Servicio depende del sistema** | Si el sistema mata el servicio (bateria baja), la deteccion por volumen se pierde hasta reabrir la app. |
| 9 | **ViewModel central sobredimensionado** | `EmergencyViewModel` (615 lineas) maneja demasiadas responsabilidades. |
| 10 | **Sin tests automatizados** | No hay tests unitarios ni de integracion. |

### Limitaciones del Entorno

| # | Limitacion | Impacto |
|---|---|---|
| 11 | **JDK 17 requerido** | El proyecto requiere exactamente Java 17 para compilar. |

---

## 14. Mejoras a Futuro

### 14.1 Arquitectura

#### Dividir EmergencyViewModel
El `EmergencyViewModel` actual (615 lineas) viola parcialmente SRP al manejar emergencias, GPS, zonas, SMS y audio. Deberia dividirse en:

| ViewModel propuesto | Responsabilidad |
|---|---|
| `EmergencyViewModel` | Solo coordinacion de emergencia |
| `LocationViewModel` | Operaciones de GPS y deteccion de zonas |
| `AudioViewModel` | Grabacion y gestion de audio |
| `ContactViewModel` | CRUD de contactos |

#### Adoptar Navigation Compose
Reemplazar la navegacion basada en String por Jetpack Navigation Compose para:
- Navegacion type-safe con argumentos tipados
- Mejor manejo del back stack
- Deep linking
- Animaciones de transicion

#### Inyeccion de Dependencias (Hilt/Koin)
Reemplazar la instanciacion manual:
```kotlin
// Actual (manual)
private val locationUtils = LocationUtils(context)
private val audioRecorder = AudioRecorder(context)

// Propuesto (con Hilt)
@Inject lateinit var locationUtils: LocationUtils
@Inject lateinit var audioRecorder: AudioRecorder
```

#### Capa de Use Cases
Agregar casos de uso entre ViewModel y Repository:
- `ActivateEmergencyUseCase`
- `SendEmergencySMSUseCase`
- `DetectDangerZoneUseCase`
- `RecordAudioUseCase`

### 14.2 Datos

#### Zonas peligrosas dinamicas
- Conectar a una API remota para obtener datos actualizados
- Almacenar en cache local con Room
- Permitir a los usuarios reportar zonas peligrosas
- Implementar estrategia de sincronizacion offline-first

#### Manejo de errores robusto
```kotlin
// Propuesto: Sealed class para resultados
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val message: String, val exception: Exception?) : Result<T>()
    class Loading<T> : Result<T>()
}
```

### 14.3 Testing

#### Agregar tests unitarios
- Tests de ViewModels con `MockK` o `Mockito`
- Tests de DAOs con Room in-memory database
- Tests de utilidades (`cleanPhoneNumber`, `LocationUtils`, etc.)

#### Agregar tests de UI
- Tests de Compose con `ComposeTestRule`
- Tests de navegacion
- Tests de integracion end-to-end

### 14.4 Funcionalidades

| Mejora | Descripcion |
|---|---|
| **Autenticacion** | Login con biometria o PIN para proteger datos sensibles |
| **Modo silencioso** | Activar emergencia sin sonido ni vibracion (situaciones de peligro con agresor cerca) |
| **Ubicacion en tiempo real** | Compartir ubicacion en vivo con contactos durante emergencia |
| **Soporte multiidioma** | Internacionalizacion (i18n) para ingles y otros idiomas |
| **Widget de escritorio** | Boton de emergencia accesible desde la pantalla de inicio |
| **Wear OS** | Companion app para smartwatches con activacion por gestos |
| **Modularizacion** | Separar en modulos Gradle (`:core:data`, `:feature:emergency`, `:feature:map`, etc.) |
| **Accesibilidad** | Content descriptions para TalkBack, soporte completo de a11y |
| **Analytics** | Metricas de uso para mejorar la experiencia |

### 14.5 Seguridad

| Mejora | Descripcion |
|---|---|
| **Encriptacion de BD** | Usar SQLCipher para encriptar Room database |
| **Ofuscacion de codigo** | Configurar ProGuard/R8 para release builds |
| **Certificate pinning** | Si se agrega API remota, implementar pinning SSL |
| **Borrado remoto** | Capacidad de borrar datos del dispositivo remotamente |

---

## 15. Estadisticas del Proyecto

| Metrica | Valor |
|---|---|
| **Total lineas de codigo** | ~7,200 lineas Kotlin |
| **Pantallas** | 7 (Home, Emergency, Map, Contacts, Settings, History, Splash) |
| **ViewModels** | 3 (Emergency, Settings, History) |
| **Servicios** | 2 (LocationTracking, VolumeEmergency) |
| **Entidades Room** | 2 (EmergencyContact, EmergencyRecord) |
| **DAOs** | 3 (EmergencyDao, EmergencyRecordDao, EmergencyHistoryDao) |
| **Utilidades** | 4 (Location, Notification, Audio, WhatsApp) |
| **Patrones de diseno** | 9 identificados |
| **Principios SOLID** | 5/5 aplicados |
| **Dependencias** | 20+ librerias |
| **Permisos** | 15 requeridos |
| **Min SDK** | 28 (Android 9) |
| **Target SDK** | 34 (Android 14) |
| **Compile SDK** | 35 |

---

*Documentacion generada para el proyecto SafeZone AI - Febrero 2026*
