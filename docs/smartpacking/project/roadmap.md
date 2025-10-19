# Smart Packing Assistant - Project Roadmap

## Phase 1: Setup & Fundamentals ✅

- [x] Select topic (Smart Packing Assistant)
- [x] Plan project structure
- [x] Install PostgreSQL 15 locally
- [x] Set up pgAdmin 4
- [x] Create database "packing_assistant"
- [x] Design database schema
- [x] Create schema in local database
- [x] Initialize API Gateway with Spring Initializr
- [x] Initialize AI Worker with Spring Initializr
- [x] Create basic documentation (fundamental.md)

---

## Phase 2: Shared Module ✅

- [x] Create Shared Module folder structure
- [x] Configure `build.gradle.kts` for Shared Module
- [x] Define enums (TravelType, Season)
- [x] Create DTOs:
    - [x] PackingRequest.kt
    - [x] PackingResponse.kt
    - [x] PackingItem.kt
    - [x] WeatherInfo.kt (in model/ folder)
- [x] Adjust root `settings.gradle.kts` (include all 3 modules)
- [x] Include Shared Module as dependency in API Gateway
- [x] Include Shared Module as dependency in AI Worker
- [x] Test Shared Module build (Kotlin version aligned to 1.9.25)

---

## Phase 3: API Gateway - Database Layer ✅

- [x] Flyway Migration setup in `build.gradle.kts`
- [x] Create first migration (V1__initial_schema.sql)
- [x] Create JPA Entities:
    - [x] Session.kt (Session management)
    - [x] PackingList.kt (Main entity with JSONB and TEXT[] support)
    - [x] ChatMessage.kt (Chat history)
    - [x] Configure annotations (@Entity, @Table, @Id, etc.)
- [x] Create Repository Interfaces:
    - [x] SessionRepository.kt (with custom queries)
    - [x] PackingListRepository.kt (with search methods)
    - [x] ChatMessageRepository.kt (for chat history)
- [x] `application.properties` Database configuration (PostgreSQL credentials, JPA, Flyway)
- [x] Flyway Migration deployed (disabled for local, enabled for Docker/K8s)
- [x] Verify repository with tests (Unit + Integration tests)
- [x] Application started successfully (Port 8080, Health check: UP)

---

## Phase 4: API Gateway - REST Layer ✅

- [x] Create PackingController.kt
- [x] Implement POST /api/packing/generate endpoint
- [x] Add request validation (@Valid, @NotNull)
- [x] GET /api/packing/{id} endpoint for saved lists
- [x] GET /api/packing/session endpoint for session lists
- [x] GET /api/packing/session/recent endpoint with limit
- [x] GET /api/packing/search endpoint for destination search
- [x] GET /api/packing/health endpoint (Health Check)
- [x] Create PackingListService.kt (Business Logic)
- [x] Create SessionService.kt (Session Management)
- [x] Create SessionController.kt (Session Endpoints)
- [x] Create AiWorkerClient.kt (Mock Implementation for AI Worker)
- [x] Create RestTemplateConfig.kt
- [x] Implement error handling (GlobalExceptionHandler)
- [x] Create custom exceptions
- [x] Test REST endpoints (curl)
- [x] Verify database persistence

---

## Phase 5: AI Worker - Dummy Data ✅

- [x] Create folder `src/main/resources/data/`
- [x] Create `weather_data.json` (dummy weather data)
- [x] Create `culture_tips.json` (dummy culture tips)
- [x] Implement WeatherService.kt (reads JSON)
- [x] Implement CultureService.kt (reads JSON)
- [x] Test services
- [x] Configure application.properties (Port 8081, file locations, Actuator)
- [x] Create data models (WeatherData.kt, CultureTip.kt)
- [x] Successful build (./gradlew build)
- [x] Application started (Port 8081, Health: UP)
- [x] Verify data loading (3 locations, 12 weather entries, 9 culture tips)

---

## Phase 6A: AI Worker - Spring AI Setup & Configuration ✅

- [x] Get OpenAI API Key from instructor (configured in .env file via spring-dotenv)
- [x] **Migrated to Spring AI** from openai-gpt3-java for better Spring Boot integration
- [x] Add Spring AI dependency in `build.gradle.kts` (spring-ai-openai-spring-boot-starter:1.0.0-M4)
- [x] Add Spring Milestone Repository for Spring AI access
- [x] Add spring-dotenv dependency (me.paulschwarz:spring-dotenv:4.0.0)
- [x] Configure API Key in `application.properties` as environment variable (spring.ai.openai.api-key)
- [x] .env file loaded with spring-dotenv library (symlink created in ai-worker directory)
- [x] Refactor OpenAiConfig.kt for Spring AI (ChatClient bean, auto-configuration)
- [x] Implement basic connection test with ChatClient (OpenAiHealthService.kt)
- [x] Test configuration (API Key validation)
- [x] Verify: Config loads successfully, ChatClient initialized
- [x] Successful build (./gradlew build)
- [x] Service started (Port 8081, Health: UP)
- [x] OpenAI connection verified with test API call (response: "OK")

