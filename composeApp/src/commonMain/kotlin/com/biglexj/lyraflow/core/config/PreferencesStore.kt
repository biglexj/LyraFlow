package com.biglexj.lyraflow.core.config

interface PreferencesStore {
    fun load(): AppPreferences
    fun save(preferences: AppPreferences)
}
