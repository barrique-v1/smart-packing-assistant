# Kubernetes Deployment Guide

This guide explains how to deploy the Smart Packing Assistant to a Kubernetes cluster using Kind (Kubernetes in Docker).

## Prerequisites

- Docker Desktop running
- Kind installed
- kubectl installed
- Docker images built (from Phase 8)

## Quick Start

### 1. Install Kind (if not already installed)

**Windows (PowerShell)**:
```powershell
choco install kind
# OR
curl.exe -Lo kind-windows-amd64.exe https://kind.sigs.k8s.io/dl/v0.20.0/kind-windows-amd64
Move-Item .\kind-windows-amd64.exe C:\Windows\System32\kind.exe
```

**Linux/macOS**:
```bash
# macOS
brew install kind

# Linux
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

Verify installation:
```bash
kind version
```

### 2. Create Kind Cluster

```bash
# Create a new cluster named "packing-assistant"
kind create cluster --name packing-assistant

# Verify cluster is running
kubectl cluster-info --context kind-packing-assistant
```

### 3. Build Docker Images (if not done)

```bash
# From project root
docker compose build
```

### 4. Load Docker Images into Kind

**IMPORTANT**: Kind clusters don't have access to local Docker images by default. You must load them explicitly:

```bash
# Load API Gateway image
kind load docker-image smart-packing-assistant-api-gateway:latest --name packing-assistant

# Load AI Worker image
kind load docker-image smart-packing-assistant-ai-worker:latest --name packing-assistant

# Verify images are loaded
docker exec -it packing-assistant-control-plane crictl images | grep smart-packing
```

### 5. Deploy to Kubernetes

Deploy all manifests in order:

```bash
# Apply all manifests
kubectl apply -f k8s/

# OR apply in specific order (recommended for first deployment)
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-postgres-secret.yaml
kubectl apply -f k8s/02-postgres-pvc.yaml
kubectl apply -f k8s/03-postgres-deployment.yaml
kubectl apply -f k8s/04-postgres-service.yaml
kubectl apply -f k8s/05-ai-worker-deployment.yaml
kubectl apply -f k8s/06-ai-worker-service.yaml
kubectl apply -f k8s/07-api-gateway-deployment.yaml
kubectl apply -f k8s/08-api-gateway-service.yaml
```

### 6. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n packing-assistant

# Expected output (after 60-90 seconds):
# NAME                           READY   STATUS    RESTARTS   AGE
# postgres-xxx                   1/1     Running   0          2m
# ai-worker-xxx                  1/1     Running   0          1m
# api-gateway-xxx                1/1     Running   0          1m

# Check services
kubectl get services -n packing-assistant

# Expected output:
# NAME          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
# postgres      ClusterIP   10.96.x.x       <none>        5432/TCP         2m
# ai-worker     ClusterIP   10.96.x.x       <none>        8081/TCP         1m
# api-gateway   NodePort    10.96.x.x       <none>        8080:30080/TCP   1m

# Check logs if issues
kubectl logs -n packing-assistant deployment/api-gateway
kubectl logs -n packing-assistant deployment/ai-worker
kubectl logs -n packing-assistant deployment/postgres
```

### 7. Access the Application

#### Option A: NodePort (Simplest)

The API Gateway is exposed via NodePort on port 30080:

```bash
# Access directly (Kind automatically maps to localhost)
curl http://localhost:30080/actuator/health

# Create session
curl -X POST http://localhost:30080/api/sessions

# Generate packing list
curl -X POST http://localhost:30080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: YOUR_TOKEN_HERE" \
  -d '{
    "destination": "Iceland",
    "durationDays": 5,
    "season": "WINTER",
    "travelType": "VACATION"
  }'
```

#### Option B: Port-Forward (Alternative)

```bash
# Forward local port 8080 to API Gateway
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080

# In another terminal, test the API
curl http://localhost:8080/actuator/health
```

#### Option C: Port-Forward for AI Worker (Testing)

```bash
# Forward local port 8081 to AI Worker
kubectl port-forward -n packing-assistant service/ai-worker 8081:8081

# Test AI Worker directly
curl http://localhost:8081/actuator/health
```

---

## Architecture in Kubernetes

