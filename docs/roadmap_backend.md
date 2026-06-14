# Specifiche Tecniche Backend

# "SaluteTwin": Architettura Edge-First &

# AI Gateway

## 1. Visione Architetturale: Il Backend come "Insight

## Receiver"

### 1.1 Cambio di Paradigma: Edge-First Computation

La revisione v2.0 formalizza un netto distacco dal modello cloud-centrico. In questa
architettura, il dispositivo mobile (Android) è promosso a **Primary Computation Node**. Il
backend cessa di essere un motore di calcolo scientifico e diventa un **AI Orchestration Layer**
leggero e stateless.^1
**Divisione delle Responsabilità:**
● **Mobile (Edge):** È l'unica fonte di verità per le metriche fisiologiche. L'app Android
acquisisce i dati grezzi da Health Connect, esegue la pulizia del segnale, calcola le medie
mobili esponenziali (EWMA), determina la Baseline RHR (30 giorni) e computa lo _Score di
Readiness_ finale utilizzando librerie native (Kotlin/C++).
● **Backend (Cloud):** Agisce come un "Insight Receiver". Non vede mai serie temporali
grezze. Riceve esclusivamente un payload JSON di metadati sintetici (es. {"readiness": 78,
"sleep_debt_min": 45}). Il suo unico compito è usare questi metadati per costruire il
contesto (Prompt Engineering) per il Large Language Model (LLM), gestire l'identità
dell'utente e coordinare i servizi di monetizzazione.

### 1.2 "Zero-Knowledge" sui Dati Grezzi

Questa strategia eleva la privacy a standard strutturale. Poiché il backend non riceve mai i dati
grezzi (battito per battito, log del sonno dettagliati), il rischio di esposizione di dati sanitari
sensibili lato server è eliminato alla radice. Il database backend (Firestore) non dovrà mai
ospitare collezioni per dati biometrici, nemmeno temporaneamente, semplificando
radicalmente la compliance GDPR e HIPAA.

## 2. Interfaccia Dati e Protocollo "Insight Receiver"

### 2.1 Eliminazione del "Burst Sync" e Ingestion Leggera

I precedenti riferimenti alla sincronizzazione massiva di dati storici (/v1/ingest/backfill) sono


deprecati e rimossi. Non è più necessario trasferire gigabyte di storico per calcolare le
baseline, poiché queste risiedono e vengono aggiornate localmente sul dispositivo
dell'utente.^1
Il nuovo contratto di interfaccia si basa su singoli eventi di "Check-in" o "Insight Request".
● **Endpoint Primario:** POST /v1/agent/insight
● **Payload (JSON Schema):** Il backend accetta un oggetto piatto e anonimizzato
contenente solo i risultati dei calcoli locali.
JSON
{
"metrics": {
"readiness_score": 75 , // Calcolato su Android
"rhr_deviation": -2.5, // Calcolato su Android (vs Baseline locale)
"sleep_balance_score": 80 , // Calcolato su Android
"activity_strain": 14.5 // Calcolato su Android
},
"user_context": {
"local_time": "08:30",
"user_intent": "daily_briefing" // o query specifica dell'utente
}
}

### 2.2 Validazione Rigorosa dei Metadati

Il backend non ricalcola nulla, ma deve _validare_ l'integrità semantica dei dati ricevuti per
evitare che input corrotti o manipolati generino allucinazioni nell'AI. Utilizzando **Pydantic v2** ,
ogni campo viene verificato rispetto a range fisiologici plausibili (es. 0 <= readiness <= 100).
Se l'app invia un valore anomalo (es. readiness: 999), la richiesta viene respinta
istantaneamente con un errore 422 Unprocessable Entity prima ancora di toccare la logica di
business o l'LLM.^2

## 3. Stack Tecnologico: Python per l'Orchestrazione I/O

### 3.1 Giustificazione Tecnologica Aggiornata: Perché Python?

L'abbandono dei calcoli scientifici (NumPy/SciPy) lato server cambia la motivazione per l'uso


