# ============================================
# Stage 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the entire project (build context is the repo root)
# .dockerignore should exclude target/ dirs and unnecessary files
COPY . .

# Build only cards-service (and its parent POM dependencies)
RUN mvn clean package -pl cards-service -am -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=3

# ============================================
# Stage 2: Production Image
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# OCI metadata labels
LABEL org.opencontainers.image.title="cards-service" \
    org.opencontainers.image.description="Elevens Bank Cards Service" \
    org.opencontainers.image.source="https://github.com/ShrikarMukesh/elevens-bank" \
    org.opencontainers.image.vendor="Elevens Bank"

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/cards-service/target/*.jar app.jar

# Create logs directory for Logback file appender
RUN mkdir -p /app/logs

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose application port and management port
EXPOSE 2001 8079

# JVM container-aware settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Health check using actuator
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=60s \
    CMD wget -qO- http://localhost:8079/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
