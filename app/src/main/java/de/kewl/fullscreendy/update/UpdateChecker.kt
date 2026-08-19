package de.kewl.fullscreendy.update

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val apkUrl: String)

/** Feste Projekt-Adressen (GitHub, Unterstützen). */
object Repo {
    const val OWNER = "Glenn-Dandy"
    const val URL = "https://github.com/Glenn-Dandy/fullscreendy"
    const val SUPPORT_URL = "https://paypal.me/GlennDandy"
}

/**
 * Prüft die GitHub-Releases auf eine neuere Version. Für Dev-Builds werden auch
 * Pre-Releases berücksichtigt, für stabile Builds nur das „latest"-Release.
 * Netzwerkzugriff – nur aus einem IO-Kontext aufrufen.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val API = "https://api.github.com/repos/Glenn-Dandy/fullscreendy/releases"

    fun check(currentVersion: String, includePrereleases: Boolean): UpdateInfo? {
        return runCatching {
            val release: JSONObject = if (includePrereleases) {
                val body = fetch("$API?per_page=30") ?: return null
                val arr = JSONArray(body)
                var best: JSONObject? = null
                var bestV: List<Int>? = null
                for (i in 0 until arr.length()) {
                    val rel = arr.getJSONObject(i)
                    if (rel.optBoolean("draft")) continue
                    val v = parse(rel.optString("tag_name"))
                    if (bestV == null || greater(v, bestV!!)) { bestV = v; best = rel }
                }
                best ?: return null
            } else {
                val body = fetch("$API/latest") ?: return null
                JSONObject(body)
            }

            val tag = release.optString("tag_name")
            if (!greater(parse(tag), parse(currentVersion))) return null
            val apk = apkAsset(release) ?: return null
            UpdateInfo(tag.removePrefix("v"), apk)
        }.onFailure { Log.w(TAG, "Update-Prüfung fehlgeschlagen", it) }.getOrNull()
    }

    private fun apkAsset(rel: JSONObject): String? {
        val assets = rel.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                return a.optString("browser_download_url")
            }
        }
        return null
    }

    private fun fetch(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FullScreendy")
        }
        return try {
            if (conn.responseCode != 200) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Version -> (major, minor, patch, devN). Stabil hat devN = MAX (steht "nach" allen dev). */
    private fun parse(v: String): List<Int> {
        val s = v.removePrefix("v").trim()
        val idx = s.indexOf("-dev.")
        val core = if (idx >= 0) s.substring(0, idx) else s
        val dev = if (idx >= 0) s.substring(idx + 5).toIntOrNull() ?: 0 else Int.MAX_VALUE
        val p = core.split(".")
        return listOf(
            p.getOrNull(0)?.toIntOrNull() ?: 0,
            p.getOrNull(1)?.toIntOrNull() ?: 0,
            p.getOrNull(2)?.toIntOrNull() ?: 0,
            dev
        )
    }

    private fun greater(a: List<Int>, b: List<Int>): Boolean {
        for (i in 0 until 4) if (a[i] != b[i]) return a[i] > b[i]
        return false
    }
}
