package com.shaarli.poster.util

object UrlNormalizer {
    fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
