package de.kewl.fullscreendy.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLang {
    EN, DE;

    companion object {
        fun from(code: String): AppLang = if (code.equals("de", ignoreCase = true)) DE else EN
    }
}

/**
 * Zentrale, zweisprachige UI-Texte. Standard ist Englisch; Deutsch per [AppLang.DE].
 * Bewusst kein Android-Ressourcen-Locale, damit die Sprache in der App umschaltbar
 * ist, unabhängig von der Systemsprache.
 */
class Strings(private val lang: AppLang) {
    private fun t(en: String, de: String) = if (lang == AppLang.DE) de else en

    // Menü / Drawer
    val version get() = t("Version", "Version")
    val statusConnected get() = t("MQTT connected", "MQTT verbunden")
    val statusDisconnected get() = t("MQTT disconnected", "MQTT getrennt")
    val navDashboard get() = t("Dashboard", "Dashboard")
    val navReload get() = t("Reload", "Neu laden")
    val navClearCache get() = t("Clear cache", "Cache leeren")
    val navScreenOff get() = t("Screen off", "Bildschirm aus")
    val navSettings get() = t("Settings", "Einstellungen")
    val navAbout get() = t("About", "Über")
    val navExit get() = t("Exit app", "App beenden")

    // Allgemein
    val save get() = t("Save", "Speichern")
    val back get() = t("Back", "Zurück")
    val cancel get() = t("Cancel", "Abbrechen")
    val ok get() = t("OK", "OK")

    // Einstellungen – Abschnitte
    val settings get() = t("Settings", "Einstellungen")
    val secConnection get() = t("Connection", "Verbindung")
    val secConnectionDesc get() = t("Dashboard URL, MQTT broker, topics", "Dashboard-URL, MQTT-Broker, Topics")
    val secDisplay get() = t("Display", "Anzeige")
    val secDisplayDesc get() = t("Zoom, font size, screen", "Zoom, Schriftgröße, Bildschirm")
    val secBehavior get() = t("Behavior", "Verhalten")
    val secBehaviorDesc get() = t("Motion, speech, sound, refresh", "Bewegung, Sprache, Ton, Aktualisieren")
    val secSounds get() = t("Sounds", "Töne")
    val secSoundsDesc get() = t("Sound folder", "Sound-Ordner")
    val secSystem get() = t("System", "System")
    val secSystemDesc get() = t("Language, autostart, PIN", "Sprache, Autostart, PIN")

    // Verbindung
    val dashboardUrl get() = t("Dashboard URL (http/https)", "Dashboard-URL (http/https)")
    val mqttBroker get() = t("MQTT broker", "MQTT-Broker")
    val host get() = t("Host / IP", "Host / IP")
    val port get() = t("Port", "Port")
    val useTls get() = t("Use TLS/SSL", "TLS/SSL verwenden")
    val username get() = t("User (optional)", "Benutzer (optional)")
    val password get() = t("Password (optional)", "Passwort (optional)")
    val topics get() = t("Topics", "Topics")
    val baseTopic get() = t("Base topic", "Basis-Topic")
    val deviceId get() = t("Device ID", "Geräte-ID")

    // Anzeige
    val ignoreFontScale get() = t("Font independent of system zoom", "Schrift unabhängig von System-Zoom")
    val allowZoom get() = t("Allow pinch-zoom in dashboard", "Zoomen im Dashboard erlauben")
    val keepScreenOn get() = t("Keep screen always on", "Bildschirm immer an")
    val dimTimeout get() = t("Screen dimming", "Bildschirm abdunkeln")
    val dimTimeoutHint get() = t(
        "Dims to black after inactivity (screen stays on so motion/sound wake reliably). Touch/motion wakes it.",
        "Dunkelt nach Inaktivität schwarz ab (Bildschirm bleibt an, damit Bewegung/Ton zuverlässig wecken). Berührung/Bewegung weckt."
    )
    val dimAfter get() = t("Dim after", "Abdunkeln nach")
    val off get() = t("off", "aus")
    val screenOff get() = t("Turn screen off", "Bildschirm ausschalten")
    val screenOffHint get() = t(
        "After even longer inactivity, turn the screen fully off (needs device admin). Then only touch/power/cmd wakes it – motion cannot.",
        "Nach noch längerer Inaktivität den Bildschirm ganz ausschalten (benötigt Geräteadmin). Dann weckt nur Berührung/Power/Befehl – Bewegung nicht."
    )
    val offAfter get() = t("Off after", "Aus nach")

