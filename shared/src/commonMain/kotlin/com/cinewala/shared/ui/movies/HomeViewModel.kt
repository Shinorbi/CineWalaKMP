package com.cinewala.shared.ui.movies

import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.data.db.WatchProgress
import com.cinewala.shared.data.model.Movie
import com.cinewala.shared.data.model.TvSeries
import com.cinewala.shared.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val recentMovies: List<Movie>,
        val recentSeries: List<TvSeries>,
        val recentlyViewed: List<RecentlyViewedItem>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

sealed class RecentlyViewedItem {
    data class MovieItem(val movie: Movie) : RecentlyViewedItem()
    data class SeriesItem(
        val series: TvSeries,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null
    ) : RecentlyViewedItem()
}

class HomeViewModel(
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData(scope)
    }

    fun loadHomeData(scope: CoroutineScope = this.scope) {
        scope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Fetch recently released movies and series in parallel
                val recentMoviesDeferred = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getPopularMovies(
                        apiKey = ApiClient.API_KEY,
                        page = 1
                    )
                }

                val recentSeriesDeferred = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getPopularTvSeries(
                        apiKey = ApiClient.API_KEY,
                        page = 1
                    )
                }

                val recentMovies = recentMoviesDeferred.results.take(10)
                val recentSeries = recentSeriesDeferred.results.take(10)

                // Load recently viewed from SQLite
                val recentlyViewed = loadRecentlyViewedFromDb()

                _uiState.value = HomeUiState.Success(
                    recentMovies = recentMovies,
                    recentSeries = recentSeries,
                    recentlyViewed = recentlyViewed
                )
            } catch (e: Exception) {
                // If API call fails but DB has recents, still show the recents
                try {
                    val recentlyViewed = loadRecentlyViewedFromDb()
                    _uiState.value = HomeUiState.Success(
                        recentMovies = emptyList(),
                        recentSeries = emptyList(),
                        recentlyViewed = recentlyViewed
                    )
                } catch (dbError: Exception) {
                    _uiState.value = HomeUiState.Error(e.message ?: "Failed to load home data")
                }
            }
        }
    }

    private suspend fun loadRecentlyViewedFromDb(): List<RecentlyViewedItem> {
        return try {
            val result = DatabaseProvider.getRepository().getAllRecents().first()
            result.mapNotNull { progress ->
                when (progress.contentType) {
                    WatchProgress.TYPE_MOVIE -> {
                        RecentlyViewedItem.MovieItem(
                            Movie(
                                id = progress.contentId.toInt(),
                                title = progress.title,
                                overview = "",
                                posterPath = progress.posterPath,
                                backdropPath = progress.backdropPath
                            )
                        )
                    }
                    WatchProgress.TYPE_TV -> {
                        RecentlyViewedItem.SeriesItem(
                            series = TvSeries(
                                id = progress.contentId.toInt(),
                                name = progress.title,
                                overview = "",
                                posterPath = progress.posterPath,
                                backdropPath = progress.backdropPath
                            ),
                            seasonNumber = progress.seasonNumber?.toInt(),
                            episodeNumber = progress.episodeNumber?.toInt()
                        )
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            // Never crash the app if the DB is unavailable; just show no recents.
            emptyList()
        }
    }
}