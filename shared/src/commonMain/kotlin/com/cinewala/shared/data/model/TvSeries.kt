package com.cinewala.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvSeriesResponse(
    val page: Int,
    val results: List<TvSeries>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class TvSeries(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null
)

@Serializable
data class TvSeriesDetail(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val seasons: List<Season> = emptyList()
)

@Serializable
data class Season(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null
)

@Serializable
data class SeasonDetail(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("season_number") val seasonNumber: Int,
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("air_date") val airDate: String? = null
)