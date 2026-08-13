package com.cinewala.shared.data.remote

import com.cinewala.shared.data.model.MovieResponse
import com.cinewala.shared.data.model.SeasonDetail
import com.cinewala.shared.data.model.SearchMultiResponse
import com.cinewala.shared.data.model.TvSeriesDetail
import com.cinewala.shared.data.model.TvSeriesResponse

interface TmdbApiService {

    suspend fun getPopularMovies(
        apiKey: String,
        page: Int = 1
    ): MovieResponse

    suspend fun getPopularTvSeries(
        apiKey: String,
        page: Int = 1
    ): TvSeriesResponse

    suspend fun searchMulti(
        apiKey: String,
        query: String,
        page: Int = 1
    ): SearchMultiResponse

    suspend fun getTvSeriesDetail(
        tvId: Int,
        apiKey: String
    ): TvSeriesDetail

    suspend fun getSeasonEpisodes(
        tvId: Int,
        seasonNumber: Int,
        apiKey: String
    ): SeasonDetail
}