package com.ai_health.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Questa annotazione dice a Hilt: "Inizia a generare il codice qui"
@HiltAndroidApp
class SaluteTwinApp : Application()