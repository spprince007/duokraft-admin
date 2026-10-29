package se.duokraft.admin.data.repo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.core.apiCall
import se.duokraft.admin.data.net.ContentTypeDto
import se.duokraft.admin.data.net.PostDto
import se.duokraft.admin.data.net.TermDto
import se.duokraft.admin.data.net.WpApi

data class PageResult<T>(val items: List<T>, val totalPages: Int, val total: Int)

/** Everything the editor can change. Null means "leave as is". */
data class ContentDraft(
    val title: String,
    val content: String?,          // Null for Elementor items: their content is not stored here.
    val excerpt: String?,
    val status: String,            // publish, draft, pending, future, private
    val dateLocal: String?,        // "yyyy-MM-ddTHH:mm:ss" in the site's time zone, for scheduling
    val featuredMedia: Long,
    val terms: Map<String, List<Long>>, // taxonomy rest_base -> term ids
)

/** Pages, posts and custom post types through the core REST API. */
class ContentRepository(private val api: WpApi) {

    suspend fun list(type: ContentTypeDto, page: Int, search: String?, status: String?): ApiResult<PageResult<PostDto>> =
        apiCall {
            val res = api.listContent(
                ns = type.restNamespace,
                base = type.restBase,
                page = page,
                search = search?.takeIf { it.isNotBlank() },
                status = status ?: "publish,draft,pending,future,private",
            )
            if (!res.isSuccessful) {
                // Asking past the last page returns 400 in WordPress; treat it as "no more items".
                if (res.code() == 400 && page > 1) return@apiCall PageResult(emptyList(), page - 1, 0)
                throw retrofit2.HttpException(res)
            }
            PageResult(
                items = res.body().orEmpty(),
                totalPages = res.headers()["X-WP-TotalPages"]?.toIntOrNull() ?: 1,
                total = res.headers()["X-WP-Total"]?.toIntOrNull() ?: 0,
            )
        }

    suspend fun get(type: ContentTypeDto, id: Long): ApiResult<PostDto> =
        apiCall { api.getContent(type.restNamespace, type.restBase, id) }

    suspend fun save(type: ContentTypeDto, id: Long?, draft: ContentDraft): ApiResult<PostDto> = apiCall {
        val body = body(type, draft)
        if (id == null) api.createContent(type.restNamespace, type.restBase, body)
        else api.updateContent(type.restNamespace, type.restBase, id, body)
    }

    suspend fun trash(type: ContentTypeDto, id: Long): ApiResult<PostDto> =
        apiCall { api.trashContent(type.restNamespace, type.restBase, id) }

    suspend fun terms(taxonomyBase: String, search: String? = null): ApiResult<List<TermDto>> = apiCall {
        val all = mutableListOf<TermDto>()
        var page = 1
        while (page <= 10) { // Up to 1000 terms; more than enough for a site like this.
            val res = api.listTerms(taxonomyBase, page = page, search = search)
            if (!res.isSuccessful) throw retrofit2.HttpException(res)
            all += res.body().orEmpty()
            val pages = res.headers()["X-WP-TotalPages"]?.toIntOrNull() ?: 1
            if (page >= pages) break
            page++
        }
        all
    }

    suspend fun createTerm(taxonomyBase: String, name: String): ApiResult<TermDto> =
        apiCall { api.createTerm(taxonomyBase, buildJsonObject { put("name", name.trim()) }) }

    private fun body(type: ContentTypeDto, d: ContentDraft): JsonObject = buildJsonObject {
        if (type.supports.title) put("title", d.title)
        if (type.supports.editor && d.content != null) put("content", d.content)
        if (type.supports.excerpt && d.excerpt != null) put("excerpt", d.excerpt)
        put("status", d.status)
        if (d.dateLocal != null) put("date", d.dateLocal)
        if (type.supports.thumbnail) put("featured_media", d.featuredMedia)
        type.taxonomies.forEach { tax ->
            d.terms[tax.restBase]?.let { ids -> putJsonArray(tax.restBase) { ids.forEach { add(it) } } }
        }
    }
}
