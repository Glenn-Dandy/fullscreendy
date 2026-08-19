package de.kewl.fullscreendy

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.data.SettingsRepository
import de.kewl.fullscreendy.device.SystemController
import de.kewl.fullscreendy.diag.DiagLog
import de.kewl.fullscreendy.i18n.AppLang
import de.kewl.fullscreendy.i18n.LocalStrings
import de.kewl.fullscreendy.i18n.Strings
import de.kewl.fullscreendy.kiosk.KioskBus
import de.kewl.fullscreendy.kiosk.KioskCommand
import de.kewl.fullscreendy.kiosk.KioskStatus
import de.kewl.fullscreendy.service.KioskService
import de.kewl.fullscreendy.ui.AboutScreen
import de.kewl.fullscreendy.ui.DashboardAuth
import de.kewl.fullscreendy.ui.KioskWebView
import de.kewl.fullscreendy.ui.PinDialog
import de.kewl.fullscreendy.ui.SectionLabel
import de.kewl.fullscreendy.ui.SettingsScreen
import de.kewl.fullscreendy.ui.StatusDot
import de.kewl.fullscreendy.ui.rememberWebController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppPage { Dashboard, Settings, About }

class MainActivity : ComponentActivity() {

    private lateinit var repo: SettingsRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Ergebnis egal – Features degradieren sanft ohne Berechtigung. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository(applicationContext)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestPermissions()
        startKioskService()

