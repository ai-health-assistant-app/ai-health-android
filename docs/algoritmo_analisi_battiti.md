# Algoritmi Biometrici Avanzati per

# Android: Derivazione di Intelligence su

# Performance e Stile di Vita da Serie

# Temporali di Frequenza Cardiaca

## 1. Analisi Architetturale e Potenziale Fisiologico dei

## Dati

L'integrazione di tecnologie indossabili negli ecosistemi di salute mobile ha segnato una
transizione fondamentale dal semplice tracciamento delle attività al monitoraggio fisiologico
sofisticato. Per un'applicazione Android che già possiede l'infrastruttura per acquisire sessioni
di frequenza cardiaca (HR) e dispone di un modulo avanzato per l'analisi del sonno, la
frontiera successiva nella creazione di valore risiede nella derivazione matematica di metriche
di secondo e terzo ordine. Queste metriche non si limitano a riportare "cosa" è accaduto, ma
quantificano "quanto" è costato all'organismo e "come" il sistema sta recuperando.
L'analisi approfondita della struttura dei dati fornita, specificamente il formato samplesJson
contenente valori di frequenza cardiaca con offset temporali, rivela sia vincoli tecnici che
opportunità significative. La granularità dei dati, che appare prevalentemente impostata su
una risoluzione di un campione al minuto (1/60 Hz) con occasionali lacune, impone un
approccio algoritmico distinto rispetto ai sistemi che dispongono di intervalli battito-battito
(RR intervals) grezzi. Sebbene questa risoluzione precluda il calcolo diretto delle metriche
cliniche di Variabilità della Frequenza Cardiaca (HRV) nel dominio del tempo e della
frequenza—come l'RMSSD (Root Mean Square of Successive Differences) o la potenza
spettrale HF/LF, che richiedono una precisione al millisecondo per rilevare l'aritmia sinusale
respiratoria—essa è tuttavia ricca di potenziale per la modellazione del carico interno,
dell'efficienza metabolica e delle tendenze autonomiche a lungo termine.^1
Questo rapporto tecnico esplora esaustivamente le metriche calcolabili, suddivise in tre
domini critici: **Quantificazione del Carico di Allenamento (Performance)** , **Monitoraggio
Metabolico e dello Stile di Vita** , e **Valutazione della Prontezza (Readiness) e dello Stress**.
L'obiettivo è fornire una roadmap ingegneristica e fisiologica per trasformare serie temporali
sparse in un motore di coaching biometrico predittivo.

### 1.1. Deostruzione del Formato Dati e Implicazioni di Segnale

La struttura JSON identificata ([{"o":0,"v":63},{"o":60000,"v":61}...]) definisce un vettore di
coppie tempo-valore dove l'offset o rappresenta i millisecondi dall'inizio della sessione e v la
frequenza cardiaca media in quel periodo. L'osservazione di intervalli regolari di 60.000 ms (


minuto) in alcuni segmenti, alternati a gap irregolari di 3-5 minuti, suggerisce che il dispositivo
operi in una modalità di campionamento conservativo o "smart recording", che registra i dati
solo al variare significativo del segnale o per risparmiare batteria.^1
Questa caratteristica richiede l'implementazione di una pipeline di pre-elaborazione robusta
all'interno dell'applicazione Android. Prima di poter calcolare metriche avanzate come il TRIMP
(Training Impulse), è imperativo normalizzare il flusso di dati. L'algoritmo di interpolazione
diventa quindi un componente critico. Mentre l'interpolazione lineare è computazionalmente
economica e sufficiente per le tendenze generali, l'interpolazione spline cubica è
raccomandata per ricostruire curve di frequenza cardiaca più naturali, specialmente per il
calcolo di derivate o integrazioni dell'area sotto la curva, riducendo l'errore introdotto dalla
discretizzazione.^2
Inoltre, la natura aggregata del dato (bpm medio per minuto) agisce come un filtro
passa-basso naturale, eliminando le componenti ad alta frequenza del segnale cardiaco.
Questo significa che mentre non possiamo misurare direttamente la modulazione vagale
istantanea (parasimpatico), possiamo osservare con eccellente fedeltà le fluttuazioni a bassa
frequenza e i trend circadiani che riflettono l'equilibrio simpato-vagale globale e le risposte
metaboliche. Pertanto, le metriche proposte in questo rapporto sono state selezionate
specificamente per la loro robustezza rispetto a questa frequenza di campionamento,
privilegiando analisi di trend, carichi cumulativi e morfologia del recupero rispetto alla
micro-variabilità.^4

## 2. Analisi della Performance: Quantificazione del

## Carico Interno

Nel contesto dell'allenamento sportivo e del fitness avanzato, la frequenza cardiaca grezza è
un indicatore incompleto. Per fornire valore reale all'utente, l'applicazione deve tradurre i bpm
in "costo fisiologico". Il concetto di Carico Interno (Internal Training Load) è fondamentale:
rappresenta lo stress biologico effettivo imposto all'organismo, indipendentemente dal lavoro
esterno (velocità o potenza) svolto.

### 2.1. L'Impulso di Allenamento (TRIMP): Il Gold Standard per i Dati

### Cardiaci

