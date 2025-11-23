package com.shaarli.poster.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlSanitizerTest {

    @Test
    fun `removes tracking params and fragments`() {
        val input = "https://example.com/path?utm_source=newsletter&foo=bar#section"
        val cleaned = UrlSanitizer.canonical(input)
        assertEquals("https://example.com/path?foo=bar", cleaned)
    }

    @Test
    fun `canonical removes root trailing slash`() {
        val cleaned = UrlSanitizer.canonical("https://example.com/")
        assertEquals("https://example.com", cleaned)
    }

    @Test
    fun `stripQueryAndFragment drops query`() {
        val cleaned = UrlSanitizer.stripQueryAndFragment("https://example.com/path?foo=bar#frag")
        assertEquals("https://example.com/path", cleaned)
    }

    @Test
    fun `returns trimmed when url cannot be parsed`() {
        val input = "  not-a-url "
        val cleaned = UrlSanitizer.cleanup(input)
        assertEquals("not-a-url", cleaned)
    }
}
