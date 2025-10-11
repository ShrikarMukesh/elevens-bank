# Use a lightweight Java 21 runtime
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the pre-built JAR into the container
COPY target/customer-service-*.jar app.jar

# Expose application port
EXPOSE 6001

# Set JVM options (optional, container-optimized)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
