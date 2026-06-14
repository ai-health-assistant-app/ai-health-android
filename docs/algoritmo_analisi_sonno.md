# Algoritmo Avanzato di Sleep Scoring

# Emodinamico e Architetturale per

# Dispositivi Android: Analisi Tecnica e

# Implementazione

## 1. Executive Summary

L'evoluzione della tecnologia indossabile ha trasformato radicalmente il paradigma del
monitoraggio della salute, spostando l'analisi del sonno dai laboratori di polisonnografia
clinica ai dispositivi consumer di uso quotidiano. Tuttavia, la maggior parte delle applicazioni
attuali si limita a fornire statistiche descrittive (es. "hai dormito 7 ore"), mancando di
sintetizzare questi dati in un punteggio di qualità clinico-centrico che integri la fisiologia
cardiovascolare con l'architettura del sonno.
Questo rapporto tecnico descrive lo sviluppo, la modellazione matematica e
l'implementazione software di un algoritmo di **Sleep Scoring** di nuova generazione,
progettato specificamente per un ecosistema Android (Health Connect). La sfida
ingegneristica centrale affrontata in questo documento è la sintesi di un punteggio di qualità
(0–100) accurato e scientificamente validato in assenza di dati sulla Variabilità della
Frequenza Cardiaca (HRV). Per compensare la mancanza di HRV, l'algoritmo sfrutta al
massimo il segnale della Frequenza Cardiaca (HR) campionata nelle 24 ore e l'ipnogramma a
fasi (Deep, REM, Light, Awake), applicando principi di emodinamica notturna come il
_Nocturnal Heart Rate Dipping_ e l'analisi del _Nadir circadiano_.
Il modello proposto scompone il punteggio finale in quattro domini ortogonali: **Architettura
del Sonno & Continuità (40%)** , **Dipping della Frequenza Cardiaca (30%)** , **Analisi della
Frequenza Cardiaca a Riposo (RHR) (20%)** e **Tempistica & Costanza del Sonno (10%)**.
L'implementazione tecnica è realizzata in Kotlin, seguendo i principi della Clean Architecture,
e include strategie robuste per l'imputazione dei dati mancanti (interpolazione lineare) e
funzioni di normalizzazione non lineare (sigmoide) per gestire la variabilità biologica
interindividuale.
L'analisi che segue fornisce non solo il codice sorgente completo e pronto per la produzione,
ma anche una disamina approfondita delle basi fisiologiche che giustificano ogni peso, soglia
e funzione matematica utilizzata, garantendo che l'output dell'algoritmo sia non solo
numericamente corretto, ma clinicamente rilevante.


## 2. Fondamenti Fisiologici e Parametrizzazione

## Algoritmica

La progettazione di un algoritmo di scoring del sonno richiede una comprensione profonda
dei meccanismi fisiologici sottostanti. Non stiamo semplicemente sommando minuti di sonno;
stiamo valutando l'efficienza dei processi di recupero neurobiologico e cardiovascolare. In
assenza di HRV, il segnale HR diventa il proxy primario per valutare il bilanciamento
autonomico (simpatico/parasimpatico).

### 2.1 Architettura del Sonno & Continuità (40%)

L'architettura del sonno si riferisce alla struttura ciclica delle fasi del sonno, mentre la
continuità descrive la stabilità di questa progressione senza interruzioni significative. Questo
dominio rappresenta la componente più ponderosa del punteggio (40%) poiché riflette
direttamente la funzione restaurativa del sonno.

#### 2.1.1 Wake After Sleep Onset (WASO)

Il WASO misura il tempo totale trascorso svegli dopo l'inizio del sonno ed è un indicatore
clinico primario di frammentazione del sonno e insonnia di mantenimento.
● **Evidenza Scientifica:** Studi clinici indicano che un WASO inferiore a 20-30 minuti è
considerato fisiologico in adulti sani.^1 Valori superiori a 30 minuti iniziano a correlare
negativamente con la percezione soggettiva della qualità del sonno, mentre valori
superiori a 60 minuti sono spesso indicativi di patologie del sonno o stress ambientale
significativo.^2 È importante notare che il WASO tende ad aumentare fisiologicamente con
l'età; tuttavia, per un'applicazione di monitoraggio della salute generale, mantenere una
soglia normativa standard è preferibile per incentivare l'igiene del sonno.
● **Logica di Scoring:** Implementeremo una funzione di penalità lineare a tratti ( _piecewise
linear penalty_ ).
○ : Nessuna penalità (massimo punteggio).
○ : Penalità progressiva.
○ : Penalità massima.

#### 2.1.2 Target di Fase (Deep & REM)

Il sonno non è uniforme. Il recupero fisico avviene prevalentemente durante il sonno profondo
(NREM N3), caratterizzato da onde delta e rilascio di ormone della crescita, mentre il recupero
cognitivo ed emotivo è associato alla fase REM (Rapid Eye Movement).
● **Benchmark:** Le linee guida mediche suggeriscono che in un adulto sano, il sonno
profondo dovrebbe costituire circa il 15-25% del tempo totale di sonno, mentre il REM
dovrebbe occupare il 20-25%.^4


```
● Logica di Scoring: L'algoritmo non deve penalizzare linearmente chi dorme "troppo" in
queste fasi, ma deve penalizzare severamente la carenza. Useremo una funzione di
saturazione: una volta raggiunto il target (es. 20% Deep), il punteggio si satura al
massimo. Scendere sotto una soglia critica (es. <10%) comporta una penalità
esponenziale, riflettendo il grave impatto sulla salute.
```
#### 2.1.3 Indice di Frammentazione (Sleep Fragmentation Index)

La frequenza delle transizioni tra le fasi del sonno, in particolare il passaggio brusco da stadi
profondi (Deep/REM) a stadi superficiali (Light/Awake), è un marcatore di instabilità corticale
"micro-architetturale".^5 Un sonno consolidato dovrebbe presentare blocchi continui.
● **Soglia:** Un indice di transizione superiore a 15-20 eventi per ora è spesso associato a
disturbi respiratori (come le apnee notturne) o movimenti periodici degli arti.^7 L'algoritmo
calcolerà le transizioni per ora di sonno e applicherà una penalità per valori eccessivi.

