package com.cinewala.shared.data.remote

import com.cinewala.shared.data.model.MovieResponse
import com.cinewala.shared.data.model.SeasonDetail
import com.cinewala.shared.data.model.SearchMultiResponse
import com.cinewala.shared.data.model.TvSeriesDetail
import com.cinewala.shared.data.model.TvSeriesResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiClient {

    const val BASE_URL = "https://api.themoviedb.org/3/"
    const val API_KEY = "779df0470c60b80ea4211a4cb08735cb"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(Logging) {
                level = LogLevel.BODY
            }
        }
    }

    val apiService: TmdbApiService = KtorTmdbApiService(httpClient)
}

class KtorTmdbApiService(
    private val client: HttpClient
) : TmdbApiService {

    override suspend fun getPopularMovies(apiKey: String, page: Int): MovieResponse {
        return client.get("${ApiClient.BASE_URL}movie/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    override suspend fun getPopularTvSeries(apiKey: String, page: Int): TvSeriesResponse {
        return client.get("${ApiClient.BASE_URL}tv/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }.body()
    }

    override suspend fun searchMulti(apiKey: String, query: String, page: Int): SearchMultiResponse {
        return client.get("${ApiClient.BASE_URL}search/multi") {
            parameter("api_key", apiKey)
            parameter("query", query)
            parameter("page", page)
        }.body()
    }

    override suspend fun getTvSeriesDetail(tvId: Int, apiKey: String): TvSeriesDetail {
        return client.get("${ApiClient.BASE_URL}tv/$tvId") {
            parameter("api_key", apiKey)
        }.body()
    }

    override suspend fun getSeasonEpisodes(tvId: Int, seasonNumber: Int, apiKey: String): SeasonDetail {
        return client.get("${ApiClient.BASE_URL}tv/$tvId/season/$seasonNumber") {
            parameter("api_key", apiKey)
        }.body()
    }
}