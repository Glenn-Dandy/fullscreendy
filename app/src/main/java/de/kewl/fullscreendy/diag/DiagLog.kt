package de.kewl.fullscreendy.diag

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Leichtgewichtiges Diagnose-Log für den Dauerbetrieb: schreibt Zeitstempel-Zeilen
 * nach .../Android/data/<pkg>/files/logs/app.log (bei Überlauf wird gekürzt).
 * Zusätzlich wird beim Start der Grund des LETZTEN Prozess-Endes ermittelt
 * (ApplicationExitInfo: CRASH/ANR/OOM/System) – abrufbar via [lastExit] und als
 * MQTT-Reading "lastExit".
 */
object DiagLog {
    private const val MAX_BYTES = 512 * 1024L
    private const val KEEP_BYTES = 200 * 1024
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Volatile private var file: File? = null

    /** Grund des letzten Prozess-Endes, z. B. "CRASH (2026-07-10 17:03:12)". */
    @Volatile var lastExit: String = "n/a"
        private set

    fun init(ctx: Context) {
        if (file != null) return
        val dir = ctx.getExternalFilesDir("logs") ?: File(ctx.filesDir, "logs").apply { mkdirs() }
        file = File(dir, "app.log")
        readLastExit(ctx)
    }

    @Synchronized
    fun log(tag: String, msg: String) {
        val f = file ?: return
        runCatching {
            if (f.length() > MAX_BYTES) {
                val tail = f.readText().takeLast(KEEP_BYTES)
                f.writeText("…(gekürzt)…\n$tail")
            }
            f.appendText("${fmt.format(Date())} [$tag] $msg\n")
        }
    }

    private fun readLastExit(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            val am = ctx.getSystemService(ActivityManager::class.java) ?: return
            // Nur der HAUPTprozess zählt (nicht die WebView-Sandbox-Prozesse).
            val info = am.getHistoricalProcessExitReasons(ctx.packageName, 0, 16)
                .firstOrNull { it.processName == ctx.packageName }
                ?: return
            val reason = reasonName(info.reason)
            val at = fmt.format(Date(info.timestamp))
            lastExit = "$reason ($at)"
            log("Exit", "Letztes Prozess-Ende: $reason um $at – ${info.description ?: "-"}")
        }
    }

    private fun reasonName(r: Int): String = when (r) {
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_CRASH -> "CRASH"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCES"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_FREEZER -> "FREEZER"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INIT_FAILURE"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
        ApplicationExitInfo.REASON_OTHER -> "SYSTEM_OTHER"
        else -> "UNKNOWN($r)"
    }
}
