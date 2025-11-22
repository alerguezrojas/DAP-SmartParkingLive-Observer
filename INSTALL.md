# 🚀 Guía de Instalación - SmartParking Live

## Prerrequisitos

### 1. Instalar Java 17 o superior

#### Opción A: OpenJDK (Recomendado)
1. Descargar desde: https://adoptium.net/
2. Elegir **Temurin 17 (LTS)** o superior
3. Durante la instalación, marcar la opción **"Set JAVA_HOME variable"**
4. Verificar la instalación:
   ```bash
   java -version
   ```

#### Opción B: Oracle JDK
1. Descargar desde: https://www.oracle.com/java/technologies/downloads/
2. Instalar y configurar JAVA_HOME

### 2. Instalar Apache Maven

#### Opción A: Instalación Manual
1. Descargar desde: https://maven.apache.org/download.cgi
2. Descomprimir en una carpeta (ej: `C:\Program Files\Apache\maven`)
3. Agregar Maven al PATH:
   - Abrir "Configuración del sistema" → "Variables de entorno"
   - En "Variables del sistema", editar "Path"
   - Añadir: `C:\Program Files\Apache\maven\bin`
4. Verificar:
   ```bash
   mvn --version
   ```

#### Opción B: Usar Chocolatey
```bash
choco install maven
```

#### Opción C: Usar IntelliJ IDEA (Más fácil)
IntelliJ IDEA incluye Maven integrado, no necesita instalación separada.

## 📦 Instalación del Proyecto

### Opción 1: Usando IntelliJ IDEA (Recomendado)

1. **Abrir el proyecto en IntelliJ IDEA**
   - File → Open → Seleccionar la carpeta del proyecto
   - IntelliJ detectará automáticamente el archivo `pom.xml`

2. **Esperar a que Maven descargue las dependencias**
   - IntelliJ mostrará una notificación para importar el proyecto Maven
   - Hacer clic en "Import Changes" o "Enable Auto-Import"
   - Esperar a que finalice la descarga (esquina inferior derecha)

3. **Ejecutar la aplicación**
   - Abrir `src/smartparking/SmartParkingApplication.java`
   - Hacer clic derecho → "Run 'SmartParkingApplication.main()'"
   - O hacer clic en el botón ▶️ verde

4. **Acceder a la interfaz web**
   - Abrir el navegador en: http://localhost:8080

### Opción 2: Usando línea de comandos

1. **Navegar a la carpeta del proyecto**
   ```bash
   cd C:\Users\alerg\IdeaProjects\DAP-SmartParkingLive-Observer
   ```

2. **Instalar dependencias**
   ```bash
   mvn clean install
   ```
   O ejecutar el script:
   ```bash
   install.bat
   ```

3. **Ejecutar la aplicación**
   ```bash
   mvn spring-boot:run
   ```
   O ejecutar el script:
   ```bash
   run.bat
   ```

4. **Acceder a la interfaz web**
   - Abrir el navegador en: http://localhost:8080

### Opción 3: Ejecutar el Main.java original (sin interfaz web)

Si solo quieres probar el patrón Observer sin la interfaz web:

1. En IntelliJ IDEA, abrir `src/smartparking/Main.java`
2. Hacer clic derecho → "Run 'Main.main()'"
3. Ver la salida en la consola

## 🔧 Solución de Problemas

### Error: "Cannot resolve symbol 'springframework'"

**Causa**: Las dependencias de Maven no se han descargado.

**Solución**:
1. En IntelliJ IDEA: Clic derecho en `pom.xml` → "Maven" → "Reload project"
2. O ejecutar en terminal: `mvn clean install`
3. Verificar que tienes conexión a Internet

### Error: "Port 8080 was already in use"

**Causa**: El puerto 8080 está ocupado por otra aplicación.

**Solución 1**: Cambiar el puerto
1. Abrir `src/main/resources/application.properties`
2. Cambiar `server.port=8080` por `server.port=8081` (o cualquier otro puerto)

**Solución 2**: Liberar el puerto
```bash
# Encontrar proceso usando el puerto 8080
netstat -ano | findstr :8080

# Terminar el proceso (reemplazar PID con el número del proceso)
taskkill /PID [PID] /F
```

### Error: "JAVA_HOME not found"

**Solución**:
1. Buscar donde está instalado Java:
   ```bash
   where java
   ```