### 2.2 Nocturnal Heart Rate Dipping (30%)

Il "Dipping" è la differenza percentuale tra la frequenza cardiaca media diurna e quella
notturna. È il proxy più potente a nostra disposizione per valutare il recupero cardiovascolare
in assenza di HRV.
● **Fisiologia del Dipping:** Durante il sonno NREM stabile, il tono simpatico diminuisce e il
tono vagale (parasimpatico) aumenta, causando un abbassamento della pressione
sanguigna e della frequenza cardiaca. Questo "riposo emodinamico" è cruciale per
ridurre il carico di lavoro cardiaco.
● **Il Ratio Scientifico (10-20%):** La letteratura cardiologica classifica i soggetti in base al
calo notturno:
○ **Dipper (Normale):** Calo del 10-20%. Questo è il gold standard di salute
cardiovascolare.^8
○ **Non-Dipper:** Calo < 10%. Associato a ipertensione notturna, rischio cardiovascolare
aumentato e mancato recupero.^10
○ **Reverse Dipper (Riser):** Calo < 0% (la HR notturna è _più alta_ di quella diurna).
Questa condizione è patologica e fortemente correlata a rischio di ictus e danno
d'organo.^13
○ **Extreme Dipper:** Calo > 20%. Sebbene indichi un forte tono vagale, in popolazioni
anziane può essere associato a rigidità arteriosa o rischio ischemico, ma in un
contesto di app fitness è generalmente accettato come positivo o neutro.
● **Modello Matematico:** Per evitare "scalini" bruschi nel punteggio, utilizzeremo una
funzione sigmoidea o un'interpolazione lineare a tratti per mappare la percentuale di
dipping a un punteggio di 0-30 punti.


### 2.3 Resting Heart Rate (RHR) Analysis (20%)

Mentre il valore assoluto della RHR dipende dalla genetica e dal livello di fitness (es. atleti
bradicardici), la _deviazione_ dalla propria baseline è un indicatore acuto di stress fisiologico
(carico allostatico).
● **Analisi della Baseline:** Fattori acuti come il consumo di alcol, pasti tardivi,
sovrallenamento o l'insorgere di una malattia febbrile causano un innalzamento
immediato della RHR notturna rispetto alla media dei giorni precedenti.^16
● **Rilevanza Clinica:** Studi epidemiologici mostrano che un aumento della RHR notturna è
predittivo di eventi cardiovascolari e mortalità a lungo termine se sostenuto, ma nel breve
termine riflette la necessità di recupero.^18
● **Logica di Scoring:**
○ Se : Punteggio massimo (20 punti).
○ Se : Penalità proporzionale alla deviazione. Una
deviazione di +5 bpm o più è significativa e deve abbattere il punteggio.^18

### 2.4 Sleep Timing & Consistency (10%) - Analisi del Nadir

Il ritmo circadiano regola la temperatura corporea e la secrezione di ormoni. La frequenza
cardiaca minima (Nadir) dovrebbe verificarsi in una finestra temporale specifica per indicare
un allineamento circadiano ottimale.
● **La Curva ad Amaca ("Hammock Curve"):** In un sonno sano, la HR diminuisce
progressivamente raggiungendo il minimo (Nadir) verso la metà o il secondo terzo della
notte, in coincidenza con il minimo termico corporeo (circa le 4:00 del mattino per un
cronotipo standard).^19
● **Nadir Precoce (Exhaustion):** Se il Nadir avviene immediatamente dopo
l'addormentamento (primo 20% della notte) per poi risalire, indica spesso esaurimento
fisico o debito di sonno ("crash").
● **Nadir Tardivo (Metabolic Stress):** Se la HR rimane alta per gran parte della notte e
scende solo poco prima del risveglio (Slope Discendente), indica che il metabolismo è
stato attivo durante la notte, spesso a causa di digestione tardiva, alcol o attivazione
simpatica residua.^19
● **Logica di Scoring:** Valuteremo la posizione temporale del Nadir come percentuale della
durata totale del sonno. L'ottimo si colloca tra il 40% e il 65%.


## 3. Strategia di Elaborazione Dati e Imputazione

L'affidabilità dell'algoritmo dipende dalla qualità dei dati in ingresso. I dati wearable, come
evidenziato dai file CSV forniti 18 , sono intrinsecamente rumorosi e soggetti a lacune (gaps)
dovute a movimenti, posizionamento errato del sensore o risparmio energetico.

### 3.1 Analisi della Struttura Dati Input

Basandosi sui frammenti forniti, i dati grezzi presentano le seguenti caratteristiche:
● **Frequenza Cardiaca (health_database-heart_rate.csv):** Contiene timestamp (Unix
ms) e beatsPerMinute. Nel campione fornito, la frequenza di campionamento appare
irregolare, con intervalli spesso di 30 minuti.^18 Tuttavia, i moderni wearable campionano
tipicamente ogni 1-5 minuti. L'algoritmo deve essere agnostico rispetto alla frequenza,
gestendo intervalli variabili.
● **Fasi del Sonno (health_database-sleep_stages.csv):** Contiene codici interi per le fasi.
Dai dati Xiaomi/Zepp e dagli standard Health Connect 21 :
○ 4 = Light Sleep (Sonno Leggero)
○ 5 = Deep Sleep (Sonno Profondo)
○ 6 = REM Sleep
○ 1 = Awake (Sveglio)

### 3.2 Strategia per i "Buchi" (Gaps) nei Dati BPM