```
┌─────────────────────────────────────────────────────┐
│   Kubernetes Cluster (Kind)                         │
│                                                      │
│   Namespace: packing-assistant                      │
│   ┌──────────────────────────────────────────┐     │
│   │  Secrets: app-secrets                     │     │
│   │  - postgres-db                            │     │
│   │  - postgres-user                          │     │
│   │  - postgres-password                      │     │
│   │  - openai-api-key                         │     │
│   └──────────────────────────────────────────┘     │
│                                                      │
│   ┌────────────────┐  ┌──────────────┐             │
│   │ PostgreSQL     │  │ PVC (1Gi)    │             │
│   │ Deployment     │←─│ Persistent   │             │
│   │ Port: 5432     │  │ Storage      │             │
│   └────────────────┘  └──────────────┘             │
│          ↑                                           │
│   Service: postgres (ClusterIP)                     │
│          ↑                                           │
│   ┌────────────────┐                                │
│   │ API Gateway    │                                │
│   │ Deployment     │                                │
│   │ Port: 8080     │                                │
│   │ Init: wait-for │                                │
│   │  - postgres    │                                │
│   │  - ai-worker   │                                │
│   └────────────────┘                                │
│          ↓                                           │
│   Service: api-gateway (NodePort:30080)             │
│          │                                           │
│          ↓                                           │
│   ┌────────────────┐                                │
│   │ AI Worker      │                                │
│   │ Deployment     │                                │
│   │ Port: 8081     │                                │
│   └────────────────┘                                │
│          ↓                                           │
│   Service: ai-worker (ClusterIP)                    │
│                                                      │
└─────────────────────────────────────────────────────┘
           │
           ↓
    OpenAI API
```

---

## Configuration Details

### Secrets

All sensitive data is stored in Kubernetes Secrets (Base64 encoded):

```yaml
# k8s/01-postgres-secret.yaml
data:
  postgres-db: cGFja2luZ19hc3Npc3RhbnQ=      # packing_assistant
  postgres-user: YWRtaW4=                     # admin
  postgres-password: c2VjcmV0MTIz             # secret123
  openai-api-key: <your-base64-encoded-key>
```

To update a secret:
```bash
# Encode new value
echo -n "new-password" | base64

# Edit secret
kubectl edit secret app-secrets -n packing-assistant
```

### Persistent Storage

PostgreSQL data is persisted using a PersistentVolumeClaim:

```bash
# Check PVC status
kubectl get pvc -n packing-assistant

# Check bound volume
kubectl get pv
```

Data survives pod restarts but is lost if the cluster is deleted.

### Resource Limits

All pods have resource requests and limits:

| Service | Memory Request | Memory Limit | CPU Request | CPU Limit |
|---------|---------------|--------------|-------------|-----------|
| PostgreSQL | 256Mi | 512Mi | 250m | 500m |
| AI Worker | 512Mi | 1Gi | 250m | 500m |
| API Gateway | 512Mi | 1Gi | 250m | 500m |

### Health Checks

All services have liveness and readiness probes:

- **PostgreSQL**: `pg_isready` command
- **AI Worker**: HTTP GET `/actuator/health`
- **API Gateway**: HTTP GET `/actuator/health`

---

## Common Commands

### Cluster Management

```bash
# List all Kind clusters
kind get clusters

# Delete cluster
kind delete cluster --name packing-assistant

# Recreate cluster
kind create cluster --name packing-assistant
```

### Deployment Management

```bash
# Apply all manifests
kubectl apply -f k8s/

# Delete all resources
kubectl delete -f k8s/

# Update specific deployment
kubectl apply -f k8s/07-api-gateway-deployment.yaml

# Restart deployment
kubectl rollout restart deployment/api-gateway -n packing-assistant
```

### Monitoring

```bash
# Watch pods (live updates)
kubectl get pods -n packing-assistant --watch

# Describe pod (detailed info)
kubectl describe pod <pod-name> -n packing-assistant

# Get events
kubectl get events -n packing-assistant --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n packing-assistant
kubectl top nodes
```

### Logs

```bash
# View logs
kubectl logs deployment/api-gateway -n packing-assistant

# Follow logs (tail -f)
kubectl logs -f deployment/api-gateway -n packing-assistant

# Logs from previous container (if crashed)
kubectl logs deployment/api-gateway -n packing-assistant --previous

# Logs from all replicas
kubectl logs -l app=api-gateway -n packing-assistant
```

### Debugging

```bash
# Execute command in pod
kubectl exec -it deployment/postgres -n packing-assistant -- psql -U admin -d packing_assistant

# Get shell access
kubectl exec -it deployment/api-gateway -n packing-assistant -- /bin/sh

# Port forward for debugging
kubectl port-forward -n packing-assistant deployment/api-gateway 8080:8080
```

---

## Troubleshooting

### Pods not starting

**Problem**: Pods stuck in `Pending` or `ImagePullBackOff`

**Solution**:
```bash
# Check pod status
kubectl describe pod <pod-name> -n packing-assistant

# Common issues:
# 1. Image not loaded into Kind
kind load docker-image smart-packing-assistant-api-gateway:latest --name packing-assistant

# 2. Resource constraints
kubectl describe nodes

# 3. PVC not bound
kubectl get pvc -n packing-assistant
```

