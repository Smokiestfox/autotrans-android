package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.AppSettings
import com.autotrans.android.domain.repository.SettingsRepository

/** Persists updated [AppSettings] to DataStore. */
class UpdateSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings): Result<Unit> =
        settingsRepository.updateSettings(settings)
}