Il **TRIMP (Training Impulse)** è la metrica più scientificamente validata per quantificare il
carico di allenamento basato sulla frequenza cardiaca. Sviluppato originariamente dal Dr. Eric
Banister, il TRIMP risolve il problema della non-linearità dello stress fisiologico: un minuto di
corsa al 90% della frequenza cardiaca massima è esponenzialmente più tassante di un minuto
al 60%, non linearmente proporzionale.^5


#### 2.1.1. Modello Esponenziale di Banister

Per la vostra applicazione, l'implementazione del modello esponenziale di Banister è
fortemente raccomandata rispetto ai modelli a zone lineari (come quello di Edwards), poiché
offre una risoluzione continua e riflette meglio l'accumulo di lattato ematico durante
l'esercizio.^7
La formula fondamentale da implementare nel codice Android è:
Dove:
● è^ la^ durata^ dell'intervallo^ di^ campionamento^ in^ minuti^ (nel^ vostro^ caso,^ tipicamente^ 1.0,^
ma variabile in base ai gap dei dati).
● è^ la^ Frequenza^ Cardiaca^ di^ Riserva^ frazionaria,^ un^ indicatore^ normalizzato^
dell'intensità:
È cruciale utilizzare i dati biometrici dell'utente: è il valore campionato, è
la frequenza a riposo (che l'app può derivare dall'analisi del sonno), e è la
frequenza massima.^6
● è il fattore di ponderazione esponenziale che modella la curva del lattato, specifico
per sesso:
○ **Uomini:**^
○ **Donne:**^
**Logica di Implementazione:**
L'algoritmo deve iterare attraverso l'array samplesJson. Per ogni punto dati, si calcola il TRIMP
parziale per quell'intervallo temporale. Se ci sono gap significativi (es. > 5 minuti), l'algoritmo
dovrebbe interpolare i punti intermedi o, prudentemente, considerare il carico come nullo se si
presume che il dispositivo sia stato rimosso, a meno che i dati dell'accelerometro (se
disponibili) non suggeriscano attività. La somma di tutti i TRIMP parziali fornisce il **Carico
Sessione (Session TRIMP)**.
Un valore TRIMP giornaliero permette all'utente di confrontare mele con pere: 30 minuti di HIIT
potrebbero generare 80 TRIMP, equivalenti a 90 minuti di corsa lenta. Questa normalizzazione


è essenziale per la gestione del volume di allenamento.^8

#### 2.1.2. Edwards TRIMP: Un'Alternativa Zonale

Sebbene il modello di Banister sia superiore per precisione, il metodo di Edwards può essere
offerto come alternativa più intuitiva per gli utenti abituati alle "zone cardio". Questo metodo
suddivide la FC in 5 zone percentuali e assegna un coefficiente lineare 7 :
● Zona 1 (50-60% FCmax): Coefficiente 1
● Zona 2 (60-70% FCmax): Coefficiente 2
● ...
● Zona 5 (90-100% FCmax): Coefficiente 5
Il vantaggio di calcolare entrambi è la possibilità di mostrare all'utente la distribuzione del
carico. Tuttavia, per l'analisi predittiva della performance, il TRIMP di Banister rimane la scelta
prioritaria.

### 2.2. Heart Rate Training Stress Score (hrTSS)

Per elevare l'analisi al livello di piattaforme come TrainingPeaks, l'applicazione dovrebbe
calcolare l' **hrTSS**. Mentre il TRIMP è un valore assoluto, l'hrTSS normalizza lo stress rispetto
alla soglia anaerobica dell'utente, rendendo il punteggio comparabile con metriche basate
sulla potenza (TSS) usate nel ciclismo.^12
La formula richiede la stima della Frequenza Cardiaca alla Soglia del Lattato (LTHR), che può
essere approssimata come la FC media di un test massimale di 30 minuti o inserita
manualmente dall'utente.
Dove è il punteggio TRIMP che l'utente accumulerebbe in 60
minuti mantenendo esattamente la sua LTHR. Questo calcolo fornisce un valore dove "100"
rappresenta un'ora di sforzo massimale sostenibile (Time Trial). Questo è estremamente utile
per gli atleti multisport che necessitano di unificare carichi provenienti da discipline diverse.^14

### 2.3. Modellazione Fitness-Fatigue (Performance Manager)

L'accumulo dei dati di carico (TRIMP o hrTSS) nel tempo permette di implementare un modello
matematico di adattamento fisiologico noto come "Impulse-Response Model" o Performance
Manager.^15 Questo è probabilmente l'insight più potente che l'applicazione può fornire per la


pianificazione a lungo termine.
Il modello si basa su tre curve derivate:

1. **Chronic Training Load (CTL) - "Fitness":** Rappresenta la condizione atletica acquisita
   ed è calcolato come una media mobile esponenziale (EWMA) del carico giornaliero su un
   periodo lungo (tipicamente 42 giorni).
   Un CTL in crescita indica un aumento della capacità di lavoro e della fitness
   cardiovascolare.^17
2. **Acute Training Load (ATL) - "Fatica":** Rappresenta lo stress acuto accumulato
   recentemente, calcolato come EWMA su un periodo breve (tipicamente 7 giorni).
   L'ATL reagisce rapidamente ai giorni di allenamento pesante e al riposo.^16
