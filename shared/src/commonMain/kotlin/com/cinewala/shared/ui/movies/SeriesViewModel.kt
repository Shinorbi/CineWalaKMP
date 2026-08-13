package com.cinewala.shared.ui.movies

import com.cinewala.shared.data.model.Episode
import com.cinewala.shared.data.model.Season
import com.cinewala.shared.data.model.TvSeries
import com.cinewala.shared.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SeriesUiState {
    data object Loading : SeriesUiState()
    data class Success(val series: List<TvSeries>) : SeriesUiState()
    data class Error(val message: String) : SeriesUiState()
}

sealed class SeriesDetailState {
    data object Loading : SeriesDetailState()
    data class Success(val seasons: List<Season>) : SeriesDetailState()
    data class Error(val message: String) : SeriesDetailState()
}

sealed class EpisodesState {
    data object Loading : EpisodesState()
    data class Success(val episodes: List<Episode>) : EpisodesState()
    data class Error(val message: String) : EpisodesState()
}

class SeriesViewModel(
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow<SeriesUiState>(SeriesUiState.Loading)
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<SeriesDetailState>(SeriesDetailState.Loading)
    val detailState: StateFlow<SeriesDetailState> = _detailState.asStateFlow()

    private val _episodesState = MutableStateFlow<EpisodesState>(EpisodesState.Loading)
    val episodesState: StateFlow<EpisodesState> = _episodesState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 0
    private var totalPages = 100
    private val accumulatedSeries = mutableListOf<TvSeries>()
    private val seenSeriesIds = mutableSetOf<Int>()
    private var isFetching = false

    init {
        fetchPopularTvSeries()
    }

    fun fetchPopularTvSeries() {
        if (isFetching) return
        isFetching = true
        scope.launch {
            _uiState.value = SeriesUiState.Loading
            try {
                val response = withContext(Dispatchers.Default) {
                    ApiClient.apiService.getPopularTvSeries(
                        apiKey = ApiClient.API_KEY,
                        page = 1
                    )
                }
                currentPage = 1
                totalPages = minOf(response.totalPages, 100)
                accumulatedSeries.clear()
                seenSeriesIds.clear()
                response.results.forEach { series ->
                    if (seenSeriesIds.add(series.id)) {
                        accumulatedSeries.add(series)
                    }
                }
                _uiState.value = SeriesUiState.Success(accumulatedSeries.toList())
            } catch (e: Exception) {
                _uiState.value = SeriesUiState.Error(e.message ?: "Failed to load series")
            } finally {
                isFetching = false
            }
        }
    }

    fun loadNextPage() {
        if (isFetching || _isLoadingMore.value) return
        if (currentPage >= totalPages) return

        _isLoadingMore.value = true
        scope.launch {
            try {
                val nextPage = currentPage + 1
                val response = withContext(Dispatchers.Default) {
                    ApiClient.apiService.getPopularTvSeries(
                        apiKey = ApiClient.API_KEY,
                        page = nextPage
                    )
                }
                currentPage = nextPage
                totalPages = minOf(response.totalPages, 100)
                response.results.forEach { series ->
                    if (seenSeriesIds.add(series.id)) {
                        accumulatedSeries.add(series)
                    }
                }
                _uiState.value = SeriesUiState.Success(accumulatedSeries.toList())
            } catch (e: Exception) {
                // Silently ignore pagination errors
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun fetchSeriesDetail(tvId: Int) {
        scope.launch {
            _detailState.value = SeriesDetailState.Loading
            try {
                val detail = withContext(Dispatchers.Default) {
                    ApiClient.apiService.getTvSeriesDetail(
                        tvId = tvId,
                        apiKey = ApiClient.API_KEY
                    )
                }
                _detailState.value = SeriesDetailState.Success(detail.seasons)
            } catch (e: Exception) {
                _detailState.value = SeriesDetailState.Error(e.message ?: "Failed to load seasons")
            }
        }
    }

    fun fetchSeasonEpisodes(tvId: Int, seasonNumber: Int) {
        scope.launch {
            _episodesState.value = EpisodesState.Loading
            try {
                val seasonDetail = withContext(Dispatchers.Default) {
                    ApiClient.apiService.getSeasonEpisodes(
                        tvId = tvId,
                        seasonNumber = seasonNumber,
                        apiKey = ApiClient.API_KEY
                    )
                }
                _episodesState.value = EpisodesState.Success(seasonDetail.episodes)
            } catch (e: Exception) {
                _episodesState.value = EpisodesState.Error(e.message ?: "Failed to load episodes")
            }
        }
    }
}