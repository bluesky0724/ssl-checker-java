#!/bin/bash

# SSL Certificate Monitor - Run Script

echo "🚀 Starting SSL Certificate Monitor..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version $JAVA_VERSION is not supported. Please install Java 17 or higher."
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6+."
    exit 1
fi

echo "✅ Maven version: $(mvn -version | head -n 1)"

# Build the application
echo "🔨 Building the application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

echo "✅ Build completed successfully!"

# Run the application
echo "🚀 Starting the application..."
echo "📊 Application will be available at:"
echo "   - API Base URL: http://localhost:8080/api/v1"
echo "   - Swagger UI: http://localhost:8080/api/v1/swagger-ui.html"
echo "   - H2 Console: http://localhost:8080/api/v1/h2-console"
echo "   - Health Check: http://localhost:8080/api/v1/actuator/health"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

java -jar target/ssl-monitor.jar 