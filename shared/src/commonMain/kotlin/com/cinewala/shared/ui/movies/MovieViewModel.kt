package com.cinewala.shared.ui.movies

import com.cinewala.shared.data.model.Movie
import com.cinewala.shared.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class MovieUiState {
    data object Loading : MovieUiState()
    data class Success(val movies: List<Movie>) : MovieUiState()
    data class Error(val message: String) : MovieUiState()
}

class MovieViewModel(
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow<MovieUiState>(MovieUiState.Loading)
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 0
    private var totalPages = 100
    private val accumulatedMovies = mutableListOf<Movie>()
    private val seenMovieIds = mutableSetOf<Int>()
    private var isFetching = false

    init {
        fetchPopularMovies()
    }

    fun fetchPopularMovies() {
        if (isFetching) return
        isFetching = true
        scope.launch {
            _uiState.value = MovieUiState.Loading
            try {
                val response = withContext(Dispatchers.Default) {
                    ApiClient.apiService.getPopularMovies(
                        apiKey = ApiClient.API_KEY,
                        page = 1
                    )
                }
                currentPage = 1
                totalPages = minOf(response.totalPages, 100)
                accumulatedMovies.clear()
                seenMovieIds.clear()
                response.results.forEach { movie ->
                    if (seenMovieIds.add(movie.id)) {
                        accumulatedMovies.add(movie)
                    }
                }
                _uiState.value = MovieUiState.Success(accumulatedMovies.toList())
            } catch (e: Exception) {
                _uiState.value = MovieUiState.Error(e.message ?: "Failed to load movies")
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
                    ApiClient.apiService.getPopularMovies(
                        apiKey = ApiClient.API_KEY,
                        page = nextPage
                    )
                }
                currentPage = nextPage
                totalPages = minOf(response.totalPages, 100)
                response.results.forEach { movie ->
                    if (seenMovieIds.add(movie.id)) {
                        accumulatedMovies.add(movie)
                    }
                }
                _uiState.value = MovieUiState.Success(accumulatedMovies.toList())
            } catch (e: Exception) {
                // Silently ignore pagination errors; user can scroll again to retry
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}