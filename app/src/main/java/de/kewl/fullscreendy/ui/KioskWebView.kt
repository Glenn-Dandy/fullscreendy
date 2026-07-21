package de.kewl.fullscreendy.ui

import android.annotation.SuppressLint
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

/** Hält eine Referenz auf die aktive WebView, damit Befehle sie steuern können. */
class WebController {
    var webView: WebView? = null
        internal set

    fun load(url: String) {
        if (url.isNotBlank()) webView?.post { webView?.loadUrl(url) }
    }

    fun reload() {
        webView?.post { webView?.reload() }
    }

    fun clearCache() {
        webView?.post { webView?.clearCache(true) }
    }
}

@Composable
fun rememberWebController(): WebController = remember { WebController() }

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KioskWebView(
    url: String,
    controller: WebController,
    ignoreSystemFontScale: Boolean,
    zoomEnabled: Boolean,
    pullToRefresh: Boolean,
    authUser: String = "",
    authPass: String = "",
    allowInvalidCerts: Boolean = false,
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
                    override fun onReceivedHttpAuthRequest(
                        view: WebView,
                        handler: android.webkit.HttpAuthHandler,
                        host: String?,
                        realm: String?
                    ) {
                        if (authUser.isNotBlank()) handler.proceed(authUser, authPass)
                        else handler.cancel()
                    }

                    // Selbst-signierte/ungültige HTTPS-Zertifikate (z. B. FHEM lokal):
                    // nur laden, wenn der Nutzer es bewusst erlaubt hat. Sonst Standard
                    // (Abbruch) – ein ungeprüftes proceed() wäre ein MITM-Risiko.
                    override fun onReceivedSslError(
                        view: WebView,
                        handler: android.webkit.SslErrorHandler,
                        error: android.net.http.SslError
                    ) {
                        if (allowInvalidCerts) handler.proceed() else handler.cancel()
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

            SwipeRefreshLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(web)
                isEnabled = pullToRefresh
                setOnRefreshListener { web.reload() }
            }
        },
        update = { refresh ->
            refresh.isEnabled = pullToRefresh
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
