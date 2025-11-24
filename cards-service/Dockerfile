FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl cards-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/cards-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
