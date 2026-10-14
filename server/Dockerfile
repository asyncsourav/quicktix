# 1. Build stage with Maven and JDK 25
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and compile jar
COPY src ./src
RUN mvn clean package -DskipTests  




# 2. Runtime stage with lightweight JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
