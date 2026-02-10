# SafeZone AI - Documentacion del Proyecto

## Descripcion General
Aplicacion Android de seguridad personal que detecta zonas peligrosas por GPS, permite activar emergencias con un boton o 3 pulsaciones rapidas del boton de volumen (incluso en background), envia SMS automatico con ubicacion, graba audio como evidencia, y comparte todo por WhatsApp.

---

## Stack Tecnico
- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Arquitectura**: MVVM (Model-View-ViewModel)
- **Base de datos**: Room (SQLite)
- **Ubicacion**: Google Play Services - FusedLocationProviderClient
- **Mapas**: OpenStreetMap (osmdroid 6.1.18) - sin API key, gratuito
- **Compilacion**: Gradle Kotlin DSL, compileSdk 35, JDK 17
- **Min SDK**: 28 (Android 9) | **Target SDK**: 34 (Android 14)

### Dependencias Principales
| Dependencia | Version | Uso |
|-------------|---------|-----|
| Jetpack Compose BOM | 2024.10.00 | Framework de UI declarativa |
| Room | 2.6.1 | Persistencia local (contactos, historial) |
| Play Services Location | 21.1.0 | GPS y ubicacion |
| osmdroid | 6.1.18 | Mapas OpenStreetMap |
| Coroutines | 1.7.3 | Programacion asincrona |
| Accompanist Permissions | 0.32.0 | Manejo de permisos en Compose |
| SplashScreen | core-splashscreen | Pantalla de inicio animada |

---

## Estructura del Proyecto

```
app/src/main/java/com/example/safezoneai/
|
|-- MainActivity.kt              # Punto de entrada, permisos, navegacion, volumen x3
|
|-- ui/                           # CAPA DE PRESENTACION (Views)
|   |-- HomeScreen.kt            # Pantalla principal con boton de emergencia
|   |-- EmergencyScreen.kt       # Pantalla activa durante emergencia
|   |-- MapScreen.kt             # Mapa OpenStreetMap con zonas peligrosas
|   |-- ContactScreen.kt         # CRUD de contactos de emergencia
|   |-- SettingsScreen.kt        # Configuracion de la app
|   |-- HistoryScreen.kt         # Historial de emergencias activadas
|   |-- SplashScreen.kt          # Splash animado al iniciar
|   |-- WhatsappButton.kt        # Componente boton WhatsApp
|   |-- theme/
|       |-- Color.kt             # Paleta: SafeGreen, DangerRed, PureWhite, etc.
|       |-- Theme.kt             # Tema Material 3
|       |-- Type.kt              # Tipografia
|
|-- viewmodel/                    # CAPA DE LOGICA (ViewModels)
|   |-- EmergencyViewModel.kt    # ViewModel central: emergencia, GPS, zonas, SMS, audio
|   |-- SettingsViewModel.kt     # Configuracion con SharedPreferences + StateFlow
|   |-- HistoryViewModel.kt      # Consulta de historial desde Room
|
|-- data/                         # CAPA DE DATOS (Model)
|   |-- SmartZoneDetector.kt     # Deteccion de ciudad + zonas peligrosas hardcoded
|   |-- ZoneRepository.kt        # Repositorio de zonas (interfaz)
|   |-- local/
|       |-- AppDatabase.kt       # Room Database principal
|       |-- EmergencyContact.kt  # Entity: contacto de emergencia
|       |-- EmergencyRecord.kt   # Entity: registro de emergencia
|       |-- EmergencyDao.kt      # DAO: operaciones sobre contactos
|       |-- EmergencyRecordDao.kt # DAO: operaciones sobre registros
|       |-- EmergencyHistoryDao.kt # DAO: consultas de historial
|
|-- utils/                        # UTILIDADES
|   |-- LocationUtils.kt         # GPS: permisos, updates, check GPS habilitado
|   |-- NotificationUtils.kt     # Notificaciones, SMS, vibracion
|   |-- AudioRecorder.kt         # Grabacion de audio (MediaRecorder)
|   |-- WhatsappUtils.kt         # Compartir ubicacion/audio por WhatsApp
|
|-- service/                      # SERVICIOS EN BACKGROUND
    |-- LocationTrackingService.kt   # Foreground service: monitoreo GPS continuo
    |-- VolumeEmergencyService.kt    # Foreground service: deteccion volumen x3
```

