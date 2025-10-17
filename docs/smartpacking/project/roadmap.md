# Smart Packing Assistant - Projekt Roadmap

## Phase 1: Setup & Grundlagen ✅

- [x] Thema auswählen (Smart Packing Assistant)
- [x] Projektstruktur planen
- [x] PostgreSQL 15 lokal installieren
- [x] pgAdmin 4 einrichten
- [x] Datenbank "packing_assistant" erstellen
- [x] Datenbankschema entwerfen
- [x] Schema in lokaler Datenbank anlegen
- [x] API Gateway mit Spring Initializr initialisieren
- [x] AI Worker mit Spring Initializr initialisieren
- [x] Grundlegende Dokumentation erstellen (fundamental.md)

---

## Phase 2: Shared Module ✅

- [x] Shared Module Ordnerstruktur erstellen
- [x] `build.gradle.kts` für Shared Module konfigurieren
- [x] Enums definieren (TravelType, Season)
- [x] DTOs erstellen:
    - [x] PackingRequest.kt
    - [x] PackingResponse.kt
    - [x] PackingItem.kt
    - [x] WeatherInfo.kt (in model/ folder)
- [x] Root `settings.gradle.kts` anpassen (alle 3 Module einbinden)
- [x] Shared Module in API Gateway als Dependency einbinden
- [x] Shared Module in AI Worker als Dependency einbinden
- [x] Shared Module Build testen (Kotlin version aligned to 1.9.25)

---

## Phase 3: API Gateway - Database Layer ✅

- [x] Flyway Migration Setup in `build.gradle.kts`
- [x] Erste Migration erstellen (V1__initial_schema.sql)
- [x] JPA Entities erstellen:
    - [x] Session.kt (Session management)
    - [x] PackingList.kt (Main entity with JSONB and TEXT[] support)
    - [x] ChatMessage.kt (Chat history)
    - [x] Annotations konfigurieren (@Entity, @Table, @Id, etc.)
- [x] Repository Interfaces erstellen:
    - [x] SessionRepository.kt (with custom queries)
    - [x] PackingListRepository.kt (with search methods)
    - [x] ChatMessageRepository.kt (for chat history)
- [x] `application.properties` Database-Konfiguration (PostgreSQL credentials, JPA, Flyway)
- [x] Flyway Migration bereitgestellt (disabled for local, enabled for Docker/K8s)
- [x] Repository mit Test verifizieren (Unit + Integration tests)
- [x] Application erfolgreich gestartet (Port 8080, Health check: UP)

---

## Phase 4: API Gateway - REST Layer ✅

- [x] PackingController.kt erstellen
- [x] POST /api/packing/generate Endpoint implementieren
- [x] Request Validation einbauen (@Valid, @NotNull)
- [x] GET /api/packing/{id} Endpoint für gespeicherte Listen
- [x] GET /api/packing/session Endpoint für Session-Listen
- [x] GET /api/packing/session/recent Endpoint mit Limit
- [x] GET /api/packing/search Endpoint für Destinationssuche
- [x] GET /api/packing/health Endpoint (Health Check)
- [x] PackingListService.kt erstellen (Business Logic)
- [x] SessionService.kt erstellen (Session Management)
- [x] SessionController.kt erstellen (Session Endpoints)
- [x] AiWorkerClient.kt erstellen (Mock Implementation für AI Worker)
- [x] RestTemplateConfig.kt erstellen
- [x] Error Handling implementieren (GlobalExceptionHandler)
- [x] Custom Exceptions erstellen
- [x] REST Endpoints getestet (curl)
- [x] Database Persistence verifiziert

---

## Phase 5: AI Worker - Dummy-Daten ✅

- [x] Ordner `src/main/resources/data/` erstellen
- [x] `weather_data.json` erstellen (Dummy-Wetterdaten)
- [x] `culture_tips.json` erstellen (Dummy-Kultur-Tipps)
- [x] WeatherService.kt implementieren (liest JSON)
- [x] CultureService.kt implementieren (liest JSON)
- [x] Services testen
- [x] application.properties konfiguriert (Port 8081, Datei-Locations, Actuator)
- [x] Data Models erstellt (WeatherData.kt, CultureTip.kt)
- [x] Build erfolgreich (./gradlew build)
- [x] Application gestartet (Port 8081, Health: UP)
- [x] Data loading verifiziert (3 locations, 12 weather entries, 9 culture tips)

---

## Phase 6A: AI Worker - Spring AI Setup & Configuration ✅

