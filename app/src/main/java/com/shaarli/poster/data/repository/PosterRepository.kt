package com.shaarli.poster.data.repository

import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import com.shaarli.poster.data.model.ShaarliSettings

enum class PostStatus { Posted, Queued, Failed }

data class PostResult(
    val status: PostStatus,
    val message: String? = null
)

data class RetryResult(
    val posted: Int,
    val remaining: Int
)

interface PosterRepository {
    suspend fun loadSettings(): ShaarliSettings
    suspend fun saveSettings(settings: ShaarliSettings)
    suspend fun clearSettings()
    suspend fun listDrafts(): List<Draft>
    suspend fun saveDraft(payload: LinkPayload): Draft
    suspend fun removeDraft(id: String)
    suspend fun retryDrafts(settings: ShaarliSettings): RetryResult
    suspend fun postLink(settings: ShaarliSettings, payload: LinkPayload): PostResult
    suspend fun testConnection(settings: ShaarliSettings): Result<Unit>
}
