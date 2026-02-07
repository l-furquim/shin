FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file
COPY target/upload-service-*.jar app.jar

# Expose upload service port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
