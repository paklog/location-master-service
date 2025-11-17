# Location Master Service Helm Chart

Helm chart for deploying the Location Master Service to Kubernetes with Gateway API support.

## Overview

This Helm chart deploys the Location Master Service, a WMS microservice for warehouse location hierarchy management, built with Spring Boot Native (GraalVM).

## Features

- **Gateway API Integration**: Uses Kubernetes Gateway API (HTTPRoute) for modern ingress management
- **Spring Boot Native**: Optimized GraalVM native image for fast startup and low memory footprint
- **High Availability**: Configurable replicas with pod anti-affinity
- **Auto-scaling**: HorizontalPodAutoscaler based on CPU/Memory
- **Observability**: Prometheus metrics, OpenTelemetry tracing, health checks
- **Security**: Non-root containers, read-only root filesystem, security contexts
- **Production-ready**: PodDisruptionBudget, resource limits, health probes

## Prerequisites

- Kubernetes 1.25+
- Helm 3.8+
- Gateway API CRDs installed (v1.0.0+)
- PostgreSQL database
- Kafka cluster
- (Optional) Prometheus Operator for ServiceMonitor

### Install Gateway API CRDs

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
```

## Installation

### Quick Start

```bash
# Install with default values
helm install location-master ./deployments/helm/location-master-service

# Install with custom values
helm install location-master ./deployments/helm/location-master-service \
  --set image.tag=native \
  --set database.host=postgres-service \
  --set kafka.bootstrapServers=kafka-service:9092
```

### Custom Values File

```bash
# Create your custom values
cat > my-values.yaml <<EOF
replicaCount: 5

image:
  repository: my-registry/location-master-service
  tag: v1.0.0

database:
  host: my-postgres.example.com
  port: 5432
  name: locationdb
  username: appuser

kafka:
  bootstrapServers: my-kafka.example.com:9092

gateway:
  enabled: true
  hostnames:
    - location.example.com
EOF

# Install with custom values
helm install location-master ./deployments/helm/location-master-service -f my-values.yaml
```

## Configuration

### Common Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of replicas | `3` |
| `image.repository` | Container image repository | `location-master-service` |
| `image.tag` | Container image tag | `native` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |

### Database Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `database.host` | PostgreSQL host | `postgres-service` |
| `database.port` | PostgreSQL port | `5432` |
| `database.name` | Database name | `locationdb` |
| `database.username` | Database username | `postgres` |
| `database.existingSecret` | Existing secret for password | `location-master-db-secret` |

### Kafka Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `kafka.bootstrapServers` | Kafka bootstrap servers | `kafka-service:9092` |
| `kafka.topic` | CloudEvents topic | `physical-operations.location-master.events` |
| `kafka.consumerGroup` | Consumer group ID | `location-master-group` |

### Gateway API Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `gateway.enabled` | Enable Gateway API HTTPRoute | `true` |
| `gateway.gatewayClassName` | Gateway class name | `default` |
| `gateway.gatewayName` | Gateway name to attach to | `wms-gateway` |
| `gateway.hostnames` | Hostnames for routing | `["location-master.paklog.local"]` |
| `gateway.parentRefs` | Parent gateway references | See values.yaml |

### Autoscaling Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `2` |
| `autoscaling.maxReplicas` | Maximum replicas | `10` |
| `autoscaling.targetCPUUtilizationPercentage` | Target CPU % | `80` |
| `autoscaling.targetMemoryUtilizationPercentage` | Target Memory % | `80` |

### Resources

| Parameter | Description | Default |
|-----------|-------------|---------|
| `resources.limits.cpu` | CPU limit | `500m` |
| `resources.limits.memory` | Memory limit | `256Mi` |
| `resources.requests.cpu` | CPU request | `100m` |
| `resources.requests.memory` | Memory request | `128Mi` |

### Observability

| Parameter | Description | Default |
|-----------|-------------|---------|
| `observability.prometheus.enabled` | Enable Prometheus metrics | `true` |
| `observability.tracing.enabled` | Enable OpenTelemetry tracing | `true` |
| `observability.tracing.endpoint` | OTLP trace endpoint | `http://tempo:4318/v1/traces` |
| `serviceMonitor.enabled` | Create ServiceMonitor | `true` |

## Gateway API Setup

This chart uses Kubernetes Gateway API for traffic management. You need to have a Gateway resource configured.

### Example Gateway Configuration

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: wms-gateway
  namespace: default
spec:
  gatewayClassName: default
  listeners:
  - name: http
    protocol: HTTP
    port: 80
    allowedRoutes:
      namespaces:
        from: All
