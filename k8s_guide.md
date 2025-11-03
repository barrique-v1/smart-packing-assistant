# Kubernetes Deployment Guide (Docker Desktop)

This guide explains how to deploy the **Smart Packing Assistant with RAG (Retrieval-Augmented Generation)** to Kubernetes using **Docker Desktop's built-in Kubernetes cluster**.

## What You'll Deploy

The complete Smart Packing Assistant application includes:
- **API Gateway**: REST API with session management and PostgreSQL persistence
- **AI Worker**: OpenAI GPT-4 integration with RAG-enhanced recommendations
- **PostgreSQL**: Database for sessions and packing lists
- **Qdrant**: Vector database for RAG knowledge base (140 expert-curated packing items)

**RAG Enhancement**: The AI Worker retrieves semantically similar packing items from a curated knowledge base before generating recommendations, resulting in more accurate, expert-verified suggestions.

---

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
- **OpenAI API Key** (for AI Worker)
- **Python 3.9+** (for embedding generation)
- **4GB+ RAM** and **2+ CPUs** allocated to Docker Desktop

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
# Output: docker-desktop

# Verify cluster info
kubectl cluster-info
# Output: Kubernetes control plane is running at https://kubernetes.docker.internal:6443
```

### 2. Set Up OpenAI API Key

```bash
# Update the secret file with your API key
echo -n "sk-your-actual-openai-key" | base64

# Copy the output and update k8s/01-postgres-secret.yaml
# Replace the placeholder openai-api-key value
```

### 3. Build Docker Images

```bash
# From project root
docker compose build
```

```bash
# Verify images exist
docker images | grep smart-packing
```

**Expected output**:
```
smart-packing-assistant-api-gateway    latest    abc123    2 minutes ago    400MB
smart-packing-assistant-ai-worker      latest    def456    2 minutes ago    380MB
smart-packing-assistant-qdrant         latest    ghi789    2 minutes ago    150MB
```

### 4. Generate RAG Knowledge Base Embeddings

**Install Python dependencies**:
```bash
pip3 install openai tqdm
```

**Set environment variable**:
```bash
export OPENAI_API_KEY=sk-your-actual-openai-key
```

**Generate embeddings** (creates vector representations of 140 packing items):
```bash
python3 scripts/generate-embeddings.py
```

**Expected output**:
```
======================================================================
  Smart Packing Assistant - Embedding Generation
======================================================================
✅ OpenAI API key found (starts with 'sk-proj-NG...')
📖 Loading knowledge base from data/packing-knowledge.csv...
✅ Loaded 140 items from knowledge base

🤖 Generating embeddings for 140 items...
   Model: text-embedding-3-small (1536 dimensions)

✅ Generated 140 embeddings successfully
💾 Saving embeddings to data/packing-embeddings.json...
✅ Saved 140 points to data/packing-embeddings.json
   File size: 6.28 MB
```

This step is **required** for RAG functionality. The embeddings file will be imported to Qdrant later.

### 5. Deploy to Kubernetes

**Deploy all services in order**:

```bash
# 1. Create namespace and secrets
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-postgres-secret.yaml

# 2. Deploy PostgreSQL (database for sessions/lists)
kubectl apply -f k8s/02-postgres-pvc.yaml
kubectl apply -f k8s/03-postgres-deployment.yaml
kubectl apply -f k8s/04-postgres-service.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n packing-assistant --timeout=120s

# 3. Deploy Qdrant (vector database for RAG)
kubectl apply -f k8s/11-qdrant-pvc.yaml
kubectl apply -f k8s/12-qdrant-deployment.yaml
kubectl apply -f k8s/13-qdrant-service.yaml

# Wait for Qdrant to be ready
kubectl wait --for=condition=ready pod -l app=qdrant -n packing-assistant --timeout=120s

# 4. Deploy AI Worker (GPT-4 + RAG)
kubectl apply -f k8s/05-ai-worker-deployment.yaml
kubectl apply -f k8s/06-ai-worker-service.yaml

# Wait for AI Worker to be ready
kubectl wait --for=condition=ready pod -l app=ai-worker -n packing-assistant --timeout=120s

# 5. Deploy API Gateway (REST API)
kubectl apply -f k8s/07-api-gateway-deployment.yaml
kubectl apply -f k8s/08-api-gateway-service.yaml

