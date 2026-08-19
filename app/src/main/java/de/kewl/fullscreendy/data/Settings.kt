package de.kewl.fullscreendy.data

/**
 * Ein Dashboard (ein Reiter in den Einstellungen). Jedes Dashboard bringt seinen
 * eigenen Login und seine eigene Zertifikats-Einstellung mit, damit auch mehrere
 * Server gemischt werden können (z. B. FHEM und Grafana).
 */
data class DashboardConfig(
    val name: String = "",
    val url: String = "",
    val user: String = "",
    val pass: String = "",
    val allowInvalidCerts: Boolean = false,
) {
    /** Anzeigename; ohne eigenen Namen "Dashboard 1", "Dashboard 2", … */
    fun displayName(index: Int): String = name.trim().ifBlank { "Dashboard ${index + 1}" }
}

/**
 * Alle vom Nutzer konfigurierbaren Einstellungen der Kiosk-App.
 */
data class Settings(
    /** Mindestens eines, höchstens [MAX_DASHBOARDS]; Index 0 ist "Dashboard 1". */
    val dashboards: List<DashboardConfig> = listOf(DashboardConfig()),
    /** Index des Standard-Dashboards – wird beim Aufwecken immer angezeigt. */
    val defaultDashboard: Int = 0,
    val mqttHost: String = "",
    val mqttPort: Int = 1883,
    val mqttTls: Boolean = false,
    val mqttUser: String = "",
    val mqttPass: String = "",
    val baseTopic: String = "fhem/tablet",
    val deviceId: String = "tablet1",
    val motionEnabled: Boolean = true,
    val motionWakesScreen: Boolean = true,
    val motionSensitivity: Int = 50,
    val soundWakeEnabled: Boolean = false,
    val soundSensitivity: Int = 50,
    val keepScreenOn: Boolean = false,
    /** Nach so vielen Sekunden Inaktivität schwarz abdunkeln (Overlay); 0 = nie. */
    val dimTimeoutSecs: Int = 60,
    /** Nach so vielen Sekunden Inaktivität Bildschirm ausschalten (Geräteadmin); 0 = nie. */
    val screenOffSecs: Int = 0,
    val ignoreSystemFontScale: Boolean = true,
    val ttsEnabled: Boolean = true,
    val mediaEnabled: Boolean = true,
    val zoomEnabled: Boolean = false,
    val pullToRefresh: Boolean = true,
    /** Dashboard-Seiten dürfen das Mikrofon nutzen (getUserMedia); aus = Anfragen werden abgelehnt. */
    val webMicEnabled: Boolean = false,
    /** Dashboard periodisch neu laden (setzt WebView-Speicher zurück); Minuten, 0 = aus. */
    val reloadIntervalMins: Int = 360,
    val startOnBoot: Boolean = true,
    /** UI-Sprache: "en" (Standard) oder "de". */
    val language: String = "en",
    /** PIN-Abfrage vor den Einstellungen; aus = Einstellungen ohne PIN erreichbar. */
    val pinEnabled: Boolean = true,
    val adminPin: String = "0000",
) {
    val isConfigured: Boolean
        get() = dashboards.any { it.url.isNotBlank() }

    /** Index des Standard-Dashboards, auf den gültigen Bereich begrenzt. */
    val defaultIndex: Int
        get() = defaultDashboard.coerceIn(0, dashboards.lastIndex.coerceAtLeast(0))

    /** Dashboard an [index]; außerhalb des Bereichs das Standard-Dashboard. */
    fun dashboardAt(index: Int): DashboardConfig =
        dashboards.getOrNull(index) ?: dashboards.getOrNull(defaultIndex) ?: DashboardConfig()

    val defaultDashboardConfig: DashboardConfig
        get() = dashboardAt(defaultIndex)

    /** Im Menü sichtbare Dashboards (Index → Konfiguration); leere URLs bleiben außen vor. */
    fun usableDashboards(): List<IndexedValue<DashboardConfig>> =
        dashboards.withIndex().filter { it.value.url.isNotBlank() }

    /** Basis-Topic für dieses Gerät, z. B. "fhem/tablet/tablet1". */
    val deviceTopic: String
        get() = "${baseTopic.trimEnd('/')}/$deviceId"

    companion object {
        const val MAX_DASHBOARDS = 3
    }
}
