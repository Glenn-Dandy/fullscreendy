package de.kewl.fullscreendy.service

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import de.kewl.fullscreendy.FullScreendyApp
import de.kewl.fullscreendy.MainActivity
import de.kewl.fullscreendy.R
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.data.SettingsRepository
import de.kewl.fullscreendy.device.BatteryMonitor
import de.kewl.fullscreendy.device.BatteryState
import de.kewl.fullscreendy.device.DeviceInfo
import de.kewl.fullscreendy.device.MediaManager
import de.kewl.fullscreendy.device.MotionDetector
import de.kewl.fullscreendy.device.SoundDetector
import de.kewl.fullscreendy.device.SystemController
import de.kewl.fullscreendy.device.TtsManager
import de.kewl.fullscreendy.diag.DiagLog
import de.kewl.fullscreendy.kiosk.KioskBus
import de.kewl.fullscreendy.kiosk.KioskCommand
import de.kewl.fullscreendy.kiosk.KioskStatus
import de.kewl.fullscreendy.mqtt.MqttConfig
import de.kewl.fullscreendy.mqtt.MqttManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Dauerhaft laufender Dienst: hält die MQTT-Verbindung zu FHEM, meldet Akku- und
 * Bewegungsstatus und führt eingehende Befehle (TTS, Bildschirm, URL) aus.
 */
class KioskService : LifecycleService() {

    private lateinit var repo: SettingsRepository
    private lateinit var tts: TtsManager
    private lateinit var media: MediaManager

    private var mqtt: MqttManager? = null
    private var battery: BatteryMonitor? = null
    private var motion: MotionDetector? = null
    private var sound: SoundDetector? = null

    private var settings: Settings = Settings()
    private var lastBattery: BatteryState? = null
    private var screenOn: Boolean = true
    private var currentUrl: String = ""
    private var activeDashboard: Int = 0
    private var brightnessPercent: Int = -1 // -1 = auto

    // Verhindert unnötige Neustarts von MQTT/Detektoren bei irrelevanten Änderungen.
    private var mqttKey: String? = null
    private var detectorKey: String? = null

    override fun onCreate() {
        super.onCreate()
        DiagLog.init(applicationContext)
        DiagLog.log(TAG, "Service onCreate (batteryOptIgnoriert=" +
            "${SystemController.isIgnoringBatteryOptimizations(this)})")
        repo = SettingsRepository(applicationContext)
        tts = TtsManager(applicationContext)
        media = MediaManager(applicationContext)
        updateForeground(camera = false, microphone = false)

        lifecycleScope.launch {
            repo.settings.distinctUntilChanged().collect { s ->
                settings = s
                applySettings(s)
            }
        }
        // Die UI meldet, welches Dashboard gerade sichtbar ist (Menü, MQTT, Aufwecken).
        lifecycleScope.launch {
            KioskStatus.activeDashboard.collect { index ->
                activeDashboard = index
                currentUrl = settings.dashboardAt(index).url
                publishDashboard()
                publishUrl()
            }
        }
        // Heartbeat: alle 30 min eine Log-Zeile inkl. Speicherlage → Todeszeitpunkt UND
        // Speicher-Trend (Richtung LOW_MEMORY) lassen sich später ablesen.
        lifecycleScope.launch {
            var mins = 0
            while (true) {
                kotlinx.coroutines.delay(30 * 60_000L)
                mins += 30
                DiagLog.log(TAG, "Heartbeat – Dienst läuft seit ${mins} min – " +
                    DiagLog.memorySnapshot(applicationContext))
            }
        }
    }

