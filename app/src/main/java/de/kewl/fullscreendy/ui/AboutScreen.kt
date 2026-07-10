package de.kewl.fullscreendy.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.kewl.fullscreendy.BuildConfig
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.device.DeviceInfo
import de.kewl.fullscreendy.i18n.LocalStrings
import de.kewl.fullscreendy.update.UpdateChecker
import de.kewl.fullscreendy.update.UpdateInfo
import de.kewl.fullscreendy.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(settings: Settings, onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ip = remember { DeviceInfo.ipv4() }

    var checking by remember { mutableStateOf(true) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }

    // Beim Öffnen auf neue Version prüfen (Dev-Builds inkl. Pre-Releases).
    LaunchedEffect(Unit) {
        update = withContext(Dispatchers.IO) {
            UpdateChecker.check(DeviceInfo.appVersion, includePrereleases = BuildConfig.DEV)
        }
        checking = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(title = s.about, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("FullScreendy", style = MaterialTheme.typography.headlineSmall)
                InfoRow(s.appVersionLabel, DeviceInfo.appVersion)
                InfoRow(s.androidVersionLabel, DeviceInfo.androidVersion)
                InfoRow(s.ipAddressLabel, ip)
                InfoRow(s.deviceIdLabel, settings.deviceId)
                InfoRow(s.urlLabel, settings.dashboardUrl.ifBlank { "—" })

                HorizontalDivider()

                // --- Update ---
                val info = update
                when {
                    checking -> Text(s.updateChecking, style = MaterialTheme.typography.bodyMedium)
                    info == null -> Text(s.updateUpToDate, style = MaterialTheme.typography.bodyMedium)
                    else -> {
                        Text(
                            "${s.updateAvailable} ${info.version}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                if (!Updater.canInstall(context)) {
                                    runCatching {
                                        context.startActivity(
                                            Updater.unknownSourcesIntent(context)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                    Toast.makeText(context, s.updateAllowInstall, Toast.LENGTH_LONG).show()
                                } else {
                                    scope.launch {
                                        downloading = true
                                        val file = withContext(Dispatchers.IO) {
                                            Updater.download(context, info.apkUrl)
                                        }
                                        downloading = false
                                        if (file != null) Updater.install(context, file)
                                        else Toast.makeText(context, s.updateError, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !downloading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (downloading) s.updateDownloading else s.updateInstall)
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    s.githubRepo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                )
                Text(REPO_URL, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private const val REPO_URL = "https://github.com/Glenn-Dandy/fullscreendy"

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
