# Spring Native Build Guide

This guide explains how to build and run the Location Master Service as a GraalVM native image.

## What is Spring Native?

Spring Native provides support for compiling Spring applications to native executables using GraalVM. This offers several benefits:

- **Instant startup** - Applications start in milliseconds instead of seconds
- **Reduced memory footprint** - Lower memory consumption compared to JVM
- **Smaller container images** - Native images are significantly smaller
- **Better cloud efficiency** - Ideal for serverless and containerized environments

## Prerequisites

### For Local Native Builds

1. **GraalVM** (22.3 or later)
   ```bash
   # Using SDKMAN
   sdk install java 21-graalce
   sdk use java 21-graalce
   ```

2. **Native Image Tool**
   ```bash
   gu install native-image
   ```

3. **Build Tools**
   - macOS: Xcode Command Line Tools
   - Linux: gcc, glibc-devel, zlib-devel
   - Windows: Visual Studio Build Tools

### For Docker Native Builds

- Docker Desktop or Docker Engine

## Building Native Images

### Option 1: Build with Docker (Recommended)

The easiest way to build a native image is using Docker:

```bash
# Using the build script
./build-docker-native.sh

# Or manually
docker build -t location-master-service:native .
```

**Advantages:**
- No need to install GraalVM locally
- Consistent builds across environments
- Includes all runtime dependencies

### Option 2: Build Locally

Build a native executable on your local machine:

```bash
# Using the build script
./build-native.sh

# Or manually
./mvnw package -Pnative -DskipTests
```

The native executable will be created at:
```
target/location-master-service
```

**Note:** Local builds require GraalVM to be installed.

### Option 3: Spring Boot Buildpacks

Spring Boot can create Docker images using Cloud Native Buildpacks:

```bash
# Build native image with buildpacks
./mvnw spring-boot:build-image -Pnative

# Specify custom image name
./mvnw spring-boot:build-image -Pnative -Dspring-boot.build-image.imageName=location-service:native
```

## Running Native Images

### Run with Docker Compose

The complete stack (application + dependencies):

```bash
docker-compose up
```

This starts:
- PostgreSQL database
- Kafka broker
- Location Master Service (native image)

### Run Standalone Docker

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/locationdb \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  location-master-service:native
```

### Run Local Native Executable

```bash
# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/location_master
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Run the executable
./target/location-master-service
```

## Build Times and Image Sizes

### Typical Build Times
- **Docker Native Build**: 5-10 minutes (first build)
- **Local Native Build**: 3-8 minutes
- **Subsequent builds**: Faster with Maven cache

### Image Size Comparison
| Build Type | Size |
|------------|------|
| Standard JVM (openjdk:21-slim) | ~300-400 MB |
| Native Image | ~100-150 MB |

### Startup Time Comparison
| Build Type | Startup Time |
|------------|--------------|
| Standard JVM | 3-5 seconds |
| Native Image | 50-200 milliseconds |

## Configuration for Native Images

### Application Profiles

The application supports different profiles:

- **default**: Local development (JVM)
- **docker**: Docker containerized deployment
- **native**: Native image specific settings (if needed)

### Environment Variables

Key environment variables for Docker deployment:

```bash
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/locationdb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## Troubleshooting

### Build Failures

1. **Out of Memory During Build**
   ```bash
   # Increase Docker memory to at least 8GB
   # Docker Desktop -> Settings -> Resources -> Memory
   ```

2. **Missing Reflection Configuration**
   - Spring Boot 3.x handles most cases automatically
   - For custom reflection needs, add to `reflect-config.json`

3. **Serialization Issues**
   ```bash
   # Check JsonSerializer/Deserializer configurations
   # Ensure all DTOs are properly annotated
   ```

### Runtime Issues

1. **Database Connection Failures**
   - Verify PostgreSQL is accessible
   - Check connection string and credentials
   - Ensure database is initialized

2. **Kafka Connection Issues**
   - Verify Kafka is running
   - Check bootstrap servers configuration
   - Ensure topics are created

### Testing Native Images

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# API Documentation
open http://localhost:8080/swagger-ui.html
```

## Performance Tuning

### Memory Settings

Native images have different memory characteristics:

```bash
# Set max heap size (if needed)
docker run -e JAVA_OPTS="-Xmx256m" location-master-service:native
```

### GraalVM Native Image Options

Add to `pom.xml` under native profile if needed:

```xml
<buildArgs>
  <buildArg>--initialize-at-build-time=org.slf4j</buildArg>
  <buildArg>--enable-url-protocols=http,https</buildArg>
  <buildArg>-H:+ReportExceptionStackTraces</buildArg>
</buildArgs>
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Native Build

on: [push]

jobs:
  native-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up GraalVM
        uses: graalvm/setup-graalvm@v1
        with:
          version: 'latest'
          java-version: '21'
          components: 'native-image'

      - name: Build Native Image
        run: ./mvnw package -Pnative -DskipTests

      - name: Build Docker Image
        run: docker build -t location-service:native .
```

## Production Deployment

### Best Practices

1. **Use Multi-Stage Builds** - Minimize final image size
2. **Non-Root User** - Run as non-privileged user
3. **Health Checks** - Configure proper health endpoints
4. **Resource Limits** - Set memory and CPU limits
5. **Monitoring** - Enable Prometheus metrics

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: location-master-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: app
        image: location-master-service:native
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 10
```

## Additional Resources

- [Spring Native Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Spring Boot with GraalVM](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-and-virtual)
