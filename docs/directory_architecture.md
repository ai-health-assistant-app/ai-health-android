# 🏗️ Architettura del Progetto Android

Il progetto segue i principi della **Clean Architecture**, dividendo le responsabilità in tre layer principali (Data, Domain, Presentation) per garantire scalabilità e testabilità.

## 📂 Struttura delle Directory

```text
com.ai_health.assistant
 data                # <--- (Il "Data Handling", "Database", "Data Collector")
    database        # (Il blocco "Database") Entity e DAO di Room
    healthconnect   # (Il blocco "Data Collector") Manager per leggere da Health Connect
    api             # (Il blocco "Backend LLM") Client Retrofit per il server Python
    repository      # (Il blocco "Data Handling") Coordina le fonti dati

 domain              # <--- (Il blocco "Algo")
    model           # Modelli di dati "puri" (senza librerie Android)
    usecase         # La logica di business (es. "CalculateRecoveryScore")

 presentation        # <--- (Il blocco "UX/UI")
    MainAcivity.kt  # Entry point: Il "regista" della Single Activity Architecture
    dashboard       # Schermate e logica della dashboard
    onboarding      # Schermate di benvenuto
    ui.theme           # Colori, Font, Stili

 workers             # <--- (Il blocco "Notifications")
    DataSyncWorker.kt # Task in background che si sveglia e scarica i dati

 di                  # (Dependency Injection) Moduli Hilt per collegare tutto