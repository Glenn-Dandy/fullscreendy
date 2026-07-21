package de.kewl.fullscreendy.data

/**
 * Alle vom Nutzer konfigurierbaren Einstellungen der Kiosk-App.
 */
data class Settings(
    val dashboardUrl: String = "",
    /** Optionaler HTTP-Basic-Auth-Login fürs Dashboard (z. B. FHEM basicAuth); leer = keiner. */
    val dashboardUser: String = "",
    val dashboardPass: String = "",
    /** HTTPS mit selbst-signiertem/ungültigem Zertifikat zulassen (unsicher, daher Standard aus). */
    val allowInvalidCerts: Boolean = false,
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
    /** Dashboard periodisch neu laden (setzt WebView-Speicher zurück); Minuten, 0 = aus. */
    val reloadIntervalMins: Int = 360,
    val startOnBoot: Boolean = true,
    /** UI-Sprache: "en" (Standard) oder "de". */
    val language: String = "en",
    val adminPin: String = "0000",
) {
    val isConfigured: Boolean
        get() = dashboardUrl.isNotBlank()

    /** Basis-Topic für dieses Gerät, z. B. "fhem/tablet/tablet1". */
    val deviceTopic: String
        get() = "${baseTopic.trimEnd('/')}/$deviceId"
}
