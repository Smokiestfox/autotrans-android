package com.autotrans.android.domain.repository

import com.autotrans.android.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Persists and observes [AppSettings] via DataStore.
 * Implementation lives in :data.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
}
