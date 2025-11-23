package com.shaarli.poster.data.network

import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShaarliClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ShaarliClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = ShaarliClient(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `create link posts payload with token header`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val payload = LinkPayload(
            url = "https://example.com",
            title = "Example",
            description = "desc",
            tags = listOf("tag1"),
            isPrivate = true
        )

        val result = client.createLink(settings, payload)
        assert(result.isSuccess)
        assert(result.getOrNull() is CreateLinkResult.Created)

        val request = server.takeRequest()
        assertEquals("/api/v1/links", request.path)
        val authHeader = request.getHeader("Authorization")
        assert(authHeader?.startsWith("Bearer ") == true)
    }

    @Test
    fun `validate calls info endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val result = client.validate(settings)
        assert(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("/api/v1/info", request.path)
        val authHeader = request.getHeader("Authorization")
        assert(authHeader?.startsWith("Bearer ") == true)
    }

    @Test
    fun `create link returns conflict with existing link payload`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody(
                    """
                    {
                      "id": 42,
                      "url": "https://example.com",
                      "title": "Existing",
                      "description": "desc",
                      "tags": ["tag1", "tag2"],
                      "private": false
                    }
                    """.trimIndent()
                )
        )
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val payload = LinkPayload(
            url = "https://example.com",
            title = "Example",
            description = "desc",
            tags = listOf("tag1"),
            isPrivate = true
        )

        val result = client.createLink(settings, payload)
        val conflict = result.getOrNull() as? CreateLinkResult.Conflict
        assertEquals(42, conflict?.existing?.id)
        assertEquals("https://example.com", conflict?.existing?.url)
    }

    @Test
    fun `findLinkByUrl strips tracking params and returns link`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": 99,
                        "url": "https://example.com/path",
                        "title": "Existing",
                        "description": "desc",
                        "tags": ["tag1"],
                        "private": true
                      }
                    ]
                    """.trimIndent()
                )
        )
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val result = client.findLinkByUrl(
            settings,
            "https://example.com/path?utm_source=newsletter&utm_medium=email#section"
        )
        val request = server.takeRequest()
        val searchterm = request.requestUrl?.queryParameter("searchterm")
        assertEquals("example.com/path", searchterm)
        val link = result.getOrNull()
        assertEquals(99, link?.id)
        assertEquals(listOf("tag1"), link?.tags)
        assertEquals(true, link?.isPrivate)
    }

    @Test
    fun `findLinkByUrl matches even when stored link lacks query params`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": 100,
                        "url": "https://example.com/path",
                        "title": "Existing",
                        "description": "desc",
                        "tags": [],
                        "private": false
                      }
                    ]
                    """.trimIndent()
                )
        )
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val result = client.findLinkByUrl(
            settings,
            "https://example.com/path?foo=bar"
        )
        val link = result.getOrNull()
        assertEquals(100, link?.id)
        assertEquals("Existing", link?.title)
    }

    @Test
    fun `findLinkByUrl matches when schemes differ`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    [
                      {
                        "id": 101,
                        "url": "http://example.com/path",
                        "title": "Existing HTTP",
                        "description": "desc",
                        "tags": [],
                        "private": false
                      }
                    ]
                    """.trimIndent()
                )
        )
        val settings = ShaarliSettings(
            baseUrl = server.url("/").toString(),
            apiSecret = "secret"
        )
        val result = client.findLinkByUrl(
            settings,
            "https://example.com/path"
        )
        val link = result.getOrNull()
        assertEquals(101, link?.id)
        assertEquals("Existing HTTP", link?.title)
    }
}
