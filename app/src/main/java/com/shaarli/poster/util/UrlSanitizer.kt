package com.shaarli.poster.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normalizes bookmark URLs similarly to Shaarli's server-side cleanup:
 * - strips known tracking parameters and fragments
 * - lowercases host/scheme (handled by HttpUrl)
 * - trims trailing slash for root URLs to improve substring searches
 */
object UrlSanitizer {
    private val noisyPrefixes = listOf(
        "action_object_map",
        "action_ref_map",
        "action_type_map",
        "fb_",
        "fb",
        "PHPSESSID",
        "__scoop",
        "utm_",
        "xtor",
        "campaign_"
    )

    fun canonical(url: String): String {
        val parsed = url.trim().toHttpUrlOrNull() ?: return url.trim()
        val cleaned = parsed
            .newBuilder()
            .fragment(null)
            .also { builder ->
                val names = parsed.queryParameterNames
                val toRemove = names.filter { name -> noisyPrefixes.any { prefix -> name.startsWith(prefix) } }
                toRemove.forEach { name -> builder.removeAllQueryParameters(name) }
            }
            .build()
        return cleaned.toString().removeRootTrailingSlash(cleaned.encodedPath)
    }

    fun stripQueryAndFragment(url: String): String {
        val parsed = url.trim().toHttpUrlOrNull() ?: return url.trim()
        val cleaned = parsed.newBuilder()
            .fragment(null)
            .query(null)
            .build()
        return cleaned.toString().removeRootTrailingSlash(cleaned.encodedPath)
    }

    // Backwards-compatible alias used in tests and call sites.
    fun cleanup(url: String): String = canonical(url)

    private fun String.removeRootTrailingSlash(path: String): String {
        return if (path == "/") removeSuffix("/") else this
    }
}