# Wait for API Gateway to be ready
kubectl wait --for=condition=ready pod -l app=api-gateway -n packing-assistant --timeout=120s

# 6. Deploy Frontend
kubectl apply -f k8s/09-frontend-deployment.yaml
kubectl apply -f k8s/10-frontend-service.yaml

# Wait for Frontend to be ready
kubectl wait --for=condition=ready pod -l app=frontend -n packing-assistant --timeout=120s
```

### 6. Import RAG Knowledge Base to Qdrant

**Install Python dependency**:
```bash
pip3 install requests
```

**Set up port forwarding** (in a new terminal window):
```bash
kubectl port-forward -n packing-assistant service/qdrant 6333:6333
```

**Import embeddings** (in another terminal):
```bash
python3 scripts/import-to-qdrant.py --recreate
```

**Expected output**:
```
======================================================================
  Smart Packing Assistant - Qdrant Import
======================================================================
📖 Loading embeddings from data/packing-embeddings.json...
✅ Loaded 140 points from file
   Model: text-embedding-3-small
   Dimensions: 1536

🔍 Checking Qdrant connection at http://localhost:6333...
✅ Qdrant is healthy and accessible

🔧 Creating collection 'packing_knowledge'...
✅ Collection 'packing_knowledge' created successfully
   Vector size: 1536
   Distance metric: Cosine

📤 Uploading 140 points to Qdrant...
✅ Successfully uploaded 140 points

✔️  Verifying import...
   Points in collection: 140
   Vectors in collection: 140
✅ Import verified successfully - all 140 points imported

🔍 Testing search with sample query...
   Query item: Passport
   Top 5 results:
      1. Passport (documents) - score: 1.0000
      2. Visa Documentation (documents) - score: 0.8316
      3. Travel Insurance Documents (documents) - score: 0.7739
```

**Close the port-forward** (Ctrl+C in the terminal running port-forward).

### 7. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n packing-assistant

# Expected output (after 60-90 seconds):
# NAME                           READY   STATUS    RESTARTS   AGE
# postgres-xxx                   1/1     Running   0          3m
# qdrant-xxx                     1/1     Running   0          2m
# ai-worker-xxx                  1/1     Running   0          2m
# api-gateway-xxx                1/1     Running   0          1m
# frontend-xxx                   1/1     Running   0          1m
```

```bash
# Check services
kubectl get services -n packing-assistant

# Expected output:
# NAME          TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
# postgres      ClusterIP   10.96.x.x       <none>        5432/TCP         3m
# qdrant        ClusterIP   10.96.x.x       <none>        6333/TCP         2m
# ai-worker     ClusterIP   10.96.x.x       <none>        8081/TCP         2m
# api-gateway   NodePort    10.96.x.x       <none>        8080:30080/TCP   1m
# frontend      NodePort    10.96.x.x       <none>        80:30081/TCP     1m
```

### 8. Access and Test the Application

**Set up port forwarding** (macOS):
```bash
# Frontend (when you want to access the UI)
kubectl port-forward -n packing-assistant service/frontend 5173:80
```

```bash
# API Gateway (for testing API endpoints)
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080
```

**Test health endpoint**:
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**Create a session**:
```bash
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sessions)
echo $SESSION_RESPONSE

# Expected: {"sessionToken":"abc123...","sessionId":"uuid-here"}

# Extract token (macOS/Linux with jq)
TOKEN=$(echo $SESSION_RESPONSE | jq -r '.sessionToken')
echo "Token: $TOKEN"

# OR copy token manually from output
```

**Generate packing list with RAG** (Tokyo Summer Vacation):
```bash
curl -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "Tokyo",
    "durationDays": 7,
    "season": "SUMMER",
    "travelType": "VACATION"
  }' | jq '.'
```

**Verify RAG is working** - Check AI Worker logs:
```bash
kubectl logs -n packing-assistant deployment/ai-worker --tail=30 | grep "Retrieved items"
```

**Test Business Trip** (New York Winter Business):
```bash
curl -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "New York",
    "durationDays": 3,
    "season": "WINTER",
    "travelType": "BUSINESS"
  }' | jq '.'
```

