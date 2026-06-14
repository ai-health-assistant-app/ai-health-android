# Rapporto Tecnico Esecutivo:

# Riorientamento Strategico e Roadmap

# Architetturale per "HealthTwin" (Visione

# 2026 - Passive Sensing)

**Data** : 29 Gennaio 2026

**Autore** : Lead Software Architect & Senior Research Scientist

**Stato** : Definitivo - Approvato per Esecuzione

**Classificazione** : Documento Interno Confidenziale

**Versione** : 2.1 (Revisione Post-Pivot No-HRV)

## 1. Executive Summary: Il Cambio di Paradigma verso la

## "Frictionless Intelligence"

### 1.1 Contesto Strategico e Motivazione del Pivot

L'evoluzione del mercato della _mobile health_ (mHealth) nel biennio 2024-2026 ha delineato
una frattura netta tra le tecnologie clinicamente orientate e le soluzioni di benessere di massa.
L'analisi preliminare condotta sul progetto "HealthTwin" aveva inizialmente ipotizzato un
modello ibrido, che combinasse la raccolta dati passiva dai dispositivi indossabili con una
misurazione attiva della Variabilità della Frequenza Cardiaca (HRV) tramite fotopletismografia
video (vPPG) utilizzando la fotocamera dello smartphone. Tuttavia, una rigorosa revisione
basata sulle evidenze scientifiche recenti e sui dati di utilizzo reale ha imposto una revisione
radicale di questo approccio.^1

La decisione di eliminare il modulo di acquisizione vPPG (Camera HRV) non rappresenta un
ridimensionamento delle ambizioni del progetto, bensì un'evoluzione verso un paradigma di
**"Frictionless Intelligence"**. I dati dimostrano inequivocabilmente che la richiesta di
misurazioni attive – che impongono all'utente una stasi fisica e l'apposizione del dito
sull'obiettivo per un periodo compreso tra 60 e 180 secondi – costituisce il singolo punto di
abbandono più critico nel ciclo di vita dell'utente.^1 In un mercato saturo, la _ritenzione_ è
determinata dalla capacità dell'applicazione di inserirsi silenziosamente nella routine
quotidiana, non dalla richiesta di nuovi comportamenti.


Parallelamente, l'ecosistema dei dispositivi indossabili di fascia media (dominato da brand
come Xiaomi, Huawei, Amazfit) ha raggiunto una maturità sensoristica e una penetrazione di
mercato tale da rendere la raccolta passiva non solo sufficiente, ma statisticamente superiore
per l'analisi dei trend a lungo termine.^1 Sebbene questi dispositivi spesso non espongano il
dato HRV grezzo (intervalli R-R) con la stessa fedeltà di un elettrocardiogramma, la ricchezza
dei dati surrogati – in particolare la Frequenza Cardiaca a Riposo (RHR) notturna e
l'architettura del sonno – permette, attraverso sofisticate tecniche di elaborazione del
segnale, di ricostruire un quadro fisiologico accurato dello stress sistemico.

### 1.2 Obiettivi del Nuovo Documento Architetturale

Il presente rapporto ha lo scopo di ridefinire l'architettura tecnica e la roadmap di sviluppo di
"HealthTwin" alla luce della rimozione del componente vPPG. L'obiettivo primario si sposta
dalla visione artificiale (Computer Vision) alla scienza dei dati (Signal Processing) e
all'ingegneria dell'integrazione (System Integration).

Le implicazioni di questa scelta sono profonde:

1. **Riduzione del Debito Tecnico** : Si elimina la necessità di gestire la vasta frammentazione
   hardware delle fotocamere Android (API Camera2/CameraX, calibrazione ISO, gestione
   flash LED), risparmiando centinaia di ore di sviluppo e manutenzione.^1
2. **Mitigazione del Rischio Privacy** : Rimuovendo l'acquisizione di flussi video del flusso
   sanguigno, l'applicazione riduce drasticamente la propria superficie di attacco e
   semplifica la conformità normativa (GDPR, MDR), uscendo dalla zona grigia dei dispositivi
   diagnostici.^1
