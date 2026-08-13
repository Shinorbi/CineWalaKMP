package com.cinewala.shared.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.data.db.WatchProgress
import com.cinewala.shared.data.model.Episode
import com.cinewala.shared.data.model.Season
import com.cinewala.shared.data.model.TvSeries
import com.cinewala.shared.data.remote.ApiClient
import com.cinewala.shared.ui.movies.EpisodesState
import com.cinewala.shared.ui.movies.rememberSeriesViewModel
import com.cinewala.shared.ui.theme.NetflixSurface
import com.cinewala.shared.ui.theme.Primary
import com.cinewala.shared.util.currentTimeMillis
import kotlinx.coroutines.launch

@Composable
fun SeasonEpisodesScreen(
    series: TvSeries,
    season: Season,
    modifier: Modifier = Modifier,
    initialEpisodeNumber: Int? = null,
    onBack: () -> Unit
) {
    val viewModel = rememberSeriesViewModel()
    val episodesState by viewModel.episodesState.collectAsState()

    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var episodeProgress by remember { mutableStateOf<WatchProgress?>(null) }
    var latestEpisodeProgress by remember { mutableStateOf<PlayerProgress?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(series.id, season.seasonNumber) {
        viewModel.fetchSeasonEpisodes(series.id, season.seasonNumber)
    }

    // Auto-select the target episode once the episode list has loaded
    LaunchedEffect(episodesState, initialEpisodeNumber) {
        if (initialEpisodeNumber != null && selectedEpisode == null) {
            val state = episodesState
            if (state is EpisodesState.Success) {
                state.episodes.find { it.episodeNumber == initialEpisodeNumber }?.let { ep ->
                    selectedEpisode = ep
                }
            }
        }
    }

    // Load progress for the selected episode
    LaunchedEffect(selectedEpisode) {
        val episode = selectedEpisode ?: return@LaunchedEffect
        episodeProgress = try {
            DatabaseProvider.getRepository().getProgress(
                contentId = series.id.toLong(),
                contentType = WatchProgress.TYPE_TV,
                seasonNumber = season.seasonNumber.toLong(),
                episodeNumber = episode.episodeNumber.toLong()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveEpisodeProgress(episode: Episode, progress: PlayerProgress) {
        scope.launch {
            try {
                DatabaseProvider.getRepository().upsertProgress(
                    WatchProgress(
                        contentId = series.id.toLong(),
                        contentType = WatchProgress.TYPE_TV,
                        title = series.name,
                        posterPath = series.posterPath,
                        backdropPath = series.backdropPath,
                        seasonNumber = season.seasonNumber.toLong(),
                        episodeNumber = episode.episodeNumber.toLong(),
                        episodeTitle = episode.name,
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

    val currentEpisode = selectedEpisode
    if (currentEpisode != null) {
        PlayerScreen(
            url = "https://player.videasy.net/tv/${series.id}/${season.seasonNumber}/${currentEpisode.episodeNumber}",
            title = "${series.name} - ${season.name}",
            modifier = modifier,
            initialProgressSeconds = episodeProgress?.currentPosition ?: 0,
            onProgressUpdate = { progress ->
                latestEpisodeProgress = progress
                saveEpisodeProgress(currentEpisode, progress)
            },
            onBack = {
                // Save the latest known progress before leaving
                latestEpisodeProgress?.let { saveEpisodeProgress(currentEpisode, it) }
                selectedEpisode = null
            }
        )
        return
    }

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
            Column {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "${season.name} • ${season.episodeCount} episodes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (val state = episodesState) {
            is EpisodesState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is EpisodesState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is EpisodesState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.episodes, key = { it.id }) { episode ->
                        EpisodeCard(
                            episode = episode,
                            seriesId = series.id,
                            seasonNumber = season.seasonNumber,
                            onClick = { selectedEpisode = episode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: Episode,
    seriesId: Int,
    seasonNumber: Int,
    onClick: () -> Unit
) {
    var progress by remember { mutableStateOf<WatchProgress?>(null) }

    // Load progress for this episode
    LaunchedEffect(episode.id) {
        progress = try {
            DatabaseProvider.getRepository().getProgress(
                contentId = seriesId.toLong(),
                contentType = WatchProgress.TYPE_TV,
                seasonNumber = seasonNumber.toLong(),
                episodeNumber = episode.episodeNumber.toLong()
            )
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = NetflixSurface)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(78.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbnailUrl = episode.stillPath?.let {
                        ApiClient.IMAGE_BASE_URL + it
                    }
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = episode.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = "E${episode.episodeNumber} • ${episode.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = episode.overview.ifBlank { "No description available." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress bar if this episode has been watched
            val episodeProgress = progress
            if (episodeProgress != null && episodeProgress.progressPercent > 0.0) {
                LinearProgressIndicator(
                    progress = { episodeProgress.progressPercent.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