**Expected**: Business-specific items like:
- Business Suit, Dress Shirts, Formal Shoes
- Laptop, Briefcase, Business Cards
- Winter Coat, Thermal Underwear, Gloves

---

## Architecture in Kubernetes

```
┌───────────────────────────────────────────────────────────────┐
│   Docker Desktop Kubernetes Cluster                           │
│   Context: docker-desktop                                     │
│                                                                │
│   Namespace: packing-assistant                                │
│   ┌────────────────────────────────────────────────┐         │
│   │  Secret: app-secrets                           │         │
│   │  - postgres-db, postgres-user, postgres-pass   │         │
│   │  - openai-api-key (for AI Worker)              │         │
│   └────────────────────────────────────────────────┘         │
│                                                                │
│   ┌─────────────────┐  ┌──────────────────┐                 │
│   │ PostgreSQL      │  │ PVC (1Gi)        │                 │
│   │ Deployment      │←─│ Persistent       │                 │
│   │ Port: 5432      │  │ Storage          │                 │
│   └─────────────────┘  └──────────────────┘                 │
│          ↑                                                     │
│   Service: postgres (ClusterIP)                               │
│          ↑                                                     │
│   ┌──────────────────────────────────────────┐               │
│   │ API Gateway Deployment                   │               │
│   │ Port: 8080                               │               │
│   │ - Session management                     │               │
│   │ - REST API endpoints                     │               │
│   │ - Database persistence                   │               │
│   └──────────────────────────────────────────┘               │
│          ↓                          ↓                         │
│   Service: api-gateway      HTTP → AI Worker                 │
│   (NodePort: 30080)                 Deployment               │
│                                     Port: 8081               │
│                                     - GPT-4 integration      │
│                                     - RAG pipeline           │
│                                            ↓                  │
│                                     Service: qdrant          │
│                                     (ClusterIP)              │
│                                            ↓                  │
│                              ┌─────────────────────┐         │
│                              │ Qdrant Deployment   │         │
│                              │ Port: 6333          │         │
│                              │ - Vector search     │         │
│                              │ - 140 embeddings    │         │
│                              └─────────────────────┘         │
│                                     ↓                         │
│                              ┌──────────────────┐            │
│                              │ PVC (500Mi)      │            │
│                              │ Vector storage   │            │
│                              └──────────────────┘            │
│                                                                │
└───────────────────────────────────────────────────────────────┘
           ↓                              ↓
    localhost:8080                  OpenAI API
    (via port-forward)              (GPT-4 + embeddings)
```

---

## RAG System Architecture

The RAG (Retrieval-Augmented Generation) pipeline enhances AI responses:

**5-Stage RAG Pipeline**:
1. **Vector Search**: Converts user query → 1536-dim embedding → searches Qdrant with filters
2. **Contextual Data**: Fetches weather info + culture tips
3. **Enhanced Prompt**: Combines retrieved items (up to 20) + context → sends to GPT-4
4. **GPT Generation**: Creates packing list grounded in expert knowledge
5. **Response Validation**: Validates and returns structured JSON

**Knowledge Base**:
- **140 expert-curated items** covering:
  - Universal essentials (passport, chargers, hygiene)
  - Business travel (suits, laptop, business cards)
  - Beach/tropical (swimsuit, sunscreen, snorkel gear)
  - Mountain/adventure (hiking boots, camping gear)
  - Arctic/cold weather (thermal layers, winter gear)

**Performance Metrics**:
- Vector Search: 400-1200ms
- GPT Generation: 9-12 seconds
- Total Response Time: ~10-13 seconds
- Items Retrieved per query: 20 (top-K limit)
- Similarity Threshold: 0.40 (scores typically 0.40-0.65)

---

## Common Commands

### Update Kubernetes Pods

```bash
  docker compose build && kubectl rollout restart deployment --all -n
  packing-assistant
```


### Deployment Management

```bash
# Apply all manifests at once
kubectl apply -f k8s/

# Delete all resources (keeps PVC data)
kubectl delete -f k8s/ --ignore-not-found=true

# Update specific deployment
kubectl apply -f k8s/07-api-gateway-deployment.yaml

# Restart a deployment (rolling restart)
kubectl rollout restart deployment/api-gateway -n packing-assistant
kubectl rollout restart deployment/ai-worker -n packing-assistant

# Check rollout status
kubectl rollout status deployment/api-gateway -n packing-assistant

# Scale deployment
kubectl scale deployment/ai-worker --replicas=2 -n packing-assistant
```

