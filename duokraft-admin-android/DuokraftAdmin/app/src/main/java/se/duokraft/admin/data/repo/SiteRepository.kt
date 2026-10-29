package se.duokraft.admin.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.core.apiCall
import se.duokraft.admin.data.net.ContentTypeDto
import se.duokraft.admin.data.net.DashboardDto
import se.duokraft.admin.data.net.SiteDto
import se.duokraft.admin.data.net.WpApi

/** Site info (name, user, which content types exist) and the dashboard summary. */
class SiteRepository(private val api: WpApi) {
    private val _site = MutableStateFlow<SiteDto?>(null)
    val site: StateFlow<SiteDto?> = _site

    fun remember(site: SiteDto) {
        _site.value = site
    }

    fun forget() {
        _site.value = null
    }

    suspend fun refreshSite(): ApiResult<SiteDto> =
        apiCall { api.site() }.also { if (it is ApiResult.Ok) _site.value = it.value }

    suspend fun dashboard(): ApiResult<DashboardDto> = apiCall { api.dashboard() }

    /** Looks up a content type by slug (e.g. "page", "post" or a custom post type). */
    fun type(slug: String): ContentTypeDto? = _site.value?.contentTypes?.firstOrNull { it.slug == slug }
}
