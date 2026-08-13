package com.cinewala.shared.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Full-screen player that loads the given streaming URL in a platform WebView.
 * On iOS this uses WKWebView; on Android it uses WebView.
 *
 * @param url The streaming URL to load.
 * @param title The title shown in the top bar.
 * @param initialProgressSeconds Start position in seconds (passed as ?progress= to the player).
 * @param onProgressUpdate Called when the player emits progress events via postMessage.
 */
@Composable
fun PlayerScreen(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    initialProgressSeconds: Long = 0,
    onProgressUpdate: (PlayerProgress) -> Unit = {},
    onBack: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        PlatformWebView(
            url = url,
            initialProgressSeconds = initialProgressSeconds,
            onProgressUpdate = onProgressUpdate,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Platform-specific WebView implementation.
 */
@Composable
expect fun PlatformWebView(
    url: String,
    modifier: Modifier = Modifier,
    initialProgressSeconds: Long = 0,
    onProgressUpdate: (PlayerProgress) -> Unit = {}
)