        setContent { KioskRoot(repo) }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        // Im Vordergrund: Kamera/Mikrofon-Detektoren (neu) starten – im Hintergrund
        // ist das oft blockiert.
        ContextCompat.startForegroundService(
            this,
            Intent(this, KioskService::class.java).setAction(KioskService.ACTION_REFRESH)
        )
    }

    /** Bildschirm wecken und unsicheren Sperrbildschirm lösen (soweit möglich). */
    fun unlockDevice() {
        runCatching {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun requestPermissions() {
        val needed = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        // Android < 11: klassische Storage-Berechtigung für den Sound-Ordner.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            needed += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun startKioskService() {
        ContextCompat.startForegroundService(this, Intent(this, KioskService::class.java))
    }

    fun setScreenBrightness(level: Float) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (level < 0f)
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else level.coerceIn(0f, 1f)
        }
    }

    fun setKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Composable
    private fun KioskRoot(repo: SettingsRepository) {
        // WICHTIG: erst auf die echten, gespeicherten Werte warten (initial = null).
        // Sonst zeigt der erste Frame leere Defaults und ein Zurück/Speichern könnte
        // die gespeicherten Einstellungen überschreiben.
        val settings = repo.settings.collectAsState(initial = null).value
        if (settings == null) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {}
            return
        }
        val strings = remember(settings.language) { Strings(AppLang.from(settings.language)) }

        CompositionLocalProvider(LocalStrings provides strings) {
            KioskContent(repo, settings)
        }
    }

    @Composable
    private fun KioskContent(repo: SettingsRepository, settings: Settings) {
        val scope = rememberCoroutineScope()
        val webController = rememberWebController()
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        var page by remember { mutableStateOf(AppPage.Dashboard) }
        var showPin by remember { mutableStateOf(false) }
        var overlayVisible by remember { mutableStateOf(false) }
        var brightness by remember { mutableStateOf(-1f) }
        var activityNonce by remember { mutableStateOf(0) }
        // Gerade angezeigtes Dashboard; wird beim Aufwecken aufs Standard zurückgesetzt.
        var dashboardIndex by remember { mutableStateOf(settings.defaultIndex) }
        // Per MQTT (cmd/url) gesetzte Ad-hoc-Adresse; überlagert das Dashboard, bis
        // wieder eines gewählt wird oder der Bildschirm aufwacht.
        var overrideUrl by remember { mutableStateOf<String?>(null) }

        val activeIndex = dashboardIndex.coerceIn(0, settings.dashboards.lastIndex.coerceAtLeast(0))
        val dashboard = settings.dashboardAt(activeIndex)

        /** Dashboard wechseln und eine eventuelle Ad-hoc-URL verwerfen. */
        fun showDashboard(index: Int) {
            overrideUrl = null
            dashboardIndex = index
        }

        LaunchedEffect(settings.isConfigured) {
            if (!settings.isConfigured) page = AppPage.Settings
        }
        // Der Dienst meldet das aktive Dashboard per MQTT.
        LaunchedEffect(activeIndex) { KioskStatus.setActiveDashboard(activeIndex) }
        // Bildschirm anlassen, wenn Wecken-auf-Bewegung/Ton oder Auto-Abdunkeln aktiv ist –
        // nur so laufen Kamera/Mikrofon zuverlässig weiter (kein OS-Screen-Off, kein
        // Wallpaper-Blitzen). "Aus" wird über das schwarze Overlay realisiert.
        val keepScreenOn = settings.keepScreenOn ||
            (settings.motionEnabled && settings.motionWakesScreen) ||
            settings.soundWakeEnabled ||
            settings.dimTimeoutSecs > 0
        LaunchedEffect(keepScreenOn) { setKeepScreenOn(keepScreenOn) }
        LaunchedEffect(overlayVisible, brightness) {
            setScreenBrightness(if (overlayVisible) 0f else brightness)
        }
        // Beim Aufwecken (Overlay verschwindet) immer zurück aufs Standard-Dashboard –
        // egal, was zuletzt manuell im Menü gewählt wurde.
        LaunchedEffect(overlayVisible) {
            if (!overlayVisible) showDashboard(settings.defaultIndex)
        }
        // Nach Inaktivität abdunkeln und (optional) ausschalten; jede Berührung/Bewegung
        // setzt den Timer zurück (activityNonce ändert sich → Effekt startet neu).
        LaunchedEffect(activityNonce, settings.dimTimeoutSecs, settings.screenOffSecs, page) {
            if (page != AppPage.Dashboard) return@LaunchedEffect
            if (settings.dimTimeoutSecs > 0) {
                delay(settings.dimTimeoutSecs * 1000L)
                overlayVisible = true
            }
            if (settings.screenOffSecs > 0) {
                val already = if (settings.dimTimeoutSecs > 0) settings.dimTimeoutSecs else 0
                delay((settings.screenOffSecs - already).coerceAtLeast(0) * 1000L)
                SystemController.lock(this@MainActivity) // Bildschirm aus (benötigt Geräteadmin)
            }
        }
        LaunchedEffect(Unit) {
            KioskBus.commands.collect { cmd ->
                when (cmd) {
                    is KioskCommand.LoadUrl -> overrideUrl = cmd.url
                    is KioskCommand.SelectDashboard -> showDashboard(cmd.index)
                    KioskCommand.Reload -> webController.reload()
                    KioskCommand.ClearCache -> webController.clearCache()
                    is KioskCommand.Screen -> {
                        // Nur Overlay entfernen/setzen – KEIN Keyguard-Eingriff beim Wecken
                        // (das holte in 0.4.4 den Wischcode nach vorn). Entsperren nur via cmd/unlock.
                        overlayVisible = !cmd.on
                        if (cmd.on) activityNonce++ // Abdunkel-Timer neu starten
                    }
                    is KioskCommand.Brightness -> brightness = cmd.level
                    KioskCommand.Unlock -> unlockDevice()
                }
            }
        }
        // Periodischer Reload: setzt den über Tage wachsenden WebView-Speicher
        // (DOM/JS/Bild-Cache) zurück – der größte Hebel gegen LOW_MEMORY im Dauerbetrieb.
        LaunchedEffect(settings.reloadIntervalMins) {
            val mins = settings.reloadIntervalMins
            if (mins > 0) {
                while (true) {
                    delay(mins * 60_000L)
                    webController.reload()
                    DiagLog.log("Reload", "Periodischer WebView-Reload (alle ${mins} min)")
                }
            }
        }
        // Bei Speicherdruck des Systems proaktiv den WebView-Speicher freigeben –
        // senkt die Chance, dass Android die App als Erstes killt.
        val appContext = LocalContext.current.applicationContext
        DisposableEffect(webController) {
            val cb = object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {}
                @Deprecated("Deprecated in Java") override fun onLowMemory() {}
                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                        webController.webView?.let { wv ->
                            wv.post {
                                wv.clearCache(false)
                                @Suppress("DEPRECATION") wv.freeMemory()
                            }
                        }
                        DiagLog.log("Mem", "onTrimMemory($level) – WebView-Speicher freigegeben")
                    }
                }
            }
            appContext.registerComponentCallbacks(cb)
            onDispose { appContext.unregisterComponentCallbacks(cb) }
        }

        fun closeDrawer() = scope.launch { drawerState.close() }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                AppDrawer(
                    settings = settings,
                    activeDashboard = activeIndex,
                    onDashboard = { index -> closeDrawer(); page = AppPage.Dashboard; showDashboard(index) },
                    onReload = { closeDrawer(); webController.reload() },
                    onClearCache = { closeDrawer(); webController.clearCache() },
                    onScreenOff = { closeDrawer(); overlayVisible = true },
                    onSettings = {
                        closeDrawer()
                        if (settings.pinEnabled) showPin = true else page = AppPage.Settings
                    },
                    onAbout = { closeDrawer(); page = AppPage.About },
                    onExit = {
                        stopService(Intent(this@MainActivity, KioskService::class.java))
                        finishAndRemoveTask()
                    },
                )
            }
        ) {
            // Schwarzer Hintergrund (kein heller "weißer Blitz" beim Aufwecken, bevor
            // die WebView neu zeichnet). Unterseiten haben eigene, thematisierte Flächen.
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Jede Berührung zählt als Aktivität (Initial-Pass = nicht konsumierend,
                        // damit WebView/Buttons weiter funktionieren).
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    if (overlayVisible) overlayVisible = false
                                    activityNonce++
                                }
                            }
                        }
                ) {
                    // Dashboard-WebView ist immer vorhanden; Unterseiten legen sich darüber.
                    // Login/Zertifikat kommen über den Controller, damit ein Dashboard-Wechsel
                    // die WebView nicht neu aufbaut.
                    key(settings.ignoreSystemFontScale, settings.zoomEnabled) {
                        KioskWebView(
                            url = overrideUrl ?: dashboard.url,
                            controller = webController,
                            ignoreSystemFontScale = settings.ignoreSystemFontScale,
                            zoomEnabled = settings.zoomEnabled,
                            pullToRefresh = settings.pullToRefresh,
                            auth = DashboardAuth(
                                user = dashboard.user,
                                pass = dashboard.pass,
                                allowInvalidCerts = dashboard.allowInvalidCerts
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (overlayVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                                .clickable {
                                    overlayVisible = false
                                    KioskBus.send(KioskCommand.Screen(on = true))
                                }
                        )
                    }

                    // Wisch-Zone am linken Rand öffnet das Menü.
                    if (page == AppPage.Dashboard && drawerState.isClosed && !overlayVisible) {
                        var acc by remember { mutableStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxHeight()
                                .width(28.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { acc = 0f },
                                        onHorizontalDrag = { _, delta ->
                                            acc += delta
                                            if (acc > 120f) {
                                                scope.launch { drawerState.open() }
                                                acc = -1e6f
                                            }
                                        },
                                        onDragEnd = { acc = 0f }
                                    )
                                }
                        )
                    }

                    when (page) {
                        AppPage.Settings -> SettingsScreen(
                            initial = settings,
                            onPersist = { scope.launch { repo.save(it) } },
                            onExit = { if (settings.isConfigured) page = AppPage.Dashboard }
                        )
                        AppPage.About -> AboutScreen(settings, onBack = { page = AppPage.Dashboard })
                        AppPage.Dashboard -> Unit
                    }
                }
            }
        }

        if (showPin) {
            PinDialog(
                expectedPin = settings.adminPin,
                onSuccess = { showPin = false; page = AppPage.Settings },
                onDismiss = { showPin = false }
            )
        }
    }
}

