package com.shaarli.poster.ui

import com.shaarli.poster.data.metadata.TitleFetcher
import com.shaarli.poster.data.model.ShareStatus
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
        val viewModel = MainViewModel(FakeTitleFetcher(Result.success("Hello world")))
        viewModel.updateShareForm { it.copy(url = "https://example.com") }

        viewModel.prefillTitle()
        advanceUntilIdle()

        val state = viewModel.uiState.value.shareForm
        assertEquals("Hello world", state.title)
        assertEquals(ShareStatus.Idle, state.status)
    }

    @Test
    fun `prefill title reports error`() = runTest(dispatcher) {
        val viewModel = MainViewModel(FakeTitleFetcher(Result.failure(IllegalStateException("boom"))))
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
