package com.shaarli.poster.data.model

data class Draft(
    val id: String,
    val payload: LinkPayload,
    val createdAtEpochMillis: Long
)
