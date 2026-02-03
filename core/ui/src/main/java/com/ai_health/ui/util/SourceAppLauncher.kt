package com.ai_health.ui.util

import android.content.Context
import android.content.Intent
import android.widget.Toast


/**
 * Helper class to launch the source application of the health data.
 */
object SourceAppLauncher {

    private const val GOOGLE_FIT_PACKAGE = "com.google.android.apps.fitness"
    private const val GMS_PACKAGE = "com.google.android.gms"

    fun getAppName(context: Context, packageName: String?): String {
        if (packageName.isNullOrBlank()) return "Health Connect"
        if (packageName == GMS_PACKAGE) return "Google Fit" // GMS usually implies Fit in this context

        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
             // Fallback to simple name if not installed or error
             packageName.substringAfterLast('.').capitalize()
        }
    }

    fun launchApp(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank()) {
            Toast.makeText(context, "App sorgente non trovata", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPackage = if (packageName == GMS_PACKAGE) {
            GOOGLE_FIT_PACKAGE
        } else {
            packageName
        }

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "App non installata: $targetPackage", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Impossibile avviare l'app", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
