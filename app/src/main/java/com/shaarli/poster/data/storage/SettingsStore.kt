package com.shaarli.poster.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shaarli.poster.data.model.ShaarliSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun load(): ShaarliSettings = withContext(Dispatchers.IO) {
        ShaarliSettings(
            baseUrl = preferences.getString(KEY_BASE_URL, "") ?: "",
            apiSecret = preferences.getString(KEY_API_SECRET, "") ?: ""
        )
    }

    suspend fun save(settings: ShaarliSettings) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString(KEY_BASE_URL, settings.baseUrl)
            .putString(KEY_API_SECRET, settings.apiSecret)
            .apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_FILE = "shaarli_poster.secure.prefs"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_SECRET = "api_secret"
    }
}
