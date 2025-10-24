# Kubernetes Deployment Guide (Docker Desktop)

This guide explains how to deploy the Smart Packing Assistant to Kubernetes using **Docker Desktop's built-in Kubernetes cluster**.

## Why Docker Desktop Kubernetes?

Docker Desktop includes a fully functional Kubernetes cluster that's **simpler to use** than Kind for local development:

✅ **Built-in** - No separate installation
✅ **Automatic image access** - No need to load Docker images manually
✅ **One-click setup** - Enable in settings and you're ready
✅ **Persistent cluster** - Survives Docker Desktop restarts
✅ **Production-grade** - Same Kubernetes as any other cluster

**Perfect for**: Local development, testing, university projects, learning Kubernetes

---

## Prerequisites

- **Docker Desktop** installed and running (version 4.0+)
- **kubectl** installed (usually comes with Docker Desktop)
- Docker images built (from Phase 8)

---

## Quick Start

### 1. Enable Kubernetes in Docker Desktop

**macOS**:
1. Click Docker icon in menu bar → **Settings** (gear icon)
2. Navigate to **Kubernetes** tab
3. Check ☑️ **Enable Kubernetes**
4. Click **Apply & Restart**
5. Wait 2-3 minutes for Kubernetes to initialize

**Windows**:
1. Right-click Docker icon in system tray → **Settings**
2. Go to **Kubernetes** tab
3. Check ☑️ **Enable Kubernetes**
4. Click **Apply & Restart**
5. Wait 2-3 minutes for initialization

**Verify Kubernetes is running**:
```bash
# Check context (should show docker-desktop)
kubectl config current-context

# Should output: docker-desktop

# Verify cluster info
kubectl cluster-info

# Should show: Kubernetes control plane is running at https://kubernetes.docker.internal:6443
```

### 2. Build Docker Images

```bash
# From project root
docker compose build

# Verify images exist
docker images | grep smart-packing
```

**Expected output**:
```
smart-packing-assistant-api-gateway    latest    abc123def456    2 minutes ago    400MB
smart-packing-assistant-ai-worker      latest    def456ghi789    2 minutes ago    380MB
```

### 3. Deploy to Kubernetes

**Option A: Deploy all at once** (simplest):
```bash
kubectl apply -f k8s/
```

**Option B: Deploy in order** (recommended for first deployment):
```bash
# 1. Create namespace
kubectl apply -f k8s/00-namespace.yaml

# 2. Create secrets
kubectl apply -f k8s/01-postgres-secret.yaml

# 3. Create persistent storage
kubectl apply -f k8s/02-postgres-pvc.yaml

# 4. Deploy PostgreSQL
kubectl apply -f k8s/03-postgres-deployment.yaml
kubectl apply -f k8s/04-postgres-service.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n packing-assistant --timeout=120s

# 5. Deploy AI Worker
kubectl apply -f k8s/05-ai-worker-deployment.yaml
kubectl apply -f k8s/06-ai-worker-service.yaml

# Wait for AI Worker to be ready
kubectl wait --for=condition=ready pod -l app=ai-worker -n packing-assistant --timeout=120s

# 6. Deploy API Gateway
kubectl apply -f k8s/07-api-gateway-deployment.yaml
kubectl apply -f k8s/08-api-gateway-service.yaml

# Wait for API Gateway to be ready
kubectl wait --for=condition=ready pod -l app=api-gateway -n packing-assistant --timeout=120s
```

### 4. Verify Deployment

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

# Check logs if needed
kubectl logs -n packing-assistant deployment/api-gateway --tail=50
kubectl logs -n packing-assistant deployment/ai-worker --tail=50
kubectl logs -n packing-assistant deployment/postgres --tail=50
```

### 5. Access the Application

The API Gateway is exposed via **NodePort on port 30080**:

```bash
# Test health endpoint
curl http://localhost:30080/actuator/health

# Expected: {"status":"UP"}

# Create a session
curl -X POST http://localhost:30080/api/sessions