**NOTA IMPORTANTE**: `MainActivity.kt` esta fisicamente en la carpeta `ui/` pero su package es `com.example.safezoneai` (no `com.example.safezoneai.ui`). Esto es intencional y no debe cambiarse.

---

## Patrones de Diseno Utilizados

### 1. MVVM (Model-View-ViewModel)
- **View**: Composables en `ui/` - solo presentacion, sin logica de negocio
- **ViewModel**: `EmergencyViewModel`, `SettingsViewModel`, `HistoryViewModel` - logica y estado reactivo
- **Model**: Room entities + DAOs + `SmartZoneDetector`
- Comunicacion View <-> ViewModel via `StateFlow` + `collectAsState()`

### 2. Observer Pattern
- `StateFlow` y `MutableStateFlow` para estados reactivos
- Las pantallas observan cambios con `collectAsState()` y se recomponen automaticamente
- Ejemplo: `_currentLocation`, `_detectedCity`, `_isLoadingZones`, `_isEmergencyActive`

### 3. Repository Pattern
- `ZoneRepository` como interfaz para acceso a datos de zonas
- DAOs de Room como repositorios de datos locales
- Abstraccion entre la fuente de datos y el ViewModel

### 4. Front Controller Pattern
- `MainActivity` como punto de entrada unico
- Gestiona permisos, navegacion, ciclo de vida, y deteccion de volumen

### 5. Strategy Pattern
- Cada pantalla implementa su propia estrategia de visualizacion
- `SmartZoneDetector.fetchDangerousZones()` selecciona estrategia segun ciudad detectada

### 6. State Pattern
- Navegacion basada en estado String (`currentScreen`)
- El comportamiento de la app cambia segun pantalla actual

### 7. Callback Pattern
- `ActivityResultContracts` para permisos y contact picker
- Lambdas para navegacion entre pantallas (`onNavigateToMap`, `onBack`, etc.)

### 8. Singleton Pattern
- `AppDatabase.getDatabase()` - instancia unica de la base de datos
- `companion object` en servicios para estado compartido (ej: `LocationTrackingService.isRunning`)

---

## Principios SOLID Aplicados

### S - Single Responsibility Principle (SRP)
- `MainActivity`: solo gestiona permisos, ciclo de vida y navegacion
- `EmergencyViewModel`: solo logica de emergencia y estado
- `LocationUtils`: solo operaciones de GPS
- `NotificationUtils`: solo notificaciones, SMS y vibracion
- `SmartZoneDetector`: solo deteccion de ciudad y zonas
- Cada Screen: solo presentacion de su funcionalidad

### O - Open/Closed Principle (OCP)
- `SmartZoneDetector` esta abierto a extension (agregar nuevas ciudades) sin modificar la logica existente
- Nuevas pantallas se agregan al `when(currentScreen)` sin modificar las existentes
- `DangerLevel` enum extensible para nuevos niveles

### L - Liskov Substitution Principle (LSP)
- `EmergencyViewModel` extiende `AndroidViewModel` y puede usarse donde se espere un ViewModel
- Todas las entities de Room son data classes intercambiables en listas/flujos

### I - Interface Segregation Principle (ISP)
- DAOs separados por responsabilidad: `EmergencyDao` (contactos), `EmergencyRecordDao` (registros)
- Cada Screen recibe solo los callbacks que necesita (no un objeto "god")

### D - Dependency Inversion Principle (DIP)
- ViewModels dependen de abstracciones (`StateFlow`, `Flow`) no de implementaciones concretas
- Room DAOs como interfaz entre ViewModel y SQLite
- `LocationUtils` abstrae `FusedLocationProviderClient`

