# 🚗 SmartParking Live - Sistema de Gestión de Aparcamiento

Sistema de gestión de aparcamiento en tiempo real implementando el patrón Observer con interfaz web moderna.

## 📋 Características

- ✅ **Patrón Observer**: Notificaciones en tiempo real a múltiples observadores.
- 🌐 **Interfaz Web Moderna**: Dashboard responsive con visualización en tiempo real.
- 🔌 **WebSocket**: Actualizaciones instantáneas sin recargar la página.
- 📊 **Estadísticas en Vivo**: Contadores de plazas libres, ocupadas y en mantenimiento.
- 🎯 **API REST Completa**: Endpoints para gestión externa.
- 📱 **Simulación Móvil**: Observador que simula notificaciones a dispositivos móviles.
- 🔐 **Módulo de Seguridad**: Observador para alertas de seguridad.
- 📈 **Módulo de Estadísticas**: Observador para análisis de datos.

## 🛠️ Tecnologías Utilizadas

- **Backend**: Java 17+, Spring Boot 3.1.5
- **WebSocket**: STOMP over SockJS
- **Frontend**: HTML5, CSS3 (Variables, Flexbox/Grid), JavaScript Vanilla
- **Patrón de Diseño**: Observer
- **Build Tool**: Maven

## 🏗️ Arquitectura

### Patrón Observer

El sistema implementa el patrón Observer donde:

- **Subject**: `ParkingSpot` (cada plaza de aparcamiento)
- **Observers**: 
  - `WebDashboardObserver`: Panel web con resumen.
  - `SecurityModuleObserver`: Alertas de seguridad.
  - `StatisticsModuleObserver`: Estadísticas en tiempo real.
  - `MobileNotifierObserver`: Notificaciones móviles simuladas.
  - `WebSocketObserver`: Puente para notificaciones web en tiempo real.

### Estructura del Proyecto

```
src/
├── main/
│   ├── java/smartparking/
│   │   ├── SmartParkingApplication.java  # Aplicación Spring Boot
│   │   ├── controller/                   # REST API
│   │   ├── service/                      # Lógica de negocio
│   │   ├── web/                          # Configuración WebSocket
│   │   ├── model/                        # Modelos (ParkingLot, ParkingSpot)
│   │   ├── observer/                     # Interfaz ParkingObserver
│   │   └── observers/                    # Implementaciones de observadores
│   └── resources/
│       ├── static/                       # Frontend (HTML, CSS, JS)
│       └── application.properties        # Configuración
```

## 🚀 Instalación y Ejecución

### Prerrequisitos

1. **Java 17** o superior instalado (`java -version`).
2. **Maven** instalado (`mvn -version`) o usar el wrapper/IDE.
3. Puerto **8080** libre.

### Pasos para ejecutar

#### Opción 1: Usando IntelliJ IDEA (Recomendado)
1. Abrir el proyecto en IntelliJ IDEA.
2. Esperar a que Maven descargue las dependencias.
3. Ejecutar la clase `src/main/java/smartparking/SmartParkingApplication.java`.

#### Opción 2: Línea de Comandos
1. Navegar a la carpeta del proyecto.
2. Compilar y ejecutar:
   ```bash
   mvn spring-boot:run
   ```

#### Opción 3: Empaquetado JAR
1. Compilar:
   ```bash
   mvn clean package
   ```
2. Ejecutar:
   ```bash
   java -jar target/smart-parking-live-observer-1.0.0.jar
   ```

Una vez iniciado, accede a:
- **Web**: [http://localhost:8080](http://localhost:8080)
- **API**: [http://localhost:8080/api/parking](http://localhost:8080/api/parking)

## 🎨 Interfaz Web

La interfaz web ha sido diseñada para ser intuitiva y profesional:

- **Dashboard**: Muestra tarjetas con estadísticas clave (Total, Libres, Ocupadas, Mantenimiento).
- **Mapa de Plazas**: Visualización gráfica del estado de cada plaza.
  - 🟢 Libre
  - 🔴 Ocupada
  - 🟠 Mantenimiento
- **Registro de Actividad**: Log en tiempo real de todos los eventos del sistema.
- **Panel de Control**: Permite cambiar manualmente el estado de las plazas para pruebas.

### Flujo de Datos en Tiempo Real
1. Cambio de estado en el Backend (API/Simulación).
2. `ParkingSpot` notifica a `WebSocketObserver`.
3. `WebSocketObserver` envía mensaje STOMP a `/topic/parking-updates`.
4. Frontend recibe el mensaje y actualiza el DOM instantáneamente.

## 📡 API REST

Endpoints disponibles para integración:

- `GET /api/parking/spots`: Listar todas las plazas.
- `GET /api/parking/spots/{id}`: Obtener detalle de una plaza.
- `PUT /api/parking/spots/{id}/status`: Cambiar estado.
  ```json
  { "status": "OCCUPIED" }
  ```
- `GET /api/parking/statistics`: Obtener estadísticas actuales.

### Ejemplo cURL
```bash
curl -X PUT http://localhost:8080/api/parking/spots/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"OCCUPIED"}'
```

## 🔧 Solución de Problemas

- **Puerto 8080 ocupado**:
  Edita `src/main/resources/application.properties` y cambia `server.port=8081`.
- **Errores de compilación**:
  Ejecuta `mvn clean install` para forzar la descarga de dependencias.
- **No conecta WebSocket**:
  Verifica que no haya firewalls bloqueando el puerto y revisa la consola del navegador (F12).

## 🤝 Contribuir

Este es un proyecto educativo de la Universidad de La Laguna (ULL) para demostrar el patrón de diseño Observer en un entorno real con Spring Boot.

---
Desarrollado con ❤️ usando Java y Spring Boot.

