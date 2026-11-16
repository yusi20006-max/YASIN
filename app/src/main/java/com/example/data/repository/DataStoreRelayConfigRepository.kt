package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.remote.RelayPingClient
import com.example.data.remote.RelayPingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.yasinDataStore: DataStore<Preferences> by preferencesDataStore(name = "yasin_hub_prefs")

class DataStoreRelayConfigRepository(
    private val context: Context,
    private val pingClient: RelayPingClient
) : RelayConfigRepository {

    private object Keys {
        val RELAY_BASE_URL = stringPreferencesKey("relay_base_url")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AUTO_ATTACH_CONTEXT = booleanPreferencesKey("auto_attach_context")
        val EITAAYAR_TOKEN = stringPreferencesKey("eitaayar_token")
        val EITAA_CHANNEL_ID = stringPreferencesKey("eitaa_channel_id")
    }

    override val settingsFlow: Flow<RelaySettings> = context.yasinDataStore.data.map { prefs ->
        RelaySettings(
            relayBaseUrl = prefs[Keys.RELAY_BASE_URL].orEmpty(),
            githubOwner = prefs[Keys.GITHUB_OWNER]?.takeIf { it.isNotBlank() } ?: "yusi20006-max",
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            autoAttachContext = prefs[Keys.AUTO_ATTACH_CONTEXT] ?: true,
            eitaayarToken = prefs[Keys.EITAAYAR_TOKEN].orEmpty(),
            eitaaChannelId = prefs[Keys.EITAA_CHANNEL_ID].orEmpty()
        )
    }

    override suspend fun updateRelayUrl(url: String) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.RELAY_BASE_URL] = url.trim()
        }
    }

    override suspend fun updateGithubOwner(owner: String) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.GITHUB_OWNER] = owner.trim().ifBlank { "yusi20006-max" }
        }
    }

    override suspend fun updateDarkTheme(dark: Boolean) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = dark
        }
    }

    override suspend fun updateAutoAttachContext(enabled: Boolean) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.AUTO_ATTACH_CONTEXT] = enabled
        }
    }

    override suspend fun updateEitaayarConfig(token: String, channelId: String) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.EITAAYAR_TOKEN] = token.trim()
            prefs[Keys.EITAA_CHANNEL_ID] = channelId.trim()
        }
    }

    override suspend fun pingRelay(url: String): RelayPingResult = pingClient.ping(url)
}
