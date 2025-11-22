package com.shaarli.poster

import android.app.Application
import android.content.Context
import com.shaarli.poster.data.network.NetworkStatus
import com.shaarli.poster.data.network.ShaarliClient
import com.shaarli.poster.data.repository.ShaarliRepository
import com.shaarli.poster.data.storage.DraftStore
import com.shaarli.poster.data.storage.SettingsStore
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.net.CookieManager
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val cookieManager = CookieManager()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .addInterceptor(logging)
        .build()

    private val draftsFile = File(context.filesDir, "drafts.json")

    val repository = ShaarliRepository(
        settingsStore = SettingsStore(context),
        draftStore = DraftStore(draftsFile),
        client = ShaarliClient(okHttpClient),
        networkStatus = NetworkStatus(context)
    )
}

class ShaarliPosterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
