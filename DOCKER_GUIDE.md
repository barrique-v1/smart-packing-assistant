# Docker Deployment Guide

This guide explains how to build and run the Smart Packing Assistant using Docker and Docker Compose.

## Prerequisites

- Docker Desktop 20.10+ (Windows/Mac) or Docker Engine (Linux)
- Docker Compose 2.0+
- OpenAI API Key

## Quick Start

### 1. Set up environment variables

Edit the `.env` file in the project root and add your OpenAI API key:

```bash
# .env
OPENAI_API_KEY=sk-your-actual-openai-api-key-here
POSTGRES_DB=NAME
POSTGRES_USER=USER
POSTGRES_PASSWORD=PASSWORD
```

**Important**: The `.env.example` file is provided as a template. Copy it to `.env` and replace `sk-your-actual-openai-api-key-here` with your actual OpenAI API key.

### 2. Build all services

```bash
docker compose build
```

This will build:
- `postgres` (PostgreSQL 15 database)
- `ai-worker` (AI Worker service on port 8081)
- `api-gateway` (API Gateway service on port 8080)
- `frontend` (React frontend on port 5173)

**Note**: The first build will take 10-15 minutes as it downloads dependencies and compiles the Kotlin code.

### 3. Start all services

```bash
docker compose up -d
```

This starts all containers in detached mode.

### 4. Check service health

```bash
# View all running containers
docker compose ps

# Check logs
docker compose logs -f

# Check specific service logs
docker compose logs -f api-gateway
docker compose logs -f ai-worker
docker compose logs -f postgres
docker compose logs -f frontend
```

### 5. Wait for services to be ready

The services have health checks configured. Wait 60-90 seconds for all services to become healthy:

```bash
# Check health status
docker compose ps

# Expected output:
# NAME                  STATUS
# packing-postgres      Up (healthy)
# packing-ai-worker     Up (healthy)
# packing-api-gateway   Up (healthy)
# packing-frontend      Up (healthy)
```

### 6. Test the API

```bash
# Create a session
curl -X POST http://localhost:8080/api/sessions

# Save the sessionToken from the response
# Example response: {"sessionToken":"abc123...","sessionId":"uuid"}

# Generate a packing list
curl -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: YOUR_SESSION_TOKEN_HERE" \
  -d '{
    "destination": "Iceland",
    "durationDays": 5,
    "season": "WINTER",
    "travelType": "VACATION"
  }'
```

### 7. Access the frontend

Open your browser and navigate to:
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8080/actuator/health
- AI Worker: http://localhost:8081/actuator/health

## Architecture

The Docker Compose setup includes:

1. **PostgreSQL Database** (`postgres`)
   - Port: 5432
   - Database: `packing_assistant`
   - Persistent volume: `postgres-data`
   - Health check: `pg_isready`

2. **AI Worker** (`ai-worker`)
   - Port: 8081
   - Handles OpenAI API calls
   - Requires: `OPENAI_API_KEY` environment variable
   - Health check: `/actuator/health`

3. **API Gateway** (`api-gateway`)
   - Port: 8080
   - Main REST API
   - Connects to PostgreSQL and AI Worker
   - Runs Flyway migrations on startup
   - Health check: `/actuator/health`

4. **Frontend** (`frontend`)
   - Port: 5173 (mapped to container port 80)
   - React + Vite application
   - Served by nginx
   - Health check: HTTP GET on `/`

## Service Dependencies

The services start in the following order:

1. `postgres` (no dependencies)
2. `ai-worker` (no dependencies)
3. `api-gateway` (depends on `postgres` and `ai-worker` being healthy)
4. `frontend` (depends on `api-gateway` being healthy)

## Common Commands

### Build services

```bash
# Build all services
docker compose build

# Build specific service
docker compose build api-gateway
docker compose build ai-worker
docker compose build frontend

# Build without cache (clean build)
docker compose build --no-cache
```

### Start/Stop services

```bash
# Start all services
docker compose up -d

# Start specific service
docker compose up -d api-gateway

# Stop all services
docker compose down

# Stop and remove volumes (WARNING: deletes database data)
docker compose down -v
```

### Logs

```bash
# View all logs
docker compose logs

# Follow logs (live tail)
docker compose logs -f

# View logs for specific service
docker compose logs api-gateway
docker compose logs -f ai-worker

# Last 100 lines
docker compose logs --tail=100
```

