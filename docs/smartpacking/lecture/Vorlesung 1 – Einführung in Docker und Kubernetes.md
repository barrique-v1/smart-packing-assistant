HTW Berlin  
Datum: 05.10.2025  
Ort: Berlin  
Verfasser: Zouia Alkurdi & Oliver Richter  

---

## Thema der 1. Vorlesung

- Was ist Docker?  
- Was ist Kubernetes?  
- Warum werden diese Technologien genutzt?  
- Einrichtung der Entwicklungsumgebung (Docker + Kubernetes)  
- Erste Schritte mit Containern und Pods  
- Verbindung zur Aufgabe: Umsetzung einer AI-App mit Docker, Kubernetes

---

## Einführung: Was ist Docker?

Docker ist eine Plattform, um **Anwendungen in Containern** auszuführen.  
Ein Container ist wie eine kleine, abgeschlossene Box, in der alles liegt, was eine App braucht:  
- der Programmcode  
- die Abhängigkeiten (z. B. Bibliotheken, Python-Pakete)  
- die Laufzeitumgebung  

### Vorteile von Docker
- **Unabhängig vom System**: Container laufen auf jedem Rechner gleich.  
- **Leichtgewichtig**: Container sind kleiner und schneller als virtuelle Maschinen.  
- **Wiederverwendbar**: Ein Image kann von vielen Personen genutzt werden.  
- **Einfacher Start**: Ein Container ist mit einem einzigen Befehl startbar.  

### Beispiel
```bash
docker run hello-world
```

---

## Einführung: Was ist Kubernetes?

Kubernetes (oft abgekürzt als "K8s") ist ein System, das viele Docker-Container **verwaltet und steuert**.  
Es sorgt dafür, dass Apps zuverlässig laufen, auch wenn einzelne Container ausfallen.  

### Wichtige Funktionen
- **Orchestrierung**: Kubernetes startet, überwacht und beendet Container automatisch.  
- **Skalierung**: Kubernetes kann mehr Container starten, wenn die Nachfrage steigt.  
- **Self-Healing**: Wenn ein Container abstürzt, wird er automatisch ersetzt.  
- **Netzwerk**: Container bekommen automatisch eine Verbindung untereinander.  

### Wichtige Begriffe
- **Pod**: kleinste Einheit, enthält einen oder mehrere Container.  
- **Deployment**: beschreibt, wie viele Pods einer App laufen sollen.  
- **Service**: macht eine App im Netzwerk erreichbar.  

---

## Warum Docker und Kubernetes zusammen?

Docker ist gut, um **eine einzelne App in einem Container** laufen zu lassen.  
Wenn aber viele Container und Services zusammenarbeiten sollen, braucht man ein System wie Kubernetes.  

- **Docker** = Container bauen und starten  
- **Kubernetes** = Container im großen Maßstab betreiben  

Beispiel:  
Eine AI-App hat eine **API** (Anfragen beantworten) und einen **Worker** (Modelle berechnen).  
- Mit Docker packen wir API und Worker in Container.  
- Mit Kubernetes stellen wir sicher, dass beide immer laufen und erreichbar sind.  

---

## Entwicklungsumgebung einrichten

# 1. Docker Desktop installieren
# Windows (PowerShell, mit winget)
winget install --id Docker.DockerDesktop -e

# Windows (PowerShell, mit Chocolatey)
choco install docker-desktop -y

# macOS (Homebrew)
brew install --cask docker

# Test ob Docker läuft
docker run hello-world

# 2. Kubernetes in Docker Desktop aktivieren
# Einstellungen-Datei bearbeiten (Pfad abhängig vom System)

# Windows
notepad "$Env:APPDATA\Docker\settings.json"

# macOS
vi ~/Library/Group\ Containers/group.com.docker/settings-store.json
  "KubernetesEnabled": true,
  "KubernetesMode": "kind",

# Folgenden Eintrag setzen:
"kubernetesEnabled": true
Danach Docker Desktop neu starten

# Windows
Stop-Process -Name "Docker Desktop" -Force
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

# macOS
osascript -e 'quit app "Docker Desktop"'
open -a "Docker Desktop"


# 3. Kubernetes CLI (kubectl) installieren
# Windows
choco install kubernetes-cli

# macOS
brew install kubernetes-cli

# 4. Kubernetes-Cluster testen
kubectl cluster-info
kubectl get nodes
---

## Verbindung zur Aufgabe

Diese Vorlesung bildet die technische Grundlage für die Abgabe:  

- **Docker**: App in einem Container starten  
- **Kubernetes**: Container als Services betreiben    
- **Pitch**: Idee und Umsetzung erklären