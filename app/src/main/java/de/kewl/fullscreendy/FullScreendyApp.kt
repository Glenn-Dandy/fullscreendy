package de.kewl.fullscreendy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import de.kewl.fullscreendy.diag.DiagLog

class FullScreendyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Diagnose: Log-Datei initialisieren, letzten Exit-Grund lesen und
        // ungefangene Abstürze mit Stacktrace protokollieren.
        DiagLog.init(this)
        DiagLog.log("App", "Prozess gestartet (v${BuildConfig.VERSION_NAME})")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { DiagLog.log("CRASH", Log.getStackTraceString(e)) }
            previous?.uncaughtException(thread, e)
        }

        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIF_CHANNEL_ID = "kiosk_service"
        const val NOTIF_ID = 42
    }
}
