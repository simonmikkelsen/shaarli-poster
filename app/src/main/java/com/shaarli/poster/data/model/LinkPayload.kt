package com.shaarli.poster.data.model

data class LinkPayload(
    val id: Int? = null,
    val url: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val isPrivate: Boolean
) {
    companion object {
        fun fromState(state: ShareFormState): LinkPayload {
            val parsedTags = state.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return LinkPayload(
                url = state.url.trim(),
                title = state.title.trim(),
                description = state.description.trim(),
                tags = parsedTags,
                isPrivate = state.isPrivate
            )
        }
    }
}