3. **Training Stress Balance (TSB) - "Forma":** La differenza tra fitness e fatica.
   ○ **TSB Positivo (> +5):** Indica freschezza. L'atleta è riposato e pronto per la
   prestazione (tapering).
   ○ **TSB Negativo (-10 a -30):** Indica una fase di carico produttivo.
   ○ **TSB Profondamente Negativo (< -30):** Rischio elevato di overtraining o malattia.^18
   Implementando questi algoritmi, l'app può fornire consigli dinamici: "Il tuo TSB è -35, rischio
   infortuni elevato. Consigliato riposo oggi" oppure "TSB +10, sei in condizione ottimale per una
   gara".

### 2.4. Decoppiamento Aerobico e Deriva Cardiaca (Cardiac Drift)

L'efficienza cardiovascolare può essere misurata analizzando la relazione tra output (passo o
potenza, se disponibili, o semplicemente costanza dello sforzo) e input (frequenza cardiaca)
nel tempo. Questo fenomeno è noto come **Decoppiamento Aerobico**.^20
Se l'applicazione ha accesso ai dati di velocità/passo (GPS) o cadenza, può calcolare il
rapporto **Pa:HR** (Pace-to-Heart Rate). In una sessione aerobica steady-state (es. corsa lunga
a ritmo costante), la FC dovrebbe rimanere stabile. Tuttavia, a causa della fatica,
disidratazione e termoregolazione, la FC tende a salire anche a ritmo costante.
**Algoritmo di Calcolo:**

1. Identificare sessioni aerobiche lunghe (>40 min) e stabili.
2. Dividere la sessione in due metà (es. primi 40% vs ultimi 40%, escludendo


```
riscaldamento/defaticamento).
```
3. Calcolare^ il^ rapporto^ medio^ per^ entrambe^ le^ metà.^
4.
Un valore inferiore al 5% indica un eccellente adattamento aerobico ("durabilità"). Valori
superiori indicano che l'atleta perde efficienza e necessita di più allenamento di base.^22 Se i
dati di passo non sono affidabili, si può analizzare la **Deriva della Frequenza Cardiaca** pura
(aumento dei bpm) assumendo uno sforzo costante, sebbene con minore precisione.^20

## 3. Analisi Metabolica e dello Stile di Vita

Oltre alla performance atletica, i dati cardiaci offrono una finestra unica sul metabolismo
dell'utente e sulle risposte fisiologiche allo stile di vita quotidiano.

### 3.1. Analisi della Risposta Post-Prandiale

Ricerche emergenti collegano le dinamiche della frequenza cardiaca dopo i pasti con la salute
metabolica, la sensibilità all'insulina e la risposta glicemica.^24 Ingerire un pasto, specialmente
se ricco di carboidrati raffinati o abbondante, provoca un aumento della gittata cardiaca e
della FC per sostenere la digestione e gestire il carico glicemico.
**Metrica: Carico Metabolico Post-Prandiale**
L'app può implementare un algoritmo di rilevamento dei picchi post-prandiali:

1. **Input:** Timestamp dei pasti (inserimento manuale o inferenza da pattern orari).
2. **Finestra di Analisi:** 120 minuti post-evento.
3. **Calcolo:** Monitorare la FC media e i picchi rispetto alla FC a riposo (RHR) pre-pasto.
4. **Indice:** Calcolare l' **Area Sotto la Curva (AUC)** dell'elevazione della FC.
   **Interpretazione:** Un'elevazione prolungata e accentuata (es. >15 bpm sopra la base per >
   min) può indicare un pasto infiammatorio, una scarsa risposta insulinica o un'attivazione
   simpatica eccessiva dovuta a intolleranze.^26 Questo fornisce all'utente un feedback immediato
   su come la dieta impatta il suo "motore" cardiaco, incentivando scelte alimentari che
   mantengono la stabilità autonomica.


### 3.2. Frequenza Respiratoria (RR) da PPG

