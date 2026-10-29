package se.duokraft.admin.data.auth

import android.net.Uri
import android.os.Build
import se.duokraft.admin.core.ApiResult
import se.duokraft.admin.core.apiCall
import se.duokraft.admin.data.net.SiteDto
import se.duokraft.admin.data.net.WpApi

/**
 * Login uses WordPress' built-in "Authorize Application" flow:
 * 1. The app opens the website's own login page in a browser tab.
 * 2. The user logs in on WordPress and approves "Duokraft Admin".
 * 3. WordPress redirects to duokraftadmin://auth with a revocable application password.
 * The user's real password never passes through the app.
 */
class AuthRepository(
    private val siteUrl: String,
    private val appId: String,
    private val session: SessionStore,
    private val api: WpApi,
) {
    fun authorizeUrl(): Uri {
        val state = session.newLoginState()
        val callback = "$CALLBACK?state=$state"
        return Uri.parse(siteUrl).buildUpon()
            .appendEncodedPath("wp-admin/authorize-application.php")
            .appendQueryParameter("app_name", "Duokraft Admin (${Build.MANUFACTURER} ${Build.MODEL})")
            .appendQueryParameter("app_id", appId)
            .appendQueryParameter("success_url", callback)
            .appendQueryParameter("reject_url", "$callback&rejected=1")
            .build()
    }

    /** Handles the duokraftadmin://auth redirect. Returns the site info on success. */
    suspend fun handleCallback(uri: Uri): ApiResult<SiteDto> {
        val expectedState = session.consumeLoginState()
        if (expectedState == null || uri.getQueryParameter("state") != expectedState) {
            return ApiResult.Err("This login link is not valid. Please start the login again.")
        }
        if (uri.getQueryParameter("rejected") != null) {
            return ApiResult.Err("Login was cancelled on the website.")
        }
        val returnedSite = uri.getQueryParameter("site_url").orEmpty()
        if (Uri.parse(returnedSite).host != Uri.parse(siteUrl).host) {
            return ApiResult.Err("The login came from a different website and was refused.")
        }
        val user = uri.getQueryParameter("user_login")
        val password = uri.getQueryParameter("password")
        if (user.isNullOrBlank() || password.isNullOrBlank()) {
            return ApiResult.Err("The website did not return a login. Please try again.")
        }

        session.save(user, password)
        val result = apiCall { api.site() }
        if (result is ApiResult.Err) session.clear()
        return result
    }

    /** Revokes this device's application password on the website, then forgets it locally. */
    suspend fun logout() {
        if (session.isLoggedIn) {
            apiCall {
                val current = api.introspectAppPassword()
                api.deleteAppPassword(current.uuid)
            }
        }
        session.clear()
    }

    companion object {
        const val CALLBACK = "duokraftadmin://auth"
    }
}
