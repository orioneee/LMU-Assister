package com.orioooneee.lmuasister.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable

@Serializable
private data class RaceShareCardErrorDto(val error: String? = null)

internal suspend fun HttpResponse.toRaceShareCardException(): BackendApiException {
    val body = bodyAsText()
    val code = runCatching {
        ProfileJson.decodeFromString<RaceShareCardErrorDto>(body).error
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "share_card_http_${status.value}"
    return BackendApiException(
        statusCode = status.value,
        code = code,
        detail = body.take(240),
    )
}

internal suspend fun HttpClient.downloadRaceShareCard(url: String): ByteArray {
    require(url.startsWith("https://") || url.startsWith("http://")) {
        "share_card_invalid_url"
    }
    val response = get(url)
    if (response.status.value !in 200..299) {
        val failure = response.toRaceShareCardException()
        throw BackendApiException(
            statusCode = failure.statusCode,
            code = "share_card_download_failed",
            detail = failure.message.orEmpty(),
        )
    }
    return response.bodyAsBytes().also {
        require(it.isNotEmpty()) { "share_card_empty" }
    }
}