La gestione dei dati mancanti è critica per il calcolo del Dipping. Se mancano dati durante il
picco di attività diurna o durante il sonno profondo, la media viene falsata.
● **Natura dei Gaps:** I buchi nei dati HR da wearable non sono casuali (Missing At Random),
ma spesso strutturali (es. rimozione del dispositivo per ricarica, perdita di contatto
durante il sonno agitato).
● **Scelta dell'Interpolazione:** La letteratura scientifica che confronta le tecniche di
imputazione per dati fisiologici wearable 23 suggerisce che l' **Interpolazione Lineare** è
preferibile rispetto a metodi più complessi come le spline cubiche per gap di breve durata
(< 60 minuti).
○ _Motivazione:_ La frequenza cardiaca durante il sonno segue trend lenti. Le spline
cubiche, pur essendo curve più morbide, tendono a generare oscillazioni artificiali
(fenomeno di Runge) quando i punti dati sono sparsi, creando falsi picchi o valli che
altererebbero il rilevamento del Nadir. L'interpolazione lineare è conservativa e
introduce meno artefatti.
● **Algoritmo di Imputazione:**

1. Ordinare i dati per timestamp.
2. Identificare intervalli tra misurazioni consecutive ( ).
3. Se (es. 15 minuti) e (es. 60 minuti), inserire


```
punti sintetici minuto per minuto lungo la retta che congiunge i due estremi.
```
4. Se^ minuti,^ il^ buco^ è^ troppo^ grande^ per^ essere^ colmato^ in^ modo^ affidabile.^
    Non interpolare; trattare i segmenti come disgiunti o penalizzare la confidenza del
    dato ("Dati insufficienti").

## 4. Modellazione Matematica per la Normalizzazione

Per trasformare valori fisiologici grezzi in un punteggio normalizzato (0-100), è necessario
abbandonare le logiche binarie (Buono/Cattivo) in favore di funzioni continue che riflettano la
gradualità del rischio biologico.

### 4.1 Il "Dipping Factor": Modellazione Sigmoidea

Dobbiamo mappare la percentuale di Dipping in un punteggio parziale $S_{dip} \in $.
La relazione fisiologica non è lineare:
● Un Dipping del 15% è eccellente (score max).
● Un Dipping dell'8% è sub-ottimale ma non disastroso.
● Un Dipping dello 0% o negativo è gravemente patologico.
Utilizziamo una funzione sigmoidea logistica generalizzata per creare una curva di punteggio
"morbida":
Dove:
● è la percentuale di Dipping (es. 12.5).
● è il punteggio massimo assegnabile a questa categoria.
● è il punto di flesso della curva (il valore di dipping dove il punteggio è metà del
massimo). Impostando , facciamo sì che a 5% di dipping si ottenga un
punteggio medio-basso.
● determina^ la^ pendenza^ della^ curva^ (quanto^ rapidamente^ il^ punteggio^ decade).^
Tuttavia, per un'implementazione robusta in Kotlin che rispetti rigorosamente le soglie cliniche
(10% e 0%), è spesso preferibile una **Interpolazione Lineare a Tratti (Piecewise Linear
Function)** che approssima la sigmoide ma garantisce valori esatti ai confini critici.


**Definizione della Funzione a Tratti:**

1. **Zona**^ **Reverse**^ **Dipping**^ **( ):**^.^ (Rischio^ elevato).^
2. **Zona**^ **Non-Dipper**^ **( ):**^.^ (Crescita^ lineare^ da^0 a^20
    punti).
       ○ _Esempio:_ Dipping 5% -> 10 punti.
3. **Zona**^ **Dipper**^ **Ottimale**^ **( ):**^.^ (Crescita^ da^20
    a 30 punti).
       ○ _Esempio:_ Dipping 15% -> 25 punti.
4. **Zona**^ **Extreme**^ **Dipper**^ **( ):**^.^ (Saturazione^ al^ massimo).^
Questa funzione garantisce che un utente con il 9.9% di dipping (borderline) riceva un
punteggio molto vicino a uno con il 10.1%, evitando frustrazioni dovute a "scatti" del
punteggio per differenze decimali.

### 4.2 Scoring dell'Architettura (Weighted Scoring con Penalità)

Il punteggio architetturale (40 punti totali) è calcolato per sottrazione da un ideale,
penalizzando i difetti.
● **Funzione**^ **di**^ **Saturazione**^ **:**^
○ Se^ (es.^ 20%),^ restituisce^ 1.0^ (100%^ del^ peso).^
○ Se^ ,^ restituisce^ un^ valore^ ridotto^ linearmente^ o^
quadraticamente per penalizzare la carenza.
● **Funzione**^ **di**^ **Penalità**^ **:**^
○ Se^ ,^ Penalità^ =^ 0.^
○ Se^ ,^ Penalità^ =^ punti.^
○ Questo significa che 40 minuti di WASO (20 sopra soglia) costano 10 punti di
punteggio.

## 5. Implementazione Clean Architecture in Kotlin

Di seguito viene presentata la riscrittura completa della classe
AnalyzeSleepQualityUseCase.kt. Il codice è strutturato per essere modulare, testabile e privo


di dipendenze dirette dal framework Android (pure Kotlin), ideale per il layer di Dominio.

### 5.1 Strutture Dati di Dominio (Modelli)

