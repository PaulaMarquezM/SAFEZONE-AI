<div align="center">

# SafeZone AI

### Aplicación Android de Seguridad Personal

*Protección inteligente en la palma de tu mano*

---

![Android](https://img.shields.io/badge/Android-API%2028%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.10.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-Design%20System-757575?style=for-the-badge&logo=materialdesign&logoColor=white)
![Room](https://img.shields.io/badge/Room-2.6.1-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)

</div>

---

## Descripcion

**SafeZone AI** es una aplicacion Android de seguridad personal que combina GPS, SMS automatico, grabacion de audio y mapas de zonas peligrosas para proteger al usuario en situaciones de riesgo. Desarrollada completamente en Kotlin con arquitectura MVVM y UI declarativa con Jetpack Compose.

La app esta disenada para responder en segundos: con un solo toque o tres pulsaciones del boton de volumen (incluso con la pantalla apagada), alerta a tus contactos de emergencia con tu ubicacion exacta.

---

## Funcionalidades Principales

### Activacion de Emergencia
- Boton de emergencia grande y accesible en la pantalla principal
- **Deteccion por boton de volumen**: 3 pulsaciones rapidas en 2 segundos activan la emergencia — funciona con la app abierta o en background
- Vibracion haptica inmediata como confirmacion

### Alertas Automaticas
- **SMS instantaneo** enviado a todos los contactos de emergencia con enlace de Google Maps a la ubicacion actual
- **Compartir por WhatsApp**: mensaje con ubicacion + archivo de audio adjunto
- Funciona sin WhatsApp instalado (SMS como respaldo)

### Grabacion de Audio
- Grabacion automatica al activar emergencia (duracion configurable, default 60 segundos)
- Audio guardado localmente y compartido por WhatsApp como evidencia

### Mapa de Zonas Peligrosas
- Mapa interactivo con **OpenStreetMap** (sin costo, sin API key)
- Zonas coloreadas por nivel de peligro: SAFE, LOW, MEDIUM, HIGH, CRITICAL
- Deteccion automatica de la ciudad del usuario via Geocoder
- Alerta con notificacion y vibracion al entrar en zona de riesgo
- Vista alternativa en lista con distancia a cada zona

### Monitoreo en Background
- Foreground service para monitoreo GPS continuo
- Notificacion persistente con estado actual
- Se reinicia automaticamente si el sistema lo mata (START_STICKY)

### Gestion de Contactos
- CRUD completo de contactos de emergencia
- Importacion directa desde la agenda del telefono
- Limpieza automatica del formato de numeros

### Historial
- Registro de cada emergencia: fecha, ubicacion, zona detectada, audio, contactos notificados

---

## Stack Tecnologico

| Tecnologia | Version | Proposito |
|---|---|---|
| **Kotlin** | 1.9.x | Lenguaje principal |
| **Jetpack Compose** | BOM 2024.10.00 | UI declarativa moderna |
| **Material 3** | via Compose BOM | Sistema de diseno |
| **Room** | 2.6.1 | Persistencia local (SQLite) |
| **Coroutines** | 1.7.3 | Programacion asincrona |
| **Play Services Location** | 21.1.0 | GPS (FusedLocationProviderClient) |
| **osmdroid** | 6.1.18 | Mapas OpenStreetMap (sin API key) |
| **Accompanist Permissions** | 0.32.0 | Manejo de permisos en Compose |
| **SplashScreen API** | core-splashscreen | Pantalla de inicio animada |

- **Min SDK**: 28 (Android 9) | **Target SDK**: 34 (Android 14)
- **Build**: Gradle Kotlin DSL | **JDK**: 17

---

## Arquitectura

El proyecto sigue el patron **MVVM (Model-View-ViewModel)** con separacion estricta de capas:

```
┌─────────────────────────────────────────┐
│              VIEW LAYER                 │
│   Composables (Jetpack Compose)         │
│   HomeScreen · EmergencyScreen          │
│   MapScreen · ContactScreen             │
│   HistoryScreen · SettingsScreen        │
└────────────────┬────────────────────────┘
                 │ StateFlow + collectAsState()
┌────────────────▼────────────────────────┐
│           VIEWMODEL LAYER               │
│   EmergencyViewModel (logica central)   │
│   SettingsViewModel (configuracion)     │
│   HistoryViewModel (historial)          │
└────────────────┬────────────────────────┘
                 │ Repository Pattern
┌────────────────▼────────────────────────┐
│              DATA LAYER                 │
│   Room DB · DAOs · SmartZoneDetector    │
│   LocationUtils · NotificationUtils     │
│   AudioRecorder · WhatsappUtils         │
└─────────────────────────────────────────┘
```

### Patrones de Diseno Aplicados

| Patron | Implementacion |
|---|---|
| **MVVM** | Separacion View / ViewModel / Model |
| **Observer** | `StateFlow` + `collectAsState()` para UI reactiva |
| **Repository** | DAOs de Room como capa de abstraccion de datos |
| **Front Controller** | `MainActivity` como punto de entrada unico |
| **Singleton** | `AppDatabase.getDatabase()` — instancia unica |
| **Strategy** | `SmartZoneDetector` selecciona datos segun ciudad detectada |
| **State** | Navegacion basada en estado String (`currentScreen`) |

### Principios SOLID

- **S** — Cada clase tiene una sola responsabilidad (`LocationUtils`, `NotificationUtils`, `AudioRecorder`)
- **O** — `SmartZoneDetector` es extensible sin modificar logica existente (nuevas ciudades)
- **L** — `EmergencyViewModel` extiende `AndroidViewModel` sin romper contratos
- **I** — DAOs separados por dominio: `EmergencyDao` (contactos), `EmergencyRecordDao` (registros)
- **D** — ViewModels dependen de `StateFlow`/`Flow`, no de implementaciones concretas

---

## Estructura del Proyecto

```
app/src/main/java/com/example/safezoneai/
│
├── MainActivity.kt               # Punto de entrada, permisos, navegacion, volumen x3
│
├── ui/                           # Capa de presentacion
│   ├── HomeScreen.kt             # Pantalla principal con boton de emergencia
│   ├── EmergencyScreen.kt        # Pantalla activa durante emergencia
│   ├── MapScreen.kt              # Mapa OSM con zonas peligrosas
│   ├── ContactScreen.kt          # CRUD de contactos de emergencia
│   ├── SettingsScreen.kt         # Configuracion
│   ├── HistoryScreen.kt          # Historial de emergencias
│   ├── SplashScreen.kt           # Splash animado al iniciar
│   ├── WhatsappButton.kt         # Componente boton WhatsApp
│   └── theme/                    # Colores, tipografia, tema Material 3
│
├── viewmodel/                    # Capa de logica y estado
│   ├── EmergencyViewModel.kt     # ViewModel central: GPS, SMS, audio, zonas
│   ├── SettingsViewModel.kt      # SharedPreferences + StateFlow
│   └── HistoryViewModel.kt       # Consultas Room para historial
│
├── data/                         # Capa de datos
│   ├── SmartZoneDetector.kt      # Deteccion de ciudad + zonas peligrosas
│   ├── ZoneRepository.kt         # Interfaz repositorio de zonas
│   └── local/
│       ├── AppDatabase.kt        # Room Database
│       ├── EmergencyContact.kt   # Entity: contacto de emergencia
│       ├── EmergencyRecord.kt    # Entity: registro de emergencia
│       ├── EmergencyDao.kt       # DAO: operaciones de contactos
│       └── EmergencyRecordDao.kt # DAO: operaciones de registros
│
├── utils/                        # Utilidades
│   ├── LocationUtils.kt          # GPS: FusedLocationProviderClient
│   ├── NotificationUtils.kt      # Notificaciones, SMS, vibracion
│   ├── AudioRecorder.kt          # Grabacion con MediaRecorder
│   └── WhatsappUtils.kt          # Compartir por WhatsApp
│
└── service/                      # Servicios en background
    ├── LocationTrackingService.kt # Foreground service: monitoreo GPS
    └── VolumeEmergencyService.kt  # Foreground service: volumen x3
```

---

## Flujo de Emergencia

```
Usuario presiona boton rojo (o volumen x3)
           │
           ▼
 1. _isEmergencyActive = true
 2. Vibrar inmediatamente
 3. Enviar SMS a todos los contactos (con ubicacion actual)
           │
           ▼
 4. Obtener ubicacion fresca (GPS)
 5. Mostrar notificacion con ubicacion
 6. Abrir WhatsApp con mensaje + link Maps
           │
           ▼
 7. Iniciar grabacion de audio (MediaRecorder)
 8. Esperar duracion configurada (default 60s)
 9. Detener grabacion
10. Compartir audio por WhatsApp
           │
           ▼
11. Guardar registro en Room DB (EmergencyRecord)
```

---

## Permisos Utilizados

| Permiso | Justificacion |
|---|---|
| `ACCESS_FINE_LOCATION` | GPS preciso para alertas y mapa |
| `ACCESS_BACKGROUND_LOCATION` | Monitoreo en segundo plano |
| `RECORD_AUDIO` | Grabacion de evidencia de audio |
| `SEND_SMS` | Alertas automaticas a contactos |
| `POST_NOTIFICATIONS` | Notificaciones (Android 13+) |
| `FOREGROUND_SERVICE` | Servicios de monitoreo continuo |
| `INTERNET` | Carga de mapas OSM y Geocoder |

---

## Ciudades con Datos de Zonas Peligrosas

La app incluye datos hardcoded de zonas peligrosas para las siguientes ciudades:

- **Ecuador**: Cuenca, Quito, Guayaquil, Portoviejo
- **Mexico**: Ciudad de Mexico (CDMX)
- **Colombia**: Bogota

---

## Instalacion y Compilacion

```bash
# Clonar el repositorio
git clone https://github.com/PaulaMarquezM/SAFEZONE-AI.git
cd SAFEZONE-AI

# Compilar (requiere JDK 17)
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug
```

> **Requisito**: JDK 17. La app no requiere ninguna API key.

---

## Sobre el Proyecto

Desarrollado como proyecto integrador aplicando conceptos de:

- Desarrollo Android nativo con Kotlin moderno
- UI declarativa con Jetpack Compose y Material Design 3
- Arquitectura limpia (MVVM + Repository Pattern)
- Principios SOLID en codigo de produccion
- Servicios en background (Foreground Services)
- Persistencia local con Room/SQLite
- Integracion con servicios del sistema (SMS, GPS, Audio, Contactos)

---

<div align="center">

Hecho con Kotlin para Android

</div>
