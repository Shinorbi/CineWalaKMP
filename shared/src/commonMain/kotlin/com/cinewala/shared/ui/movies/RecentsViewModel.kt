package com.cinewala.shared.ui.movies

import com.cinewala.shared.data.db.DatabaseProvider
import com.cinewala.shared.data.db.WatchProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecentsUiState {
    data object Loading : RecentsUiState()
    data class Success(val recents: List<WatchProgress>) : RecentsUiState()
    data class Error(val message: String) : RecentsUiState()
}

class RecentsViewModel(
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<RecentsUiState>(RecentsUiState.Loading)
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

    init {
        loadRecents()
    }

    fun loadRecents() {
        scope.launch {
            _uiState.value = RecentsUiState.Loading
            try {
                val repository = DatabaseProvider.getRepository()
                repository.getAllRecents().collect { recents ->
                    _uiState.value = RecentsUiState.Success(recents)
                }
            } catch (e: Exception) {
                _uiState.value = RecentsUiState.Error(e.message ?: "Failed to load recents")
            }
        }
    }

    fun removeItem(progress: WatchProgress) {
        scope.launch {
            try {
                val repository = DatabaseProvider.getRepository()
                repository.deleteProgress(
                    contentId = progress.contentId,
                    contentType = progress.contentType,
                    seasonNumber = progress.seasonNumber,
                    episodeNumber = progress.episodeNumber
                )
                loadRecents()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}