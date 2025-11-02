# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
- This file should reach more than 500 lines.

## Project Overview

**Smart Packing Assistant** is an AI-powered travel assistant that generates personalized packing lists. Users input their destination, duration, season, and travel type (business/vacation/backpacking), and the AI generates a categorized packing list with context-aware recommendations including weather considerations, cultural tips, and activity-specific items.

**University Project** (HTW Berlin) - Deadline: November 22, 2025, 23:59:59 Uhr
- **Grading**: Concept/README (40pts), AI (25pts), Docker (20pts), Kubernetes (10pts), Pitch (5pts)
- **Instructor**: alkurdiz@htw-berlin.de
- **Deployment Goal**: Production-ready application with full security

### Tech Stack
- **Frontend**: React 18.3.1 + TypeScript 5.9.3 + Vite 7.1.7 (Port: 5173)
- **Backend**: Spring Boot 3.5.6 + Kotlin 1.9.25 + Java 21
  - **API Gateway** (Port: 8080): REST API, PostgreSQL, session management, business logic
  - **AI Worker** (Port: 8081): OpenAI integration, prompt engineering, response parsing
- **Database**: PostgreSQL 15 (Port: 5432) - Only accessed by API Gateway
- **Build Tools**: Gradle 8.14.3, npm

## Architecture

Microservices architecture:

```
Frontend (React) :5173 → API Gateway (Spring) :8080 → AI Worker (Spring) :8081
                              ↓
                         PostgreSQL :5432
```

**Services**:
1. **Frontend** (`services/frontend/`): React UI with Vite
2. **API Gateway** (`services/api-gateway/`): Main API with PostgreSQL persistence
3. **AI Worker** (`services/ai-worker/`): AI processing (OpenAI integration)
4. **Shared** (`services/shared/`): Shared DTOs, enums, models

## Core Features

1. **Generate Packing Lists**: Based on destination, duration, season, travel type
2. **Context-Aware Recommendations**: Weather data, cultural tips, activity-specific items
3. **Session Management**: Secure session tokens, 24-hour auto-expiry
4. **Database Persistence**: PostgreSQL with JPA/Hibernate
5. **AI Integration**: OpenAI GPT-5 with anti-hallucination safeguards

## AI Integration

### Configuration
- **Framework**: Spring AI 1.0.0-M4 (official Spring AI framework)
- **API Key**: Configured in `.env` file → `OPENAI_API_KEY` environment variable
- **Model**: GPT-5 with temperature 0.3 (reliability over creativity)
- **Max Tokens**: 2000
- **Response Format**: Structured JSON with 5 categories

### Anti-Hallucination Strategy
- **Low Temperature** (0.3): Reduces creative/invented responses
- **Structured Prompts**: Clear instructions with JSON schema
- **Response Validation**: 3-100 items total, 1-50 quantity per item
- **Fallback Mechanism**: Dummy data if AI fails
- **Explicit Instructions**: "Only list items, no inventions"

### Prompt Engineering
- **System Prompt**: Role definition, anti-hallucination guidelines, output format
- **User Prompt**: Destination, duration, season, travel type, weather context, culture tips
- **Temperature 0.3**: Prioritizes accuracy over creativity
- **Validation**: Schema validation, item count checks, quantity limits

## Security

**CRITICAL**: Production deployment requires full security implementation.

### Current Implementation
- ✅ **Session tokens required** via `X-Session-Token` header (32-byte Base64)
- ✅ **Input validation** with Jakarta Bean Validation
- ✅ **SQL injection prevention** via JPA parameterized queries
- ✅ **Error handling** without information leakage
- ⚠️ **No authentication** (session-based only)
- ⚠️ **No rate limiting** (DoS vulnerability)
- ⚠️ **Hardcoded DB credentials** in application.properties

### Production Requirements