3. **Investimento Algoritmico** : Le risorse liberate vengono riallocate nello sviluppo di
   algoritmi proprietari di _Detrending Circadiano_ e nella gestione avanzata delle API Health
   Connect, trasformando l'app da semplice "lettore" a motore analitico predittivo.

## 2. Fondamenti Scientifici dell'Approccio

## "Passive-Only"

La validità scientifica di "HealthTwin" senza misurazione diretta dell'HRV si fonda sulla
robustezza dei biomarcatori surrogati e sulla capacità di pulire il segnale dai rumori fisiologici
naturali.

### 2.1 La Frequenza Cardiaca a Riposo (RHR) come Proxy dello Stato

### Autonomico

La letteratura scientifica conferma l'esistenza di una correlazione inversa estremamente forte
tra la Frequenza Cardiaca a Riposo (RHR) e la Variabilità della Frequenza Cardiaca (HRV). In
termini fisiologici, un aumento del tono simpatico (risposta "lotta o fuga") si manifesta
simultaneamente con un innalzamento della frequenza cardiaca basale e una riduzione della


variabilità tra i battiti.^1

Per la popolazione target dell'applicazione – individui interessati al benessere generale e alla
gestione dello stress, piuttosto che atleti d'élite – il monitoraggio longitudinale della RHR
notturna offre insight di valore comparabile all'HRV puntuale:

```
● Sensibilità ai Fattori di Stress : Un incremento progressivo della RHR di 3-5 battiti al
minuto (bpm) rispetto alla media mobile personale è un indicatore precoce di
sovrallenamento, infiammazione sistemica o insorgenza di patologie virali.^1
● Stabilità del Dato : A differenza dell'HRV misurato via smartphone, che è suscettibile a
errori procedurali (micro-movimenti, pressione eccessiva del dito), la RHR acquisita
durante il sonno da un wearable è un dato mediato su migliaia di campioni,
intrinsecamente più stabile.^1
```
### 2.2 Il Ruolo Critico dell'Architettura del Sonno

L'approccio "Passive-Only" eleva il sonno da semplice metrica di durata a indicatore primario
di resilienza neurale. I wearable moderni, anche di fascia economica, sono in grado di
discriminare con accettabile precisione tra sonno leggero, sonno profondo (NREM) e fase
REM.

```
● Recupero Fisico (NREM) : La quantità di sonno profondo è correlata alla secrezione
dell'ormone della crescita e al ripristino delle scorte di glicogeno, agendo come proxy del
recupero muscolare e metabolico.^1
● Recupero Cognitivo (REM) : La latenza e la durata della fase REM riflettono lo stato di
elaborazione emotiva e consolidamento mnemonico. Una frammentazione del sonno REM
è spesso associata a stati di ansia o stress psicologico elevato.^5 Integrare queste
metriche permette di costruire un modello multidimensionale della "Readiness" che non
dipende da un singolo numero (HRV), ma da un quadro olistico del riposo.
```
### 2.3 Detrending Circadiano: L'Intelligenza Matematica

Il vero differenziale tecnologico di "HealthTwin" risiede nel trattamento del dato. I dati
biometrici grezzi sono influenzati dai ritmi circadiani: la frequenza cardiaca oscilla
naturalmente durante le 24 ore, indipendentemente dallo stress. Ignorare questa oscillazione
porta a falsi positivi (es. interpretare l'aumento fisiologico pomeridiano della FC come stress).
L'applicazione implementerà algoritmi di **Detrending** (come lo _Smoothness Priors Approach_ -
SPA) per separare la componente ritmica naturale dalle fluttuazioni anomale. Studi indicano
che l'applicazione di queste tecniche di filtraggio migliora l'accuratezza della rilevazione dello
stress fino al 13,67% rispetto all'uso dei dati grezzi.^6 Questo processo trasforma un dato
"povero" (FC campionata ogni minuto) in un indicatore clinico di alta qualità.


## 3. Architettura Tecnica Aggiornata (Visione 2026)

L'eliminazione della componente video impone una ristrutturazione del grafo delle dipendenze
e delle responsabilità dei moduli. L'architettura evolve da un sistema ibrido Sensor/Data a un
sistema puro **Data-Processing/Analytics**.

