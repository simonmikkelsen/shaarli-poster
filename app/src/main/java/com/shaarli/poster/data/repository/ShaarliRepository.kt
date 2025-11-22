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
        return if (settings.authType == com.shaarli.poster.data.model.AuthType.Session) {
            client.login(settings)
        } else {
            client.validate(settings)
        }
    }

    private suspend fun postLinkInternal(
        settings: ShaarliSettings,
        payload: LinkPayload,
        queueOnFail: Boolean
    ): PostResult {
        if (!networkStatus.isOnline()) {
            val draft = if (queueOnFail) draftStore.saveDraft(payload) else null
            return PostResult(
                status = PostStatus.Queued,
                message = if (draft != null) "Offline; queued draft ${draft.id.take(8)}" else "Offline"
            )
        }

        val authResult = if (settings.authType == com.shaarli.poster.data.model.AuthType.Session) {
            client.login(settings)
        } else {
            Result.success(Unit)
        }
        if (authResult.isFailure) {
            if (queueOnFail) {
                draftStore.saveDraft(payload)
            }
            val message = authResult.exceptionOrNull()?.message ?: "Authentication failed"
            return PostResult(PostStatus.Failed, message)
        }

        val response = client.createLink(settings, payload)
        return if (response.isSuccess) {
            PostResult(PostStatus.Posted, "Link posted successfully")
        } else {
            val message = response.exceptionOrNull()?.message ?: "Failed to post link"
            if (queueOnFail) {
                draftStore.saveDraft(payload)
            }
            PostResult(PostStatus.Failed, message)
        }
    }
}
