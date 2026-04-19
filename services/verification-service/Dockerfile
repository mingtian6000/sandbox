# Use Eclipse Temurin JDK 21 as base image
FROM eclipse-temurin:21-jdk-alpine AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY verification-service/pom.xml verification-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY verification-service/src verification-service/src

# Build the application
RUN ./mvnw package -pl verification-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/verification-service/target/*.jar app.jar

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8082/api/verification/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]