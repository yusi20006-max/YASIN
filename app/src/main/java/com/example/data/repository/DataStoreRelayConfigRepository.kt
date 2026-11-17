package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val PRESS_AUTO_PUBLISH = booleanPreferencesKey("press_auto_publish")
        val PRESS_HOURLY_LIMIT = intPreferencesKey("press_hourly_limit")
        val PRESS_AUTO_REWRITE = booleanPreferencesKey("press_auto_rewrite")
        val PRESS_LAST_AUTO_RUN = longPreferencesKey("press_last_auto_run")
    }

    override val settingsFlow: Flow<RelaySettings> = context.yasinDataStore.data.map { prefs ->
        RelaySettings(
            relayBaseUrl = prefs[Keys.RELAY_BASE_URL].orEmpty(),
            githubOwner = prefs[Keys.GITHUB_OWNER]?.takeIf { it.isNotBlank() } ?: "yusi20006-max",
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            autoAttachContext = prefs[Keys.AUTO_ATTACH_CONTEXT] ?: true,
            eitaayarToken = prefs[Keys.EITAAYAR_TOKEN].orEmpty(),
            eitaaChannelId = prefs[Keys.EITAA_CHANNEL_ID].orEmpty(),
            pressAutoPublishEnabled = prefs[Keys.PRESS_AUTO_PUBLISH] ?: false,
            pressHourlyLimit = (prefs[Keys.PRESS_HOURLY_LIMIT] ?: 20).coerceIn(1, 50),
            pressAutoRewriteWithAi = prefs[Keys.PRESS_AUTO_REWRITE] ?: false,
            pressLastAutoRunAt = prefs[Keys.PRESS_LAST_AUTO_RUN] ?: 0L
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

    override suspend fun updatePressAutoPublishEnabled(enabled: Boolean) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.PRESS_AUTO_PUBLISH] = enabled
        }
    }

    override suspend fun updatePressHourlyLimit(limit: Int) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.PRESS_HOURLY_LIMIT] = limit.coerceIn(1, 50)
        }
    }

    override suspend fun updatePressAutoRewriteWithAi(enabled: Boolean) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.PRESS_AUTO_REWRITE] = enabled
        }
    }

    override suspend fun updatePressLastAutoRunAt(timestamp: Long) {
        context.yasinDataStore.edit { prefs ->
            prefs[Keys.PRESS_LAST_AUTO_RUN] = timestamp
        }
    }

    override suspend fun pingRelay(url: String): RelayPingResult = pingClient.ping(url)
}
