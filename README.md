# Smart Packing Assistant

---

## 1. Executive Summary – Kurze Zusammenfassung des Projekts

Smart Packing Assistant ist eine intelligente Microservices-Anwendung, die Reisenden beim Packen hilft.
Die Application generiert personalisierte Packlisten basierend auf Reiseziel, Dauer, Jahreszeit und verschiedene
Reisearten wie Geschäftsreisen, Urlaub oder Backpacking. Durch KI-gestützte Analysen mit OpenAI GPT-4 und einer 
Wissensbasis von 140 validierten Gegenständen erhalten Nutzer:innen individuell angepasste Packlisten.
Die Anwendung nutzt moderne Technologien wie Docker und Kubernetes sowie eine Microservices-Architektur
mit React-Frontend, Spring Boot-Backend, PostgreSQL-Datenbank und Qdrant Vector Database. Das Projekt
demonstriert den praktischen Einsatz von wissensbasierter KI-Generierung zur Lösung eines alltäglichen Problems
und kombiniert AI-Integration, Container-Orchestrierung und sichere Session-Verwaltung. Die Lösung ist
vollständig lokal deployable und demonstriert production-ready Architektur-Patterns.

## 2. Ziele des Projekts – Welche Ziele verfolgt Ihr Projekt, welches Problem wird gelöst?

Das Hauptziel ist die Entwicklung eines KI-gestützten Assistenten, der das Problem ineffizienter und
unvollständiger Reisevorbereitungen löst. Viele Reisende vergessen wichtige Gegenstände oder packen
unnötige Dinge ein, was zu Stress, Unannehmlichkeiten und zusätzlichen Kosten am Zielort führt. Der
Smart Packing Assistant löst dieses Problem durch die Bereitstellung personalisierter, kontextbewusster
Packlisten, die Reiseziel, Wetterbedingungen, kulturelle Besonderheiten und Reiseart
(Business/Urlaub/Backpacking) berücksichtigen. Durch die Kombination von 140 validierten Items mit der Kreativität 
generativer KI erreicht das System eine hohe Zuverlässigkeit bei gleichzeitiger Flexibilität für individuelle 
Reiseszenarien. Das Projekt zielt darauf ab, Reisenden Zeit zu sparen, Stress zu reduzieren und eine bessere 
Vorbereitung für ihre Reisen zu ermöglichen.

## 3. Anwendung und Nutzung – Wie wird die Lösung verwendet, wer sind die Hauptnutzer:innen?

Die Lösung richtet sich an drei Hauptnutzergruppen mit unterschiedlichen Reisebedürfnissen. Geschäftsreisende
benötigen formelle Kleidung und arbeitsrelevante Dinge wie Laptops und Präsentationsmaterialien, während Urlauber
Dinge für Entspannung, Sightseeing oder sportliche Aktivitäten planen. Backpacker legen Wert auf
leichtes, vielseitiges Equipment für Abenteuerreisen mit speziellen Anforderungen an Gewicht und
Funktionalität. Die Nutzung erfolgt über eine intuitive Weboberfläche, bei der Nutzer Reiseziel, Reisedauer
in Tagen, Jahreszeit und Reiseart eingeben. Der Smart Packing Assistant generiert daraufhin innerhalb weniger
Sekunden eine kategorisierte Packliste mit Empfehlungen für Kleidung, Elektronik, Hygieneartikel und sonstige
Gegenstände.

**Code-Repository:** https://github.com/barrique-v1/smart-packing-assistant

**Pitch:** [Pitch.mp3](./Pitch.mp3) (Audio, 1 - 3 Min)

## 4. Entwicklungsstand – Idee, Proof of Concept, Prototyp oder Einsatzbereit?

Das Projekt ist ein fertiger funktionsfähiger Prototyp mit vollständig implementierten Kernfunktionen.
Alle wesentlichen Features sind getestet und einsatzbereit, darunter AI-Integration mit OpenAI GPT-4,
RAG-basierte semantische Suche mit Qdrant Vector Database, PostgreSQL-Persistenz für Sessions und Packlisten,
sowie vollständige Docker-Containerisierung und Kubernetes-Deployment.
Die Anwendung läuft stabil für lokale Entwicklung, Docker-basiertes Deployment sowie lokales Kubernetes. Für echte
Produktionsumgebungen fehlen unter anderem noch HTTPS/TLS-Verschlüsselung, robuste Authentifizierung über Session-Tokens
hinaus und Rate Limiting gegen DoS-Angriffe, jedoch ermöglicht der aktuelle Stand bereits den sofortigen
praktischen Einsatz in kontrollierten Umgebungen.

## 5. Projektdetails – Welche Kernfunktionen oder Besonderheiten bietet Ihr Projekt?

Das Projekt kombiniert mehrere innovative Technologien. Die Kernfunktion basiert auf Retrieval-Augmented Generation: 
Eine Qdrant Vector Database mit 140 validierten Items trifft auf GPT-4 für kontextspezifische Anpassung. 
Semantische Suche liefert die relevantesten Items, ergänzt durch Wetterdaten und kulturelle Hinweise.
Anti-Hallucination-Mechanismen garantieren Verlässlichkeit durch Destination-Validierung und Similarity-Score-Filterung 
ab 0.4. Das Session-Management nutzt sichere 32-Byte Tokens mit 24-Stunden Expiry. Die Microservices-Architektur trennt 
Frontend (Port 5173), API Gateway mit PostgreSQL (Port 8080) und stateless AI Worker (Port 8081) klar voneinander.

## 6. Innovation – Was ist neu und besonders innovativ?