Una delle opportunità più sofisticate offerte dai dati di un sensore ottico (PPG) è la
derivazione della frequenza respiratoria senza sensori aggiuntivi. Sebbene i dati samplesJson
siano aggregati, se l'app potesse accedere o se il dispositivo fornisse dati con una frequenza
leggermente superiore (es. 1 Hz) o le variabilità inter-battito, si potrebbe applicare l'estrazione
della RR. Tuttavia, anche con dati a 1 minuto, è possibile inferire pattern respiratori basati
sull' **Aritmia Sinusale Respiratoria (RSA)** macroscopica, sebbene con limiti.^28
**Algoritmi di Estrazione (Teoria per Espansione Futura):**
Le tecniche principali si basano sulla modulazione del segnale PPG:
● **Modulazione di Ampiezza (RIAV):** Il volume del polso diminuisce durante l'inspirazione
(vasocostrizione periferica).
● **Modulazione di Frequenza (RIFV):** La durata del ciclo cardiaco si accorcia in
inspirazione (RSA).
● **Modulazione di Intensità (RIIV):** Cambiamenti nella linea di base dovuti alla pressione
intratoracica.^29
Per l'implementazione attuale su Android con dati 1-minuto, si può stimare la **Stabilità
Respiratoria Notturna**. Analizzando la varianza della FC durante le fasi di sonno profondo
(dove il respiro è regolare), deviazioni improvvise possono indicare eventi di apnea o disturbi
respiratori. Un algoritmo di **"Respiratory Instability Index"** basato sulla deviazione standard
della FC in finestre di sonno profondo (fornite dall'analisi del sonno esistente) fornirebbe un
proxy della salute respiratoria.^31

### 3.3. Daily Heart Rate Per Step (DHRPS): Un Nuovo Marker di Rischio

Recenti studi presentati all'American College of Cardiology hanno evidenziato una metrica
semplice ma potente: il rapporto tra frequenza cardiaca giornaliera e passi.^33
**Formula:
Significato Clinico:** Questo rapporto normalizza il lavoro cardiaco rispetto al volume di
attività fisica. Un valore elevato indica che il cuore deve lavorare molto anche per un basso
output di movimento, suggerendo una scarsa fitness cardiovascolare o fattori di rischio
latenti. Al contrario, un valore basso indica un sistema cardiovascolare efficiente. Monitorare il
trend mensile del DHRPS offre all'utente una visione chiara del miglioramento dell'efficienza
cardiaca, spesso più sensibile della sola RHR.^35


### 3.4. Calorie e Dispendio Energetico: Limiti e Correzioni

Sebbene l'app probabilmente calcoli già le calorie, è fondamentale raffinare questi calcoli
utilizzando formule basate sulla frequenza cardiaca che considerino l'intensità metabolica. La
formula di **Keytel (2005)** è spesso citata per la sua accuratezza in assenza di VO2 diretto 37 :
È cruciale applicare questa formula solo quando la FC supera una certa soglia (es. >30%
HRR), poiché a riposo la relazione FC-Calorie non è lineare e il calcolo dovrebbe basarsi sul
Metabolismo Basale (BMR). L'app deve distinguere intelligentemente tra "Calorie Attive"
(formula Keytel) e "Calorie Basali" (Mifflin-St Jeor) per evitare sovrastime grossolane, un
errore comune nei wearable.^39

## 4. Salute Autonomica e Gestione dello Stress

Il sistema nervoso autonomo (SNA) è il regolatore principale della risposta allo stress e al
recupero. La sfida tecnica qui è stimare lo stato del SNA senza l'accesso diretto ai dati
battito-battito (RR intervals) necessari per l'HRV clinico tradizionale. Tuttavia, la letteratura
scientifica offre metodi robusti per approssimare questi stati anche con dati campionati.

### 4.1. Variabilità della Frequenza Cardiaca (HRV) da Serie Temporali

### Sparse

Come notato, metriche come l'RMSSD richiedono precisione millimetrica. Tuttavia, la
**Variabilità Minuto-per-Minuto** è un proxy valido per la regolazione autonomica a lungo
termine.^4

#### 4.1.1. Heart Rate Volatility Score

Durante il sonno, un cuore sano mostra una variabilità naturale legata ai cicli del sonno (FC più
bassa e stabile in NREM, più variabile in REM). Un tracciato piatto ("metronomico") o
eccessivamente erratico può indicare stress.
**Algoritmo Proposto:**

1. Isolare i dati di FC durante la finestra di sonno.
2. Calcolare la deviazione standard ( ) dei campioni di FC minuto per minuto.
3. Normalizzare questo valore rispetto alla media dell'utente.
4. **Interpretazione:** Un calo significativo della rispetto alla baseline personale
   suggerisce un'iperattivazione simpatica notturna (il sistema "combatti o fuggi" non si


```
spegne), che inibisce la naturale modulazione vagale del ritmo cardiaco.^40
```
#### 4.1.2. Approssimazione dell'Indice di Stress di Baevsky

L'Indice di Stress di Baevsky (SI) è una metrica geometrica utilizzata nella medicina spaziale
russa per valutare la rigidità del ritmo cardiaco. Sebbene idealmente calcolato su intervalli RR,
adattamenti statistici permettono di stimarlo dalla distribuzione (istogramma) delle frequenze
cardiache campionate in un periodo di riposo (es. 5-10 minuti di sonno profondo).^42
● **Moda**^ **( ):**^ Il^ valore^ di^ FC^ più^ frequente^ nel^ campione^ (convertito^ in^ secondi^ per^
coerenza con la formula originale: ).
● **Ampiezza**^ **della**^ **Moda**^ **( ):**^ La^ percentuale^ di^ campioni^ che^ cadono^ nel^ bin^ della^
Moda (es. bin di 2-3 bpm).
● **Range**^ **Variazionale**^ **( ):**^ La^ differenza^ tra^ il^ valore^ massimo^ e^ minimo^ di^ FC^
nel campione (in secondi).
Un valore SI elevato indica che il cuore batte a un ritmo molto rigido e costante (alta ,
basso ), segno di un forte controllo simpatico e basso tono vagale. Questa
metrica è eccellente per visualizzare lo "stress fisiologico" accumulato.^44

### 4.2. Resting Heart Rate (RHR) e Rilevamento Malattie

La Frequenza Cardiaca a Riposo (RHR) è forse il biomarcatore più sottovalutato. Non è solo un
numero statico; la sua tendenza è un potente predittore di infiammazione sistemica e infezioni
virali.^46

#### 4.2.1. Z-Score per il Rilevamento Anomalie (Illness Algorithm)

Studi su larga scala (es. dati Fitbit durante COVID-19) hanno dimostrato che l'RHR sale
significativamente giorni prima della comparsa dei sintomi. Per rendere questo dato azionabile
nell'app, si deve implementare un rilevatore di anomalie statistiche basato sul **Z-Score**.^35
**Algoritmo:**

1. Calcolare la media mobile ( ) e la deviazione standard ( ) dell'RHR degli ultimi 30
   giorni (baseline).
2. Ogni mattina, calcolare lo Z-Score dell'RHR odierno ( ):


3. **Logica di Allerta:**
   ○ : Zona Verde (Normale).
   ○ : Zona Gialla (Attenzione: stress, alcol, ciclo mestruale, o inizio
   infezione).
   ○ : Zona Rossa (Allarme: alta probabilità di malattia o overtraining acuto).
   L'app può notificare l'utente: "La tua frequenza a riposo è anormalmente alta oggi (+
   deviazioni standard). Il tuo corpo potrebbe combattere un'infezione o necessitare di recupero
   immediato."

