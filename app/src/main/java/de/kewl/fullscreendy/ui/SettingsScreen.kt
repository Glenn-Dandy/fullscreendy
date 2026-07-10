package de.kewl.fullscreendy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
import androidx.core.content.ContextCompat
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.device.SystemController
import de.kewl.fullscreendy.i18n.LocalStrings
import de.kewl.fullscreendy.i18n.Strings
import de.kewl.fullscreendy.kiosk.KioskStatus
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

private enum class Section { Home, Connection, Display, Behavior, Sounds, System }

@Composable
fun SettingsScreen(
    initial: Settings,
    onPersist: (Settings) -> Unit,
    onExit: () -> Unit,
) {
    val s = LocalStrings.current
    var draft by remember { mutableStateOf(initial) }
    var section by remember { mutableStateOf(Section.Home) }

    // Auto-Speichern: ~1 s nach der letzten Änderung persistieren (kein Save-Button).
    LaunchedEffect(draft) {
        delay(1000)
        onPersist(draft)
    }

    val title = when (section) {
        Section.Home -> s.settings
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (section) {
                    Section.Home -> {
                        CategoryRow(s.secConnection, s.secConnectionDesc) { section = Section.Connection }
                        CategoryRow(s.secDisplay, s.secDisplayDesc) { section = Section.Display }
                        CategoryRow(s.secBehavior, s.secBehaviorDesc) { section = Section.Behavior }
                        CategoryRow(s.secSounds, s.secSoundsDesc) { section = Section.Sounds }
                        CategoryRow(s.secSystem, s.secSystemDesc) { section = Section.System }
                    }
                    Section.Connection -> ConnectionSection(draft, s) { draft = it }
                    Section.Display -> DisplaySection(draft, s) { draft = it }
                    Section.Behavior -> BehaviorSection(draft, s) { draft = it }
                    Section.Sounds -> SoundsSection(s)
                    Section.System -> SystemSection(draft, s) { draft = it }
                }
            }
        }
    }
}

@Composable
private fun ConnectionSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    var portText by remember { mutableStateOf(draft.mqttPort.toString()) }

    OutlinedTextField(
        value = draft.dashboardUrl,
        onValueChange = { onChange(draft.copy(dashboardUrl = it)) },
        label = { Text(s.dashboardUrl) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    HorizontalDivider()
    Text(s.mqttBroker, style = MaterialTheme.typography.titleMedium)
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
    HorizontalDivider()
    Text(s.topics, style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun DisplaySection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    SwitchRow(s.ignoreFontScale, draft.ignoreSystemFontScale) {
        onChange(draft.copy(ignoreSystemFontScale = it))
    }
    SwitchRow(s.allowZoom, draft.zoomEnabled) { onChange(draft.copy(zoomEnabled = it)) }
    SwitchRow(s.keepScreenOn, draft.keepScreenOn) { onChange(draft.copy(keepScreenOn = it)) }
    HorizontalDivider()
    Text(s.dimTimeout, style = MaterialTheme.typography.titleMedium)
    Text(s.dimTimeoutHint, style = MaterialTheme.typography.bodySmall)
    SliderRow(
        label = s.dimAfter,
        value = draft.dimTimeoutSecs,
        max = 300,
        suffix = " s",
        zeroLabel = s.off
    ) { onChange(draft.copy(dimTimeoutSecs = it)) }

    HorizontalDivider()
    Text(s.screenOff, style = MaterialTheme.typography.titleMedium)
    Text(s.screenOffHint, style = MaterialTheme.typography.bodySmall)
    SliderRow(
        label = s.offAfter,
        value = draft.screenOffSecs,
        max = 600,
        suffix = " s",
        zeroLabel = s.off
    ) { onChange(draft.copy(screenOffSecs = it)) }
}

@Composable
private fun BehaviorSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    SwitchRow(s.motionDetection, draft.motionEnabled) { onChange(draft.copy(motionEnabled = it)) }
    if (draft.motionEnabled) {
        SwitchRow(s.motionWakesScreen, draft.motionWakesScreen) {
            onChange(draft.copy(motionWakesScreen = it))
        }
        SliderRow(s.motionSensitivity, draft.motionSensitivity) {
            onChange(draft.copy(motionSensitivity = it))
        }
    }
    SwitchRow(s.soundWake, draft.soundWakeEnabled) { onChange(draft.copy(soundWakeEnabled = it)) }
    if (draft.soundWakeEnabled) {
        SliderRow(s.soundSensitivity, draft.soundSensitivity) {
            onChange(draft.copy(soundSensitivity = it))
        }
    }
    SwitchRow(s.pullToRefresh, draft.pullToRefresh) { onChange(draft.copy(pullToRefresh = it)) }
    SwitchRow(s.ttsEnabled, draft.ttsEnabled) { onChange(draft.copy(ttsEnabled = it)) }
    SwitchRow(s.mediaEnabled, draft.mediaEnabled) { onChange(draft.copy(mediaEnabled = it)) }

    if (draft.motionEnabled || draft.soundWakeEnabled) {
        HorizontalDivider()
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

@Composable
private fun IndicatorRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SoundsSection(s: Strings) {
    val soundsPath = remember {
        @Suppress("DEPRECATION")
        File(Environment.getExternalStorageDirectory(), "FullScreendy").absolutePath
    }
    Text(s.soundsHint, style = MaterialTheme.typography.bodyMedium)
    Text(soundsPath, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SystemSection(draft: Settings, s: Strings, onChange: (Settings) -> Unit) {
    Text(s.language, style = MaterialTheme.typography.titleMedium)
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
    HorizontalDivider()
    SwitchRow(s.startOnBoot, draft.startOnBoot) { onChange(draft.copy(startOnBoot = it)) }
    OutlinedTextField(
        value = draft.adminPin,
        onValueChange = { onChange(draft.copy(adminPin = it.filter(Char::isDigit).take(8))) },
        label = { Text(s.adminPin) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )

    HorizontalDivider()
    Text(s.permissionsTitle, style = MaterialTheme.typography.titleMedium)
    val context = LocalContext.current

    fun hasPerm(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    // Status aller Berechtigungen – wird bei Rückkehr in die App aktualisiert.
    var adminActive by remember { mutableStateOf(SystemController.isAdminActive(context)) }
    var brightnessOk by remember { mutableStateOf(SystemController.canWriteSettings(context)) }
    var fileOk by remember { mutableStateOf(SystemController.hasAllFilesAccess()) }
    var cameraOk by remember { mutableStateOf(hasPerm(Manifest.permission.CAMERA)) }
    var micOk by remember { mutableStateOf(hasPerm(Manifest.permission.RECORD_AUDIO)) }
    var batteryOk by remember { mutableStateOf(SystemController.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                adminActive = SystemController.isAdminActive(context)
                brightnessOk = SystemController.canWriteSettings(context)
                fileOk = SystemController.hasAllFilesAccess()
                cameraOk = hasPerm(Manifest.permission.CAMERA)
                micOk = hasPerm(Manifest.permission.RECORD_AUDIO)
                batteryOk = SystemController.isIgnoringBatteryOptimizations(context)
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
        onClick = { open(SystemController.allFilesAccessIntent(context)) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (fileOk) s.fileAccessActive else s.allowFileAccess) }
    OutlinedButton(
        onClick = { open(SystemController.batteryOptIntent(context)) },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (batteryOk) s.batteryOptActive else s.allowBatteryOpt) }
}

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

@Composable
private fun CategoryRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