### 3.1 Strategia di Modularizzazione "Feature-Based"

Il principio guida rimane la separazione verticale delle funzionalità per garantire scalabilità e
manutenibilità, ma la composizione dei moduli cambia drasticamente.

#### 3.1.1 Moduli Dismessi (Legacy Removal)

La rimozione del codice morto è il primo passo per sanare il debito tecnico potenziale.

```
● ❌ :feature:measurements : Viene eliminato l'intero modulo dedicato all'interfaccia
utente di misurazione, inclusi il viewfinder della fotocamera, le animazioni di feedback in
tempo reale e i grafici istantanei del segnale PPG.
● ❌ :core:camera : Viene rimosso il modulo di basso livello contenente le dipendenze
CameraX, gli algoritmi di elaborazione immagini (conversione YUV-RGB), i filtri di segnale
(Butterworth/Bandpass) e la logica di calcolo dei picchi rMSSD.
```
#### 3.1.2 Nuova Topologia dei Moduli

```
Livello Modulo Nome Modulo Responsabilità
Tecnica &
Innovazione
```
```
Stack Tecnologico
Chiave
```
```
App :app Entry point
dell'applicazione.
Configurazione del
grafo di
navigazione (ora
semplificato senza
il tab "Misura").
Root Dependency
Injection.
```
```
Hilt, Jetpack
Navigation
```
```
Feature :feature:dashboard Cuore della UI.
Visualizzazione
aggregata dello
score "Readiness".
Gestione
complessa degli
```
```
Jetpack Compose,
Vico Charts
```

```
stati di errore (dati
mancanti, latenza
sync).
```
**Feature** :feature:onboarding Flusso di
benvenuto
focalizzato
sull'autorizzazione
**Health Connect** e
sulla spiegazione
del valore del
monitoraggio
passivo.

```
Accompanist
Permissions
```
**Feature** :feature:chat Interfaccia
conversazionale.
L'agente AI riceve
ora in input il
contesto
RHR/Sonno e i
trend calcolati,
fornendo feedback
qualitativo.

```
Compose,
Markdown Text
```
**Core (Analytics)** ✨ **:core:analytics Nuovo Core IP**.
Contiene la logica
matematica pura:
algoritmi di
Detrending (SPA),
calcolo delle medie
mobili esponenziali
(EWMA), logica di
scoring della
Readiness.

```
Apache Commons
Math /
Kotlin-Statistics
```
**Core (Data)** :core:data Implementazione
del Repository
Pattern
"Offline-First".
Orchestrazione tra
dati locali (Room) e

```
Room, DataStore,
Coroutines
```

```
nuovi dati da
Health Connect.
```
```
Core (System) ⚡
:core:health-conn
ect
```
```
Modulo critico di
integrazione.
Gestione delle
policy Android
14/15, Background
Read Permissions,
logica di
deduplicazione e
backfill storico.
```
```
Android Health
Connect API
```
```
Core (Domain) :core:domain Use Cases puri e
Entities di dominio.
Definisce il
linguaggio ubiquo
(es.
DailyReadiness,
SleepDebt).
```
```
Pure Kotlin (KMP
Ready)
```
### 3.2 Flusso Dati e Persistenza (Offline-First)

Il requisito di robustezza impone che l'applicazione sia pienamente funzionale anche in
assenza di connessione internet e con sincronizzazioni sporadiche dei wearable.

1. **Sorgente di Verità (Single Source of Truth)** : Il database locale **Room** rimane l'unica
   fonte di verità per la UI. La Dashboard osserva il database, mai direttamente l'API Health
   Connect.
2. **Pipeline di Ingestione** :
   ○ Il modulo :core:health-connect interroga l'API Android periodicamente (tramite
   WorkManager) o su richiesta utente.
   ○ I dati grezzi (Raw Records) vengono passati a :core:analytics per la normalizzazione e
   il calcolo degli score derivati.
   ○ I dati elaborati (Score, Trend, Cleaned RHR) vengono persistiti in Room.
