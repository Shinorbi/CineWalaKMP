package com.cinewala.shared.ui.movies

import com.cinewala.shared.data.model.SearchResult
import com.cinewala.shared.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val scope: CoroutineScope
) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private var currentSearchPage = 0
    private var totalSearchPages = 1
    private var isSearchFetching = false
    private var currentSearchQuery = ""

    companion object {
        private const val DEBOUNCE_DELAY_MS = 500L // 500ms debounce
    }

    init {
        // Setup debounced search
        scope.launch {
            _searchQuery
                .debounce(DEBOUNCE_DELAY_MS)
                .distinctUntilChanged()
                .filter { it.length >= 2 } // Minimum 2 characters to search
                .map { query -> query }
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                        _searchError.value = null
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchResults.value = emptyList()
            _searchError.value = null
        }
    }

    private suspend fun performSearch(query: String) {
        if (isSearchFetching) return
        if (query == currentSearchQuery && currentSearchPage > 1) return

        isSearchFetching = true
        _isSearching.value = true
        _searchError.value = null

        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.searchMulti(
                    apiKey = ApiClient.API_KEY,
                    query = query,
                    page = 1
                )
            }

            currentSearchQuery = query
            currentSearchPage = 1
            totalSearchPages = response.totalPages

            val results = response.results
                .mapNotNull { it.toSearchResult() }
                .filter { result ->
                    // Filter out person results, only keep movies and tv series
                    result is SearchResult.MovieResult || result is SearchResult.SeriesResult
                }

            _searchResults.value = results
        } catch (e: Exception) {
            _searchError.value = e.message ?: "Search failed"
        } finally {
            _isSearching.value = false
            isSearchFetching = false
        }
    }

    fun loadNextSearchPage() {
        if (isSearchFetching || _isSearching.value) return
        if (currentSearchPage >= totalSearchPages) return
        if (currentSearchQuery.isBlank()) return

        _isSearching.value = true
        scope.launch {
            try {
                val nextPage = currentSearchPage + 1
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.searchMulti(
                        apiKey = ApiClient.API_KEY,
                        query = currentSearchQuery,
                        page = nextPage
                    )
                }

                currentSearchPage = nextPage
                val newResults = response.results
                    .mapNotNull { it.toSearchResult() }
                    .filter { result ->
                        result is SearchResult.MovieResult || result is SearchResult.SeriesResult
                    }

                _searchResults.value = _searchResults.value + newResults
            } catch (e: Exception) {
                // Silently ignore pagination errors
            } finally {
                _isSearching.value = false
                isSearchFetching = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchError.value = null
        currentSearchQuery = ""
        currentSearchPage = 0
        totalSearchPages = 1
    }
}