    /** Android 15: Timeout für zeitbegrenzte FGS-Typen (z. B. dataSync) – loggen! */
    override fun onTimeout(startId: Int) {
        DiagLog.log(TAG, "FGS onTimeout(startId=$startId) – System beendet den Diensttyp!")
        super.onTimeout(startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Ein NULL-Intent bedeutet: das System hat den Dienst nach einem Kill über
        // START_STICKY selbst neu gestartet. Dabei kommt aber NUR der Dienst zurück –
        // die Kiosk-Activity (WebView) nicht. Deshalb holen wir die Anzeige aktiv zurück,
        // sonst bleibt der Bildschirm nach LOW_MEMORY schwarz/„nicht aufweckbar".
        if (intent == null) relaunchUi()
        // Von der Activity beim Fokus ausgelöst: Kamera/Mikrofon-Detektoren (neu)
        // starten, wenn die App im Vordergrund ist (im Hintergrund oft blockiert).
        if (intent?.action == ACTION_REFRESH) startDetectors(settings)
        return START_STICKY
    }

    /** Startet die Kiosk-Activity aus dem Hintergrund neu (braucht Overlay-Berechtigung). */
    private fun relaunchUi() {
        if (!SystemController.canDrawOverlays(this)) {
            DiagLog.log(TAG, "Cold-Restart: UI-Neustart übersprungen (keine Overlay-Berechtigung)")
            return
        }
        DiagLog.log(TAG, "Cold-Restart nach Kill – hole Kiosk-UI zurück (${DiagLog.memorySnapshot(applicationContext)})")
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }.onFailure { DiagLog.log(TAG, "UI-Neustart fehlgeschlagen: ${it.message}") }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    // ---- Konfiguration anwenden -------------------------------------------------

    private fun applySettings(s: Settings) {
        val url = s.dashboardAt(activeDashboard).url
        if (currentUrl != url) {
            currentUrl = url
            publishUrl()
        }

        // Batteriemonitor (immer aktiv)
        if (battery == null) {
            battery = BatteryMonitor(applicationContext) { state -> onBattery(state) }.also { it.start() }
        }

        // Detektoren nur neu aufsetzen, wenn sich ihre Konfiguration geändert hat.
        val dk = "${s.motionEnabled}|${s.motionSensitivity}|${s.soundWakeEnabled}|${s.soundSensitivity}"
        if (dk != detectorKey) {
            detectorKey = dk
            motion?.stop(); motion = null
            sound?.stop(); sound = null
            KioskStatus.setMotionActive(false)
        }
        startDetectors(s)

        // MQTT nur neu verbinden, wenn sich die Verbindungsdaten geändert haben.
        val mk = "${s.mqttHost}|${s.mqttPort}|${s.mqttTls}|${s.mqttUser}|${s.mqttPass}|${s.deviceTopic}"
        if (mk != mqttKey) {
            mqttKey = mk
            connectMqtt(s)
        }
    }

    private fun granted(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    /**
     * Startet Bewegungs-/Ton-Erkennung passend zu den Einstellungen. Wird sowohl
     * bei Einstellungsänderungen als auch beim App-Fokus (ACTION_REFRESH) aufgerufen;
     * bereits laufende Detektoren bleiben unangetastet. Kamera/Mikrofon im Hintergrund
     * zu starten kann fehlschlagen – daher runCatching (kein Absturz).
     */
    private fun startDetectors(s: Settings) {
        val wantMotion = s.motionEnabled && granted(Manifest.permission.CAMERA)
        val wantSound = s.soundWakeEnabled && granted(Manifest.permission.RECORD_AUDIO)

        updateForeground(camera = wantMotion, microphone = wantSound)

        if (wantMotion && motion == null) {
            runCatching {
                motion = MotionDetector(
                    applicationContext,
                    s.motionSensitivity,
                    onMotionChanged = { active -> onMotion(active) },
                    onMotionPulse = { onMotionPulse() },
                ).also { it.start(this) }
            }.onFailure { Log.w(TAG, "Bewegungserkennung nicht gestartet", it) }
        } else if (!wantMotion && motion != null) {
            motion?.stop(); motion = null
        }

        if (wantSound && sound == null) {
            runCatching {
                sound = SoundDetector(s.soundSensitivity) { onSoundWake() }.also { it.start() }
            }.onFailure { Log.w(TAG, "Tonerkennung nicht gestartet", it) }
        } else if (!wantSound && sound != null) {
            sound?.stop(); sound = null
        }
    }

    private fun connectMqtt(s: Settings) {
        mqtt?.disconnect()
        if (s.mqttHost.isBlank()) {
            mqtt = null
            return
        }
        val dt = s.deviceTopic
        val cfg = MqttConfig(
            host = s.mqttHost,
            port = s.mqttPort,
            tls = s.mqttTls,
            username = s.mqttUser,
            password = s.mqttPass,
            clientId = "fullscreendy-${s.deviceId}",
            commandTopic = "$dt/cmd/#",
            statusTopic = "$dt/status",
        )
        mqtt = MqttManager(
            onCommand = { topic, payload -> handleCommand(topic, payload) },
            onConnected = { onMqttConnected() },
            onConnectionChanged = { connected -> KioskStatus.setMqttConnected(connected) },
        ).also { it.connect(cfg) }
    }

    // ---- Telemetrie -------------------------------------------------------------

    private fun onMqttConnected() {
        val dt = settings.deviceTopic
        mqtt?.apply {
            publish("$dt/status", "online", retained = true, qos = 1)
            publish("$dt/appVersion", DeviceInfo.appVersion, retained = true)
            publish("$dt/androidVersion", DeviceInfo.androidVersion, retained = true)
            publish("$dt/ip", DeviceInfo.ipv4(), retained = true)
            // Grund des letzten Prozess-Endes (CRASH/ANR/LOW_MEMORY/…) – für Ferndiagnose.
            publish("$dt/lastExit", DiagLog.lastExit, retained = true)
        }
        lastBattery?.let { publishBattery(it) }
        publishScreen(screenOn)
        publishUrl()
        publishDashboard()
        publishBrightness()
        publishVolume()
    }

    private fun publishVolume() {
        mqtt?.publish("${settings.deviceTopic}/volume", SystemController.getVolumePercent(this).toString(), retained = true)
    }

    private fun publishUrl() {
        mqtt?.publish("${settings.deviceTopic}/url", currentUrl, retained = true)
    }

    /** Nummer (1-basiert) und Name des gerade angezeigten Dashboards. */
    private fun publishDashboard() {
        val dt = settings.deviceTopic
        val index = activeDashboard
        mqtt?.publish("$dt/dashboard", (index + 1).toString(), retained = true)
        mqtt?.publish("$dt/dashboardName", settings.dashboardAt(index).displayName(index), retained = true)
    }

    private fun publishBrightness() {
        val value = if (brightnessPercent < 0) "auto" else brightnessPercent.toString()
        mqtt?.publish("${settings.deviceTopic}/brightness", value, retained = true)
    }

    private fun onBattery(state: BatteryState) {
        lastBattery = state
        publishBattery(state)
    }

    private fun publishBattery(state: BatteryState) {
        val dt = settings.deviceTopic
        mqtt?.apply {
            publish("$dt/battery", state.level.toString(), retained = true)
            publish("$dt/charging", if (state.charging) "on" else "off", retained = true)
            publish("$dt/plug", state.plugged, retained = true)
            // Locale.US erzwingt Punkt als Dezimaltrenner (sonst "24,5" bei deutschem Gerät).
            publish("$dt/batteryTemp", String.format(Locale.US, "%.1f", state.temperatureC), retained = true)
        }
    }

    /** Flanke aktiv/inaktiv – nur für das MQTT-Reading und den Test-Indikator. */
    private fun onMotion(active: Boolean) {
        KioskStatus.setMotionActive(active)
        val dt = settings.deviceTopic
        mqtt?.publish("$dt/motion", if (active) "on" else "off", retained = true)
        mqtt?.publish("$dt/presence", if (active) "present" else "absent", retained = true)
    }

    /** Feuert bei JEDER Bewegung (gedrosselt): weckt & setzt den Abdunkel-Timer zurück. */
    private fun onMotionPulse() {
        if (!settings.motionWakesScreen) return
        wakeScreen()
        if (!screenOn) {
            screenOn = true
            publishScreen(true)
        }
        KioskBus.send(KioskCommand.Screen(on = true)) // hebt Overlay auf + Timer-Reset
    }

    private fun onSoundWake() {
        KioskStatus.pulseSound()
        wakeScreen()
        screenOn = true
        KioskBus.send(KioskCommand.Screen(on = true))
        publishScreen(true)
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "fullscreendy:wake"
        )
        runCatching { wl.acquire(3_000L) }
    }

    /** Dashboard per Nummer (1..3) oder Name umschalten. */
    private fun selectDashboard(payload: String) {
        val wanted = payload.trim()
        val index = wanted.toIntOrNull()?.minus(1)
            ?: settings.dashboards.withIndex().firstOrNull { (i, d) ->
                d.name.equals(wanted, ignoreCase = true) || d.displayName(i).equals(wanted, ignoreCase = true)
            }?.index
        if (index == null || index !in settings.dashboards.indices) {
            Log.w(TAG, "Unbekanntes Dashboard: $payload")
            return
        }
        // Die UI schaltet um und meldet es über KioskStatus zurück (→ Readings).
        KioskBus.send(KioskCommand.SelectDashboard(index))
    }

    private fun publishScreen(on: Boolean) {
        val dt = settings.deviceTopic
        mqtt?.publish("$dt/screen", if (on) "on" else "off", retained = true)
    }

    // ---- Eingehende Befehle -----------------------------------------------------

    private fun handleCommand(topic: String, payload: String) {
        val sub = topic.substringAfterLast("/cmd/", "").ifEmpty {
            topic.substringAfterLast('/')
        }
        Log.i(TAG, "Befehl: $sub = $payload")
        when (sub.lowercase()) {
            "tts", "say", "speak" -> if (settings.ttsEnabled) tts.speak(payload)
            "mediaplay", "media", "play" -> if (settings.mediaEnabled) media.play(payload)
            "mediastop" -> media.stop()
            "clearcache", "cache" -> KioskBus.send(KioskCommand.ClearCache)
            "url", "load" -> {
                currentUrl = payload.trim()
                KioskBus.send(KioskCommand.LoadUrl(currentUrl))
                publishUrl()
            }
            "dashboard", "db" -> selectDashboard(payload)
            "reload", "refresh" -> KioskBus.send(KioskCommand.Reload)
            "screen" -> setScreen(isOn(payload))
            "screensaver" -> setScreen(!isOn(payload)) // screensaver an == Bildschirm aus
            "lock" -> if (!SystemController.lock(this)) Log.w(TAG, "Sperren fehlgeschlagen (Geräteadmin aktiv?)")
            "unlock" -> KioskBus.send(KioskCommand.Unlock)
            "vibrate" -> SystemController.vibrate(this, payload.trim().toLongOrNull() ?: 200L)
            "volume", "vol" -> {
                payload.trim().toIntOrNull()?.let { SystemController.setVolumePercent(this, it) }
                publishVolume()
            }
            "brightness" -> {
                val trimmed = payload.trim().lowercase()
                if (trimmed == "auto" || trimmed == "-1") {
                    brightnessPercent = -1
                    KioskBus.send(KioskCommand.Brightness(-1f)) // Fenster-Override lösen
                } else {
                    val pct = trimmed.toIntOrNull()?.coerceIn(0, 100) ?: return
                    brightnessPercent = pct
                    // Echte Hardware-Helligkeit (voller Bereich); sonst Fenster-Fallback.
                    if (SystemController.setSystemBrightness(this, pct)) {
                        KioskBus.send(KioskCommand.Brightness(-1f))
                    } else {
                        KioskBus.send(KioskCommand.Brightness(pct / 100f))
                    }
                }
                publishBrightness()
            }
            else -> Log.w(TAG, "Unbekannter Befehl: $sub")
        }
    }

    private fun setScreen(on: Boolean) {
        if (on) wakeScreen()
        screenOn = on
        KioskBus.send(KioskCommand.Screen(on))
        publishScreen(on)
    }

    private fun isOn(payload: String): Boolean = when (payload.trim().lowercase()) {
        "on", "1", "true", "an", "ein" -> true
        else -> false
    }

    // ---- Foreground / Notification ---------------------------------------------

    private fun updateForeground(camera: Boolean, microphone: Boolean) {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, FullScreendyApp.NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        // specialUse (API 34+) statt dataSync: dataSync wird auf Android 15 nach ~6 h
        // pro Tag hart beendet (onTimeout) – das riss im Dauerbetrieb die App ab.
        var type = if (Build.VERSION.SDK_INT >= 34)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        else
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        if (camera) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        if (microphone) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        runCatching { ServiceCompat.startForeground(this, FullScreendyApp.NOTIF_ID, notif, type) }
            .onFailure { Log.w(TAG, "startForeground(type=$type) fehlgeschlagen", it) }
    }

    override fun onDestroy() {
        DiagLog.log(TAG, "Service onDestroy")
        val dt = settings.deviceTopic
        mqtt?.publish("$dt/status", "offline", retained = true, qos = 1)
        mqtt?.disconnect()
        battery?.stop()
        motion?.stop()
        sound?.stop()
        tts.shutdown()
        media.stop()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KioskService"
        const val ACTION_REFRESH = "de.kewl.fullscreendy.action.REFRESH"
    }
}
