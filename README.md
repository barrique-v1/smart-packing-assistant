# Smart Packing Assistant

---

## 1. Executive Summary : Kurze Zusammenfassung des Projekts

Smart Packing Assistant ist eine intelligente Microservices-Anwendung, die Reisenden beim Packen hilft. Das System 
generiert personalisierte Packlisten basierend auf Reiseziel, Dauer, Jahreszeit und Reiseart (Business, Urlaub, Backpacking).
Durch KI-gestützte Analysen mit OpenAI GPT-5 erhalten Nutzer:innen individuell angepasste Packlisten für ihre Reise.
Die Anwendung nutzt moderne Cloud-Technologien (Docker, Kubernetes) und eine Microservices-Architektur mit
React-Frontend, Spring Boot-Backend und PostgreSQL-Datenbank.
Das Projekt demonstriert den praktischen Einsatz von künstlicher Intelligenz zur Lösung eines alltäglichen Problems und 
kombiniert dabei AI-Integration, Container-Orchestrierung und sichere Session-Verwaltung.
Die Lösung ist vollständig lokal deploybar und produktionsreif skalierbar.

## 2. Ziele des Projekts : Welche Ziele verfolgt Ihr Projekt, welches Problem wird gelöst?

Das Hauptziel ist die Entwicklung eines KI-gestützten Assistenten, der das Problem ineffizienter und unvollständiger 
Reisevorbereitungen löst.
Viele Reisende vergessen wichtige Gegenstände oder packen unnötige ein, was zu Stress und zusätzlichen Kosten führt.
Dieses Problem löst der Smart-Packing-Assistant, er sorgt für die Bereitstellung personalisierter, kontextbewusster 
Packlisten, die Destination, Wetter und Aktivitäten berücksichtigen.

## 3. Anwendung und Nutzung : Wie wird die Lösung verwendet, wer sind die Hauptnutzer:innen?

Die Lösung richtet sich an drei Hauptnutzergruppen: Geschäftsreisende, Urlauber und Backpacker.
Die Nutzung erfolgt über eine Weboberfläche: Nutzer geben Reiseziel (z.B. Paris), Dauer (5 Tage), Jahreszeit (Frühling)
und Reiseart (Urlaub) ein.
Der Smart Packing Assistant generiert darauf innerhalb kurzer Zeit eine kategorisierte Packliste, speziell für die 
eingegebenen Anforderungen.

**Code-Repository:** https://github.com/your-username/smart-packing-assistant
**Pitch:** [pitch.mp3](./pitch.mp3) (Audio, 1 - 3 Min)

## 4. Entwicklungsstand : Idee, Proof of Concept, Prototyp oder Einsatzbereit?

Das Projekt ist ein fertiger Prototyp. Alle Kernfunktionen sind vollständig implementiert und getestet: AI-Integration 
mit OpenAI GPT-5, PostgreSQL-Persistenz, Session-Management, Docker-Containerisierung und Kubernetes-Deployment.
Die Anwendung läuft stabil in drei Deployment-Szenarien: lokal (entwicklung), Docker Compose (Integration) und 
Kubernetes (Produktion). Das System verfügt über Health Checks, Fehlerbehandlung und Input-Validierung.
Für echte Produktionsumgebungen fehlen unter anderem noch HTTPS/TLS-Verschlüsselung, Authentifizierung (aktuell nur 
Session-Tokens), Rate Limiting gegen DoS-Angriffe.
Der aktuelle Stand ermöglicht jedoch sofortigen praktischen Einsatz in kontrollierten Umgebungen.

## 5. Projektdetails : Welche Kernfunktionen oder Besonderheiten bietet Ihr Projekt?

Das Projekt bietet folgende Kernfunktionen mit besonderen technischen Merkmalen:
**KI-gestützte Packlisten-Generierung** mit OpenAI GPT-5, optimiert durch Temperature 0.3 für Zuverlässigkeit statt 
Kreativität und strukturierte Prompts.
**Kontextbewusste Empfehlungen** durch Integration von Wetterdaten und kulturellen Tipps aus Dummy-Datenbanken.
**Session-Management** mit sicheren 32-Byte Base64-Tokens, 24-Stunden Auto-Expiry und Session-Historie für wiederholte 
Nutzung.
**Microservices-Architektur** mit klarer Trennung: React-Frontend (Port 5173), API Gateway (Port 8080), stateless 
AI Worker (Port 8081) und PostgreSQL-Datenbank für flexible Itemspeicherung.
**Anti-Hallucination-Mechanismen** mit Fallback zu Dummy-Daten bei AI-Fehlern und expliziten Prompt-Anweisungen gegen 
erfundene Inhalte.
**DevOps-Excellence** mit Multi-Stage Docker Builds, Kubernetes Init Containers für Service-Dependencies und umfassenden
Health Checks.

