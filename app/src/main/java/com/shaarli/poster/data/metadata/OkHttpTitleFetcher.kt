package com.shaarli.poster.data.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.min

class OkHttpTitleFetcher(
    private val client: OkHttpClient = defaultClient
) : TitleFetcher {

    override suspend fun fetchTitle(url: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        val httpUrl = trimmed.toHttpUrlOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(IllegalStateException("Failed with HTTP ${response.code}"))
                }

                val body = response.body ?: return@use Result.failure(IllegalStateException("Empty body"))
                val source = body.source()
                source.request(MAX_BYTES.toLong())
                val buffer = source.buffer.clone()
                val text = buffer.readUtf8(min(buffer.size, MAX_BYTES.toLong()))
                val title = TITLE_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim()
                if (title.isNullOrEmpty()) {
                    Result.failure(IllegalStateException("Title not found"))
                } else {
                    Result.success(title)
                }
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    companion object {
        private const val MAX_BYTES = 512 * 1024
        private val TITLE_REGEX = Regex("(?is)<title\\b[^>]*>(.*?)</title>")

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