### Monitoring & Debugging

```bash
# Watch pods (live updates)
kubectl get pods -n packing-assistant --watch

# Get detailed pod info
kubectl describe pod <pod-name> -n packing-assistant

# View logs
kubectl logs deployment/api-gateway -n packing-assistant --tail=50
kubectl logs deployment/ai-worker -n packing-assistant --tail=50
kubectl logs deployment/qdrant -n packing-assistant --tail=50

# Follow logs (live tail)
kubectl logs -f deployment/ai-worker -n packing-assistant

# Execute commands in pod
kubectl exec -it deployment/postgres -n packing-assistant -- psql -U admin -d packing_assistant
kubectl exec -it deployment/qdrant -n packing-assistant -- /bin/sh
```

### Port Forwarding

```bash
# Frontend (access UI)
kubectl port-forward -n packing-assistant service/frontend 5173:5173

# API Gateway (required for testing)
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080

# AI Worker (for direct testing)
kubectl port-forward -n packing-assistant service/ai-worker 8081:8081

# Qdrant (for knowledge base management)
kubectl port-forward -n packing-assistant service/qdrant 6333:6333

# PostgreSQL (for database access)
kubectl port-forward -n packing-assistant service/postgres 5432:5432
```

### RAG-Specific Commands

```bash
# Check Qdrant collection status
kubectl port-forward -n packing-assistant service/qdrant 6333:6333 &
curl http://localhost:6333/collections/packing_knowledge | jq '.'

# Verify embeddings count
curl http://localhost:6333/collections/packing_knowledge | jq '.result.points_count'
# Expected: 140

# Test vector search directly
python3 << 'EOF'
import requests
response = requests.get('http://localhost:6333/collections/packing_knowledge')
print(f"Status: {response.json()['result']['status']}")
print(f"Vectors: {response.json()['result']['vectors_count']}")
EOF

# Check AI Worker can connect to Qdrant
kubectl exec -it deployment/ai-worker -n packing-assistant -- wget -qO- http://qdrant:6333/collections/packing_knowledge

# Reimport knowledge base (if needed)
kubectl port-forward -n packing-assistant service/qdrant 6333:6333
python3 scripts/import-to-qdrant.py --recreate
```

---

## Troubleshooting

### 1. RAG Not Working (0 Retrieved Items)

**Symptoms**: AI Worker logs show:
```
Retrieved items: 0
⚠️ Vector search returned no items - falling back to pure GPT
```

**Solutions**:

```bash
# 1. Verify Qdrant has embeddings
kubectl port-forward -n packing-assistant service/qdrant 6333:6333
curl http://localhost:6333/collections/packing_knowledge | jq '.result.points_count'
# Should show: 140

# 2. Check if collection exists
curl http://localhost:6333/collections | jq '.result.collections'
# Should show: [{"name": "packing_knowledge"}]

# 3. Reimport embeddings if missing
python3 scripts/import-to-qdrant.py --recreate

# 4. Restart AI Worker after reimport
kubectl rollout restart deployment/ai-worker -n packing-assistant

# 5. Check AI Worker logs for vector search
kubectl logs deployment/ai-worker -n packing-assistant --tail=50 | grep "Vector search"
# Should show: "✓ Vector search completed in XXms - found 20 items"
```

### 2. Qdrant Pod Not Starting

**Symptoms**:
```
NAME                  READY   STATUS             RESTARTS   AGE
qdrant-xxx            0/1     CrashLoopBackOff   3          2m
```

**Solutions**:

```bash
# 1. Check Qdrant logs
kubectl logs deployment/qdrant -n packing-assistant

# 2. Verify PVC is bound
kubectl get pvc -n packing-assistant | grep qdrant
# Status should be: Bound

# 3. Check storage class
kubectl get pvc qdrant-pvc -n packing-assistant -o yaml | grep storageClassName

# 4. Recreate PVC if needed
kubectl delete pvc qdrant-pvc -n packing-assistant
kubectl apply -f k8s/11-qdrant-pvc.yaml

# 5. Restart Qdrant
kubectl rollout restart deployment/qdrant -n packing-assistant
```

