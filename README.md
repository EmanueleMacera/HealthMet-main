# Corso di Studio: Internet of Things (IoT) - Università degli Studi di Genova (2021)

## Presentazione

Il corso di Internet of Things (IoT) riguarda i sistemi in cui dispositivi fisici sono connessi a Internet. IoT è un tema centrale dell'informatica, considerando che le connessioni globali dei dispositivi IoT sono destinate a crescere per molti anni. Questo insegnamento copre tutti i principali livelli di progettazione e realizzazione di un sistema IoT (edge, trasporto e computazione): sensori, attuatori, programmazione di dispositivi, protocolli IoT, programmazione a eventi e cloud computing.

## Obiettivi e Contenuti

### Obiettivi Formativi

Metodi di apprendimento, protocolli, architetture e piattaforme per lo sviluppo di applicazioni distribuite e mobili per l'Internet delle cose. Questo include protocolli machine to machine, algoritmi distribuiti per la tolleranza agli errori e la replicazione, architetture orientate ai servizi, sistemi operativi embedded, dati in tempo reale e in streaming, geolocalizzazione e framework collaborativi.

### Modalità Didattiche

Lezioni, laboratorio, progetto e studio individuale.

### Programma/Contenuto

L'obiettivo del corso è fornire le conoscenze e metodologie necessarie alla progettazione, sviluppo e analisi di applicazioni distribuite e mobili nel contesto dell’Internet delle cose. Si presta particolare attenzione a protocolli machine to machine, framework e piattaforme per servizi distribuiti, dati in tempo reale e di geolocalizzazione, e per l’interconnessione e coordinamento di insiemi di dispositivi eterogenei.

## Esami

### Modalità d'Esame

Esame orale e discussione del progetto.

### Modalità di Accertamento

Gli esercizi di programmazione individuali hanno lo scopo di valutare la capacità di applicare i concetti di base della programmazione a eventi e l'apprendimento dei protocolli IoT più utilizzati. La valutazione si basa sulla correttezza, efficienza e leggibilità del codice.

Il progetto finale ha lo scopo di verificare l'apprendimento delle nozioni di sensori/attuatori e programmazione di dispositivi, la capacità di progettare e implementare un'architettura IoT a partire da specifiche informali e utilizzare una piattaforma IoT. La valutazione si basa sull'adeguatezza delle scelte architetturali, l'efficacia, scalabilità, usabilità e correttezza del sistema implementato.

La presentazione e discussione del progetto finale ha lo scopo di verificare se gli studenti abbiano attivamente collaborato allo sviluppo del progetto. La valutazione si basa sul livello di comprensione del funzionamento globale e dei dettagli tecnici del progetto.

## Introduzione Progetto d'Esame: HealthMet

HeathMet è un'applicazione dedicata alla sicurezza delle persone in moto. Il nome deriva dalla combinazione di "health" (salute) e "helmet" (casco). L'applicazione utilizza un giroscopio integrato nel casco per rilevare incidenti e inviare un messaggio di emergenza con la posizione GPS ad un contatto fidato precedentemente selezionato.

### Funzionalità Principali:

- Collegamento del telefono all'Arduino nel casco tramite Bluetooth.
- Selezione di un contatto dalla rubrica a cui inviare l'SMS di emergenza.
- Monitoraggio continuo per segnali provenienti dal casco.
- Invio automatico di un SMS contenente la posizione GPS in caso di impatto.

### Componenti del Progetto:

#### Rilevamento dell'Impatto
- File: `giroscopio.ino`
- L'impatto viene rilevato tramite un giroscopio collegato ad Arduino.
- Analizzando i valori sull'asse delle z, si supera una soglia per rilevare un impatto.
- Una volta rilevato, viene inviato un segnale all'applicazione.

#### ReadDataFromArduino()
- La funzione `ReadDataFromArduino()` riceve il segnale di emergenza.
- Richiama la funzione `RequestCurrentLocation()` per ottenere la posizione GPS.

#### RequestCurrentLocation()
- Ottiene la posizione GPS utilizzando l'API di Google.
- Dipendenza: `implementation 'com.google.android.gms:play-services-location:21.0.1'`

#### Invio SMS
- La funzione `sendEmergencySms()` si occupa di inviare l'SMS di emergenza.
- Utilizza la classe `SmsManager`.
- Parametri:
  - `phoneNumber`: Numero di telefono selezionato dalla rubrica.
  - `message`: "Aiuto!" seguito dalla latitudine e longitudine.
