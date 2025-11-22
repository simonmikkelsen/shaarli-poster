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
}