**Must-Have Before Deployment**:
1. **HTTPS/TLS**: SSL certificates, reverse proxy with TLS termination
2. **Authentication**: API keys (MVP) or OAuth2/JWT (full production)
3. **Rate Limiting**: 10 req/min for `/api/packing/generate`, 100 req/min others
4. **Secrets Management**: Environment variables, Kubernetes Secrets
5. **Database Security**: SSL connections, strong passwords, network isolation
6. **CORS**: Whitelist production domains only (never `*` with credentials)
7. **Security Headers**: CSP, HSTS, X-Frame-Options, X-Content-Type-Options
8. **Container Security**: Non-root users, read-only filesystems
9. **Dependency Scanning**: OWASP dependency-check in CI/CD
10. **Logging**: Structured logs, security event monitoring, no PII leakage

**Configuration Examples**:
```properties
# Production database (application-prod.properties)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

```yaml
# Kubernetes Secret
apiVersion: v1
kind: Secret
metadata:
  name: api-gateway-secrets
data:
  database-password: <base64>
  openai-api-key: <base64>
```

**Security Checklist**: See full checklist in project documentation.
**Resources**: Spring Security, OWASP Top 10, Kubernetes Security Best Practices

## Database Schema

PostgreSQL 15 database (`packing_assistant`) with 5 tables:

1. **sessions**: Session management (id, session_token, created_at, last_activity, is_active)
2. **packing_lists**: Generated lists (id, session_id, destination, items_json JSONB, weather_info, culture_tips TEXT[])
3. **chat_messages**: Chat history (id, packing_list_id, role, content, ai_model)
4. **dummy_weather_data**: Weather simulation (location, season, temp_min/max, conditions)
5. **dummy_culture_tips**: Cultural tips (location, category, tip, importance)

**Key Features**:
- JSONB for flexible item storage with GIN indexes
- Foreign keys with cascade delete
- Views: `active_sessions_summary`, `packing_lists_with_chat_count`
- Functions: `cleanup_inactive_sessions()`, `update_session_activity()`

**Migration**: Flyway migrations in `services/api-gateway/src/main/resources/db/migration/`

## Common Commands

### Backend Services
```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Tests
./gradlew test

# Clean
./gradlew clean
```

### Docker & Kubernetes
```bash
# Docker Compose
docker compose up -d
docker compose down

# Kubernetes
kind create cluster
kubectl apply -f k8s/
kubectl get pods
kubectl get services
kubectl port-forward service/api-gateway 8080:8080
```

### Testing
- **Backend**: JUnit 5 tests in `src/test/kotlin/`
- **API**: Postman collection in `POSTMAN_TESTING_GUIDE.md`
- **E2E**: See `services/api-gateway/END_TO_END_TESTING.md`

## Key Technical Details

### API Gateway (`services/api-gateway/`)
**Configuration** (`application.properties`):
- Port: 8080
- Database: PostgreSQL at localhost:5432/packing_assistant
- Credentials: admin/secret123 (development only - change for production)
- Flyway: Disabled for local (`spring.flyway.enabled=false`), enable for Docker/K8s
- JPA: `ddl-auto=validate`, SQL logging enabled
- AI Worker URL: http://localhost:8081

**Structure**:
```
com.smartpacking.api/
├── controller/     # REST endpoints (PackingController, SessionController)
├── service/        # Business logic (PackingListService, SessionService, AiWorkerClient)
├── repository/     # JPA repositories
├── entity/         # JPA entities (Session, PackingList, ChatMessage)
├── exception/      # Exception handling
└── config/         # Configuration (RestTemplate)
```

### AI Worker (`services/ai-worker/`)
**Configuration** (`application.properties`):
- Port: 8081
- OpenAI Model: gpt-5
- Temperature: 0.3
- Timeout: 30 seconds
- Max Tokens: 2000

**Structure**:
```
com.smartpacking.ai/
├── controller/     # AiController (POST /api/ai/generate, GET /health, /test-connection)
├── service/        # AiService, PromptService, WeatherService, CultureService, OpenAiHealthService
├── model/          # Data models (AiPackingResponse, PackingItem, PackingCategories, WeatherData, CultureTip)
├── exception/      # Custom exceptions (7 types) + GlobalExceptionHandler
├── mapper/         # DTO mapping (PackingResponseMapper)
├── dto/            # ErrorResponse DTOs
└── config/         # OpenAI configuration (Spring AI)
```

**Data Files** (`src/main/resources/data/`):
- `weather_data.json`: Weather by destination/season
- `culture_tips.json`: Cultural tips by destination

**API Endpoints**:
- `POST /api/ai/generate` - Generate packing list (returns 201 Created)
- `GET /api/ai/health` - Service health check
- `GET /api/ai/test-connection` - Test OpenAI API connectivity

### Shared Module (`services/shared/`)
```
com.smartpacking.shared/
├── dto/       # PackingRequest, PackingResponse, PackingItem
├── enums/     # TravelType (BUSINESS, VACATION, BACKPACKING), Season
└── model/     # WeatherInfo
```

**Key DTOs**:
- `PackingRequest`: Input validation with Jakarta Bean Validation (@NotBlank, @Min, @Max)
- `PackingResponse`: Response structure with UUID, destination, categories, weather, culture tips
- `PackingCategories`: Categorized items (clothing, tech, hygiene, documents, other)
- `PackingItem`: Individual item (item name, quantity, reason)

### Frontend (`services/frontend/`)
**Configuration**:
- Port: 5173 (development), 80 (production via Nginx)
- Build Tool: Vite 7.1.7
- Package Manager: npm

**Structure**:
```
src/
├── App.tsx                # Main application component
├── types.ts               # TypeScript interfaces (API types, enums)
├── components/
│   ├── PackingForm.tsx    # User input form
│   ├── PackingList.tsx    # Display generated list
│   └── History.tsx        # Historical lists
└── services/
    └── api.ts             # HTTP client (axios)
