#  Grid Finders

> **Sistema Profesional de Monitorización de Aparcamiento en Tiempo Real**
> *Anteriormente conocido como SmartParking Live Observer*

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-ES6%2B-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)

**Grid Finders** es una plataforma avanzada de gestión y visualización de disponibilidad de aparcamiento que integra datos en tiempo real de la API gubernamental de Singapur (data.gov.sg). Diseñado con una arquitectura robusta basada en eventos y microservicios, permite a los usuarios encontrar plazas libres, analizar tendencias históricas y gestionar eventos de comunidad (KDDs).

---

##  Características Principales

###  Integración de Datos en Tiempo Real
- **Sincronización en Vivo**: Conexión directa con la API de LTA (Land Transport Authority) de Singapur.
- **Actualización Inteligente**: Sistema de *polling* optimizado que detecta cambios y actualiza solo los datos necesarios para minimizar el tráfico de red.
- **Bootstrap Asíncrono**: Carga inicial de metadatos (~2200 parkings) en segundo plano sin bloquear el arranque de la aplicación.

###  Dashboard Profesional
- **Visualización Interactiva**: Gráficos dinámicos con **Chart.js** que muestran la ocupación actual y tendencias históricas.
- **Mapa Geoespacial**: Integración con **Leaflet** para visualizar la ubicación exacta de los parkings y su estado (Libre/Ocupado) mediante códigos de color.
- **WebSockets**: Actualizaciones *push* al navegador sin necesidad de recargar la página.

###  Persistencia y Análisis
- **Base de Datos PostgreSQL**: Almacenamiento robusto de metadatos de parkings y registros históricos.
- **Histórico de Ocupación**: Seguimiento de cambios de disponibilidad a lo largo del tiempo para análisis predictivo.
- **Detección de Cambios**: Algoritmo eficiente que solo almacena nuevos registros cuando hay variaciones reales en la disponibilidad.

###  Comunidad y Eventos (KDD)
- **Gestión de Usuarios**: Registro e inicio de sesión seguro.
- **Eventos KDD**: Creación y gestión de quedadas (KDDs) en ubicaciones específicas.
- **Validación**: Reglas de negocio estrictas para nombres de usuario y creación de eventos.

---

##  Stack Tecnológico

### Backend
- **Java 17**: Lenguaje base.
- **Spring Boot 3.5**: Framework principal (Web, Data JPA, WebSocket).
- **Hibernate / JPA**: ORM para la gestión de base de datos.
- **PostgreSQL**: Motor de base de datos relacional.
- **Maven**: Gestión de dependencias y construcción.

### Frontend
- **Vanilla JavaScript (ES6+)**: Lógica de cliente ligera y rápida.
- **HTML5 / CSS3**: Diseño responsivo y moderno con variables CSS.
- **Chart.js**: Librería de gráficos para visualización de datos.
- **Leaflet**: Librería de mapas interactivos.
- **SockJS & STOMP**: Comunicación WebSocket robusta.

---

##  Arquitectura y Patrones de Diseño

El proyecto sigue una arquitectura limpia y modular, destacando el uso de los siguientes patrones:

1.  **Patrón Observer (Observador)**:
    *   Núcleo del sistema de notificaciones.
    *   Clases como WebSocketObserver, StatisticsModuleObserver y ActivityLogObserver reaccionan automáticamente a cambios en el estado de las plazas (ParkingSpot).

2.  **Patrón Repository**:
    *   Abstracción de la capa de datos mediante CarparkRepository y ParkingHistoryRepository.

3.  **Patrón Singleton**:
    *   Gestión de servicios (ParkingService, RealTimeParkingUpdater) a través del contenedor de Spring.

4.  **Patrón Adapter**:
    *   SingaporeCarparkClient adapta los datos JSON externos al modelo de dominio interno.

---

##  Configuración

El archivo src/main/resources/application.properties permite configurar el comportamiento del sistema:

`properties
# Nombre de la Aplicación
spring.application.name=Grid Finders

# Configuración del Servidor
server.port=8080

# Base de Datos (PostgreSQL)
spring.datasource.url=jdbc:postgresql://<HOST>:<PORT>/<DB_NAME>
spring.datasource.username=<USER>
spring.datasource.password=<PASSWORD>

# Optimización JPA
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true

# Intervalo de Actualización (ms)
parking.update-interval-ms=30000
` 

##  Instalación y Ejecución

1.  **Clonar el repositorio**:
    `ash
    git clone https://github.com/alerguezrojas/DAP-SmartParkingLive-Observer.git
    cd DAP-SmartParkingLive-Observer
    ` 

2.  **Configurar Base de Datos**:
    *   Asegúrate de tener una instancia de PostgreSQL corriendo.
    *   Actualiza las credenciales en pplication.properties.

3.  **Compilar y Ejecutar**:
    `ash
    ./mvnw spring-boot:run
    ` 

4.  **Acceder al Dashboard**:
    *   Abre tu navegador en http://localhost:8080.

---

##  Autores

*   **Alejandro** - *Lead Developer & Backend Architect*
*   **Aitor** - *Frontend Developer & UX Specialist*

---

##  Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE.md](LICENSE.md) para más detalles.