@Composable
private fun AppDrawer(
    settings: Settings,
    activeDashboard: Int,
    onDashboard: (Int) -> Unit,
    onReload: () -> Unit,
    onClearCache: () -> Unit,
    onScreenOff: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    val mqttConnected by KioskStatus.mqttConnected.collectAsState()
    // Halb konfigurierte Reiter (ohne URL) tauchen im Menü nicht auf.
    val dashboards = settings.usableDashboards().ifEmpty {
        listOf(IndexedValue(0, settings.dashboardAt(0)))
    }

    ModalDrawerSheet(modifier = Modifier.width(320.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "FullScreendy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${s.version} ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(mqttConnected)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (mqttConnected) s.statusConnected else s.statusDisconnected,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            HorizontalDivider()

            val itemPad = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)

            SectionLabel(s.navDashboards, modifier = Modifier.padding(start = 28.dp, top = 12.dp))
            dashboards.forEach { (index, config) ->
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    label = { Text(config.displayName(index)) },
                    badge = {
                        if (index == settings.defaultIndex) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = s.defaultBadge,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    selected = index == activeDashboard,
                    onClick = { onDashboard(index) },
                    modifier = itemPad
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel(s.navActions, modifier = Modifier.padding(start = 28.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                label = { Text(s.navReload) }, selected = false, onClick = onReload, modifier = itemPad
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                label = { Text(s.navClearCache) }, selected = false, onClick = onClearCache, modifier = itemPad
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                label = { Text(s.navScreenOff) }, selected = false, onClick = onScreenOff, modifier = itemPad
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel(s.navApp, modifier = Modifier.padding(start = 28.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text(s.navSettings) }, selected = false, onClick = onSettings, modifier = itemPad
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                label = { Text(s.navAbout) }, selected = false, onClick = onAbout, modifier = itemPad
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                label = { Text(s.navExit) }, selected = false, onClick = onExit, modifier = itemPad
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
