package de.kewl.fullscreendy.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import de.kewl.fullscreendy.BuildConfig
import de.kewl.fullscreendy.data.Settings
import de.kewl.fullscreendy.device.DeviceInfo
import de.kewl.fullscreendy.i18n.LocalStrings
import de.kewl.fullscreendy.update.Repo
import de.kewl.fullscreendy.update.UpdateChecker
import de.kewl.fullscreendy.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Zustand der Update-Prüfung inkl. Download-Rückmeldung. */
private sealed interface UpdateUi {
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val info: UpdateInfo) : UpdateUi
    data object Failed : UpdateUi
}

@Composable
fun AboutScreen(settings: Settings, onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ip = remember { DeviceInfo.ipv4() }

    var state by remember { mutableStateOf<UpdateUi>(UpdateUi.Checking) }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    suspend fun check() {
        state = UpdateUi.Checking
        // Dev-Builds prüfen auch Pre-Releases mit.
        val info = withContext(Dispatchers.IO) {
            UpdateChecker.check(DeviceInfo.appVersion, includePrereleases = BuildConfig.DEV)
        }
        state = if (info != null) UpdateUi.Available(info) else UpdateUi.UpToDate
    }

    // Beim Öffnen einmal automatisch prüfen.
    LaunchedEffect(Unit) { check() }


    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            TitleBar(title = s.about, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppHeader()

                SectionCard(s.aboutDevice) {
                    InfoRow(s.appVersionLabel, DeviceInfo.appVersion)
                    InfoRow(s.androidVersionLabel, DeviceInfo.androidVersion)
                    InfoRow(s.ipAddressLabel, ip)
                    InfoRow(s.deviceIdLabel, settings.deviceId)
                }

                SectionCard(s.aboutUpdate) {
                    when (val st = state) {
                        UpdateUi.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(s.updateChecking, style = MaterialTheme.typography.bodyLarge)
                        }

                        UpdateUi.UpToDate -> {
                            Text(
                                "${s.updateUpToDate} (${DeviceInfo.appVersion})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            RowDivider()
                            ActionRow(Icons.Filled.Refresh, s.updateCheckAgain, showArrow = false) {
                                scope.launch { check() }
                            }
                        }

                        UpdateUi.Failed -> {
                            Text(
                                s.updateError,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            RowDivider()
                            ActionRow(Icons.Filled.Refresh, s.updateCheckAgain, showArrow = false) {
                                scope.launch { check() }
                            }
                        }

                        is UpdateUi.Available -> {
                            Text(
                                "${s.updateAvailable} ${st.info.version}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            RowDivider()
                            // Bewusst kein Download in der App: Das Installieren fremder
                            // APKs braucht REQUEST_INSTALL_PACKAGES, und F-Droid nimmt
                            // keine Apps, die sich selbst aktualisieren.
                            ActionRow(Icons.AutoMirrored.Filled.OpenInNew, s.updateOpenPage) {
                                openUrl("${Repo.URL}/releases")
                            }
                        }

                    }
                }

                SectionCard(s.aboutProject) {
                    ActionRow(Icons.Filled.Star, s.starGithub) { openUrl(Repo.URL) }
                    RowDivider()
                    ActionRow(Icons.Filled.Code, s.viewSource) { openUrl(Repo.URL) }
                    RowDivider()
                    ActionRow(
                        Icons.Filled.Favorite,
                        s.supportProject,
                        tint = MaterialTheme.colorScheme.error
                    ) { openUrl(Repo.SUPPORT_URL) }
                }

                Text(
                    "${s.licenseLine} · © ${Repo.OWNER}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

/** App-Icon, Name und Version als Kopf der Seite. */
@Composable
private fun AppHeader() {
    val s = LocalStrings.current
    val context = LocalContext.current
    val icon = remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(width = 144, height = 144).asImageBitmap()
        }.getOrNull()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Dashboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(
            "FullScreendy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${s.version} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
