package de.kewl.fullscreendy.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import de.kewl.fullscreendy.AdminReceiver

/**
 * Systemnahe Aktionen: Bildschirm sperren (Geräteadmin) und echte Hardware-
 * Helligkeit setzen (Settings.System, benötigt WRITE_SETTINGS).
 */
object SystemController {

    private fun dpm(ctx: Context): DevicePolicyManager? =
        ctx.getSystemService(DevicePolicyManager::class.java)

    private fun admin(ctx: Context) = ComponentName(ctx, AdminReceiver::class.java)

    fun isAdminActive(ctx: Context): Boolean =
        dpm(ctx)?.isAdminActive(admin(ctx)) == true

    /** Sperrt sofort. Erfordert aktiven Geräteadministrator. */
    fun lock(ctx: Context): Boolean {
        val d = dpm(ctx) ?: return false
        if (!d.isAdminActive(admin(ctx))) return false
        return runCatching { d.lockNow() }.isSuccess
    }

    fun canWriteSettings(ctx: Context): Boolean = Settings.System.canWrite(ctx)

    // ---- Medien-Lautstärke (STREAM_MUSIC – betrifft TTS und Töne) --------------

    private fun audio(ctx: Context) = ctx.getSystemService(AudioManager::class.java)

    /** Aktuelle Medienlautstärke in Prozent (0..100). */
    fun getVolumePercent(ctx: Context): Int {
        val am = audio(ctx) ?: return 0
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (cur * 100 / max) else 0
    }

    /** Setzt die Medienlautstärke (0..100) und gibt den tatsächlichen Prozentwert zurück. */
    fun setVolumePercent(ctx: Context, percent: Int): Int {
        val am = audio(ctx) ?: return 0
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (percent.coerceIn(0, 100) * max + 50) / 100 // gerundet
        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, target.coerceIn(0, max), 0) }
        return getVolumePercent(ctx)
    }

    /** Vibriert für [ms] Millisekunden (1..5000). Für haptisches Feedback per MQTT. */
    @Suppress("DEPRECATION")
    fun vibrate(ctx: Context, ms: Long) {
        val duration = ms.coerceIn(1, 5000)
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        } ?: return
        runCatching {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Setzt die echte Geräte-Helligkeit (0..100 → 0..255). true bei Erfolg. */
    fun setSystemBrightness(ctx: Context, percent: Int): Boolean {
        if (!Settings.System.canWrite(ctx)) return false
        val value = percent.coerceIn(0, 100) * 255 / 100
        return runCatching {
            Settings.System.putInt(
                ctx.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }.isSuccess
    }

    // ---- Intents, um die nötigen Berechtigungen zu erteilen --------------------

    fun writeSettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /** Fallback, falls der direkte Geräteadmin-Dialog nicht verfügbar ist. */
    fun securitySettingsIntent(): Intent = Intent(Settings.ACTION_SECURITY_SETTINGS)

    /** App-Info-Seite (dort Kamera/Mikrofon prüfen/entziehen). */
    fun appDetailsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /** Ist die App von der Akku-Optimierung ausgenommen? (wichtig für 24/7-Betrieb) */
    fun isIgnoringBatteryOptimizations(ctx: Context): Boolean =
        ctx.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(ctx.packageName) == true

    @Suppress("BatteryLife")
    fun batteryOptIntent(ctx: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${ctx.packageName}")
        )

    /** true, wenn die App beliebige Dateien lesen darf (für den Sound-Ordner). */
    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true // < Android 11: klassische Speicherberechtigung genügt

    fun allFilesAccessIntent(ctx: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${ctx.packageName}")
            )
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${ctx.packageName}")
            )
        }

    /** Darf die App über anderen Apps zeichnen? (nötig, um die UI nach Kill neu zu holen) */
    fun canDrawOverlays(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(ctx) else true

    fun overlaySettingsIntent(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))

    fun deviceAdminIntent(ctx: Context): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin(ctx))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Für 'Bildschirm sperren' per MQTT."
            )
}