### 4.3. Analisi Morfologica del Sonno: Il "Dipping" Cardiaco

Oltre alla media, la _forma_ della curva di FC notturna è diagnostica. Il fenomeno del "Dipping"
fisiologico prevede che la pressione e la FC scendano del 10-20% durante il sonno. L'assenza
di questo calo ("Non-dipping") è un fattore di rischio cardiovascolare indipendente.^49
**Metrica: Sleep Dipping Ratio**
Inoltre, si può analizzare la tempistica del punto di minimo (Nadir).
● **Profilo "Amaca" (Ottimale):** Il minimo si raggiunge a metà del sonno, con una risalita
graduale verso il risveglio.
● **Profilo "Pendio Discendente" (Metabolicamente Stressato):** La FC scende lentamente
tutta la notte e raggiunge il minimo solo al risveglio. Questo pattern è classico dopo
l'assunzione di alcol o pasti pesanti tardivi, indicando che il corpo ha passato gran parte
della notte a "lavorare" metabolicamente invece di riposare.^51

## 5. Età Fitness e VO2 Max Stimato

La stima della capacità aerobica massima (VO2 Max) è il gold standard per valutare la
longevità cardiovascolare. Sebbene i test di laboratorio siano i più precisi, l'equazione di
**Uth-Sørensen-Overgaard-Pedersen** fornisce una stima sorprendentemente accurata per la
popolazione generale utilizzando solo dati di frequenza cardiaca.^53


Per rendere questo dato comprensibile all'utente, l'applicazione dovrebbe convertirlo in **"Età
Fitness"** (o Età Biologica). Questo si ottiene confrontando il VO2 Max stimato dell'utente con
le tabelle normative dell'ACSM (American College of Sports Medicine). Se un utente di 50 anni
ha un VO2 Max di 45 ml/kg/min (valore medio per un 30enne), la sua "Età Fitness" è 30 anni.
Questo è un potente strumento di gamification e motivazione.^56

## 6. Sintesi: Algoritmo di "Readiness" Giornaliero

Per evitare la paralisi da analisi, l'applicazione deve sintetizzare queste metriche disparate in
un unico punteggio di "Prontezza" (Readiness) o "Batteria Corporea", che risponda alla
domanda: _"Quanto duramente posso spingere oggi?"_.^58
**Proposta di Algoritmo di Sintesi (Punteggio 0-100):**
Il punteggio deve essere una somma ponderata dei fattori di Recupero e Fatica:
Dove:

1. **(40%):** Punteggio derivato dall'analisi del sonno esistente (durata +
   efficienza).
2. **(30%):** Derivato dallo Z-Score inverso. Se RHR è stabile o bassa, il
   punteggio è alto. Se RHR > baseline, il punteggio crolla rapidamente.
3. **(20%):** Basato sul TSB (Forma). Se l'utente è molto affaticato (TSB molto
   negativo), questo componente riduce la Readiness.
4. **(10%):** Basato sul punteggio di volatilità o stabilità notturna descritto sopra.
   **Logica Ponderale:**
   ● Se lo Z-Score dell'RHR indica malattia (es. > 2.0), la Readiness dovrebbe essere forzata a
   un valore basso ("Rest Day"), indipendentemente dal sonno, per proteggere l'utente.^60
   ● Il punteggio finale viene mappato su una scala semaforica:
   ○ **85-100 (Verde):** Alta intensità consentita.
   ○ **60-84 (Giallo):** Mantenimento / Recupero attivo.
   ○ **0-59 (Rosso):** Riposo necessario.


## 7. Conclusioni e Raccomandazioni Tecniche

L'analisi dei dati disponibili conferma che, nonostante l'assenza di dati battito-battito ad alta
frequenza, è possibile costruire un ecosistema analitico di livello professionale. L'applicazione
non deve limitarsi a visualizzare i grafici della FC, ma deve agire come un interprete fisiologico.
**Raccomandazioni Prioritarie per l'Implementazione:**