Die zentrale Innovation liegt in der Hybrid-AI-Architektur, die Retrieval-Augmented Generation mit Large
Language Model-Generierung kombiniert. Eine Wissensbasis mit 140 verifizierten Items
wird mittels Vector-Embeddings semantisch durchsucht und die Ergebnisse werden mit GPT-4 kombiniert, wodurch
Zuverlässigkeit mit der Kreativität generativer KI vereint wird. Jeder Prompt wird durch kontextspezifische
Wetterdaten und kulturelle Besonderheiten ergänzt, wodurch deutlich maßgeschneiderte Ergebnisse erzielt werden können 
als mit reinen LLM-Systemen.

## 7. Wirkung (Impact) – Welchen konkreten Nutzen bringt Ihr Projekt?

Der konkrete Nutzen des Smart Packing Assistant zeigt sich in mehreren Dimensionen für verschiedene Nutzergruppen.
Zeitersparnis entsteht durch automatische Generierung vollständiger Packlisten innerhalb weniger Sekunden statt
manueller Zusammenstellung über Stunden, wobei die AI-gestützte Analyse sicherstellt, dass keine wichtigen Items
vergessen werden. Stressreduktion wird durch die Gewissheit erreicht, dass alle notwendigen Gegenstände berücksichtigt
wurden, einschließlich lokaler Anforderungen wie Adapter, Visa-Dokumente oder kulturell angemessene
Kleidung. Kosteneinsparungen entstehen durch Vermeidung vergessener Gegenstände, die teuer am Zielort nachgekauft
werden müssten, sowie durch Vermeidung von Übergepäck durch intelligente Mengenempfehlungen. Das System kann als
White-Label-Lösung in bestehende Reise-Plattformen integriert werden und somit einer Vielzahl von Nutzern weltweit
bei der Reisevorbereitung unterstützen.

## 8. Technische Exzellenz – Welche Technologien, Daten oder Algorithmen werden genutzt?

Das Projekt nutzt einen modernen Tech-Stack mit State-of-the-Art-Technologien. Das Backend basiert auf Kotlin
mit Spring Boot und Spring AI für LLM-Integration sowie JPA/Hibernate für ORM. Die
Datenschicht umfasst PostgreSQL 15 mit Flyway sowie Qdrant Vector Database für semantische Suche mit 
1536-dimensionalen Embeddings. Die AI-Integration erfolgt über OpenAI GPT-4 mit Temperature 0.3 für konsistente Antworten
und strukturiertem Prompt Engineering sowie text-embedding-3-small für Vector Embeddings aus data/packing-knowledge.csv 
mit 140 validierten Gegenständen. Das Frontend nutzt React, TypeScript und Vite. 
Die DevOps-Infrastruktur umfasst Docker Builds mit Gradle, Eclipse Temurin JRE 21, Node 20 und Nginx Alpine,
Docker Compose sowie Kubernetes. Die Algorithmen umfassen Vector-Similarity-Search mit Cosine-Distance bei Score 0.4, 
Prompt-Optimierung für minimale Hallucination-Rates sowie Response-Validierung und Session-Cleanup.

## 9. Ethik, Transparenz und Inklusion – Wie stellen Sie Fairness, Transparenz und Sicherheit sicher?

Ethik und Transparenz werden auf mehreren Ebenen sichergestellt. Der Datenschutz wird durch session-basiertes
Design ohne Speicherung personenbezogener Daten gewährleistet, wobei Sessions automatisch nach 24 Stunden
verfallen. Fairness wird durch kulturell neutrale Wissensbasis gewährleistet mit Tipps für diverse 
Reiseziele ohne Vorurteile. Die Sicherheit basiert auf Input-Validierung mittels Jakarta Bean Validation, 
SQL-Injection-Prevention durch JPA-Parameterized-Queries, Error Handling sowie sicheren 
32-Byte Session-Tokens. Inklusion wird durch API-First-Design für Accessibility-Tools ermöglicht.

## 10. Zukunftsvision – Wie könnte das Projekt in 5-10 Jahren aussehen?

In fünf bis zehn Jahren könnte der Smart Packing Assistant zu einer globalen Reise-Intelligence-Plattform
evolvieren. Die AI-Evolution würde multimodale Large Language Models für Bildanalyse von Gepäckstücken,
Spracheingabe für hands-free Interaktion sowie personalisierte Lernalgorithmen für individuelle Präferenzen
über mehrere Reisen hinweg integrieren. Ökosystem-Integration könnte nahtlose Einbindung in Reise-Apps wie
Booking.com, Kooperationen mit Fluglinien für Gepäck-Optimierung sowie Smart-Home-Integration für automatische
Erinnerungen umfassen. Erweiterte Features würden Echtzeit-Wetterupdates, Social-Sharing von bewährten Packlisten sowie 
Nachhaltigkeit-Tracking mit Co2-Scores und Empfehlungen für Leihservices statt Neukauf ermöglichen. 
Enterprise-Anwendungen für Firmenreisen könnten Compliance-Checks für Visumanforderungen, Gesundheitszertifikate und 
automatisierte Kostenabrechnung bieten. Das System soll vollständig als Open-Source-Projekt entwickelt werden.

## Wichtige Dateien und Datenquellen

#### Dummy Data & Supported Destinations
- [culture_tips.json](services/ai-worker/src/main/resources/data/culture_tips.json)
- [weather_data.json](services/ai-worker/src/main/resources/data/weather_data.json)


  Es existieren Dummy-Daten für 3 Reiseziele, dementsprechend sind zurzeit nur für diese Wetter- und Kultur-Tipps verfügbar.

#### Unterstützte Reiseziele sind in der Datei `application.properties` konfiguriert:
- [application.properties](services/ai-worker/src/main/resources/application.properties)

#### RAG Knowledge Base mit 140 Pack-Items:
- [packing-knowledge.csv](data/packing-knowledge.csv)

