package com.cinewala.shared.data.remote

import com.cinewala.shared.data.model.MovieResponse
import com.cinewala.shared.data.model.SeasonDetail
import com.cinewala.shared.data.model.SearchMultiResponse
import com.cinewala.shared.data.model.TmdbErrorResponse
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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
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
        return safeGet("${ApiClient.BASE_URL}movie/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }
    }

    override suspend fun getPopularTvSeries(apiKey: String, page: Int): TvSeriesResponse {
        return safeGet("${ApiClient.BASE_URL}tv/popular") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }
    }

    override suspend fun getTopRatedTvSeries(apiKey: String, page: Int): TvSeriesResponse {
        return safeGet("${ApiClient.BASE_URL}tv/top_rated") {
            parameter("api_key", apiKey)
            parameter("page", page)
        }
    }

    override suspend fun searchMulti(apiKey: String, query: String, page: Int): SearchMultiResponse {
        return safeGet("${ApiClient.BASE_URL}search/multi") {
            parameter("api_key", apiKey)
            parameter("query", query)
            parameter("page", page)
        }
    }

    override suspend fun getTvSeriesDetail(tvId: Int, apiKey: String): TvSeriesDetail {
        return safeGet("${ApiClient.BASE_URL}tv/$tvId") {
            parameter("api_key", apiKey)
        }
    }

    override suspend fun getSeasonEpisodes(tvId: Int, seasonNumber: Int, apiKey: String): SeasonDetail {
        return safeGet("${ApiClient.BASE_URL}tv/$tvId/season/$seasonNumber") {
            parameter("api_key", apiKey)
        }
    }

    /**
     * Performs a GET request and throws a descriptive exception if the response
     * is not successful, instead of letting Ktor attempt to deserialize an
     * error body into the expected response type (which causes JsonConvertException).
     */
    private suspend inline fun <reified T> safeGet(
        url: String,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit
    ): T {
        val response: HttpResponse = client.get(url) { block() }
        if (response.status.value !in 200..299) {
            val errorMessage = try {
                val error = response.body<TmdbErrorResponse>()
                error.statusMessage.ifBlank { "HTTP ${response.status.value}" }
            } catch (e: Exception) {
                "HTTP ${response.status.value}"
            }
            throw ApiException(response.status, errorMessage)
        }
        return response.body()
    }
}

class ApiException(
    val statusCode: HttpStatusCode,
    override val message: String
) : Exception(message)