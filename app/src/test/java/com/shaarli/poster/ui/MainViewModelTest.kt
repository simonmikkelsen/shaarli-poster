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
}

private class FakeTitleFetcher(
    private val result: Result<String>
) : TitleFetcher {
    override suspend fun fetchTitle(url: String): Result<String> = result
}

private class FakeRepository : PosterRepository {
    override suspend fun loadSettings(): ShaarliSettings = ShaarliSettings()
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
    override suspend fun findExistingLink(settings: ShaarliSettings, url: String): Result<LinkPayload?> =
        Result.success(null)
}
