# 🚗 SmartParking Live - Sistema de Gestión de Aparcamiento

Sistema de gestión de aparcamiento en tiempo real implementando el patrón Observer con interfaz web.

## 📋 Características

- ✅ Patrón Observer para notificaciones en tiempo real
- 🌐 Interfaz web moderna y responsive
- 🔌 WebSocket para actualizaciones en tiempo real
- 📊 Dashboard con estadísticas en vivo
- 🎯 API REST completa
- 📱 Notificaciones móviles simuladas
- 🔐 Módulo de seguridad
- 📈 Módulo de estadísticas

## 🛠️ Tecnologías Utilizadas

- **Backend**: Spring Boot 3.1.5
- **WebSocket**: STOMP over SockJS
- **Frontend**: HTML5, CSS3, JavaScript vanilla
- **Patrón de Diseño**: Observer
- **Java**: 17+

## 🚀 Instalación y Ejecución

### Requisitos Previos

- Java 17 o superior
- Maven 3.6+

### Pasos para ejecutar

1. **Clonar o descargar el proyecto**

2. **Compilar el proyecto con Maven:**
   ```bash
   mvn clean install
   ```

3. **Ejecutar la aplicación:**
   ```bash
   mvn spring-boot:run
   ```

   O ejecutar directamente la clase `SmartParkingApplication`

4. **Acceder a la interfaz web:**
   
   Abrir el navegador en: [http://localhost:8080](http://localhost:8080)

## 📡 API REST

### Endpoints disponibles:

- **GET** `/api/parking/spots` - Obtener todas las plazas
- **GET** `/api/parking/spots/{id}` - Obtener una plaza específica
- **PUT** `/api/parking/spots/{id}/status` - Cambiar estado de una plaza
- **GET** `/api/parking/statistics` - Obtener estadísticas del parking

### Ejemplo de cambio de estado:

```bash
curl -X PUT http://localhost:8080/api/parking/spots/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"OCCUPIED"}'
```

## 🎨 Interfaz Web

La interfaz web incluye:

- **Dashboard en tiempo real** con estadísticas
- **Grid visual** de todas las plazas con código de colores:
  - 🟢 Verde: Plaza libre
  - 🟠 Naranja: Plaza ocupada
  - 🔴 Rojo: Plaza fuera de servicio
- **Registro de actividad** con todas las actualizaciones
- **Controles de prueba** para cambiar estados manualmente

## 🏗️ Arquitectura

### Patrón Observer

El sistema implementa el patrón Observer donde:

- **Subject**: `ParkingSpot` (cada plaza de aparcamiento)
- **Observers**: 
  - `WebDashboardObserver` - Panel web con resumen
  - `SecurityModuleObserver` - Alertas de seguridad
  - `StatisticsModuleObserver` - Estadísticas en tiempo real
  - `MobileNotifierObserver` - Notificaciones móviles simuladas
  - `WebSocketObserver` - Notificaciones web en tiempo real

### Estructura del Proyecto

```
src/
├── smartparking/
│   ├── SmartParkingApplication.java  # Aplicación Spring Boot
│   ├── Main.java                     # Aplicación consola original
│   ├── controller/
│   │   └── ParkingController.java    # REST API
│   ├── service/
│   │   └── ParkingService.java       # Lógica de negocio
│   ├── web/
│   │   └── WebSocketConfig.java      # Configuración WebSocket
│   ├── model/
│   │   ├── ParkingLot.java          # Modelo del parking
│   │   ├── ParkingSpot.java         # Modelo de plaza (Subject)
│   │   └── SpotStatus.java          # Estados posibles
│   ├── observer/
│   │   └── ParkingObserver.java     # Interfaz Observer
│   └── observers/
│       ├── WebDashboardObserver.java
│       ├── SecurityModuleObserver.java
│       ├── StatisticsModuleObserver.java
│       ├── MobileNotifierObserver.java
│       └── WebSocketObserver.java
└── main/resources/
    ├── static/
    │   ├── index.html               # Interfaz web
    │   ├── styles.css               # Estilos
    │   └── app.js                   # Lógica cliente
    └── application.properties       # Configuración Spring
```

## 🔄 Flujo de Datos

1. **Cambio de Estado**: Se actualiza el estado de una plaza (vía API o simulación)
2. **Notificación**: `ParkingSpot` notifica a todos sus observadores
3. **Procesamiento**: Cada observador procesa la actualización según su responsabilidad
4. **WebSocket**: `WebSocketObserver` envía la actualización a los clientes web
5. **Actualización UI**: La interfaz web se actualiza en tiempo real

## 🎯 Uso

### Simulación Automática

Al iniciar la aplicación, se ejecuta una simulación automática después de 5 segundos que:
- Ocupa varias plazas
- Libera algunas plazas
- Marca plazas fuera de servicio

### Control Manual

Desde la interfaz web puedes:
1. Seleccionar cualquier plaza del selector
2. Elegir un nuevo estado
3. Hacer clic en "Actualizar Estado"
4. Ver la actualización en tiempo real en el dashboard

## 📝 Logs de Consola

Los observadores imprimen información detallada en la consola:
- `[WebDashboard]` - Resumen del estado del parking
- `[SecurityModule]` - Alertas de seguridad
- `[StatisticsModule]` - Estadísticas de ocupación
- `[MobileNotifier]` - Notificaciones de plazas específicas

## 🌟 Características Avanzadas

- **Conexión automática**: El cliente web se reconecta automáticamente si se pierde la conexión
- **Animaciones suaves**: Transiciones visuales para cambios de estado
- **Responsive**: La interfaz se adapta a diferentes tamaños de pantalla
- **Registro de actividad**: Historial de los últimos 50 cambios
- **Indicador de conexión**: Muestra el estado de la conexión WebSocket

## 🤝 Contribuir

Este proyecto es un ejemplo educativo del patrón Observer aplicado a un sistema real.

## 📄 Licencia

Proyecto educativo - Universidad de La Laguna (ULL)

---

Desarrollado con ❤️ usando el patrón Observer

