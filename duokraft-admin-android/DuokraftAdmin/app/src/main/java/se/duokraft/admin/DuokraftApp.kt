package se.duokraft.admin

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import se.duokraft.admin.data.AppContainer

class DuokraftApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    // Coil uses the authenticated client so private/draft media previews load too, and can show SVG icons.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { container.httpClient }
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()
}
