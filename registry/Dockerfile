# registry/Dockerfile
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY registry/pom.xml registry/
COPY registry/pom.xml registry/
COPY registry/pom.xml registry/
# Copy source code for all modules only if they have inter-dependencies
# Better: Copy only the relevant module's source
COPY registry/src registry/src/

# Instead of mvn clean install -DskipTests
# We'll run the install command for the *specific module*
RUN mvn -f registry/pom.xml clean install -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copy only the built JAR for this specific module
COPY --from=builder /app/registry/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
