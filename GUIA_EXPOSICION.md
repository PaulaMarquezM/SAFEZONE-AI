# GUIA DE EXPOSICION - SafeZone AI

## Como Iniciar la Exposicion (30 segundos)

> "SafeZone AI es una app Android de seguridad personal. Detecta zonas peligrosas por GPS, y si el usuario esta en peligro, puede presionar un boton de emergencia o presionar 3 veces el boton de volumen - incluso con la app cerrada - para enviar automaticamente un SMS con su ubicacion, grabar audio como evidencia y compartir todo por WhatsApp."

---

## 1. ARQUITECTURA MVVM - "Como dividieron los modulos?"

### Que decir:

> "Usamos MVVM que significa Model-View-ViewModel. Dividimos la app en 3 capas claras:"

**View (lo que ve el usuario)** - Paquete `ui/`
- 7 pantallas hechas con Jetpack Compose: Home, Emergency, Map, Contacts, Settings, History, Splash
- Las pantallas NO tienen logica de negocio, solo muestran datos
- Ejemplo: `HomeScreen.kt` solo muestra el boton rojo y el estado de la zona, no decide si es peligrosa

**ViewModel (la logica)** - Paquete `viewmodel/`
- `EmergencyViewModel` (615 lineas) - Controla emergencia, GPS, zonas, SMS, audio
- `SettingsViewModel` (97 lineas) - Controla configuracion con SharedPreferences
- `HistoryViewModel` - Consulta historial de emergencias desde Room

**Model (los datos)** - Paquete `data/`
- `EmergencyContact` y `EmergencyRecord` son las tablas en Room (SQLite)
- `SmartZoneDetector` tiene los datos de zonas peligrosas de 6 ciudades
- Los DAOs (`EmergencyDao`, `EmergencyRecordDao`) hacen las consultas SQL

### Como se comunican las capas:

```
View (HomeScreen) --observa--> ViewModel (EmergencyViewModel) --usa--> Model (Room DB + SmartZoneDetector)
```

La comunicacion es con `StateFlow`. El ViewModel tiene un estado privado mutable:
```kotlin
// En EmergencyViewModel.kt linea 62-63
private val _isEmergencyActive = MutableStateFlow(false)   // privado, solo el ViewModel lo cambia
val isEmergencyActive: StateFlow<Boolean> = _isEmergencyActive.asStateFlow()  // publico, las pantallas lo leen
```

Y la pantalla lo observa con `collectAsState()`:
```kotlin
// En EmergencyScreen.kt linea 34
val isEmergencyActive by viewModel.isEmergencyActive.collectAsState()
```

> "Cuando el ViewModel cambia `_isEmergencyActive` a `true`, la pantalla se recompone automaticamente. No necesitamos decirle a la pantalla que se actualice - Compose lo hace solo gracias al patron Observer."

### Si preguntan "Por que no usaron Navigation Compose?"

> "Usamos navegacion basada en un String state (`currentScreen`) en MainActivity. Lo reconocemos como una limitacion - Navigation Compose daria type-safety y mejor manejo del back stack. Es una mejora planeada a futuro."

---

## 2. PRINCIPIOS SOLID - "Como aplicaron SOLID?"

### S - Single Responsibility (Responsabilidad Unica)

> "Cada clase hace UNA sola cosa:"

| Clase | Responsabilidad unica |
|-------|----------------------|
| `MainActivity.kt` | Solo permisos, navegacion y ciclo de vida |
| `EmergencyViewModel.kt` | Solo logica de emergencia y estado |
| `LocationUtils.kt` | Solo operaciones de GPS |
| `NotificationUtils.kt` | Solo notificaciones, SMS y vibracion |
| `AudioRecorder.kt` | Solo grabacion de audio |
| `SmartZoneDetector.kt` | Solo deteccion de ciudad y zonas peligrosas |
| `WhatsappUtils.kt` | Solo compartir por WhatsApp |

**Ejemplo concreto:** Cuando se activa una emergencia, `EmergencyViewModel` NO envia el SMS directamente. Le dice a `NotificationUtils` que lo haga:

```kotlin
// EmergencyViewModel.kt linea 526-532
notificationUtils.sendEmergencySMS(
    contacts = contacts,
    latitude = latitude,
    longitude = longitude,
    audioFilePath = audioFilePath,
    hasLocation = hasLocation
)
```

