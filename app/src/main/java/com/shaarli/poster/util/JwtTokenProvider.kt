package com.shaarli.poster.util

import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class JwtTokenProvider {

    fun generate(secret: String): String? {
        val key = secret.trim()
        if (key.isBlank()) return null

        val header = JSONObject().apply {
            put("typ", "JWT")
            put("alg", "HS512")
        }.toString()

        val now = (System.currentTimeMillis() / 1000L) - CLOCK_SKEW_SECONDS
        val payload = JSONObject().apply {
            put("iat", now)
        }.toString()

        val headerBase64 = base64Url(header)
        val payloadBase64 = base64Url(payload)
        val content = "$headerBase64.$payloadBase64"
        val signature = hmacSha512Base64Url(content, key) ?: return null
        return "$content.$signature"
    }

    private fun base64Url(input: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(input.toByteArray(StandardCharsets.UTF_8))

    private fun hmacSha512Base64Url(data: String, secret: String): String? {
        return try {
            val mac = Mac.getInstance("HmacSHA512")
            val keySpec = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            mac.init(keySpec)
            val raw = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        // Cushion to mitigate small client/server clock skew; token remains within 9-minute validity.
        private const val CLOCK_SKEW_SECONDS = 30L
    }
}
