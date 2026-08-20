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
    val navDashboards get() = t("Dashboards", "Dashboards")
    val navActions get() = t("Actions", "Aktionen")
    val navApp get() = t("App", "App")
    val navReload get() = t("Reload", "Neu laden")
    val navClearCache get() = t("Clear cache", "Cache leeren")
    val navScreenOff get() = t("Screen off", "Bildschirm aus")
    val navSettings get() = t("Settings", "Einstellungen")
    val navAbout get() = t("About", "Über")
    val navExit get() = t("Exit app", "App beenden")
    val defaultBadge get() = t("Default", "Standard")

    // Allgemein
    val save get() = t("Save", "Speichern")
    val back get() = t("Back", "Zurück")
    val cancel get() = t("Cancel", "Abbrechen")
    val ok get() = t("OK", "OK")
    val remove get() = t("Remove", "Entfernen")

    // Einstellungen – Abschnitte
    val settings get() = t("Settings", "Einstellungen")
    val secDashboards get() = t("Dashboards", "Dashboards")
    val secDashboardsDesc get() = t(
        "URLs, names, login, default dashboard",
        "URLs, Namen, Login, Standard-Dashboard"
    )
    val secConnection get() = t("Connection", "Verbindung")
    val secConnectionDesc get() = t("MQTT broker, topics, device ID", "MQTT-Broker, Topics, Geräte-ID")
    val secDisplay get() = t("Display", "Anzeige")
    val secDisplayDesc get() = t("Zoom, font size, screen", "Zoom, Schriftgröße, Bildschirm")
    val secBehavior get() = t("Behavior", "Verhalten")
    val secBehaviorDesc get() = t("Motion, speech, sound, refresh", "Bewegung, Sprache, Ton, Aktualisieren")
    val secSounds get() = t("Sounds", "Töne")
    val secSoundsDesc get() = t("Sound folder", "Sound-Ordner")
    val secSystem get() = t("System", "System")
    val secSystemDesc get() = t("Language, autostart, PIN", "Sprache, Autostart, PIN")

    // Dashboards
    val dashboardsHint get() = t(
        "Up to 3 dashboards. Dashboard 1 always exists, more can be added with “+”.",
        "Bis zu 3 Dashboards. Dashboard 1 gibt es immer, weitere über „+“ hinzufügen."
    )
    val addDashboard get() = t("Add", "Hinzufügen")
    val dashboardName get() = t("Display name (menu)", "Anzeigename (Menü)")
    val dashboardUrl get() = t("Dashboard URL (http/https)", "Dashboard-URL (http/https)")
    val setAsDefault get() = t("Use as default dashboard", "Als Standard-Dashboard verwenden")
    val defaultDashboardHint get() = t(
        "The default dashboard is always shown again when the screen wakes up.",
        "Das Standard-Dashboard wird beim Aufwecken des Bildschirms immer wieder angezeigt."
    )
    val isDefaultDashboard get() = t("This is the default dashboard", "Dies ist das Standard-Dashboard")
    val removeDashboard get() = t("Remove this dashboard", "Dieses Dashboard entfernen")
    val removeDashboardConfirm get() = t(
        "Remove this dashboard including URL and login?",
        "Dieses Dashboard samt URL und Login entfernen?"
    )
    val dashboardLogin get() = t("Dashboard login (optional)", "Dashboard-Login (optional)")
    val dashboardLoginHint get() = t(
        "For a password-protected dashboard (e.g. FHEM basicAuth). Leave empty for none. Cleaner than user:pass in the URL.",
        "Für ein passwortgeschütztes Dashboard (z. B. FHEM basicAuth). Leer = keiner. Sauberer als user:pass in der URL."
    )
    val dashboardUser get() = t("Login user", "Login-Benutzer")
    val dashboardPass get() = t("Login password", "Login-Passwort")
    val allowInvalidCerts get() = t("Allow self-signed certificates", "Selbst-signierte Zertifikate erlauben")
    val allowInvalidCertsHint get() = t(
        "Enable for a local HTTPS dashboard with a self-signed/invalid certificate (e.g. FHEM). Insecure over untrusted networks – only use on your own LAN.",
        "Für ein lokales HTTPS-Dashboard mit selbst-signiertem/ungültigem Zertifikat aktivieren (z. B. FHEM). In fremden Netzen unsicher – nur im eigenen LAN nutzen."
    )

    // Verbindung (MQTT)
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
    val autoReload get() = t("Periodic reload", "Periodischer Reload")
    val autoReloadHint get() = t(
        "Reloads the dashboard regularly to free up memory (prevents the app being killed after days). Off = never.",
        "Lädt das Dashboard regelmäßig neu, um Speicher freizugeben (verhindert, dass die App nach Tagen beendet wird). Aus = nie."
    )
    val reloadEvery get() = t("Reload every", "Neu laden alle")
    val hoursShort get() = t(" h", " h")

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
    val pullToRefreshHint get() = t(
        "Only a downward pull that starts at the very top of the page reloads. Scrolling up simply stops at the top.",
        "Nur ein Zug nach unten, der am Seitenanfang beginnt, lädt neu. Hochscrollen stoppt einfach oben."
    )
    // Mikrofon (eine Aufgabe zur Zeit)
    val micSection get() = t("Microphone", "Mikrofon")
    val micExclusiveHint get() = t(
        "The microphone can only serve one purpose at a time.",
        "Das Mikrofon kann immer nur eine Aufgabe gleichzeitig erfüllen."
    )
    val micUseNone get() = t("Nothing", "Nichts")
    val micUseWake get() = t("Wake on sound", "Wecken bei Ton")
    val micUseWakeHint get() = t(
        "Loud ambient noise wakes the display (loudness only, no recording).",
        "Lauter Umgebungsschall weckt das Display (nur Lautstärke, keine Aufnahme)."
    )
    val micUseWeb get() = t("Voice control in dashboard", "Sprachsteuerung im Dashboard")
    val micUseWebHint get() = t(
        "Lets the loaded page use the microphone (getUserMedia, speech recognition). Only for dashboards you trust.",
        "Erlaubt der geladenen Seite das Mikrofon (getUserMedia, Spracherkennung). Nur für Dashboards, denen du vertraust."
    )
    val micNeedsPermission get() = t("Microphone permission missing", "Mikrofon-Berechtigung fehlt")
    val micRequestPermission get() = t("Grant microphone permission", "Mikrofon-Berechtigung erteilen")
    val micNeedsHttps get() = t(
        "The default dashboard uses http:// – without HTTPS the page gets no microphone at all.",
        "Das Standard-Dashboard läuft über http:// – ohne HTTPS bekommt die Seite gar kein Mikrofon."
    )
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
    val pinProtection get() = t("PIN protection for settings", "PIN-Schutz für Einstellungen")
    val pinProtectionHint get() = t(
        "Off: settings and the menu open without a PIN.",
        "Aus: Einstellungen und Menü öffnen sich ohne PIN."
    )
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
    val allowBatteryOpt get() = t("Disable battery optimization (24/7)", "Akku-Optimierung deaktivieren (24/7)")
    val batteryOptActive get() = t("Battery optimization disabled ✓", "Akku-Optimierung deaktiviert ✓")
    val allowOverlay get() = t("Display over other apps (auto-recover)", "Über anderen Apps anzeigen (Auto-Wiederherstellung)")
    val overlayActive get() = t("Display over other apps active ✓", "Über anderen Apps aktiv ✓")
    val openFailed get() = t("Could not open system settings", "Systemeinstellungen konnten nicht geöffnet werden")

    // Über
    val about get() = t("About", "Über")
    val aboutDevice get() = t("Device", "Gerät")
    val aboutUpdate get() = t("Update", "Update")
    val aboutProject get() = t("Project", "Projekt")
    val githubRepo get() = t("Project on GitHub ↗", "Projekt auf GitHub ↗")
    val starGithub get() = t("Star on GitHub", "Stern auf GitHub geben")
    val viewSource get() = t("View source code", "Quellcode ansehen")
    val supportProject get() = t("Support the project", "Projekt unterstützen")
    val updateChecking get() = t("Checking for updates…", "Suche nach Updates…")
    val updateUpToDate get() = t("You have the latest version", "Du hast die neueste Version")
    val updateAvailable get() = t("Update available:", "Update verfügbar:")
    val updateInstall get() = t("Download & install", "Herunterladen & installieren")
    val updateDownloading get() = t("Downloading…", "Wird heruntergeladen…")
    val updateCheckAgain get() = t("Check again", "Erneut prüfen")
    val updateOpenPage get() = t("Open release page", "Zur Release-Seite")
    val updateViaFdroid get() = t(
        "Installed via F-Droid? Then update there – this build has no installer.",
        "Über F-Droid installiert? Dann dort aktualisieren – dieser Build bringt keinen Installer mit."
    )
    val updateError get() = t("Update check failed", "Update-Prüfung fehlgeschlagen")
    val updateDownloadFailed get() = t("Download failed", "Download fehlgeschlagen")
    val updateAllowInstall get() = t(
        "Please allow installing unknown apps, then tap again.",
        "Bitte Installation unbekannter Apps erlauben, dann erneut tippen."
    )
    val appVersionLabel get() = t("App version", "App-Version")
    val androidVersionLabel get() = t("Android version", "Android-Version")
    val ipAddressLabel get() = t("IP address", "IP-Adresse")
    val deviceIdLabel get() = t("Device ID", "Geräte-ID")
    val licenseLine get() = t("MIT license", "MIT-Lizenz")

    // PIN
    val pinTitle get() = t("Admin PIN", "Admin-PIN")
    val pinEnter get() = t("Enter PIN", "PIN eingeben")
    val pinWrong get() = t("Wrong PIN", "Falsche PIN")
    val pinDefaultHint get() = t("Default: 0000", "Standard: 0000")
}

val LocalStrings = staticCompositionLocalOf { Strings(AppLang.EN) }
