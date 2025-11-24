FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl transaction-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/transaction-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