---

## Requerimientos Funcionales

### RF01 - Activacion de Emergencia
- El usuario puede activar una emergencia presionando el boton rojo en HomeScreen
- Al activar: vibra, envia SMS, abre WhatsApp, graba audio, guarda en historial
- El SMS se envia inmediatamente con la ubicacion actual disponible

### RF02 - Emergencia por Boton de Volumen
- 3 pulsaciones rapidas del boton de volumen (subir o bajar) en 2 segundos activan emergencia
- Funciona con la app abierta (onKeyDown en MainActivity)
- Funciona con la app en background (VolumeEmergencyService con ContentObserver)
- Trae la app al frente y navega a EmergencyScreen

### RF03 - Deteccion de Zonas Peligrosas
- Detecta automaticamente la ciudad del usuario via Geocoder
- Muestra zonas peligrosas con niveles: SAFE, LOW, MEDIUM, HIGH, CRITICAL
- Alerta con notificacion y vibracion al entrar en zona peligrosa

### RF04 - Mapa de Zonas
- Visualizacion en mapa OpenStreetMap con circulos de colores por nivel de peligro
- Vista de lista alternativa con distancia a cada zona
- Ubicacion del usuario en tiempo real

### RF05 - Gestion de Contactos de Emergencia
- CRUD completo: agregar, editar, eliminar contactos
- Importar contactos desde la agenda del telefono
- Limpieza automatica del numero telefonico
- Marcar contacto como "principal"

### RF06 - Envio de SMS de Emergencia
- Envia SMS automatico a todos los contactos registrados
- Incluye enlace de Google Maps con ubicacion
- Funciona sin ubicacion (mensaje alternativo)

### RF07 - Grabacion de Audio
- Graba audio automaticamente durante la emergencia
- Duracion configurable en Settings (default 60 segundos)
- Audio compartido via WhatsApp al finalizar

### RF08 - Compartir por WhatsApp
- Envia ubicacion como mensaje de texto con link de Maps
- Envia audio grabado como archivo adjunto
- Funciona con WhatsApp instalado

### RF09 - Historial de Emergencias
- Registro de cada emergencia con: fecha, ubicacion, zona, audio, contactos notificados
- Visualizacion en pantalla de historial

### RF10 - Configuracion
- Duracion de grabacion de audio
- Activar/desactivar monitoreo en background
- Gestionado via SharedPreferences

### RF11 - Monitoreo en Background
- Servicio foreground que monitorea ubicacion continuamente
- Detecta entrada a zonas peligrosas y alerta
- Notificacion persistente con estado actual

---

## Requerimientos No Funcionales

### RNF01 - Rendimiento
- SMS se envia en menos de 1 segundo tras activar emergencia
- Actualizaciones GPS cada 5 segundos (foreground) / 10 segundos (background)
- UI responsiva con Compose (recomposicion parcial)

### RNF02 - Disponibilidad
- La deteccion por volumen funciona en background via foreground service
- START_STICKY en servicios para reinicio automatico si el sistema los mata
- Funciona sin internet (SMS, GPS) excepto mapas y Geocoder

### RNF03 - Seguridad
- No se almacenan datos sensibles en texto plano
- Permisos solicitados solo los necesarios (principio de minimo privilegio)
- SMS enviado via SmsManager nativo (no via APIs externas)

### RNF04 - Usabilidad
- Boton de emergencia grande y accesible
- Activacion por volumen x3 para situaciones donde no se puede usar la pantalla
- Importacion de contactos desde agenda para evitar errores de tipeo
- Mensajes de error claros y en espanol

### RNF05 - Compatibilidad
- Android 9+ (API 28) hasta Android 14+ (API 34)
- Funciona sin Google Maps API key (usa OpenStreetMap)
- Compatible con dispositivos sin WhatsApp (SMS como respaldo)

### RNF06 - Mantenibilidad
- Arquitectura MVVM con separacion clara de capas
- Principios SOLID aplicados
- Codigo organizado por funcionalidad en paquetes

