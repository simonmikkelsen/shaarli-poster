package com.shaarli.poster.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlNormalizerTest {

    @Test
    fun `adds https when missing`() {
        val normalized = UrlNormalizer.normalizeBaseUrl("example.com")
        assertEquals("https://example.com/", normalized)
    }

    @Test
    fun `preserves existing scheme and adds trailing slash`() {
        val normalized = UrlNormalizer.normalizeBaseUrl("http://demo.local")
        assertEquals("http://demo.local/", normalized)
    }

    @Test
    fun `returns empty for empty input`() {
        val normalized = UrlNormalizer.normalizeBaseUrl("   ")
        assertEquals("", normalized)
    }
}