**Si preguntan la debilidad:** Reconocer que `EmergencyViewModel` tiene 615 lineas y maneja emergencia + GPS + zonas + SMS + audio. Viola parcialmente SRP. La mejora seria dividirlo en `EmergencyViewModel`, `LocationViewModel`, `AudioViewModel` y `ContactViewModel`.

### O - Open/Closed (Abierto a extension, cerrado a modificacion)

> "SmartZoneDetector esta abierto a extension. Si queremos agregar una nueva ciudad, solo agregamos un nuevo bloque de datos sin tocar la logica existente:"

```kotlin
// SmartZoneDetector.kt - fetchDangerousZones()
// Para agregar Manta, solo anadimos:
"manta" -> listOf(
    DangerousZone("Puerto", -0.95, -80.73, 300f, DangerLevel.MEDIUM, "Zona portuaria")
)
// No modificamos nada de Cuenca, Quito, etc.
```

Tambien para agregar una nueva pantalla, solo se agrega un nuevo `when` case sin tocar las pantallas existentes.

### L - Liskov Substitution (Sustitucion de Liskov)

> "Nuestros ViewModels extienden `AndroidViewModel`, que extiende `ViewModel`. Cualquier lugar donde se use un ViewModel generico puede recibir nuestro EmergencyViewModel sin problemas."

```kotlin
// EmergencyViewModel.kt linea 44
class EmergencyViewModel(application: Application) : AndroidViewModel(application)
```

Todas las entities de Room son `data class`, que son intercambiables en listas y flujos.

### I - Interface Segregation (Segregacion de Interfaces)

> "Separamos los DAOs por responsabilidad en vez de tener un DAO gigante:"

- `EmergencyDao` - Solo CRUD de contactos (insertContact, updateContact, deleteContact, getAllContacts)
- `EmergencyRecordDao` - Solo CRUD de registros de emergencia
- `EmergencyHistoryDao` - Solo consultas de historial

```kotlin
// EmergencyDao.kt - Solo operaciones de contactos
@Dao
interface EmergencyDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}
```

> "Cada Screen recibe solo los callbacks que necesita. Por ejemplo, ContactScreen recibe funciones para agregar/editar/eliminar contactos, pero no recibe funciones de emergencia o mapas."

### D - Dependency Inversion (Inversion de Dependencias)

> "Los ViewModels dependen de abstracciones, no de implementaciones concretas:"

- Las pantallas dependen de `StateFlow<T>` (abstraccion), no de `MutableStateFlow<T>` (implementacion)
- Los ViewModels usan `Flow<List<EmergencyContact>>` del DAO, no acceden a SQLite directamente
- `LocationUtils` abstrae `FusedLocationProviderClient` - si cambiaramos el proveedor de GPS, solo cambiariamos esta clase

```kotlin
// La pantalla depende de StateFlow (abstraccion)
val isEmergencyActive: StateFlow<Boolean>  // ← no sabe que por dentro es MutableStateFlow

// El ViewModel depende de Flow del DAO (abstraccion)
val emergencyContacts: StateFlow<List<EmergencyContact>> = emergencyDao
    .getAllContacts()  // ← retorna Flow, no sabe que por dentro es SQL
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

## 3. PATRONES DE DISENO - "Que patrones usaron?"

### 3.1 Observer (el mas importante)

> "Es el corazon de nuestra app. Usamos `StateFlow` y `collectAsState()` para que la UI se actualice automaticamente."

**Como funciona:**
1. El ViewModel tiene un `MutableStateFlow` (el sujeto/subject)
2. La pantalla se suscribe con `collectAsState()` (el observador)
3. Cuando el estado cambia, Compose recompone la UI automaticamente

```kotlin
// SUJETO (EmergencyViewModel.kt)
private val _currentLocation = MutableStateFlow<Location?>(null)  // Estado mutable privado
val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()  // Expuesto como inmutable

