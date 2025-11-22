package com.shaarli.poster.data.model

data class ShareFormState(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val tags: String = "",
    val isPrivate: Boolean = false,
    val status: ShareStatus = ShareStatus.Idle,
    val errorMessage: String? = null
)

enum class ShareStatus {
    Idle,
    Prefilling,
    Posting,
    Success,
    Error
}
