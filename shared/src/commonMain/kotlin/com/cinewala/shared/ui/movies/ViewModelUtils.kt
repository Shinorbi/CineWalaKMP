package com.cinewala.shared.ui.movies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

@Composable
fun rememberAppCoroutineScope(): CoroutineScope {
    return remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
}

@Composable
fun rememberHomeViewModel(): HomeViewModel {
    val scope = rememberAppCoroutineScope()
    return remember { HomeViewModel(scope) }
}

@Composable
fun rememberMovieViewModel(): MovieViewModel {
    val scope = rememberAppCoroutineScope()
    return remember { MovieViewModel(scope) }
}

@Composable
fun rememberSeriesViewModel(): SeriesViewModel {
    val scope = rememberAppCoroutineScope()
    return remember { SeriesViewModel(scope) }
}

@Composable
fun rememberSearchViewModel(): SearchViewModel {
    val scope = rememberAppCoroutineScope()
    return remember { SearchViewModel(scope) }
}

@Composable
fun rememberRecentsViewModel(): RecentsViewModel {
    val scope = rememberAppCoroutineScope()
    return remember { RecentsViewModel(scope) }
}
