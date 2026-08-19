package de.kewl.fullscreendy.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import de.kewl.fullscreendy.data.DashboardConfig
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.device.SystemController
import de.kewl.fullscreendy.i18n.LocalStrings
import de.kewl.fullscreendy.i18n.Strings
import de.kewl.fullscreendy.kiosk.KioskStatus
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

private enum class Section { Home, Dashboards, Connection, Display, Behavior, Sounds, System }

@Composable
fun SettingsScreen(
    initial: Settings,
    onPersist: (Settings) -> Unit,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    var draft by remember { mutableStateOf(initial) }
    var section by remember { mutableStateOf(Section.Home) }
    var dashboardTab by remember { mutableStateOf(initial.defaultIndex) }

    // Auto-Speichern: ~1 s nach der letzten Änderung persistieren (kein Save-Button).
    LaunchedEffect(draft) {
        delay(1000)
        onPersist(draft)
    }

    val title = when (section) {
        Section.Home -> s.settings
        Section.Dashboards -> s.secDashboards
        Section.Connection -> s.secConnection
        Section.Display -> s.secDisplay
        Section.Behavior -> s.secBehavior
        Section.Sounds -> s.secSounds
        Section.System -> s.secSystem
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(
                title = title,
                onBack = {
                    if (section == Section.Home) { onPersist(draft); onExit() } else section = Section.Home
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (section) {
                    Section.Home -> SectionCard {
                        ActionRow(Icons.Filled.Dashboard, s.secDashboards, s.secDashboardsDesc) {
                            section = Section.Dashboards
                        }
                        RowDivider()
                        ActionRow(Icons.Filled.Router, s.secConnection, s.secConnectionDesc) {
                            section = Section.Connection
                        }
                        RowDivider()
                        ActionRow(Icons.Filled.Brightness6, s.secDisplay, s.secDisplayDesc) {
                            section = Section.Display
                        }
                        RowDivider()
                        ActionRow(Icons.Filled.Tune, s.secBehavior, s.secBehaviorDesc) {
                            section = Section.Behavior
                        }
                        RowDivider()
                        ActionRow(Icons.AutoMirrored.Filled.VolumeUp, s.secSounds, s.secSoundsDesc) {
                            section = Section.Sounds
                        }
                        RowDivider()
                        ActionRow(Icons.Filled.Settings, s.secSystem, s.secSystemDesc) {
                            section = Section.System
                        }
                    }
                    Section.Dashboards -> DashboardsSection(
                        draft = draft,
                        s = s,
                        tab = dashboardTab,
                        onTab = { dashboardTab = it },
                        onChange = { draft = it }
                    )
                    Section.Connection -> ConnectionSection(draft, s) { draft = it }
                    Section.Display -> DisplaySection(draft, s) { draft = it }
                    Section.Behavior -> BehaviorSection(draft, s) { draft = it }
                    Section.Sounds -> SoundsSection(s)
                    Section.System -> SystemSection(draft, s) { draft = it }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---- Dashboards -----------------------------------------------------------------

@Composable
private fun DashboardsSection(
    draft: Settings,
    s: Strings,
    tab: Int,
    onTab: (Int) -> Unit,
    onChange: (Settings) -> Unit,
) {
    // Es gibt immer mindestens Dashboard 1 – notfalls wiederherstellen.
    if (draft.dashboards.isEmpty()) {
        onChange(draft.copy(dashboards = listOf(DashboardConfig())))
        return
    }
    val index = tab.coerceIn(0, draft.dashboards.lastIndex)
    val dashboard = draft.dashboards[index]
    var confirmRemove by remember { mutableStateOf(false) }

    /** Ändert nur das gerade offene Dashboard. */
    fun edit(block: (DashboardConfig) -> DashboardConfig) {
        val list = draft.dashboards.toMutableList()
        list[index] = block(list[index])
        onChange(draft.copy(dashboards = list))
    }

    // Reiter: Dashboard 1 / 2 / 3 (+ Hinzufügen)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        draft.dashboards.forEachIndexed { i, d ->
            FilterChip(
                selected = i == index,
                onClick = { onTab(i) },
                label = { Text(d.displayName(i)) },
                leadingIcon = if (i == draft.defaultIndex) {
                    { Icon(Icons.Filled.Star, contentDescription = s.defaultBadge, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        if (draft.dashboards.size < Settings.MAX_DASHBOARDS) {
            AssistChip(
                onClick = {
                    val list = draft.dashboards + DashboardConfig()
                    onChange(draft.copy(dashboards = list))
                    onTab(list.lastIndex)
                },
                label = { Text(s.addDashboard) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
    Text(s.dashboardsHint, style = MaterialTheme.typography.bodySmall)

    SectionCard(dashboard.displayName(index)) {
        OutlinedTextField(
            value = dashboard.name,
            onValueChange = { v -> edit { it.copy(name = v) } },
            label = { Text(s.dashboardName) },
            placeholder = { Text("Dashboard ${index + 1}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dashboard.url,
            onValueChange = { v -> edit { it.copy(url = v) } },
            label = { Text(s.dashboardUrl) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(s.dashboardLogin) {
        Text(s.dashboardLoginHint, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = dashboard.user,
            onValueChange = { v -> edit { it.copy(user = v) } },
            label = { Text(s.dashboardUser) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dashboard.pass,
            onValueChange = { v -> edit { it.copy(pass = v) } },
            label = { Text(s.dashboardPass) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        SwitchRow(s.allowInvalidCerts, dashboard.allowInvalidCerts, hint = s.allowInvalidCertsHint) { v ->
            edit { it.copy(allowInvalidCerts = v) }
        }
    }

    SectionCard {
        if (index == draft.defaultIndex) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text(s.isDefaultDashboard, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            ActionRow(Icons.Filled.StarBorder, s.setAsDefault, showArrow = false) {
                onChange(draft.copy(defaultDashboard = index))
            }
        }
        Text(s.defaultDashboardHint, style = MaterialTheme.typography.bodySmall)
        if (draft.dashboards.size > 1) {
            RowDivider()
            ActionRow(
                Icons.Filled.Delete,
                s.removeDashboard,
                tint = MaterialTheme.colorScheme.error,
                showArrow = false
            ) { confirmRemove = true }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(dashboard.displayName(index)) },
            text = { Text(s.removeDashboardConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    val list = draft.dashboards.toMutableList().apply { removeAt(index) }
                    val newDefault = when {
                        draft.defaultIndex == index -> 0
                        draft.defaultIndex > index -> draft.defaultIndex - 1
                        else -> draft.defaultIndex
                    }
                    onChange(draft.copy(dashboards = list, defaultDashboard = newDefault))
                    onTab(index.coerceAtMost(list.lastIndex))
                }) { Text(s.remove) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(s.cancel) } }
        )
    }
}

// ---- Verbindung (MQTT) ----------------------------------------------------------

@Composable
private fun ConnectionSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    var portText by remember { mutableStateOf(draft.mqttPort.toString()) }

    SectionCard(s.mqttBroker) {
        OutlinedTextField(
            value = draft.mqttHost,
            onValueChange = { onChange(draft.copy(mqttHost = it)) },
            label = { Text(s.host) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = portText,
            onValueChange = {
                portText = it.filter(Char::isDigit)
                onChange(draft.copy(mqttPort = portText.toIntOrNull() ?: 1883))
            },
            label = { Text(s.port) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        SwitchRow(s.useTls, draft.mqttTls) { onChange(draft.copy(mqttTls = it)) }
        OutlinedTextField(
            value = draft.mqttUser,
            onValueChange = { onChange(draft.copy(mqttUser = it)) },
            label = { Text(s.username) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.mqttPass,
            onValueChange = { onChange(draft.copy(mqttPass = it)) },
            label = { Text(s.password) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(s.topics) {
        OutlinedTextField(
            value = draft.baseTopic,
            onValueChange = { onChange(draft.copy(baseTopic = it)) },
            label = { Text(s.baseTopic) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = draft.deviceId,
            onValueChange = { onChange(draft.copy(deviceId = it)) },
            label = { Text(s.deviceId) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "${draft.baseTopic.trimEnd('/')}/${draft.deviceId}/…",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ---- Anzeige --------------------------------------------------------------------

@Composable
private fun DisplaySection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    SectionCard {
        SwitchRow(s.ignoreFontScale, draft.ignoreSystemFontScale) {
            onChange(draft.copy(ignoreSystemFontScale = it))
        }
        RowDivider()
        SwitchRow(s.allowZoom, draft.zoomEnabled) { onChange(draft.copy(zoomEnabled = it)) }
        RowDivider()
        SwitchRow(s.keepScreenOn, draft.keepScreenOn) { onChange(draft.copy(keepScreenOn = it)) }
    }

    SectionCard(s.dimTimeout) {
        Text(s.dimTimeoutHint, style = MaterialTheme.typography.bodySmall)
        SliderRow(
            label = s.dimAfter,
            value = draft.dimTimeoutSecs,
            max = 300,
            suffix = " s",
            zeroLabel = s.off
        ) { onChange(draft.copy(dimTimeoutSecs = it)) }
    }

    SectionCard(s.screenOff) {
        Text(s.screenOffHint, style = MaterialTheme.typography.bodySmall)
        SliderRow(
            label = s.offAfter,
            value = draft.screenOffSecs,
            max = 600,
            suffix = " s",
            zeroLabel = s.off
        ) { onChange(draft.copy(screenOffSecs = it)) }
    }

    SectionCard(s.autoReload) {
        Text(s.autoReloadHint, style = MaterialTheme.typography.bodySmall)
        // Slider in Stunden (1-h-Schritte); intern als Minuten gespeichert.
        SliderRow(
            label = s.reloadEvery,
            value = draft.reloadIntervalMins / 60,
            max = 24,
            suffix = s.hoursShort,
            zeroLabel = s.off
        ) { onChange(draft.copy(reloadIntervalMins = it * 60)) }
    }
}

// ---- Verhalten ------------------------------------------------------------------

@Composable
private fun BehaviorSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    SectionCard {
        SwitchRow(s.motionDetection, draft.motionEnabled) { onChange(draft.copy(motionEnabled = it)) }
        if (draft.motionEnabled) {
            SwitchRow(s.motionWakesScreen, draft.motionWakesScreen) {
                onChange(draft.copy(motionWakesScreen = it))
            }
            SliderRow(s.motionSensitivity, draft.motionSensitivity) {
                onChange(draft.copy(motionSensitivity = it))
            }
        }
        RowDivider()
        SwitchRow(s.soundWake, draft.soundWakeEnabled) { onChange(draft.copy(soundWakeEnabled = it)) }
        if (draft.soundWakeEnabled) {
            SliderRow(s.soundSensitivity, draft.soundSensitivity) {
                onChange(draft.copy(soundSensitivity = it))
            }
        }
    }

    SectionCard {
        SwitchRow(s.pullToRefresh, draft.pullToRefresh, hint = s.pullToRefreshHint) {
            onChange(draft.copy(pullToRefresh = it))
        }
        RowDivider()
        SwitchRow(s.ttsEnabled, draft.ttsEnabled) { onChange(draft.copy(ttsEnabled = it)) }
        RowDivider()
        SwitchRow(s.mediaEnabled, draft.mediaEnabled) { onChange(draft.copy(mediaEnabled = it)) }
    }

    if (draft.motionEnabled || draft.soundWakeEnabled) {
        SectionCard {
            Text(s.testHint, style = MaterialTheme.typography.bodySmall)
            val context = LocalContext.current

            if (draft.motionEnabled) {
                val motionActive by KioskStatus.motionActive.collectAsState()
                LaunchedEffect(motionActive) {
                    if (motionActive) SystemController.vibrate(context, 100)
                }
                IndicatorRow(s.motionTest, motionActive)
            }
            if (draft.soundWakeEnabled) {
                val soundAt by KioskStatus.soundAt.collectAsState()
                var soundFlash by remember { mutableStateOf(false) }
                LaunchedEffect(soundAt) {
                    if (soundAt > 0) {
                        SystemController.vibrate(context, 100)
                        soundFlash = true
                        delay(1500)
                        soundFlash = false
                    }
                }
                IndicatorRow(s.soundTest, soundFlash)
            }
        }
    }
}

// ---- Töne -----------------------------------------------------------------------

@Composable
private fun SoundsSection(s: Strings) {
    val soundsPath = remember {
        @Suppress("DEPRECATION")
        File(Environment.getExternalStorageDirectory(), "FullScreendy").absolutePath
    }
    SectionCard {
        Text(s.soundsHint, style = MaterialTheme.typography.bodyMedium)
        Text(soundsPath, style = MaterialTheme.typography.bodySmall)
    }
}

// ---- System ---------------------------------------------------------------------

@Composable
private fun SystemSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    SectionCard(s.language) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = draft.language == "en",
                onClick = { onChange(draft.copy(language = "en")) },
                label = { Text(s.languageEnglish) }
            )
            FilterChip(
                selected = draft.language == "de",
                onClick = { onChange(draft.copy(language = "de")) },
                label = { Text(s.languageGerman) }
            )
        }
    }

    SectionCard {
        SwitchRow(s.startOnBoot, draft.startOnBoot) { onChange(draft.copy(startOnBoot = it)) }
        RowDivider()
        SwitchRow(s.pinProtection, draft.pinEnabled, hint = s.pinProtectionHint) {
            onChange(draft.copy(pinEnabled = it))
        }
        if (draft.pinEnabled) {
            OutlinedTextField(
                value = draft.adminPin,
                onValueChange = { onChange(draft.copy(adminPin = it.filter(Char::isDigit).take(8))) },
                label = { Text(s.adminPin) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    PermissionsCard(s)
}

@Composable
private fun PermissionsCard(s: Strings) {
    val context = LocalContext.current

    fun hasPerm(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    // Status aller Berechtigungen – wird bei Rückkehr in die App aktualisiert.
    var adminActive by remember { mutableStateOf(SystemController.isAdminActive(context)) }
    var brightnessOk by remember { mutableStateOf(SystemController.canWriteSettings(context)) }
    var fileOk by remember { mutableStateOf(SystemController.hasAllFilesAccess(context)) }
    var cameraOk by remember { mutableStateOf(hasPerm(Manifest.permission.CAMERA)) }
    var micOk by remember { mutableStateOf(hasPerm(Manifest.permission.RECORD_AUDIO)) }
    var batteryOk by remember { mutableStateOf(SystemController.isIgnoringBatteryOptimizations(context)) }
    var overlayOk by remember { mutableStateOf(SystemController.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                adminActive = SystemController.isAdminActive(context)
                brightnessOk = SystemController.canWriteSettings(context)
                fileOk = SystemController.hasAllFilesAccess(context)
                cameraOk = hasPerm(Manifest.permission.CAMERA)
                micOk = hasPerm(Manifest.permission.RECORD_AUDIO)
                batteryOk = SystemController.isIgnoringBatteryOptimizations(context)
                overlayOk = SystemController.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraOk = granted }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micOk = granted }
    // Android < 11: Sound-Ordner braucht die klassische Storage-Laufzeitberechtigung.
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> fileOk = granted }

    // WICHTIG: aus der Activity OHNE FLAG_ACTIVITY_NEW_TASK starten – sonst bricht
    // der Geräteadmin-Dialog (der ein Ergebnis erwartet) sofort ab und kehrt zurück.
    val activity = context.findActivity()
    fun launch(i: Intent): Boolean = runCatching {
        if (activity != null) activity.startActivity(i)
        else context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    fun open(intent: Intent, fallback: Intent? = null) {
        if (!launch(intent)) {
            val fbOk = fallback != null && launch(fallback)
            if (!fbOk) Toast.makeText(context, s.openFailed, Toast.LENGTH_SHORT).show()
        }
    }

    SectionCard(s.permissionsTitle) {
        OutlinedButton(
            onClick = {
                open(
                    SystemController.deviceAdminIntent(context),
                    fallback = SystemController.securitySettingsIntent()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (adminActive) s.adminActive else s.enableDeviceAdmin) }
        OutlinedButton(
            onClick = {
                // Nicht erteilt → Laufzeit-Abfrage; erteilt → zur App-Info (prüfen/entziehen).
                if (cameraOk) open(SystemController.appDetailsIntent(context))
                else cameraLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (cameraOk) s.cameraActive else s.allowCamera) }
        OutlinedButton(
            onClick = {
                if (micOk) open(SystemController.appDetailsIntent(context))
                else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (micOk) s.micActive else s.allowMic) }
        OutlinedButton(
            onClick = { open(SystemController.writeSettingsIntent(context)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (brightnessOk) s.brightnessActive else s.allowBrightness) }
        OutlinedButton(
            onClick = {
                // Ab Android 11: „Alle Dateien"-Systemseite. Darunter (Android 9/10):
                // klassische Laufzeit-Abfrage, sonst App-Info zum Prüfen/Entziehen.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    open(SystemController.allFilesAccessIntent(context))
                } else if (!fileOk) {
                    storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    open(SystemController.appDetailsIntent(context))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (fileOk) s.fileAccessActive else s.allowFileAccess) }
        OutlinedButton(
            onClick = { open(SystemController.batteryOptIntent(context)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (batteryOk) s.batteryOptActive else s.allowBatteryOpt) }
        OutlinedButton(
            onClick = { open(SystemController.overlaySettingsIntent(context)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (overlayOk) s.overlayActive else s.allowOverlay) }
    }
}

// ---- Bausteine ------------------------------------------------------------------

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    max: Int = 100,
    suffix: String = "",
    zeroLabel: String? = null,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (value == 0 && zeroLabel != null) zeroLabel else "$value$suffix",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..max.toFloat()
        )
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
