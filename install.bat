@echo off
echo =========================================
echo SmartParking Live - Instalacion
echo =========================================
echo.

REM Verificar Java
echo Verificando Java...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java no esta instalado
    echo Por favor, instale Java 17 o superior
    pause
    exit /b 1
)
echo.

REM Verificar Maven
echo Verificando Maven...
mvn --version
if %errorlevel% neq 0 (
    echo ERROR: Maven no esta instalado
    echo.
    echo Por favor, instale Maven desde:
    echo https://maven.apache.org/download.cgi
    echo.
    echo O utilice el wrapper de Maven incluido en el proyecto:
    echo mvnw.cmd clean install
    pause
    exit /b 1
)
echo.

echo Instalando dependencias...
mvn clean install -DskipTests

if %errorlevel% neq 0 (
    echo ERROR: Fallo la instalacion
    pause
    exit /b 1
)

echo.
echo =========================================
echo Instalacion completada con exito
echo =========================================
echo.
echo Para ejecutar el proyecto:
echo   1. Ejecute: run.bat
echo   2. O bien: mvn spring-boot:run
echo   3. Abra http://localhost:8080 en su navegador
echo.
pause

