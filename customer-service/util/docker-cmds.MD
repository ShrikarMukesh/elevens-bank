Got it ✅ — you want a **simple, standalone Dockerfile** that runs your `customer-service` **without worrying about the parent POM**.

This works if you **already built the JAR locally** (outside Docker) using Maven:

```bash
mvn clean package -DskipTests
```

Then the Dockerfile only needs to **copy the built JAR** and run it.

---

## 🐳 **Simple Dockerfile for customer-service**

```dockerfile
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
```

---

### 🏗️ **How to build and run**

1. **Build the Docker image** (run from the project root):

```bash
docker build -t customer-service:latest .
```

2. **Run the container**:

```bash
docker run -d -p 6001:6001 --name customer-service customer-service:latest
```

3. **Check logs**:

```bash
docker logs -f customer-service
```

---

✅ **Notes**

* This is the simplest approach — **no multi-stage build** needed.
* The **JAR must exist locally** in `target/`.
* Ideal for testing or small deployments.