## 6. Innovation : Was ist neu und besonders innovativ?

Die Innovation liegt in der kontextbewussten AI-Orchestrierung, bei der das System nicht nur GPT-5 nutzt,
sondern Prompts dynamisch mit Wetter- und Kulturdaten anreichert, ein Ansatz, der über simple AI-Wrapper hinausgeht.

## 7. Wirkung (Impact) : Welchen konkreten Nutzen bringt Ihr Projekt?

Der konkrete Nutzen zeigt sich vor allem im Zeitersparnis für die Nutzer:innen, Packlisten werden generiert und müssen 
nicht mehr selbst erstellt werden,
das führt zu Stressreduktion und Kosteneinsparungen durch Vermeidung vergessener Gegenstände.
Langfristig kann das System in Reise-Plattformen integriert werden und vielen Nutzer:innen bei der Erstellung von 
Packlisten unterstützen.

## 8. Technische Exzellenz : Welche Technologien, Daten oder Algorithmen werden genutzt?

Das Projekt nutzt einen modernen Tech-Stack mit State-of-the-Art-Technologien:
**Backend** mit Kotlin 1.9.25 und Spring Boot 3.5.6, Spring AI 1.0.0-M4 (offizielle Spring-Integration für LLMs)
**Datenbank** PostgreSQL 15 mit JPA/Hibernate für ORM, Flyway 9.16.3 für Migrationen und HikariCP für Connection 
Pooling.
**AI-Integration** über OpenAI GPT-5 mit Temperature 0.3, Max Tokens 2000 und strukturiertem Prompt Engineering (System 
+ User Prompts mit JSON-Schema).
**Frontend** mit React 18.3.1, TypeScript 5.9.3 für Type Safety und Vite 7.1.7 für optimierte Builds (<1s HMR).
**DevOps** mit Docker Multi-Stage Builds (Gradle + Eclipse Temurin JRE 21 für Backend, Node 20 +
+ Nginx Alpine für Frontend), Docker Compose für lokale Orchestrierung und Kubernetes mit 12 Manifesten
+ (Deployments, Services, PersistentVolumeClaims, Secrets).
**Datenmanagement** durch Flyway-Migrationen für versionierte Schema-Evolution und JSONB für flexible, performante
+ Item-Storage.
**Algorithmen** umfassen Response-Validierung (Regex, Count Checks), Session-Cleanup (automatische Expiry) und
+ Prompt-Optimierung für minimale Hallucination-Rates.

## 9. Ethik, Transparenz und Inklusion  Wie stellen Sie Fairness, Transparenz und Sicherheit sicher?

Ethik und Transparenz werden auf mehreren Ebenen sichergestellt:
**Datenschutz** durch Session-basiertes Design ohne
Nutzeridentitäten – keine personenbezogenen Daten werden gespeichert, Sessions expirieren nach 24 Stunden automatisch.
**AI-Transparenz** durch Offenlegung des verwendeten Modells (GPT-4) in jeder Response, explizite Kennzeichnung von
AI-generierten Inhalten und die Möglichkeit für Nutzer, generierte Listen manuell anzupassen.
**Fairness** wird durch kulturell neutrale Dummy-Daten gewährleistet: Das System bietet Tipps für diverse Destinationen
(Paris, Tokyo, New York) ohne kulturelle Vorurteile.
**Sicherheit** durch Input-Validierung (Jakarta Bean Validation),
SQL-Injection-Prevention (JPA Parameterized Queries), Error Handling ohne Information Leakage und sichere Session-Tokens
(32-Byte Secure Random).
**Inklusion** durch API-First-Design, das Integration in Accessibility-Tools ermöglicht, und mehrsprachige
Erweiterbarkeit (aktuell Englisch, vorbereitet für i18n).
**Umweltverantwortung** durch effizienten Resource-Einsatz (stateless Services, optimierte Docker Images,
<200MB Footprint).

## 10. Zukunftsvision  Wie könnte das Projekt in 5-10 Jahren aussehen?

