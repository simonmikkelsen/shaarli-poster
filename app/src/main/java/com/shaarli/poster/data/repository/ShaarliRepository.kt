package com.shaarli.poster.data.repository

import android.util.Log
import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.data.network.NetworkStatus
import com.shaarli.poster.data.network.ShaarliClient
import com.shaarli.poster.data.storage.DraftStore
import com.shaarli.poster.data.storage.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ShaarliRepository(
    private val settingsStore: SettingsStore,
    private val draftStore: DraftStore,
    private val client: ShaarliClient,
    private val networkStatus: NetworkStatus
) : PosterRepository {

    override suspend fun loadSettings(): ShaarliSettings = settingsStore.load()

    override suspend fun saveSettings(settings: ShaarliSettings) {
        settingsStore.save(settings)
    }

    override suspend fun clearSettings() {
        settingsStore.clear()
    }

    override suspend fun listDrafts(): List<Draft> = draftStore.list()

    override suspend fun saveDraft(payload: LinkPayload): Draft = draftStore.saveDraft(payload)

    override suspend fun removeDraft(id: String) {
        draftStore.removeDraft(id)
    }

    override suspend fun retryDrafts(settings: ShaarliSettings): RetryResult = withContext(Dispatchers.IO) {
        val drafts = draftStore.list()
        var posted = 0
        val remainingDrafts = mutableListOf<Draft>()

        drafts.forEach { draft ->
            val result = postLinkInternal(settings, draft.payload, queueOnFail = false)
            if (result.status == PostStatus.Posted) {
                posted++
            } else {
                remainingDrafts.add(draft)
            }
        }

        draftStore.replaceAll(remainingDrafts)
        RetryResult(posted = posted, remaining = remainingDrafts.size)
    }

    override suspend fun postLink(settings: ShaarliSettings, payload: LinkPayload): PostResult =
        postLinkInternal(settings, payload, queueOnFail = true)

    override suspend fun testConnection(settings: ShaarliSettings): Result<Unit> {
        if (!networkStatus.isOnline()) {
            return Result.failure(IllegalStateException("Offline"))
        }
        return client.validate(settings)
    }

    private suspend fun postLinkInternal(
        settings: ShaarliSettings,
        payload: LinkPayload,
        queueOnFail: Boolean
    ): PostResult {
        val response = runCatching { client.createLink(settings, payload) }
        return response.fold(
            onSuccess = { result ->
                result.fold(
                    onSuccess = { PostResult(PostStatus.Posted, "Link posted successfully") },
                    onFailure = { error ->
                        if (queueOnFail) {
                            draftStore.saveDraft(payload)
                        }
                        PostResult(PostStatus.Failed, error.message ?: "Failed to post link")
                    }
                )
            },
            onFailure = { throwable ->
                val message = throwable.message ?: "Network error"
                if (queueOnFail && throwable is IOException) {
                    val draft = draftStore.saveDraft(payload)
                    PostResult(PostStatus.Queued, "Network issue; queued draft ${draft.id.take(8)}")
                } else {
                    PostResult(PostStatus.Failed, message)
                }
            }
        )
    }
}
