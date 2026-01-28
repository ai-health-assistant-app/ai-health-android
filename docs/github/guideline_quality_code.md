# Guida alla Qualità del Codice (CI/CD)

Questa guida definisce il flusso di lavoro obbligatorio per garantire che il branch `main` rimanga sempre stabile e compilabile, supportando la strategia *Trunk-Based Development* e il modello operativo "1 Lead + 2 Contributors" .

## 1. Infrastruttura CI (GitHub Actions)

Il progetto utilizza GitHub Actions per automatizzare la verifica del codice (Build & Lint) come previsto dalla Roadmap .

### Configurazione
Il file di configurazione deve essere creato in `.github/workflows/android_ci.yml`.

```yaml
name: Android CI (Build & Lint)

# Trigger: Esegue il controllo su:
# 1. OGNI push su qualsiasi branch (feedback immediato per lo sviluppatore)
# 2. Ogni Pull Request verso il main (sicurezza prima del merge)
on:
  push:
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    name: Build, Lint & Test
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      # Setup JDK 17 (Richiesto per Kotlin 2.1+ e AGP recenti )
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      # Setup Gradle con caching automatico per build veloci 
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      # 1. Analisi statica del codice (Quality Gate)
      - name: Run Lint
        run: ./gradlew lintDebug

      # 2. Esecuzione Unit Test (Business Logic per Core Domain )
      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      # 3. Compilazione (Build Check)
      - name: Build Debug APK
        run: ./gradlew assembleDebug
```
## 2. Controlli Locali (Prima del Push)
Per ridurre il carico sulla CI e velocizzare lo sviluppo, è necessario verificare il codice in locale prima di inviarlo.

Comandi Manuali
Eseguire questi comandi nel terminale di Android Studio:

Bash
```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```
Windows:

DOS
```
gradlew.bat lintDebug testDebugUnitTest assembleDebug
```
`lintDebug`: Cerca errori di stile o potenziali bug.

`testDebugUnitTest`: Verifica la logica di business (es. algoritmi `:core:domain`).

`assembleDebug`: Verifica che l'app compili correttamente.

## 3. Automazione Locale (Git Hook Pre-Push)
Per automatizzare il controllo di qualità e impedire l'invio di codice rotto, configuriamo un Git Hook. Questo impedisce il "Merge Hell" citato nella strategia .

### Installazione dell'Hook
Navigare nella cartella nascosta `.git/hooks/` all'interno del progetto.

Creare un file chiamato esattamente `pre-push` (senza estensione `.sh` o `.txt`).

Incollare il seguente contenuto:

Bash
```bash
#!/bin/sh

echo "🚧 Esecuzione controlli di qualità automatici pre-push..."

# Esegue Lint e Unit Test
# Se falliscono, il push viene abortito
./gradlew lintDebug testDebugUnitTest

# Verifica il codice di uscita del comando precedente
if [ $? -ne 0 ]; then
    echo "❌ BLOCCO PUSH: Ci sono errori nel codice o nei test."
    echo "   Controlla l'output qui sopra e correggi gli errori."
    exit 1
fi

echo "✅ Codice pulito. Procedo con il push..."
exit 0
```
(Solo Mac/Linux) Rendere il file eseguibile da terminale:

Bash
```bash
chmod +x .git/hooks/pre-push
```