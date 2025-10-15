## Vision

Ein KI-gestützter Assistent, der personalisierte Packlisten für Reisen erstellt. Nutzer geben Reiseziel, Dauer, Jahreszeit und Reiseart an – die KI generiert eine maßgeschneiderte Packliste mit Begründungen.

## Kernfunktionen

### MVP (Minimum Viable Product)

1. **Basis-Packliste generieren**
    - Input: Zielort, Reisedauer, Jahreszeit, Reiseart (Business/Urlaub/Backpacking)
    - Output: Kategorisierte Packliste (Kleidung, Technik, Hygiene, Dokumente, Sonstiges)
2. **Kontextbewusste Empfehlungen**
    - Wetter-Simulation berücksichtigen
    - Kulturelle Besonderheiten (z.B. "In Dubai: Schultern bedecken")
    - Aktivitäten-spezifisch (Wandern → Wanderschuhe)
3. **Nachfragen beantworten**
    - Chat-Interface: "Brauche ich einen Adapter?" → "Ja, Island nutzt Typ F"

### Optional (bei mehr Zeit)

- PDF-Export der Packliste
- Gewichts-Schätzung für Handgepäck
- Einkaufsliste für fehlende Items

## Technische Architektur

```
┌─────────────────────────────────────────────┐
│  Frontend (React + TypeScript + Vite)       │
│  Port: 5173                                 │
│  - UI für User                              │
└────────────────┬────────────────────────────┘
                 │ HTTP
                 ▼
┌─────────────────────────────────────────────┐
│  API Gateway (Kotlin + Gradle + Spring Boot)│  Service 1
│  Port: 8080                                 │
│  - REST API                                 │
│  - Input Validation                         │
│  - Database Operations (PostgreSQL)         │
│  - Session Management                       │
│  - Business Logic                           │
└────────────────┬────────────────────────────┘
                 │ HTTP (Internal)
                 ▼
┌─────────────────────────────────────────────┐
│  AI Worker (Kotlin + Gradle + Spring Boot)  │  Service 2
│  Port: 8081                                 │
│  - OpenAI Integration                       │
│  - Prompt Engineering                       │
│  - AI Response Parsing                      │
│  - Dummy-Daten Management                   │
└─────────────────────────────────────────────┘

         ┌───────────────┐
         │  PostgreSQL   │  Service 3
         │  Port: 5432   │
         └───────────────┘
                ▲
                │
    Nur API Gateway greift zu!
```

## Prompt-Engineering Beispiel

```
System: Du bist ein erfahrener Reiseexperte. Erstelle eine
präzise Packliste basierend auf folgenden Kriterien:

User Input:
- Ziel: Island
- Dauer: 5 Tage
- Saison: März
- Reiseart: Urlaub/Natur

Aufgabe:
1. Erstelle eine kategorisierte Packliste
2. Begründe wichtige Items kurz
3. Warne vor klimatischen Besonderheiten
4. Gib kulturelle Hinweise

Format: JSON mit Kategorien [Kleidung, Technik, Hygiene,
Dokumente, Sonstiges]

```

### Halluzinations-Vermeidung

- Klare Anweisungen: "Nur Items auflisten, keine Erfindungen"
- Temperature: 0.3 (weniger kreativ = zuverlässiger)
- Fallback: "Ich bin mir nicht sicher" bei unbekannten Zielen
- Validierung: Prüfe ob Antwort dem Schema entspricht

## Repository-Struktur

```markdown
smart-packing-assistant/
│
├── README.md
├── pitch.mp3
├── docker-compose.yml
├── settings.gradle.kts              # Multi-Project Build
│
├── services/
│   ├── shared/                      # Gemeinsames Modul
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       └── main/
│   │           └── kotlin/
│   │               └── com/smartpacking/shared/
│   │                   ├── dto/
│   │                   │   ├── PackingRequest.kt
│   │                   │   └── PackingResponse.kt
│   │                   ├── enums/
│   │                   │   ├── TravelType.kt
│   │                   │   └── Season.kt
│   │                   └── model/
│   │                       └── WeatherInfo.kt
│   │
│   ├── api-gateway/                 # Kotlin Spring Boot
│   │   ├── Dockerfile
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       │   ├── kotlin/
│   │       │   │   └── com/smartpacking/api/
│   │       │   │       ├── ApiGatewayApplication.kt
│   │       │   │       ├── controller/
│   │       │   │       │   └── PackingController.kt
│   │       │   │       ├── service/
│   │       │   │       │   ├── PackingListService.kt
│   │       │   │       │   └── AiWorkerClient.kt
│   │       │   │       ├── repository/
│   │       │   │       │   └── PackingListRepository.kt
│   │       │   │       ├── entity/
│   │       │   │       │   └── PackingList.kt
│   │       │   │       └── config/
│   │       │   │           └── RestTemplateConfig.kt
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── db/migration/
│   │       │           └── V1__initial_schema.sql
│   │       └── test/
│   │
│   ├── ai-worker/                   # Kotlin Spring Boot
│   │   ├── Dockerfile
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       │   ├── kotlin/
│   │       │   │   └── com/smartpacking/ai/
│   │       │   │       ├── AiWorkerApplication.kt
│   │       │   │       ├── controller/
│   │       │   │       │   └── AiController.kt
│   │       │   │       ├── service/
│   │       │   │       │   ├── AiService.kt
│   │       │   │       │   ├── PromptService.kt
│   │       │   │       │   ├── WeatherService.kt
│   │       │   │       │   └── CultureService.kt
│   │       │   │       └── config/
│   │       │   │           └── OpenAiConfig.kt
│   │       │   └── resources/
│   │       │       ├── application.yml
│   │       │       └── data/
│   │       │           ├── weather_data.json
│   │       │           └── culture_tips.json
│   │       └── test/
│   │
│   └── frontend/                    # React TypeScript
│       ├── Dockerfile
│       ├── package.json
│       ├── tsconfig.json
│       ├── vite.config.ts
│       └── src/
│           ├── components/
│           ├── services/
│           └── App.tsx
│
├── k8s/
│   ├── namespace.yaml
│   ├── postgres-secret.yaml
│   ├── postgres-pvc.yaml
│   ├── postgres-deployment.yaml
│   ├── postgres-service.yaml
│   ├── api-gateway-deployment.yaml
│   ├── api-gateway-service.yaml
│   ├── ai-worker-deployment.yaml
│   ├── ai-worker-service.yaml
│   ├── frontend-deployment.yaml
│   └── frontend-service.yaml
│
└── docs/
├── architecture.md
└── api-documentation.md
```

## Techstack

```markdown
API Gateway:
  - Kotlin 1.9.25
  - Gradle
  - Spring Boot 3.5.6
  - JVM 21

    Dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Flyway Migration
  - Validation
  - Spring Boot DevTools
  - Spring Configuration Processor
  - Spring Boot Actuator
  
AI Worker:
  - Kotlin 1.9.25
  - Gradle
  - Spring Boot 3.5.6
  - JVM 21
    
    Dependencies:
    - Spring Web
    - Validation
    - Spring Boot DevTools
    - Spring Configuration Processor
    - Spring Boot Actuator

Frontend:
  - React 18.3.1
  - TypeScript 5.9.3
  - Vite 5.x  
  - Axios 1.6.x
  - React Router
  - TailwindCSS

Database:
  - Database: PostgreSQL 15
  - Container: postgres:15-alpine
  - ORM: Spring Data JPA / Hibernate
  - Migrations: Flyway
```