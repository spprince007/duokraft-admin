package se.duokraft.admin.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Result of an API call with a message that can be shown to a non-technical user. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val code: Int? = null) : ApiResult<Nothing>
}

private val errJson = Json { ignoreUnknownKeys = true }

/** Runs [block] and turns every failure into a friendly [ApiResult.Err]. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    ApiResult.Err(httpMessage(e), e.code())
} catch (e: IOException) {
    ApiResult.Err("No connection to the website. Check your internet and try again.")
} catch (e: Exception) {
    ApiResult.Err("Something went wrong: ${e.message ?: e.javaClass.simpleName}")
}

private fun httpMessage(e: HttpException): String {
    // WordPress errors look like {"code":"...","message":"..."}; prefer its message.
    val wpMessage = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        errJson.parseToJsonElement(body).jsonObject["message"]?.jsonPrimitive?.content
    }.getOrNull()?.let { android.text.Html.fromHtml(it, 0).toString().trim() }

    return when (e.code()) {
        401 -> "Your login has expired. Please log in again."
        403 -> wpMessage ?: "You don't have permission to do that."
        404 -> wpMessage ?: "That item no longer exists."
        413 -> "The file is too large for the website."
        429 -> wpMessage ?: "Too many requests. Wait a moment and try again."
        in 500..599 -> "The website had a problem (${e.code()}). Try again shortly."
        else -> wpMessage ?: "Request failed (${e.code()})."
    }
}

inline fun <T, R> ApiResult<T>.map(f: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(f(value))
    is ApiResult.Err -> this
}