In 5-10 Jahren könnte Smart Packing Assistant zu einer **globalen Reise-Intelligence-Plattform** evolvieren:
**AI-Evolution** durch Integration multimodaler LLMs, die Bilder von Gepäckstücken analysieren
("Habe ich das eingepackt?"), Stimme für hands-free Interaktion und personalisierte Lernalgorithmen, die individuelle
Präferenzen speichern. **Ökosystem-Integration** in Reise-Apps (Booking.com, Airbnb), Fluglinien (Gepäck-Optimierung
für Gewichtslimits) und Smart-Home-Systeme (automatische Erinnerungen 48h vor Abflug). **Erweiterte Features** wie
Echtzeit-Wetterupdates während der Reise, AR-Visualisierung (Virtual Packing mit Smartphone-Kamera), Social-Sharing
von Packlisten und Community-basierte Empfehlungen. **Nachhaltigkeit-Focus** durch CO₂-Tracking pro Gepäckstück,
Nachhaltigkeits-Scores für Items und Circular-Economy-Integration (Leihservices für selten genutzte Items).
**Enterprise-Anwendungen** für Firmenreisen mit Compliance-Checks (Visumanforderungen, Gesundheitszertifikate)
und automatisierter Kostenabrechnung.
**Technisch** vollständig auf Edge-AI migriert für Offline-Funktionalität und Datenschutz. Das Projekt wird Open Source
mit aktiver Community-Entwicklung.

---

## Installation & Deployment

**Voraussetzungen:** Java 21, Node.js 20+, PostgreSQL 15, Docker, Kubernetes (Kind/Minikube)

**Lokale Installation:**
```bash
# PostgreSQL Setup
brew install postgresql@15 && brew services start postgresql@15
createuser -s admin && psql -U admin -c "CREATE DATABASE packing_assistant;"
psql -U admin -d packing_assistant -c "ALTER USER admin PASSWORD 'secret123';"

# Environment
echo "OPENAI_API_KEY=sk-proj-your-key" > .env && export $(cat .env | xargs)
psql -U admin -d packing_assistant -f services/api-gateway/src/main/resources/db/migration/V1__initial_schema.sql

# Build & Run
./gradlew build
cd services/ai-worker && ./gradlew bootRun &
cd services/api-gateway && ./gradlew bootRun &
cd services/frontend && npm install && npm run dev
```

**Docker Deployment:**
```bash
echo "OPENAI_API_KEY=sk-proj-your-key" > .env
docker compose up -d
```

**Kubernetes Deployment:**
```bash
kind create cluster --name packing-assistant
docker build -t smart-packing/api-gateway:latest ./services/api-gateway
docker build -t smart-packing/ai-worker:latest ./services/ai-worker
docker build -t smart-packing/frontend:latest ./services/frontend
kind load docker-image smart-packing/api-gateway:latest --name packing-assistant
kind load docker-image smart-packing/ai-worker:latest --name packing-assistant
kind load docker-image smart-packing/frontend:latest --name packing-assistant
echo -n "sk-proj-your-key" | base64  # Output in k8s/01-postgres-secret.yaml eintragen
kubectl apply -f k8s/
kubectl get pods -n packing-assistant -w
kubectl port-forward -n packing-assistant service/api-gateway 8080:8080
```

**API Nutzung:**
```bash
# Session erstellen
SESSION=$(curl -s -X POST http://localhost:8080/api/sessions | jq -r '.sessionToken')

# Packliste generieren
curl -X POST http://localhost:8080/api/packing/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Token: $SESSION" \
  -d '{"destination":"Paris","durationDays":5,"travelType":"VACATION","season":"SPRING"}'
```

**Tests:**
```bash
./gradlew test                                # Unit Tests
docker compose up -d && sleep 30              # E2E Setup
curl http://localhost:8080/api/packing/health # Health Check
```

---

## Projektstruktur

```
smart-packing-assistant/
 services/
    api-gateway/          # Spring Boot REST API (Port 8080)
    ai-worker/            # Spring Boot AI Service (Port 8081)
    frontend/             # React + Vite (Port 5173)
    shared/               # Shared DTOs, Enums
 k8s/                      # 12 Kubernetes Manifeste
 docs/                     # Guides (Docker, K8s, Testing)
 docker-compose.yml        # Container Orchestration
 README.md                 # Diese Datei
```

## Weitere Dokumentation

- **CLAUDE.md** - Projektrichtlinien & Architektur (500+ Zeilen)
- **POSTMAN_TESTING_GUIDE.md** - API Testing (12 Beispiele)
- **DOCKER_GUIDE.md** - Docker Deployment Details
- **KUBERNETES_DD_GUIDE.md** - Kubernetes Deployment & Debugging
- **API_DOCUMENTATION.md** - AI Worker API Reference

## Kontakt

**HTW Berlin** | **Dozent:** alkurdiz@htw-berlin.de | **Abgabe:** 22. November 2025, 23:59:59 Uhr