di Python, ma ne rafforza la necessità per altri motivi.
● **Non più CPU-Bound:** Non dovendo più invertire matrici o calcolare derivate sui segnali, il
carico CPU del backend crolla drasticamente.
● **Dominio I/O Bound:** Il nuovo collo di bottiglia è l'attesa della rete. Ogni richiesta
comporta chiamate multiple ad alta latenza: verifica token Firebase, recupero profilo
Firestore, e soprattutto l'attesa della generazione token-per-token dell'LLM
(OpenAI/Gemini).
● **Python & Async/Await:** Python, con **FastAPI** e il server ASGI (Uvicorn), è scelto
specificamente per la sua capacità di gestire migliaia di queste connessioni "in attesa"
(coroutine) simultaneamente su un singolo thread, senza bloccare le risorse. Inoltre,
Python rimane il linguaggio nativo (Lingua Franca) per l'interazione con l'ecosistema AI
moderno, offrendo le SDK più aggiornate e stabili per il Prompt Engineering e la gestione
strutturata degli output degli LLM.^1

### 3.2 Database Minimalista (Solo Utenti)

La persistenza è limitata strettamente ai dati di account.
● **Firestore (NoSQL):** Utilizzato esclusivamente per:
○ Profili Utente (ID, Email Hash, Data Registrazione).
○ Stato Abbonamento (sincronizzato via Webhook RevenueCat).
○ Preferenze Utente (es. "Tono del coach: Severo/Empatico").
● **Policy di Esclusione:** È tecnicamente impossibile scrivere metriche di salute su Firestore.
Le _Security Rules_ e i modelli Pydantic del repository layer devono escludere
esplicitamente qualsiasi campo numerico non relativo alla configurazione dell'app.

## 4. Strategia di Sviluppo & Deployment: "Local-First"

Per accelerare lo sviluppo senza costi cloud prematuri e complessità di rete, si adotta una
strategia di sviluppo locale containerizzato che replica l'ambiente di produzione.

### 4.1 Ambiente Locale con Docker Compose

Lo sviluppo avviene su macchina locale orchestrando i servizi tramite docker-compose.
Questo garantisce che ogni sviluppatore lavori in un ambiente identico alla produzione
(Google Cloud Run).
**Struttura docker-compose.yml:**

1. **Backend (FastAPI):** Monta il codice sorgente come volume per l'hot-reloading
   immediato.
2. **Firebase Emulator Suite:** Container ufficiale che emula localmente Firestore e Auth.
   Questo permette di testare la creazione utenti e le query DB senza connettersi al cloud


```
reale e senza sporcare i dati di produzione.^5
```
3. **Tunnel (Cloudflared/Ngrok):** Espone il backend locale a internet per permettere all'app
   Android fisica di connettersi.

### 4.2 Tunneling e Compatibilità Firebase Auth

Il problema critico nello sviluppo mobile locale è che Firebase Auth richiede domini sicuri
(HTTPS) e whitelisted per i redirect OAuth. Usare localhost su Android è complesso.
**Soluzione Raccomandata: Cloudflare Tunnel**
Rispetto a Ngrok (che cambia URL ad ogni riavvio nella versione free), Cloudflare Tunnel
permette di associare un sottodominio stabile (es. dev-api.tuodominio.com) al servizio locale.
**Configurazione "Anti-Rottura" per Auth:**