```

### HTTPRoute Behavior

The chart creates an HTTPRoute with the following paths:
- `/api/v1/locations` - Main API endpoints
- `/actuator` - Health checks and metrics
- `/swagger-ui` - API documentation
- `/api-docs` - OpenAPI specification

## Security

### Secrets Management

The chart creates a default secret for database credentials. For production:

```bash
# Create external secret
kubectl create secret generic location-master-db-secret \
  --from-literal=password='your-secure-password'

# Install chart
helm install location-master ./deployments/helm/location-master-service \
  --set database.existingSecret=location-master-db-secret
```

### Security Context

The deployment uses:
- Non-root user (UID 1001)
- Read-only root filesystem
- Dropped all capabilities
- seccomp profile

## Monitoring and Observability

### Prometheus Metrics

If `serviceMonitor.enabled=true`, a ServiceMonitor is created for Prometheus Operator:

```bash
# View metrics
kubectl port-forward svc/location-master 8080:8080
curl http://localhost:8080/actuator/prometheus
```

### Health Checks

Three types of probes are configured:
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Startup**: Initial health check with extended timeout

### Distributed Tracing

OpenTelemetry tracing is enabled by default. Configure your collector:

```yaml
observability:
  tracing:
    enabled: true
    endpoint: http://tempo:4318/v1/traces
    samplingProbability: 1.0
```

## High Availability

### Pod Anti-Affinity

The chart configures pod anti-affinity to spread pods across nodes:

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values:
                  - location-master-service
          topologyKey: kubernetes.io/hostname
```

### PodDisruptionBudget

Ensures minimum availability during cluster maintenance:

```yaml
podDisruptionBudget:
  enabled: true
  minAvailable: 1
```

## Upgrade and Rollback

### Upgrade

```bash
# Upgrade to new version
helm upgrade location-master ./deployments/helm/location-master-service \
  --set image.tag=v1.1.0

# Upgrade with values file
helm upgrade location-master ./deployments/helm/location-master-service \
  -f my-values.yaml
```

### Rollback

```bash
# View history
helm history location-master

# Rollback to previous version
helm rollback location-master

# Rollback to specific revision
helm rollback location-master 2
```

## Troubleshooting

### View Pods

```bash
kubectl get pods -l app.kubernetes.io/name=location-master-service
```

### View Logs

```bash
# All pods
kubectl logs -l app.kubernetes.io/name=location-master-service -f

# Specific pod
kubectl logs <pod-name> -f
```

### Describe Pod

```bash
kubectl describe pod <pod-name>
```

### Check HTTPRoute

```bash
kubectl get httproute
kubectl describe httproute location-master
```

### Check Gateway

```bash
kubectl get gateway
kubectl describe gateway wms-gateway
```

### Database Connection Issues

```bash
# Test database connectivity
kubectl run psql-test --rm -it --image=postgres:16 -- \
  psql -h postgres-service -U postgres -d locationdb
```

### Kafka Connection Issues

```bash
# Test Kafka connectivity
kubectl run kafka-test --rm -it --image=confluentinc/cp-kafka:7.5.0 -- \
  kafka-broker-api-versions --bootstrap-server kafka-service:9092
```

## Uninstallation

```bash
# Uninstall the release
helm uninstall location-master

# Uninstall and delete PVCs (if any)
helm uninstall location-master
kubectl delete pvc -l app.kubernetes.io/instance=location-master
```

## Development

### Lint Chart

```bash
helm lint ./deployments/helm/location-master-service
```

### Dry Run

```bash
helm install location-master ./deployments/helm/location-master-service --dry-run --debug
```

### Template Generation

```bash
helm template location-master ./deployments/helm/location-master-service > manifest.yaml
```

## Example Deployments

### Development Environment

```yaml
# dev-values.yaml
replicaCount: 1
autoscaling:
  enabled: false
resources:
  limits:
    cpu: 200m
    memory: 128Mi
  requests:
    cpu: 50m
    memory: 64Mi
```

```bash
helm install location-master ./deployments/helm/location-master-service -f dev-values.yaml
```

### Production Environment

```yaml
# prod-values.yaml
replicaCount: 5
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20
resources:
  limits:
    cpu: 1000m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
database:
  host: prod-postgres.example.com
  existingSecret: prod-db-secret
kafka:
  bootstrapServers: prod-kafka.example.com:9092
gateway:
  hostnames:
    - location.prod.example.com
observability:
  tracing:
    samplingProbability: 0.1
```

```bash
helm install location-master ./deployments/helm/location-master-service \
  -f prod-values.yaml \
  --namespace production
```

## Contributing

For chart improvements or bug reports, please open an issue at:
https://github.com/paklog/location-master-service/issues

## License

See the main project LICENSE file.
