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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cinewala.shared.data.model.Movie
import com.cinewala.shared.data.model.TvSeries
import com.cinewala.shared.data.remote.ApiClient
import com.cinewala.shared.ui.movies.HomeUiState
import com.cinewala.shared.ui.movies.RecentlyViewedItem
import com.cinewala.shared.ui.movies.rememberHomeViewModel
import com.cinewala.shared.ui.theme.Primary

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onMovieClick: (Movie) -> Unit = {},
    onSeriesClick: (TvSeries, seasonNumber: Int?, episodeNumber: Int?) -> Unit = { _, _, _ -> }
) {
    val viewModel = rememberHomeViewModel()
    val homeState by viewModel.uiState.collectAsState()

    when (homeState) {
        is HomeUiState.Loading -> {
            LoadingHomeScreen(modifier)
        }
        is HomeUiState.Error -> {
            ErrorHomeScreen(
                message = (homeState as HomeUiState.Error).message,
                modifier = modifier
            )
        }
        is HomeUiState.Success -> {
            val successState = homeState as HomeUiState.Success
            HomeContent(
                recentMovies = successState.recentMovies,
                recentSeries = successState.recentSeries,
                recentlyViewed = successState.recentlyViewed,
                onMovieClick = onMovieClick,
                onSeriesClick = onSeriesClick,
                modifier = modifier
            )
        }
        else -> {
            LoadingHomeScreen(modifier)
        }
    }
}

@Composable
private fun HomeContent(
    recentMovies: List<Movie>,
    recentSeries: List<TvSeries>,
    recentlyViewed: List<RecentlyViewedItem>,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (TvSeries, seasonNumber: Int?, episodeNumber: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        if (recentMovies.isNotEmpty()) {
            SectionTitle(title = "Recently Released Movies")
            HorizontalMovieList(movies = recentMovies, onMovieClick = onMovieClick)
        }

        if (recentSeries.isNotEmpty()) {
            SectionTitle(title = "Top Rated TV Series")
            HorizontalSeriesList(series = recentSeries, onSeriesClick = onSeriesClick)
        }

        if (recentlyViewed.isNotEmpty()) {
            SectionTitle(title = "Recently Viewed")
            RecentlyViewedSection(
                items = recentlyViewed,
                onMovieClick = onMovieClick,
                onSeriesClick = onSeriesClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun HorizontalMovieList(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies, key = { it.id }) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie) })
        }
    }
}

@Composable
private fun HorizontalSeriesList(
    series: List<TvSeries>,
    onSeriesClick: (TvSeries, seasonNumber: Int?, episodeNumber: Int?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(series, key = { it.id }) { series ->
            SeriesCard(series = series, onClick = { onSeriesClick(series, null, null) })
        }
    }
}

@Composable
private fun MovieCard(
    movie: Movie,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = movie.posterPath?.let { "${ApiClient.IMAGE_BASE_URL}/$it" },
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
            }
        }

        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        movie.releaseDate?.let { date ->
            Text(
                text = date.take(4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SeriesCard(
    series: TvSeries,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = series.posterPath?.let { "${ApiClient.IMAGE_BASE_URL}/$it" },
                contentDescription = series.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
            }
        }

        Text(
            text = series.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        series.firstAirDate?.let { date ->
            Text(
                text = date.take(4),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentlyViewedSection(
    items: List<RecentlyViewedItem>,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (TvSeries, seasonNumber: Int?, episodeNumber: Int?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { item ->
            when (item) {
                is RecentlyViewedItem.MovieItem -> "rv_movie_${item.movie.id}"
                is RecentlyViewedItem.SeriesItem -> "rv_series_${item.series.id}"
            }
        }) { item ->
            when (item) {
                is RecentlyViewedItem.MovieItem -> {
                    MovieCard(movie = item.movie, onClick = { onMovieClick(item.movie) })
                }
                is RecentlyViewedItem.SeriesItem -> {
                    SeriesCard(
                        series = item.series,
                        onClick = {
                            onSeriesClick(item.series, item.seasonNumber, item.episodeNumber)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingHomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Primary)
    }
}

@Composable
private fun ErrorHomeScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Failed to load content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
