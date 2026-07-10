package de.kewl.fullscreendy.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Lädt die Update-APK herunter und stößt die Installation an. */
object Updater {
    private const val TAG = "Updater"

    /** Darf die App APKs installieren? (Android „Unbekannte Apps installieren"). */
    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun unknownSourcesIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))

    /** Lädt die APK nach externalFilesDir/update.apk. Nur aus IO-Kontext aufrufen. */
    fun download(ctx: Context, url: String): File? {
        return runCatching {
            val dir = ctx.getExternalFilesDir(null) ?: ctx.cacheDir
            val out = File(dir, "update.apk")
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "FullScreendy")
            }
            try {
                conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
            } finally {
                conn.disconnect()
            }
            out
        }.onFailure { Log.w(TAG, "Download fehlgeschlagen", it) }.getOrNull()
    }

    fun install(ctx: Context, file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }.onFailure { Log.w(TAG, "Install-Intent fehlgeschlagen", it) }
    }
}
