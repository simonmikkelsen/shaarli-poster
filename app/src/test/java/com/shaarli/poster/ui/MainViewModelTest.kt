package com.shaarli.poster.ui

import com.shaarli.poster.data.metadata.TitleFetcher
import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShareStatus
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.data.repository.PosterRepository
import com.shaarli.poster.data.repository.PostResult
import com.shaarli.poster.data.repository.PostStatus
import com.shaarli.poster.data.repository.RetryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prefill title succeeds`() = runTest(dispatcher) {
        val viewModel = MainViewModel(
            repository = FakeRepository(),
            titleFetcher = FakeTitleFetcher(Result.success("Hello world"))
        )
        viewModel.updateShareForm { it.copy(url = "https://example.com") }

        viewModel.prefillTitle()
        advanceUntilIdle()

        val state = viewModel.uiState.value.shareForm
        assertEquals("Hello world", state.title)
        assertEquals(ShareStatus.Idle, state.status)
    }

    @Test
    fun `prefill title reports error`() = runTest(dispatcher) {
        val viewModel = MainViewModel(
            repository = FakeRepository(),
            titleFetcher = FakeTitleFetcher(Result.failure(IllegalStateException("boom")))
        )
        viewModel.updateShareForm { it.copy(url = "https://example.com") }

        viewModel.prefillTitle()
        advanceUntilIdle()

        val state = viewModel.uiState.value.shareForm
        assertEquals(ShareStatus.Error, state.status)
        assertEquals("boom", state.errorMessage)
    }

    @Test
    fun `shared url loads existing link and skips title fetch`() = runTest(dispatcher) {
        var titleFetchCalls = 0
        val existingLink = LinkPayload(
            id = 42,
            url = "https://example.com",
            title = "Saved title",
            description = "Saved description",
            tags = listOf("tag1", "tag2"),
            isPrivate = true
        )
        val viewModel = MainViewModel(
            repository = FakeRepository(Result.success(existingLink)),
            titleFetcher = FakeTitleFetcher(Result.success("Unused")) { titleFetchCalls++ }
        )

        viewModel.applySharedUrl(" https://example.com ")
        advanceUntilIdle()

        val form = viewModel.uiState.value.shareForm
        assertEquals("Saved title", form.title)
        assertEquals("Saved description", form.description)
        assertEquals("tag1, tag2", form.tags)
        assertEquals(true, form.isPrivate)
        assertEquals(42, form.existingLinkId)
        assertEquals("Existing link loaded", form.infoMessage)
        assertEquals(0, titleFetchCalls)
    }

    @Test
    fun `shared url falls back to title when not found`() = runTest(dispatcher) {
        var titleFetchCalls = 0
        val repo = FakeRepository()
        val viewModel = MainViewModel(
            repository = repo,
            titleFetcher = FakeTitleFetcher(Result.success("Fetched title")) { titleFetchCalls++ }
        )

        viewModel.applySharedUrl("https://example.com/page")
        advanceUntilIdle()

        val form = viewModel.uiState.value.shareForm
        assertEquals("Fetched title", form.title)
        assertEquals(ShareStatus.Idle, form.status)
        assertEquals(null, form.existingLinkId)
        assertEquals(1, repo.findExistingLinkInvocations)
        assertEquals(1, titleFetchCalls)
    }
}

private class FakeTitleFetcher(
    private val result: Result<String>,
    private val onFetch: (() -> Unit)? = null
) : TitleFetcher {
    override suspend fun fetchTitle(url: String): Result<String> {
        onFetch?.invoke()
        return result
    }
}

private class FakeRepository(
    private val existingLinkResult: Result<LinkPayload?> = Result.success(null)
) : PosterRepository {
    var findExistingLinkInvocations = 0
    override suspend fun loadSettings(): ShaarliSettings =
        ShaarliSettings(baseUrl = "https://example.com", apiSecret = "secret")
    override suspend fun saveSettings(settings: ShaarliSettings) {}
    override suspend fun clearSettings() {}
    override suspend fun listDrafts(): List<Draft> = emptyList()
    override suspend fun saveDraft(payload: LinkPayload): Draft = Draft("1", payload, 0)
    override suspend fun removeDraft(id: String) {}
    override suspend fun retryDrafts(settings: ShaarliSettings): RetryResult = RetryResult(0, 0)
    override suspend fun postLink(settings: ShaarliSettings, payload: LinkPayload): PostResult =
        PostResult(PostStatus.Posted, null)

    override suspend fun updateLink(settings: ShaarliSettings, payload: LinkPayload): PostResult =
        PostResult(PostStatus.Posted, null)

    override suspend fun testConnection(settings: ShaarliSettings): Result<Unit> = Result.success(Unit)
    override suspend fun findExistingLink(settings: ShaarliSettings, url: String): Result<LinkPayload?> {
        findExistingLinkInvocations++
        return existingLinkResult
    }
}
