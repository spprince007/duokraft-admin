package se.duokraft.admin.data

import android.content.Context
import okhttp3.OkHttpClient
import se.duokraft.admin.BuildConfig
import se.duokraft.admin.data.auth.AuthRepository
import se.duokraft.admin.data.auth.SessionStore
import se.duokraft.admin.data.net.HttpFactory
import se.duokraft.admin.data.net.WpApi
import se.duokraft.admin.data.repo.ContentRepository
import se.duokraft.admin.data.repo.ImageCompressor
import se.duokraft.admin.data.repo.MediaRepository
import se.duokraft.admin.data.repo.SiteRepository

/** Manual dependency container: one instance of each service for the whole app. */
class AppContainer(context: Context) {
    val siteUrl: String = BuildConfig.SITE_URL
    val session = SessionStore(context)
    val httpClient: OkHttpClient = HttpFactory.client(session)
    val api: WpApi = HttpFactory.api(siteUrl, httpClient)

    val auth = AuthRepository(siteUrl, BuildConfig.WP_APP_ID, session, api)
    val site = SiteRepository(api)
    val content = ContentRepository(api)
    val media = MediaRepository(api, ImageCompressor(context))
}
