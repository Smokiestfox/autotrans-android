package com.autotrans.android.domain.usecase

import com.autotrans.android.domain.model.DownloadProgress
import com.autotrans.android.domain.model.Language
import com.autotrans.android.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow

/**
 * Downloads an ML Kit language model.
 * Emits [DownloadProgress] updates until completion or failure.
 */
class DownloadLanguageModelUseCase(
    private val repository: LanguageRepository
) {
    operator fun invoke(language: Language): Flow<DownloadProgress> =
        repository.downloadModel(language)
}
