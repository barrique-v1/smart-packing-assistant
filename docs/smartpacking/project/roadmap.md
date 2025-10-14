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

## Phase 2: Shared Module

- [ ] Shared Module Ordnerstruktur erstellen
- [ ] `build.gradle.kts` für Shared Module konfigurieren
- [ ] Enums definieren (TravelType, Season)
- [ ] DTOs erstellen:
    - [ ] PackingRequest.kt
    - [ ] PackingResponse.kt
    - [ ] PackingItem.kt
    - [ ] WeatherInfo.kt
- [ ] Root `settings.gradle.kts` anpassen (alle 3 Module einbinden)
- [ ] Shared Module in API Gateway als Dependency einbinden
- [ ] Shared Module in AI Worker als Dependency einbinden
- [ ] Shared Module Build testen

---

## Phase 3: API Gateway - Database Layer

- [ ] Flyway Migration Setup in `build.gradle.kts`
- [ ] Erste Migration erstellen (V1__initial_schema.sql)
- [ ] JPA Entities erstellen:
    - [ ] PackingList.kt (entspricht DB-Tabelle)
    - [ ] Annotations konfigurieren (@Entity, @Table, @Id, etc.)
- [ ] Repository Interface erstellen (PackingListRepository.kt)
- [ ] `application.yml` Database-Konfiguration prüfen
- [ ] Flyway Migration ausführen
- [ ] Repository mit Test verifizieren

---

## Phase 4: API Gateway - REST Layer

- [ ] PackingController.kt erstellen
- [ ] POST /api/packing/generate Endpoint implementieren
- [ ] Request Validation einbauen (@Valid, @NotNull)
- [ ] GET /api/packing/{id} Endpoint für gespeicherte Listen
- [ ] GET /api/packing/history Endpoint für alle Listen
- [ ] PackingListService.kt erstellen (Business Logic)
- [ ] AiWorkerClient.kt erstellen (RestTemplate für AI Worker)
- [ ] RestTemplateConfig.kt erstellen
- [ ] Error Handling implementieren
- [ ] Actuator Health Endpoint testen

---

## Phase 5: AI Worker - Dummy-Daten

- [ ] Ordner `src/main/resources/data/` erstellen
- [ ] `weather_data.json` erstellen (Dummy-Wetterdaten)
- [ ] `culture_tips.json` erstellen (Dummy-Kultur-Tipps)
- [ ] WeatherService.kt implementieren (liest JSON)
- [ ] CultureService.kt implementieren (liest JSON)
- [ ] Services testen

---

## Phase 6: AI Worker - OpenAI Integration

- [ ] OpenAI API Key vom Dozenten besorgen
- [ ] API Key in `application.yml` als Environment Variable konfigurieren
- [ ] OpenAiConfig.kt erstellen
- [ ] PromptService.kt erstellen (Prompt Engineering)
- [ ] System Prompt definieren
- [ ] User Prompt Template erstellen
- [ ] AiService.kt implementieren:
    - [ ] OpenAI Client initialisieren
    - [ ] ChatRequest erstellen
    - [ ] Response parsing implementieren
    - [ ] JSON Validation einbauen
    - [ ] Halluzination-Vermeidung (Temperature 0.3)
    - [ ] Fallback-Logik bei Fehlern
- [ ] AiController.kt erstellen
- [ ] POST /api/ai/generate Endpoint implementieren
- [ ] AI Worker lokal testen

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