1. **Preprocessing:** Implementare un'interpolazione intelligente per colmare i gap di
   campionamento e rendere solidi i calcoli integrali (TRIMP).
2. **Focalizzazione sul Carico:** Il TRIMP di Banister e il modello Fitness-Fatigue (PMC) sono
   gli strumenti più impattanti per l'utente sportivo e dovrebbero costituire il cuore della
   sezione "Performance".
3. **Prevenzione Salute:** L'algoritmo Z-Score sulla RHR è una funzionalità "salvavita" a basso
   costo computazionale che offre un valore immenso per il rilevamento precoce di malattie.
4. **Trasparenza:** Etichettare le metriche di variabilità come "Stabilità Cardiaca" o "Volatilità"
   anziché "HRV Clinico" per mantenere rigore scientifico, pur fornendo insight validi sullo
   stress.
   Implementando questi algoritmi, la vostra applicazione Android evolverà da semplice tracker
   passivo a coach proattivo, capace di guidare l'utente verso l'ottimizzazione della performance
   e la salvaguardia della salute a lungo termine.

### Appendice: Tabella di Riferimento delle Formule Chiave

```
Metrica Formula / Logica Dati Richiesti Scopo
Banister TRIMP Tempo, FC Media,
FC Max, FC Riposo
Quantificazione
Carico Allenamento
Heart Rate
Reserve
FC Max, FC Riposo Baseline Intensità
VO2 Max Stimato FC Max, FC Riposo Fitness
Cardiorespiratoria
Sleep Dip % FC Riposo Giorno,
FC Media Sonno
Qualità Recupero
Cardiovascolare
```

```
Decoppiamento
Aerobico
FC, Passo/Potenza
(o tempo)
Resistenza
Aerobica / Drift
Z-Score (Malattia) Storico RHR
Giornaliero
Rilevamento
Anomalie/Infezioni
Baevsky SI
(Approx)
Istogramma FC
(Analisi
Distribuzione)
Stress / Tono
Simpatico
DHRPS FC 24h, Pedometro Efficienza Cardiaca
Globale
```
#### Bibliografia

#### 1. health_database-heart_rate_sessions.csv

#### 2. Calculating Training Impulse, accesso eseguito il giorno febbraio 9, 2026,

#### https://static.csbsju.edu/documents/Sports%20Medicine/Final%20Schlagen%20S

#### choenecker%20Poster.pdf

#### 3. Effects of Missing Data on Heart Rate Variability Metrics - MDPI, accesso eseguito

#### il giorno febbraio 9, 2026, https://www.mdpi.com/1424-8220/22/15/

#### 4. On the estimation of beat-to-beat time domain heart rate variability indices from

#### smoothed heart rate time series | medRxiv, accesso eseguito il giorno febbraio 9,

#### 2026, https://www.medrxiv.org/content/10.1101/2023.10.27.23297692v1.full-text

#### 5. The eTRIMP method for bodybuilding training load assessment: A review with a

#### case study, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.organscigroup.us/articles/AMM-7-133.php

#### 6. What is TRIMP? How Sports Teams Use It to Optimize Training - Firstbeat, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://www.firstbeat.com/en/blog/what-is-trimp/

#### 7. The Validity of the Session Rating of Perceived Exertion Method for Measuring

#### Internal Training Load in Professional Classical Ballet Dancers - Frontiers, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2020.

#### 0/full

#### 8. TRIMP: A Science-Backed Way to Measure Training Load in Endurance Sports -

#### Ludum, accesso eseguito il giorno febbraio 9, 2026,

#### https://ludum.com/blog/data-performance-analytics/trimp-as-a-training-load-sc

#### ore/

#### 9. Real-time TRIMP/min: How to Use the Firstbeat Sports App Feature in Training,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.firstbeat.com/en/blog/real-time-trimp-min/

#### 10. Interpreting Training Data - Firstbeat, accesso eseguito il giorno febbraio 9, 2026,


#### https://www.firstbeat.com/en/professional-sports/learning-center/interpreting-tra

#### ining-data/

#### 11. Session Rating of Perceived Exertion Is a Superior Method to Monitor Internal

#### Training Loads of Functional Fitness Training Sessions Performed at Different

#### Intensities When Compared to Training Impulse - PMC, accesso eseguito il giorno

#### febbraio 9, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC7435063/

#### 12. Training with TSS vs. hrTSS: What's the difference? | TrainingPeaks, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://www.trainingpeaks.com/learn/articles/training-with-tss-vs-hrtss-whats-th

#### e-difference/

#### 13. Formula to Calculate HrTSS - Training - TrainerRoad, accesso eseguito il giorno

#### febbraio 9, 2026,

#### https://www.trainerroad.com/forum/t/formula-to-calculate-hrtss/

#### 14. Training with TSS and hrTSS - Suunto, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.suunto.com/sports/News-Articles-container-page/training-with-tss-

#### and-hrtss/

#### 15. Load Balance: How ATL, CTL, and TSB Shape Your Performance - Sonar Health,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.sonarhealth.co/blog/load-balance/

#### 16. A Coach's Guide to ATL, CTL & TSB - TrainingPeaks, accesso eseguito il giorno

