package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.AppSettings
import com.autotrans.android.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Observes [AppSettings] changes from DataStore. */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = settingsRepository.settings
}
