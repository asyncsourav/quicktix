

# 1. "Go grab an official Linux container that ALREADY has Maven and JDK 21 inside it and rename it as 'build' "
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
# change directory to app and further all code will be executed there 
WORKDIR /app
# we are copying the configuration of 'pom.xml' into '/app' directory
COPY pom.xml .
# copy the entire '/src' folder into the '/app' directory
COPY src ./src
# Inside this isolated container, compile the code to 'app.jar'
RUN mvn clean package -DskipTests  




# 2. "Now grab another tiny, fresh Linux container that ONLY has the Java Runtime (JRE)"
FROM eclipse-temurin:21-jre-alpine
# change the directory to '/app'
WORKDIR /app
# Copy ONLY the final 'app.jar' from the first container named 'build' (line 3) into this one
COPY --from=build /app/target/*.jar app.jar
# the server runs on port '8080'
EXPOSE 8080
# Run the jar inside this container
ENTRYPOINT ["java", "-jar", "app.jar"]