---

## Phase 6B: AI Worker - Prompt Engineering ✅

- [x] Create PromptService.kt (comprehensive service with weather and culture integration)
- [x] Define System Prompt:
    - [x] Anti-Hallucination Guidelines (reliability over creativity, no inventions)
    - [x] Output Format Specification (structured JSON with 5 categories)
    - [x] Reliability over Creativity (temperature 0.3, admit uncertainty)
- [x] Create User Prompt Template:
    - [x] Destination Parameter (with normalization)
    - [x] Duration Parameter (realistic quantities)
    - [x] Season Parameter (weather-based recommendations)
    - [x] Travel Type Parameter (BUSINESS, VACATION, BACKPACKING specific instructions)
- [x] Implement Prompt Builder Method (buildPackingListPrompt with full context)
- [x] Configure temperature 0.3 (already set in application.properties)
- [x] Test prompts with sample inputs (6 comprehensive tests covering all scenarios)
- [x] Verify: Prompts generate correctly with all context (weather, culture tips, travel type)

---

## Phase 6C: AI Worker - AI Service Core Implementation ✅

- [x] Create AiService.kt (comprehensive service with logging and metrics)
- [x] Initialize OpenAI Client (inject ChatClient from Spring AI config)
- [x] Create ChatRequest (integrate PromptService with system and user prompts)
- [x] Implement basic API call (using Spring AI fluent API)
- [x] Add raw response logging (comprehensive logging at multiple levels: INFO, DEBUG, TRACE)
- [x] Test with simple requests (created AiServiceTest with 5 test scenarios)
- [x] Verify: API calls work, responses received (build successful, tests pass)

---

## Phase 6D: AI Worker - Response Processing & Error Handling ✅

- [x] Implement response parsing (JSON extraction from AI response)
- [x] Add JSON validation (schema validation)
- [x] Create data model for AI response
- [x] Verify hallucination prevention
- [x] Implement fallback logic for errors:
    - [x] API Timeout Handling
    - [x] Invalid JSON Handling
    - [x] Rate Limit Handling
    - [x] Fallback to Dummy Data when necessary
- [x] Exception handling (comprehensive error scenarios)
- [x] Integration with WeatherService
- [x] Integration with CultureService
- [x] Verify: Responses parsed correctly, all errors handled gracefully

---

## Phase 6E: AI Worker - REST API & Testing ✅

- [x] Create AiController.kt
- [x] Implement POST /api/ai/generate endpoint
- [x] Validate request DTO (@Valid annotations)
- [x] Format response DTO
- [x] Format error responses (proper HTTP status codes)
- [x] Add performance logging (generation time tracking)
- [x] Test AI Worker locally (Port 8081)
- [x] End-to-End test with curl/Postman:
    - [x] Test valid request (Iceland, 5 days, WINTER, VACATION)
    - [x] Test invalid destination
    - [x] Test API error scenarios
- [x] Document API endpoints
- [x] Verify: Endpoint works end-to-end, returns valid packing lists

---

## Phase 7: Connect Services ✅

- [x] API Gateway: Configure RestTemplate for AI Worker
- [x] API Gateway: Set service URL in `application.properties`
- [x] Implement end-to-end flow:
    - [x] Receive request in API Gateway
    - [x] Forward request to AI Worker via HTTP
    - [x] Receive response from AI Worker
    - [x] Save in database (existing functionality)
    - [x] Return response to client (existing functionality)
- [x] Remove MockAiWorkerClient, implement RealAiWorkerClient
- [x] Comprehensive error handling (connection failures, 4xx, 5xx errors)
- [x] Build verification (API Gateway compiles successfully)
- [x] Create END_TO_END_TESTING.md with:
    - [x] Service startup instructions
    - [x] 9 test scenarios (3 successful flows, 3 error cases)
    - [x] Database verification queries
    - [x] Performance monitoring
    - [x] Troubleshooting guide

---

## Phase 8: Docker

- [ ] Create Dockerfile for API Gateway
- [ ] Create Dockerfile for AI Worker
- [ ] Create Dockerfile for Frontend (optional)
- [ ] Create `.dockerignore` files
- [ ] Create `docker-compose.yml` with:
    - [ ] PostgreSQL Service
    - [ ] API Gateway Service
    - [ ] AI Worker Service
    - [ ] Frontend Service (optional)
    - [ ] Network configuration
    - [ ] Volume for PostgreSQL
    - [ ] Environment Variables
