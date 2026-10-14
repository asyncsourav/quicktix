

# 1. "Go grab an official Linux container that ALREADY has Maven and JDK 21 inside it"
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Inside this isolated container, compile the code to app.jar
RUN mvn clean package -DskipTests  




# 2. "Now grab another tiny, fresh Linux container that ONLY has the Java Runtime (JRE)"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy ONLY the final app.jar from the first container into this one
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Run the jar inside this container
ENTRYPOINT ["java", "-jar", "app.jar"]
