package com.shaarli.poster.data.repository

import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings
import com.shaarli.poster.data.network.NetworkStatus
import com.shaarli.poster.data.network.CreateLinkResult
import com.shaarli.poster.data.network.ShaarliClient
import com.shaarli.poster.data.storage.SettingsStore

class ShaarliRepository(
    private val settingsStore: SettingsStore,
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

    override suspend fun postLink(settings: ShaarliSettings, payload: LinkPayload): PostResult =
        postLinkInternal(settings, payload)

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
        payload: LinkPayload
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
                                handleConflict(settings, payload, createResult.existing)
                        }
                    },
                    onFailure = { error ->
                        PostResult(PostStatus.Failed, error.message ?: "Failed to post link")
                    }
                )
            },
            onFailure = { throwable ->
                PostResult(PostStatus.Failed, throwable.message ?: "Network error")
            }
        )
    }

    private suspend fun handleConflict(
        settings: ShaarliSettings,
        payload: LinkPayload,
        existing: LinkPayload
    ): PostResult {
        val linkId = existing.id ?: return PostResult(PostStatus.Failed, "Duplicate link found, but missing id")
        val updatePayload = payload.copy(id = linkId)
        val updateResult = client.updateLink(settings, updatePayload)
        return updateResult.fold(
            onSuccess = { PostResult(PostStatus.Posted, "Link updated successfully") },
            onFailure = { error -> PostResult(PostStatus.Failed, error.message ?: "Failed to update link") }
        )
    }
}
