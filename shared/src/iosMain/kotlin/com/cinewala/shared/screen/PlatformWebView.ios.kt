package com.cinewala.shared.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

/**
 * JavaScript injected into the WKWebView to capture video progress.
 *
 * Strategy:
 *  1. Listen for `postMessage` events (object or JSON-string, unwraps wrappers,
 *     normalizes % to a fraction).
 *  2. Fall back to polling the <video> element every 5 seconds.
 */
private const val PROGRESS_BRIDGE_JS = """
    (function() {
        var lastTime = -10;

        function extractProgress(data) {
            if (typeof data === 'string') {
                try { data = JSON.parse(data); } catch (e) { return null; }
            }
            if (!data || typeof data !== 'object') return null;

            var payload = data;
            if (payload.data && typeof payload.data === 'object') {
                payload = payload.data;
            }
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
                if (window.webkit && window.webkit.messageHandlers &&
                    window.webkit.messageHandlers.progressHandler) {
                    window.webkit.messageHandlers.progressHandler.postMessage({
                        progress: Number(progress) || 0,
                        duration: Number(duration) || 0,
                        currentTime: Number(currentTime) || 0
                    });
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
 * JavaScript used to pause any playing media before the WKWebView is released.
 * This prevents audio/video from continuing to play in the background after
 * the player screen is closed.
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

/**
 * WKScriptMessageHandler that receives progress messages from the injected JS bridge.
 * In Kotlin/Native the message body is an NSDictionary, so we access its values
 * with NSString/NSNumber APIs rather than the Kotlin Map interface.
 */
private class ProgressMessageHandler(
    private val onProgressUpdate: (PlayerProgress) -> Unit
) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        try {
            val body = didReceiveScriptMessage.body
            if (body is NSDictionary) {
                val progressObj = body.objectForKey("progress") as? NSNumber
                val durationObj = body.objectForKey("duration") as? NSNumber
                val currentTimeObj = body.objectForKey("currentTime") as? NSNumber
                onProgressUpdate(
                    PlayerProgress(
                        progress = progressObj?.doubleValue ?: 0.0,
                        duration = durationObj?.longLongValue?.toLong() ?: 0L,
                        currentTime = currentTimeObj?.longLongValue?.toLong() ?: 0L
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore malformed messages; never crash on bad data
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(
    url: String,
    modifier: Modifier,
    initialProgressSeconds: Long,
    onProgressUpdate: (PlayerProgress) -> Unit
) {
    // Append progress param to the URL if provided
    val finalUrl = if (initialProgressSeconds > 0) {
        val separator = if (url.contains("?")) "&" else "?"
        "$url${separator}progress=$initialProgressSeconds"
    } else {
        url
    }

    // Retain the message handler strongly so WKWebView keeps it alive.
    // Without this, Kotlin/Native's memory manager may free it and the
    // process can be killed (SIGKILL / watchdog).
    val messageHandler = remember(onProgressUpdate) {
        ProgressMessageHandler(onProgressUpdate)
    }

    UIKitView(
        modifier = modifier,
        factory = {
            val contentController = WKUserContentController().apply {
                addScriptMessageHandler(
                    messageHandler,
                    name = "progressHandler"
                )
                addUserScript(
                    WKUserScript(
                        source = PROGRESS_BRIDGE_JS,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
                        forMainFrameOnly = true
                    )
                )
            }
            val config = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                mediaTypesRequiringUserActionForPlayback = 0u // allow autoplay
                this.userContentController = contentController
            }
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                allowsBackForwardNavigationGestures = true
                scrollView.bounces = false

                val nsUrl = NSURL.URLWithString(finalUrl)
                if (nsUrl != null) {
                    loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
            }
        },
        update = { webView ->
            val current = webView.URL?.absoluteString
            if (current != finalUrl) {
                val nsUrl = NSURL.URLWithString(finalUrl)
                if (nsUrl != null) {
                    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
            }
        },
        onRelease = { webView ->
            // Pause any playing media before releasing the WKWebView.
            webView.evaluateJavaScript(STOP_MEDIA_JS, completionHandler = null)
            webView.stopLoading()
            // Unload the page to release media/audio resources.
            webView.loadHTMLString("", baseURL = null)
            // Remove the script message handler to avoid leaks.
            webView.configuration.userContentController.removeScriptMessageHandlerForName("progressHandler")
        }
    )
}