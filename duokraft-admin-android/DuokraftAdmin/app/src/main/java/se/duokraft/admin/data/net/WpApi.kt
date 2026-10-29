package se.duokraft.admin.data.net

import kotlinx.serialization.json.JsonObject
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Core WordPress REST API (wp/v2) plus the Duokraft App API plugin (duokraft-app/v1).
 * Content endpoints take the post type's namespace and rest_base so custom post types work too.
 */
interface WpApi {

    // ---- Duokraft App API plugin ----
    @GET("duokraft-app/v1/site")
    suspend fun site(): SiteDto

    @GET("duokraft-app/v1/dashboard")
    suspend fun dashboard(@Query("recent") recent: Int = 5): DashboardDto

    // ---- Login housekeeping ----
    @GET("wp/v2/users/me/application-passwords/introspect")
    suspend fun introspectAppPassword(): AppPasswordDto

    @DELETE("wp/v2/users/me/application-passwords/{uuid}")
    suspend fun deleteAppPassword(@Path("uuid") uuid: String): Response<Unit>

    // ---- Content (pages, posts, custom post types) ----
    @GET("{ns}/{base}")
    suspend fun listContent(
        @Path("ns", encoded = true) ns: String,
        @Path("base", encoded = true) base: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 20,
        @Query("search") search: String? = null,
        @Query("status") status: String = "publish,draft,pending,future,private",
        @Query("orderby") orderBy: String = "modified",
        @Query("context") context: String = "edit",
        @Query("_embed") embed: String = "wp:featuredmedia",
    ): Response<List<PostDto>>

    @GET("{ns}/{base}/{id}")
    suspend fun getContent(
        @Path("ns", encoded = true) ns: String,
        @Path("base", encoded = true) base: String,
        @Path("id") id: Long,
        @Query("context") context: String = "edit",
        @Query("_embed") embed: String = "wp:featuredmedia",
    ): PostDto

    @POST("{ns}/{base}")
    suspend fun createContent(
        @Path("ns", encoded = true) ns: String,
        @Path("base", encoded = true) base: String,
        @Body body: JsonObject,
    ): PostDto

    @POST("{ns}/{base}/{id}")
    suspend fun updateContent(
        @Path("ns", encoded = true) ns: String,
        @Path("base", encoded = true) base: String,
        @Path("id") id: Long,
        @Body body: JsonObject,
    ): PostDto

    /** Without force=true WordPress moves the item to the trash. */
    @DELETE("{ns}/{base}/{id}")
    suspend fun trashContent(
        @Path("ns", encoded = true) ns: String,
        @Path("base", encoded = true) base: String,
        @Path("id") id: Long,
    ): PostDto

    // ---- Terms (categories, tags, custom taxonomies) ----
    @GET("wp/v2/{base}")
    suspend fun listTerms(
        @Path("base", encoded = true) base: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("search") search: String? = null,
        @Query("hide_empty") hideEmpty: Boolean = false,
    ): Response<List<TermDto>>

    @POST("wp/v2/{base}")
    suspend fun createTerm(@Path("base", encoded = true) base: String, @Body body: JsonObject): TermDto

    // ---- Media ----
    @GET("wp/v2/media")
    suspend fun listMedia(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 30,
        @Query("search") search: String? = null,
        @Query("media_type") mediaType: String? = "image",
        @Query("context") context: String = "edit",
    ): Response<List<MediaDto>>

    @GET("wp/v2/media/{id}")
    suspend fun getMedia(@Path("id") id: Long, @Query("context") context: String = "edit"): MediaDto

    @POST("wp/v2/media")
    suspend fun uploadMedia(
        @Header("Content-Disposition") contentDisposition: String,
        @Body file: RequestBody,
    ): MediaDto

    @POST("wp/v2/media/{id}")
    suspend fun updateMedia(@Path("id") id: Long, @Body body: JsonObject): MediaDto

    /** Media has no trash in WordPress, so deleting requires force=true. */
    @DELETE("wp/v2/media/{id}")
    suspend fun deleteMedia(@Path("id") id: Long, @Query("force") force: Boolean = true): Response<Unit>
}
