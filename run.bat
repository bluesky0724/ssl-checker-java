@echo off
REM SSL Certificate Monitor - Run Script for Windows

echo 🚀 Starting SSL Certificate Monitor...

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed. Please install Java 17 or higher.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    goto :check_version
)
:check_version
echo ✅ Java version: %JAVA_VERSION%

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven is not installed. Please install Maven 3.6+.
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('mvn -version ^| findstr /i "Apache Maven"') do (
    echo ✅ Maven version: %%g
    goto :build
)

:build
REM Build the application
echo 🔨 Building the application...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ❌ Build failed. Please check the errors above.
    pause
    exit /b 1
)

echo ✅ Build completed successfully!

REM Run the application
echo 🚀 Starting the application...
echo 📊 Application will be available at:
echo    - API Base URL: http://localhost:8080/api/v1
echo    - Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
echo    - H2 Console: http://localhost:8080/api/v1/h2-console
echo    - Health Check: http://localhost:8080/api/v1/actuator/health
echo.
echo Press Ctrl+C to stop the application
echo.

java -jar target/ssl-monitor.jar

pause 