- [ ] Build Docker images locally
- [ ] Test `docker compose up -d`
- [ ] Test services in containers
- [ ] Check logs

---

## Phase 9: Kubernetes

- [ ] Create `k8s/` folder
- [ ] Create namespace (namespace.yaml)
- [ ] PostgreSQL Setup:
    - [ ] postgres-secret.yaml (Credentials)
    - [ ] postgres-pvc.yaml (Persistent Volume Claim)
    - [ ] postgres-deployment.yaml
    - [ ] postgres-service.yaml
- [ ] API Gateway Setup:
    - [ ] api-gateway-deployment.yaml
    - [ ] api-gateway-service.yaml
- [ ] AI Worker Setup:
    - [ ] ai-worker-deployment.yaml
    - [ ] ai-worker-service.yaml
- [ ] Frontend Setup (optional):
    - [ ] frontend-deployment.yaml
    - [ ] frontend-service.yaml
- [ ] Start Kind cluster
- [ ] Create namespace: `kubectl apply -f k8s/namespace.yaml`
- [ ] Deploy all manifests: `kubectl apply -f k8s/`
- [ ] Check pods status: `kubectl get pods`
- [ ] Check services: `kubectl get services`
- [ ] Test port-forwarding
- [ ] End-to-end test in cluster

---

## Phase 10: Frontend (Optional)

- [ ] Initialize React + TypeScript + Vite project
- [ ] Set up TailwindCSS
- [ ] Install Axios for API calls
- [ ] Create components:
    - [ ] PackingForm.tsx (input form)
    - [ ] PackingList.tsx (result display)
    - [ ] History.tsx (saved lists)
- [ ] Create API service (api.ts)
- [ ] Routing with React Router
- [ ] Styling
- [ ] Local testing
- [ ] Dockerfile for Frontend
- [ ] Integrate in docker-compose.yml
- [ ] Deploy in Kubernetes

---

## Phase 11: Documentation

- [ ] Create README.md (max. 220 lines)
- [ ] Question 1: Write Executive Summary
- [ ] Question 2: Describe project goals
- [ ] Question 3: Explain application and usage
    - [ ] Insert GitHub Repository link
    - [ ] Insert Pitch link/file
- [ ] Question 4: Document development status
- [ ] Question 5: List project details and core features
- [ ] Question 6: Highlight innovation
- [ ] Question 7: Describe impact
- [ ] Question 8: Explain technical excellence
- [ ] Question 9: Address ethics, transparency, and inclusion
- [ ] Question 10: Outline future vision
- [ ] Check line count (max. 220)
- [ ] Create API documentation (docs/api-documentation.md)
- [ ] Update architecture diagram (docs/architecture.md)
- [ ] Review code comments

---

## Phase 12: Pitch

- [ ] Write pitch script (1-3 minutes)
- [ ] Define core points:
    - [ ] Problem
    - [ ] Solution
    - [ ] Technology
    - [ ] Innovation
    - [ ] Impact
- [ ] Record audio (preferred) OR create video
- [ ] Check file size (max. 25 MB)
- [ ] Save pitch in repository (`pitch.mp3` or `pitch.mp4`)
- [ ] Check quality (clarity, volume)

---

## Phase 13: Testing & Quality Assurance (optional)

- [ ] Write unit tests for services
- [ ] Integration tests for API endpoints
- [ ] Test Docker setup (clean build)
- [ ] Test Kubernetes deployment (clean cluster)
- [ ] Read and correct README
- [ ] Check code formatting
- [ ] Add comments where necessary
- [ ] Remove all TODOs in code
- [ ] Remove secrets from code (only Environment Variables)

---

## Phase 14: Prepare Submission

- [ ] Clean up GitHub Repository
- [ ] Commit all files
- [ ] Copy repository link
- [ ] Final check of README (220 lines limit)
- [ ] Final check of pitch file (25 MB limit)
- [ ] Check all submission requirements against checklist:
    - [ ] Dockerfile present
    - [ ] docker-compose.yml present
    - [ ] k8s/ folder with at least 2 services
    - [ ] README.md with 10 questions
    - [ ] Pitch file
    - [ ] Code repository link works
- [ ] Register group at Moodle (https://moodle.htw-berlin.de/mod/wiki/view.php?id=2035127)
- [ ] Prepare submission via email to alkurdiz@htw-berlin.de

---

## Phase 15: Submission

- [ ] Send repository link via email
- [ ] Upload/link pitch
- [ ] Wait for confirmation from instructor
- [ ] Create backup of entire project

---
