package com.cinewala.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbErrorResponse(
    val success: Boolean = false,
    @SerialName("status_code") val statusCode: Int = 0,
    @SerialName("status_message") val statusMessage: String = ""
)