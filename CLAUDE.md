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
5. **AI Integration**: OpenAI GPT-4 with anti-hallucination safeguards

## AI Integration

### Configuration
- **Framework**: Spring AI 1.0.0-M4 (official Spring AI framework)
- **API Key**: Configured in `.env` file → `OPENAI_API_KEY` environment variable
- **Model**: GPT-4 with temperature 0.3 (reliability over creativity)
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
- OpenAI Model: gpt-4
- Temperature: 0.3
- Timeout: 30 seconds
- Max Tokens: 2000

**Structure**:
```
com.smartpacking.ai/
├── controller/     # AiController (POST /api/ai/generate)
├── service/        # AiService, PromptService, WeatherService, CultureService
├── model/          # Data models (AiPackingResponse, PackingItem)
├── exception/      # Custom exceptions (7 types)
├── mapper/         # DTO mapping (PackingResponseMapper)
└── config/         # OpenAI configuration
```

**Data Files** (`src/main/resources/data/`):
- `weather_data.json`: Weather by destination/season
- `culture_tips.json`: Cultural tips by destination

### Shared Module (`services/shared/`)
```
com.smartpacking.shared/
├── dto/       # PackingRequest, PackingResponse, PackingItem
├── enums/     # TravelType (BUSINESS, VACATION, BACKPACKING), Season
└── model/     # WeatherInfo
```

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

**Current Phase**: Phase 7 Complete (Services Connected) ✅

**Completed**:
- ✅ Phase 1: Project setup, database schema, services initialized
- ✅ Phase 2: Shared module (DTOs, enums, models)
- ✅ Phase 3: API Gateway database layer (Flyway, JPA, repositories)
- ✅ Phase 4: API Gateway REST endpoints (controllers, services, exceptions)
- ✅ Phase 5: AI Worker dummy data (weather, culture services)
- ✅ Phase 6A-E: AI Worker full implementation (OpenAI, prompts, parsing, validation, API, testing)
- ✅ Phase 7: Services connected (HTTP client, error handling, E2E testing)

**Next Steps** (from `docs/smartpacking/project/roadmap.md`):
1. **Phase 8**: Docker (Dockerfiles, docker-compose.yml)
2. **Phase 9**: Kubernetes (manifests in `k8s/`, 2+ services)
3. **Phase 10**: Frontend (React, optional)
4. **Phase 11**: Documentation (README with 10 questions)
5. **Phase 12**: Pitch (1-3 min audio/video)

**Documentation**:
- `POSTMAN_TESTING_GUIDE.md`: API testing with Postman
- `services/api-gateway/END_TO_END_TESTING.md`: Full system testing
- `services/ai-worker/API_DOCUMENTATION.md`: AI Worker API reference
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
```bash
# Build and start all services
docker compose up -d

# Check logs
docker compose logs -f

# Stop
docker compose down
```

### Kubernetes Deployment
**Required Manifests** (in `k8s/` directory):
- `namespace.yaml` - Dedicated namespace
- `postgres-secret.yaml` - DB credentials (base64-encoded)
- `postgres-pvc.yaml` - Persistent volume claim
- `postgres-deployment.yaml`, `postgres-service.yaml`
- `api-gateway-deployment.yaml`, `api-gateway-service.yaml`
- `ai-worker-deployment.yaml`, `ai-worker-service.yaml`
- `frontend-deployment.yaml`, `frontend-service.yaml` (optional)

**Deploy**:
```bash
kind create cluster
kubectl apply -f k8s/
kubectl get pods -w
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

## Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring AI**: https://docs.spring.io/spring-ai/reference/
- **Kotlin**: https://kotlinlang.org/docs/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Docker**: https://docs.docker.com/
- **Kubernetes**: https://kubernetes.io/docs/

## Contact

- **University Project**: alkurdiz@htw-berlin.de
- **Security Issues**: [Define for production]