// OBSERVADOR (HomeScreen.kt)
val currentLocation by viewModel.currentLocation.collectAsState()  // Se suscribe y reacciona a cambios
```

> "Cada vez que el GPS envia una nueva ubicacion, `_currentLocation.value` cambia, y TODAS las pantallas que lo observan se actualizan solas. No tenemos que llamar a ningun `updateUI()` ni `notifyDataSetChanged()` como en el desarrollo Android antiguo."

### 3.2 Singleton

> "AppDatabase usa el patron Singleton con double-checked locking para garantizar que solo exista UNA instancia de la base de datos en toda la app."

```kotlin
// AppDatabase.kt lineas 27-48
companion object {
    @Volatile  // Garantiza visibilidad entre hilos
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {  // Solo un hilo puede entrar aqui
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "safezone_database"
            ).fallbackToDestructiveMigration()
             .build()
            INSTANCE = instance
            instance
        }
    }
}
```

**Si preguntan "Por que Singleton?":** "Porque crear multiples conexiones a la base de datos es costoso y puede causar problemas de concurrencia. Con Singleton nos aseguramos de que toda la app use la misma conexion."

**Si preguntan "Que es @Volatile?":** "Garantiza que cuando un hilo escribe en INSTANCE, los demas hilos ven el cambio inmediatamente. Sin volatile, un hilo podria ver un valor viejo de INSTANCE y crear una segunda instancia."

### 3.3 Repository

> "El patron Repository abstrae el origen de los datos. El ViewModel no sabe si los datos vienen de SQLite, de una API o de un archivo."

- `ZoneRepository` como interfaz para datos de zonas
- Los DAOs de Room actuan como repositorios para contactos y registros
- `SmartZoneDetector` actua como repositorio para zonas peligrosas

```kotlin
// El ViewModel solo llama al DAO, no sabe nada de SQL
emergencyDao.insertContact(contact)  // No escribe SQL, Room lo genera
emergencyDao.getAllContacts()         // Retorna Flow<List<EmergencyContact>>
```

### 3.4 Strategy

> "SmartZoneDetector usa Strategy para cargar diferentes datos segun la ciudad detectada."

```kotlin
// SmartZoneDetector.kt - fetchDangerousZones()
// Segun la ciudad, se usa una estrategia diferente:
when (cityName.lowercase()) {
    "cuenca" -> getCuencaZones()       // 5 zonas con datos reales
    "quito" -> getQuitoZones()         // 2 zonas
    "guayaquil" -> getGuayaquilZones() // 2 zonas (Monte Sinai = CRITICAL)
    "portoviejo" -> getPortoviejoZones() // 7 zonas
    "ciudad de méxico" -> getCDMXZones() // 2 zonas (Tepito = CRITICAL)
    "bogotá" -> getBogotaZones()       // 1 zona
    else -> getGenericZones()          // Zonas genericas para ciudades no soportadas
}
```

### 3.5 State

> "La navegacion de la app usa el patron State. El comportamiento cambia segun el estado actual:"

```kotlin
// En MainActivity - la pantalla mostrada depende del estado
when (currentScreen) {
    "splash" -> SplashScreen(...)
    "home" -> HomeScreen(...)
    "emergency" -> EmergencyScreen(...)
    "map" -> MapScreen(...)
    "contacts" -> ContactScreen(...)
    "settings" -> SettingsScreen(...)
    "history" -> HistoryScreen(...)
}
```

### 3.6 Front Controller

> "MainActivity actua como Front Controller - es el punto de entrada unico que gestiona permisos, navegacion, ciclo de vida y deteccion de volumen."

### 3.7 Callback

> "Usamos lambdas como callbacks para comunicacion entre componentes:"

```kotlin
// HomeScreen recibe funciones lambda para navegar
HomeScreen(
    onNavigateToMap = { currentScreen = "map" },
    onNavigateToContacts = { currentScreen = "contacts" },
    onNavigateToSettings = { currentScreen = "settings" },
    onNavigateToHistory = { currentScreen = "history" },
    onEmergencyActivated = { currentScreen = "emergency" }
)
```

---

## 4. FLUJO DE EMERGENCIA - "Que pasa cuando se presiona el boton?"

> "Este es el flujo completo, todo pasa en `activateEmergency()` del EmergencyViewModel (linea 263):"

```
1. _isEmergencyActive = true
2. VIBRAR inmediatamente (NotificationUtils.vibrateEmergency())
3. ENVIAR SMS con ubicacion actual a TODOS los contactos
4. Obtener ubicacion GPS fresca
5. Mostrar notificacion con ubicacion
6. ABRIR WHATSAPP con mensaje + link Google Maps
7. INICIAR GRABACION de audio (MediaRecorder)
8. ESPERAR duracion configurada (default 60 segundos)
9. DETENER grabacion
10. ABRIR WHATSAPP con archivo de audio .m4a
11. GUARDAR en Room (EmergencyRecord con fecha, ubicacion, audio, contactos)
```

**Lo mas importante:** El SMS se envia INMEDIATAMENTE con la ubicacion que ya tenemos (linea 274: `val quickLocation = _currentLocation.value`). No esperamos a obtener GPS fresco. Esto garantiza que si el usuario pierde la senal, al menos el SMS ya salio.

---

## 5. BOTON DE VOLUMEN - "Como funciona la deteccion por volumen?"

> "Implementamos dos mecanismos:"

### Con la app abierta:
`MainActivity.onKeyDown()` detecta `KEYCODE_VOLUME_UP` o `KEYCODE_VOLUME_DOWN`. Cuenta 3 pulsaciones en 2 segundos.

### Con la app cerrada (background):
`VolumeEmergencyService` es un Foreground Service que usa `ContentObserver` sobre `Settings.System.CONTENT_URI`:

```kotlin
// VolumeEmergencyService.kt
private val volumeChangeTimestamps = mutableListOf<Long>()  // Guarda timestamps