#### febbraio 9, 2026,

#### https://www.trainingpeaks.com/coach-blog/a-coachs-guide-to-atl-ctl-tsb/

#### 17. What are CTL, ATL, TSB & TSS? Why Do They Matter? - TrainerRoad, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://www.trainerroad.com/blog/why-tss-atl-ctl-and-tsb-matter/

#### 18. Which TrainingPeaks Metrics Should You Actually Care About? - Triathlete,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.triathlete.com/gear/tech-wearables/which-trainingpeaks-metrics-sh

#### ould-you-actually-care-about/

#### 19. The Performance Manager Chart in WKO and TrainingPeaks - FasCat Coaching,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://fascatcoaching.com/blogs/training-tips/performance-manager-chart

#### 20. Understanding the Heart Rate Drift Test: A Practical Guide for Endurance

#### Athletes, accesso eseguito il giorno febbraio 9, 2026,

#### https://uphillathlete.com/aerobic-training/heart-rate-drift/

#### 21. Are You Fit? All About Aerobic Endurance and Decoupling - TrainingPeaks,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.trainingpeaks.com/blog/aerobic-endurance-and-decoupling/

#### 22. Heart rate decoupling. - JOIN Cycling, accesso eseguito il giorno febbraio 9,

#### 2026, https://join.cc/cycling-tips/heart-rate-decoupling

#### 23. Aerobic Decoupling, Explained: What It Is And How To Use It In Training, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://marathonhandbook.com/aerobic-decoupling/

#### 24. The Cardiovascular Effects of a Meal: J‐Tpeak and Tpeak‐Tend Assessment and

#### Further Insights Into the Physiological Effects - PMC, accesso eseguito il giorno


#### febbraio 9, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC6590239/

#### 25. Central Hemodynamic and Thermoregulatory Responses to Food Intake as

#### Potential Biomarkers for Eating Detection - Interactive Journal of Medical

#### Research, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.i-jmr.org/2024/1/e52167/PDF

#### 26. Carbohydrate Content Classification Using Postprandial Heart Rate Responses

#### from Non-Invasive Wearables - PubMed, accesso eseguito il giorno febbraio 9,

#### 2026, https://pubmed.ncbi.nlm.nih.gov/39205025/

#### 27. Carbohydrate Content Classification Using Postprandial Heart Rate Responses

#### from Non-Invasive Wearables - MDPI, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.mdpi.com/1424-8220/24/16/

#### 28. Photoplethysmography-Based Respiratory Rate Estimation Algorithm for Health

#### Monitoring Applications - PMC, accesso eseguito il giorno febbraio 9, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC9056464/

#### 29. Determining respiratory rate from photoplethysmogram and electrocardiogram

#### signals using respiratory quality indices and neural networks | PLOS One -

#### Research journals, accesso eseguito il giorno febbraio 9, 2026,

#### https://journals.plos.org/plosone/article?id=10.1371/journal.pone.

#### 30. Extracting Instantaneous Respiratory Rate From Multiple Photoplethysmogram

#### Respiratory-Induced Variations - Frontiers, accesso eseguito il giorno febbraio 9,

#### 2026,

#### https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2018.

#### /full

#### 31. Measuring resting heart rate during daily life using wearable technology:

#### Examining the impact of behavioral context and methodological criteria - PMC,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC12357026/

#### 32. Breathing Rate Estimation from Head-Worn Photoplethysmography Sensor Data

#### Using Machine Learning - PubMed Central, accesso eseguito il giorno febbraio 9,

#### 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC8951087/

#### 33. Scientists Just Discovered a Smartwatch Formula That Could Change How We

#### Detect Heart Disease - SciTechDaily, accesso eseguito il giorno febbraio 9, 2026,

#### https://scitechdaily.com/scientists-just-discovered-a-smartwatch-formula-that-c

#### ould-change-how-we-detect-heart-disease/

#### 34. Counting steps is good — is combining steps and heart rate better? - Harvard

#### Health, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.health.harvard.edu/blog/counting-steps-is-good-is-combining-step

#### s-and-heart-rate-better-

#### 35. Daily Heart Rate per Step: A Wearables Metric Associated With Cardiovascular

#### Disease in a Cross-Sectional Study of the All of Us Research Program - PubMed,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://pubmed.ncbi.nlm.nih.gov/40156587/

#### 36. These two simple numbers can predict your heart disease risk, accesso eseguito

#### il giorno febbraio 9, 2026,

#### https://www.sciencefocus.com/the-human-body/two-numbers-predict-heart-dis


#### ease

#### 37. How does Myzone calculate calorie burn? - MZ-Switch Heart Rate Monitor,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://l.myzone.org/b2b/how-does-myzone-calculate-calorie-burn

#### 38. Calories Burned by Heart Rate: Understanding the Connection - COOSPO,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.coospo.com/blogs/knowledge/calories-burned-by-heart-rate-under

#### standing-the-connection

#### 39. Fitness trackers accurately measure heart rate but not calories burned - Stanford

#### Medicine, accesso eseguito il giorno febbraio 9, 2026,

#### https://med.stanford.edu/news/all-news/2017/05/fitness-trackers-accurately-mea

