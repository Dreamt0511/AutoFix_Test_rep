package com.pocketagent.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_API_BASE_URL = stringPreferencesKey("api_base_url")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        private val KEY_HAS_API_KEY = booleanPreferencesKey("has_api_key")
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_AGENT_VERSION = stringPreferencesKey("agent_version")
    }

    val hasApiKey: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_API_KEY] ?: false
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE_URL] ?: "https://api.openai.com/v1"
    }

    val modelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_NAME] ?: "gpt-4o"
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun saveApiConfig(apiKey: String, baseUrl: String, model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = apiKey
            prefs[KEY_API_BASE_URL] = baseUrl
            prefs[KEY_MODEL_NAME] = model
            prefs[KEY_HAS_API_KEY] = apiKey.isNotBlank()
        }
    }

    suspend fun getApiKey(): String {
        var key = ""
        context.dataStore.data.collect { prefs ->
            key = prefs[KEY_API_KEY] ?: ""
        }
        return key
    }

    suspend fun markLaunched() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
        }
    }

    suspend fun setAgentVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AGENT_VERSION] = version
        }
    }
}