### CrashLoopBackOff

**Problem**: Pods keep restarting

**Solution**:
```bash
# Check logs
kubectl logs deployment/api-gateway -n packing-assistant

# Common causes:
# 1. Database not ready → Init containers should handle this
# 2. Wrong secrets → Check secret values
kubectl get secret app-secrets -n packing-assistant -o yaml

# 3. Flyway migration errors → Check API Gateway logs
kubectl logs deployment/api-gateway -n packing-assistant | grep -i flyway
```

### Database connection errors

**Problem**: API Gateway can't connect to PostgreSQL

**Solution**:
```bash
# Check if postgres is running
kubectl get pods -n packing-assistant | grep postgres

# Test DNS resolution
kubectl exec -it deployment/api-gateway -n packing-assistant -- nslookup postgres

# Check if secrets are correct
kubectl get secret app-secrets -n packing-assistant -o jsonpath='{.data.postgres-password}' | base64 -d
```

### Service not accessible

**Problem**: Can't access API via NodePort

**Solution**:
```bash
# Check service
kubectl get svc api-gateway -n packing-assistant

# For Kind, ensure port mapping is correct
# NodePort 30080 should map to localhost:30080

# Test with port-forward instead
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080
curl http://localhost:8080/actuator/health
```

### Out of resources

**Problem**: Pods evicted or pending due to resource limits

**Solution**:
```bash
# Check node resources
kubectl top nodes

# Reduce resource limits in deployments
# Edit k8s/07-api-gateway-deployment.yaml
# Reduce memory.limits from 1Gi to 512Mi

kubectl apply -f k8s/07-api-gateway-deployment.yaml
```

---

## Testing End-to-End

### Full Test Flow

```bash
# 1. Check all pods are ready
kubectl get pods -n packing-assistant

# 2. Create session
SESSION_RESPONSE=$(curl -s -X POST http://localhost:30080/api/sessions)
echo $SESSION_RESPONSE

# Extract token (Linux/macOS)
TOKEN=$(echo $SESSION_RESPONSE | jq -r '.sessionToken')

# Extract token (Windows PowerShell)
$SESSION_RESPONSE | ConvertFrom-Json | Select-Object -ExpandProperty sessionToken

# 3. Generate packing list
curl -X POST http://localhost:30080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "Tokyo",
    "durationDays": 7,
    "season": "SPRING",
    "travelType": "VACATION"
  }'

# 4. Verify in database
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c "SELECT * FROM packing_lists ORDER BY created_at DESC LIMIT 1;"
```

---

## Production Considerations

**Current setup is for DEVELOPMENT/TESTING only**. For production:

### 1. Security
- **Use proper secrets management** (Sealed Secrets, External Secrets Operator)
- **Enable RBAC** for fine-grained access control
- **Network Policies** to restrict pod-to-pod communication
- **Pod Security Standards** (PSS) for security best practices

### 2. High Availability
- **Multiple replicas** for each service
- **HorizontalPodAutoscaler** for auto-scaling
- **PodDisruptionBudgets** to ensure availability during updates

### 3. Storage
- **Use production StorageClass** (AWS EBS, GCE PD, etc.)
- **Backup strategy** for PostgreSQL data
- **Stateful Sets** for PostgreSQL (instead of Deployment)

### 4. Networking
- **Ingress Controller** (nginx, Traefik) for external access
- **TLS/SSL certificates** (Let's Encrypt, cert-manager)
- **Load Balancer** instead of NodePort

### 5. Monitoring
- **Prometheus** for metrics
- **Grafana** for dashboards
- **Loki** for log aggregation
- **Jaeger** for distributed tracing

### 6. CI/CD
- **GitOps** (ArgoCD, Flux)
- **Automated testing** before deployment
- **Blue-Green** or **Canary** deployments

---

## Cleanup

### Delete All Resources

```bash
# Delete all resources in namespace
kubectl delete namespace packing-assistant

# OR delete manifests individually
kubectl delete -f k8s/
```

### Delete Kind Cluster

```bash
# Delete the entire cluster
kind delete cluster --name packing-assistant

# Verify deletion
kind get clusters
```

---

## Next Steps

After Kubernetes deployment works:
- **Phase 11**: Create README.md (max 220 lines, 40 points)
- **Phase 12**: Record pitch (1-3 minutes, 5 points)
- **Phase 14-15**: Prepare and submit project

---

## Additional Resources

- [Kind Documentation](https://kind.sigs.k8s.io/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
