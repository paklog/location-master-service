# Multi-stage Dockerfile for Spring Native Build

# Stage 1: Build the native image using GraalVM
FROM ghcr.io/graalvm/native-image-community:21-ol9 AS builder

# Install Maven
RUN microdnf install -y findutils

# Set working directory
WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -Pnative

# Copy source code
COPY src src

# Build native image
RUN ./mvnw package -Pnative -DskipTests

# Stage 2: Create minimal runtime image
FROM debian:bookworm-slim

# Install required runtime dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -r -u 1001 -g root appuser

# Set working directory
WORKDIR /app

# Copy the native executable from builder stage
COPY --from=builder /build/target/location-master-service /app/location-master-service

# Set ownership
RUN chown -R appuser:root /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD pgrep location-master-service || exit 1

# Run the native application
ENTRYPOINT ["/app/location-master-service"]
