package se.duokraft.admin.data.repo

import android.net.Uri
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.core.apiCall
import se.duokraft.admin.data.net.MediaDto
import se.duokraft.admin.data.net.WpApi

/** Media Library: browse, upload (compressed), edit details, delete. */
class MediaRepository(private val api: WpApi, private val compressor: ImageCompressor) {

    suspend fun list(page: Int, search: String?, imagesOnly: Boolean = true): ApiResult<PageResult<MediaDto>> = apiCall {
        val res = api.listMedia(page = page, search = search?.takeIf { it.isNotBlank() }, mediaType = if (imagesOnly) "image" else null)
        if (!res.isSuccessful) {
            if (res.code() == 400 && page > 1) return@apiCall PageResult(emptyList(), page - 1, 0)
            throw retrofit2.HttpException(res)
        }
        PageResult(
            items = res.body().orEmpty(),
            totalPages = res.headers()["X-WP-TotalPages"]?.toIntOrNull() ?: 1,
            total = res.headers()["X-WP-Total"]?.toIntOrNull() ?: 0,
        )
    }

    suspend fun get(id: Long): ApiResult<MediaDto> = apiCall { api.getMedia(id) }

    suspend fun upload(uri: Uri): ApiResult<MediaDto> = apiCall {
        val file = compressor.prepare(uri)
        api.uploadMedia(
            contentDisposition = "attachment; filename=\"${file.fileName}\"",
            file = file.bytes.toRequestBody(file.mimeType.toMediaType()),
        )
    }

    suspend fun update(id: Long, title: String, altText: String, caption: String): ApiResult<MediaDto> = apiCall {
        api.updateMedia(id, buildJsonObject {
            put("title", title)
            put("alt_text", altText)
            put("caption", caption)
        })
    }

    suspend fun delete(id: Long): ApiResult<Unit> = apiCall {
        val res = api.deleteMedia(id)
        if (!res.isSuccessful) throw retrofit2.HttpException(res)
    }
}
