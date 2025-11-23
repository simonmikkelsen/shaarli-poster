package com.shaarli.poster.data.repository

import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.data.network.NetworkStatus
import com.shaarli.poster.data.network.CreateLinkResult
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

    override suspend fun findExistingLink(settings: ShaarliSettings, url: String): Result<LinkPayload?> {
        return client.findLinkByUrl(settings, url)
    }

    override suspend fun updateLink(settings: ShaarliSettings, payload: LinkPayload): PostResult {
        val response = runCatching { client.updateLink(settings, payload) }
        return response.fold(
            onSuccess = { result ->
                result.fold(
                    onSuccess = { PostStatus.Posted to "Link updated successfully" },
                    onFailure = { error -> PostStatus.Failed to (error.message ?: "Failed to update link") }
                )
            },
            onFailure = { throwable ->
                PostStatus.Failed to (throwable.message ?: "Network error")
            }
        ).let { (status, message) -> PostResult(status, message) }
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
                    onSuccess = { createResult ->
                        when (createResult) {
                            CreateLinkResult.Created ->
                                PostResult(PostStatus.Posted, "Link posted successfully")
                            is CreateLinkResult.Conflict ->
                                handleConflict(settings, payload, createResult.existing, queueOnFail)
                        }
                    },
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

    private suspend fun handleConflict(
        settings: ShaarliSettings,
        payload: LinkPayload,
        existing: LinkPayload,
        queueOnFail: Boolean
    ): PostResult {
        val linkId = existing.id ?: return PostResult(PostStatus.Failed, "Duplicate link found, but missing id")
        val updatePayload = payload.copy(id = linkId)
        val updateResult = client.updateLink(settings, updatePayload)
        return updateResult.fold(
            onSuccess = { PostResult(PostStatus.Posted, "Link updated successfully") },
            onFailure = { error ->
                if (queueOnFail && error is IOException) {
                    val draft = draftStore.saveDraft(updatePayload)
                    PostResult(PostStatus.Queued, "Network issue; queued draft ${draft.id.take(8)}")
                } else {
                    PostResult(PostStatus.Failed, error.message ?: "Failed to update link")
                }
            }
        )
    }
}