# Expected: {"sessionToken":"abc123...","sessionId":"uuid-here"}
```

**Full test with packing list generation**:
```bash
# 1. Create session and save token
SESSION_RESPONSE=$(curl -s -X POST http://localhost:30080/api/sessions)
echo $SESSION_RESPONSE

# 2. Extract token (macOS/Linux with jq)
TOKEN=$(echo $SESSION_RESPONSE | jq -r '.sessionToken')

# OR extract manually (copy from output above)
TOKEN="paste-your-token-here"

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
```

---

## Key Differences from Kind Guide

| Feature | Docker Desktop | Kind |
|---------|----------------|------|
| **Installation** | Built-in (just enable) | Separate CLI tool |
| **Cluster creation** | Automatic (persistent) | Manual (`kind create cluster`) |
| **Image loading** | ❌ NOT needed (automatic) | ✅ Required (`kind load docker-image`) |
| **Context name** | `docker-desktop` | `kind-packing-assistant` |
| **NodePort access** | `localhost:30080` | `localhost:30080` |
| **Persistence** | Survives Docker restart | Deleted with cluster |
| **Multiple clusters** | One cluster only | Multiple clusters supported |

**Most important**: With Docker Desktop, you **don't need** to manually load images:
```bash
# ❌ NOT NEEDED with Docker Desktop
# kind load docker-image smart-packing-assistant-api-gateway:latest

# ✅ Images are automatically available!
```

---

## Architecture in Docker Desktop Kubernetes

```
┌───────────────────────────────────────────────────────────┐
│   Docker Desktop Kubernetes Cluster                       │
│   Context: docker-desktop                                 │
│                                                             │
│   Namespace: packing-assistant                            │
│   ┌──────────────────────────────────────────────┐       │
│   │  Secret: app-secrets                          │       │
│   │  - postgres-db: packing_assistant             │       │
│   │  - postgres-user: admin                       │       │
│   │  - postgres-password: secret123               │       │
│   │  - openai-api-key: sk-your-key                │       │
│   └──────────────────────────────────────────────┘       │
│                                                             │
│   ┌─────────────────┐  ┌──────────────────┐              │
│   │ PostgreSQL      │  │ PVC (1Gi)        │              │
│   │ Deployment      │←─│ Persistent       │              │
│   │ Port: 5432      │  │ Storage          │              │
│   │                 │  │ (Docker Desktop  │              │
│   │                 │  │  local storage)  │              │
│   └─────────────────┘  └──────────────────┘              │
│          ↑                                                 │
│   Service: postgres (ClusterIP)                           │
│          ↑                                                 │
│          │                                                 │
│   ┌─────────────────┐         ┌──────────────────┐       │
│   │ API Gateway     │────────→│ AI Worker        │       │
│   │ Deployment      │         │ Deployment       │       │
│   │ Port: 8080      │         │ Port: 8081       │       │
│   │                 │         │                  │       │
│   │ Init Containers:│         └──────────────────┘       │
│   │ - wait-for-db   │                ↓                    │
│   │ - wait-for-ai   │         Service: ai-worker         │
│   └─────────────────┘         (ClusterIP)                │
│          ↓                                                 │
│   Service: api-gateway                                    │
│   (NodePort: 30080)                                       │
│                                                             │
└───────────────────────────────────────────────────────────┘
           ↓                              ↓
    localhost:30080                  OpenAI API
    (Your browser/Postman)
```

---

## Configuration Details

### Secrets Management

All sensitive data is stored in `k8s/01-postgres-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: packing-assistant
type: Opaque
data:
  postgres-db: cGFja2luZ19hc3Npc3RhbnQ=      # packing_assistant
  postgres-user: YWRtaW4=                     # admin
  postgres-password: c2VjcmV0MTIz             # secret123
  openai-api-key: <your-base64-encoded-key>
```

**To update OpenAI API key**:
```bash
# 1. Encode your API key
echo -n "sk-your-actual-openai-key" | base64

# 2. Edit the secret file
# Replace the openai-api-key value in k8s/01-postgres-secret.yaml

# 3. Apply the updated secret
kubectl apply -f k8s/01-postgres-secret.yaml

# 4. Restart pods to pick up new secret
kubectl rollout restart deployment/ai-worker -n packing-assistant
kubectl rollout restart deployment/api-gateway -n packing-assistant
```

### Persistent Storage

PostgreSQL data is stored in a **PersistentVolumeClaim (PVC)**:

```bash
# Check PVC status
kubectl get pvc -n packing-assistant

# Expected output:
# NAME           STATUS   VOLUME                                     CAPACITY
# postgres-pvc   Bound    pvc-abc123-def4-5678-90ab-cdef12345678    1Gi

# Check bound volume
kubectl get pv
```

**Storage location**: Docker Desktop stores PVC data in its VM:
- **macOS**: `~/Library/Containers/com.docker.docker/Data/vms/0/`
- **Windows**: `C:\Users\<YourUser>\AppData\Local\Docker\wsl\data\`

**Data persistence**:
- ✅ Survives pod restarts
- ✅ Survives pod deletions
- ✅ Survives Docker Desktop restarts
- ❌ Lost if PVC is deleted
- ❌ Lost if Kubernetes is disabled in Docker Desktop

### Resource Limits

All pods have resource requests and limits to prevent resource exhaustion:

| Service | Memory Request | Memory Limit | CPU Request | CPU Limit |
|---------|---------------|--------------|-------------|-----------|
| **PostgreSQL** | 256Mi | 512Mi | 250m | 500m |
| **AI Worker** | 512Mi | 1Gi | 250m | 500m |
| **API Gateway** | 512Mi | 1Gi | 250m | 500m |
| **Total** | ~1.25Gi | ~2.5Gi | 750m | 1.5 cores |

**Docker Desktop requirements**: Allocate at least **4GB RAM** and **2 CPUs** in settings.

### Health Checks

All services have **liveness** and **readiness** probes:

**PostgreSQL**:
```yaml
livenessProbe:
  exec:
    command: ["pg_isready", "-U", "admin"]
  initialDelaySeconds: 30
  periodSeconds: 10
```

**AI Worker & API Gateway**:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

---

## Common Commands

### Deployment Management

```bash
# Apply all manifests
kubectl apply -f k8s/

# Delete all resources (keeps PVC data)
kubectl delete -f k8s/ --ignore-not-found=true

# Force delete everything including data
kubectl delete namespace packing-assistant --grace-period=0 --force

# Update specific deployment
kubectl apply -f k8s/07-api-gateway-deployment.yaml

# Restart a deployment (rolling restart)
kubectl rollout restart deployment/api-gateway -n packing-assistant

# Check rollout status
kubectl rollout status deployment/api-gateway -n packing-assistant

# Undo a rollout
kubectl rollout undo deployment/api-gateway -n packing-assistant
```

### Monitoring & Debugging

```bash
# Watch pods (live updates)
kubectl get pods -n packing-assistant --watch

# Get detailed pod info
kubectl describe pod <pod-name> -n packing-assistant

# View events (sorted by time)
kubectl get events -n packing-assistant --sort-by='.lastTimestamp'

# View logs
kubectl logs deployment/api-gateway -n packing-assistant

# Follow logs (live tail)
kubectl logs -f deployment/api-gateway -n packing-assistant

# Logs from previous container (if crashed)
kubectl logs deployment/api-gateway -n packing-assistant --previous

# Logs from all pods with label
kubectl logs -l app=api-gateway -n packing-assistant --all-containers

# Execute commands in pod
kubectl exec -it deployment/postgres -n packing-assistant -- psql -U admin -d packing_assistant

# Get shell access
kubectl exec -it deployment/api-gateway -n packing-assistant -- /bin/sh
```

### Port Forwarding (Alternative Access)

```bash
# Forward API Gateway (alternative to NodePort)
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080

# Forward AI Worker (for direct testing)
kubectl port-forward -n packing-assistant service/ai-worker 8081:8081

# Forward PostgreSQL (for direct DB access)
kubectl port-forward -n packing-assistant service/postgres 5432:5432

# Now access via localhost
curl http://localhost:8080/actuator/health
psql -h localhost -U admin -d packing_assistant
```

### Resource Monitoring

```bash
# Check resource usage (requires metrics-server)
kubectl top nodes
kubectl top pods -n packing-assistant

# Get all resources in namespace
kubectl get all -n packing-assistant

# Get resource quotas
kubectl describe namespace packing-assistant
```

---

## Troubleshooting

### 1. Kubernetes not starting in Docker Desktop

**Symptoms**:
- Kubernetes toggle keeps spinning
- "Kubernetes is starting..." for >5 minutes

**Solutions**:
```bash
# Option A: Reset Kubernetes cluster
# Docker Desktop → Settings → Kubernetes → Reset Kubernetes Cluster

# Option B: Restart Docker Desktop
# Docker Desktop → Quit Docker Desktop → Start again

# Option C: Check Docker Desktop resources
# Settings → Resources → Ensure 4GB+ RAM and 2+ CPUs allocated

# Option D: Check Docker Desktop logs
# Click bug icon in top menu bar → Troubleshoot → Get support
```

### 2. Pods stuck in "ImagePullBackOff"

**Symptoms**:
```
NAME                           READY   STATUS             RESTARTS   AGE
api-gateway-xxx                0/1     ImagePullBackOff   0          2m
```

**Cause**: Image not found or wrong image name

**Solutions**:
```bash
# 1. Check if images exist locally
docker images | grep smart-packing

# 2. Rebuild images
docker compose build

# 3. Check image name in deployment manifest
kubectl describe pod <pod-name> -n packing-assistant | grep Image:

# 4. Verify imagePullPolicy is IfNotPresent or Never (not Always)
kubectl get deployment api-gateway -n packing-assistant -o yaml | grep imagePullPolicy
```

### 3. Pods in "CrashLoopBackOff"

**Symptoms**:
```
NAME                           READY   STATUS              RESTARTS   AGE
api-gateway-xxx                0/1     CrashLoopBackOff    5          5m
```

**Cause**: Application error, usually database connection or missing secrets

**Solutions**:
```bash
# 1. Check logs
kubectl logs deployment/api-gateway -n packing-assistant --tail=100

# 2. Check if PostgreSQL is ready
kubectl get pods -n packing-assistant | grep postgres

# 3. Verify secrets exist and are correct
kubectl get secret app-secrets -n packing-assistant -o yaml

# Decode secret to verify
kubectl get secret app-secrets -n packing-assistant -o jsonpath='{.data.postgres-password}' | base64 -d

# 4. Check init containers ran successfully
kubectl describe pod <pod-name> -n packing-assistant | grep -A 10 "Init Containers"

# 5. Test database connectivity from within cluster
kubectl run -it --rm debug --image=postgres:15-alpine --restart=Never -n packing-assistant -- \
  psql -h postgres -U admin -d packing_assistant
```

### 4. Can't access API via NodePort

**Symptoms**:
```bash
curl http://localhost:30080/actuator/health
# curl: (7) Failed to connect to localhost port 30080: Connection refused
```

**Solutions**:
```bash
# 1. Check if pod is running
kubectl get pods -n packing-assistant

# 2. Check service exists
kubectl get svc api-gateway -n packing-assistant

# 3. Verify NodePort is 30080
kubectl get svc api-gateway -n packing-assistant -o yaml | grep nodePort

# 4. Test with port-forward instead
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080
curl http://localhost:8080/actuator/health

# 5. Check firewall (macOS)
# System Preferences → Security & Privacy → Firewall → Allow incoming connections

# 6. Restart Docker Desktop
```

### 5. Database connection errors

**Symptoms**: API Gateway logs show:
```
Connection to postgres:5432 refused
Cannot connect to database
```

**Solutions**:
```bash
# 1. Check if postgres pod is running
kubectl get pods -n packing-assistant | grep postgres

# 2. Test DNS resolution from API Gateway pod
kubectl exec -it deployment/api-gateway -n packing-assistant -- nslookup postgres

# Should resolve to: postgres.packing-assistant.svc.cluster.local

# 3. Check postgres service
kubectl get svc postgres -n packing-assistant

# 4. Test direct connection
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c "SELECT 1;"

# 5. Check secrets are mounted correctly
kubectl exec -it deployment/api-gateway -n packing-assistant -- env | grep POSTGRES
```

### 6. "Out of memory" errors

**Symptoms**:
- Pods get evicted
- OOMKilled status

**Solutions**:
```bash
# 1. Increase Docker Desktop memory
# Settings → Resources → Memory → Set to 6GB or 8GB

# 2. Reduce resource limits in deployments
# Edit k8s/*-deployment.yaml files
# Change memory limits from 1Gi to 512Mi

# 3. Check current resource usage
kubectl top pods -n packing-assistant

# 4. Restart Docker Desktop after changing memory
```

### 7. Flyway migration errors

**Symptoms**: API Gateway logs show:
```
Flyway migration failed
Schema validation failed
```

**Solutions**:
```bash
# 1. Check if database exists
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -l

# 2. Manually create database (if missing)
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -c "CREATE DATABASE packing_assistant;"

# 3. Check Flyway is enabled
kubectl get deployment api-gateway -n packing-assistant -o yaml | grep FLYWAY

# Should show: SPRING_FLYWAY_ENABLED: "true"

# 4. View migration history
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c "SELECT * FROM flyway_schema_history;"

# 5. Restart API Gateway after fixing
kubectl rollout restart deployment/api-gateway -n packing-assistant
```

---

## End-to-End Testing

### Complete Test Flow

```bash
# Step 1: Verify all pods are ready
kubectl get pods -n packing-assistant

# All should show: READY 1/1, STATUS Running

# Step 2: Test health endpoints
curl http://localhost:30080/actuator/health
# Expected: {"status":"UP"}

curl http://localhost:30080/api/packing/health
# Expected: {"status":"Packing API is running","timestamp":"..."}

# Step 3: Create a session
SESSION_RESPONSE=$(curl -s -X POST http://localhost:30080/api/sessions)
echo $SESSION_RESPONSE

# Expected: {"sessionToken":"abc123...","sessionId":"uuid-here"}

# Step 4: Extract session token
# macOS/Linux with jq:
TOKEN=$(echo $SESSION_RESPONSE | jq -r '.sessionToken')
echo "Token: $TOKEN"

# OR manually copy token from output

# Step 5: Generate packing list (Tokyo example)
curl -X POST http://localhost:30080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "Tokyo",
    "durationDays": 7,
    "season": "SPRING",
    "travelType": "VACATION"
  }' | jq '.'

# Expected: JSON with 5 categories (essentials, clothing, toiletries, electronics, extras)

# Step 6: Verify data in database
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c \
  "SELECT id, destination, duration_days, travel_type, created_at FROM packing_lists ORDER BY created_at DESC LIMIT 1;"

# Step 7: Test error handling (invalid token)
curl -X POST http://localhost:30080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: invalid-token-123" \
  -d '{
    "destination": "Paris",
    "durationDays": 3,
    "season": "SUMMER",
    "travelType": "BUSINESS"
  }'

