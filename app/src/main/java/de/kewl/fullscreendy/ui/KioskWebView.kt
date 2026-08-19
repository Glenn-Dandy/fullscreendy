package de.kewl.fullscreendy.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.roundToInt

/** Login/Zertifikats-Einstellungen des gerade angezeigten Dashboards. */
data class DashboardAuth(
    val user: String = "",
    val pass: String = "",
    val allowInvalidCerts: Boolean = false,
)

/** Hält eine Referenz auf die aktive WebView, damit Befehle sie steuern können. */
class WebController {
    var webView: WebView? = null
        internal set

    /**
     * Wird beim Dashboard-Wechsel aktualisiert und vom WebViewClient bei jeder
     * Anfrage frisch gelesen – so muss die WebView dafür nicht neu gebaut werden.
     */
    var auth: DashboardAuth = DashboardAuth()
        internal set

    fun reload() {
        webView?.post { webView?.reload() }
    }

    fun clearCache() {
        webView?.post { webView?.clearCache(true) }
    }
}

@Composable
fun rememberWebController(): WebController = remember { WebController() }

/**
 * SwipeRefresh, das nur auslöst, wenn die Zieh-Geste am Seitenanfang *beginnt*.
 *
 * Das Standardverhalten arbeitet mitten in der Geste: wer weit unten ist und nach
 * oben scrollt, löst beim Erreichen des Seitenanfangs sofort den Reload aus. Hier
 * wird beim Aufsetzen des Fingers einmal entschieden ("scharf gestellt") und die
 * Geste sonst komplett durchgereicht. Hochscrollen stoppt also am Seitenanfang;
 * erst ein neuer Zug von oben nach unten bringt den Lade-Kreis.
 */
private class TopPullRefreshLayout(context: Context) : SwipeRefreshLayout(context) {
    var target: WebView? = null

    private var armed = false

    private fun atTop(): Boolean = (target?.scrollY ?: 0) <= 0

    override fun canChildScrollUp(): Boolean = !atTop()

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) armed = atTop() && !isRefreshing
        if (!armed) return false
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!armed) return false
        return super.onTouchEvent(ev)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun KioskWebView(
    url: String,
    controller: WebController,
    ignoreSystemFontScale: Boolean,
    zoomEnabled: Boolean,
    pullToRefresh: Boolean,
    auth: DashboardAuth = DashboardAuth(),
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val web = WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView, request: WebResourceRequest
                    ): Boolean = false

                    override fun onPageFinished(view: WebView, url: String) {
                        (view.parent as? SwipeRefreshLayout)?.isRefreshing = false
                    }

                    // FHEM/Dashboard-Login per HTTP Basic Auth: sauberer als
                    // user:pass in der URL (die sonst im Klartext im MQTT-Reading landet).
                    // Werte kommen aus dem Controller, damit ein Dashboard-Wechsel
                    // ohne Neuaufbau der WebView greift.
                    override fun onReceivedHttpAuthRequest(
                        view: WebView,
                        handler: android.webkit.HttpAuthHandler,
                        host: String?,
                        realm: String?
                    ) {
                        val a = controller.auth
                        if (a.user.isNotBlank()) handler.proceed(a.user, a.pass)
                        else handler.cancel()
                    }

                    // Selbst-signierte/ungültige HTTPS-Zertifikate (z. B. FHEM lokal):
                    // nur laden, wenn der Nutzer es für dieses Dashboard bewusst erlaubt
                    // hat. Sonst Standard (Abbruch) – ein ungeprüftes proceed() wäre ein
                    // MITM-Risiko.
                    override fun onReceivedSslError(
                        view: WebView,
                        handler: android.webkit.SslErrorHandler,
                        error: android.net.http.SslError
                    ) {
                        if (controller.auth.allowInvalidCerts) handler.proceed() else handler.cancel()
                    }
                }
                webChromeClient = WebChromeClient()
                // Schwarz statt Weiß während Lade-/Neuzeichnen-Lücken (kein weißer Blitz).
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(zoomEnabled)
                    builtInZoomControls = zoomEnabled
                    displayZoomControls = false
                    // Schrift unabhängig vom System-Zoom: fontScale rechnerisch ausgleichen.
                    textZoom = if (ignoreSystemFontScale) {
                        val fs = ctx.resources.configuration.fontScale
                        if (fs > 0f) (100f / fs).roundToInt() else 100
                    } else 100
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                controller.webView = this
            }

            TopPullRefreshLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(web)
                target = web
                isEnabled = pullToRefresh
                // Deutlich weiter ziehen als der 64-dp-Standard: kein Reload aus Versehen.
                setDistanceToTriggerSync((PULL_TRIGGER_DP * ctx.resources.displayMetrics.density).roundToInt())
                // Lade-Kreis sichtbar auf dunklem Dashboard-Hintergrund.
                setProgressBackgroundColorSchemeColor(0xFF202124.toInt())
                setColorSchemeColors(0xFF4C8DF6.toInt())
                setOnRefreshListener { web.reload() }
            }
        },
        update = { refresh ->
            refresh.isEnabled = pullToRefresh
            controller.auth = auth
            // WebView aus dem Controller nehmen (getChildAt(0) wäre der Lade-Kreis).
            controller.webView?.let { web ->
                if (web.tag != url) {
                    web.tag = url
                    web.loadUrl(url)
                }
            }
        }
    )
}

/** Zieh-Weg bis zum Reload (Standard von SwipeRefreshLayout wären 64 dp). */
private const val PULL_TRIGGER_DP = 140f