// Cada vez que cambia el volumen del sistema:
val now = System.currentTimeMillis()
volumeChangeTimestamps.add(now)
volumeChangeTimestamps.removeAll { now - it > TIME_WINDOW_MS }  // Limpia los viejos (>2 seg)

if (volumeChangeTimestamps.size >= REQUIRED_PRESSES) {  // Si hay 3+ en la ventana
    // ACTIVAR EMERGENCIA
    vibrate()
    openAppWithEmergency()
    volumeChangeTimestamps.clear()
}
```

**Si preguntan "Funciona con la pantalla apagada?":** "Si, porque es un Foreground Service con START_STICKY. Android lo mantiene vivo y lo reinicia si lo mata. Solo falla si el usuario tiene ahorro de bateria agresivo."

---

## 6. ROOM DATABASE - "Como manejan los datos?"

> "Usamos Room que es la libreria oficial de Android para SQLite. Tenemos 2 tablas:"

**Tabla `emergency_contacts`:**
| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Int (PK auto) | Identificador |
| name | String | Nombre del contacto |
| phoneNumber | String | Numero limpio (solo digitos y +) |
| relationship | String | Familiar, Amigo/a, Policia, Otro |
| isPrimary | Boolean | Si es contacto principal |

**Tabla `emergency_records`:**
| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Int (PK auto) | Identificador |
| timestamp | Long | Momento de activacion |
| latitude/longitude | Double | Coordenadas GPS |
| audioFilePath | String? | Ruta al .m4a grabado |
| contactsNotified | Int | Cuantos contactos se alertaron |
| dangerZoneName | String? | Nombre de la zona |
| dangerLevel | String? | SAFE/LOW/MEDIUM/HIGH/CRITICAL |

> "Las consultas usan Flow para ser reactivas. Cuando se agrega un contacto, la lista se actualiza automaticamente en la pantalla sin que tengamos que refrescar nada."

---

## 7. SERVICIOS EN BACKGROUND

> "Tenemos 2 Foreground Services:"

**LocationTrackingService:**
- Monitorea GPS cada 10 segundos en background
- Detecta si el usuario entra a una zona peligrosa
- Muestra notificacion persistente "Monitoreando tu ubicacion"
- Usa `companion object` con `StateFlow` para compartir estado con el ViewModel

**VolumeEmergencyService:**
- Detecta 3 pulsaciones rapidas de volumen
- Usa `ContentObserver` en `Settings.System.CONTENT_URI`
- Si detecta emergencia: vibra, abre la app, activa emergencia
- Notificacion persistente "Proteccion activa"

> "Ambos servicios usan START_STICKY para que Android los reinicie automaticamente si los mata por falta de memoria."

---

## 8. PREGUNTAS DIFICILES QUE PUEDEN HACER

### "Por que no usaron una API para las zonas peligrosas?"

> "Decidimos usar datos hardcoded para garantizar que la deteccion funcione SIN INTERNET. Si usaramos una API, el usuario en una zona sin cobertura no recibiria alertas. Como mejora futura planeamos una estrategia offline-first: descargar datos de una API y cachearlos localmente en Room."

### "Por que no tienen tests automatizados?"

> "Solo tenemos los tests por defecto de Android (ExampleUnitTest y ExampleInstrumentedTest). El enfoque del proyecto fue funcionalidad completa, no cobertura de tests. Como mejora futura planeamos tests con MockK para ViewModels y tests de UI con ComposeTestRule."

### "Por que OpenStreetMap y no Google Maps?"

> "OpenStreetMap con osmdroid no requiere API key, es gratuito y open source. Google Maps requiere crear un proyecto en Google Cloud, habilitar APIs, y tiene limites de uso gratuito. Para un proyecto academico, OSM fue la opcion practica."

### "Que pasa si el sistema mata el servicio de volumen?"

> "Usamos START_STICKY que le dice a Android 'reinicia este servicio si lo matas'. Pero si el usuario tiene modo de ahorro de bateria agresivo o cierra la app forzadamente, el servicio puede perderse hasta que la app se reabra. Es una limitacion conocida de Android, no nuestra."

### "Por que un solo ViewModel central?"

> "EmergencyViewModel tiene 615 lineas y maneja emergencia, GPS, zonas, SMS y audio. Sabemos que viola parcialmente el principio de responsabilidad unica. Lo hicimos asi por simplicidad de desarrollo, pero la mejora seria dividirlo en 4 ViewModels especializados."

### "Que version de Android minima soportan y por que?"

> "API 28 (Android 9). Porque Foreground Service types se introdujeron en Android 9, y necesitamos `FOREGROUND_SERVICE_TYPE_LOCATION` y `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` para los servicios en background. Ademas, el 95%+ de dispositivos activos tienen Android 9 o superior."

### "Como limpian el numero de telefono al importar contactos?"

> "Tenemos la funcion `cleanPhoneNumber()` que elimina todos los caracteres que no sean digitos o '+'. Asi si el contacto tiene formato (099) 123-4567, se convierte a 0991234567. Tambien validamos que tenga minimo 10 digitos."

### "Que pasa si no hay WhatsApp instalado?"

> "WhatsApp es opcional. El SMS se envia siempre como canal principal usando SmsManager nativo. Si WhatsApp no esta instalado, el intent simplemente no se abre y el audio solo se guarda localmente."

### "Como manejan la concurrencia?"

> "Usamos Kotlin Coroutines con viewModelScope. Todas las operaciones de base de datos y GPS son suspend functions que corren en hilos de background. StateFlow garantiza thread-safety para los estados compartidos. AppDatabase usa @Volatile y synchronized para evitar race conditions."

### "Que es Jetpack Compose y por que lo eligieron?"

> "Es el framework moderno de UI de Android, recomendado por Google. Es declarativo: describimos COMO queremos que se vea la pantalla, y Compose se encarga de actualizarla cuando cambian los datos. Es mas limpio que el sistema antiguo de XML + Activities/Fragments."

---

## 9. ESTADISTICAS PARA IMPRESIONAR

- **7,213 lineas** de codigo Kotlin
- **7 pantallas** con Material Design 3
- **3 ViewModels** con estados reactivos
- **2 Foreground Services** para background
- **2 tablas** en Room Database con **3 DAOs**
- **4 utilidades** especializadas
- **8 patrones** de diseno implementados
- **5/5 principios** SOLID aplicados
- **15 permisos** de Android
- **6 ciudades** con **19 zonas** peligrosas reales
- **25+ librerias** de dependencia
- Compatible con **Android 9 hasta Android 14+**

---

## 10. DEMO EN VIVO (si aplica)

### Que mostrar en orden:
1. **Splash Screen** - Se ve animado al abrir
2. **Home** - Mostrar que detecta la ciudad y la zona
3. **Contactos** - Agregar un contacto de prueba
4. **Mapa** - Mostrar las zonas con colores
5. **Boton de emergencia** - Activar y mostrar EmergencyScreen con el timer
6. **Historial** - Mostrar que se registro la emergencia
7. **Settings** - Mostrar configuracion de duracion de audio

### Que NO hacer en demo:
- No enviar SMS real (cuesta dinero)
- No activar volumen x3 sin explicar antes que va a vibrar fuerte
- No quedarse mucho tiempo en el mapa (puede tardar en cargar)