# Expected: {"error":"Invalid or expired session","status":401}

# Step 8: Generate another list with same session
curl -X POST http://localhost:30080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "Iceland",
    "durationDays": 5,
    "season": "WINTER",
    "travelType": "BACKPACKING"
  }' | jq '.'

# Should work with same token!
```

### Performance Testing

```bash
# Test response time (requires Apache Bench or similar)
ab -n 10 -c 2 \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -p request.json \
  http://localhost:30080/api/packing/generate

# request.json contents:
echo '{
  "destination": "Berlin",
  "durationDays": 4,
  "season": "AUTUMN",
  "travelType": "VACATION"
}' > request.json
```

---

## Docker Desktop Settings Recommendations

### Minimum Requirements
- **Memory**: 4 GB (6-8 GB recommended)
- **CPUs**: 2 cores (4 recommended)
- **Disk**: 20 GB free space

### How to Adjust (macOS)
1. Docker Desktop → Settings → Resources
2. Set **Memory** to 6 GB or 8 GB
3. Set **CPUs** to 4
4. Set **Disk image size** to 60 GB
5. Click **Apply & Restart**

### How to Adjust (Windows)
1. Docker Desktop → Settings → Resources → WSL Integration
2. **Memory**: 6 GB
3. **Processors**: 4
4. **Disk image size**: 60 GB
5. Click **Apply & Restart**

---

## Cleanup

### Option 1: Delete All Resources (Keep Cluster)

```bash
# Delete namespace (removes all resources)
kubectl delete namespace packing-assistant

