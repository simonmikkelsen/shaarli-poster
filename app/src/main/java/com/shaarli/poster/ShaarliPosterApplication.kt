package com.shaarli.poster

import android.app.Application
import android.content.Context
import com.shaarli.poster.data.network.NetworkStatus
import com.shaarli.poster.data.network.ShaarliClient
import com.shaarli.poster.data.repository.ShaarliRepository
import com.shaarli.poster.data.storage.SettingsStore
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val cookieManager = CookieManager()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .addInterceptor(logging)
        .build()

    val repository = ShaarliRepository(
        settingsStore = SettingsStore(context),
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
