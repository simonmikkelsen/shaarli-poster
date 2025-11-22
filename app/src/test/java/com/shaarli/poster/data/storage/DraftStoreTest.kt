package com.shaarli.poster.data.storage

import com.shaarli.poster.data.model.LinkPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DraftStoreTest {

    @Test
    fun `save and list drafts`() = runTest {
        val tempFile = File.createTempFile("drafts", ".json")
        tempFile.deleteOnExit()
        val store = DraftStore(tempFile)
        val payload = LinkPayload(
            url = "https://example.com",
            title = "Example",
            description = "Desc",
            tags = listOf("one", "two"),
            isPrivate = false
        )

        val saved = store.saveDraft(payload)
        val drafts = store.list()

        assertEquals(1, drafts.size)
        assertEquals(saved.id, drafts.first().id)
        assertEquals("Example", drafts.first().payload.title)
    }

    @Test
    fun `remove draft`() = runTest {
        val tempFile = File.createTempFile("drafts", ".json")
        tempFile.deleteOnExit()
        val store = DraftStore(tempFile)
        val payload = LinkPayload(
            url = "https://example.com",
            title = "Example",
            description = "",
            tags = emptyList(),
            isPrivate = false
        )

        val saved = store.saveDraft(payload)
        assertTrue(store.list().isNotEmpty())
        store.removeDraft(saved.id)
        assertTrue(store.list().isEmpty())
    }
}
