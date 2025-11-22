package com.shaarli.poster.data.metadata

interface TitleFetcher {
    suspend fun fetchTitle(url: String): Result<String>
}
