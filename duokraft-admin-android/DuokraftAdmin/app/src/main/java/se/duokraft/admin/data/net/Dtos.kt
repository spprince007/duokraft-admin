package se.duokraft.admin.data.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- duokraft-app/v1 ----------

@Serializable
data class SiteDto(
    @SerialName("api_version") val apiVersion: String = "",
    val name: String = "",
    val description: String = "",
    @SerialName("home_url") val homeUrl: String = "",
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    @SerialName("front_page_id") val frontPageId: Long = 0,
    val user: SiteUserDto = SiteUserDto(),
    val features: FeaturesDto = FeaturesDto(),
    @SerialName("content_types") val contentTypes: List<ContentTypeDto> = emptyList(),
    @SerialName("wp_version") val wpVersion: String? = null,
)

@Serializable
data class SiteUserDto(
    val id: Long = 0,
    val name: String = "",
    val roles: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("can_upload") val canUpload: Boolean = false,
    @SerialName("can_menus") val canMenus: Boolean = false,
)

@Serializable
data class FeaturesDto(
    val woocommerce: Boolean = false,
    @SerialName("custom_fields") val customFields: Boolean = false,
    val elementor: Boolean = false,
)

@Serializable
data class ContentTypeDto(
    val slug: String,
    val name: String,
    @SerialName("singular_name") val singularName: String = name,
    @SerialName("rest_base") val restBase: String,
    @SerialName("rest_namespace") val restNamespace: String = "wp/v2",
    val hierarchical: Boolean = false,
    val supports: SupportsDto = SupportsDto(),
    val taxonomies: List<TaxonomyDto> = emptyList(),
    val can: CanDto = CanDto(),
)

@Serializable
data class SupportsDto(
    val title: Boolean = true,
    val editor: Boolean = true,
    val thumbnail: Boolean = false,
    val excerpt: Boolean = false,
)

@Serializable
data class TaxonomyDto(
    val slug: String,
    val name: String,
    @SerialName("rest_base") val restBase: String,
    val hierarchical: Boolean = false,
)

@Serializable
data class CanDto(val create: Boolean = false, val publish: Boolean = false, val delete: Boolean = false)

@Serializable
data class DashboardDto(
    val types: List<DashTypeDto> = emptyList(),
    val totals: Map<String, Int> = emptyMap(),
    val media: DashMediaDto = DashMediaDto(),
)

@Serializable
data class DashTypeDto(
    val slug: String,
    val name: String,
    @SerialName("rest_base") val restBase: String,
    val counts: Map<String, Int> = emptyMap(),
    val recent: List<RecentItemDto> = emptyList(),
)

@Serializable
data class RecentItemDto(
    val id: Long,
    val title: String = "",
    val status: String = "",
    val modified: String = "",
    val link: String = "",
    val builder: String = "editor",
)

@Serializable
data class DashMediaDto(val total: Int = 0, val recent: List<RecentMediaDto> = emptyList())

@Serializable
data class RecentMediaDto(
    val id: Long,
    val title: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val url: String = "",
)

// ---------- wp/v2 ----------

@Serializable
data class AppPasswordDto(val uuid: String = "", val name: String = "")

@Serializable
data class Rendered(val raw: String? = null, val rendered: String = "")

@Serializable
data class PostDto(
    val id: Long,
    val date: String? = null,
    val modified: String? = null,
    val status: String = "draft",
    val link: String = "",
    val title: Rendered = Rendered(),
    val content: Rendered = Rendered(),
    val excerpt: Rendered = Rendered(),
    @SerialName("featured_media") val featuredMedia: Long = 0,
    val categories: List<Long> = emptyList(),
    val tags: List<Long> = emptyList(),
    @SerialName("duokraft_builder") val builder: String = "editor",
    @SerialName("_embedded") val embedded: EmbeddedDto? = null,
)

@Serializable
data class EmbeddedDto(@SerialName("wp:featuredmedia") val featuredMedia: List<MediaDto> = emptyList())

@Serializable
data class MediaDto(
    val id: Long = 0,
    val date: String? = null,
    val title: Rendered = Rendered(),
    val caption: Rendered = Rendered(),
    @SerialName("alt_text") val altText: String = "",
    @SerialName("mime_type") val mimeType: String = "",
    @SerialName("source_url") val sourceUrl: String = "",
    // WordPress sends an object here, but an empty array for some files (e.g. SVG), so parse it leniently.
    @SerialName("media_details") val detailsRaw: kotlinx.serialization.json.JsonElement? = null,
    val post: Long? = null,
) {
    val details: MediaDetailsDto?
        get() = (detailsRaw as? kotlinx.serialization.json.JsonObject)?.let {
            runCatching { WpJson.decodeFromJsonElement(MediaDetailsDto.serializer(), it) }.getOrNull()
        }

    /** Small version for grids, falling back to the original file. */
    val thumbUrl: String
        get() = details?.let { it.size("medium") ?: it.size("thumbnail") } ?: sourceUrl
}

@Serializable
data class MediaDetailsDto(
    val width: Int? = null,
    val height: Int? = null,
    val filesize: Long? = null,
    val sizes: kotlinx.serialization.json.JsonElement? = null,
) {
    fun size(name: String): String? =
        ((sizes as? kotlinx.serialization.json.JsonObject)?.get(name) as? kotlinx.serialization.json.JsonObject)
            ?.get("source_url")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
}

@Serializable
data class TermDto(val id: Long, val name: String = "", val slug: String = "", val count: Int = 0)
