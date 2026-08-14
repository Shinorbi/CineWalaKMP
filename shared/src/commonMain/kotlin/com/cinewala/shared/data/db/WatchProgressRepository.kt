package com.cinewala.shared.data.db

import com.cinewala.shared.db.CineWalaDatabase
import com.cinewala.shared.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class WatchProgressRepository(
    private val database: CineWalaDatabase
) {
    private val queries = database.watchProgressQueries

    /**
     * Insert or update a watch progress entry. Also prunes entries older than 30 days.
     */
    suspend fun upsertProgress(progress: WatchProgress) {
        withContext(Dispatchers.IO) {
            // Normalize null season/episode to 0 so the PRIMARY KEY is consistent
            // for movies (SQLite treats NULL as distinct, which would otherwise
            // allow duplicate rows for the same movie).
            val season = progress.seasonNumber ?: 0L
            val episode = progress.episodeNumber ?: 0L
            queries.upsertProgress(
                content_id = progress.contentId,
                content_type = progress.contentType,
                title = progress.title,
                poster_path = progress.posterPath,
                backdrop_path = progress.backdropPath,
                season_number = season,
                episode_number = episode,
                episode_title = progress.episodeTitle,
                current_position = progress.currentPosition,
                duration = progress.duration,
                progress_percent = progress.progressPercent,
                last_watched_at = progress.lastWatchedAt
            )
            // Auto-delete entries not used in the last 30 days
            deleteOldEntries()
        }
    }

    /**
     * Get all recently watched items, ordered by most recently watched first.
     */
    fun getAllRecents(): Flow<List<WatchProgress>> {
        return queries.selectAllRecents()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { it.toWatchProgress() }
                    // Deduplicate by content identity, keeping the most recent entry.
                    .distinctBy {
                        Triple(it.contentId, it.contentType, it.seasonNumber to it.episodeNumber)
                    }
            }
    }

    /**
     * Get a single progress entry for a specific content item.
     */
    suspend fun getProgress(
        contentId: Long,
        contentType: String,
        seasonNumber: Long? = null,
        episodeNumber: Long? = null
    ): WatchProgress? {
        return withContext(Dispatchers.IO) {
            queries.selectProgress(
                content_id = contentId,
                content_type = contentType,
                season_number = seasonNumber ?: 0L,
                episode_number = episodeNumber ?: 0L
            ).executeAsOneOrNull()?.toWatchProgress()
        }
    }

    /**
     * Delete a specific progress entry.
     */
    suspend fun deleteProgress(
        contentId: Long,
        contentType: String,
        seasonNumber: Long? = null,
        episodeNumber: Long? = null
    ) {
        withContext(Dispatchers.IO) {
            queries.deleteProgress(
                content_id = contentId,
                content_type = contentType,
                season_number = seasonNumber ?: 0L,
                episode_number = episodeNumber ?: 0L
            )
        }
    }

    /**
     * Delete entries whose last_watched_at is older than the given threshold (epoch millis).
     */
    suspend fun deleteOldEntries(thresholdMillis: Long = currentTimeMillis() - THIRTY_DAYS_MILLIS) {
        withContext(Dispatchers.IO) {
            queries.deleteOldEntries(thresholdMillis)
        }
    }

    /**
     * Delete all progress entries.
     */
    suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            queries.deleteAll()
        }
    }

    private companion object {
        const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

private fun com.cinewala.shared.db.WatchProgress.toWatchProgress(): WatchProgress {
    return WatchProgress(
        contentId = content_id,
        contentType = content_type,
        title = title,
        posterPath = poster_path,
        backdropPath = backdrop_path,
        seasonNumber = season_number,
        episodeNumber = episode_number,
        episodeTitle = episode_title,
        currentPosition = current_position,
        duration = duration,
        progressPercent = progress_percent,
        lastWatchedAt = last_watched_at
    )
}