```

**Key Features**:
- Session token management
- Form validation for destination, duration, travel type, season
- Real-time API communication with API Gateway
- Responsive UI for packing list display
- Docker deployment with Nginx (multi-stage build)

**Environment Variables**:
- `VITE_API_URL`: API Gateway URL (default: http://localhost:8080)

### Kotlin Configuration
- JSR-305 strict mode (`-Xjsr305=strict`)
- All-open plugin for JPA entities (api-gateway only)
- Java 21 toolchain

## Working with the Codebase

### Adding New Endpoints
1. Create controller in `controller/` package
2. Add service layer in `service/` package
3. Use `@RestController`, `@RequestMapping`, `@Valid` annotations
4. Add tests in `src/test/kotlin/`

### Database Migrations
1. Create migration in `services/api-gateway/src/main/resources/db/migration/`
2. Naming: `V{version}__{description}.sql` (e.g., `V2__add_user_table.sql`)
3. Enable Flyway: `spring.flyway.enabled=true`
4. Run: `./gradlew bootRun`

### Session Management
**Required**: All requests to `/api/packing/*` need `X-Session-Token` header.

**Create Session**:
```bash
curl -X POST http://localhost:8080/api/sessions
# Returns: {"sessionToken": "abc123...", "sessionId": "uuid"}
```

**Use Session**:
```bash
curl -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: abc123..." \
  -d '{"destination":"Paris","durationDays":3,"travelType":"VACATION","season":"SPRING"}'
```

## API Endpoints Reference

### API Gateway Endpoints (Port 8080)

#### Session Management (`/api/sessions`)
| Method | Endpoint | Description | Auth | Response |
|--------|----------|-------------|------|----------|
| POST | `/api/sessions` | Create new session | None | `{"sessionToken": "...", "sessionId": "uuid"}` |
| GET | `/api/sessions` | Get all active sessions | None | `List<SessionResponse>` |
| GET | `/api/sessions/{token}` | Get session info | None | `SessionResponse` |
| GET | `/api/sessions/{token}/validate` | Validate session token | None | `{"valid": boolean}` |
| POST | `/api/sessions/cleanup` | Cleanup inactive sessions (>24h) | None | `{"cleaned": count}` |

#### Packing Lists (`/api/packing`)
| Method | Endpoint | Description | Auth | Response |
|--------|----------|-------------|------|----------|
| POST | `/api/packing/generate` | Generate packing list | **Required** (X-Session-Token) | `PackingResponse` (201 Created) |
| GET | `/api/packing/{id}` | Get specific packing list | None | `PackingResponse` |
| GET | `/api/packing/session` | Get all lists for session | **Required** (X-Session-Token) | `List<PackingResponse>` |
| GET | `/api/packing/session/recent` | Get recent lists (with limit) | **Required** (X-Session-Token) | `List<PackingResponse>` |
| GET | `/api/packing/search?destination={query}` | Search by destination | None | `List<PackingResponse>` |
| GET | `/api/packing/health` | Health check | None | `{"status": "UP"}` |

**CORS Configuration**: Currently allows `http://localhost:5173` (development frontend)

### AI Worker Endpoints (Port 8081)

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| POST | `/api/ai/generate` | Generate packing list from AI | `PackingResponse` (201 Created) |
| GET | `/api/ai/health` | Service health check | `{"status": "UP"}` |
| GET | `/api/ai/test-connection` | Test OpenAI API connection | `{"connected": boolean}` |

**Note**: AI Worker endpoints are internal and called by API Gateway, not directly by frontend.

## Important Constraints

1. **Database Access**: Only API Gateway accesses PostgreSQL. AI Worker is stateless.
2. **Service Communication**: API Gateway → AI Worker via HTTP (internal network in Docker/K8s)
3. **Dummy Data**: Explicitly allowed for weather, culture tips (development/fallback)
4. **AI Reliability**: Better to say "I don't know" than hallucinate (temperature 0.3, validation)
5. **Security**: Required for production (HTTPS, authentication, rate limiting, secrets management)
6. **Session Tokens**: Required via header, 32-byte secure tokens, 24-hour expiry
7. **README Limit**: Maximum 220 lines
8. **Pitch**: 1-3 minutes, max 25 MB, audio preferred
9. **Kubernetes**: Minimum 2 services with deployments

## Project Status

**Current Phase**: Phase 10 Complete (Full Stack Implementation) ✅

**Completed**:
- ✅ Phase 1: Project setup, database schema, services initialized
- ✅ Phase 2: Shared module (DTOs, enums, models)
- ✅ Phase 3: API Gateway database layer (Flyway, JPA, repositories)
- ✅ Phase 4: API Gateway REST endpoints (controllers, services, exceptions)
- ✅ Phase 5: AI Worker dummy data (weather, culture services)
- ✅ Phase 6A-E: AI Worker full implementation (OpenAI, prompts, parsing, validation, API, testing)
- ✅ Phase 7: Services connected (HTTP client, error handling, E2E testing)
- ✅ Phase 8: Docker (Multi-stage Dockerfiles, docker-compose.yml with health checks)
- ✅ Phase 9: Kubernetes (11 manifests with init containers, secrets, PVC)
- ✅ Phase 10: Frontend (React + TypeScript + Vite with Nginx deployment)

**Remaining Tasks**:
1. **Phase 11**: Documentation (Enhance README with 10 questions, currently minimal)
2. **Phase 12**: Pitch (1-3 min audio/video, max 25 MB)

**Documentation**:
- `docs/smartpacking/guides/POSTMAN_TESTING_GUIDE.md`: API testing with Postman (629 lines)
- `services/api-gateway/END_TO_END_TESTING.md`: Full system testing
- `services/ai-worker/API_DOCUMENTATION.md`: AI Worker API reference (374 lines)
- `docs/smartpacking/guides/DOCKER_GUIDE.md`: Docker deployment guide (427 lines)
- `docs/smartpacking/guides/KUBERNETES_DD_GUIDE.md`: Kubernetes deployment (926 lines)
- `docs/smartpacking/guides/KUBERNETES_UPDATE_GUIDE.md`: K8s update procedures (399 lines)
- `docs/smartpacking/project/roadmap.md`: Detailed 15-phase plan

## Environment Setup

### Local Development
```bash
# 1. Start PostgreSQL
brew services start postgresql@15
psql -U admin -d packing_assistant

# 2. Set OpenAI API Key
export OPENAI_API_KEY=sk-your-key-here

# 3. Start AI Worker
cd services/ai-worker
./gradlew bootRun

# 4. Start API Gateway
cd services/api-gateway
./gradlew bootRun

# 5. Test
curl http://localhost:8080/api/packing/health
```

### Docker Deployment
**docker-compose.yml** defines 4 services with health checks and dependencies:

1. **postgres** (postgres:15-alpine)
   - Port: 5432, Volume: postgres-data (persistent)
   - Health check: `pg_isready`

2. **ai-worker** (custom multi-stage build)
   - Port: 8081, Profile: docker
   - Health check: `/actuator/health` (60s start period)
   - Depends on: none (stateless)

3. **api-gateway** (custom multi-stage build)
   - Port: 8080, Profile: docker
   - Health check: `/actuator/health` (90s start period)
   - Depends on: postgres (healthy), ai-worker (healthy)
   - Flyway enabled automatically

4. **frontend** (custom multi-stage build with Nginx)
   - Port: 5173:80 (maps to Nginx)
   - Health check: `wget` on root path
   - Depends on: api-gateway (healthy)

**Multi-Stage Dockerfiles**:
- **Backend** (api-gateway, ai-worker): gradle:8.14-jdk21-alpine → eclipse-temurin:21-jre-alpine
  - Non-root user (spring:spring)
  - Optimized for production (JRE only)
- **Frontend**: node:20-alpine → nginx:alpine
  - Vite build optimization
  - Nginx for static file serving

**Commands**:
```bash
# Build and start all services
docker compose up -d

# Build specific service
docker compose build api-gateway

# Check logs
docker compose logs -f

# Stop and remove
docker compose down

# Stop and remove with volumes
docker compose down -v
```

### Kubernetes Deployment
**Required Manifests** (11 files in `k8s/` directory):
1. `00-namespace.yaml` - Namespace: packing-assistant
2. `01-postgres-secret.yaml` - Secret: app-secrets (DB credentials, OpenAI key)
3. `02-postgres-pvc.yaml` - PersistentVolumeClaim: 10Gi
4. `03-postgres-deployment.yaml` - PostgreSQL 15 deployment
5. `04-postgres-service.yaml` - ClusterIP service (port 5432)
6. `05-ai-worker-deployment.yaml` - AI Worker with resource limits (512Mi-1Gi)
7. `06-ai-worker-service.yaml` - ClusterIP service (port 8081)
8. `07-api-gateway-deployment.yaml` - API Gateway with init containers, resource limits
9. `08-api-gateway-service.yaml` - ClusterIP service (port 8080)
10. `09-frontend-deployment.yaml` - Frontend with Nginx (256Mi-512Mi)
11. `10-frontend-service.yaml` - LoadBalancer/NodePort (port 80)

**Key Features**:
- Init containers for service dependencies (wait-for-postgres, wait-for-ai-worker)
- Resource requests and limits for all pods
- Liveness and readiness probes with `/actuator/health`
- Secret references for sensitive data (no hardcoded credentials)
- Service discovery via DNS (service-name.namespace.svc.cluster.local)
- Persistent storage for PostgreSQL

**Deploy**:
```bash
# Create cluster
kind create cluster

# Load Docker images to kind
kind load docker-image smart-packing-assistant-api-gateway:latest
kind load docker-image smart-packing-assistant-ai-worker:latest
kind load docker-image smart-packing-assistant-frontend:latest

# Apply manifests
kubectl apply -f k8s/

# Watch deployment
kubectl get pods -n packing-assistant -w

# Port forwarding (for testing)
kubectl port-forward -n packing-assistant service/frontend 5173:80
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080
```

## Testing

### Unit Tests
```bash
# API Gateway
cd services/api-gateway
./gradlew test

# AI Worker
cd services/ai-worker
./gradlew test
```

### Integration Tests
- Located in `src/test/kotlin/`
- Require PostgreSQL running (use `@ActiveProfiles("integration")`)
- Database schema must exist

### API Testing
See `POSTMAN_TESTING_GUIDE.md` for:
- 12 Postman request examples
- Session creation workflow
- Error handling tests
- Performance benchmarks

### End-to-End Testing
See `services/api-gateway/END_TO_END_TESTING.md` for:
- Full system startup
- Service health checks
- Complete request flows
- Database verification queries
- Error scenarios

## Development Notes & Best Practices

### Environment Variables
- ✅ `.env` file is in `.gitignore` (line 10)
- ⚠️ If `.env` is tracked by git (was added before .gitignore), run:
  ```bash
  git rm --cached .env
  git commit -m "Remove .env from tracking"
  ```
- Create `.env` from `.env.example` for local development
- Never commit real API keys to version control

### CORS Configuration
- **Current**: Hardcoded to `http://localhost:5173` in controllers (`@CrossOrigin` annotation)
- **Location**: `PackingController.kt` and `SessionController.kt` (line 19)
- **Production**: Should use environment-based configuration
- **Recommendation**: Move to `CorsConfig.kt` with profile-specific allowed origins

### Frontend-Backend Communication
- **Local Dev**: Frontend (5173) → API Gateway (8080)
- **Docker**: Frontend (80 via Nginx) → API Gateway (8080)
- **K8s**: Service discovery via DNS names
- **Environment Variable**: `VITE_API_URL` configures API endpoint

### Database Credentials
- **Local**: Hardcoded in `application.properties` (admin/secret123) - OK for dev
- **Docker/K8s**: Uses environment variables from secrets - REQUIRED
- **Production**: MUST use strong passwords and Kubernetes Secrets

### README Status
- **Current**: Minimal (only "# smart-packing-assistant", 25 bytes)
- **Required**: Maximum 220 lines with 10 questions answered
- **Priority**: HIGH (worth 40 points in grading)
- **Content Needed**: Architecture, deployment, API usage, security, testing

### Git Workflow
- **Current Branch**: main
- **Modified Files** (unstaged):
  - `.gitignore`
  - `services/ai-worker/src/main/resources/application.properties`
  - `services/ai-worker/src/main/resources/application-docker.properties`
- **Untracked Documentation** (should be committed):
  - `docs/smartpacking/guides/POSTMAN_TESTING_GUIDE.md`
  - `services/ai-worker/API_DOCUMENTATION.md`
  - `services/api-gateway/END_TO_END_TESTING.md`

### Testing Strategy
- **Unit Tests**: JUnit 5 in `src/test/kotlin/`
- **Integration Tests**: Require PostgreSQL running, use `@ActiveProfiles("integration")`
- **API Tests**: Postman collection with 12+ requests
- **E2E Tests**: Full workflow from session creation to packing list generation
- **Docker Tests**: `docker compose up` → test endpoints → `docker compose down`
- **K8s Tests**: Deploy to kind → port-forward → test → cleanup

### Performance Considerations
- **AI Worker**: 30-second timeout for OpenAI API calls
- **Database**: GIN indexes on JSONB columns for fast queries
- **Session Cleanup**: Function `cleanup_inactive_sessions()` for maintenance
- **Resource Limits**: Configured in Kubernetes manifests (512Mi-1Gi memory)

## Troubleshooting

### "Connection refused" on port 8080/8081
**Solution**: Service not running. Start with `./gradlew bootRun`

### "Database connection failed"
**Solution**: PostgreSQL not running or wrong credentials
```bash
brew services start postgresql@15
psql -U admin -d packing_assistant -c "SELECT 1;"
```

### "Invalid API key" from AI Worker
**Solution**: Set correct OpenAI API key
```bash
export OPENAI_API_KEY=sk-actual-key
```

### "Session not found"
**Solution**: Create session first via `POST /api/sessions`, then use token in header

### Hot reload not working
**Solution**: DevTools enabled by default, but manual restart may be needed:
```bash
# Stop service (Ctrl+C)
./gradlew bootRun
```

## Dependencies & Build Configuration

### Backend Services (Gradle)
**Root Build** (`build.gradle.kts`, `settings.gradle.kts`):
- Gradle: 8.14.3
- Included modules: shared, api-gateway, ai-worker

**API Gateway** (`services/api-gateway/build.gradle.kts`):
- Spring Boot: 3.5.6
- Kotlin: 1.9.25 + kotlin-jvm plugin
- kotlin-spring (all-open for @Component classes)
- Dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - flyway-core
  - postgresql (runtime)
  - jackson-module-kotlin

**AI Worker** (`services/ai-worker/build.gradle.kts`):
- Spring Boot: 3.5.6
- Kotlin: 1.9.25
- Dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-validation
  - spring-ai-openai-spring-boot-starter (1.0.0-M4)
  - jackson-module-kotlin

**Shared Module** (`services/shared/build.gradle.kts`):
- Kotlin: 1.9.25
- No Spring Boot (library module)
- Dependencies:
  - jackson-annotations
  - jakarta.validation-api

### Frontend (npm)
**Package.json** (`services/frontend/package.json`):
- React: 18.3.1
- TypeScript: 5.9.3
- Vite: 7.1.7
- Dependencies:
  - axios: ^1.13.0 (HTTP client)
  - react, react-dom: ^18.3.1
- Dev Dependencies:
  - @vitejs/plugin-react: ^5.0.4
  - typescript: ~5.9.3
  - eslint: ^9.36.0
  - typescript-eslint: ^8.45.0

### Database
- PostgreSQL: 15 (alpine in containers)
- JDBC Driver: Included in spring-boot-starter-data-jpa
- Migration Tool: Flyway
- Extensions: uuid-ossp (for UUID generation)

### Container Images
- **Builder Images**:
  - gradle:8.14-jdk21-alpine (backend build)
  - node:20-alpine (frontend build)
- **Runtime Images**:
  - eclipse-temurin:21-jre-alpine (backend)
  - nginx:alpine (frontend)
  - postgres:15-alpine (database)

## Next Steps & Recommendations

### Immediate Priorities (Before November 22, 2025)

1. **README Enhancement** (HIGH PRIORITY - 40 points):
   - Expand from 25 bytes to maximum 220 lines
   - Answer 10 required questions
   - Include architecture diagram (ASCII art)
   - Add deployment instructions
   - Document API usage examples
   - Explain security considerations

2. **Pitch Preparation** (5 points):
   - 1-3 minutes audio/video
   - Maximum 25 MB file size
   - Highlight: AI integration, Docker/K8s deployment, anti-hallucination safeguards
   - Demo: Show working application (optional)

3. **Code Cleanup**:
   - Commit untracked documentation files
   - Review and commit modified application.properties files
   - Verify .env is not tracked by git

### Production Readiness Enhancements (Post-Deadline)

1. **Security Hardening**:
   - Implement API key authentication
   - Add rate limiting (Spring Cloud Gateway or Redis)
   - Environment-based CORS configuration
   - HTTPS/TLS termination (Ingress controller)
   - Security headers (CSP, HSTS, X-Frame-Options)

2. **Observability**:
   - Structured logging (JSON format)
   - Metrics collection (Micrometer + Prometheus)
   - Distributed tracing (Spring Cloud Sleuth)
   - Centralized logging (ELK stack or Loki)

3. **Scalability**:
   - Horizontal pod autoscaling (HPA)
   - Redis for session storage (stateless API Gateway)
   - Read replicas for PostgreSQL
   - CDN for frontend static assets

4. **CI/CD Pipeline**:
   - Automated testing (GitHub Actions / GitLab CI)
   - Container image scanning (Trivy)
   - Dependency vulnerability checks (OWASP)
   - Automated deployment to K8s

5. **Enhanced Features**:
   - User authentication (OAuth2 / JWT)
   - Packing list sharing (public links)
   - PDF export functionality
   - Multi-language support
   - Real-time weather API integration

## Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring AI**: https://docs.spring.io/spring-ai/reference/
- **Kotlin**: https://kotlinlang.org/docs/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Docker**: https://docs.docker.com/
- **Kubernetes**: https://kubernetes.io/docs/
- **React**: https://react.dev/
- **Vite**: https://vite.dev/
- **OpenAI API**: https://platform.openai.com/docs/

## Contact

- **University Project**: alkurdiz@htw-berlin.de
- **Security Issues**: [Define for production]
- **Project Deadline**: November 22, 2025, 23:59:59 Uhr
