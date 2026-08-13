package com.cinewala.shared.data.db

data class WatchProgress(
    val contentId: Long,
    val contentType: String,
    val title: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val seasonNumber: Long? = null,
    val episodeNumber: Long? = null,
    val episodeTitle: String? = null,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val progressPercent: Double = 0.0,
    val lastWatchedAt: Long
) {
    companion object {
        const val TYPE_MOVIE = "movie"
        const val TYPE_TV = "tv"
    }
}