### 3. Embeddings Generation Failed

**Symptoms**:
```
❌ Error: OPENAI_API_KEY environment variable not set
```

**Solutions**:

```bash
# 1. Check API key is set
echo $OPENAI_API_KEY
# Should show: sk-proj-...

# 2. Export API key
export OPENAI_API_KEY=sk-your-actual-key

# 3. Or read from .env file
export OPENAI_API_KEY=$(grep OPENAI_API_KEY .env | cut -d '=' -f2)

# 4. Regenerate embeddings
python3 scripts/generate-embeddings.py

# 5. Verify output file exists
ls -lh data/packing-embeddings.json
# Should show: ~6.3 MB file
```

### 4. AI Worker Can't Connect to Qdrant

**Symptoms**: AI Worker logs show:
```
Vector search failed: Connection refused to qdrant:6333
```

**Solutions**:

```bash
# 1. Check Qdrant service exists
kubectl get svc qdrant -n packing-assistant

# 2. Test DNS resolution from AI Worker
kubectl exec -it deployment/ai-worker -n packing-assistant -- nslookup qdrant
# Should resolve to: qdrant.packing-assistant.svc.cluster.local

# 3. Check Qdrant is accessible within cluster
kubectl run test --image=curlimages/curl --rm -it --restart=Never -n packing-assistant -- \
  curl -s http://qdrant:6333/collections

# 4. Verify Qdrant pod is running
kubectl get pods -n packing-assistant | grep qdrant
# Status should be: Running

# 5. Check Qdrant port
kubectl get svc qdrant -n packing-assistant -o yaml | grep port
# Should show: port: 6333
```

### 5. Import Script Fails

**Symptoms**:
```
Cannot connect to Qdrant: Connection refused
```

**Solutions**:

```bash
# 1. Ensure port-forward is running
kubectl port-forward -n packing-assistant service/qdrant 6333:6333

# 2. Test connection
curl http://localhost:6333/healthz
# Expected: OK

# 3. Check if another process is using port 6333
lsof -i :6333

# 4. Use different port for port-forward if needed
kubectl port-forward -n packing-assistant service/qdrant 6334:6333
python3 scripts/import-to-qdrant.py --qdrant-url http://localhost:6334

# 5. Verify embeddings file exists
ls -lh data/packing-embeddings.json
```

### 6. OpenAI API Key Not Working

**Symptoms**: AI Worker logs show:
```
401 Unauthorized: Incorrect API key provided
```

**Solutions**:

```bash
# 1. Verify API key in secret
kubectl get secret app-secrets -n packing-assistant -o jsonpath='{.data.openai-api-key}' | base64 -d
# Should show: sk-proj-...

# 2. Update secret with correct key
echo -n "sk-your-actual-key" | base64
# Copy output and update k8s/01-postgres-secret.yaml

# 3. Apply updated secret
kubectl apply -f k8s/01-postgres-secret.yaml

# 4. Restart AI Worker to pick up new secret
kubectl rollout restart deployment/ai-worker -n packing-assistant

# 5. Verify AI Worker logs
kubectl logs deployment/ai-worker -n packing-assistant --tail=20 | grep "OpenAI"
# Should show: "✓ OpenAI API key configured"
```

### 7. Database Connection Errors

**Symptoms**: API Gateway logs show:
```
Connection to postgres:5432 refused
```

**Solutions**:

```bash
# 1. Check PostgreSQL pod is running
kubectl get pods -n packing-assistant | grep postgres

# 2. Test PostgreSQL from within cluster
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c "SELECT 1;"

# 3. Check PostgreSQL service
kubectl get svc postgres -n packing-assistant

# 4. Verify secrets are correct
kubectl get secret app-secrets -n packing-assistant -o jsonpath='{.data.postgres-password}' | base64 -d

# 5. Check API Gateway environment variables
kubectl exec -it deployment/api-gateway -n packing-assistant -- env | grep POSTGRES
```

---

## End-to-End Testing with RAG

### Complete Test Flow