- [x] OpenAI API Key vom Dozenten besorgen (configured in .env file via spring-dotenv)
- [x] **Migrated to Spring AI** from openai-gpt3-java for better Spring Boot integration
- [x] Spring AI Dependency in `build.gradle.kts` hinzufügen (spring-ai-openai-spring-boot-starter:1.0.0-M4)
- [x] Spring Milestone Repository hinzugefügt für Spring AI access
- [x] spring-dotenv Dependency hinzufügen (me.paulschwarz:spring-dotenv:4.0.0)
- [x] API Key in `application.properties` als Environment Variable konfigurieren (spring.ai.openai.api-key)
- [x] .env file loaded with spring-dotenv library (symlink created in ai-worker directory)
- [x] OpenAiConfig.kt refactored for Spring AI (ChatClient bean, auto-configuration)
- [x] Basic Connection Test implementieren mit ChatClient (OpenAiHealthService.kt)
- [x] Configuration testen (API Key validation)
- [x] Verify: Config loads successfully, ChatClient initialized
- [x] Build erfolgreich (./gradlew build)
- [x] Service gestartet (Port 8081, Health: UP)
- [x] OpenAI connection verified with test API call (response: "OK")

---

## Phase 6B: AI Worker - Prompt Engineering

- [ ] PromptService.kt erstellen
- [ ] System Prompt definieren:
    - [ ] Anti-Hallucination Guidelines
    - [ ] Output Format Specification (JSON)
    - [ ] Reliability over Creativity
- [ ] User Prompt Template erstellen:
    - [ ] Destination Parameter
    - [ ] Duration Parameter
    - [ ] Season Parameter
    - [ ] Travel Type Parameter
- [ ] Prompt Builder Method implementieren
- [ ] Temperature 0.3 konfigurieren
- [ ] Test Prompts mit Sample-Inputs
- [ ] Verify: Prompts generate correctly with all context

---

## Phase 6C: AI Worker - AI Service Core Implementation

- [ ] AiService.kt erstellen
- [ ] OpenAI Client initialisieren (inject from config)
- [ ] ChatRequest erstellen (integrate PromptService)
- [ ] Basic API Call implementieren
- [ ] Raw Response Logging hinzufügen
- [ ] Test mit einfachen Requests
- [ ] Verify: API calls work, responses received

---

## Phase 6D: AI Worker - Response Processing & Error Handling

- [ ] Response Parsing implementieren (JSON extraction from AI response)
- [ ] JSON Validation einbauen (schema validation)
- [ ] Data Model für AI Response erstellen
- [ ] Halluzination-Vermeidung verifizieren
- [ ] Fallback-Logik bei Fehlern implementieren:
    - [ ] API Timeout Handling
    - [ ] Invalid JSON Handling
    - [ ] Rate Limit Handling
    - [ ] Fallback to Dummy Data wenn nötig
- [ ] Exception Handling (comprehensive error scenarios)
- [ ] Integration mit WeatherService
- [ ] Integration mit CultureService
- [ ] Verify: Responses parsed correctly, all errors handled gracefully

---

## Phase 6E: AI Worker - REST API & Testing

- [ ] AiController.kt erstellen
- [ ] POST /api/ai/generate Endpoint implementieren
- [ ] Request DTO validieren (@Valid annotations)
- [ ] Response DTO formatieren
- [ ] Error Responses formatieren (proper HTTP status codes)
- [ ] Performance Logging hinzufügen (generation time tracking)
- [ ] AI Worker lokal testen (Port 8081)
- [ ] End-to-End Test mit curl/Postman:
    - [ ] Test valid request (Iceland, 5 days, WINTER, VACATION)
    - [ ] Test invalid destination
    - [ ] Test API error scenarios
- [ ] Documentation der API Endpoints
- [ ] Verify: Endpoint works end-to-end, returns valid packing lists

---

## Phase 7: Services verbinden

- [ ] API Gateway: RestTemplate für AI Worker konfigurieren
- [ ] API Gateway: Service-URL in `application.yml` setzen
- [ ] End-to-End Flow implementieren:
    - [ ] Request im API Gateway empfangen
    - [ ] Request an AI Worker weiterleiten
    - [ ] Response von AI Worker empfangen
    - [ ] In Database speichern
    - [ ] Response an Client zurückgeben
- [ ] Beide Services parallel lokal starten
- [ ] End-to-End Test durchführen (z.B. mit curl oder Postman)
- [ ] Error Handling zwischen Services testen

---

## Phase 8: Docker

- [ ] Dockerfile für API Gateway erstellen
- [ ] Dockerfile für AI Worker erstellen
- [ ] Dockerfile für Frontend erstellen (optional)
- [ ] `.dockerignore` Dateien erstellen
- [ ] `docker-compose.yml` erstellen mit:
    - [ ] PostgreSQL Service
    - [ ] API Gateway Service
    - [ ] AI Worker Service
    - [ ] Frontend Service (optional)
    - [ ] Netzwerk-Konfiguration
    - [ ] Volume für PostgreSQL
    - [ ] Environment Variables
