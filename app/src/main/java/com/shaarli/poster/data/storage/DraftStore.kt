package com.shaarli.poster.data.storage

import com.shaarli.poster.data.model.Draft
import com.shaarli.poster.data.model.LinkPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class DraftStore(
    private val file: File
) {
    private val mutex = Mutex()

    suspend fun list(): List<Draft> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readDraftsUnsafe()
        }
    }

    suspend fun saveDraft(payload: LinkPayload): Draft = withContext(Dispatchers.IO) {
        mutex.withLock {
            val drafts = readDraftsUnsafe().toMutableList()
            val draft = Draft(
                id = UUID.randomUUID().toString(),
                payload = payload,
                createdAtEpochMillis = System.currentTimeMillis()
            )
            drafts.add(draft)
            writeDrafts(drafts)
            draft
        }
    }

    suspend fun removeDraft(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val drafts = readDraftsUnsafe().filterNot { it.id == id }
            writeDrafts(drafts)
        }
    }

    suspend fun replaceAll(drafts: List<Draft>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeDrafts(drafts)
        }
    }

    private fun writeDrafts(drafts: List<Draft>) {
        val jsonArray = JSONArray()
        drafts.forEach { draft ->
            jsonArray.put(draft.toJson())
        }
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }
        file.writeText(jsonArray.toString())
    }

    private fun JSONObject.toDraft(): Draft? {
        val id = optString("id")
        val created = optLong("createdAt", 0L)
        val payloadJson = optJSONObject("payload") ?: return null
        val url = payloadJson.optString("url")
        if (id.isBlank() || url.isBlank()) return null
        val tagsArray = payloadJson.optJSONArray("tags") ?: JSONArray()
        val tags = (0 until tagsArray.length()).mapNotNull { tagsArray.optString(it) }.filter { it.isNotBlank() }
        val payload = LinkPayload(
            url = url,
            title = payloadJson.optString("title"),
            description = payloadJson.optString("description"),
            tags = tags,
            isPrivate = payloadJson.optBoolean("isPrivate")
        )
        return Draft(
            id = id,
            payload = payload,
            createdAtEpochMillis = created
        )
    }

    private fun Draft.toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("createdAt", createdAtEpochMillis)
        val payloadJson = JSONObject().apply {
            put("url", payload.url)
            put("title", payload.title)
            put("description", payload.description)
            put("tags", JSONArray(payload.tags))
            put("isPrivate", payload.isPrivate)
        }
        json.put("payload", payloadJson)
        return json
    }

    private fun readDraftsUnsafe(): List<Draft> {
        if (!file.exists()) return emptyList()
        val content = file.readText()
        if (content.isBlank()) return emptyList()
        val jsonArray = JSONArray(content)
        return (0 until jsonArray.length()).mapNotNull { index ->
            jsonArray.optJSONObject(index)?.toDraft()
        }
    }
}
