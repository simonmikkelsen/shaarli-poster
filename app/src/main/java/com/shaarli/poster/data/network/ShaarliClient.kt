package com.shaarli.poster.data.network

import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.util.JwtTokenProvider
import com.shaarli.poster.util.UrlNormalizer
import com.shaarli.poster.util.UrlSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class CreateLinkResult {
    data object Created : CreateLinkResult()
    data class Conflict(val existing: LinkPayload) : CreateLinkResult()
}

class ShaarliClient(
    private val okHttpClient: OkHttpClient,
    private val jwtTokenProvider: JwtTokenProvider = JwtTokenProvider()
) {

    suspend fun validate(settings: ShaarliSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val token = jwtTokenProvider.generate(settings.apiSecret)
            ?: return@withContext Result.failure(IllegalArgumentException("Missing API secret"))
        val url = buildUrl(settings.baseUrl, "api/v1/info") ?: return@withContext Result.failure(
            IllegalArgumentException("Invalid base URL")
        )
        val request = Request.Builder()
            .url(url)
            .get()
            .withAuth(token)
            .build()
        return@withContext executeRequest(request)
    }

    suspend fun createLink(settings: ShaarliSettings, payload: LinkPayload): Result<CreateLinkResult> =
        withContext(Dispatchers.IO) {
            val token = jwtTokenProvider.generate(settings.apiSecret)
                ?: return@withContext Result.failure(IllegalArgumentException("Missing API secret"))
            val url = buildUrl(settings.baseUrl, "api/v1/links") ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid base URL")
            )
            val json = JSONObject().apply {
                put("url", payload.url)
                put("title", payload.title)
                put("description", payload.description)
                put("tags", JSONArray(payload.tags))
                put("private", payload.isPrivate)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .withAuth(token)
                .build()
            return@withContext try {
                okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Result.success(CreateLinkResult.Created)
                        response.code == 409 -> {
                            val bodyString = response.body?.string().orEmpty()
                            val existing = JSONObject(bodyString).toLinkPayload()
                            if (existing.id == null) {
                                Result.failure(IllegalStateException("HTTP 409 (missing link id)"))
                            } else {
                                Result.success(CreateLinkResult.Conflict(existing))
                            }
                        }
                        else -> Result.failure(IllegalStateException("HTTP ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateLink(settings: ShaarliSettings, payload: LinkPayload): Result<Unit> =
        withContext(Dispatchers.IO) {
            val token = jwtTokenProvider.generate(settings.apiSecret)
                ?: return@withContext Result.failure(IllegalArgumentException("Missing API secret"))
            val linkId = payload.id ?: return@withContext Result.failure(IllegalArgumentException("Missing link id"))
            val url = buildUrl(settings.baseUrl, "api/v1/links/$linkId")
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid base URL"))
            val json = JSONObject().apply {
                put("url", payload.url)
                put("title", payload.title)
                put("description", payload.description)
                put("tags", JSONArray(payload.tags))
                put("private", payload.isPrivate)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .withAuth(token)
                .build()
            return@withContext executeRequest(request)
        }

    suspend fun findLinkByUrl(settings: ShaarliSettings, targetUrl: String): Result<LinkPayload?> =
        withContext(Dispatchers.IO) {
            val token = jwtTokenProvider.generate(settings.apiSecret)
                ?: return@withContext Result.failure(IllegalArgumentException("Missing API secret"))
            val canonicalTarget = UrlSanitizer.canonical(targetUrl)
            val parsedTarget = targetUrl.trim().toHttpUrlOrNull()
            val searchTerm = parsedTarget?.let { "${it.host}${it.encodedPath}".trimEnd('/') }
                ?: UrlSanitizer.stripQueryAndFragment(targetUrl)
            val url = buildUrl(settings.baseUrl, "api/v1/links")
                ?.newBuilder()
                ?.addQueryParameter("searchterm", searchTerm)
                ?.addQueryParameter("limit", "20")
                ?.build()
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid base URL"))
            val request = Request.Builder()
                .url(url)
                .get()
                .withAuth(token)
                .build()

            return@withContext try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(IllegalStateException("HTTP ${response.code}"))
                    }
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    val targetBase = UrlSanitizer.stripQueryAndFragment(targetUrl)
                    val targetNoScheme = canonicalTarget.removePrefix("https://").removePrefix("http://")

                    val matchesTarget = (0 until array.length())
                        .mapNotNull { idx ->
                            val payload = array.getJSONObject(idx).toLinkPayload()
                            val payloadCanonical = UrlSanitizer.canonical(payload.url)
                            val payloadBase = UrlSanitizer.stripQueryAndFragment(payload.url)
                            val payloadNoScheme =
                                payloadCanonical.removePrefix("https://").removePrefix("http://")
                            when {
                                payloadCanonical == canonicalTarget -> payload to true
                                payloadBase == targetBase -> payload to false
                                payloadNoScheme == targetNoScheme -> payload to false
                                else -> null
                            }
                        }
                    val exact = matchesTarget.firstOrNull { it.second }?.first
                    val fallback = matchesTarget.firstOrNull()?.first
                    Result.success(exact ?: fallback)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildUrl(base: String, path: String): HttpUrl? {
        val normalized = UrlNormalizer.normalizeBaseUrl(base)
        val baseHttp = normalized.toHttpUrlOrNull() ?: return null
        return baseHttp.newBuilder()
            .addEncodedPathSegments(path.trimStart('/'))
            .build()
    }

    private fun Request.Builder.withAuth(token: String): Request.Builder =
        header("Authorization", "Bearer $token")

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

    private fun JSONObject.toLinkPayload(): LinkPayload {
        val tagsArray = optJSONArray("tags") ?: JSONArray()
        val tags = (0 until tagsArray.length()).mapNotNull { tagsArray.optString(it) }.filter { it.isNotBlank() }
        return LinkPayload(
            id = if (has("id")) optInt("id") else null,
            url = optString("url"),
            title = optString("title"),
            description = optString("description"),
            tags = tags,
            isPrivate = optBoolean("private")
        )
    }
}