    // Verhalten
    val motionDetection get() = t("Motion detection (camera)", "Bewegungserkennung (Kamera)")
    val motionWakesScreen get() = t("Motion wakes the screen", "Bewegung weckt Bildschirm")
    val motionSensitivity get() = t("Motion sensitivity", "Bewegungs-Empfindlichkeit")
    val soundWake get() = t("Wake on sound (microphone)", "Wecken bei Ton (Mikrofon)")
    val soundSensitivity get() = t("Sound sensitivity", "Ton-Empfindlichkeit")
    val motionTest get() = t("Motion test", "Bewegungs-Test")
    val soundTest get() = t("Sound test", "Ton-Test")
    val testHint get() = t(
        "Save first, then wave at the camera / make a noise – the dot lights up and the device vibrates on detection.",
        "Erst speichern, dann vor der Kamera winken / Geräusch machen – der Punkt leuchtet und das Gerät vibriert bei Erkennung."
    )
    val pullToRefresh get() = t("Pull down to reload", "Zum Aktualisieren nach unten ziehen")
    val ttsEnabled get() = t("Text-to-speech enabled", "Text-to-Speech aktiv")
    val mediaEnabled get() = t("Sound playback enabled", "Tonwiedergabe aktiv")

    // Töne
    val soundsHint get() = t(
        "Copy sound files into this folder (grant 'Allow file access' under System first), " +
            "then play via MQTT (e.g. cmd/mediaplay = ding.mp3):",
        "Tondateien in diesen Ordner kopieren (vorher unter System 'Dateizugriff erlauben'), " +
            "dann per MQTT abspielen (z. B. cmd/mediaplay = ding.mp3):"
    )

    // System
    val language get() = t("Language", "Sprache")
    val languageEnglish get() = t("English", "Englisch")
    val languageGerman get() = t("German", "Deutsch")
    val startOnBoot get() = t("Start on boot", "Beim Booten starten")
    val adminPin get() = t("Admin PIN (access to settings)", "Admin-PIN (Zugang zu Einstellungen)")
    val permissionsTitle get() = t("Permissions", "Berechtigungen")
    val enableDeviceAdmin get() = t("Enable device admin (screen lock)", "Geräteadmin aktivieren (Sperren)")
    val adminActive get() = t("Device admin active ✓", "Geräteadmin aktiv ✓")
    val allowBrightness get() = t("Allow brightness control", "Helligkeitssteuerung erlauben")
    val brightnessActive get() = t("Brightness control active ✓", "Helligkeitssteuerung aktiv ✓")
    val allowFileAccess get() = t("Allow file access (sounds)", "Dateizugriff erlauben (Töne)")
    val fileAccessActive get() = t("File access active ✓", "Dateizugriff aktiv ✓")
    val allowCamera get() = t("Allow camera (motion)", "Kamera erlauben (Bewegung)")
    val cameraActive get() = t("Camera allowed ✓", "Kamera erlaubt ✓")
    val allowMic get() = t("Allow microphone (sound)", "Mikrofon erlauben (Ton)")
    val micActive get() = t("Microphone allowed ✓", "Mikrofon erlaubt ✓")
    val openFailed get() = t("Could not open system settings", "Systemeinstellungen konnten nicht geöffnet werden")

    // Über
    val about get() = t("About", "Über")
    val githubRepo get() = t("Project on GitHub ↗", "Projekt auf GitHub ↗")
    val appVersionLabel get() = t("App version", "App-Version")
    val androidVersionLabel get() = t("Android version", "Android-Version")
    val ipAddressLabel get() = t("IP address", "IP-Adresse")
    val urlLabel get() = t("Dashboard URL", "Dashboard-URL")
    val deviceIdLabel get() = t("Device ID", "Geräte-ID")

    // PIN
    val pinTitle get() = t("Admin PIN", "Admin-PIN")
    val pinEnter get() = t("Enter PIN", "PIN eingeben")
    val pinWrong get() = t("Wrong PIN", "Falsche PIN")
    val pinDefaultHint get() = t("Default: 0000", "Standard: 0000")
}

val LocalStrings = staticCompositionLocalOf { Strings(AppLang.EN) }