```bash
# Step 1: Verify all pods are ready
kubectl get pods -n packing-assistant

# Expected: All pods Running with READY 1/1
# postgres-xxx      1/1  Running  0  5m
# qdrant-xxx        1/1  Running  0  4m
# ai-worker-xxx     1/1  Running  0  3m
# api-gateway-xxx   1/1  Running  0  2m

# Step 2: Verify Qdrant has knowledge base
kubectl port-forward -n packing-assistant service/qdrant 6333:6333 &
sleep 2
curl -s http://localhost:6333/collections/packing_knowledge | jq '.result.points_count'
# Expected: 140

# Step 3: Set up API Gateway port-forward
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080 &
sleep 2

# Step 4: Test health endpoints
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Step 5: Create session
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sessions)
echo $SESSION_RESPONSE
TOKEN=$(echo $SESSION_RESPONSE | jq -r '.sessionToken')
echo "Session Token: $TOKEN"

# Step 6: Generate Tokyo vacation packing list (RAG test)
curl -s -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "Tokyo",
    "durationDays": 7,
    "season": "SUMMER",
    "travelType": "VACATION"
  }' | jq '.categories | keys'

# Expected: ["clothing", "documents", "hygiene", "other", "tech"]

# Step 7: Verify RAG retrieval in logs
kubectl logs deployment/ai-worker -n packing-assistant --tail=30 | grep "Retrieved items"
# Expected: "Retrieved items: 20, Weather: true, Culture tips: 3"

# Step 8: Test business trip (different RAG results)
curl -s -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $TOKEN" \
  -d '{
    "destination": "New York",
    "durationDays": 3,
    "season": "WINTER",
    "travelType": "BUSINESS"
  }' | jq '.categories.clothing[:3]'

# Expected: Business items like suits, dress shirts, formal shoes

# Step 9: Verify database persistence
kubectl exec -it deployment/postgres -n packing-assistant -- \
  psql -U admin -d packing_assistant -c \
  "SELECT destination, travel_type FROM packing_lists ORDER BY created_at DESC LIMIT 2;"

# Expected: Tokyo (VACATION) and New York (BUSINESS)

# Step 10: Cleanup port-forwards
pkill -f "port-forward.*packing-assistant"
```

### RAG Performance Testing

```bash
# Test vector search performance
kubectl port-forward -n packing-assistant service/qdrant 6333:6333 &

python3 << 'EOF'
import requests
import time
from openai import OpenAI
import os

client = OpenAI(api_key=os.environ['OPENAI_API_KEY'])

# Generate test query embedding
start = time.time()
response = client.embeddings.create(
    model="text-embedding-3-small",
    input=["Packing for beach vacation"],
    dimensions=1536
)
embedding_time = time.time() - start
print(f"Embedding generation: {embedding_time:.3f}s")

# Test vector search
start = time.time()
search_result = requests.post(
    'http://localhost:6333/collections/packing_knowledge/points/search',
    json={
        "vector": response.data[0].embedding,
        "limit": 20,
        "score_threshold": 0.40
    }
).json()
search_time = time.time() - start
print(f"Vector search: {search_time:.3f}s")
print(f"Items found: {len(search_result.get('result', []))}")
print(f"Top score: {search_result['result'][0]['score']:.3f}")
EOF

pkill -f "port-forward.*packing-assistant"
```

---

## Resource Requirements

### Minimum Requirements
- **Memory**: 4 GB (6-8 GB recommended)
- **CPUs**: 2 cores (4 recommended)
- **Disk**: 20 GB free space

### Per-Service Resource Limits

| Service | Memory Request | Memory Limit | CPU Request | CPU Limit |
|---------|---------------|--------------|-------------|-----------|
| **PostgreSQL** | 256Mi | 512Mi | 250m | 500m |
| **Qdrant** | 256Mi | 512Mi | 200m | 500m |
| **AI Worker** | 512Mi | 1Gi | 250m | 500m |
| **API Gateway** | 512Mi | 1Gi | 250m | 500m |
| **Total** | ~1.5Gi | ~3Gi | 950m | 2 cores |

### Docker Desktop Settings (macOS)
1. Docker Desktop → Settings → Resources
2. Set **Memory** to 6 GB or 8 GB
3. Set **CPUs** to 4
4. Set **Disk image size** to 60 GB
5. Click **Apply & Restart**

---

## Cleanup

Delete All Resources (Keep Cluster)

```bash
# Delete namespace (removes all resources)
kubectl delete namespace packing-assistant

# Verify deletion
kubectl get namespaces
```