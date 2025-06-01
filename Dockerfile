# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy the parent pom.xml if it's a multi-module project
COPY pom.xml .
# Copy the specific module's pom.xml and source based on MODULE_NAME argument
ARG MODULE_NAME
COPY ${MODULE_NAME}/pom.xml ${MODULE_NAME}/
COPY ${MODULE_NAME}/src ${MODULE_NAME}/src/

# Build the specific module
RUN mvn -f ${MODULE_NAME}/pom.xml clean install -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Instalar curl no sistema Alpine Linux
RUN apk add --no-cache curl

# Copy the built JAR from the builder stage based on MODULE_NAME argument
ARG MODULE_NAME
COPY --from=builder /app/${MODULE_NAME}/target/*.jar app.jar

# Define default entrypoint. Can be overridden in docker-compose.yml if needed.
ENTRYPOINT ["java", "-jar", "app.jar"]