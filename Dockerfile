# ==============================================================================
# Multi-Stage Dockerfile for Rent Payment Reminder Portal
# Stage 1: Build application using Maven 3.9 and Eclipse Temurin JDK 17
# Stage 2: Production JRE 17 Runtime Environment
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy build configuration and source files
COPY pom.xml .
COPY src ./src

# Build the executable WAR artifact (skipping unit/Selenium tests during container packaging)
RUN mvn clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Runtime Environment
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

LABEL maintainer="Rent Reminder DevOps Team" \
      app="rent-payment-reminder-portal" \
      version="0.1.0-SNAPSHOT" \
      description="Rent Payment Reminder Portal Containerized Application"

WORKDIR /app

# Install curl for container health check and clean apt caches
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Security: Create non-root application user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

# Create persistent storage directory for SQLite database
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Copy the packaged Spring Boot WAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/rent-reminder-portal.war /app/app.war

# Set non-root user context
USER appuser

# Expose Spring Boot server port (8081)
EXPOSE 8081

# Configure environment defaults
ENV SERVER_PORT=8081 \
    SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/rentdb.sqlite \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck to verify the web server is answering HTTP requests
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/login || exit 1

# Execute the Spring Boot executable WAR
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -Dspring.datasource.url=${SPRING_DATASOURCE_URL} -jar /app/app.war"]