### Database access

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U admin -d packing_assistant

# List tables
\dt

# View sessions
SELECT * FROM sessions;

# View packing lists
SELECT id, destination, duration_days, season, travel_type, created_at
FROM packing_lists ORDER BY created_at DESC LIMIT 5;

# Exit psql
\q
```

### Restart services

```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart api-gateway
```

### View service details

```bash
# List running containers
docker compose ps

# View container resource usage
docker stats

# Inspect service configuration
docker compose config
```

## Troubleshooting

### Services not starting

**Problem**: Services fail to start or are unhealthy

**Solution**:
```bash
# Check logs for errors
docker compose logs -f

# Check specific service
docker compose logs api-gateway

# Common issues:
# 1. Missing OPENAI_API_KEY in .env
# 2. Port conflicts (8080, 8081, 5432, 5173 already in use)
# 3. Insufficient memory (Docker needs 4GB+ RAM)
```

### Database connection errors

**Problem**: API Gateway can't connect to PostgreSQL

**Solution**:
```bash
# Check if postgres is healthy
docker compose ps postgres

# Check postgres logs
docker compose logs postgres

# Verify database credentials in .env
cat .env | grep POSTGRES

# Restart services in order
docker compose down
docker compose up -d
```

### AI Worker fails

**Problem**: AI Worker returns errors or is unhealthy

**Solution**:
```bash
# Check if OPENAI_API_KEY is set
docker compose exec ai-worker env | grep OPENAI_API_KEY

# Test OpenAI connection
curl http://localhost:8081/actuator/health

# Check logs for errors
docker compose logs ai-worker | grep -i error
```

### Flyway migration errors

**Problem**: API Gateway fails with Flyway errors

**Solution**:
```bash
# This usually means database schema conflicts
# Option 1: Clean database and restart
docker compose down -v
docker compose up -d

# Option 2: Manually reset database
docker compose exec postgres psql -U admin -d packing_assistant -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
docker compose restart api-gateway
```

### Port conflicts

**Problem**: "Port already in use" errors

**Solution**:
```bash
# Check what's using the ports
# Windows:
netstat -ano | findstr :8080
netstat -ano | findstr :8081
netstat -ano | findstr :5432

# Linux/Mac:
lsof -i :8080
lsof -i :8081
lsof -i :5432

# Either stop the conflicting service or change ports in docker-compose.yml:
# ports:
#   - "8090:8080"  # Use 8090 instead of 8080
```

### Build failures

**Problem**: Docker build fails

**Solution**:
```bash
# Clean build cache
docker compose build --no-cache

# Check disk space
docker system df

# Clean unused images/containers
docker system prune -a

# Check build logs
docker compose build ai-worker 2>&1 | tee build.log
```

## Performance Optimization

### Reduce build time

```bash
# Use build cache
docker compose build

# Build in parallel (if supported)
COMPOSE_BAKE=true docker compose build
```

### Reduce image size

The Dockerfiles use multi-stage builds to minimize image size:
- Builder stage: Full Gradle/Node environment
- Runtime stage: Minimal JRE/nginx only

### Resource limits

Edit `docker-compose.yml` to add resource limits:

```yaml
services:
  api-gateway:
    # ... existing config ...
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          memory: 512M
```

## Security Notes

### Development vs Production

**Current setup is for DEVELOPMENT only**. For production:

1. **Use strong passwords**:
   ```bash
   POSTGRES_PASSWORD=$(openssl rand -base64 32)
   ```

2. **Use Docker Secrets** instead of environment variables:
   ```yaml
   secrets:
     db_password:
       file: ./secrets/db_password.txt
   ```

3. **Enable HTTPS** with reverse proxy (nginx/Traefik)

4. **Scan images for vulnerabilities**:
   ```bash
   docker scan smart-packing-assistant-api-gateway
   ```

5. **Run containers as non-root** (already configured in Dockerfiles)

## Next Steps

After Docker is working:
- **Phase 9**: Deploy to Kubernetes (see `k8s/` directory)
- **Phase 11**: Create README.md with project documentation
- **Phase 12**: Record pitch (1-3 minutes)

## Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [Nginx Docker Image](https://hub.docker.com/_/nginx)
