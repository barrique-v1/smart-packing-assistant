# Kubernetes Update Guide

## Service in Kubernetes aktualisieren

### Quick Start

**Änderung deployen (schnell):**
```bash
# 1. Code ändern, dann Image neu bauen
docker build -t smart-packing-assistant-ai-worker:latest services/ai-worker/

# 2. Deployment neu starten
kubectl rollout restart deployment/ai-worker -n packing-assistant

# 3. Status prüfen
kubectl get pods -n packing-assistant
```

---

## Update-Strategien

### Option 1: Rollout Restart (Einfach)

**Verwendung**: Wenn Image-Tag `latest` bleibt

```bash
# Service neu bauen
docker build -t smart-packing-assistant-api-gateway:latest services/api-gateway/

# Pod neu starten (holt neues Image)
kubectl rollout restart deployment/api-gateway -n packing-assistant

# Warten bis fertig
kubectl rollout status deployment/api-gateway -n packing-assistant
```

### Option 2: Image Update (Best Practice)

**Verwendung**: Mit Versionierung

```bash
# 1. Mit Version bauen
docker build -t smart-packing-assistant-api-gateway:1.2.0 services/api-gateway/

# 2. Deployment Image updaten
kubectl set image deployment/api-gateway \
  api-gateway=smart-packing-assistant-api-gateway:1.2.0 \
  -n packing-assistant

# 3. Rollout überwachen
kubectl rollout status deployment/api-gateway -n packing-assistant
```

### Option 3: Manifest Apply (Deklarativ)

```bash
# 1. Image bauen
docker build -t smart-packing-assistant-frontend:1.1.0 services/frontend/

# 2. Manifest editieren: k8s/09-frontend-deployment.yaml
# Zeile 21: image: smart-packing-assistant-frontend:1.1.0

# 3. Apply
kubectl apply -f k8s/09-frontend-deployment.yaml
```

---

## Alle Services gleichzeitig updaten

```bash
# 1. Alle Images neu bauen
docker compose build

# 2. Alle Deployments neu starten
kubectl rollout restart deployment/ai-worker -n packing-assistant
kubectl rollout restart deployment/api-gateway -n packing-assistant
kubectl rollout restart deployment/frontend -n packing-assistant

# 3. Status checken
kubectl get pods -n packing-assistant
```

---

## Rolling Updates (Zero-Downtime)

### Was passiert automatisch?

Kubernetes ersetzt Pods schrittweise:

```
1. Neuen Pod starten
   [Old Pod v1.0] [New Pod v2.0] 🆕

2. Health Checks abwarten
   [Old Pod v1.0] [New Pod v2.0] ✅ Ready

3. Alten Pod beenden
   [New Pod v2.0] ✅ Running
```

**Bei mehreren Replicas (z.B. 3 Pods):**
```
[Old] [Old] [Old]     → Start
[Old] [Old] [New] ✅   → Pod 1 updated
[Old] [New] [New] ✅   → Pod 2 updated
[New] [New] [New] ✅   → Pod 3 updated - Fertig!
```

**Vorteil**: Keine Downtime, Service bleibt verfügbar!

### Rollout überwachen

```bash
# Live Status
kubectl rollout status deployment/api-gateway -n packing-assistant

# Pods beobachten
kubectl get pods -n packing-assistant -w

# Logs vom neuen Pod
kubectl logs -n packing-assistant -l app=api-gateway --tail=50 -f
```

---

## Rollback bei Problemen

```bash
# Zum vorherigen Stand zurück
kubectl rollout undo deployment/api-gateway -n packing-assistant

# Deployment History anzeigen
kubectl rollout history deployment/api-gateway -n packing-assistant

# Zu spezifischer Version
kubectl rollout undo deployment/api-gateway --to-revision=2 -n packing-assistant
```

---

## Praktische Beispiele

### Beispiel 1: AI Worker Code-Änderung

```bash
# 1. Code editieren
vim services/ai-worker/src/main/kotlin/com/smartpacking/ai/service/CultureService.kt

# 2. Build (optional - Docker macht es auch)
cd services/ai-worker
../../gradlew build

# 3. Docker Image
docker build -t smart-packing-assistant-ai-worker:latest services/ai-worker/

# 4. Deploy
kubectl rollout restart deployment/ai-worker -n packing-assistant

# 5. Verifizieren
kubectl logs -n packing-assistant -l app=ai-worker --tail=20
```

### Beispiel 2: API Gateway mit Version

```bash
# Version 2.0.0 deployen
docker build -t smart-packing-assistant-api-gateway:2.0.0 services/api-gateway/

kubectl set image deployment/api-gateway \
  api-gateway=smart-packing-assistant-api-gateway:2.0.0 \
  -n packing-assistant

# Status live verfolgen
kubectl get pods -n packing-assistant -w
```

### Beispiel 3: Frontend Update

```bash
# Frontend neu bauen
cd services/frontend
npm run build
cd ../..

# Docker Image
docker build -t smart-packing-assistant-frontend:latest services/frontend/

# Deploy
kubectl rollout restart deployment/frontend -n packing-assistant

# Testen (mit Port-Forward)
kubectl port-forward -n packing-assistant service/frontend 5173:80
# Browser: http://localhost:5173
```

---

## Update Script (Automatisiert)