# Verify deletion
kubectl get namespaces
```

### Option 2: Reset Kubernetes (Nuclear Option)

```bash
# Via Docker Desktop UI:
# Settings → Kubernetes → Reset Kubernetes Cluster → Reset

# This deletes:
# - All deployments
# - All services
# - All persistent volumes
# - All namespaces (except kube-system)
```

### Option 3: Disable Kubernetes

```bash
# Via Docker Desktop UI:
# Settings → Kubernetes → Uncheck "Enable Kubernetes" → Apply

# This stops Kubernetes but preserves cluster state
# Re-enabling will restore everything
```

---

## Production Considerations

**Current setup is for DEVELOPMENT/TESTING only**. For production deployment:

### 1. Security Hardening
- **Real secrets management**: Use Sealed Secrets, External Secrets Operator, or cloud provider secrets
- **RBAC**: Enable Role-Based Access Control with least privilege
- **Network Policies**: Restrict pod-to-pod communication
- **Pod Security Standards**: Enforce security best practices
- **Image scanning**: Scan for vulnerabilities (Trivy, Snyk)

### 2. High Availability
- **Multiple replicas**: 3+ replicas per service
- **HorizontalPodAutoscaler**: Auto-scale based on CPU/memory
- **PodDisruptionBudgets**: Ensure minimum availability during updates
- **Affinity/Anti-affinity**: Spread pods across nodes

### 3. Persistent Storage
- **Production StorageClass**: Use cloud provider storage (AWS EBS, Azure Disk, GCP PD)
- **Backup strategy**: Automated PostgreSQL backups (Velero, cloud-native backups)
- **StatefulSets**: Use for PostgreSQL instead of Deployment
- **Volume snapshots**: Enable for disaster recovery

### 4. Networking & Ingress
- **Ingress Controller**: Install nginx or Traefik
- **TLS/SSL**: Use cert-manager with Let's Encrypt
- **Load Balancer**: Use cloud load balancer (not NodePort)
- **Domain names**: Configure DNS with proper domain

### 5. Monitoring & Observability
- **Metrics**: Prometheus + Grafana
- **Logging**: Loki or ELK stack
- **Tracing**: Jaeger or Zipkin
- **Alerts**: Alertmanager for critical issues

### 6. CI/CD Integration
- **GitOps**: ArgoCD or Flux for declarative deployments
- **Automated testing**: Run tests before deployment
- **Deployment strategies**: Blue-green or canary deployments
- **Rollback automation**: Automatic rollback on failures

---

## Comparison: Docker Desktop vs Production Kubernetes

| Feature | Docker Desktop | Production (EKS/GKE/AKS) |
|---------|----------------|--------------------------|
| **Purpose** | Development/Testing | Production workloads |
| **Setup** | One-click enable | Managed service + config |
| **Cluster size** | Single-node | Multi-node (3+ nodes) |
| **High availability** | ❌ No | ✅ Yes |
| **Auto-scaling** | ❌ No | ✅ Yes |
| **Load balancer** | NodePort only | Cloud LB |
| **Persistent storage** | Local VM disk | Cloud volumes (EBS, etc.) |
| **Monitoring** | Manual (kubectl) | Built-in (CloudWatch, etc.) |
| **Cost** | Free | $$$$ |
| **Best for** | Learning, development | Production apps |

---

## Next Steps

After successfully deploying to Docker Desktop Kubernetes:

1. ✅ **Kubernetes deployment works** (10 points achieved!)
2. 📝 **Create README.md** (max 220 lines, 40 points)
3. 🎤 **Record pitch video** (1-3 minutes, 5 points)
4. 📦 **Prepare final submission**
5. 🚀 **Submit before deadline**: November 22, 2025, 23:59:59

---

## Additional Resources

- [Docker Desktop Documentation](https://docs.docker.com/desktop/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

---

## FAQ

### Q: Can I use this for the university project submission?
**A:** Yes! Docker Desktop Kubernetes is a real Kubernetes cluster. The grading criteria just require "Kubernetes deployment" - it doesn't specify which cluster type. Docker Desktop is perfectly acceptable.

### Q: What if I have limited RAM/CPU on my laptop?
**A:** You can reduce resource limits in the deployment manifests. Try 256Mi memory and 100m CPU for each service.

### Q: Can I switch between Docker Desktop and Kind?
**A:** Yes! Your manifests work with both. Just switch contexts:
```bash
kubectl config use-context docker-desktop  # For Docker Desktop
kubectl config use-context kind-packing-assistant  # For Kind
```

### Q: How do I completely uninstall everything?
**A:**
1. Delete namespace: `kubectl delete namespace packing-assistant`
2. Disable Kubernetes in Docker Desktop settings
3. Delete Docker images: `docker rmi smart-packing-assistant-api-gateway smart-packing-assistant-ai-worker`

### Q: Is Docker Desktop Kubernetes production-ready?
**A:** Docker Desktop itself is for development only. However, it runs the same Kubernetes as production clusters (just single-node). For production, use managed Kubernetes services (EKS, GKE, AKS) or self-hosted multi-node clusters.

---

**Good luck with your project! 🚀**
