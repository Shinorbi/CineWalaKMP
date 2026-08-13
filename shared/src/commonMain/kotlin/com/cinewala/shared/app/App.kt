package com.cinewala.shared.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.data.model.Movie
import com.cinewala.shared.data.model.Season
import com.cinewala.shared.data.model.TvSeries
import com.cinewala.shared.screen.HomeScreen
import com.cinewala.shared.screen.MovieDetailScreen
import com.cinewala.shared.screen.MovieScreen
import com.cinewala.shared.screen.RecentsScreen
import com.cinewala.shared.screen.SearchScreen
import com.cinewala.shared.screen.SeasonEpisodesScreen
import com.cinewala.shared.screen.SeriesDetailScreen
import com.cinewala.shared.screen.SeriesScreen

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

private val BottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home),
    BottomNavItem("Movies", Icons.Filled.LocalMovies),
    BottomNavItem("Series", Icons.Filled.Tv),
    BottomNavItem("Search", Icons.Filled.Search),
    BottomNavItem("Recents", Icons.Filled.History)
)

@Composable
fun App() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedSeries by remember { mutableStateOf<TvSeries?>(null) }
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    // When non-null, SeasonEpisodesScreen will auto-select and play this episode
    var autoSelectEpisodeNumber by remember { mutableStateOf<Int?>(null) }

    // Hide the bottom navigation bar when a detail screen is shown
    val showBottomBar = selectedMovie == null && selectedSeries == null && selectedSeason == null

    // Auto-delete entries older than 30 days on app start
    LaunchedEffect(Unit) {
        try {
            DatabaseProvider.getRepository().deleteOldEntries()
        } catch (e: Exception) {
            // Ignore DB errors on startup
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            selectedSeason != null && selectedSeries != null -> {
                SeasonEpisodesScreen(
                    series = selectedSeries!!,
                    season = selectedSeason!!,
                    initialEpisodeNumber = autoSelectEpisodeNumber,
                    onBack = {
                        selectedSeason = null
                        autoSelectEpisodeNumber = null
                    },
                    modifier = contentModifier
                )
            }
            selectedMovie != null -> {
                MovieDetailScreen(
                    movie = selectedMovie!!,
                    onBack = { selectedMovie = null },
                    modifier = contentModifier
                )
            }
            selectedSeries != null -> {
                SeriesDetailScreen(
                    series = selectedSeries!!,
                    onSeasonClick = { season ->
                        selectedSeason = season
                        autoSelectEpisodeNumber = null
                    },
                    onBack = { selectedSeries = null },
                    modifier = contentModifier
                )
            }
            else -> {
                when (selectedTab) {
                    0 -> HomeScreen(
                        modifier = contentModifier,
                        onMovieClick = { selectedMovie = it },
                        onSeriesClick = { series, seasonNumber, episodeNumber ->
                            if (seasonNumber != null) {
                                // Navigate directly to the specific season + episode
                                selectedSeries = series
                                selectedSeason = Season(
                                    id = 0,
                                    name = "Season $seasonNumber",
                                    overview = "",
                                    seasonNumber = seasonNumber,
                                    episodeCount = 0,
                                    posterPath = null
                                )
                                autoSelectEpisodeNumber = episodeNumber
                            } else {
                                selectedSeries = series
                            }
                        }
                    )
                    1 -> MovieScreen(
                        modifier = contentModifier,
                        onMovieClick = { selectedMovie = it }
                    )
                    2 -> SeriesScreen(
                        modifier = contentModifier,
                        onSeriesClick = { series -> selectedSeries = series }
                    )
                    3 -> SearchScreen(
                        modifier = contentModifier,
                        onMovieClick = { selectedMovie = it },
                        onSeriesClick = { series -> selectedSeries = series }
                    )
                    4 -> RecentsScreen(
                        modifier = contentModifier,
                        onMovieClick = { recent ->
                            selectedMovie = Movie(
                                id = recent.contentId.toInt(),
                                title = recent.title,
                                overview = "",
                                posterPath = recent.posterPath,
                                backdropPath = recent.backdropPath
                            )
                        },
                        onSeriesClick = { recent ->
                            val seasonNum = recent.seasonNumber?.toInt()
                            val episodeNum = recent.episodeNumber?.toInt()
                            selectedSeries = TvSeries(
                                id = recent.contentId.toInt(),
                                name = recent.title,
                                overview = "",
                                posterPath = recent.posterPath,
                                backdropPath = recent.backdropPath
                            )
                            if (seasonNum != null) {
                                selectedSeason = Season(
                                    id = 0,
                                    name = "Season $seasonNum",
                                    overview = "",
                                    seasonNumber = seasonNum,
                                    episodeCount = 0,
                                    posterPath = null
                                )
                                autoSelectEpisodeNumber = episodeNum
                            } else {
                                selectedSeason = null
                                autoSelectEpisodeNumber = null
                            }
                        }
                    )
                }
            }
        }
    }
}