- [ ] Docker Images lokal bauen
- [ ] `docker compose up -d` testen
- [ ] Services im Container testen
- [ ] Logs prüfen

---

## Phase 9: Kubernetes

- [ ] `k8s/` Ordner erstellen
- [ ] Namespace erstellen (namespace.yaml)
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
- [ ] Kind-Cluster starten
- [ ] Namespace anlegen: `kubectl apply -f k8s/namespace.yaml`
- [ ] Alle Manifeste deployen: `kubectl apply -f k8s/`
- [ ] Pods Status prüfen: `kubectl get pods`
- [ ] Services prüfen: `kubectl get services`
- [ ] Port-Forwarding testen
- [ ] End-to-End Test im Cluster

---

## Phase 10: Frontend (Optional)

- [ ] React + TypeScript + Vite Projekt initialisieren
- [ ] TailwindCSS einrichten
- [ ] Axios für API-Calls installieren
- [ ] Components erstellen:
    - [ ] PackingForm.tsx (Input-Formular)
    - [ ] PackingList.tsx (Ergebnis-Anzeige)
    - [ ] History.tsx (Gespeicherte Listen)
- [ ] API Service erstellen (api.ts)
- [ ] Routing mit React Router
- [ ] Styling
- [ ] Lokales Testing
- [ ] Dockerfile für Frontend
- [ ] In docker-compose.yml integrieren
- [ ] In Kubernetes deployen

---

## Phase 11: Dokumentation

- [ ] README.md erstellen (max. 220 Zeilen)
- [ ] Frage 1: Executive Summary schreiben
- [ ] Frage 2: Ziele des Projekts beschreiben
- [ ] Frage 3: Anwendung und Nutzung erklären
    - [ ] GitHub Repository Link einfügen
    - [ ] Pitch Link/Datei einfügen
- [ ] Frage 4: Entwicklungsstand dokumentieren
- [ ] Frage 5: Projektdetails und Kernfunktionen auflisten
- [ ] Frage 6: Innovation hervorheben
- [ ] Frage 7: Wirkung (Impact) beschreiben
- [ ] Frage 8: Technische Exzellenz erläutern
- [ ] Frage 9: Ethik, Transparenz und Inklusion adressieren
- [ ] Frage 10: Zukunftsvision skizzieren
- [ ] Zeilenzahl prüfen (max. 220)
- [ ] API-Dokumentation erstellen (docs/api-documentation.md)
- [ ] Architektur-Diagramm aktualisieren (docs/architecture.md)
- [ ] Code-Kommentare überprüfen

---

## Phase 12: Pitch

- [ ] Pitch-Skript schreiben (1-3 Minuten)
- [ ] Kernpunkte definieren:
    - [ ] Problem
    - [ ] Lösung
    - [ ] Technologie
    - [ ] Innovation
    - [ ] Impact
- [ ] Audio aufnehmen (bevorzugt) ODER Video erstellen
- [ ] Dateigröße prüfen (max. 25 MB)
- [ ] Pitch in Repository speichern (`pitch.mp3` oder `pitch.mp4`)
- [ ] Qualität überprüfen (Verständlichkeit, Lautstärke)

---

## Phase 13: Testing & Quality Assurance (optional)

- [ ] Unit Tests für Services schreiben
- [ ] Integration Tests für API Endpoints
- [ ] Docker Setup testen (clean build)
- [ ] Kubernetes Deployment testen (clean cluster)
- [ ] README durchlesen und korrigieren
- [ ] Code-Formatierung prüfen
- [ ] Kommentare ergänzen wo nötig
- [ ] Alle TODOs im Code entfernen
- [ ] Secrets aus Code entfernen (nur Environment Variables)

---

## Phase 14: Abgabe vorbereiten

- [ ] GitHub Repository aufräumen
- [ ] Alle Dateien committen
- [ ] Repository-Link kopieren
- [ ] README final checken (220 Zeilen Limit)
- [ ] Pitch-Datei final checken (25 MB Limit)
- [ ] Alle Abgabe-Anforderungen gegen Checklist prüfen:
    - [ ] Dockerfile vorhanden
    - [ ] docker-compose.yml vorhanden
    - [ ] k8s/ Ordner mit mindestens 2 Services
    - [ ] README.md mit 10 Fragen
    - [ ] Pitch-Datei
    - [ ] Code-Repository Link funktioniert
- [ ] Gruppe bei Moodle eintragen (https://moodle.htw-berlin.de/mod/wiki/view.php?id=2035127)
- [ ] Abgabe per Email an alkurdiz@htw-berlin.de vorbereiten

---

## Phase 15: Abgabe

- [ ] Repository Link per Email senden
- [ ] Pitch hochladen/verlinken
- [ ] Bestätigung vom Dozenten abwarten
- [ ] Backup des gesamten Projekts erstellen

---