# Guía de Ejecución - SmartParking Live Observer

## ✅ Problemas Resueltos

Se han corregido los siguientes problemas:

1. **SmartParkingApplication.java** movido de `src/main/resources/` a `src/main/java/smartparking/`
2. **SpotStatus.java** ahora contiene la definición correcta del enum
3. **pom.xml** configurado correctamente con la estructura estándar de Maven
4. Archivos duplicados eliminados de resources

## 🚀 Cómo Ejecutar el Proyecto

### Opción 1: Desde IntelliJ IDEA

1. **Invalidar caché del IDE** (si aparecen errores):
   - Ve a: `File` → `Invalidate Caches...` → Marca todas las opciones → `Invalidate and Restart`

2. **Ejecutar la aplicación**:
   - Abre el archivo: `src/main/java/smartparking/SmartParkingApplication.java`
   - Haz clic derecho y selecciona `Run 'SmartParkingApplication'`
   - O presiona `Shift + F10`

### Opción 2: Desde la línea de comandos

```powershell
# Compilar el proyecto
mvn clean package

# Ejecutar la aplicación
java -jar target/smart-parking-live-observer-1.0.0.jar
```

### Opción 3: Con Maven directamente

```powershell
mvn spring-boot:run
```

## 📊 URLs de Acceso

Una vez que la aplicación esté corriendo, accede a:

- **Interfaz Web**: http://localhost:8080
- **API REST**: http://localhost:8080/api/parking
- **WebSocket**: ws://localhost:8080/ws-parking

## 🧪 Endpoints de la API REST

- `GET /api/parking/spots` - Obtener todas las plazas
- `GET /api/parking/spots/{id}` - Obtener una plaza específica
- `PUT /api/parking/spots/{id}/status` - Cambiar estado de una plaza
- `GET /api/parking/statistics` - Obtener estadísticas del parking

### Ejemplo de cambio de estado:

```bash
curl -X PUT http://localhost:8080/api/parking/spots/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "OCCUPIED"}'
```

## 🎯 Características

- ✅ Patrón Observer implementado
- ✅ Interfaz web en tiempo real con WebSocket
- ✅ API REST para gestión de plazas
- ✅ Estadísticas en tiempo real
- ✅ Simulación automática de cambios
- ✅ 5 observadores: WebDashboard, Security, Statistics, Mobile, WebSocket

## 📝 Estructura del Proyecto

```
src/main/java/smartparking/
├── SmartParkingApplication.java    # Aplicación principal
├── controller/
│   └── ParkingController.java      # Controlador REST
├── model/
│   ├── ParkingLot.java            # Modelo del parking
│   ├── ParkingSpot.java           # Modelo de plaza
│   └── SpotStatus.java            # Estados posibles
├── observer/
│   └── ParkingObserver.java       # Interfaz Observer
├── observers/
│   ├── MobileNotifierObserver.java
│   ├── SecurityModuleObserver.java
│   ├── StatisticsModuleObserver.java
│   ├── WebDashboardObserver.java
│   └── WebSocketObserver.java
├── service/
│   └── ParkingService.java        # Lógica de negocio
└── web/
    └── WebSocketConfig.java       # Configuración WebSocket

src/main/resources/
├── application.properties          # Configuración Spring
└── static/
    ├── index.html                 # Interfaz web
    ├── app.js                     # Lógica frontend
    └── styles.css                 # Estilos
```

## ⚠️ Requisitos

- Java 17 o superior
- Maven 3.6+
- Puerto 8080 disponible

## 🐛 Solución de Problemas

### Si el IDE muestra errores rojos:
1. Invalidar caché: `File → Invalidate Caches... → Invalidate and Restart`
2. Reimportar Maven: Click derecho en `pom.xml` → `Maven → Reload Project`

### Si el puerto 8080 está ocupado:
Edita `src/main/resources/application.properties` y cambia:
```properties
server.port=8080
```
a otro puerto, por ejemplo:
```properties
server.port=8081
```

## 🎉 ¡Proyecto Listo!

El proyecto está completamente funcional y listo para ejecutarse.