1. **Setup Tunnel:** Lanciare cloudflared tunnel --url [http://localhost:8000.](http://localhost:8000.)
2. **Firebase Console:** Andare in _Authentication_ -> _Settings_ -> _Authorized Domains_.
3. **Whitelist:** Aggiungere esplicitamente il dominio del tunnel (es. dev-api.tuodominio.com)
   alla lista. Senza questo passaggio, le richieste autenticate provenienti dall'app mobile
   verranno rifiutate con errore auth/unauthorized-domain o auth/network-request-failed.^6
4. **Client Mobile:** Configurare la BASE_URL dell'app (nelle build di debug) per puntare al
   dominio del tunnel invece che a Cloud Run.

## 5. Roadmap di Sviluppo Pratica (12 Settimane)

Questa roadmap è ottimizzata per arrivare al "Primo Insight" nel minor tempo possibile,
posticipando le complessità di scala.

### Fase 1: Setup "Local-First" e Fondamenta (Settimane 1-2)

```
● Obiettivo: Avere un "Hello World" che risponde all'app Android via Tunnel sicuro.
● Backend: Inizializzare progetto FastAPI con Poetry. Configurare docker-compose con
Python 3.11-slim.
● Tunneling: Configurare Cloudflare Tunnel su un dominio di sviluppo stabile.
● Auth: Configurare Firebase Admin SDK nel backend e aggiungere il dominio tunnel alla
Whitelist Firebase. Implementare la dipendenza get_current_user in FastAPI per validare i
Bearer Token inviati dall'app.
```
### Fase 2: L'Endpoint "Insight Receiver" e Mock AI (Settimane 3-5)

```
● Obiettivo: Il backend riceve i dati calcolati e risponde (senza vera AI).
● API Contract: Definire i modelli Pydantic per il payload dei metadati (Readiness, RHR,
ecc.).
```

```
● Endpoint: Implementare POST /v1/agent/insight.
● Mocking: Invece di chiamare OpenAI, restituire risposte statiche ("Il tuo readiness è
{score}, riposati") per permettere al team mobile di testare la UI senza attendere
l'integrazione LLM reale e senza costi API.
```
### Fase 3: Integrazione Database e LLM Reale (Settimane 6-9)

```
● Obiettivo: Persistenza utenti e intelligenza reale.
● Firestore: Implementare il Repository Pattern per leggere/scrivere le preferenze utente
(tono, obiettivi) su Firestore (usando l'emulatore locale per i test).
● Prompt Engineering: Sostituire il Mock con chiamate reali a OpenAI/Gemini. Costruire il
prompt dinamico: "Agisci come un coach. L'utente ha readiness {readiness}. Il tono deve
essere {user_pref_tone}."
● Sanitizzazione: Integrare regex o librerie leggere per assicurare che nessun testo libero
inviato dall'utente contenga PII prima di passarlo all'LLM.
```
### Fase 4: Produzione e Deploy Cloud (Settimane 10-12)

```
● Obiettivo: Go Live su Google Cloud.
● Container: Creare il Dockerfile di produzione (multi-stage build per ridurre le
dimensioni).
● Cloud Run: Eseguire il primo deploy su Google Cloud Run (completamente gestito).
Configurare le variabili d'ambiente (API Keys) in modo sicuro tramite Secret Manager.
● Switch Mobile: Aggiornare l'app Android di produzione per puntare all'URL di Cloud Run
(https://api-prod.tuodominio.com) invece che al tunnel.
```
#### Bibliografia

#### 1. Specifiche Backend mHealth _SaluteTwin_.pdf

#### 2. More ways to deploy with Firebase App Hosting, accesso eseguito il giorno

#### febbraio 12, 2026,

#### https://firebase.blog/posts/2025/05/more-ways-to-deploy-app-hosting/

#### 3. The Local Firebase Emulator UI in 15 minutes - YouTube, accesso eseguito il

#### giorno febbraio 12, 2026, https://www.youtube.com/watch?v=pkgvFNPdiEs

#### 4. Build and Deploy a Remote MCP Server to Google Cloud Run in Under 10

#### Minutes, accesso eseguito il giorno febbraio 12, 2026,

#### https://cloud.google.com/blog/topics/developers-practitioners/build-and-deploy-

#### a-remote-mcp-server-to-google-cloud-run-in-under-10-minutes

#### 5. Authenticating users | Cloud Run - Google Cloud Documentation, accesso

#### eseguito il giorno febbraio 12, 2026,

#### https://docs.cloud.google.com/run/docs/authenticating/end-users

#### 6. Set up authorized domains and restrict your browser API keys for Firebase

#### authentication, accesso eseguito il giorno febbraio 12, 2026,

#### https://www.youtube.com/watch?v=a5rhO6lbjoY

#### 7. Connect a custom domain | Firebase Hosting - Google, accesso eseguito il giorno


#### febbraio 12, 2026, https://firebase.google.com/docs/hosting/custom-domain


