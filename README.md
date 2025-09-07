# Sudoku Multiplayer Game

Un gioco Sudoku multiplayer distribuito basato su architettura peer-to-peer per l'esame di Architetture Distribuite per il Cloud (2021/2022).

## 📋 Panoramica

Sudoku Game è un'applicazione multiplayer che permette a più giocatori di competere simultaneamente risolvendo puzzle Sudoku. Il sistema utilizza una rete peer-to-peer con tabella hash distribuita (DHT) per gestire le partite e i giocatori connessi.

### Caratteristiche Principali

- **Multiplayer competitivo**: Più giocatori possono competere sulla stessa griglia Sudoku
- **Sistema di punteggio**: Punti assegnati per mosse corrette, penalità per errori
- **Architettura P2P**: Nessun server centrale, completamente distribuito
- **Interfaccia console**: Interfaccia testuale interattiva e colorata
- **Docker ready**: Facile deployment tramite container

## 🎮 Come Giocare

### 1. Login
- Inserire un nickname di 3-7 caratteri
- Accesso al tabellone delle sfide disponibili

### 2. Creazione/Partecipazione Partita
- **Crea nuova partita**: `@<codice-partita>`
- **Unisciti a partita**: `><codice-partita>`
- La partita inizia automaticamente con almeno 2 giocatori

### 3. Gameplay
- **Inserimento valore**: `xy-N` (dove x,y sono coordinate e N è il numero)
- **Punteggio**:
  - +1 punto: valore corretto e non ancora inserito
  - 0 punti: valore corretto ma già presente
  - -1 punto: valore errato
- **Abbandono partita**: comando dedicato disponibile in gioco

### 4. Fine Partita
- Il giocatore con più punti vince quando il Sudoku è completato
- Countdown automatico prima del ritorno al tabellone sfide

## 🏗️ Architettura Tecnica

### Componenti Principali

- **DHT (Distributed Hash Table)**: Memorizza dati di gioco e giocatori
- **Peer-to-Peer Network**: Comunicazione diretta tra nodi
- **Sincronizzazione**: Aggiornamenti in tempo reale tramite messaggi diretti

### Flusso Operativo

1. **Aggiornamento DHT**: Ogni operazione aggiorna prima la risorsa distribuita
2. **Notifica diretta**: Invio dell'aggiornamento a tutti i partecipanti
3. **Consistenza**: Garantisce che i nuovi giocatori vedano lo stato più recente

## 🛠️ Stack Tecnologico

- **Java**: Linguaggio di programmazione principale
- **TomP2P**: Libreria per networking peer-to-peer e DHT
- **Beryx**: Framework per interfacce console interattive
- **JUnit 5**: Testing framework
- **Maven**: Gestione dipendenze e build
- **Docker**: Containerizzazione e deployment

## 📦 Installazione e Avvio

### Prerequisiti

- Docker
- Git (per clonare il repository)

### Build del Container

```bash
# Clone del repository (se necessario)
git clone <repository-url>
cd sudoku-game

# Build dell'immagine Docker
docker build --no-cache -t sudoku-game .
```

### Avvio della Rete

#### 1. Creazione della Rete Docker

```bash
docker network create --subnet=172.20.0.0/16 network
```

#### 2. Avvio del Master Peer

```bash
# Prima esecuzione
docker run -i --net network --ip 172.20.128.0 -e MASTERIP="172.20.128.0" -e ID=0 --name MASTER-PEER sudoku-game

# Esecuzioni successive
docker start -i MASTER-PEER
```

#### 3. Aggiunta di Peer Aggiuntivi

```bash
# Prima esecuzione di un peer (sostituire X con ID univoco)
docker run -i --net network -e MASTERIP="172.20.128.0" -e ID=X --name PEER-X sudoku-game

# Esecuzioni successive
docker start -i PEER-X
```

## 📸 Screenshots

### Schermata di Login
![Login](https://user-images.githubusercontent.com/74552824/200955802-e4e01708-be1b-40ec-bc1e-60eed6e99525.png)

### Tabellone Sfide
![Tabellone Sfide](https://user-images.githubusercontent.com/74552824/200955800-a1aacab5-62c3-4926-8c73-aa0da7d7c2c3.png)

### Partita in Corso
![Partita](https://user-images.githubusercontent.com/74552824/200955798-c82af045-b204-4437-9915-dc4f0437ff43.png)

### Fine Partita
![Termine Partita](https://user-images.githubusercontent.com/74552824/201118214-ca8fe4b3-3a88-4bc0-bf65-6ffa5a615665.png)

## ⚠️ Limitazioni Conosciute

### Compatibilità Terminale
- **Colori**: La colorazione del testo funziona solo su terminali compatibili con Swing
- **Altri terminali**: Su terminali non-Swing il testo apparirà senza colorazione

### Aggiornamenti Automatici
- **Esecuzione locale**: Refresh automatico di board e tabellone
- **Esecuzione remota**: Necessario premere Enter quando richiesto per aggiornare la visualizzazione

