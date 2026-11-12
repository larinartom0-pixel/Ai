package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aiagent_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
        private const val KEY_CONTEXT_LIMIT = "context_limit"
        private const val KEY_AUTO_RETRY = "auto_retry_enabled"
        private const val KEY_RETRY_INTERVAL = "retry_interval_seconds"
        private const val KEY_ADB_HOST = "adb_host"
        private const val KEY_ADB_PORT = "adb_port"
        private const val KEY_ADB_PAIRING_PORT = "adb_pairing_port"
        private const val KEY_ADB_PAIRING_CODE = "adb_pairing_code"

        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_RETRY_INTERVAL = 30 // 30 seconds as specified by user
        const val DEFAULT_CONTEXT_LIMIT = 20
        const val DEFAULT_ADB_HOST = "127.0.0.1"
        const val DEFAULT_ADB_PORT = 5555
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    var customBaseUrl: String
        get() = prefs.getString(KEY_CUSTOM_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_BASE_URL, value.trim()).apply()

    var contextLimit: Int
        get() = prefs.getInt(KEY_CONTEXT_LIMIT, DEFAULT_CONTEXT_LIMIT)
        set(value) = prefs.edit().putInt(KEY_CONTEXT_LIMIT, value).apply()

    var autoRetryEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETRY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RETRY, value).apply()

    var retryIntervalSeconds: Int
        get() = prefs.getInt(KEY_RETRY_INTERVAL, DEFAULT_RETRY_INTERVAL)
        set(value) = prefs.edit().putInt(KEY_RETRY_INTERVAL, value).apply()

    var adbHost: String
        get() = prefs.getString(KEY_ADB_HOST, DEFAULT_ADB_HOST) ?: DEFAULT_ADB_HOST
        set(value) = prefs.edit().putString(KEY_ADB_HOST, value.trim()).apply()

    var adbPort: Int
        get() = prefs.getInt(KEY_ADB_PORT, DEFAULT_ADB_PORT)
        set(value) = prefs.edit().putInt(KEY_ADB_PORT, value).apply()

    var adbPairingPort: Int
        get() = prefs.getInt(KEY_ADB_PAIRING_PORT, 0)
        set(value) = prefs.edit().putInt(KEY_ADB_PAIRING_PORT, value).apply()

    var adbPairingCode: String
        get() = prefs.getString(KEY_ADB_PAIRING_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ADB_PAIRING_CODE, value.trim()).apply()
}
