FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file
COPY target/metadata-service-*.jar app.jar

# Expose metadata service port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