#### sure-heart-rate-but-not-calories-burned.html

#### 40. Heart Rate Variability Measurement through a Smart Wearable Device: Another

#### Breakthrough for Personal Health Monitoring? - PMC, accesso eseguito il giorno

#### febbraio 9, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC10742885/

#### 41. Robust Interbeat Interval and Heart Rate Variability Estimation Method from

#### Various Morphological Features using Wearable Sensors - PMC, accesso eseguito

#### il giorno febbraio 9, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC11036325/

#### 42. The Role of Heart Rate Variability (HRV) in Different Hypertensive Syndromes -

#### MDPI, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.mdpi.com/2075-4418/13/4/

#### 43. Optimizing Autonomic Function Analysis via Heart Rate Variability Associated

#### With Motor Activity of the Human Colon - Frontiers, accesso eseguito il giorno

#### febbraio 9, 2026,

#### https://www.frontiersin.org/journals/physiology/articles/10.3389/fphys.2021.

#### /full

#### 44. Synthetic assessment of cardiac autonomic modulation and Baevsky stress index

#### in patients with synucleinopathies | European Heart Journal | Oxford Academic,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://academic.oup.com/eurheartj/article/45/Supplement_1/ehae666.3017/

#### 98

#### 45. Determining autonomic sympathetic tone and reactivity using Baevsky's stress

#### index, accesso eseguito il giorno febbraio 9, 2026,

#### https://journals.physiology.org/doi/10.1152/ajpregu.00243.

#### 46. Resting Heart Rate Variability Measured by Consumer Wearables and Its

#### Associations with Diverse Health Domains in Five Longitudinal Studies - MDPI,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.mdpi.com/1424-8220/25/23/

#### 47. Harnessing wearable device data to improve state-level real-time surveillance of

#### influenza-like illness in the USA: a population-based study - PMC, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC8048388/

#### 48. Continuous Monitoring of Heart Rate Variability in Free-Living Conditions Using

#### Wearable Sensors: Exploratory Observational Study - PMC, accesso eseguito il

#### giorno febbraio 9, 2026, https://pmc.ncbi.nlm.nih.gov/articles/PMC11339560/


#### 49. Assessment of heart rate measurements by commercial wearable fitness

#### trackers for early identification of metabolic syndrome risk - PubMed Central,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC11470009/

#### 50. Wearable-Measured Sleep and Resting Heart Rate Variability as an Outcome of

#### and Predictor for Subjective Stress Measures: A Multiple N-of-1 Observational

#### Study - MDPI, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.mdpi.com/1424-8220/23/1/

#### 51. How does Garmin work out body battery - FitStrapsUK, accesso eseguito il

#### giorno febbraio 9, 2026,

#### https://fitstraps.co.uk/blogs/news/body-battery-explained-how-does-garmin-wo

#### rk-out-body-battery

#### 52. Body Battery Frequently Asked Questions | Garmin Customer Support, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://support.garmin.com/en-US/?faq=VOFJAsiXut9K19k1qEn5W

#### 53. Heart Rate to VO 2 Max Calculator - runbundle, accesso eseguito il giorno

#### febbraio 9, 2026,

#### https://runbundle.com/tools/vo2-max-calculators/heart-rate-vo2-max-calculator

#### 54. Estimation of VO2max from the ratio between HRmax and HRrest--the Heart

#### Rate Ratio Method - PubMed, accesso eseguito il giorno febbraio 9, 2026,

#### https://pubmed.ncbi.nlm.nih.gov/14624296/

#### 55. Developing First Native Regression Equations to Predict of Cardiorespiratory

#### Fitness in Healthy Boys - NIH, accesso eseguito il giorno febbraio 9, 2026,

#### https://pmc.ncbi.nlm.nih.gov/articles/PMC10903308/

#### 56. Fitness Age | Garmin Technology, accesso eseguito il giorno febbraio 9, 2026,

#### https://www.garmin.com/en-US/garmin-technology/health-science/fitness-age/

#### 57. Calculators › Estimate Fitness, VO2 Max, HR, Body Fat Values. - Personal Trainers,

#### accesso eseguito il giorno febbraio 9, 2026,

#### https://www.tribelocus.com/calculators/

#### 58. What's my readiness score in the Fitbit app - Google Help, accesso eseguito il

#### giorno febbraio 9, 2026, https://support.google.com/fitbit/answer/14236710?hl=en

#### 59. WHOOP Recovery: How It Works, Key Metrics, and Tips, accesso eseguito il

#### giorno febbraio 9, 2026,

#### https://www.whoop.com/us/en/thelocker/how-does-whoop-recovery-work-101/

#### 60. Sleep and Readiness Scores : r/ouraring - Reddit, accesso eseguito il giorno

#### febbraio 9, 2026,

#### https://www.reddit.com/r/ouraring/comments/1ozgsap/sleep_and_readiness_scor

#### es/

#### 61. On Heart Rate Variability (HRV) and readiness | by Marco Altini - Medium, accesso

#### eseguito il giorno febbraio 9, 2026,

#### https://medium.com/@altini_marco/on-heart-rate-variability-hrv-and-readiness-

#### 94a499ed05b


