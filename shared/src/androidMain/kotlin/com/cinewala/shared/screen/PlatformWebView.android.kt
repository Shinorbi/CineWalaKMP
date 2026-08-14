package com.cinewala.shared.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * JavaScript injected into the WebView to capture video progress.
 *
 * Strategy:
 *  1. Listen for `postMessage` events (handles object or JSON-string payloads,
 *     unwraps {type:'progress', ...} wrappers, and normalizes % to a fraction).
 *  2. Fall back to polling the <video> element, which works with any player
 *     that uses a standard HTML5 video tag, regardless of its messaging.
 */
private const val PROGRESS_BRIDGE_JS = """
    (function() {
        var lastTime = -10;

        function extractProgress(data) {
            if (typeof data === 'string') {
                try { data = JSON.parse(data); } catch (e) { return null; }
            }
            if (!data || typeof data !== 'object') return null;

            // Unwrap {type:'progress', data:{...}} style messages
            var payload = data;
            if (payload.data && typeof payload.data === 'object') {
                payload = payload.data;
            }
            // Skip messages that are not progress-related
            if (payload.type && payload.type !== 'progress' &&
                payload.type !== 'watchProgress' && payload.type !== 'timeupdate') {
                return null;
            }

            var progress = payload.progress;
            var duration = payload.duration;
            var currentTime = payload.currentTime;
            if (progress === undefined && duration === undefined && currentTime === undefined) {
                return null;
            }

            // normalize: percentage (0-100) -> fraction (0-1)
            var p = Number(progress) || 0;
            if (p > 1) p = p / 100;

            return {
                progress: p,
                duration: Number(duration) || 0,
                currentTime: Number(currentTime) || 0
            };
        }

        function sendToNative(progress, duration, currentTime) {
            try {
                if (window.AndroidBridge) {
                    window.AndroidBridge.onProgress(
                        Number(progress) || 0,
                        Number(duration) || 0,
                        Number(currentTime) || 0
                    );
                }
            } catch (e) {}
        }

        function handleMessage(event) {
            try {
                var result = extractProgress(event.data);
                if (result) {
                    sendToNative(result.progress, result.duration, result.currentTime);
                }
            } catch (e) {}
        }
        window.addEventListener('message', handleMessage);
        document.addEventListener('message', handleMessage);

        // Fallback: poll the video element every 5 seconds
        setInterval(function() {
            try {
                var video = document.querySelector('video');
                if (video && video.duration > 0 && !isNaN(video.currentTime)) {
                    var t = Math.floor(video.currentTime);
                    var d = Math.floor(video.duration);
                    var p = d > 0 ? (t / d) : 0;
                    // Only send when position changed by >= 5s
                    if (Math.abs(t - lastTime) >= 5) {
                        lastTime = t;
                        sendToNative(p, d, t);
                    }
                }
            } catch (e) {}
        }, 5000);
    })();
"""

/**
 * JavaScript used to pause any playing media and stop the progress polling
 * before the WebView is released. This prevents audio/video from continuing
 * to play in the background after the player screen is closed.
 */
private const val STOP_MEDIA_JS = """
    (function() {
        try {
            var video = document.querySelector('video');
            if (video) {
                video.pause();
                video.removeAttribute('src');
                video.load();
            }
        } catch (e) {}
    })();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(
    url: String,
    modifier: Modifier,
    initialProgressSeconds: Long,
    onProgressUpdate: (PlayerProgress) -> Unit
) {
    val loading = remember { mutableStateOf(false) }

    // Append progress param to the URL if provided
    val finalUrl = remember(url, initialProgressSeconds) {
        if (initialProgressSeconds > 0) {
            val separator = if (url.contains("?")) "&" else "?"
            "$url${separator}progress=$initialProgressSeconds"
        } else {
            url
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Bridge for receiving progress events from injected JS
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onProgress(progress: Double, duration: Double, currentTime: Double) {
                        onProgressUpdate(
                            PlayerProgress(
                                progress = progress,
                                duration = duration.toLong(),
                                currentTime = currentTime.toLong()
                            )
                        )
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?
                    ) {
                        loading.value = true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        loading.value = false
                        // Inject the progress bridge after page load
                        view?.evaluateJavascript(PROGRESS_BRIDGE_JS, null)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                loadUrl(finalUrl)
            }
        },
        update = { webView ->
            if (webView.url != finalUrl) {
                webView.loadUrl(finalUrl)
            }
        },
        onRelease = { webView ->
            // Pause any playing media and stop the progress polling before release.
            webView.evaluateJavascript(STOP_MEDIA_JS, null)
            webView.stopLoading()
            // Unload the page to release media/audio resources.
            webView.loadUrl("about:blank")
            // Fully destroy the WebView so no background playback continues.
            webView.destroy()
        }
    )
}