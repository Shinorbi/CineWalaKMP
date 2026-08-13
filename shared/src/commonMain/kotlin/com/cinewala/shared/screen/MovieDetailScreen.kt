package com.cinewala.shared.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.data.db.WatchProgress
import com.cinewala.shared.data.model.Movie
import com.cinewala.shared.data.remote.ApiClient
import com.cinewala.shared.ui.theme.NetflixRed
import com.cinewala.shared.util.currentTimeMillis
import com.cinewala.shared.util.formatDuration
import com.cinewala.shared.util.formatRating
import kotlinx.coroutines.launch

@Composable
fun MovieDetailScreen(
    movie: Movie,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var savedProgress by remember { mutableStateOf<WatchProgress?>(null) }
    var latestProgress by remember { mutableStateOf<PlayerProgress?>(null) }
    val scope = rememberCoroutineScope()

    // Load saved progress from SQLite
    LaunchedEffect(movie.id) {
        savedProgress = try {
            DatabaseProvider.getRepository().getProgress(
                contentId = movie.id.toLong(),
                contentType = WatchProgress.TYPE_MOVIE
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveMovieProgress(progress: PlayerProgress) {
        scope.launch {
            try {
                DatabaseProvider.getRepository().upsertProgress(
                    WatchProgress(
                        contentId = movie.id.toLong(),
                        contentType = WatchProgress.TYPE_MOVIE,
                        title = movie.title,
                        posterPath = movie.posterPath,
                        backdropPath = movie.backdropPath,
                        currentPosition = progress.currentTime,
                        duration = progress.duration,
                        progressPercent = progress.progress,
                        lastWatchedAt = currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Ignore DB errors during playback
            }
        }
    }

    if (isPlaying) {
        PlayerScreen(

            url = "https://player.videasy.net/movie/${movie.id}",
            title = movie.title,
            modifier = modifier,
            initialProgressSeconds = savedProgress?.currentPosition ?: 0,
            onProgressUpdate = { progress ->
                latestProgress = progress
                saveMovieProgress(progress)
            },
            onBack = {
                // Save the latest known progress before leaving
                latestProgress?.let { saveMovieProgress(it) }
                isPlaying = false
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                text = "Movie Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Backdrop image
        val backdropUrl = movie.backdropPath?.let {
            ApiClient.IMAGE_BASE_URL.replace("w500", "w780") + it
        }
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating and release date row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "★ ${formatRating(movie.voteAverage)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = movie.releaseDate ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resume button if progress exists and not finished
            val hasProgress = savedProgress != null &&
                savedProgress!!.currentPosition > 0 &&
                savedProgress!!.progressPercent < 0.95

            if (hasProgress) {
                OutlinedButton(
                    onClick = { isPlaying = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "▶  Resume at ${formatDuration(savedProgress!!.currentPosition)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Play Now button
            Button(
                onClick = { isPlaying = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
            ) {
                Text(
                    text = if (hasProgress) "▶  Restart" else "▶  Play Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = movie.overview.ifBlank { "No overview available." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}