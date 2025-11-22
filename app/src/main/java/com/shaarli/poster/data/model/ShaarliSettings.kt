package com.shaarli.poster.data.model

enum class AuthType {
    Token,
    Session
}

data class ShaarliSettings(
    val baseUrl: String = "",
    val authType: AuthType = AuthType.Token,
    val username: String = "",
    val password: String = "",
    val apiSecret: String = ""
)
