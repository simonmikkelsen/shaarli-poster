package com.shaarli.poster.data.network

import com.shaarli.poster.data.model.AuthType
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ShaarliClient(
    private val okHttpClient: OkHttpClient
) {

    suspend fun validate(settings: ShaarliSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val url = buildUrl(settings.baseUrl, "api/v1/info") ?: return@withContext Result.failure(
            IllegalArgumentException("Invalid base URL")
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .withAuth(settings)
            .build()
        return@withContext executeRequest(request)
    }

    suspend fun login(settings: ShaarliSettings): Result<Unit> = withContext(Dispatchers.IO) {
        if (settings.authType != AuthType.Session) return@withContext Result.success(Unit)
        val url = buildUrl(settings.baseUrl, "login") ?: return@withContext Result.failure(
            IllegalArgumentException("Invalid base URL")
        )
        val formBody = FormBody.Builder()
            .add("login", settings.username)
            .add("password", settings.password)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()
        return@withContext executeRequest(request)
    }

    suspend fun createLink(settings: ShaarliSettings, payload: LinkPayload): Result<Unit> =
        withContext(Dispatchers.IO) {
            val url = buildUrl(settings.baseUrl, "api/v1/links") ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid base URL")
            )
            val json = JSONObject().apply {
                put("url", payload.url)
                put("title", payload.title)
                put("description", payload.description)
                put("tags", payload.tags)
                put("private", payload.isPrivate)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .withAuth(settings)
                .build()
            return@withContext executeRequest(request)
        }

    private fun buildUrl(base: String, path: String): HttpUrl? {
        val normalized = UrlNormalizer.normalizeBaseUrl(base)
        val baseHttp = normalized.toHttpUrlOrNull() ?: return null
        return baseHttp.newBuilder()
            .addEncodedPathSegments(path.trimStart('/'))
            .build()
    }

    private fun Request.Builder.withAuth(settings: ShaarliSettings): Request.Builder {
        return when (settings.authType) {
            AuthType.Token -> {
                if (settings.apiSecret.isNotBlank()) {
                    header("X-Api-Token", settings.apiSecret)
                } else {
                    this
                }
            }
            AuthType.Session -> this
        }
    }

    private fun executeRequest(request: Request): Result<Unit> {
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
