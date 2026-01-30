# Multi-stage build for Dropwizard application

# Stage 1: Build the application
FROM maven:3.8-openjdk-11 AS builder

WORKDIR /app

# Copy the parent POM and module POMs
COPY pom.xml .
COPY models/pom.xml models/
COPY core/pom.xml core/
COPY server/pom.xml server/

# Download dependencies (this layer will be cached if POMs don't change)
RUN mvn dependency:go-offline -B

# Copy the source code
COPY models/src models/src
COPY core/src core/src
COPY server/src server/src

# Build the application
RUN mvn clean install -DskipTests

# Stage 2: Run the application
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/server/target/server-1.0-SNAPSHOT.jar /app/server.jar

# Copy the configuration file
COPY server/config.yml /app/config.yml

# Expose application and admin ports
EXPOSE 8080 8081

# Run the application
ENTRYPOINT ["java", "-jar", "/app/server.jar", "server", "/app/config.yml"]