2. Configurar JAVA_HOME:
   - Variables de entorno → Nueva variable del sistema
   - Nombre: `JAVA_HOME`
   - Valor: Ruta a la carpeta JDK (ej: `C:\Program Files\Java\jdk-17`)

### La interfaz web no se actualiza en tiempo real

**Solución**:
1. Abrir la consola del navegador (F12)
2. Verificar que WebSocket está conectado (debe mostrar "🔌 WebSocket conectado")
3. Si no conecta, verificar que no hay firewall bloqueando el puerto 8080

### Error de compilación en archivos Java

**Solución**:
1. File → Project Structure → Project
2. Verificar que el SDK es Java 17 o superior
3. Project language level: 17

## 📱 Uso de la Interfaz Web

### Panel Principal

- **Estadísticas**: Muestra en tiempo real las plazas libres, ocupadas y fuera de servicio
- **Grid de Plazas**: Visualización en color de todas las plazas
  - 🟢 Verde: Libre
  - 🟠 Naranja: Ocupada
  - 🔴 Rojo: Fuera de servicio
- **Registro de Actividad**: Historial de cambios en tiempo real
- **Controles**: Para cambiar manualmente el estado de las plazas

### Probar la Aplicación

1. La aplicación inicia una simulación automática después de 5 segundos
2. También puedes cambiar estados manualmente:
   - Selecciona una plaza del menú desplegable
   - Elige un nuevo estado
   - Haz clic en "Actualizar Estado"
3. Los cambios se reflejarán instantáneamente en todos los navegadores conectados

## 🌐 API REST

### Endpoints Disponibles

```bash
# Obtener todas las plazas
GET http://localhost:8080/api/parking/spots

# Obtener una plaza específica
GET http://localhost:8080/api/parking/spots/1

# Cambiar estado de una plaza
PUT http://localhost:8080/api/parking/spots/1/status
Content-Type: application/json
{
  "status": "OCCUPIED"
}

# Obtener estadísticas
GET http://localhost:8080/api/parking/statistics
```

### Probar con curl

```bash
# Cambiar plaza 1 a ocupada
curl -X PUT http://localhost:8080/api/parking/spots/1/status ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"OCCUPIED\"}"

# Ver estadísticas
curl http://localhost:8080/api/parking/statistics
```

### Probar con Postman

1. Importar colección desde: `docs/SmartParking-API.postman_collection.json` (si existe)
2. O crear peticiones manualmente usando los endpoints anteriores

## 🎓 Estructura del Proyecto para Entrega

```
DAP-SmartParkingLive-Observer/
├── pom.xml                          # Configuración Maven
├── README.md                        # Documentación principal
├── INSTALL.md                       # Esta guía
├── install.bat                      # Script de instalación
├── run.bat                          # Script de ejecución
└── src/
    ├── smartparking/
    │   ├── SmartParkingApplication.java  # App Spring Boot (NUEVO)
    │   ├── Main.java                     # App consola original
    │   ├── controller/                   # REST API (NUEVO)
    │   ├── service/                      # Lógica de negocio (NUEVO)
    │   ├── web/                          # Config WebSocket (NUEVO)
    │   ├── model/                        # Modelos del dominio
    │   ├── observer/                     # Interfaz Observer
    │   └── observers/                    # Implementaciones Observer
    └── main/resources/
        ├── static/                       # Interfaz web (NUEVO)
        │   ├── index.html
        │   ├── styles.css
        │   └── app.js
        └── application.properties        # Configuración Spring
```

## 📞 Soporte

Si tienes problemas:

1. Verifica que cumples todos los prerrequisitos
2. Revisa la sección de solución de problemas
3. Consulta los logs en la consola de IntelliJ o en el terminal
4. Verifica la consola del navegador (F12) para errores de JavaScript

## ✅ Checklist de Instalación

- [ ] Java 17+ instalado y verificado (`java -version`)
- [ ] Maven instalado (o usando IntelliJ)
- [ ] Proyecto abierto en IntelliJ IDEA
- [ ] Dependencias descargadas (sin errores rojos en el código)
- [ ] Aplicación ejecutada correctamente
- [ ] Navegador abierto en http://localhost:8080
- [ ] Interfaz web cargada correctamente
- [ ] Indicador de conexión en verde
- [ ] Simulación automática funcionando

¡Listo para usar! 🚀

