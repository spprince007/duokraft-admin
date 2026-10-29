package se.duokraft.admin.data.net

import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import se.duokraft.admin.BuildConfig
import se.duokraft.admin.data.auth.SessionStore
import java.util.concurrent.TimeUnit

val WpJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

object HttpFactory {

    fun client(session: SessionStore): OkHttpClient {
        val siteHost = java.net.URI(BuildConfig.SITE_URL).host

        val auth = Interceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder()
                .header("User-Agent", "DuokraftAdmin/${BuildConfig.VERSION_NAME} (Android)")
            val creds = session.credentials
            // Only ever send the login to our own website, and only over HTTPS.
            val ours = request.url.host == siteHost && request.isHttps
            if (creds != null && ours) {
                builder.header("Authorization", Credentials.basic(creds.userLogin, creds.appPassword))
            }
            val response = chain.proceed(builder.build())
            if (response.code == 401 && creds != null && ours) session.markExpired()
            response
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(auth)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC = method, URL, status. Never headers (they contain the login).
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()
    }

    fun api(siteUrl: String, client: OkHttpClient): WpApi =
        Retrofit.Builder()
            .baseUrl(siteUrl.trimEnd('/') + "/wp-json/")
            .client(client)
            .addConverterFactory(WpJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WpApi::class.java)
}
