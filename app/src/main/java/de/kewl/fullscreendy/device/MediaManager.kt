package de.kewl.fullscreendy.device

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Spielt lokal gespeicherte Tondateien (oder Stream-URLs) im Hintergrund ab –
 * unabhängig davon, was die WebView gerade anzeigt.
 *
 * Die Dateien liegen in einem Ordner, den der Nutzer über die Android-Ordnerauswahl
 * (Storage Access Framework) freigibt. Die App braucht dafür **keine**
 * Speicher-Berechtigung: Sie bekommt genau diesen einen Ordner und sonst nichts.
 * Ohne Auswahl wird der app-eigene Ordner verwendet.
 *
 * Payload-Auflösung:
 *  - beginnt mit "http"      → Stream-URL
 *  - beginnt mit "content://" → Dokument-URI
 *  - sonst                    → Datei im freigegebenen Ordner, sonst im App-Ordner;
 *                               Unterordner mit "/" (z. B. "klingel/ding.mp3")
 */
class MediaManager(private val context: Context) {

    private var player: MediaPlayer? = null

    /** Vom Nutzer freigegebener Ordner; wird vom Dienst aus den Einstellungen gesetzt. */
    @Volatile
    var soundsTreeUri: String = ""

    /** App-eigener Ordner als Rückfallebene – ohne jede Berechtigung les- und beschreibbar. */
    val fallbackDir: File
        get() = (context.getExternalFilesDir("sounds") ?: File(context.filesDir, "sounds"))
            .also { it.mkdirs() }

    /** Sucht [name] (ggf. mit Unterordnern) im freigegebenen Ordner. */
    private fun findInTree(name: String): Uri? {
        val tree = soundsTreeUri.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            var node = DocumentFile.fromTreeUri(context, Uri.parse(tree)) ?: return null
            val parts = name.split('/').filter { it.isNotBlank() }
            for ((i, part) in parts.withIndex()) {
                val child = node.findFile(part) ?: return null
                if (i < parts.lastIndex && !child.isDirectory) return null
                node = child
            }
            if (node.isFile) node.uri else null
        }.getOrNull()
    }

    fun play(spec: String) {
        val src = spec.trim()
        if (src.isEmpty()) return

        stop()
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                when {
                    src.startsWith("http", ignoreCase = true) -> setDataSource(src)
                    src.startsWith("content://") -> setDataSource(context, Uri.parse(src))
                    else -> {
                        val fromTree = findInTree(src)
                        val local = File(fallbackDir, src)
                        when {
                            fromTree != null -> setDataSource(context, fromTree)
                            local.isFile -> setDataSource(local.absolutePath)
                            else -> {
                                Log.w(TAG, "Tondatei nicht gefunden: $src")
                                release(); player = null
                                return
                            }
                        }
                    }
                }
                setOnCompletionListener { it.release(); if (player === it) player = null }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer-Fehler ($what/$extra)")
                    mp.release(); if (player === mp) player = null; true
                }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }.onFailure { Log.e(TAG, "Abspielen fehlgeschlagen: $src", it) }
    }

    fun stop() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
    }

    companion object {
        private const val TAG = "MediaManager"
    }
}
