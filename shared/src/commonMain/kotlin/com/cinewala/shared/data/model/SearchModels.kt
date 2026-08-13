package com.cinewala.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Unified search result that can be either movie or tv series
sealed class SearchResult {
    data class MovieResult(val movie: Movie) : SearchResult()
    data class SeriesResult(val series: TvSeries) : SearchResult()
}

@Serializable
data class SearchMultiResponse(
    val page: Int,
    val results: List<SearchItem>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class SearchItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("media_type") val mediaType: String = ""
) {
    fun toSearchResult(): SearchResult? {
        return when (mediaType) {
            "movie" -> {
                if (title != null && releaseDate != null) {
                    SearchResult.MovieResult(
                        movie = Movie(
                            id = id,
                            title = title,
                            overview = overview,
                            posterPath = posterPath,
                            backdropPath = backdropPath,
                            voteAverage = voteAverage,
                            releaseDate = releaseDate,
                            originalLanguage = originalLanguage
                        )
                    )
                } else null
            }
            "tv" -> {
                if (name != null && firstAirDate != null) {
                    SearchResult.SeriesResult(
                        series = TvSeries(
                            id = id,
                            name = name,
                            overview = overview,
                            posterPath = posterPath,
                            backdropPath = backdropPath,
                            voteAverage = voteAverage,
                            firstAirDate = firstAirDate,
                            originalLanguage = originalLanguage
                        )
                    )
                } else null
            }
            else -> null
        }
    }
}