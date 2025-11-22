@echo off
echo =========================================
echo SmartParking Live - Servidor Web
echo =========================================
echo.
echo Iniciando servidor Spring Boot...
echo La aplicacion estara disponible en:
echo   http://localhost:8080
echo.
echo Presione Ctrl+C para detener el servidor
echo =========================================
echo.

mvn spring-boot:run

if %errorlevel% neq 0 (
    echo.
    echo ERROR: No se pudo iniciar el servidor
    echo.
    echo Asegurese de que:
    echo   1. Maven esta instalado
    echo   2. Ha ejecutado install.bat primero
    echo   3. El puerto 8080 no esta en uso
    echo.
    pause
)