3. **Gestione Identificativi** : Per evitare duplicazioni durante il sync multi-device (es. utente
   che passa da Xiaomi a Pixel Watch), si utilizzeranno hash composti basati su timestamp e
   tipo di dato come chiavi primarie logiche, ignorando gli ID generati dai dispositivi
   sorgente che possono essere instabili.^7


## 4. Deep Dive Algoritmico: Il Motore "No-HRV"

In assenza di un sensore diretto per l'HRV, l'intelligenza dell'applicazione si sposta sulla
capacità di inferire lo stato psicofisico incrociando metriche correlate.

### 4.1 Algoritmo Composito di "Daily Readiness"

L'algoritmo di Readiness non è una semplice media, ma un modello pesato che valuta la
deviazione dall'omeostasi personale dell'utente.

**Formula del Modello (Concettuale):**

Dove le componenti sono definite come segue:

#### A. Analisi della RHR (Peso: 50%)

La componente più critica. Non si valuta il valore assoluto, ma la deviazione dalla **Baseline**
(media mobile degli ultimi 30 giorni).

```
● Calcolo Baseline : Utilizzo di una media mobile esponenziale (EWMA) per dare più peso
ai giorni recenti ma mantenere stabilità storica.
● Scoring :
```
```
○ : Punteggio positivo (Recupero ottimale).
```
```
○ : Penalità lieve (Possibile stress/affaticamento).
```
```
○ : Penalità grave (Alta probabilità di
malattia/sovrallenamento).^8
```
#### B. Bilancio del Sonno (Peso: 40%)

Valutazione del "Debito di Sonno" accumulato.

```
● Sleep Debt : Differenza tra il bisogno di sonno individuale (stimato inizialmente a 8h e
raffinato nel tempo) e la media del sonno degli ultimi 14 giorni.
● Qualità : Un moltiplicatore basato sulla percentuale di Sonno Profondo + REM. Se la
durata è sufficiente ma il sonno profondo è < 10%, lo score viene ridotto.^4
```
#### C. Indice di Attività Precedente (Peso: 10%)

Analisi del carico residuo. Se il giorno precedente l'utente ha avuto un carico di attività
eccezionale (es. 20.000 passi contro una media di 5.000), si applica un fattore di
smorzamento al Readiness Score per suggerire recupero, indipendentemente dalla RHR.^8


### 4.2 Signal Processing: Detrending con Smoothness Priors (SPA)

Per implementare la funzionalità di "Stress Detection" durante la giornata (usando i dati di
frequenza cardiaca continua se disponibili), implementeremo l'algoritmo **SPA** nel modulo
:core:analytics. Questo metodo è superiore ai filtri passa-alto standard per la rimozione dei
trend non stazionari come il ritmo circadiano.^6

**Specifiche di Implementazione Kotlin:**

L'algoritmo risolve il problema di regolarizzazione:

Dove è il segnale HR, è il trend, è la matrice delle differenze seconde discrete e è
il parametro di smoothing (tipicamente impostato a 500 per l'analisi HR). In Kotlin, questo
richiede l'uso di una libreria di algebra lineare (come kmath o Multik) per la risoluzione

efficiente di sistemi matriciali a banda. Il segnale risultante rappresenta le variazioni di
frequenza cardiaca _detrendizzate_ , correlate direttamente agli eventi di stress acuto.^6

## 5. La Sfida dell'Integrazione: Health Connect e

## l'Ecosistema Android

L'affidarsi esclusivamente a Health Connect introduce complessità significative legate alla
frammentazione dell'ecosistema Android e alle politiche di gestione energetica dei produttori
(OEM).

### 5.1 Analisi delle Criticità Tecniche (Android 14/15)

#### 5.1.1 Limitazioni dei Permessi di Background

Con Android 15 (API Level 35), Google ha introdotto il permesso
READ_HEALTH_DATA_IN_BACKGROUND. Tuttavia, questo permesso è considerato "sensibile"
e non viene concesso automaticamente.

```
● Vincolo : Le app che non sono "Health Controllers" o che non dimostrano una necessità
critica potrebbero vedersi negare questo permesso.
● Impatto : Senza lettura in background, "HealthTwin" può aggiornare i dati solo quando
l'utente apre l'app (Foreground).
● Workaround Architetturale : Implementare un servizio ibrido. Utilizzare WorkManager
per tentare letture periodiche (ogni 4-6 ore) se il permesso è concesso. In caso contrario,
implementare un "Aggressive Foreground Sync" che esegue il fetch di tutti i dati mancanti
```

```
nell'istante ON_RESUME dell'Activity principale.^12
```
#### 5.1.2 Il Problema della Latenza OEM (Xiaomi/Huawei)

I dispositivi Xiaomi (tramite app Mi Fitness/Zepp Life) e Huawei (Huawei Health) non
sincronizzano i dati con Health Connect in tempo reale. Spesso, la scrittura avviene in batch
solo quando l'utente apre l'app del produttore.^14

```
● Scenario di Errore : L'utente apre "HealthTwin" alle 09:00. Il braccialetto ha registrato il
sonno, ma Mi Fitness non l'ha ancora scritto in Health Connect. "HealthTwin" vede 0 dati.
● Strategia di Mitigazione (UX) :
```
1. **Rilevamento Stale Data** : Se l'ultimo dato risale a > 6 ore fa, mostrare un avviso non
   intrusivo nella Dashboard.
2. **Deep Linking** : Fornire un pulsante "Sincronizza Ora" che apre direttamente l'app del
   produttore (tramite Intent package name) per forzare il push dei dati.^14

#### 5.1.3 Finestra Temporale dei 30 Giorni

Health Connect limita la lettura dei dati storici a 30 giorni prima della concessione del
permesso.^5

```
● Strategia di Ingestione : Al primo avvio, il modulo :core:health-connect deve eseguire un
"Backfill Job". Questo job scaricherà la massima granularità disponibile (30 giorni) per
calcolare immediatamente una Baseline RHR affidabile. Senza questo passaggio,
l'algoritmo di Readiness impiegherebbe settimane per diventare accurato.^7
```
### 5.2 Specifiche di Sincronizzazione Dati

```
Tipo Dato Frequenza Sync
Ideale
```
```
Comportamento
Fallback
```
```
Note Critiche
```
```
Passi / Attività Ogni 15-30 min Aggiornamento al
lancio app
```
```
Usare StepsRecord.
Attenzione alla
deduplicazione
multi-source.
```
```
Sonno (Sessioni) 1 volta al giorno
(Mattina)
```
```
Richiesta manuale
utente
```
```
Leggere
SleepSessionRecor
d e
SleepStageRecord
per dettaglio
REM/Deep.
```

```
Frequenza
Cardiaca
```
```
Continuo (se disp.) Aggregati orari Richiede gestione
enorme volume
dati. Per MVP,
considerare solo
RHR aggregata se il
continuo è troppo
oneroso.
```
```
RHR (A Riposo) 1 volta al giorno Calcolo derivato da
FC min
```
```
Se il campo
RestingHeartRateR
ecord è vuoto,
stimare usando la
media dei minimi
notturni.
```
## 6. Roadmap Esecutiva Aggiornata: Piano di 12

## Settimane

La roadmap è strutturata in 6 Sprint bisettimanali, seguendo una metodologia _Trunk-Based
Development_ adattata al modello operativo 1 Lead + 2 Contributor.

### Fase 1: Fondamenta e Ingestione Dati (Settimane 1-4)

**Sprint 1: Architettura e Setup Health Connect**

```
● Obiettivo : Hello World modulare in grado di connettersi a Health Connect.
● Lead Architect : Refactoring build logic (Gradle Version Catalog). Setup Hilt. Definizione
interfacce :core:domain.
● Contributor A (UI) : Setup Design System Material3. Implementazione schermate
Onboarding con spiegazione chiara dei permessi (Why Passive tracking works).
● Contributor B (Backend/Data) : Setup :core:health-connect. Implementazione
PermissionController. Gestione eccezioni per device senza Health Connect installato
(Android 13-).^15
```
**Sprint 2: Persistenza e Logica "Offline-First"**

```
● Obiettivo : L'app legge passi e sonno e li salva in locale.
● Lead Architect : Design schema Room Database. Implementazione logica di
Deduplicazione dati (evitare doppi conteggi passi da telefono e braccialetto).^7
● Contributor A : Dashboard scheletrica. Implementazione componenti grafici base (Card
Sonno, Card Attività).
● Contributor B : Implementazione SyncWorker (WorkManager). Logica di Backfill storico
```

```
30 giorni al primo avvio.
```
### Fase 2: Il Motore Analitico (Settimane 5-8)

**Sprint 3: Algoritmi di Readiness e Baseline**

```
● Obiettivo : Trasformare i dati grezzi in insight (Readiness Score).
● Lead Architect : Sviluppo modulo :core:analytics. Implementazione algoritmi statistici
(EWMA per Baseline RHR, Z-Score per anomalie).
● Contributor A : Visualizzazione avanzata dati. Grafici trend (Vico/MPAndroidChart).
Visualizzazione differenziale ("Oggi vs Media").
● Contributor B : Integrazione DataStore per preferenze utente (es. orario sveglia target).
Integrazione API Meteo semplice (contesto ambientale).
```
**Sprint 4: Advanced Signal Processing (Detrending)**

```
● Obiettivo : Implementazione della differenziazione tecnologica (SPA).
● Lead Architect : Porting dell'algoritmo Smoothness Priors in Kotlin. Ottimizzazione
performance per esecuzione su main thread o dispatcher default.
● Contributor A : UI Dettaglio Stress. Visualizzazione della curva HR detrendizzata (se dati
continui disponibili).
● Contributor B : Testing unitario intensivo degli algoritmi matematici. Verifica edge cases
(es. dati HR mancanti, buchi temporali).
```
### Fase 3: User Experience e Rilascio (Settimane 9-12)

**Sprint 5: Chat Empatica e Feedback Loop**

```
● Obiettivo : L'utente interagisce con il "Gemello Digitale".
● Lead Architect : Engineering dei Prompt per LLM. Il prompt deve ricevere in input il JSON
della Readiness e generare testo motivazionale/empatico.
● Contributor A : UI Chat completa :feature:chat. Gestione stati loading e typing.
● Contributor B : Integrazione client API LLM (OpenAI/Gemini/Anthropic) in :core:network.
Gestione sicura API Keys (CMake/BuildConfig).
```
**Sprint 6: Polish, Mitigazione Latenza e Beta**

```
● Obiettivo : Rilascio versione testabile.
● Lead Architect : Profiling energetico e memoria. Review finale conformità Privacy Policy
per Health Connect permissions.
● Contributor A : Implementazione flow "Risoluzione Problemi Sync" (guide visuali per
Xiaomi/Huawei battery settings).
● Contributor B : Testing su dispositivi fisici eterogenei (Samsung, Pixel, Xiaomi). Bug fixing
finale.
```

## 7. Analisi dei Rischi e Strategie di Mitigazione

La rimozione della fotocamera riduce i rischi hardware ma eleva i rischi di dipendenza
software.

```
Rischio
Identificato
```
```
Probabilità Impatto Strategia di
Mitigazione
Tecnica
```
```
Latenza Dati
(Xiaomi/Huawei)
```
```
Alta Alto UX Trasparente :
Mostrare
timestamp ultimo
sync ("Dati
aggiornati a 4 ore
fa"). Deep Link :
Pulsante per aprire
app produttore e
forzare sync.^14
```
```
Rifiuto Permessi
Background
```
```
Media Medio Aggressive
Foreground Sync :
Fetch totale dei dati
all'ON_RESUME.
Cache locale
robusta per
mostrare sempre
qualcosa all'utente.
```
```
Qualità Dati
Scadente
```
```
Media Basso Filtraggio
Euristico : In
:core:analytics,
scartare sessioni di
sonno < 3h o con 0
min REM (probabili
artefatti o riposini
non significativi).^4
```
```
Mancanza RHR
Gresa
```
```
Bassa Medio Algoritmo
Fallback : Se
RestingHeartRateR
ecord manca,
```

```
calcolare la media
del 5° percentile dei
campioni
HeartRateRecord
notturni disponibili.
```
## 8. Conclusioni

Il passaggio all'architettura **"Passive-Only"** posiziona "HealthTwin" come un prodotto
tecnologicamente maturo, allineato con le aspettative di privacy e usabilità dell'utente del

2026. Sebbene si rinunci al controllo diretto dell'acquisizione dati (vPPG), si guadagna in
      scalabilità, ritenzione utente e robustezza del dato longitudinale.

La complessità non scompare, ma trasla: dalla visione artificiale all'analisi statistica avanzata.
Il successo del progetto dipenderà ora dalla qualità dell'implementazione degli algoritmi di
**Detrending** e dalla fluidità della gestione delle idiosincrasie di **Health Connect**. Con la
roadmap qui definita, il team ha una guida chiara per navigare questa transizione e
consegnare un MVP di valore superiore entro 12 settimane.

#### Bibliografia

#### 1. Impatto Assenza HRV su App Fitness.pdf

#### 2. Unlocking Health Insights: How HRV and RHR Reveal Stress, Recovery, and

#### Performance, accesso eseguito il giorno gennaio 28, 2026,

#### https://miketnelson.com/hrv-rhr-correlation/

#### 3. New Report: Resting Heart Rate and Heart Rate Variability | FitnessGenes®,

#### accesso eseguito il giorno gennaio 28, 2026,

#### https://www.fitnessgenes.com/blog/new-report-rhr-and-hrv

#### 4. Sleep Quality Prediction From Wearable Data Using Deep Learning - PMC - NIH,

#### accesso eseguito il giorno gennaio 28, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC5116102/

#### 5. The 30-days restriction behind Health-Connect data while deprecating Google

#### Fit APIs, accesso eseguito il giorno gennaio 28, 2026,

#### https://stackoverflow.com/questions/77593290/the-30-days-restriction-behind-h

#### ealth-connect-data-while-deprecating-google-fit

#### 6. Heart rate variability with circadian rhythm removed achieved high accuracy for

#### stress assessment across all times throughout the day - NIH, accesso eseguito il

#### giorno gennaio 28, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC12034550/

#### 7. Synchronize data | Android health & fitness, accesso eseguito il giorno gennaio

#### 28, 2026,

#### https://developer.android.com/health-and-fitness/health-connect/sync-data

#### 8. What's my readiness score in the Fitbit app - Google Help, accesso eseguito il


#### giorno gennaio 28, 2026,

#### https://support.google.com/fitbit/answer/14236710?hl=en

#### 9. Fitbit Daily Readiness Score Review: Here's How it Works | DC Rainmaker, accesso

#### eseguito il giorno gennaio 28, 2026,

#### https://www.dcrainmaker.com/2021/11/fitbit-readiness-review.html

#### 10. New to the Oura App: Understanding Sleep Debt - The Pulse Blog, accesso

#### eseguito il giorno gennaio 28, 2026, https://ouraring.com/blog/sleep-debt/

#### 11. Source code for neurokit2.signal.signal_detrend, accesso eseguito il giorno

#### gennaio 28, 2026,

#### https://neuropsychology.github.io/NeuroKit/_modules/neurokit2/signal/signal_detr

#### end.html

#### 12. Using Health Connect in the Android Mobile Inform SDK - Validic Technical

#### Documentation, accesso eseguito il giorno gennaio 28, 2026,

#### https://helpdocs.validic.com/docs/native-android-mobile-inform-sdk-using-andr

#### oid-health-connect

#### 13. Read raw data | Android health & fitness, accesso eseguito il giorno gennaio 28,

#### 2026,

#### https://developer.android.com/health-and-fitness/health-connect/read-data

#### 14. [HUAWEI] The HUAWEI TruSleep™ data syncing is slow on my band/watch,

#### accesso eseguito il giorno gennaio 28, 2026,

#### https://consumer.huawei.com/en/support/content/en-us00733892/

#### 15. Get started with Health Connect - Android Help, accesso eseguito il giorno

#### gennaio 28, 2026, https://support.google.com/android/answer/12201227?hl=en