**update-service.sh**:
```bash
#!/bin/bash

SERVICE=$1
VERSION=${2:-latest}

if [ -z "$SERVICE" ]; then
    echo "Usage: ./update-service.sh <service> [version]"
    echo "Example: ./update-service.sh ai-worker 1.2.0"
    exit 1
fi

echo "🔨 Building ${SERVICE}:${VERSION}..."
docker build -t smart-packing-assistant-${SERVICE}:${VERSION} services/${SERVICE}/

if [ "$VERSION" = "latest" ]; then
    echo "🚀 Restarting deployment..."
    kubectl rollout restart deployment/${SERVICE} -n packing-assistant
else
    echo "🚀 Updating image to ${VERSION}..."
    kubectl set image deployment/${SERVICE} \
        ${SERVICE}=smart-packing-assistant-${SERVICE}:${VERSION} \
        -n packing-assistant
fi

echo "⏳ Waiting for rollout..."
kubectl rollout status deployment/${SERVICE} -n packing-assistant

echo "✅ Update complete!"
kubectl get pods -n packing-assistant -l app=${SERVICE}
```

**Verwendung:**
```bash
chmod +x update-service.sh

# Mit 'latest' (schnell)
./update-service.sh ai-worker

# Mit Version (best practice)
./update-service.sh api-gateway 2.0.0
```

---

## Status & Debugging

### Deployment Info

```bash
# Aktuelle Image-Version
kubectl get deployment api-gateway -n packing-assistant \
  -o jsonpath='{.spec.template.spec.containers[0].image}'

# Deployment Details
kubectl describe deployment api-gateway -n packing-assistant

# Update History
kubectl rollout history deployment/api-gateway -n packing-assistant
```

### Pod Status

```bash
# Alle Pods
kubectl get pods -n packing-assistant

# Pod Details
kubectl describe pod -n packing-assistant -l app=api-gateway

# Logs (aktueller Pod)
kubectl logs -n packing-assistant -l app=api-gateway --tail=50

# Logs (vorheriger Pod - bei Crash)
kubectl logs -n packing-assistant -l app=api-gateway --previous
```

### Troubleshooting

**Problem: Pod startet nicht**
```bash
# Events checken
kubectl describe pod -n packing-assistant -l app=ai-worker

# Häufige Ursachen:
# - Image nicht gefunden (ImagePullBackOff)
# - Health Check fehlschlägt (CrashLoopBackOff)
# - Resource Limits überschritten (OOMKilled)
```

**Problem: ImagePullBackOff**
```bash
# Image lokal vorhanden?
docker images | grep smart-packing-assistant

# Neu bauen
docker build -t smart-packing-assistant-ai-worker:latest services/ai-worker/

# Pod löschen (wird neu erstellt)
kubectl delete pod -n packing-assistant -l app=ai-worker
```

**Problem: CrashLoopBackOff**
```bash
# Logs vom abgestürzten Container
kubectl logs -n packing-assistant -l app=api-gateway --previous

# Häufige Ursachen:
# - Datenbank nicht erreichbar
# - Umgebungsvariablen fehlen
# - Port bereits belegt
```

---

## Best Practices

### ✅ DO:

1. **Versionierung**: Nutze Semantic Versioning (`1.0.0`, `1.1.0`, `2.0.0`)
2. **Testen**: Teste lokal mit Docker Compose vor Kubernetes Deploy
3. **Monitoring**: Überwache Rollout mit `kubectl rollout status`
4. **Logs**: Checke Logs nach Update
5. **Rollback-Plan**: Kenne `kubectl rollout undo`

### ❌ DON'T:

1. **Keine manuellen Changes**: Nicht in Container reingehen und Code ändern
2. **Nicht warten**: Nicht mehrere Updates parallel starten
3. **Kein Force**: Vermeide `kubectl delete pod` (nutze `rollout restart`)
4. **Production != latest**: In Production immer Versionen nutzen

---

## Vergleich: Docker Compose vs. Kubernetes Updates

| Aktion | Docker Compose | Kubernetes |
|--------|---------------|------------|
| **Update** | `docker compose up -d --build` | `kubectl rollout restart` |
| **Downtime** | Ja (kurz) | Nein (Rolling Update) |
| **Rollback** | Image-Tag ändern + restart | `kubectl rollout undo` |
| **Versioning** | Manuell (docker-compose.yml) | Automatisch (Revisions) |
| **Zero-Downtime** | Nein (bei 1 Replica) | Ja (bei >1 Replica) |

---

## Production Deployment (Ausblick)

Für echte Production mit Container Registry:

```bash
# 1. Image taggen und pushen
docker tag smart-packing-assistant-api-gateway:2.0.0 \
  your-registry.com/api-gateway:2.0.0
docker push your-registry.com/api-gateway:2.0.0

# 2. Deployment mit Registry-Image
kubectl set image deployment/api-gateway \
  api-gateway=your-registry.com/api-gateway:2.0.0 \
  -n packing-assistant

# 3. imagePullPolicy: Always in Manifest
```

**Registry-Optionen:**
- Docker Hub (`docker.io/username/image:tag`)
- GitHub Container Registry (`ghcr.io/username/image:tag`)
- AWS ECR, Google GCR, Azure ACR

---

## Zusammenfassung

**Für euer Docker Desktop Setup:**

```bash
# Standard Workflow (Development)
docker build -t smart-packing-assistant-ai-worker:latest services/ai-worker/
kubectl rollout restart deployment/ai-worker -n packing-assistant
```

**Mit Versionierung (Production-like):**

```bash
# Mit Version
docker build -t smart-packing-assistant-api-gateway:1.2.0 services/api-gateway/
kubectl set image deployment/api-gateway \
  api-gateway=smart-packing-assistant-api-gateway:1.2.0 \
  -n packing-assistant
```

**Rollback:**
```bash
kubectl rollout undo deployment/api-gateway -n packing-assistant
```

Das war's! Bei Fragen siehe `KUBERNETES_DD_GUIDE.md` für weitere Details.