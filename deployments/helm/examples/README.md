# Helm Chart Examples

This directory contains example configurations for deploying the Location Master Service in different environments.

## Files

- **gateway.yaml** - Example Gateway API Gateway and GatewayClass resources
- **values-dev.yaml** - Development environment configuration
- **values-prod.yaml** - Production environment configuration

## Quick Start

### 1. Install Gateway API CRDs

First, install the Gateway API CRDs if not already present:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
```

### 2. Install a Gateway Controller

Choose and install one of the following Gateway controllers:

**Envoy Gateway (Recommended for getting started):**
```bash
helm install eg oci://docker.io/envoyproxy/gateway-helm --version v1.0.0 -n envoy-gateway-system --create-namespace
```

**Istio:**
```bash
istioctl install --set profile=minimal -y
```

**Kong:**
```bash
helm install kong kong/ingress -n kong --create-namespace
```

**Contour:**
```bash
kubectl apply -f https://projectcontour.io/quickstart/contour-gateway.yaml
```

### 3. Create the Gateway

Edit the `gateway.yaml` file to match your Gateway controller, then apply:

```bash
# Edit the controllerName in gateway.yaml first
kubectl apply -f deployments/helm/examples/gateway.yaml
```

Verify the Gateway is ready:

```bash
kubectl get gateway wms-gateway
```

### 4. Deploy the Application

#### Development Environment

```bash
# Create namespace
kubectl create namespace dev

# Create database secret
kubectl create secret generic location-master-db-secret \
  --from-literal=password='dev-password' \
  -n dev

# Install the chart
helm install location-master ./deployments/helm/location-master-service \
  -f deployments/helm/examples/values-dev.yaml \
  -n dev
```

#### Production Environment

```bash
# Create namespace
kubectl create namespace production

# Create database secret (use your actual password)
kubectl create secret generic location-master-db-secret-prod \
  --from-literal=password='YOUR-SECURE-PASSWORD' \
  -n production

# Create image pull secret if using private registry
kubectl create secret docker-registry registry-credentials \
  --docker-server=my-registry.example.com \
  --docker-username=user \
  --docker-password=password \
  --docker-email=email@example.com \
  -n production

# Install the chart
helm install location-master ./deployments/helm/location-master-service \
  -f deployments/helm/examples/values-prod.yaml \
  -n production
```

## Testing the Deployment

### Get Gateway IP/Hostname

```bash
kubectl get gateway wms-gateway -o jsonpath='{.status.addresses[0].value}'
```

### Add to /etc/hosts (for local testing)

```bash
# Development
echo "$(kubectl get gateway wms-gateway -o jsonpath='{.status.addresses[0].value}') location-master.dev.local" | sudo tee -a /etc/hosts

# Production
echo "$(kubectl get gateway wms-gateway -o jsonpath='{.status.addresses[0].value}') location.prod.example.com" | sudo tee -a /etc/hosts
```

### Test Endpoints

```bash
# Health check
curl http://location-master.dev.local/actuator/health

# API endpoint
curl http://location-master.dev.local/api/v1/locations

# Swagger UI
open http://location-master.dev.local/swagger-ui.html
```

## Monitoring

### View Pods

```bash
# Development
kubectl get pods -n dev -l app.kubernetes.io/name=location-master-service

# Production
kubectl get pods -n production -l app.kubernetes.io/name=location-master-service
```

### View Logs

```bash
# Development
kubectl logs -n dev -l app.kubernetes.io/name=location-master-service -f

# Production
kubectl logs -n production -l app.kubernetes.io/name=location-master-service -f --tail=100
```

### Check HTTPRoute

```bash
kubectl get httproute -n dev
kubectl describe httproute location-master -n dev
```

### Metrics

```bash
# Port-forward to access Prometheus metrics
kubectl port-forward -n dev svc/location-master 8080:8080
curl http://localhost:8080/actuator/prometheus
```

## Customization

### Override Specific Values

```bash
# Use example values as base and override specific values
helm install location-master ./deployments/helm/location-master-service \
  -f deployments/helm/examples/values-dev.yaml \
  --set replicaCount=2 \
  --set image.tag=v1.1.0
```

### Create Your Own Values File

```bash
# Copy an example and modify
cp deployments/helm/examples/values-dev.yaml my-values.yaml

# Edit my-values.yaml with your configuration

# Install with custom values
helm install location-master ./deployments/helm/location-master-service \
  -f my-values.yaml
```

## Environment Differences

### Development Environment Features
- Single replica (no autoscaling)
- Reduced resource limits
- Full tracing (100% sampling)
- Verbose logging
- No PodDisruptionBudget

### Production Environment Features
- 5 initial replicas with autoscaling (3-20)
- Higher resource limits
- Reduced tracing (10% sampling)
- Production logging levels
- PodDisruptionBudget for high availability
- Multiple Kafka brokers
- Node affinity and tolerations
- Enhanced security with image pull secrets

## Troubleshooting

### Gateway Not Ready

```bash
# Check Gateway status
kubectl describe gateway wms-gateway

# Check Gateway controller logs
kubectl logs -n envoy-gateway-system -l control-plane=envoy-gateway
```

### HTTPRoute Not Working

```bash
# Check HTTPRoute status
kubectl get httproute -n dev
kubectl describe httproute location-master -n dev

# Verify parentRefs match the Gateway
kubectl get httproute location-master -n dev -o yaml | grep -A 5 parentRefs
```

### Pod Not Starting

```bash
# Describe the pod
kubectl describe pod <pod-name> -n dev

# Check events
kubectl get events -n dev --sort-by='.lastTimestamp'

# Check secrets exist
kubectl get secrets -n dev
```

### Database Connection Issues

```bash
# Test database connectivity
kubectl run psql-test --rm -it --image=postgres:16 -n dev -- \
  psql -h postgres-service -U postgres -d locationdb_dev
```

## Cleanup

### Development

```bash
helm uninstall location-master -n dev
kubectl delete namespace dev
```

### Production

```bash
helm uninstall location-master -n production
kubectl delete namespace production
```

### Gateway (if needed)

```bash
kubectl delete -f deployments/helm/examples/gateway.yaml
```

## Additional Resources

- [Gateway API Documentation](https://gateway-api.sigs.k8s.io/)
- [Envoy Gateway Docs](https://gateway.envoyproxy.io/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [Helm Chart Documentation](../location-master-service/README.md)