---

## Limitaciones Conocidas

### Limitaciones de la App
1. **Zonas peligrosas hardcoded**: Solo 6 ciudades tienen datos reales (Cuenca, Quito, Guayaquil, Portoviejo, CDMX, Bogota). Otras ciudades reciben zonas genericas con coordenadas 0,0.
2. **Geocoder requiere internet**: La deteccion de ciudad falla sin conexion a internet. El GPS funciona offline pero no la identificacion de la ciudad.
3. **Sin navegacion robusta**: Navegacion basada en String state, no usa Navigation Compose. Puede causar problemas si crece la app.
4. **WhatsApp obligatorio para audio**: Si WhatsApp no esta instalado, el audio grabado no se comparte (solo se guarda localmente).
5. **Sin autenticacion**: No hay login de usuario, cualquiera con acceso al dispositivo puede ver/modificar contactos e historial.

### Limitaciones Tecnicas
6. **SMS requiere saldo**: El envio de SMS depende del plan/saldo del operador movil.
7. **SmsManager puede ser null en Android 12+**: Se maneja con null-check pero en dispositivos sin telefonia no funciona.
8. **Volumen en background depende del servicio**: Si el sistema mata el servicio (bateria baja, modo ahorro), la deteccion por volumen se pierde hasta que la app se reabre.
9. **Un solo ViewModel central**: `EmergencyViewModel` maneja demasiadas responsabilidades. Deberia dividirse en el futuro.

### Limitaciones del Entorno de Desarrollo
10. **JDK 17 requerido**: La maquina de desarrollo tiene Java 25 (muy nuevo) y Java 15 (muy viejo). Configurar `JAVA_HOME` a JDK 17 antes de compilar.

---

## Permisos de la App

| Permiso | Uso |
|---------|-----|
| `ACCESS_FINE_LOCATION` | GPS preciso |
| `ACCESS_COARSE_LOCATION` | Ubicacion aproximada |
| `ACCESS_BACKGROUND_LOCATION` | Monitoreo en background |
| `RECORD_AUDIO` | Grabacion de evidencia |
| `SEND_SMS` | Alertas por SMS |
| `VIBRATE` | Feedback haptico |
| `POST_NOTIFICATIONS` | Notificaciones (Android 13+) |
| `INTERNET` | Mapas y Geocoder |
| `FOREGROUND_SERVICE` | Servicios en background |
| `FOREGROUND_SERVICE_LOCATION` | Servicio de monitoreo GPS |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Servicio de deteccion de volumen |

---

## Flujo de Emergencia (Detallado)

```
Usuario presiona boton rojo (o volumen x3)
    |
    v
1. _isEmergencyActive = true
2. VIBRAR inmediatamente
3. ENVIAR SMS (con _currentLocation.value, sin esperar GPS fresco)
    |
    v
4. getCurrentLocation() -> ubicacion fresca (puede tardar)
5. Mostrar notificacion con ubicacion
6. Abrir WhatsApp con mensaje + link Maps
    |
    v
7. Iniciar grabacion de audio (MediaRecorder)
8. Esperar duracion configurada (default 60s)
9. Detener grabacion
10. Abrir WhatsApp con archivo de audio
    |
    v
11. Guardar en Room DB (EmergencyRecord)
```

---

## Configuracion del Entorno

```bash
# Verificar JDK
java -version  # Debe ser 17

# Compilar
./gradlew assembleDebug

# Si falla por JDK, configurar:
export JAVA_HOME=/path/to/jdk-17
```

## Convenciones de Codigo
- Kotlin con nombres de variables/funciones en ingles
- Comentarios y textos de UI en espanol
- Theme colors en `ui/theme/Color.kt`
- Errores con try/catch + printStackTrace
- StateFlow para estados reactivos
- Contactos se importan de la agenda o se escriben manualmente con limpieza automatica (`cleanPhoneNumber()`)