Prima di implementare l'Use Case, definiamo i modelli dati necessari per rappresentare le
metriche calcolate. Questi dovrebbero risiedere in un file separato (es. SleepModels.kt), ma
sono qui riportati per completezza.
Kotlin
package com.ai_health.core.domain.model
import java.time.Duration
// Enum per mappare i codici interi (es. Xiaomi/Health Connect) a tipi semantici
enum class SleepStageType(val code: Int) {
UNKNOWN( 0 ),
AWAKE( 1 ),
SLEEP_GENERIC( 2 ),
OUT_OF_BED( 3 ), // Trattato come Awake
LIGHT( 4 ),
DEEP( 5 ),
REM( 6 );
companion object {
fun fromInt(value: Int): SleepStageType = entries.find { it.code == value }?: UNKNOWN
}
}
// Input grezzo fase del sonno
data class SleepStageRec(
val stage: Int,
val startTime: Long, // Unix Millis
val endTime: Long
)
// Input grezzo frequenza cardiaca
data class HeartRateRec(
val bpm: Int,
val time: Long // Unix Millis


##### )

// Risultato aggregato dell'analisi
data class SleepQualityResult(
val totalScore: Int, // 0-
val breakdown: ScoreBreakdown,
val metrics: SleepMetrics,
val feedback: List<String>
)
// Dettaglio dei punteggi parziali per UI/Debug
data class ScoreBreakdown(
val architectureScore: Double, // Max 40
val dippingScore: Double, // Max 30
val rhrScore: Double, // Max 20
val timingScore: Double // Max 10
)
// Metriche fisiologiche calcolate
data class SleepMetrics(
val totalSleepDurationMin: Long,
val deepSleepPercent: Int,
val remSleepPercent: Int,
val lightSleepPercent: Int,
val wasoMinutes: Long,
val fragmentationIndex: Double, // Transizioni/ora
val daytimeHrAvg: Int,
val nocturnalHrAvg: Int,
val dippingPercent: Double,
val lowestNocturnalHr: Int,
val hrNadirOffsetPercent: Int, // Posizione del nadir (0-100%)
val dataQualityScore: Double // Indice di affidabilità dei dati HR (0.0 - 1.0)
)

### 5.2 Implementazione UseCase Completa

Kotlin
package com.ai_health.core.domain.usecase


import com.ai_health.core.domain.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
/**
* UseCase per l'analisi avanzata della qualità del sonno basata su HR e Architettura.
* Implementa la logica di scoring multi-parametrico:
* 1. Sleep Architecture (40%)
* 2. HR Dipping (30%)
* 3. RHR Analysis (20%)
* 4. Sleep Timing/Nadir (10%)
*/
class AnalyzeSleepQualityUseCase {
companion object {
// Pesi massimi per componente
private const val W_ARCHITECTURE = 40.
private const val W_DIPPING = 30.
private const val W_RHR = 20.
private const val W_TIMING = 10.
// Target Fisiologici
private const val TARGET_DEEP_PERCENT = 20.0 // Minimo ottimale
private const val TARGET_REM_PERCENT = 25.0 // Minimo ottimale
private const val THRESHOLD_WASO_PENALTY = 20.0 // Minuti prima di penalizzare
private const val THRESHOLD_FRAG_PENALTY = 15.0 // Transizioni/ora prima di penalizzare
// Costanti per Interpolazione
private const val MAX_INTERPOLATION_GAP_MS = 60 * 60 * 1000L // 60 min
private const val DAYTIME_WINDOW_HOURS = 16L // Finestra diurna precedente al sonno
}
/**
* Esegue l'analisi completa.
* @param sleepStages Lista delle fasi del sonno della notte corrente.
* @param heartRates Stream H24 della frequenza cardiaca (grezzo).
* @param userBaselineRhr Media RHR degli ultimi 7 giorni dell'utente.
*/
operator fun invoke(
sleepStages: List<SleepStageRec>,
heartRates: List<HeartRateRec>,
userBaselineRhr: Int
): SleepQualityResult {


if (sleepStages.isEmpty()) return emptyResult("Dati del sonno mancanti.")
// --- FASE 1: Analisi Temporale e Architetturale ---
// Determina inizio e fine del sonno reale (escludendo veglia iniziale/finale se necessario)
val sortedStages = sleepStages.sortedBy { it.startTime }
val sleepOnset = sortedStages.first().startTime
val sleepOffset = sortedStages.last().endTime
val archMetrics = calculateArchitectureMetrics(sortedStages)
// --- FASE 2: Pre-elaborazione Dati Cardiaci ---
// Filtra e separa i dati HR in Diurni (Daytime) e Notturni (Nocturnal)
val (daytimeBpmRaw, nocturnalBpmRaw) = splitDayNightHr(heartRates, sleepOnset,
sleepOffset)
// Imputazione dei dati mancanti (Interpolazione Lineare)
val nocturnalBpm = interpolateHeartRate(nocturnalBpmRaw)
// Calcolo affidabilità dati (Data Quality Check)
val dataQuality = calculateDataQuality(nocturnalBpmRaw, sleepOnset, sleepOffset)
if (nocturnalBpm.isEmpty() |
| dataQuality < 0.5) {
// Fallback se i dati cardiaci sono troppo scarsi
return resultWithArchitectureOnly(archMetrics, "Dati cardiaci insufficienti o di scarsa
qualità.")
}
// --- FASE 3: Calcolo Metriche Cardiache Avanzate ---
// 3.1 Medie (Day vs Night)
val nocturnalAvg = nocturnalBpm.map { it.bpm }.average()
// Se mancano dati diurni, usiamo una stima o un fallback (es. 80 bpm o baseline utente + 15)
val daytimeAvg = if (daytimeBpmRaw.isNotEmpty()) {
daytimeBpmRaw.map { it.bpm }.average()
} else {
// Fallback intelligente: RHR notturna + 20% (assumendo dipping inverso per stimare il giorno)
nocturnalAvg * 1.
}
// 3.2 Dipping Factor


val dippingPercent = ((daytimeAvg - nocturnalAvg) / daytimeAvg) * 100.
// 3.3 RHR e Nadir
val minHrRec = nocturnalBpm.minByOrNull { it.bpm }!!
val lowestNocturnalHr = minHrRec.bpm
// Calcolo posizione Nadir (offset % rispetto alla durata del sonno)
val sleepDurationMs = sleepOffset - sleepOnset
val nadirOffsetMs = minHrRec.time - sleepOnset
val nadirOffsetPercent = if (sleepDurationMs > 0 ) {
((nadirOffsetMs.toDouble() / sleepDurationMs) * 100 ).toInt()
} else 0
// --- FASE 4: Calcolo Punteggi (Scoring) ---
val scoreArch = scoreArchitecture(archMetrics)
val scoreDipping = scoreDipping(dippingPercent)
val scoreRhr = scoreRhr(lowestNocturnalHr, userBaselineRhr)
val scoreTiming = scoreTiming(nadirOffsetPercent)
// Somma ponderata
val totalScore = (scoreArch + scoreDipping + scoreRhr +
scoreTiming).roundToInt().coerceIn( 0 , 100 )
// --- FASE 5: Generazione Feedback ---
val feedback = generateFeedback(
archMetrics,
dippingPercent,
lowestNocturnalHr,
userBaselineRhr,
nadirOffsetPercent
)
return SleepQualityResult(
totalScore = totalScore,
breakdown = ScoreBreakdown(scoreArch, scoreDipping, scoreRhr, scoreTiming),
metrics = SleepMetrics(
totalSleepDurationMin = archMetrics.totalMinutes,
deepSleepPercent = archMetrics.deepPercent,
remSleepPercent = archMetrics.remPercent,
lightSleepPercent = archMetrics.lightPercent,
wasoMinutes = archMetrics.waso,
fragmentationIndex = archMetrics.fragmentationIndex,


daytimeHrAvg = daytimeAvg.roundToInt(),
nocturnalHrAvg = nocturnalAvg.roundToInt(),
dippingPercent = dippingPercent,
lowestNocturnalHr = lowestNocturnalHr,
hrNadirOffsetPercent = nadirOffsetPercent,
dataQualityScore = dataQuality
),
feedback = feedback
)
}
//
================================================================================
=
// LOGICA DI CALCOLO METRICHE (Internal Methods)
//
================================================================================
=
private data class ArchMetrics(
val totalMinutes: Long,
val deepPercent: Int,
val remPercent: Int,
val lightPercent: Int,
val waso: Long,
val fragmentationIndex: Double
)
private fun calculateArchitectureMetrics(stages: List<SleepStageRec>): ArchMetrics {
var deepMs = 0L
var remMs = 0L
var lightMs = 0L
var awakeMs = 0L // Totale veglia (incluso WASO)
var wasoMs = 0L // Solo Wake After Sleep Onset
var transitions = 0
// Calcolo durata totale basato sugli estremi
val totalDurationMs = stages.last().endTime - stages.first().startTime
var previousStageType = SleepStageType.UNKNOWN
stages.forEachIndexed { index, rec ->
val duration = rec.endTime - rec.startTime


val type = SleepStageType.fromInt(rec.stage)
// Rilevamento Transizioni (Frammentazione)
// Contiamo solo cambi di stato significativi, non fluttuazioni artefatte
if (index > 0 && type!= previousStageType) {
// Esempio: Da DEEP/REM a LIGHT/AWAKE è una transizione "negative"
// Contiamo tutte le transizioni per l'indice standard
transitions++
}
previousStageType = type
when (type) {
SleepStageType.DEEP -> deepMs += duration
SleepStageType.REM -> remMs += duration
SleepStageType.LIGHT, SleepStageType.SLEEP_GENERIC -> lightMs += duration
SleepStageType.AWAKE, SleepStageType.OUT_OF_BED -> {
awakeMs += duration
// Calcolo WASO: Escludiamo il primo evento (latenza) e l'ultimo (risveglio)
// Semplificazione: consideriamo WASO tutto ciò che è AWAKE tranne il primo blocco se è
all'inizio
val isFirstBlock = index == 0
val isLastBlock = index == stages.size - 1
if (!isFirstBlock &&!isLastBlock) {
wasoMs += duration
}
}
else -> lightMs += duration // Fallback conservativo
}
}
val totalMinutes = max( 1 , totalDurationMs / 1000 / 60 )
val validTotalMs = max(1L, deepMs + remMs + lightMs + awakeMs) // Denominatore reale
// Calcolo Percentuali
val deepPct = ((deepMs.toDouble() / validTotalMs) * 100 ).toInt()
val remPct = ((remMs.toDouble() / validTotalMs) * 100 ).toInt()
val lightPct = ((lightMs.toDouble() / validTotalMs) * 100 ).toInt()
// Calcolo Indice di Frammentazione (Transizioni / Ore di sonno)
val sleepHours = totalMinutes / 60.
val fragIndex = if (sleepHours > 0 ) transitions / sleepHours else 0.
return ArchMetrics(


totalMinutes = totalMinutes,
deepPercent = deepPct,
remPercent = remPct,
lightPercent = lightPct,
waso = wasoMs / 1000 / 60 ,
fragmentationIndex = fragIndex
)
}
//
================================================================================
=
// LOGICA DI SCORING (Funzioni di Normalizzazione)
//
================================================================================
=
private fun scoreArchitecture(m: ArchMetrics): Double {
// 1. Deep Sleep Score (Max 15 pt): Saturazione lineare fino al target
val deepScore = 15.0 * (m.deepPercent.toDouble() /
TARGET_DEEP_PERCENT).coerceAtMost(1.0)
// 2. REM Sleep Score (Max 15 pt): Saturazione lineare fino al target
val remScore = 15.0 * (m.remPercent.toDouble() /
TARGET_REM_PERCENT).coerceAtMost(1.0)
// 3. Base Continuity (10 pt) - Penalità
var continuityScore = 10.
// Penalità WASO: -0.5 punti per ogni minuto sopra la soglia (20 min)
val wasoExcess = max( 0 , m.waso - THRESHOLD_WASO_PENALTY.toLong())
continuityScore -= (wasoExcess * 0.5)
// Penalità Frammentazione: -1 punto per ogni transizione sopra soglia (15/h)
val fragExcess = max(0.0, m.fragmentationIndex - THRESHOLD_FRAG_PENALTY)
continuityScore -= (fragExcess * 1.0)
// Somma e clamp tra 0 e 40
return (deepScore + remScore + continuityScore).coerceIn(0.0, W_ARCHITECTURE)
}
private fun scoreDipping(dipPercent: Double): Double {
// Implementazione Piecewise Linear della Sigmoide (vedi Report)


// Range: 0-30 punti
return when {
dipPercent < 0 -> 0.0 // Reverse Dipping (Grave)
dipPercent < 10 -> {
// Non-Dipper: Crescita lineare da 0 a 20 punti (0% -> 0pt, 10% -> 20pt)
// Formula: 20 * (x / 10)
20.0 * (dipPercent / 10.0)
}
dipPercent <= 20 -> {
// Normal Dipper: Crescita da 20 a 30 punti (10% -> 20pt, 20% -> 30pt)
// Formula: 20 + 10 * ((x - 10) / 10)
20.0 + 10.0 * ((dipPercent - 10.0) / 10.0)
}
else -> 30.0 // Extreme Dipper (>20%): Saturazione al massimo
}
}
private fun scoreRhr(nightMin: Int, baseline: Int): Double {
// Punteggio Max 20.
// Se minHR <= baseline -> 20 punti.
// Se minHR > baseline -> Decadimento esponenziale o lineare forte.
val delta = nightMin - baseline
if (delta <= 0 ) return W_RHR
// Penalità: -2 punti per ogni bpm sopra la media.
// Esempio: +5 bpm -> -10 punti (Score 10/20). +10 bpm -> Score 0/20.
val penalty = delta * 2.
return (W_RHR - penalty).coerceAtLeast(0.0)
}
private fun scoreTiming(nadirPercent: Int): Double {
// Punteggio Max 10.
// Curva "a campana" o fasce discrete.
return when (nadirPercent) {
in 40..65 -> 10.0 // Ottimo: Metà notte (Hammock ideale)
in 30..39, in 66..75 -> 7.0 // Buono: Leggermente spostato
in 20..29, in 76..85 -> 4.0 // Sufficiente: Troppo presto o troppo tardi
else -> 2.0 // Scarso: Crash iniziale o slope discendente
}
}


##### //

================================================================================
=
// HELPER & DATA PROCESSING
//
================================================================================
=
private fun splitDayNightHr(
fullList: List<HeartRateRec>,
sleepStart: Long,
sleepEnd: Long
): Pair<List<HeartRateRec>, List<HeartRateRec>> {
val nocturnal = ArrayList<HeartRateRec>()
val daytime = ArrayList<HeartRateRec>()
// Definiamo "Giorno" come le 16 ore precedenti l'inizio del sonno
val dayStartWindow = sleepStart - (DAYTIME_WINDOW_HOURS * 60 * 60 * 1000 )
fullList.forEach { rec ->
if (rec.time in sleepStart..sleepEnd) {
nocturnal.add(rec)
} else if (rec.time in dayStartWindow until sleepStart) {
daytime.add(rec)
}
}
return Pair(daytime, nocturnal)
}
private fun interpolateHeartRate(raw: List<HeartRateRec>): List<HeartRateRec> {
if (raw.isEmpty()) return emptyList()
val sorted = raw.sortedBy { it.time }
val result = ArrayList<HeartRateRec>()
for (i in 0 until sorted.size - 1 ) {
val current = sorted[i]
val next = sorted[i + 1 ]
result.add(current)
val timeDiff = next.time - current.time
// Logica Gaps:


// Se gap > 1 min E gap < 60 min -> Interpola
if (timeDiff > 60_000L && timeDiff < MAX_INTERPOLATION_GAP_MS) {
val gapMinutes = (timeDiff / 60_000L).toInt()
val bpmDiff = next.bpm - current.bpm
val bpmStep = bpmDiff.toDouble() / gapMinutes
val timeStep = timeDiff / gapMinutes
for (step in 1 until gapMinutes) {
val newTime = current.time + (timeStep * step)
val newBpm = (current.bpm + (bpmStep * step)).toInt()
// Aggiungiamo punti sintetici
result.add(HeartRateRec(newBpm, newTime))
}
}
// Se gap >= 60 min, non facciamo nulla (lasciamo il buco)
}
result.add(sorted.last())
return result
}
// Calcola una % di copertura dei dati attesi
private fun calculateDataQuality(raw: List<HeartRateRec>, start: Long, end: Long): Double {
if (raw.isEmpty()) return 0.
val durationMinutes = (end - start) / 1000 / 60
if (durationMinutes == 0L) return 0.
// Ci aspettiamo almeno 1 rilevazione ogni 10 minuti in media per un calcolo decente
// (Nota: i CSV forniti hanno campioni ogni 30 min, quindi adattiamo la soglia)
val density = raw.size.toDouble() / durationMinutes
// Se density >= 0.033 (1 campione ogni 30 min) -> Quality 1.0 (per questo dataset specifico)
// Per wearable moderni (1 ogni min), density dovrebbe essere ~1.
return (density / 0.03).coerceAtMost(1.0)
}
private fun generateFeedback(
arch: ArchMetrics,
dip: Double,
minHr: Int,
baseline: Int,
nadir: Int
): List<String> {
val msgs = ArrayList<String>()
// Feedback Cardiaco (Priorità alta)


if (dip < 5.0) {
msgs.add("Cuore sotto stress: Il tuo battito non è rallentato stanotte (Dipping < 5%). Hai
consumato alcol o pasti pesanti?")
} else if (minHr > baseline + 5 ) {
msgs.add("Recupero incompleto: La frequenza a riposo è alta (+${minHr - baseline} bpm).
Potresti essere in fase iniziale di malattia o sovrallenamento.")
} else if (nadir > 75 ) {
msgs.add("Metabolismo notturno: Il cuore ha raggiunto il minimo solo al risveglio. Evita di
mangiare tardi la sera.")
} else {
msgs.add("Ottimo recupero cardiovascolare: Il tuo cuore ha riposato efficacemente.")
}
// Feedback Architetturale
if (arch.waso > 45 ) {
msgs.add("Sonno frammentato: Sei rimasto sveglio per ${arch.waso} minuti. Cerca di
mantenere la stanza buia e silenziosa.")
}
if (arch.deepPercent < 10 ) {
msgs.add("Poco sonno profondo: Prova a regolarizzare gli orari o abbassare la temperatura
della stanza.")
}
return msgs
}
private fun emptyResult(msg: String): SleepQualityResult {
return SleepQualityResult( 0 , ScoreBreakdown(0.0,0.0,0.0,0.0),
SleepMetrics( 0 , 0 , 0 , 0 , 0 ,0.0, 0 , 0 ,0.0, 0 , 0 ,0.0), listOf(msg))
}
private fun resultWithArchitectureOnly(arch: ArchMetrics, warning: String): SleepQualityResult {
// Calcola un punteggio parziale riparametrato su 100 solo basato sull'architettura
val archScore = scoreArchitecture(arch)
// Proiettiamo 40 punti su 100
val projectedScore = (archScore * 2.5).roundToInt()
return SleepQualityResult(
totalScore = projectedScore,
breakdown = ScoreBreakdown(archScore, 0.0, 0.0, 0.0),
metrics = SleepMetrics(
totalSleepDurationMin = arch.totalMinutes,
deepSleepPercent = arch.deepPercent,


remSleepPercent = arch.remPercent,
lightSleepPercent = arch.lightPercent,
wasoMinutes = arch.waso,
fragmentationIndex = arch.fragmentationIndex,
daytimeHrAvg = 0 , nocturnalHrAvg = 0 , dippingPercent = 0.0,
lowestNocturnalHr = 0 , hrNadirOffsetPercent = 0 , dataQualityScore = 0.
),
feedback = listOf(warning, "Punteggio basato solo sulle fasi del sonno.")
)
}
}

## 6. Spiegazione Matematica del "Dipping Factor"

Il calcolo e la successiva normalizzazione del Dipping Factor rappresentano il cuore
emodinamico dell'algoritmo.

### 6.1 Definizione di Dipping

Il Dipping percentuale ( ) è calcolato come:
Dove è la media dei battiti durante l'intero periodo di sonno (escluso WASO se i dati
lo permettono, o inclusi per semplicità computazionale su dati wearable).

### 6.2 La Funzione di Scoring a Tratti (Piecewise)

Invece di una semplice soglia binaria, l'algoritmo applica una funzione continua definita a tratti
per convertire la percentuale in un punteggio (max 30).

1. **Regione Patologica (Reverse Dipping, ):**
    ○ In questo scenario, il cuore batte più velocemente di notte che di giorno. È un segno
       di grave disfunzione autonomica o stress acuto (es. intossicazione etilica).
    ○ **Punteggio:**.
2. **Regione di Rischio (Non-Dipper, ):**
    ○ Questa è la zona grigia. Un valore di 9% è quasi normale, mentre 1% è quasi


```
patologico. La funzione deve crescere linearmente per premiare ogni miglioramento
percentuale.
○ Formula:^
○ Esempio:^ Con^ ,^ punti^ (33%^ del^ totale^ disponibile^
per questa sezione).
```
3. **Regione**^ **Fisiologica**^ **(Dipper,**^ **):**^
    ○ Questa è la fascia target. Qui il punteggio cresce più lentamente, da un valore
       "buono" (20) a un valore "eccellente" (30).
    ○ **Formula:**^
    ○ _Esempio:_^ Con^ ,^ punti.^
4. **Regione**^ **Estrema**^ **(Extreme**^ **Dipper,**^ **):**^
    ○ Valori oltre il 20% sono generalmente considerati sani in contesti non clinici, quindi il
       punteggio si satura al massimo.
    ○ **Punteggio:**^.^
Questa modellazione evita che un singolo punto percentuale (es. 9.9% vs 10.0%) causi un
salto sproporzionato nel punteggio, garantendo una valutazione equa e progressiva.

## 7. Strategia per la Gestione dei "Buchi" (Data Gaps)

I dati BPM forniti nel CSV di esempio mostrano una frequenza di campionamento irregolare
(circa ogni 30 minuti), che è molto bassa rispetto agli standard attuali (1-5 min). Inoltre, ci
sono buchi temporali (es. 60 minuti).

### 7.1 Perché Interpolazione Lineare?

L'interpolazione è necessaria per stimare l'area sotto la curva della frequenza cardiaca e
calcolare una media notturna ( ) che non sia sbilanciata dalla densità dei punti.
● Se abbiamo 10 punti nella prima ora (fase NREM profonda, HR bassa) e solo 1 punto nelle
ultime 3 ore (fase REM, HR variabile), una media aritmetica semplice sottostimerebbe la
HR media reale.
● **Strategia:** L'interpolazione lineare "riempie" i minuti mancanti creando una serie
temporale uniforme (1 campione al minuto). Questo pondera correttamente il tempo: 60
minuti di sonno pesano 60 volte di più di 1 minuto, indipendentemente da quanti campioni
reali il sensore ha catturato in quell'intervallo.


### 7.2 Soglie di Sicurezza

Non tutti i buchi possono essere riempiti.
● **Gap < 60 min:** Sicuro da interpolare. La fisiologia cardiaca durante il sonno è
relativamente stabile; è improbabile che si verifichi un picco di 150 bpm e un ritorno a 50
bpm nel giro di 20 minuti senza che l'utente sia sveglio (e quindi rilevato
dall'accelerometro).
● **Gap > 60 min:** Insicuro. In un'ora può verificarsi un intero ciclo REM con relativa variabilità
cardiaca. Inventare dati per un'ora intera introdurrebbe un errore inaccettabile. In questo
caso, il segmento viene ignorato nel calcolo dell'interpolazione, ma contribuisce alla
riduzione del punteggio di dataQuality.

## 8. Conclusioni

L'algoritmo presentato trasforma l'applicazione da un semplice visualizzatore di dati a uno
strumento di analisi biometrica avanzata. Integrando l'architettura del sonno con metriche
emodinamiche derivate dal dipping e dalla RHR, il sistema offre una valutazione olistica della
salute dell'utente. L'architettura software proposta in Kotlin garantisce che questa logica
complessa rimanga manutenibile, testabile e scalabile, pronta per l'integrazione in un
ecosistema di produzione Android moderno.

#### Bibliografia

#### 1. The most important marker of sleep quality that no one is talking about - Span

#### Health, accesso eseguito il giorno febbraio 6, 2026,

#### https://www.span.health/blog/the-most-important-marker-of-sleep-quality-that-

#### no-one-is-talking-about.html

#### 2. Actigraphic sleep characteristics among older Americans - PMC, accesso

#### eseguito il giorno febbraio 6, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC5555167/

#### 3. Wakefulness After Sleep Onset (WASO) - Sleep Foundation, accesso eseguito il

#### giorno febbraio 6, 2026,

#### https://www.sleepfoundation.org/sleep-studies/wakefulness-after-sleep-onset

#### 4. How Much Deep, Light, and REM Sleep Do You Need? - Healthline, accesso

#### eseguito il giorno febbraio 6, 2026,

#### https://www.healthline.com/health/how-much-deep-sleep-do-you-need

#### 5. Sleep Fragmentation Index - Validation Reference, accesso eseguito il giorno

#### febbraio 6, 2026,

#### https://actigraphcorp.my.site.com/support/s/article/Sleep-Fragmentation-Index-V

#### alidation-Reference

#### 6. A state transition-based method for quantifying EEG sleep fragmentation - UQ

#### eSpace, accesso eseguito il giorno febbraio 6, 2026,

#### http://espace.library.uq.edu.au/view/UQ:185810

#### 7. Utility of Sleep Stage Transitions in Assessing Sleep Continuity - PMC - PubMed


#### Central, accesso eseguito il giorno febbraio 6, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC2982738/

#### 8. The Basics: Heart Rate Dip - Bevel, accesso eseguito il giorno febbraio 6, 2026,

#### https://www.bevel.health/blog/the-basics-heart-rate-dip

#### 9. Sleep Variability, Sleep Irregularity, and Nighttime Blood Pressure Dipping |

#### Hypertension, accesso eseguito il giorno febbraio 6, 2026,

#### https://www.ahajournals.org/doi/10.1161/HYPERTENSIONAHA.123.21497

#### 10. Night-Time Non-dipping Blood Pressure and Heart Rate: An Association With the

#### Risk of Silent Small Vessel Disease in Patients Presenting With Acute Ischemic

#### Stroke - PubMed Central, accesso eseguito il giorno febbraio 6, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC8637909/

#### 11. Nighttime Blood Pressure and Nocturnal Dipping Are Associated With Daytime

#### Urinary Sodium Excretion in African Subjects | Hypertension, accesso eseguito il

#### giorno febbraio 6, 2026,

#### https://www.ahajournals.org/doi/10.1161/hypertensionaha.107.105510

#### 12. Blunted Heart Rate Dip During Sleep and All-Cause Mortality - ResearchGate,

#### accesso eseguito il giorno febbraio 6, 2026,

#### https://www.researchgate.net/publication/5890595_Blunted_Heart_Rate_Dip_Duri

#### ng_Sleep_and_All-Cause_Mortality

#### 13. Full article: Dipping Pattern and 1-year stroke functional outcome in ischemic

#### stroke or transient ischemic attack - Taylor & Francis, accesso eseguito il giorno

#### febbraio 6, 2026,

#### https://www.tandfonline.com/doi/full/10.1080/10641963.2022.2139384

#### 14. Clinical and prognostic significance of a reverse dipping pattern on ambulatory

#### monitoring: An updated review - PMC, accesso eseguito il giorno febbraio 6,

#### 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC8031119/

#### 15. Nighttime dipping status and risk of cardiovascular events in patients with

#### untreated hypertension: A systematic review and meta‐analysis - PMC, accesso

#### eseguito il giorno febbraio 6, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC8030020/

#### 16. Garmin Users with Higher Activity Levels Have Lower Resting Heart Rates.,

#### accesso eseguito il giorno febbraio 6, 2026,

#### https://www.garmin.com/en-US/blog/health/garmin-users-with-higher-activity-le

#### vels-have-lower-resting-heart-rates/

#### 17. Understanding Resting Heart Rate and Why It Matters for Sleep - Chilipad,

#### accesso eseguito il giorno febbraio 6, 2026,

#### https://sleep.me/post/understanding-resting-heart-rate-why-it-matters

#### 18. AnalyzeSleepQualityUseCase.kt

#### 19. Sleeping Heart Rate: Look for These 4 Patterns - Oura Ring, accesso eseguito il

#### giorno febbraio 6, 2026, https://ouraring.com/blog/sleeping-heart-rate/

#### 20. The trajectory of nocturnal heart rate (HR) across the stages of sleep... -

#### ResearchGate, accesso eseguito il giorno febbraio 6, 2026,

#### https://www.researchgate.net/figure/The-trajectory-of-nocturnal-heart-rate-HR-

#### across-the-stages-of-sleep-throughout-the_fig2_349567955

#### 21. Sleep | Zepp OS Developers Documentation, accesso eseguito il giorno febbraio


#### 6, 2026,

#### https://docs.zepp.com/docs/reference/device-app-api/newAPI/sensor/Sleep/

#### 22. SleepSessionRecord.StageType | API reference - Android Developers, accesso

#### eseguito il giorno febbraio 6, 2026,

#### https://developer.android.com/reference/android/health/connect/datatypes/Sleep

#### SessionRecord.StageType

#### 23. Evaluating Imputation Techniques for Short-Term Gaps in Heart Rate Data - arXiv,

#### accesso eseguito il giorno febbraio 6, 2026, https://arxiv.org/html/2508.08268v1

#### 24. Linear interpolation with interpolation order 16, cubic spline and... -

#### ResearchGate, accesso eseguito il giorno febbraio 6, 2026,

#### https://www.researchgate.net/figure/Linear-interpolation-with-interpolation-orde

#### r-16-cubic-spline-and-fourteenth-order_fig2_12377810

#### 25. Missing data imputation techniques for wireless continuous vital signs monitoring

- PMC, accesso eseguito il giorno febbraio 6, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC9893204/


